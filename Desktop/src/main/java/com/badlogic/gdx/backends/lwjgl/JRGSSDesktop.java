package com.badlogic.gdx.backends.lwjgl;

import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl.audio.Ogg;
import com.badlogic.gdx.backends.lwjgl.audio.OpenALAudio;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Clipboard;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ObjectMap;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jrgss.JRGSSApplication;
import org.jrgss.JRGSSApplicationListener;
import org.jrgss.api.*;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;

import java.awt.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author matt
 * @date 7/5/14
 */
@Data
public class JRGSSDesktop extends LwjglApplication implements JRGSSApplication{

    JRGSSApplicationListener jrgssApplicationListener;

    public JRGSSDesktop(JRGSSApplicationListener listener, String title, int width, int height) {
        this(listener, createConfig(title, width, height));
    }

    public JRGSSDesktop(JRGSSApplicationListener listener) {
        this(listener, null, 640, 480);
    }

    public JRGSSDesktop(JRGSSApplicationListener listener, LwjglApplicationConfiguration config) {
        this(listener, config, new LwjglGraphics(config));
    }

    public JRGSSDesktop(JRGSSApplicationListener listener, Canvas canvas) {
        this(listener, new LwjglApplicationConfiguration(), new LwjglGraphics(canvas));
    }

    public JRGSSDesktop(JRGSSApplicationListener listener, LwjglApplicationConfiguration config, Canvas canvas) {
        this(listener, config, new LwjglGraphics(canvas, config));
    }

    public JRGSSDesktop(JRGSSApplicationListener listener, LwjglApplicationConfiguration config, LwjglGraphics graphics) {
        super(listener, config, graphics);
        this.jrgssApplicationListener = listener;
    }

    private static LwjglApplicationConfiguration createConfig(String title, int width, int height) {
        LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
        config.title = title;
        config.width = width;
        config.height = height;
        config.vSyncEnabled = true;
        return config;
    }

    volatile boolean killAudio = false;
    Thread audioUpdateThread = new Thread(new Runnable() {
        @Override
        public void run() {
            while(!killAudio) {
                if (audio != null) {
                    synchronized (org.jrgss.api.Audio.class) {
                        audio.update();

                    }

                }
                try {
                    Thread.sleep(16);
                }catch (Exception e){}
            }
        }
    });

    @Override
    void mainLoop() {
        Array<LifecycleListener> lifecycleListeners = this.lifecycleListeners;
        jrgssApplicationListener.loadScripts();
        try {
            graphics.setupDisplay();
        } catch (LWJGLException e) {
            throw new GdxRuntimeException(e);
        }
        audio.registerMusic("ogx", Ogg.Music.class);
        audio.registerSound("ogx", Ogg.Sound.class);
        listener.create();
        graphics.resize = true;

        lastWidth = graphics.getWidth();
        lastHeight = graphics.getHeight();

        graphics.lastTime = System.nanoTime();
        wasActive = true;
        audioUpdateThread.setDaemon(true);
        audioUpdateThread.start();
        jrgssApplicationListener.getMain().main();

        synchronized (lifecycleListeners) {
            for (LifecycleListener listener : lifecycleListeners) {
                listener.pause();
                listener.dispose();
            }
        }
        killAudio = true;
        while (audioUpdateThread.isAlive()) {
            try{
                Thread.sleep(10);
            }catch (Exception e) {}
        }
        listener.pause();
        listener.dispose();
        Display.destroy();
        if (audio != null) audio.dispose();
        if (graphics.config.forceExit) System.exit(-1);
    }

    int lastWidth;
    int lastHeight;
    boolean wasActive;

    @Override
    public void exit() {
        synchronized (lifecycleListeners) {
            for (LifecycleListener listener : lifecycleListeners) {
                listener.pause();
                listener.dispose();
            }
        }
        listener.pause();
        listener.dispose();
        Display.destroy();
        if (audio != null) audio.dispose();
        System.exit(0);
    }

    @Override
    public void handlePlatform() {
        Display.processMessages();
        if (Display.isCloseRequested()) exit();

        boolean isActive = Display.isActive();
        if (wasActive && !isActive) { // if it's just recently minimized from active state
            wasActive = false;
            synchronized (lifecycleListeners) {
                for (LifecycleListener listener : lifecycleListeners)
                    listener.pause();
            }
            listener.pause();
        }
        if (!wasActive && isActive) { // if it's just recently focused from minimized state
            wasActive = true;
            listener.resume();
            synchronized (lifecycleListeners) {
                for (LifecycleListener listener : lifecycleListeners)
                    listener.resume();
            }
        }

        boolean shouldRender = false;

        if (graphics.canvas != null) {
            int width = graphics.canvas.getWidth();
            int height = graphics.canvas.getHeight();
            if (lastWidth != width || lastHeight != height) {
                lastWidth = width;
                lastHeight = height;
                Gdx.gl.glViewport(0, 0, lastWidth, lastHeight);
                listener.resize(lastWidth, lastHeight);
                shouldRender = true;
            }
        } else {
            graphics.config.x = Display.getX();
            graphics.config.y = Display.getY();
            if (graphics.resize || Display.wasResized() || Display.getWidth() != graphics.config.width
                    || Display.getHeight() != graphics.config.height) {
                graphics.resize = false;
                Gdx.gl.glViewport(0, 0, Display.getWidth(), Display.getHeight());
                graphics.config.width = Display.getWidth();
                graphics.config.height = Display.getHeight();
                if (listener != null) listener.resize(Display.getWidth(), Display.getHeight());
                graphics.requestRendering();
            }
        }

        if (executeRunnables()) shouldRender = true;


        input.update();
        shouldRender |= graphics.shouldRender();
        input.processEvents();
        //if (audio != null) audio.update();

        if (!isActive && graphics.config.backgroundFPS == -1) shouldRender = false;
        int frameRate = isActive ? graphics.config.foregroundFPS : graphics.config.backgroundFPS;
        if (shouldRender) {
            graphics.updateTime();
            listener.render();
            Display.update(false);
        } else {
            // Sleeps to avoid wasting CPU in an empty loop.
            if (frameRate == -1) frameRate = 10;
            if (frameRate == 0) frameRate = graphics.config.backgroundFPS;
            if (frameRate == 0) frameRate = 30;
        }
        if (frameRate > 0) Display.sync(frameRate);
    }
}
