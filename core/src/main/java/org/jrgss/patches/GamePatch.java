package org.jrgss.patches;

import org.jrgss.ConfigReader;
import org.jrgss.api.Sprite;

/**
 * Common interface for game specific fixes. Some games depend on ruby's garbage collection to dispose of images
 * instead of calling dispose. Due to differences between CRuby's GC and Java's, this can sometimes cause graphical
 * bugs. This interface lets us handle these edge cases by manually altering the offending Sprites.
 */
public interface GamePatch {
    boolean shouldEnable(ConfigReader configReader);

    void onSpriteCreate(Sprite s);

    void onTransitionBegin();

    void onTransitionEnd();
}
