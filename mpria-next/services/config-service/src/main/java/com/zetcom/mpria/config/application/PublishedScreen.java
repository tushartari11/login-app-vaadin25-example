package com.zetcom.mpria.config.application;

import com.zetcom.mpria.config.domain.ScreenLayout;
import com.zetcom.mpria.pm.Layout;

/**
 * A configured screen together with the revision it was read from.
 *
 * <p>The revision id travels with the layout because that is what makes the layout cacheable: a
 * published revision never changes, so a consumer that knows which one it holds can keep it for
 * ever and never call this service again.
 */
public record PublishedScreen(String revisionId, ScreenLayout screen) {

    /** Convenience for callers that want the layout tree rather than the wrapper. */
    public Layout layout() {
        return screen.layout();
    }
}
