package com.zetcom.mpria.config.application;

import java.util.List;

import com.zetcom.mpria.config.application.port.ConfigurationRepository;
import com.zetcom.mpria.config.domain.ComponentDefinition;
import com.zetcom.mpria.config.domain.ComponentRegistry;
import com.zetcom.mpria.config.domain.ConfigurationRevision;
import com.zetcom.mpria.config.domain.RecordTypeDefinition;

import org.springframework.stereotype.Service;

/**
 * Read use cases: everything the rest of the platform resolves from configuration.
 *
 * <p>Every answer here comes from the current <em>published</em> revision and never from a draft.
 * That is the whole availability argument for this service: consumers pin to a revision and cache
 * it, so config-service may be down, upgraded or firewalled off without cataloguing stopping.
 */
@Service
public class PublishedConfigurationService {

    private final ConfigurationRepository repository;
    private final ComponentRegistry registry;

    public PublishedConfigurationService(ConfigurationRepository repository, ComponentRegistry registry) {
        this.repository = repository;
        this.registry = registry;
    }

    public ConfigurationRevision currentRevision() {
        return repository.currentPublished().orElseThrow(RevisionNotFoundException::nothingPublished);
    }

    public RecordTypeDefinition recordType(String recordType) {
        return currentRevision().recordType(recordType)
                .orElseThrow(() -> new RecordTypeNotFoundException(recordType));
    }

    public PublishedScreen screen(String screenId) {
        ConfigurationRevision revision = currentRevision();
        return new PublishedScreen(revision.id(), revision.screen(screenId)
                .orElseThrow(() -> new ScreenNotFoundException(screenId)));
    }

    /** The palette, and the component map every renderer implements. */
    public List<ComponentDefinition> components() {
        return registry.entries();
    }
}
