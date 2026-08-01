package com.zetcom.mpria.config.application;

import java.util.List;

import com.zetcom.mpria.config.domain.ConfigurationError;

/**
 * The verdict on an imported bundle.
 *
 * @param draftId the draft the import opened, or {@code null} for a dry run — a dry run writes
 *                nothing, so there is nothing for the caller to go and look at
 */
public record ImportReport(boolean valid,
                           List<ConfigurationError> errors,
                           String draftId,
                           ConfigurationDiff diff) {

    public ImportReport {
        errors = errors == null ? List.of() : List.copyOf(errors);
    }
}
