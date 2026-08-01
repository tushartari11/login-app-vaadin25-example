package com.zetcom.mpria.catalog.api;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.zetcom.mpria.catalog.domain.CatalogRecord;

/**
 * Wire types for the record API.
 *
 * <p>Bean-validation annotations here cover only structural concerns — presence and length of the
 * spine attributes. Everything about the {@code data} payload is validated against the customer's
 * configured schema in the domain, because that is where the rules are configured.
 */
final class RecordDtos {

    private RecordDtos() {
    }

    record CreateRecordRequest(@NotBlank @Size(max = 100) String recordType,
                               @Size(max = 255) String identifier,
                               Map<String, Object> data) {
    }

    record UpdateRecordRequest(@Size(max = 255) String identifier,
                               Map<String, Object> data) {
    }

    record RecordResponse(UUID id,
                          String recordType,
                          int schemaVersion,
                          String identifier,
                          String title,
                          String lifecycle,
                          Map<String, Object> data,
                          String etag,
                          Instant createdAt,
                          String createdBy,
                          Instant updatedAt,
                          String updatedBy) {

        static RecordResponse of(CatalogRecord record) {
            return new RecordResponse(
                    record.id(),
                    record.recordType(),
                    record.schemaVersion(),
                    record.identifier(),
                    record.title(),
                    record.lifecycle().name(),
                    record.data(),
                    record.etag(),
                    record.createdAt(),
                    record.createdBy(),
                    record.updatedAt(),
                    record.updatedBy());
        }
    }
}
