package com.zetcom.mpria.bff.api;

import java.util.UUID;

import com.zetcom.mpria.bff.api.ScreenDtos.SubmissionRequest;
import com.zetcom.mpria.bff.application.PresentationModelAssembler;
import com.zetcom.mpria.bff.application.RecordSubmission;
import com.zetcom.mpria.bff.application.ScreenRoutes;
import com.zetcom.mpria.bff.domain.Lifecycle;
import com.zetcom.mpria.pm.PresentationModel;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The screen API — the only endpoints the web client calls.
 *
 * <p>Thin by design: translate, delegate, return. Every one of these methods answers with a complete
 * {@link PresentationModel}, including the ones that write, because a client that had to re-fetch
 * after a save would have to know how — and knowing how is exactly what {@code actions[].href}
 * exists to avoid.
 */
@RestController
@RequestMapping(ScreenRoutes.SCREENS)
class ScreenController {

    private final PresentationModelAssembler assembler;

    ScreenController(PresentationModelAssembler assembler) {
        this.assembler = assembler;
    }

    /**
     * A blank form.
     *
     * <p>{@code recordType} is redundant — the screen names its own — and is checked rather than
     * trusted, so a client working from a stale link is told it disagrees instead of being served a
     * form for the wrong type.
     */
    @GetMapping("/{screenId}/new")
    PresentationModel blankForm(@PathVariable String screenId,
                               @RequestParam String recordType) {
        return assembler.blankForm(screenId, recordType);
    }

    @GetMapping("/{screenId}/{recordId}")
    PresentationModel populatedForm(@PathVariable String screenId,
                                    @PathVariable UUID recordId) {
        return assembler.existingRecord(screenId, recordId);
    }

    /**
     * Submits a form.
     *
     * @param ifMatch the ETag the form was rendered from, taken from {@code meta.etag}. Optional at
     *                the protocol level so a deliberate force-write remains expressible, but the
     *                reference client always sends it
     */
    @PutMapping("/{screenId}/{recordId}")
    PresentationModel submit(@PathVariable String screenId,
                             @PathVariable UUID recordId,
                             @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
                             @RequestBody SubmissionRequest request) {
        return assembler.submit(screenId, recordId,
                new RecordSubmission(request.identifier(), request.values(), ifMatch));
    }

    /** Creates a record of the type this screen edits. */
    @PostMapping("/{screenId}")
    PresentationModel create(@PathVariable String screenId,
                             @RequestBody SubmissionRequest request) {
        return assembler.create(screenId,
                new RecordSubmission(request.identifier(), request.values(), null));
    }

    /**
     * Moves a record through its lifecycle.
     *
     * <p>This is the path the model's {@code publish} and {@code archive} actions carry in their
     * hrefs, so a client follows the URL it was given and never composes one. Binding {@code target}
     * to the enum means a state that does not exist is refused by the framework, before any use case
     * is entered.
     */
    @PostMapping("/{screenId}/{recordId}/lifecycle/{target}")
    PresentationModel transition(@PathVariable String screenId,
                                 @PathVariable UUID recordId,
                                 @PathVariable Lifecycle target) {
        return assembler.transition(screenId, recordId, target);
    }
}
