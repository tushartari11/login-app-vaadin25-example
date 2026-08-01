package com.zetcom.mpria.bff.domain;

import java.util.Set;

/**
 * The states a catalogue record moves through, as far as presentation is concerned.
 *
 * <p>This service holds no records, so it does not own the transition rules — catalog-service does,
 * and rejects an illegal move. What is needed here is narrower: which actions to offer and whether
 * the form is editable. Modelling only that keeps the duplication honest instead of copying an
 * aggregate's state machine into a second service where it could silently drift.
 */
public enum Lifecycle {

    DRAFT,
    PUBLISHED,
    ARCHIVED;

    private static final Set<Lifecycle> EDITABLE = Set.of(DRAFT, PUBLISHED);

    public boolean allowsEditing() {
        return EDITABLE.contains(this);
    }
}
