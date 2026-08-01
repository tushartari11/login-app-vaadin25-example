package com.zetcom.mpria.catalog.infrastructure.configclient;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.zetcom.mpria.catalog.application.port.RecordSchemaProvider;
import com.zetcom.mpria.catalog.domain.FieldDefinition;
import com.zetcom.mpria.catalog.domain.FieldType;
import com.zetcom.mpria.catalog.domain.RecordSchema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Fetches compiled record schemas from the configuration service, and caches them.
 *
 * <p>A published configuration revision is immutable, so a fetched schema can be held
 * indefinitely. That is what decouples availability: config-service may be restarted, upgraded or
 * firewalled off without cataloguing stopping.
 *
 * <p><strong>Skeleton scope.</strong> Two things are deliberately simplified here and are Phase-2
 * work, both noted in ARCHITECTURE.md:
 * <ul>
 *   <li>The cache is invalidated by nothing. It should be evicted by the
 *       {@code ConfigurationPublished} event, which is why {@link #evictAll()} already exists.</li>
 *   <li>Authentication relays the caller's token. The target design is the client-credentials
 *       grant with a per-service client and an audience restriction, so a compromised token from
 *       one service cannot be replayed against another.</li>
 * </ul>
 */
@Component
class ConfigServiceSchemaProvider implements RecordSchemaProvider {

    private static final Logger log = LoggerFactory.getLogger(ConfigServiceSchemaProvider.class);

    private final RestClient client;
    private final Map<CacheKey, RecordSchema> cache = new ConcurrentHashMap<>();

    ConfigServiceSchemaProvider(RestClient.Builder builder, ConfigServiceProperties properties) {
        this.client = builder.baseUrl(properties.baseUrl()).build();
    }

    @Override
    public Optional<RecordSchema> schemaFor(UUID tenantId, String recordType) {
        RecordSchema cached = cache.get(new CacheKey(tenantId, recordType));
        if (cached != null) {
            return Optional.of(cached);
        }
        return fetch(tenantId, recordType).map(schema -> {
            cache.put(new CacheKey(tenantId, recordType), schema);
            return schema;
        });
    }

    /** Invalidates every cached schema. Wired to the configuration-published event in Phase 2. */
    void evictAll() {
        cache.clear();
    }

    private Optional<RecordSchema> fetch(UUID tenantId, String recordType) {
        try {
            SchemaPayload payload = client.get()
                    .uri("/api/v1/record-types/{recordType}/schema", recordType)
                    .header("X-Tenant-Id", tenantId.toString())
                    .headers(this::relayCallerToken)
                    .retrieve()
                    .body(SchemaPayload.class);

            return Optional.ofNullable(payload).map(SchemaPayload::toDomain);
        } catch (RestClientException e) {
            log.warn("could not load schema for recordType={} tenantId={}: {}",
                    recordType, tenantId, e.getMessage());
            return Optional.empty();
        }
    }

    private void relayCallerToken(HttpHeaders headers) {
        if (SecurityContextHolder.getContext().getAuthentication()
                instanceof JwtAuthenticationToken authentication
                && authentication.getToken() instanceof Jwt jwt) {
            headers.setBearerAuth(jwt.getTokenValue());
        }
    }

    private record CacheKey(UUID tenantId, String recordType) {
    }

    /** Wire shape of the configuration service's schema endpoint. */
    record SchemaPayload(String recordType,
                         int schemaVersion,
                         String titleFieldId,
                         List<FieldPayload> fields) {

        RecordSchema toDomain() {
            return new RecordSchema(
                    recordType,
                    schemaVersion,
                    fields.stream().map(FieldPayload::toDomain).toList(),
                    titleFieldId);
        }
    }

    record FieldPayload(String id,
                        String type,
                        String label,
                        boolean required,
                        Integer maxLength,
                        Set<String> allowedValues) {

        FieldDefinition toDomain() {
            return new FieldDefinition(
                    id, FieldType.valueOf(type), label, required, maxLength,
                    allowedValues == null ? Set.of() : allowedValues);
        }
    }
}
