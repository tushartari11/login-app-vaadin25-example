package com.zetcom.mpria.catalog.api;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import com.zetcom.mpria.catalog.api.RecordDtos.CreateRecordRequest;
import com.zetcom.mpria.catalog.api.RecordDtos.RecordResponse;
import com.zetcom.mpria.catalog.api.RecordDtos.UpdateRecordRequest;
import com.zetcom.mpria.catalog.application.CatalogPrincipal;
import com.zetcom.mpria.catalog.application.CatalogRecordService;
import com.zetcom.mpria.catalog.domain.CatalogRecord;
import com.zetcom.mpria.catalog.domain.Lifecycle;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
 * The record API.
 *
 * <p>Thin by design: translate, delegate, translate back. Any {@code if} in here that is not about
 * HTTP is a business rule in the wrong layer.
 */
@RestController
@RequestMapping("/api/v1/records")
class CatalogRecordController {

    private final CatalogRecordService catalog;

    CatalogRecordController(CatalogRecordService catalog) {
        this.catalog = catalog;
    }

    @PostMapping
    ResponseEntity<RecordResponse> create(@AuthenticationPrincipal Jwt jwt,
                                          @Valid @RequestBody CreateRecordRequest request) {
        CatalogPrincipal principal = JwtPrincipals.from(jwt);
        CatalogRecord created = catalog.create(
                principal, request.recordType(), request.identifier(), request.data());

        return ResponseEntity
                .created(URI.create("/api/v1/records/" + created.id()))
                .eTag(created.etag())
                .body(RecordResponse.of(created));
    }

    @GetMapping("/{recordId}")
    ResponseEntity<RecordResponse> get(@AuthenticationPrincipal Jwt jwt,
                                       @PathVariable UUID recordId) {
        CatalogRecord record = catalog.get(JwtPrincipals.from(jwt), recordId);
        return ResponseEntity.ok().eTag(record.etag()).body(RecordResponse.of(record));
    }

    @GetMapping
    List<RecordResponse> list(@AuthenticationPrincipal Jwt jwt,
                              @RequestParam String recordType,
                              @RequestParam(defaultValue = "50") int limit) {
        return catalog.listByType(JwtPrincipals.from(jwt), recordType, limit).stream()
                .map(RecordResponse::of)
                .toList();
    }

    /**
     * @param ifMatch the ETag the client last saw. Optional at the protocol level, but the BFF
     *                always sends it — a form submitted against a stale copy must not win silently.
     */
    @PutMapping("/{recordId}")
    ResponseEntity<RecordResponse> update(@AuthenticationPrincipal Jwt jwt,
                                          @PathVariable UUID recordId,
                                          @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
                                          @Valid @RequestBody UpdateRecordRequest request) {
        CatalogRecord updated = catalog.update(
                JwtPrincipals.from(jwt), recordId, request.identifier(), request.data(), ifMatch);

        return ResponseEntity.ok().eTag(updated.etag()).body(RecordResponse.of(updated));
    }

    @PostMapping("/{recordId}/lifecycle/{target}")
    ResponseEntity<RecordResponse> transition(@AuthenticationPrincipal Jwt jwt,
                                              @PathVariable UUID recordId,
                                              @PathVariable Lifecycle target) {
        CatalogRecord moved = catalog.transition(JwtPrincipals.from(jwt), recordId, target);
        return ResponseEntity.ok().eTag(moved.etag()).body(RecordResponse.of(moved));
    }
}
