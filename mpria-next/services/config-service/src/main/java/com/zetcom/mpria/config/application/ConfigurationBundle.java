package com.zetcom.mpria.config.application;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import com.zetcom.mpria.config.domain.ConfigurationRevision;
import com.zetcom.mpria.config.domain.FieldDefinition;
import com.zetcom.mpria.config.domain.FieldType;
import com.zetcom.mpria.config.domain.RecordTypeDefinition;
import com.zetcom.mpria.config.domain.ScreenLayout;
import com.zetcom.mpria.pm.Layout;

/**
 * A configuration as plain, portable JSON.
 *
 * <p>One shape serves three jobs — the startup seed, the export download and the import upload —
 * because a bundle that cannot be re-imported is not a backup, and a seed in a different format
 * from an export is a second thing to keep correct.
 *
 * <p>It lives in the application layer rather than in {@code api} or {@code infrastructure}
 * precisely because both of those need it, and neither may depend on the other.
 *
 * @param revisionId  the revision this bundle was taken from. Recorded for provenance and ignored
 *                    on import: an importing installation names its own revisions, or two sites
 *                    would eventually disagree about what {@code rev-4} contains
 * @param publishedAt when the source revision was published, or {@code null} if it never was
 */
public record ConfigurationBundle(String revisionId,
                                  Instant publishedAt,
                                  List<RecordTypeBundle> recordTypes,
                                  List<ScreenBundle> screens) {

    public ConfigurationBundle {
        recordTypes = recordTypes == null ? List.of() : List.copyOf(recordTypes);
        screens = screens == null ? List.of() : List.copyOf(screens);
    }

    public static ConfigurationBundle of(ConfigurationRevision revision) {
        return new ConfigurationBundle(
                revision.id(),
                revision.publishedAt(),
                revision.recordTypes().stream().map(RecordTypeBundle::of).toList(),
                revision.screens().stream().map(ScreenBundle::of).toList());
    }

    /** Reads the bundle as the published revision a cold start begins from. */
    public ConfigurationRevision toPublishedRevision() {
        if (publishedAt == null) {
            throw new IllegalArgumentException(
                    "bundle '" + revisionId + "' carries no publishedAt and cannot be a published revision");
        }
        return ConfigurationRevision.published(revisionId, publishedAt, toRecordTypes(), toScreens());
    }

    /** Reads the bundle as a fresh, editable draft — what an import produces. */
    public ConfigurationRevision toDraft(String draftId) {
        return ConfigurationRevision.draft(draftId, toRecordTypes(), toScreens());
    }

    private List<RecordTypeDefinition> toRecordTypes() {
        return recordTypes.stream().map(RecordTypeBundle::toDefinition).toList();
    }

    private List<ScreenLayout> toScreens() {
        return screens.stream().map(ScreenBundle::toScreenLayout).toList();
    }

    /**
     * The wire shape of one record type.
     *
     * <p>This is also the body of {@code GET /api/v1/record-types/{recordType}/schema}, which
     * {@code catalog-service} already consumes. Serving the export shape as the schema shape is
     * what stops the two drifting apart.
     */
    public record RecordTypeBundle(String recordType,
                                   int schemaVersion,
                                   String titleFieldId,
                                   List<FieldBundle> fields) {

        public static RecordTypeBundle of(RecordTypeDefinition definition) {
            return new RecordTypeBundle(
                    definition.recordType(),
                    definition.schemaVersion(),
                    definition.titleFieldId(),
                    definition.fields().stream().map(FieldBundle::of).toList());
        }

        RecordTypeDefinition toDefinition() {
            return new RecordTypeDefinition(recordType, schemaVersion, titleFieldId,
                    fields == null ? List.of() : fields.stream().map(FieldBundle::toDefinition).toList());
        }
    }

    /** @param type a {@link FieldType} name; a string on the wire so a client needs no Java enum */
    public record FieldBundle(String id,
                              String type,
                              String label,
                              boolean required,
                              Integer maxLength,
                              List<String> allowedValues) {

        static FieldBundle of(FieldDefinition definition) {
            return new FieldBundle(
                    definition.id(),
                    definition.type().name(),
                    definition.label(),
                    definition.required(),
                    definition.maxLength(),
                    definition.allowedValues());
        }

        FieldDefinition toDefinition() {
            return new FieldDefinition(id, fieldType(), label, required, maxLength,
                    allowedValues == null ? List.of() : allowedValues);
        }

        private FieldType fieldType() {
            try {
                return FieldType.valueOf(type);
            } catch (IllegalArgumentException | NullPointerException e) {
                throw new IllegalArgumentException("field '" + id + "' has unknown type '" + type
                        + "'; expected one of " + Arrays.toString(FieldType.values()), e);
            }
        }
    }

    /** @param layout the real {@code pm-contract} layout, not a parallel shape of this service's own */
    public record ScreenBundle(String screenId, String recordType, Layout layout) {

        static ScreenBundle of(ScreenLayout screen) {
            return new ScreenBundle(screen.screenId(), screen.recordType(), screen.layout());
        }

        ScreenLayout toScreenLayout() {
            return new ScreenLayout(screenId, recordType, layout);
        }
    }
}
