package net.okitsu.ysmepicfightcompat.compat;

import net.okitsu.ysmepicfightcompat.CompatMod;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.ModLoadingIssue;
import net.neoforged.neoforgespi.language.IModInfo;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;

/**
 * Processes NeoForge's registered loading issues after every mod has completed
 * sided setup.
 *
 * <p>{@link ModLoader} is initialized before ordinary mod mixin configs are
 * applied, so intercepting issue registration with a mod mixin is not
 * reliable. NeoForge 21 has no public removal API; the mutable issue list is
 * therefore accessed reflectively at load complete. Issue metadata is read
 * through the public {@link ModLoadingIssue} record accessors. These are
 * NeoForge classes, not obfuscated official-YSM internals.</p>
 */
public final class YSMCompatibilityWarningFilter {
    private static final String LOADING_ISSUES_FIELD = "loadingIssues";

    private YSMCompatibilityWarningFilter() {
    }

    /**
     * Retains and remembers the target warning on its first launch, then
     * removes only that exact warning on later launches. Any reflection or
     * config failure is fail-open and leaves all NeoForge issues untouched.
     */
    public static void processRegisteredWarnings() {
        try {
            List<ModLoadingIssue> issues = mutableIssues();
            synchronized (issues) {
                processWarnings(issues);
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            CompatMod.LOG.warn(
                    "YSM-EF Compat: could not process the official YSM/Epic Fight compatibility warning; retaining it",
                    exception);
        }
    }

    static int processWarnings(List<ModLoadingIssue> issues) {
        int removed = 0;
        Iterator<ModLoadingIssue> iterator = issues.iterator();
        while (iterator.hasNext()) {
            ModLoadingIssue issue = iterator.next();
            if (issue.severity() != ModLoadingIssue.Severity.WARNING) {
                continue;
            }
            WarningMetadata metadata = metadata(issue);
            if (YSMCompatibilityWarningState.shouldSuppress(
                    metadata.sourceModId(), metadata.messageKey(), metadata.context())) {
                iterator.remove();
                removed++;
            }
        }

        if (removed > 0) {
            CompatMod.LOG.info(
                    "YSM-EF Compat: removed {} already-shown official YSM/Epic Fight compatibility warning(s)",
                    removed);
        }
        return removed;
    }

    @SuppressWarnings("unchecked")
    private static List<ModLoadingIssue> mutableIssues() throws ReflectiveOperationException {
        return (List<ModLoadingIssue>) readStaticField(ModLoader.class, LOADING_ISSUES_FIELD);
    }

    static WarningMetadata metadata(ModLoadingIssue issue) {
        IModInfo modInfo = issue.affectedMod();
        String sourceModId = modInfo == null ? null : modInfo.getModId();
        String messageKey = issue.translationKey();
        List<Object> context = issue.translationArgs();
        return new WarningMetadata(sourceModId, messageKey, context);
    }

    private static Object readStaticField(Class<?> owner, String fieldName)
            throws ReflectiveOperationException {
        Field field = owner.getDeclaredField(fieldName);
        if (!field.trySetAccessible()) {
            throw new IllegalAccessException("Cannot access " + owner.getName() + "." + fieldName);
        }
        return field.get(null);
    }

    record WarningMetadata(String sourceModId, String messageKey, List<?> context) {
    }
}
