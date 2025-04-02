package org.jrgss;

import com.badlogic.gdx.Application;

public interface JRGSSApplication extends Application {
    void handlePlatform();

    void fullscreen();

    boolean isFocused();

    void windowed(int var1, int var2);

    void runWithGLContext(Runnable var1);

    void runWithGLContextPriority(Runnable var1);

    void addAudioUpdater(AudioUpdateFunction var1);

    default String getBuildId() {
        return "Unknown";
    }
}
