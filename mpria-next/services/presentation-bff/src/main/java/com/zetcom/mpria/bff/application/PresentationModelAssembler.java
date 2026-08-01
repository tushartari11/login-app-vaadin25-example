package com.zetcom.mpria.bff.application;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.zetcom.mpria.bff.application.port.CatalogGateway;
import com.zetcom.mpria.bff.application.port.CatalogGateway.WriteResult;
import com.zetcom.mpria.bff.application.port.ConfigurationGateway;
import com.zetcom.mpria.bff.domain.ConfigurationRevision;
import com.zetcom.mpria.bff.domain.FieldDefinition;
import com.zetcom.mpria.bff.domain.FieldErrors;
import com.zetcom.mpria.bff.domain.LayoutSchemaMismatchException;
import com.zetcom.mpria.bff.domain.Lifecycle;
import com.zetcom.mpria.bff.domain.RecordSchema;
import com.zetcom.mpria.bff.domain.RecordSnapshot;
import com.zetcom.mpria.bff.domain.RecordTypeMismatchException;
import com.zetcom.mpria.bff.domain.ScreenDefinition;
import com.zetcom.mpria.pm.ActionModel;
import com.zetcom.mpria.pm.FieldOption;
import com.zetcom.mpria.pm.FieldState;
import com.zetcom.mpria.pm.PresentationModel;
import com.zetcom.mpria.pm.ScreenMeta;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Composes configuration, data and lifecycle into a {@link PresentationModel}.
 *
 * <p>This is the layer MpRIA v8 called {@code MpApplicationModel}, with the Vaadin removed. It is
 * pure orchestration and mapping — no HTTP, no Spring, no annotations — which is what lets the six
 * assembly rules of {@code docs/CONTRACTS.md} be tested against hand-written fakes in milliseconds
 * rather than against a running stack.
 *
 * <p>It is also the only place that decides what a client may do. {@code enabled}, {@code readOnly}
 * and {@code disabledReason} are computed here from configuration and lifecycle, so a client
 * physically does not know how to construct a forbidden request and client-side authorisation cannot
 * drift from server-side (ADR-0003).
 */
public class PresentationModelAssembler {

    private static final Logger log = LoggerFactory.getLogger(PresentationModelAssembler.class);

    /** i18n keys, resolved by the client. Never a caption — the server does not know the language. */
    private static final String CAPABILITY_EDIT = "EDIT";
    private static final String REASON_ALREADY_ARCHIVED = "record.alreadyArchived";
    private static final String REASON_ALREADY_PUBLISHED = "record.alreadyPublished";

    private final ConfigurationGateway configuration;
    private final CatalogGateway catalog;

    public PresentationModelAssembler(ConfigurationGateway configuration, CatalogGateway catalog) {
        this.configuration = configuration;
        this.catalog = catalog;
    }

    /**
     * A blank form for a record that does not exist yet.
     *
     * @param recordType what the caller believes this screen edits, checked against configuration
     */
    public PresentationModel blankForm(String screenId, String recordType) {
        Screen screen = resolve(screenId);
        if (!screen.definition().recordType().equals(recordType)) {
            throw new RecordTypeMismatchException(screenId, screen.definition().recordType(), recordType);
        }
        return assemble(screen, null, Map.of(), FieldErrors.none());
    }

    /** A populated form for an existing record. */
    public PresentationModel existingRecord(String screenId, UUID recordId) {
        Screen screen = resolve(screenId);
        RecordSnapshot record = catalog.get(recordId);
        return assemble(screen, record, record.data(), FieldErrors.none());
    }

    /**
     * Creates a record and returns the screen re-rendered from the outcome.
     *
     * <p>The record type is the screen's, never the caller's: a create that could nominate its own
     * type would let a form write to a schema its layout was not validated against.
     */
    public PresentationModel create(String screenId, RecordSubmission submission) {
        Screen screen = resolve(screenId);
        return switch (catalog.create(screen.definition().recordType(), submission)) {
            case WriteResult.Stored(RecordSnapshot record) ->
                    assemble(screen, record, record.data(), FieldErrors.none());
            // Nothing was stored, so there is still no record: the blank form comes back, holding
            // what the user typed and the reasons it was refused.
            case WriteResult.Rejected(FieldErrors errors) ->
                    assemble(screen, null, submission.data(), errors);
        };
    }

    /**
     * Writes a submitted form and returns the screen re-rendered from the outcome.
     *
     * <p>A refusal is answered with a form, not an error. That is the single most important
     * behaviour in this service: validation messages have to arrive attached to the controls that
     * produced them, or the user is left with a document they cannot act on.
     */
    public PresentationModel submit(String screenId, UUID recordId, RecordSubmission submission) {
        Screen screen = resolve(screenId);
        return switch (catalog.update(recordId, submission)) {
            case WriteResult.Stored(RecordSnapshot record) ->
                    assemble(screen, record, record.data(), FieldErrors.none());
            // The values shown are the caller's, so nothing they typed is lost. The lifecycle and
            // the ETag are re-read, because those are the server's truth and the retry needs them.
            case WriteResult.Rejected(FieldErrors errors) ->
                    assemble(screen, catalog.get(recordId), submission.data(), errors);
        };
    }

    /**
     * Moves the record through its lifecycle and returns the screen re-rendered from the result.
     *
     * <p>No check that the move is permitted happens here, even though the model this caller acted on
     * said whether it was. catalog-service owns the state machine and refuses an illegal move, and a
     * second copy of that rule in the BFF is exactly the duplication that drifts. What the model
     * decides is what to <em>offer</em>; what the owning service decides is what is <em>allowed</em>.
     */
    public PresentationModel transition(String screenId, UUID recordId, Lifecycle target) {
        Screen screen = resolve(screenId);
        RecordSnapshot moved = catalog.transition(recordId, target);
        return assemble(screen, moved, moved.data(), FieldErrors.none());
    }

    /**
     * Resolves the configuration for a screen, pinned to one revision.
     *
     * <p>The revision is resolved once per request and both lookups are made against it, so a
     * publish landing mid-request cannot produce a layout and a schema that disagree.
     */
    private Screen resolve(String screenId) {
        ConfigurationRevision revision = configuration.currentRevision();
        ScreenDefinition definition = configuration.screen(revision, screenId);
        RecordSchema schema = configuration.schema(revision, definition.recordType());
        return new Screen(revision, definition, schema);
    }

    private PresentationModel assemble(Screen screen,
                                       RecordSnapshot record,
                                       Map<String, Object> values,
                                       FieldErrors errors) {
        boolean editable = record == null || record.lifecycle().allowsEditing();
        Map<String, FieldState> fields = fieldStates(screen, values, errors, editable);
        warnAboutUnattachableErrors(screen, errors, fields.keySet());

        return new PresentationModel(
                meta(screen, record, editable),
                screen.definition().layout(),
                fields,
                actions(screen, record));
    }

    /**
     * One {@link FieldState} per field the layout binds — no more and no fewer.
     *
     * <p>Not per field the schema defines: a record type with two hundred fields would otherwise
     * send all of them to render a form showing six. Not fewer either, because
     * {@link PresentationModel} refuses to be built with a bound field it has no state for.
     */
    private Map<String, FieldState> fieldStates(Screen screen,
                                                Map<String, Object> values,
                                                FieldErrors errors,
                                                boolean editable) {
        Map<String, FieldState> fields = new LinkedHashMap<>();
        for (String fieldId : screen.definition().layout().boundFieldIds()) {
            if (fields.containsKey(fieldId)) {
                // A layout may legitimately bind one field in two places; its state is the same.
                continue;
            }
            FieldDefinition definition = screen.schema().field(fieldId)
                    .orElseThrow(() -> new LayoutSchemaMismatchException(
                            screen.definition().screenId(), screen.schema().recordType(), fieldId));
            fields.put(fieldId, fieldState(definition, values.get(fieldId), errors, editable));
        }
        return fields;
    }

    /**
     * Merges what configuration says about a field with what the record currently holds.
     *
     * <p>{@code hidden} is always false in Phase 0. Configured visibility expressions are Phase 1;
     * until then a field is either on the layout or it is not, and a screen that should not show a
     * field is configured without it rather than shipping it hidden to the browser.
     *
     * <p><strong>When that changes, read this first.</strong> {@code hidden} is a layout instruction,
     * not a redaction: the whole model is serialised to the client, so a hidden field's value is still
     * readable in the browser. This was verified against the rendered payload, not assumed. A renderer
     * cannot strip it either — deciding whether a value may be seen is the server's call
     * (ARCHITECTURE.md §7.1). So a field hidden because the <em>principal may not see it</em> must be
     * emitted with a null value here, while a field hidden because it is <em>irrelevant given other
     * values</em> may keep its value. Setting the flag alone would leak.
     */
    private FieldState fieldState(FieldDefinition definition,
                                  Object value,
                                  FieldErrors errors,
                                  boolean editable) {
        List<FieldOption> options = definition.allowedValues().stream()
                // Label defaults to the value itself. Vocabulary labels come from config-service in
                // Phase 1; inventing an i18n key here would be a convention nobody configured.
                .map(allowed -> new FieldOption(allowed, null))
                .toList();

        return new FieldState(
                definition.type(),
                definition.label(),
                value,
                options,
                !editable,
                definition.required(),
                false,
                errors.messagesFor(definition.id()));
    }

    /**
     * @param editable drives {@code capabilities}: what the lifecycle permits. Whether <em>this</em>
     *                 principal may edit <em>this</em> record's organisational unit is a fine-grained
     *                 decision that belongs to {@code access-service}, never to the BFF
     *                 (ARCHITECTURE.md §10)
     */
    private ScreenMeta meta(Screen screen, RecordSnapshot record, boolean editable) {
        return new ScreenMeta(
                screen.definition().screenId(),
                screen.revision().revisionId(),
                record == null ? null : record.id().toString(),
                record == null ? null : record.etag(),
                editable ? List.of(CAPABILITY_EDIT) : List.of());
    }

    /**
     * The hypermedia actions, computed from the record's lifecycle.
     *
     * <p>A record that does not exist yet is offered only {@code save}. Advertising a disabled
     * {@code publish} on a blank form would describe a state the record cannot be in, and the client
     * would have to explain a button that could never apply.
     */
    private List<ActionModel> actions(Screen screen, RecordSnapshot record) {
        String screenId = screen.definition().screenId();
        if (record == null) {
            return List.of(ActionModel.enabled(
                    "save", "action.save", "POST", ScreenRoutes.screen(screenId)));
        }

        List<ActionModel> actions = new ArrayList<>();
        actions.add(ActionModel.enabled(
                "save", "action.save", "PUT", ScreenRoutes.record(screenId, record.id())));
        actions.add(publish(screenId, record));
        actions.add(archive(screenId, record));
        return actions;
    }

    /** Publishing is a draft's transition. Anything already past that state has nowhere to go. */
    private ActionModel publish(String screenId, RecordSnapshot record) {
        String href = ScreenRoutes.lifecycle(screenId, record.id(), Lifecycle.PUBLISHED);
        if (record.lifecycle() == Lifecycle.DRAFT) {
            return ActionModel.enabled("publish", "action.publish", "POST", href);
        }
        String reason = record.lifecycle() == Lifecycle.ARCHIVED
                ? REASON_ALREADY_ARCHIVED
                : REASON_ALREADY_PUBLISHED;
        return ActionModel.disabled("publish", "action.publish", "POST", href, reason);
    }

    /** Archiving always asks first: it takes a record out of circulation and does not reverse. */
    private ActionModel archive(String screenId, RecordSnapshot record) {
        String href = ScreenRoutes.lifecycle(screenId, record.id(), Lifecycle.ARCHIVED);
        ActionModel action = record.lifecycle() == Lifecycle.ARCHIVED
                ? ActionModel.disabled("archive", "action.archive", "POST", href, REASON_ALREADY_ARCHIVED)
                : ActionModel.enabled("archive", "action.archive", "POST", href);
        return action.requiringConfirmation();
    }

    /**
     * A validation message for a field no slot binds has nowhere to be rendered.
     *
     * <p>Dropping it is the only option that still returns a usable form, but it is a configuration
     * defect worth a log line: the record type requires something the screen gives the user no way to
     * enter, so the form can never be submitted successfully.
     */
    private void warnAboutUnattachableErrors(Screen screen, FieldErrors errors, Set<String> rendered) {
        List<String> unattachable = errors.fieldIds().stream()
                .filter(fieldId -> !rendered.contains(fieldId))
                .toList();
        if (!unattachable.isEmpty()) {
            log.warn("screen '{}' (revision {}) binds no control for rejected field(s) {}; "
                            + "the message cannot be shown and the form cannot be corrected",
                    screen.definition().screenId(), screen.revision().revisionId(), unattachable);
        }
    }

    /** One request's pinned configuration, carried together so no method re-resolves it. */
    private record Screen(ConfigurationRevision revision, ScreenDefinition definition, RecordSchema schema) {
    }
}
