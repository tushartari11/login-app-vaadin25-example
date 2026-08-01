package com.zetcom.mpria.config.application;

import java.util.List;
import java.util.function.Function;

import com.zetcom.mpria.config.domain.ConfigurationRevision;
import com.zetcom.mpria.config.domain.RecordTypeDefinition;
import com.zetcom.mpria.config.domain.ScreenLayout;

/**
 * What importing a bundle would actually change.
 *
 * <p>An import is a supply-chain event: a configuration bundle can change what the application
 * does. "Your configuration differs" is not a reviewable answer, so a dry run reports the affected
 * ids and a human decides.
 */
public record ConfigurationDiff(List<String> recordTypesAdded,
                                List<String> recordTypesRemoved,
                                List<String> recordTypesChanged,
                                List<String> screensAdded,
                                List<String> screensRemoved,
                                List<String> screensChanged) {

    public static ConfigurationDiff unchanged() {
        return new ConfigurationDiff(List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }

    /** @param before the configuration in force today; {@code null} before the first publication */
    public static ConfigurationDiff between(ConfigurationRevision before, ConfigurationRevision after) {
        List<RecordTypeDefinition> currentTypes =
                before == null ? List.of() : before.recordTypes();
        List<ScreenLayout> currentScreens = before == null ? List.of() : before.screens();

        return new ConfigurationDiff(
                added(currentTypes, after.recordTypes(), RecordTypeDefinition::recordType),
                added(after.recordTypes(), currentTypes, RecordTypeDefinition::recordType),
                changed(currentTypes, after.recordTypes(), RecordTypeDefinition::recordType),
                added(currentScreens, after.screens(), ScreenLayout::screenId),
                added(after.screens(), currentScreens, ScreenLayout::screenId),
                changed(currentScreens, after.screens(), ScreenLayout::screenId));
    }

    /** Ids present in {@code candidates} and absent from {@code baseline}. */
    private static <T> List<String> added(List<T> baseline, List<T> candidates, Function<T, String> id) {
        List<String> known = baseline.stream().map(id).toList();
        return candidates.stream().map(id).filter(candidate -> !known.contains(candidate)).toList();
    }

    /**
     * Ids present in both whose content differs. Value equality does the comparison: every domain
     * type down to the layout tree is a record, so an unchanged definition compares equal.
     */
    private static <T> List<String> changed(List<T> baseline, List<T> candidates, Function<T, String> id) {
        return candidates.stream()
                .filter(candidate -> baseline.stream()
                        .anyMatch(existing -> id.apply(existing).equals(id.apply(candidate))
                                && !existing.equals(candidate)))
                .map(id)
                .toList();
    }
}
