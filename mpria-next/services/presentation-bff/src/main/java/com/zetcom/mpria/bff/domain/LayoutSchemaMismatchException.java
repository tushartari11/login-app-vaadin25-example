package com.zetcom.mpria.bff.domain;

/**
 * A configured layout binds a field its record type does not define.
 *
 * <p>This is a configuration defect, not a caller error, and it fails the whole request rather than
 * dropping the offending slot. A half-built form is worse than an error page: the user would submit
 * it, believe the record was complete, and never see the field that was silently omitted.
 * config-service is supposed to reject this at publish time — reaching here means that gate leaked.
 */
public class LayoutSchemaMismatchException extends RuntimeException {

    public LayoutSchemaMismatchException(String screenId, String recordType, String fieldId) {
        super("layout '" + screenId + "' binds field '" + fieldId
                + "' which record type '" + recordType + "' does not define");
    }
}
