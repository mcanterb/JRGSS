package org.jrgss.patches;

import org.jrgss.ConfigReader;
import org.jrgss.api.Sprite;

import java.util.ArrayList;
import java.util.List;

public class GamePatches {
    private static final GamePatch[] patches = new GamePatch[]{
        new ASMPatch()
    };
    private static final List<GamePatch> installedPatches = new ArrayList<>();

    // Cannot be instantiated.
    private GamePatches() {
    }

    public static void installPatches(ConfigReader ini) {
        for (GamePatch patch : patches) {
            if (patch.shouldEnable(ini)) {
                installedPatches.add(patch);
                // Gdx.app is not initialized yet, so we can't use Gdx.app.log
                System.out.println("[GamePatches] Applied patch: " + patch.getClass().getName());
            }
        }
    }

    public static void onSpriteCreate(Sprite s) {
        for (GamePatch patch : installedPatches) {
            patch.onSpriteCreate(s);
        }
    }

    public static void onTransitionBegin() {
        for (GamePatch patch : installedPatches) {
            patch.onTransitionBegin();
        }
    }

    public static void onTransitionEnd() {
        for (GamePatch patch : installedPatches) {
            patch.onTransitionBegin();
        }
    }
}
