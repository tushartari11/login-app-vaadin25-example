package com.zetcom.mpria.bff.application;

/**
 * A service this request depends on could not be reached, or answered in a way this service has no
 * mapping for.
 *
 * <p>Named after the cause rather than the symptom so the failure is not mistaken for a caller
 * error. The BFF composes other services; when one of them is down the honest answer is that the
 * screen cannot be assembled, not that the record is missing or the form invalid.
 */
public class UpstreamUnavailableException extends RuntimeException {

    public UpstreamUnavailableException(String service, String detail) {
        super(service + " could not answer: " + detail);
    }

    public UpstreamUnavailableException(String service, String detail, Throwable cause) {
        super(service + " could not answer: " + detail, cause);
    }
}
