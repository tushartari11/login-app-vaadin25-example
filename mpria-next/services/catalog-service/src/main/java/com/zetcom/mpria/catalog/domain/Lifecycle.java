package com.zetcom.mpria.catalog.domain;

import java.util.Set;

/** The states a catalogue record moves through, and the transitions that are legal between them. */
public enum Lifecycle {

    DRAFT,
    PUBLISHED,
    ARCHIVED;

    private static final Set<Lifecycle> EDITABLE = Set.of(DRAFT, PUBLISHED);

    public boolean allowsEditing() {
        return EDITABLE.contains(this);
    }

    boolean canTransitionTo(Lifecycle target) {
        return switch (this) {
            case DRAFT -> target == PUBLISHED || target == ARCHIVED;
            case PUBLISHED -> target == ARCHIVED || target == DRAFT;
            case ARCHIVED -> false;
        };
    }
}
