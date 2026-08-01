package com.zetcom.mpria.bff.infrastructure.configclient;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.zetcom.mpria.bff.application.UpstreamUnavailableException;
import com.zetcom.mpria.bff.application.port.ConfigurationGateway;
import com.zetcom.mpria.bff.domain.ConfigurationRevision;
import com.zetcom.mpria.bff.domain.FieldDefinition;
import com.zetcom.mpria.bff.domain.RecordSchema;
import com.zetcom.mpria.bff.domain.RecordTypeNotConfiguredException;
import com.zetcom.mpria.bff.domain.ScreenDefinition;
import com.zetcom.mpria.bff.domain.ScreenNotFoundException;
import com.zetcom.mpria.bff.infrastructure.security.CallerCredentials;
import com.zetcom.mpria.pm.Layout;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Reads configuration from config-service, and caches it.
 *
 * <p>A published revision is immutable, so a layout or schema fetched at that revision can be held
 * for as long as the process lives. That is what decouples availability in the direction the
 * architecture requires: config-service may be restarted, upgraded or firewalled off and screens keep
 * rendering from the last revision seen (ARCHITECTURE.md §8).
 *
 * <p><strong>Skeleton scope.</strong> The cache is invalidated by nothing. It should be evicted by
 * the {@code ConfigurationPublished} event, which is why {@link #evictAll()} already exists; until
 * then a publish reaches a running BFF only through {@link #currentRevision()} changing, which
 * changes every cache key anyway.
 */
@Component
class ConfigServiceGateway implements ConfigurationGateway {

    private static final String SERVICE = "config-service";
    private static final String TENANT_HEADER = "X-Tenant-Id";

    private final RestClient client;
    private final Map<CacheKey, ScreenDefinition> screens = new ConcurrentHashMap<>();
    private final Map<CacheKey, RecordSchema> schemas = new ConcurrentHashMap<>();

    ConfigServiceGateway(RestClient.Builder builder, ConfigServiceProperties properties) {
        this.client = builder.baseUrl(properties.baseUrl()).build();
    }

    @Override
    public ConfigurationRevision currentRevision() {
        RevisionPayload payload = get("/api/v1/revisions/current", RevisionPayload.class,
                () -> new UpstreamUnavailableException(SERVICE, "no revision is published"));
        return new ConfigurationRevision(payload.revisionId());
    }

    @Override
    public ScreenDefinition screen(ConfigurationRevision revision, String screenId) {
        return screens.computeIfAbsent(key(revision, screenId), key -> {
            ScreenPayload payload = get("/api/v1/screens/" + screenId, ScreenPayload.class,
                    () -> new ScreenNotFoundException(screenId, revision.revisionId()));
            return payload.toDomain();
        });
    }

    @Override
    public RecordSchema schema(ConfigurationRevision revision, String recordType) {
        return schemas.computeIfAbsent(key(revision, recordType), key -> {
            SchemaPayload payload = get(
                    "/api/v1/record-types/" + recordType + "/schema", SchemaPayload.class,
                    () -> new RecordTypeNotConfiguredException(recordType, revision.revisionId()));
            return payload.toDomain();
        });
    }

    /** Invalidates every cached revision. Wired to the configuration-published event in Phase 2. */
    void evictAll() {
        screens.clear();
        schemas.clear();
    }

    /**
     * One GET, with the caller's identity attached and upstream statuses translated.
     *
     * <p>{@code exchange} rather than {@code retrieve}: the default status handler turns every 4xx
     * into the same exception, and a 404 from config-service means something specific — the revision
     * does not define what was asked for — that must not be reported as an outage.
     *
     * @param onNotFound what a 404 means for this particular resource
     */
    private <T> T get(String path, Class<T> payloadType, Supplier<RuntimeException> onNotFound) {
        try {
            return client.get()
                    .uri(path)
                    .header(TENANT_HEADER, CallerCredentials.tenantId().toString())
                    .headers(CallerCredentials::relayBearerToken)
                    .exchange((request, response) -> {
                        HttpStatusCode status = response.getStatusCode();
                        if (status.value() == HttpStatus.NOT_FOUND.value()) {
                            throw onNotFound.get();
                        }
                        if (status.isError()) {
                            throw new UpstreamUnavailableException(SERVICE, "HTTP " + status.value());
                        }
                        T payload = response.bodyTo(payloadType);
                        if (payload == null) {
                            throw new UpstreamUnavailableException(SERVICE, "empty response from " + path);
                        }
                        return payload;
                    });
        } catch (RestClientException e) {
            throw new UpstreamUnavailableException(SERVICE, "request to " + path + " failed", e);
        }
    }

    private static CacheKey key(ConfigurationRevision revision, String resourceId) {
        // The tenant is part of the key: two customers on one revision have different configurations.
        return new CacheKey(revision.revisionId(), CallerCredentials.tenantId(), resourceId);
    }

    private record CacheKey(String revisionId, UUID tenantId, String resourceId) {
    }

    /**
     * The members of each configuration payload this service reads.
     *
     * <p>Unknown properties are ignored throughout. config-service owns these shapes and the admin
     * application will grow them; a BFF that failed on an added member would make every configuration
     * feature a coordinated release. A publication timestamp, a status and a field's {@code maxLength}
     * are all skipped for the same reason — nothing here reasons about them, and validation belongs to
     * catalog-service.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record RevisionPayload(String revisionId) {
    }

    /**
     * Wire shape of {@code GET /api/v1/screens/{screenId}}.
     *
     * <p>{@code layout} is deserialised as the real {@code pm-contract} type, so the layout an
     * administrator saved is the layout the renderer receives. The payload also repeats the revision it
     * came from; the caller has already pinned one, and a disagreement would mean a publish landed
     * mid-request — which the next request resolves on its own.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record ScreenPayload(String screenId, String recordType, Layout layout) {

        ScreenDefinition toDomain() {
            return new ScreenDefinition(screenId, recordType, layout);
        }
    }

    /** Wire shape of {@code GET /api/v1/record-types/{recordType}/schema}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record SchemaPayload(String recordType,
                         int schemaVersion,
                         String titleFieldId,
                         List<FieldPayload> fields) {

        RecordSchema toDomain() {
            return new RecordSchema(recordType, schemaVersion, titleFieldId,
                    fields == null ? List.of() : fields.stream().map(FieldPayload::toDomain).toList());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FieldPayload(String id,
                        String type,
                        String label,
                        boolean required,
                        List<String> allowedValues) {

        FieldDefinition toDomain() {
            return new FieldDefinition(id, type, label, required, allowedValues);
        }
    }
}
