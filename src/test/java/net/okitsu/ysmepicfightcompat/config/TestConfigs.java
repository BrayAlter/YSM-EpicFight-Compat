package net.okitsu.ysmepicfightcompat.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.FileConfig;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforgespi.language.IModInfo;

/**
 * Test-only replacement for the removed {@code ForgeConfigSpec#setConfig(CommentedConfig)}.
 *
 * <p>NeoForge 21.1 binds raw NightConfig objects through
 * {@link ModConfigSpec#acceptConfig(IConfigSpec.ILoadedConfig)}, whose {@code ILoadedConfig}
 * is sealed to the loader's package-private {@code LoadedConfig} record. This helper constructs
 * that record reflectively around a detached {@link ModConfig} whose container has no event bus,
 * so {@code save()} follows the loader's own code path (skipping the null-bus reload event) and
 * file-backed configs are written to their own path exactly like Forge's {@code setConfig} did.</p>
 */
public final class TestConfigs {
    private static final Object BOOTSTRAP = bootstrapLoaderEnvironment();

    private TestConfigs() {
    }

    /**
     * {@code ConfigTracker}'s static initializer resolves {@code FMLPaths.GAMEDIR} and the FML
     * config, which are only set up by a real launcher. Point every {@code FMLPaths} entry at a
     * throwaway directory and load the FML config from there so the loader's own save path works
     * inside plain unit tests.
     */
    private static Object bootstrapLoaderEnvironment() {
        try {
            Path gameDir = java.nio.file.Files.createTempDirectory("ysm-compat-test-game");
            java.lang.reflect.Field absolutePath =
                    net.neoforged.fml.loading.FMLPaths.class.getDeclaredField("absolutePath");
            absolutePath.setAccessible(true);
            java.lang.reflect.Field relativePath =
                    net.neoforged.fml.loading.FMLPaths.class.getDeclaredField("relativePath");
            relativePath.setAccessible(true);
            for (net.neoforged.fml.loading.FMLPaths entry
                    : net.neoforged.fml.loading.FMLPaths.values()) {
                Path resolved = gameDir.resolve((Path) relativePath.get(entry));
                if (resolved.getFileName() != null
                        && resolved.getFileName().toString().indexOf('.') < 0) {
                    java.nio.file.Files.createDirectories(resolved);
                } else if (resolved.getParent() != null) {
                    java.nio.file.Files.createDirectories(resolved.getParent());
                }
                absolutePath.set(entry, resolved);
            }
            net.neoforged.fml.loading.FMLConfig.load();
            return gameDir;
        } catch (ReflectiveOperationException | java.io.IOException exception) {
            throw new IllegalStateException(
                    "Unable to bootstrap FMLPaths for config tests", exception);
        }
    }

    public static void setConfig(ModConfigSpec spec, CommentedConfig config) {
        if (config == null) {
            spec.acceptConfig(null);
            return;
        }
        try {
            // Never hand LoadedConfig a path: nightconfig 3.8 FileConfigs hold a file lock while
            // open, so the loader's external ConfigTracker.writeConfig can never replace the file
            // during a test. Saves go through the FileConfig's own writer via save(spec) instead.
            Class<?> loadedConfigClass = Class.forName("net.neoforged.fml.config.LoadedConfig");
            Constructor<?> loadedConfigConstructor = loadedConfigClass.getDeclaredConstructor(
                    CommentedConfig.class, Path.class, ModConfig.class);
            loadedConfigConstructor.setAccessible(true);
            spec.acceptConfig((IConfigSpec.ILoadedConfig) loadedConfigConstructor.newInstance(
                    config, null, detachedModConfig(spec)));
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "Unable to bind a test config through NeoForge's LoadedConfig", exception);
        }
    }

    /**
     * Persists the spec's bound config the way Forge's {@code ForgeConfigSpec#save} did: through
     * the file config's own writer, which owns the nightconfig file lock and therefore can write
     * while the config is still open.
     */
    public static void save(ModConfigSpec spec) {
        try {
            java.lang.reflect.Field loadedConfigField =
                    ModConfigSpec.class.getDeclaredField("loadedConfig");
            loadedConfigField.setAccessible(true);
            if (loadedConfigField.get(spec) instanceof IConfigSpec.ILoadedConfig loaded
                    && loaded.config() instanceof FileConfig file) {
                file.save();
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to save the bound test config", exception);
        }
    }

    private static ModConfig detachedModConfig(ModConfigSpec spec)
            throws ReflectiveOperationException {
        Constructor<ModConfig> constructor = ModConfig.class.getDeclaredConstructor(
                ModConfig.Type.class, IConfigSpec.class, ModContainer.class, String.class,
                ReentrantLock.class);
        constructor.setAccessible(true);
        return constructor.newInstance(ModConfig.Type.CLIENT, spec, new TestContainer(),
                "ysm_epicfight_compat-test.toml", new ReentrantLock());
    }

    private static final class TestContainer extends ModContainer {
        private TestContainer() {
            super(stubModInfo());
        }

        @Override
        public IEventBus getEventBus() {
            return null;
        }

        private static IModInfo stubModInfo() {
            InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
                case "getModId", "getNamespace", "getDisplayName" -> "ysm_epicfight_compat_test";
                case "getDependencies" -> Collections.emptyList();
                case "getConfig", "getUpdateURL", "getModURL", "getLogoFile" -> Optional.empty();
                case "toString" -> "ysm_epicfight_compat test mod info";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> null;
            };
            return (IModInfo) Proxy.newProxyInstance(TestConfigs.class.getClassLoader(),
                    new Class<?>[] {IModInfo.class}, handler);
        }
    }
}
