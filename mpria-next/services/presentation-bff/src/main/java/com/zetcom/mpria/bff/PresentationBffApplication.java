package com.zetcom.mpria.bff;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Presentation BFF — the presentation-model layer, in Java, emitting JSON.
 *
 * <p>Owns no data and holds no session. It reads configuration and records from the services that own
 * them and composes a {@code PresentationModel} the client renders without knowing what a record type
 * is (ADR-0003).
 *
 * <p>No {@code @Bean} methods and no {@code @EnableConfigurationProperties} here on purpose. This class
 * is loaded by every test slice, and anything declared on it is instantiated even when the slice has
 * filtered out what it depends on. Wiring lives in ordinary {@code @Configuration} classes, which a
 * slice excludes entirely.
 */
@SpringBootApplication
public class PresentationBffApplication {

    public static void main(String[] args) {
        SpringApplication.run(PresentationBffApplication.class, args);
    }
}
