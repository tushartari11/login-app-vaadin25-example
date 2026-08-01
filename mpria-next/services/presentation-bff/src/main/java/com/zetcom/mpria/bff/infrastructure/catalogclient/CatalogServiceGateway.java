package com.zetcom.mpria.bff.infrastructure.catalogclient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.zetcom.mpria.bff.application.RecordSubmission;
import com.zetcom.mpria.bff.application.UpstreamUnavailableException;
import com.zetcom.mpria.bff.application.port.CatalogGateway;
import com.zetcom.mpria.bff.domain.FieldErrors;
import com.zetcom.mpria.bff.domain.IllegalLifecycleTransitionException;
import com.zetcom.mpria.bff.domain.Lifecycle;
import com.zetcom.mpria.bff.domain.RecordNotFoundException;
import com.zetcom.mpria.bff.domain.RecordSnapshot;
import com.zetcom.mpria.bff.domain.StaleRecordException;
import com.zetcom.mpria.bff.infrastructure.security.CallerCredentials;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Reads and writes records through catalog-service.
 *
 * <p>Nothing is cached. Records are mutable and this is a read-through view of somebody else's
 * transactional core; a stale form is worse than a slow one.
 *
 * <p>The one translation that matters lives in {@link #write}: a 422 becomes a
 * {@link WriteResult.Rejected} carrying the field errors, not an exception. The whole reason the BFF
 * can answer a failed submission with a re-rendered form is that this method does not throw.
 *
 * <p><strong>Skeleton scope.</strong> Calls relay the caller's own token; see
 * {@link CallerCredentials} for why, and for what replaces it in Phase 1.
 */
@Component
class CatalogServiceGateway implements CatalogGateway {

    private static final String SERVICE = "catalog-service";
    private static final String RECORDS = "/api/v1/records";

    /** The RFC 9457 problem type catalog-service uses for a move its state machine forbids. */
    private static final String ILLEGAL_TRANSITION = "illegal-transition";

    private final RestClient client;

    CatalogServiceGateway(RestClient.Builder builder, CatalogServiceProperties properties) {
        this.client = builder.baseUrl(properties.baseUrl()).build();
    }

    @Override
    public RecordSnapshot get(UUID recordId) {
        try {
            return client.get()
                    .uri(RECORDS + "/" + recordId)
                    .headers(CallerCredentials::relayBearerToken)
                    .exchange((request, response) -> {
                        rejectFailure(response.getStatusCode(), recordId, null);
                        return required(response.bodyTo(RecordPayload.class), recordId).toDomain();
                    });
        } catch (RestClientException e) {
            throw new UpstreamUnavailableException(SERVICE, "could not read record " + recordId, e);
        }
    }

    @Override
    public WriteResult create(String recordType, RecordSubmission submission) {
        RestClient.RequestBodySpec request = client.post()
                .uri(RECORDS)
                .contentType(MediaType.APPLICATION_JSON);

        return write(request.body(
                new CreateRequest(recordType, submission.identifier(), submission.data())), null, null);
    }

    @Override
    public WriteResult update(UUID recordId, RecordSubmission submission) {
        RestClient.RequestBodySpec request = client.put()
                .uri(RECORDS + "/" + recordId)
                .contentType(MediaType.APPLICATION_JSON);
        if (submission.etag() != null) {
            // The BFF always sends the precondition: a form submitted against a copy that has since
            // been overwritten must lose, not win silently.
            request = request.header(HttpHeaders.IF_MATCH, submission.etag());
        }

        return write(request.body(
                new UpdateRequest(submission.identifier(), submission.data())), recordId, submission.etag());
    }

    @Override
    public RecordSnapshot transition(UUID recordId, Lifecycle target) {
        try {
            return client.post()
                    .uri(RECORDS + "/" + recordId + "/lifecycle/" + target.name())
                    .headers(CallerCredentials::relayBearerToken)
                    .exchange((request, response) -> {
                        if (response.getStatusCode().value() == HttpStatus.CONFLICT.value()) {
                            throw conflict(response, recordId, target);
                        }
                        rejectFailure(response.getStatusCode(), recordId, null);
                        return required(response.bodyTo(RecordPayload.class), recordId).toDomain();
                    });
        } catch (RestClientException e) {
            throw new UpstreamUnavailableException(
                    SERVICE, "could not move record " + recordId + " to " + target, e);
        }
    }

    /**
     * Tells the two meanings of a 409 apart.
     *
     * <p>catalog-service answers both an illegal transition and a lost write race with a Conflict, and
     * they call for different things from the user: one move is simply not permitted, the other needs a
     * reload because somebody else won. The status cannot distinguish them, so the problem document's
     * {@code type} does — which is what RFC 9457 types are for.
     */
    private static RuntimeException conflict(RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse response,
                                             UUID recordId,
                                             Lifecycle target) {
        ProblemPayload problem = response.bodyTo(ProblemPayload.class);
        if (problem != null && problem.isProblem(ILLEGAL_TRANSITION)) {
            return new IllegalLifecycleTransitionException(recordId, target);
        }
        // Unrecognised or absent: the safe reading is that the record moved, which sends the caller
        // back to read it rather than telling them an allowed action is forbidden.
        return new StaleRecordException(recordId, null);
    }

    /**
     * Performs a write and classifies the answer.
     *
     * <p>{@code exchange} rather than {@code retrieve} for one reason: the default status handler
     * would turn the 422 into an exception, and the 422 is not a failure here — it is the form coming
     * back with messages on it.
     *
     * @param recordId the record being written, or {@code null} for a create
     */
    private WriteResult write(RestClient.RequestBodySpec request, UUID recordId, String etag) {
        try {
            return request
                    .headers(CallerCredentials::relayBearerToken)
                    .exchange((httpRequest, response) -> {
                        HttpStatusCode status = response.getStatusCode();
                        if (status.value() == HttpStatus.UNPROCESSABLE_CONTENT.value()) {
                            return new WriteResult.Rejected(
                                    fieldErrors(response.bodyTo(ProblemPayload.class)));
                        }
                        rejectFailure(status, recordId, etag);
                        return new WriteResult.Stored(
                                required(response.bodyTo(RecordPayload.class), recordId).toDomain());
                    });
        } catch (RestClientException e) {
            throw new UpstreamUnavailableException(SERVICE, "could not write record " + recordId, e);
        }
    }

    /**
     * Translates every status this service does not handle as data.
     *
     * <p>A 412 is deliberately not folded into a rejection: it means somebody else won the race, and
     * re-rendering the caller's values with a message beside them would invite them to submit again
     * and discard the winner's edit. A 409 is the same situation detected one layer deeper, in the
     * database, so it is reported the same way.
     *
     * @param recordId {@code null} for a create, where neither a missing record nor a stale version
     *                 is a meaningful answer and any error is therefore an upstream fault
     */
    private static void rejectFailure(HttpStatusCode status, UUID recordId, String etag) {
        if (recordId != null) {
            if (status.value() == HttpStatus.NOT_FOUND.value()) {
                throw new RecordNotFoundException(recordId);
            }
            if (status.value() == HttpStatus.PRECONDITION_FAILED.value()
                    || status.value() == HttpStatus.CONFLICT.value()) {
                throw new StaleRecordException(recordId, etag);
            }
        }
        if (status.isError()) {
            throw new UpstreamUnavailableException(SERVICE, "HTTP " + status.value());
        }
    }

    private static RecordPayload required(RecordPayload payload, UUID recordId) {
        if (payload == null) {
            throw new UpstreamUnavailableException(SERVICE, "empty record body for " + recordId);
        }
        return payload;
    }

    /**
     * A problem document with no {@code errors} member still rejected the write. Reporting no field
     * errors is honest — the form comes back unchanged rather than claiming a success that never
     * happened.
     */
    private static FieldErrors fieldErrors(ProblemPayload problem) {
        if (problem == null || problem.errors() == null) {
            return FieldErrors.none();
        }
        return new FieldErrors(problem.errors());
    }

    /** Request shape of {@code POST /api/v1/records}. */
    record CreateRequest(String recordType, String identifier, Map<String, Object> data) {
    }

    /** Request shape of {@code PUT /api/v1/records/{id}} — id in the path, version in {@code If-Match}. */
    record UpdateRequest(String identifier, Map<String, Object> data) {
    }

    /**
     * The members of a record response this service reads.
     *
     * <p>Unknown properties are ignored on purpose. catalog-service owns this shape and may add to
     * it; a BFF that failed on an added audit field would couple two deployments together for no
     * gain. Timestamps and authorship are among what is skipped: presentation does not reason about
     * them, and a field this service does not read is a field it cannot accidentally depend on.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record RecordPayload(UUID id,
                         String recordType,
                         int schemaVersion,
                         String identifier,
                         String title,
                         String lifecycle,
                         Map<String, Object> data,
                         String etag) {

        RecordSnapshot toDomain() {
            return new RecordSnapshot(id, recordType, schemaVersion, identifier, title,
                    Lifecycle.valueOf(lifecycle), data, etag);
        }
    }

    /**
     * The members of an RFC 9457 problem document this service acts on.
     *
     * @param type   the problem identifier, which is what distinguishes two failures sharing a status
     * @param errors field id to messages, as catalog-service's validation handler writes it
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record ProblemPayload(String type, Map<String, List<String>> errors) {

        /** Matches on the last path segment, so the documentation host can move without breaking this. */
        boolean isProblem(String name) {
            return type != null && type.endsWith("/" + name);
        }
    }
}
