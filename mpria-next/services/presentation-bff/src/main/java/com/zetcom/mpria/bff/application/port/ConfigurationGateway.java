package com.zetcom.mpria.bff.application.port;

import com.zetcom.mpria.bff.domain.ConfigurationRevision;
import com.zetcom.mpria.bff.domain.RecordSchema;
import com.zetcom.mpria.bff.domain.ScreenDefinition;

/**
 * Where configuration comes from.
 *
 * <p>Layout and schema are both requested <em>at</em> a revision the caller resolved once. That is
 * not ceremony: without it, a publish landing between the two calls would produce a form whose
 * arrangement and field definitions came from different configurations.
 *
 * <p>The tenant is deliberately absent from every signature. It comes from the caller's token, so an
 * adapter reads it from the security context and no code path exists by which a request could
 * nominate a tenant it was not issued for.
 */
public interface ConfigurationGateway {

    /** The revision currently published, which every lookup in this request will be pinned to. */
    ConfigurationRevision currentRevision();

    ScreenDefinition screen(ConfigurationRevision revision, String screenId);

    RecordSchema schema(ConfigurationRevision revision, String recordType);
}
