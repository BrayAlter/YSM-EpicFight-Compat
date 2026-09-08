package net.okitsu.ysmepicfightcompat.compat;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.ModLoadingIssue;
import net.neoforged.neoforgespi.language.IModInfo;
import net.okitsu.ysmepicfightcompat.config.ClientPreferences;
import net.okitsu.ysmepicfightcompat.config.TestConfigs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YSMCompatibilityWarningFilterTest {
    @Test
    void neoForgeFieldsStillMatchTheReflectionBoundary() throws ReflectiveOperationException {
        assertEquals(List.class, ModLoader.class.getDeclaredField("loadingIssues").getType());
        assertEquals(IModInfo.class, ModLoadingIssue.class.getDeclaredField("affectedMod").getType());
        assertEquals(String.class, ModLoadingIssue.class.getDeclaredField("translationKey").getType());
        assertEquals(List.class, ModLoadingIssue.class.getDeclaredField("translationArgs").getType());
    }

    @Test
    void firstLaunchKeepsAndNextLaunchRemovesOnlyTheTarget(@TempDir Path directory)
            throws ReflectiveOperationException {
        Path path = directory.resolve("ysm-epicfight-compat-client.toml");
        // Match the loader's synchronous writes before simulating the next client launch.
        try (CommentedFileConfig first = CommentedFileConfig.builder(path).sync().build()) {
            first.load();
            TestConfigs.setConfig(ClientPreferences.CLIENT_SPEC, first);
            List<ModLoadingIssue> warnings = samples();
            assertEquals(0, YSMCompatibilityWarningFilter.processWarnings(warnings));
            assertEquals(3, warnings.size());
            assertTrue(ClientPreferences.YSM_WARNING_ACKNOWLEDGED.get());
            // The harness binds without a loader save path; flush the acknowledgement explicitly.
            TestConfigs.save(ClientPreferences.CLIENT_SPEC);
        } finally {
            TestConfigs.setConfig(ClientPreferences.CLIENT_SPEC, null);
        }
        try (CommentedFileConfig next = CommentedFileConfig.builder(path).sync().build()) {
            next.load();
            TestConfigs.setConfig(ClientPreferences.CLIENT_SPEC, next);
            List<ModLoadingIssue> warnings = samples();
            assertEquals(1, YSMCompatibilityWarningFilter.processWarnings(warnings));
            assertEquals(2, warnings.size());
        } finally {
            TestConfigs.setConfig(ClientPreferences.CLIENT_SPEC, null);
        }
    }

    private static List<ModLoadingIssue> samples() {
        return new ArrayList<>(List.of(
                warning("yes_steve_model", "error.yes_steve_model.incompatible_mod", "Epic Fight"),
                warning("yes_steve_model", "error.yes_steve_model.incompatible_mod", "Another Mod"),
                warning("another_mod", "error.yes_steve_model.incompatible_mod", "Epic Fight")));
    }

    private static ModLoadingIssue warning(String modId, String message, Object... context) {
        IModInfo info = (IModInfo) Proxy.newProxyInstance(IModInfo.class.getClassLoader(),
                new Class<?>[]{IModInfo.class}, (proxy, method, arguments) -> {
                    if (method.getName().equals("getModId") || method.getName().equals("toString")) {
                        return modId;
                    }
                    // withAffectedMod resolves the owning file: return an empty stub chain.
                    if (method.getName().equals("getOwningFile")) {
                        return stub(net.neoforged.neoforgespi.language.IModFileInfo.class);
                    }
                    return defaultValue(method.getReturnType());
                });
        return ModLoadingIssue.warning(message, context).withAffectedMod(info);
    }

    private static Object stub(Class<?> type) {
        return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("toString")) {
                        return type.getSimpleName() + " stub";
                    }
                    if (method.getReturnType().isInterface()) {
                        return stub(method.getReturnType());
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0.0F;
        return 0.0D;
    }
}
