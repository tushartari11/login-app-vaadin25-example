package com.zetcom.mpria.config.api;

import com.zetcom.mpria.config.api.ConfigDtos.DraftResponse;
import com.zetcom.mpria.config.api.ConfigDtos.ImportReportResponse;
import com.zetcom.mpria.config.api.ConfigDtos.RevisionResponse;
import com.zetcom.mpria.config.application.ConfigurationBundle;
import com.zetcom.mpria.config.application.ConfigurationDraftService;
import com.zetcom.mpria.config.domain.ConfigurationRevision;
import com.zetcom.mpria.pm.Layout;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
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
 * The administration API behind the layout designer.
 *
 * <p>Every route here is gated on {@code mpria-config-admin} by the filter chain, and publication
 * additionally on {@code mpria-config-publisher}.
 */
@RestController
@RequestMapping("/api/v1/admin")
class AdminConfigurationController {

    private final ConfigurationDraftService drafts;

    AdminConfigurationController(ConfigurationDraftService drafts) {
        this.drafts = drafts;
    }

    @GetMapping("/drafts/{draftId}")
    ResponseEntity<DraftResponse> draft(@PathVariable String draftId) {
        return draftResponse(drafts.draft(draftId));
    }

    /**
     * Replaces one screen's layout.
     *
     * @param ifMatch the ETag the designer last saw. Optional at the protocol level; the designer
     *                always sends it, because autosave every few seconds is exactly the situation
     *                in which two administrators overwrite each other
     */
    @PutMapping("/drafts/{draftId}/layouts/{screenId}")
    ResponseEntity<DraftResponse> replaceLayout(
            @PathVariable String draftId,
            @PathVariable String screenId,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @RequestBody Layout layout) {

        return draftResponse(drafts.replaceLayout(draftId, screenId, layout, ifMatch));
    }

    @PostMapping("/drafts/{draftId}/publish")
    RevisionResponse publish(@PathVariable String draftId) {
        return RevisionResponse.of(drafts.publish(draftId));
    }

    @GetMapping("/revisions/{revisionId}/export")
    ConfigurationBundle export(@PathVariable String revisionId) {
        return ConfigurationBundle.of(drafts.revision(revisionId));
    }

    /**
     * Validates an uploaded bundle and reports what it would change.
     *
     * <p>Defaults to a dry run. Writing is the surprising outcome of uploading a file, so it has to
     * be the one the caller asks for explicitly; and even a real import only opens a draft, which
     * still has to pass the publish gate and a human before anyone sees it.
     */
    @PostMapping("/import")
    ImportReportResponse importBundle(@RequestParam(defaultValue = "true") boolean dryRun,
                                      @RequestBody ConfigurationBundle bundle) {
        return ImportReportResponse.of(drafts.importBundle(bundle, dryRun));
    }

    private static ResponseEntity<DraftResponse> draftResponse(ConfigurationRevision draft) {
        return ResponseEntity.ok().eTag(draft.etag()).body(DraftResponse.of(draft));
    }
}
