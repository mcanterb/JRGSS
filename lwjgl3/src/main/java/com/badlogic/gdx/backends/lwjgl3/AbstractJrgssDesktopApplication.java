package com.badlogic.gdx.backends.lwjgl3;

import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.utils.Array;
import org.jrgss.JRGSSApplication;
import org.jrgss.JRGSSApplicationListener;

abstract public class AbstractJrgssDesktopApplication  extends Lwjgl3Application implements JRGSSApplication {
    private static final int TARGET_FPS = 60;
    private static final Sync sync = new Sync();
    public AbstractJrgssDesktopApplication(JRGSSApplicationListener listener, Lwjgl3ApplicationConfiguration config) {
        super(listener, config);
    }

    protected Array<Lwjgl3Window> getWindows() {
        return windows;
    }

    protected void updateWindow(Lwjgl3Window window) {
        window.update();
    }

    protected Graphics getWindowGraphics(Lwjgl3Window window) {
        return window.getGraphics();
    }

    protected Input getWindowInput(Lwjgl3Window window) {
        return window.getInput();
    }

    protected void requestRendering(Lwjgl3Window window) {
        window.requestRendering();
    }

    protected boolean shouldClose(Lwjgl3Window window) {
        return window.shouldClose();
    }

    protected void sync() {
        sync.sync(TARGET_FPS);
    }
}
