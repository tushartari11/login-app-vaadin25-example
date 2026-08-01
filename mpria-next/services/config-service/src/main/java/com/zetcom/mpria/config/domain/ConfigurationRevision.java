package com.zetcom.mpria.config.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * One complete configuration — every record type and every screen — at one point in its lifecycle.
 *
 * <p>The record is immutable and every transition returns a new instance. That is what makes a
 * published revision cacheable for ever by its consumers, and what makes rollback a pointer move to
 * an earlier revision rather than a restore (docs/ui-configuration.md §2.1).
 *
 * @param version the draft's optimistic-lock counter, surfaced as {@link #etag()}. Only a draft
 *                ever advances it; a published revision cannot change, so it has nothing to lock.
 */
public record ConfigurationRevision(String id,
                                    RevisionStatus status,
                                    Instant publishedAt,
                                    int version,
                                    List<RecordTypeDefinition> recordTypes,
                                    List<ScreenLayout> screens) {

    public ConfigurationRevision {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("revision id must not be blank");
        }
        if (status == null) {
            throw new IllegalArgumentException("revision '" + id + "' has no status");
        }
        if (version < 0) {
            throw new IllegalArgumentException("revision '" + id + "' has a negative version");
        }
        boolean published = status == RevisionStatus.PUBLISHED;
        if (published && publishedAt == null) {
            throw new IllegalArgumentException("published revision '" + id + "' has no publishedAt");
        }
        if (!published && publishedAt != null) {
            throw new IllegalArgumentException(
                    "revision '" + id + "' is " + status + " and must not carry a publishedAt");
        }
        recordTypes = recordTypes == null ? List.of() : List.copyOf(recordTypes);
        screens = screens == null ? List.of() : List.copyOf(screens);
        requireDistinct(id, recordTypes.stream().map(RecordTypeDefinition::recordType).toList(),
                "record type");
        requireDistinct(id, screens.stream().map(ScreenLayout::screenId).toList(), "screen");
    }

    public static ConfigurationRevision draft(String id,
                                              List<RecordTypeDefinition> recordTypes,
                                              List<ScreenLayout> screens) {
        return new ConfigurationRevision(id, RevisionStatus.DRAFT, null, 0, recordTypes, screens);
    }

    public static ConfigurationRevision published(String id,
                                                  Instant publishedAt,
                                                  List<RecordTypeDefinition> recordTypes,
                                                  List<ScreenLayout> screens) {
        return new ConfigurationRevision(
                id, RevisionStatus.PUBLISHED, publishedAt, 0, recordTypes, screens);
    }

    public Optional<RecordTypeDefinition> recordType(String recordType) {
        return recordTypes.stream().filter(t -> t.recordType().equals(recordType)).findFirst();
    }

    public Optional<ScreenLayout> screen(String screenId) {
        return screens.stream().filter(s -> s.screenId().equals(screenId)).findFirst();
    }

    /**
     * The value a client must echo as {@code If-Match} on its next write. The designer autosaves
     * every few seconds, so without it two admins editing one draft silently overwrite each other.
     */
    public String etag() {
        return "\"" + version + "\"";
    }

    /**
     * Replaces (or adds) one screen's layout.
     *
     * <p>Editing a {@link RevisionStatus#VALIDATED} candidate returns it to {@code DRAFT}: the
     * verdict described content that no longer exists, and carrying it forward would let unchecked
     * configuration reach {@link #publishAs}.
     */
    public ConfigurationRevision withScreen(ScreenLayout screen) {
        requireMutable();
        List<ScreenLayout> replaced = new ArrayList<>(screens.size() + 1);
        boolean found = false;
        for (ScreenLayout existing : screens) {
            boolean same = existing.screenId().equals(screen.screenId());
            replaced.add(same ? screen : existing);
            found |= same;
        }
        if (!found) {
            replaced.add(screen);
        }
        return new ConfigurationRevision(
                id, RevisionStatus.DRAFT, null, version + 1, recordTypes, replaced);
    }

    /** Marks the draft as the validated candidate. Called only by the publish gate. */
    public ConfigurationRevision validated() {
        requireMutable();
        return new ConfigurationRevision(
                id, RevisionStatus.VALIDATED, null, version + 1, recordTypes, screens);
    }

    /**
     * Emits the immutable revision consumers will pin to.
     *
     * <p>Only a validated candidate may be published — the gate is a state transition rather than a
     * call the caller could forget to make.
     */
    public ConfigurationRevision publishAs(String revisionId, Instant at) {
        if (status != RevisionStatus.VALIDATED) {
            throw new IllegalStateException(
                    "revision '" + id + "' is " + status + " and must be VALIDATED before publication");
        }
        return published(revisionId, at, recordTypes, screens);
    }

    /** Reopens this revision's content as a fresh, editable draft. */
    public ConfigurationRevision asDraft(String draftId) {
        return draft(draftId, recordTypes, screens);
    }

    private void requireMutable() {
        if (status == RevisionStatus.PUBLISHED) {
            throw new IllegalStateException("published revision '" + id + "' is immutable");
        }
    }

    private static void requireDistinct(String id, List<String> ids, String what) {
        Set<String> seen = new HashSet<>();
        for (String value : ids) {
            if (!seen.add(value)) {
                throw new IllegalArgumentException(
                        "revision '" + id + "' has a duplicate " + what + " '" + value + "'");
            }
        }
    }
}
