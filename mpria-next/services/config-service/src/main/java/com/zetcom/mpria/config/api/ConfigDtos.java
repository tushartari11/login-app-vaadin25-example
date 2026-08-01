package com.zetcom.mpria.config.api;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.zetcom.mpria.config.application.ConfigurationBundle;
import com.zetcom.mpria.config.application.ConfigurationDiff;
import com.zetcom.mpria.config.application.ImportReport;
import com.zetcom.mpria.config.application.PublishedScreen;
import com.zetcom.mpria.config.domain.ComponentDefinition;
import com.zetcom.mpria.config.domain.ConfigurationError;
import com.zetcom.mpria.config.domain.ConfigurationRevision;
import com.zetcom.mpria.config.domain.FieldType;
import com.zetcom.mpria.pm.Layout;

/**
 * Wire types for the configuration API.
 *
 * <p>Only the shapes that are genuinely the API's own live here. The schema and the export bundle
 * are {@link ConfigurationBundle} types, served straight out of the application layer, because a
 * separate DTO carrying the same JSON is one more thing that can drift from the exported file.
 */
final class ConfigDtos {

    private ConfigDtos() {
    }

    /**
     * @param revisionId the revision the layout came from. A consumer caches by it, and a layout
     *                   without one cannot be cached safely
     */
    record ScreenResponse(String screenId, String recordType, String revisionId, Layout layout) {

        static ScreenResponse of(PublishedScreen screen) {
            return new ScreenResponse(
                    screen.screen().screenId(),
                    screen.screen().recordType(),
                    screen.revisionId(),
                    screen.layout());
        }
    }

    record RevisionResponse(String revisionId, Instant publishedAt, String status) {

        static RevisionResponse of(ConfigurationRevision revision) {
            return new RevisionResponse(revision.id(), revision.publishedAt(), revision.status().name());
        }
    }

    record ComponentResponse(String type,
                             String label,
                             List<String> acceptsFieldTypes,
                             Map<String, ComponentOptionResponse> options) {

        static ComponentResponse of(ComponentDefinition component) {
            // Insertion-ordered: the inspector lists a component's options in declaration order.
            Map<String, ComponentOptionResponse> options = new LinkedHashMap<>();
            component.options().forEach(
                    (name, spec) -> options.put(name, ComponentOptionResponse.of(spec)));

            return new ComponentResponse(
                    component.type(),
                    component.label(),
                    component.acceptsFieldTypes().stream().map(FieldType::name).toList(),
                    Collections.unmodifiableMap(options));
        }
    }

    /** @param defaultValue serialised as {@code default}, a Java keyword and so not usable as a name */
    record ComponentOptionResponse(String type, @JsonProperty("default") Object defaultValue) {

        static ComponentOptionResponse of(ComponentDefinition.OptionSpec option) {
            return new ComponentOptionResponse(option.type(), option.defaultValue());
        }
    }

    /** The mutable draft the designer edits. Its ETag travels in the header, not the body. */
    record DraftResponse(String draftId,
                         String status,
                         List<ConfigurationBundle.RecordTypeBundle> recordTypes,
                         List<ConfigurationBundle.ScreenBundle> screens) {

        static DraftResponse of(ConfigurationRevision draft) {
            ConfigurationBundle content = ConfigurationBundle.of(draft);
            return new DraftResponse(
                    draft.id(), draft.status().name(), content.recordTypes(), content.screens());
        }
    }

    record ImportReportResponse(boolean valid,
                                List<ConfigurationErrorResponse> errors,
                                String draftId,
                                ConfigurationDiff diff) {

        static ImportReportResponse of(ImportReport report) {
            return new ImportReportResponse(
                    report.valid(),
                    ConfigurationErrorResponse.of(report.errors()),
                    report.draftId(),
                    report.diff());
        }
    }

    /** @param nodeId absent when the problem is with the screen as a whole rather than a node in it */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record ConfigurationErrorResponse(String screenId, String nodeId, String message) {

        static List<ConfigurationErrorResponse> of(List<ConfigurationError> errors) {
            return errors.stream()
                    .map(error -> new ConfigurationErrorResponse(
                            error.screenId(), error.nodeId(), error.message()))
                    .toList();
        }
    }
}
