package com.zetcom.mpria.pm;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Envelope information for a rendered screen.
 *
 * @param revisionId the configuration revision this screen was assembled from. Immutable, so a
 *                   client or an intermediate cache may hold it indefinitely
 * @param etag       optimistic-concurrency token for the underlying record, echoed back on write
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ScreenMeta(String screenId,
                         String revisionId,
                         String recordId,
                         String etag,
                         List<String> capabilities) {

    public ScreenMeta {
        screenId = Contract.requireId(screenId, "screenId");
        revisionId = Contract.requireId(revisionId, "revisionId");
        capabilities = Contract.immutableList(capabilities);
    }
}
