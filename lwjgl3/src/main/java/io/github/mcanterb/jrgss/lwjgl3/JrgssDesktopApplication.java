package io.github.mcanterb.jrgss.lwjgl3;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.backends.lwjgl3.AbstractJrgssDesktopApplication;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;
import com.badlogic.gdx.backends.lwjgl3.audio.OpenALLwjgl3Audio;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ObjectMap;
import org.jrgss.AudioUpdateFunction;
import org.jrgss.JRGSSApplicationListener;
import org.jrgss.api.Graphics;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;

import java.net.JarURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.Attributes;
import java.util.jar.Manifest;


public class JrgssDesktopApplication extends AbstractJrgssDesktopApplication {
    private boolean running = true;
    private Array<Runnable> runnables = new Array<>();
    private Array<Runnable> executedRunnables = new Array<>();
    private Array<AudioUpdateFunction> audioUpdaters = new Array<>();
    private Array<Lwjgl3Window> windows = null;
    JRGSSApplicationListener jrgssApplicationListener;
    int lastWidth;
    int lastHeight;
    private boolean windowed = true;
    volatile boolean killAudio = false;
    private AtomicBoolean isFocused = new AtomicBoolean(false);
    Thread audioUpdateThread = new Thread(new Runnable() {
        final Array<AudioUpdateFunction> toRemove = new Array<>();

        @Override
        public void run() {
            OpenALLwjgl3Audio audio = (OpenALLwjgl3Audio) getAudio();
            while (!killAudio) {
                if (getAudio() != null) {
                    synchronized (org.jrgss.api.Audio.class) {
                        for (int i = 0; i < audioUpdaters.size; i++) {
                            if (audioUpdaters.get(i).update()) {
                                this.toRemove.add(audioUpdaters.get(i));
                            }
                        }

                        audio.update();

                        audioUpdaters.removeAll(this.toRemove, true);
                        this.toRemove.clear();
                    }
                }

                try {
                    Thread.sleep(50L);
                } catch (Exception var4) {
                }
            }
        }
    });


    public JrgssDesktopApplication(JRGSSApplicationListener jrgssApplicationListener, Lwjgl3ApplicationConfiguration config) {
        super(jrgssApplicationListener, config);
    }

    protected void preLoop() {
        windows = getWindows();
        running = true;
        runnables = new Array<>();
        executedRunnables = new Array<>();
        audioUpdaters = new Array<>();
        windowed = true;
        killAudio = false;
        isFocused = new AtomicBoolean(false);
        audioUpdateThread = new Thread(new Runnable() {
            final Array<AudioUpdateFunction> toRemove = new Array<>();

            @Override
            public void run() {
                OpenALLwjgl3Audio audio = (OpenALLwjgl3Audio) getAudio();
                while (!killAudio) {
                    if (getAudio() != null) {
                        synchronized (org.jrgss.api.Audio.class) {
                            for (int i = 0; i < audioUpdaters.size; i++) {
                                if (audioUpdaters.get(i).update()) {
                                    this.toRemove.add(audioUpdaters.get(i));
                                }
                            }

                            audio.update();

                            audioUpdaters.removeAll(this.toRemove, true);
                            this.toRemove.clear();
                        }
                    }

                    try {
                        Thread.sleep(50L);
                    } catch (Exception var4) {
                    }
                }
            }
        });
        jrgssApplicationListener = (JRGSSApplicationListener) getCurrentWindow().getListener();

        handlePlatform();
        jrgssApplicationListener.loadSplashScreen();
        GLFW.glfwSwapBuffers(this.getCurrentWindow().getWindowHandle());
        jrgssApplicationListener.loadScripts();
    }

    @Override
    protected void loop() {
        preLoop();
        try {
            Graphics.init();
        } catch (Exception e) {
            throw new GdxRuntimeException(e);
        }
        this.audioUpdateThread.setDaemon(true);
        this.audioUpdateThread.start();

        try {
            this.jrgssApplicationListener.getMain().main();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void fullscreen() {
        long monitor = GLFW.glfwGetPrimaryMonitor();
        GLFWVidMode mode = GLFW.glfwGetVideoMode(monitor);
        Gdx.app.log("JrgssDesktopApplication", "Switching to " + mode);
        this.getGraphics().setFullscreenMode(this.getGraphics().getDisplayMode());
        GLFW.glfwSetWindowPos(getCurrentWindow().getWindowHandle(), 0, 0);
        GLFW.glfwSetInputMode(getCurrentWindow().getWindowHandle(), 208897, 212994);
        GLFW.glfwSwapInterval(1);
        this.windowed = false;
    }

    @Override
    public void windowed(int width, int height) {
        this.windowed = true;
        if (this.getGraphics().isFullscreen() || width != this.getGraphics().getWidth() || height != this.getGraphics().getHeight()) {
            this.getGraphics().setWindowedMode(width, height);
            GLFW.glfwSetInputMode(getCurrentWindow().getWindowHandle(), 208897, 212993);
            GLFW.glfwSwapInterval(1);
        }
    }

    @Override
    public void runWithGLContext(Runnable runnable) {
        this.runnables.add(runnable);
    }

    @Override
    public void runWithGLContextPriority(Runnable runnable) {
        this.runnables.insert(0, runnable);
    }

    @Override
    public void addAudioUpdater(AudioUpdateFunction function) {
        synchronized (org.jrgss.api.Audio.class) {
            this.audioUpdaters.add(function);
        }
    }
    @Override
    public void postRunnable (Runnable runnable) {
        synchronized (this) {
            runnables.add(runnable);
        }
    }

    @Override
    public com.badlogic.gdx.Graphics getGraphics () {
        return getWindowGraphics(getCurrentWindow());
    }

    @Override
    public void handlePlatform() {
        if (windows.size != 1) {
            Gdx.app.error("JRGSSDesktopApplication", "JRGSS Applications cannot have more than 1 window!");
        }
        Lwjgl3Window window = windows.first();
        synchronized (this) {
            this.executedRunnables.addAll(this.runnables);
            this.runnables.clear();
        }

        for (Runnable runnable : this.executedRunnables) {
            if (runnable != null) {
                runnable.run();
            } else {
                Gdx.app.log("JRGSSDesktop", "runWithGLContextRunnable was null.");
            }
        }
        this.executedRunnables.clear();


        if (running) {
            Gdx.graphics = getWindowGraphics(window);
            Gdx.gl30 = Gdx.graphics.getGL30();
            Gdx.gl20 = Gdx.gl30 != null ? Gdx.gl30 : Gdx.graphics.getGL20();
            Gdx.gl = Gdx.gl30 != null ? Gdx.gl30 : Gdx.gl20;
            Gdx.input = getWindowInput(window);
            GLFW.glfwMakeContextCurrent(window.getWindowHandle());
            synchronized (this) {
                int width = Gdx.graphics.getWidth();
                int height = Gdx.graphics.getHeight();
                if (this.lastWidth != width || this.lastHeight != height) {
                    this.lastWidth = width;
                    this.lastHeight = height;
                    Gdx.gl.glViewport(0, 0, this.lastWidth, this.lastHeight);
                }

                updateWindow(window);

            }
            GLFW.glfwPollEvents();

            boolean shouldRequestRendering;
            synchronized (runnables) {
                shouldRequestRendering = runnables.size > 0;
                executedRunnables.clear();
                executedRunnables.addAll(runnables);
                runnables.clear();
            }
            for (Runnable runnable : executedRunnables) {
                runnable.run();
            }
            if (shouldRequestRendering) {
                // Must follow Runnables execution so changes done by Runnables are reflected
                // in the following render.
                requestRendering(window);
            }

            if (shouldClose(window)) {
                windows.removeValue(window, false);
            }

            sync();
        }
    }

    @Override
    public boolean isFocused() {
        return isFocused.get();
    }

    public int getLastWidth() {
        return this.lastWidth;
    }

    public int getLastHeight() {
        return this.lastHeight;
    }
    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    @Override
    public String getBuildId() {
        try {
            URL jarURL = this.getClass().getResource("/org/jrgss/Desktop.class");
            JarURLConnection jurlConn = (JarURLConnection) jarURL.openConnection();
            Manifest mf = jurlConn.getManifest();
            Attributes attributes = mf.getMainAttributes();
            if (attributes != null) {
                String val = attributes.getValue("JRGSS-Build");
                if (val != null) {
                    return val;
                }
            }
        } catch (Exception var6) {
        }

        return "???";
    }

    public JRGSSApplicationListener getJrgssApplicationListener() {
        return this.jrgssApplicationListener;
    }

    public void setJrgssApplicationListener(JRGSSApplicationListener jrgssApplicationListener) {
        this.jrgssApplicationListener = jrgssApplicationListener;
    }

    public Lwjgl3Window getCurrentWindow() {
        return this.getWindows().first();
    }

    public boolean isRunning() {
        return this.running;
    }

    public Array<Runnable> getRunnables() {
        return this.runnables;
    }

    public Array<Runnable> getExecutedRunnables() {
        return this.executedRunnables;
    }
}

