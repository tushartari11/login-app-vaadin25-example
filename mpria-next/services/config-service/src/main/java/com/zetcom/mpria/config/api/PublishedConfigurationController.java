package com.zetcom.mpria.config.api;

import java.util.List;

import com.zetcom.mpria.config.api.ConfigDtos.ComponentResponse;
import com.zetcom.mpria.config.api.ConfigDtos.RevisionResponse;
import com.zetcom.mpria.config.api.ConfigDtos.ScreenResponse;
import com.zetcom.mpria.config.application.ConfigurationBundle.RecordTypeBundle;
import com.zetcom.mpria.config.application.PublishedConfigurationService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The published configuration, as every other service reads it.
 *
 * <p>Thin by design: translate, delegate, translate back.
 *
 * <p>Reading needs only an authenticated principal. The operator client resolves a layout on every
 * screen it opens, so requiring an administrative role here would make cataloguing depend on
 * configuration privileges.
 */
@RestController
@RequestMapping("/api/v1")
class PublishedConfigurationController {

    private final PublishedConfigurationService configuration;

    PublishedConfigurationController(PublishedConfigurationService configuration) {
        this.configuration = configuration;
    }

    /**
     * The compiled shape of one record type.
     *
     * <p>Already consumed by {@code catalog-service}, which compiles it into the validator guarding
     * its payload column. The response is the export bundle's record-type shape, so what a
     * consumer parses and what an administrator downloads can never be two different things.
     */
    @GetMapping("/record-types/{recordType}/schema")
    RecordTypeBundle schema(@PathVariable String recordType) {
        return RecordTypeBundle.of(configuration.recordType(recordType));
    }

    @GetMapping("/screens/{screenId}")
    ScreenResponse screen(@PathVariable String screenId) {
        return ScreenResponse.of(configuration.screen(screenId));
    }

    @GetMapping("/revisions/current")
    RevisionResponse currentRevision() {
        return RevisionResponse.of(configuration.currentRevision());
    }

    /** The palette the admin console draws from, and the component map every renderer implements. */
    @GetMapping("/components")
    List<ComponentResponse> components() {
        return configuration.components().stream().map(ComponentResponse::of).toList();
    }
}
