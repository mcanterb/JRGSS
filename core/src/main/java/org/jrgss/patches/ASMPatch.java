package org.jrgss.patches;

import org.jrgss.ConfigReader;
import org.jrgss.api.Sprite;

/**
 * Patch for Always Sometimes Monsters.
 */
public class ASMPatch implements GamePatch {
    private Sprite vdoglogoSprite = null;
    private Sprite devolverlogoSprite = null;

    @Override
    public boolean shouldEnable(ConfigReader configReader) {
        return configReader.getTitle().equals("Always Sometimes Monsters");
    }

    @Override
    public void onSpriteCreate(Sprite s) {
        if (s.getBitmap() == null || s.getBitmap().getPath() == null) {
            return;
        }
        if (s.getBitmap().getPath().equals("Graphics/Pictures/devolverlogo")) {
            s.setZ(-12);
            devolverlogoSprite = s;
        }
        if (s.getBitmap().getPath().equals("Graphics/Pictures/vdoglogo")) {
            s.setZ(-11);
            vdoglogoSprite = s;
        }
    }

    @Override
    public void onTransitionBegin() {
        if (vdoglogoSprite != null && devolverlogoSprite != null) {
            vdoglogoSprite.dispose();
            devolverlogoSprite.dispose();
            vdoglogoSprite = null;
            devolverlogoSprite = null;
        }
    }

    @Override
    public void onTransitionEnd() {
    }
}
