package com.zetcom.mpria.config.application;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import com.zetcom.mpria.config.application.port.ConfigurationRepository;
import com.zetcom.mpria.config.domain.ConfigurationError;
import com.zetcom.mpria.config.domain.ConfigurationRevision;
import com.zetcom.mpria.config.domain.ConfigurationValidationException;
import com.zetcom.mpria.config.domain.ConfigurationValidator;
import com.zetcom.mpria.config.domain.RevisionStatus;
import com.zetcom.mpria.config.domain.ScreenLayout;
import com.zetcom.mpria.pm.Layout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Write use cases: editing a draft, and the gate between that draft and everybody else.
 *
 * <p>Business rules live in the domain — the validator decides what is publishable and the
 * revision decides what transitions are legal. What lives here is orchestration: load, delegate,
 * store.
 */
@Service
public class ConfigurationDraftService {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationDraftService.class);

    private final ConfigurationRepository repository;
    private final ConfigurationValidator validator;
    private final Clock clock;

    public ConfigurationDraftService(ConfigurationRepository repository,
                                     ConfigurationValidator validator,
                                     Clock clock) {
        this.repository = repository;
        this.validator = validator;
        this.clock = clock;
    }

    public ConfigurationRevision draft(String draftId) {
        ConfigurationRevision revision = repository.findById(draftId)
                .orElseThrow(() -> new DraftNotFoundException(draftId));
        if (revision.status() == RevisionStatus.PUBLISHED) {
            // Not a permission failure: there is simply no editable draft under that id.
            throw new DraftNotFoundException(draftId);
        }
        return revision;
    }

    /**
     * Replaces one screen's layout on a draft.
     *
     * <p>Deliberately does not validate. A draft is allowed to be incoherent mid-edit — a field
     * dragged off the canvas before its replacement is dragged on is a normal keystroke, not an
     * error — and validation is the publish gate.
     *
     * @param ifMatch the ETag the caller last saw, or {@code null} to write unconditionally
     */
    public ConfigurationRevision replaceLayout(String draftId,
                                               String screenId,
                                               Layout layout,
                                               String ifMatch) {
        ConfigurationRevision draft = draft(draftId);
        requireCurrent(draft, ifMatch);
        if (!screenId.equals(layout.screenId())) {
            throw new IllegalArgumentException("the path names screen '" + screenId
                    + "' but the layout declares '" + layout.screenId() + "'");
        }
        return repository.save(
                draft.withScreen(new ScreenLayout(screenId, layout.recordType(), layout)));
    }

    /**
     * Validates the draft and, if it passes, emits a new immutable revision as the current one.
     *
     * <p>The draft is left in place and marked {@code VALIDATED}, so the administrator can keep
     * editing from where they were; the next edit returns it to {@code DRAFT}.
     */
    public ConfigurationRevision publish(String draftId) {
        ConfigurationRevision draft = draft(draftId);
        validator.requireValid(draft);

        ConfigurationRevision candidate = repository.save(draft.validated());
        ConfigurationRevision published = candidate.publishAs(newRevisionId(), clock.instant());
        repository.publish(published);

        log.info("published configuration revision {} from draft {}", published.id(), draftId);
        return published;
    }

    /** Any revision by id, draft or published — what an export bundles. */
    public ConfigurationRevision revision(String revisionId) {
        return repository.findById(revisionId)
                .orElseThrow(() -> new RevisionNotFoundException(revisionId));
    }

    /**
     * Validates a bundle and reports what importing it would change.
     *
     * <p>A dry run writes nothing. A real import opens a <em>draft</em> and never publishes: an
     * uploaded file must go through the same gate and the same human review as anything an
     * administrator built by hand.
     */
    public ImportReport importBundle(ConfigurationBundle bundle, boolean dryRun) {
        String draftId = newDraftId();
        ConfigurationRevision candidate = bundle.toDraft(draftId);
        List<ConfigurationError> errors = validator.validate(candidate);
        ConfigurationDiff diff = ConfigurationDiff.between(
                repository.currentPublished().orElse(null), candidate);

        if (dryRun) {
            return new ImportReport(errors.isEmpty(), errors, null, diff);
        }
        if (!errors.isEmpty()) {
            throw new ConfigurationValidationException(errors);
        }
        repository.save(candidate);
        log.info("imported configuration bundle {} as draft {}", bundle.revisionId(), draftId);
        return new ImportReport(true, List.of(), draftId, diff);
    }

    private static void requireCurrent(ConfigurationRevision draft, String ifMatch) {
        if (ifMatch != null && !ifMatch.equals(draft.etag())) {
            throw new StaleDraftException(draft.id(), ifMatch, draft.etag());
        }
    }

    private static String newRevisionId() {
        return "rev-" + UUID.randomUUID();
    }

    private static String newDraftId() {
        return "draft-" + UUID.randomUUID();
    }
}
