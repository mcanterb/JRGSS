package org.jrgss.api;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.JrgssBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.google.common.collect.Lists;
import org.jrgss.FileUtil;
import org.jrgss.JRGSSApplication;
import org.jrgss.JRGSSGame;
import org.jrgss.Timer;
import org.jrgss.api.win32.Win32Util;
import org.jrgss.patches.GamePatches;
import org.jrgss.shaders.AlphaBlendingShader;
import org.jrgss.shaders.TransitionShaderProgram;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;
import org.jruby.ext.fiber.ThreadFiber;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.builtin.IRubyObject;

import java.lang.ref.Reference;
import java.nio.Buffer;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@JRubyModule(name = "Graphics")
public class Graphics extends RubyObject {
    public static final Comparator<AbstractRenderable> alternateComparator = (o1, o2) -> {
        if (o1 instanceof Viewport && o1 == o2.getViewport()) {
            return 1;
        } else if (o2 instanceof Viewport && o2 == o1.getViewport()) {
            return -1;
        } else {
            int o2VPZ = o2.getViewport() == null ? 0 : o2.getViewport().getZ();
            int o1VPZ = o1.getViewport() == null ? 0 : o1.getViewport().getZ();
            int ret = Integer.compare(o1VPZ, o2VPZ);
            if (ret != 0) {
                return ret;
            } else {
                ret = Integer.compare(o1.getY(), o2.getY());
                return ret != 0 ? ret : Long.compare(o1.getCreationTime(), o2.getCreationTime());
            }
        }
    };
    private static final JrgssBatch freezeBatch = new JrgssBatch();
    private static final long totalTime = 0L;
    private static final int times = 0;
    private static final List<AbstractRenderable> renderables = Lists.newArrayListWithCapacity(256);
    static Ruby runtime;
    static RubyModule rubyClass;
    private static boolean fullscreen = false;
    private static double scale = 1.0;
    private static double physicalScale = 1.0;
    private static float hiResScale = 1.0F;
    private static int frameCount = 0;
    private static int frameRate = 60;
    private static int brightness = 255;
    private static FrameBuffer backBuffer;
    private static float backBufferOpacity = 0.0F;
    private static float transitionFade = 1.0F;
    private static boolean transitioning = false;
    private static Texture transitionTexture = null;
    private static float vague;
    private static FrameBuffer tempBuffer;
    private static int desiredWidth = 544;
    private static int desiredHeight = 416;
    private static int x = 0;
    private static int y = 0;
    private static int width = 0;
    private static int height = 0;
    private static int lastBlendMode = 32774;
    private static JrgssBatch spritesBatch;
    private static long lastFrame = 0L;
    private static JrgssBatch finalBatch;

    public Graphics(Ruby runtime, RubyClass metaClass, boolean objectSpace) {
        super(runtime, metaClass, objectSpace);
    }

    public Graphics(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass);
    }

    public static void tempBufferBegin() {
        tempBuffer.begin();
    }

    public static void tempBufferEnd() {
        tempBuffer.end();
    }

    public static FrameBuffer getTempBuffer() {
        return tempBuffer;
    }

    public static float getHiResScale() {
        return hiResScale;
    }

    public static int getViewportWidth() {
        return tempBuffer.getWidth();
    }

    public static int getViewportHeight() {
        return tempBuffer.getHeight();
    }

    public static int getWidth() {
        return desiredWidth;
    }

    public static void setWidth(int width) {
        desiredWidth = width;
        DisplayMode mode = Gdx.graphics.getDisplayMode();
        updateDisplayParams(mode.width, mode.height);
    }

    public static int getHeight() {
        return desiredHeight;
    }

    public static void setHeight(int height) {
        desiredHeight = height;
        DisplayMode mode = Gdx.graphics.getDisplayMode();
        updateDisplayParams(mode.width, mode.height);
    }

    public static FrameBuffer checkBufferSize(FrameBuffer buffer) {
        if (buffer == null || buffer.getWidth() != (int) (getWidth() * hiResScale) || buffer.getHeight() != (int) (getHeight() * hiResScale)) {
            if (buffer != null) {
                buffer.dispose();
            }

            buffer = new FrameBuffer(Format.RGB888, (int) (getWidth() * hiResScale), (int) (getHeight() * hiResScale), false);
            buffer.getColorBufferTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
            Gdx.app.log("Graphics", "new buffer @ size = " + buffer.getWidth() + ", " + buffer.getHeight());
        }

        return buffer;
    }

    public static void toggleFullScreen() {
        ((JRGSSApplication) Gdx.app).runWithGLContextPriority(() -> {
            fullscreen = !fullscreen;
            resize_screen(desiredWidth, desiredHeight);
        });
    }

    public static boolean isFullscreen() {
        return fullscreen;
    }

    public static void setFullscreen(final boolean fullscreen) {
        ((JRGSSApplication) Gdx.app).runWithGLContextPriority(() -> {
            if (fullscreen != Graphics.fullscreen) {
                Graphics.fullscreen = fullscreen;
                resize_screen(desiredWidth, desiredHeight);
            }
        });
    }

    public static void init() {
        resize_screen(desiredWidth, desiredHeight);
        AbstractRenderable.startCleanupThread();
    }

    public static void resize_screen(int width, int height) {
        Gdx.app.log("Graphics", "Requested a larger screen size. " + width + "x" + height + ". Let's give it a shot!");
        desiredHeight = height;
        desiredWidth = width;
        if (!fullscreen) {
            ((JRGSSApplication) Gdx.app).windowed(width, height);
            x = 0;
            y = 0;
            Graphics.width = Gdx.graphics.getWidth();
            Graphics.height = Gdx.graphics.getHeight();
            physicalScale = 1.0;
            scale = (float) Gdx.graphics.getWidth() / width;
            hiResScale = (float) height / desiredHeight * (float) scale;
            Gdx.app.log("Graphics", "High Resolution scale set to " + hiResScale);
            Font.resetCache();
            Bitmap.reloadAll();
            tempBuffer = checkBufferSize(tempBuffer);
        } else {
            DisplayMode mode = Gdx.graphics.getDisplayMode();
            Gdx.app.log("Graphics", "Fullscreen resolution is " + mode.width + "x" + mode.height);
            if (!Gdx.graphics.isFullscreen()) {
                ((JRGSSApplication) Gdx.app).fullscreen();
                updateDisplayParams(mode.width, mode.height);
            }

            scale = (float) Gdx.graphics.getWidth() / mode.width;
            physicalScale = (float) Gdx.graphics.getBackBufferWidth() / mode.width;
        }

        if (scale < 0.01) {
            scale = 1.0;
            physicalScale = 1.0;
        }

        Gdx.app.log("Graphics", "We are now at " + getWidth() + "x" + getHeight() + "@(" + scale + "," + physicalScale + ")");
    }

    private static void updateDisplayParams(int width, int height) {
        float ratio = (float) width / height;
        float desiredRatio = (float) desiredWidth / desiredHeight;
        if (ratio < desiredRatio) {
            x = 0;
            Graphics.width = width;
            Graphics.height = (int) (width / desiredRatio);
            y = (height - Graphics.height) / 2;
            hiResScale = (float) width / desiredWidth;
        } else {
            y = 0;
            Graphics.height = height;
            Graphics.width = (int) (height * desiredRatio);
            x = (width - Graphics.width) / 2;
            hiResScale = (float) height / desiredHeight;
        }

        Gdx.app.log("Graphics", "High Resolution scale set to " + hiResScale);
        Font.resetCache();
        Bitmap.reloadAll();
        tempBuffer = checkBufferSize(tempBuffer);
    }

    private static JrgssBatch getSpritesBatch() {
        if (spritesBatch == null) {
            spritesBatch = new JrgssBatch();
            spritesBatch.setProjectionMatrix(JRGSSGame.camera.combined);
        }

        return spritesBatch;
    }

    private static void renderToBoundFramebuffer(JrgssBatch batch) {
        GL20 gl = Gdx.gl;
        Gdx.gl.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        gl.glClear(17408);
        renderables.clear();
        synchronized (AbstractRenderable.class) {
            AbstractRenderable.renderQueue.values().stream().filter(renderableWeakReference -> renderableWeakReference.get() != null).map(Reference::get).forEach(renderables::add);
        }

        Collections.sort(renderables);

        for (AbstractRenderable renderable : renderables) {
            if (renderable instanceof Sprite) {
                JrgssBatch s = getSpritesBatch();
                if (!s.isDrawing()) {
                    AlphaBlendingShader.begin();
                    s.begin();
                }

                renderable.render(s);
            } else {
                if (getSpritesBatch().isDrawing()) {
                    getSpritesBatch().end();
                    AlphaBlendingShader.end();
                    Gdx.gl.glBlendEquation(32774);
                    lastBlendMode = 32774;
                }

                renderable.render(batch);
            }
        }

        Viewport.reset();
        if (getSpritesBatch().isDrawing()) {
            getSpritesBatch().end();
            AlphaBlendingShader.end();
            Gdx.gl.glBlendEquation(32774);
            lastBlendMode = 32774;
        }
    }

    public static void render(JrgssBatch batch) {
        tempBuffer = checkBufferSize(tempBuffer);
        GL20 gl = Gdx.gl;
        Gdx.gl.glClearColor(0.0F, 0.0F, 0.0F, 1.0F);
        gl.glClear(17408);
        if (transitioning) {
            batch.begin();
            batch.draw(backBuffer.getColorBufferTexture(), 0.0F, 0.0F);
            batch.end();
            batch.begin();
            TransitionShaderProgram.get().begin();
            TransitionShaderProgram.get().setFade(transitionFade);
            TransitionShaderProgram.get().setVague(vague);
            batch.draw(transitionTexture, 0.0F, 0.0F, (float) getWidth(), (float) getHeight());
            batch.end();
            batch.setShader(null);
        } else {
            tempBuffer.begin();
            renderToBoundFramebuffer(batch);
            tempBuffer.end();
            if (finalBatch == null) {
                finalBatch = new JrgssBatch();
            }

            OrthographicCamera camera = new OrthographicCamera(10.0F, 10.0F);
            camera.setToOrtho(true);
            camera.update();
            finalBatch.setProjectionMatrix(camera.combined);
            finalBatch.begin();
            finalBatch.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            finalBatch.draw(tempBuffer.getColorBufferTexture(), (float) x, (float) y, (float) (tempBuffer.getWidth() * physicalScale), (float) (tempBuffer.getHeight() * physicalScale));
            if (backBuffer != null) {
                finalBatch.setColor(1.0F, 1.0F, 1.0F, backBufferOpacity);
                finalBatch.draw(backBuffer.getColorBufferTexture(), (float) x, (float) y, (float) (backBuffer.getWidth() * physicalScale), (float) (backBuffer.getHeight() * physicalScale));
            }

            if (brightness != 255) {
                finalBatch.setColor(0.0F, 0.0F, 0.0F, (255.0F - brightness) / 255.0F);
                finalBatch.draw(Sprite.getColorTexture(), (float) x, (float) y, (float) (tempBuffer.getWidth() * physicalScale), (float) (tempBuffer.getHeight() * physicalScale));
                finalBatch.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            }

            finalBatch.end();
        }

        frameCount++;
        lastFrame = System.nanoTime();
    }

    public static void update() {
        if (Input.isFullscreenTriggered()) {
            toggleFullScreen();
        }
        ((JRGSSApplication) Gdx.app).handlePlatform();
        Timer.increment();
    }

    public static void wait(int duration) {
        if (!JRGSSGame.isRenderThread()) {
            final ThreadFiber tf = Ruby.getGlobalRuntime().getCurrentContext().getFiber();
            JRGSSGame.runWithGLContext(new Runnable() {
                @Override
                public void run() {
                    tf.resume(Ruby.getGlobalRuntime().getCurrentContext(), new IRubyObject[0]);
                }
            });
            ThreadFiber.yield(Ruby.getGlobalRuntime().getCurrentContext(), null);
        } else {
            JRGSSGame.runWithGLContext(() -> {
                for (int i = 0; i < duration; i++) {
                    update();
                }
            });
        }
    }

    public static void freeze() {
        TransitionShaderProgram.get();
        Gdx.app.log("Graphics", "Freeze called! Brightness is " + brightness);
        freezeBatch.setProjectionMatrix(JRGSSGame.camera.combined);
        freezeBatch.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        backBuffer = checkBufferSize(backBuffer);
        backBuffer.getColorBufferTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        backBuffer.begin();
        Gdx.gl.glClearColor(0.0F, 0.0F, 0.0F, 1.0F);
        renderToBoundFramebuffer(freezeBatch);
        backBuffer.end();
        backBufferOpacity = 1.0F;
    }

    public static void transition() {
        transition(10);
    }

    public static void transition(int duration) {
        Gdx.app.log("Graphics", "Transition called for duration " + duration);
        System.gc();
        Gdx.app.log("Graphics", "Heap Memory: " + Gdx.app.getJavaHeap() / 1000L + "K , Native Memory: " + Gdx.app.getNativeHeap() / 1000L + "K");
        GamePatches.onTransitionBegin();
        if (backBuffer == null) {
            Gdx.app.log("Graphics", "WARN: Calling transition without a freeze!");
        } else {
            brightness = 255;
            float fadeStep = 1.0F / duration;

            for (int i = duration; i >= 0; i--) {
                backBufferOpacity = i * fadeStep;
                update();
            }

            if (backBuffer != null) {
                backBuffer.dispose();
            }

            backBuffer = null;
            GamePatches.onTransitionEnd();
        }
    }

    public static void transition(int duration, String filename) {
        transition(duration, filename, 60);
    }

    public static void transition(int duration, String filename, int vague) {
        System.gc();
        if (backBuffer == null) {
            Gdx.app.log("Graphics", "WARN: Calling transition without a freeze!");
        } else if (duration < 2) {
            backBuffer.dispose();
            backBuffer = null;
        } else {
            brightness = 255;
            FileHandle file = FileUtil.loadImg(filename);
            Pixmap img = new Pixmap(file);
            transitionTexture = new Texture(img);
            transitioning = true;
            Graphics.vague = vague / 255.0F;
            float step = 1.0F / (duration - 1);

            for (int i = 0; i < duration; i++) {
                transitionFade = i * step;
                update();
            }

            transitioning = false;
            transitionTexture.dispose();
            transitionTexture = null;
            if (backBuffer == null) {
                backBuffer.dispose();
            }

            backBuffer = null;
        }
    }

    public static void fadeout(int duration) {
        Gdx.app.log("Graphics", "Fadeout Called with duration " + duration);
        float fadeStep = 255.0F / duration;

        for (int i = duration; i >= 0; i--) {
            brightness = (int) (i * fadeStep);
            update();
        }

        System.gc();
    }

    public static void fadein(int duration) {
        Gdx.app.log("Graphics", "Fadein Called with duration " + duration);
        float fadeStep = 255.0F / duration;
        Gdx.app.log("Graphics", "fade Step = " + fadeStep);

        for (int i = 0; i <= duration; i++) {
            brightness = (int) (i * fadeStep);
            update();
        }

        System.gc();
    }

    @JRubyMethod(name = "fadein", module = true)
    public static IRubyObject fadein(IRubyObject self, IRubyObject arg) {
        int duration = Win32Util.getInt(arg);
        fadein(duration);
        return Win32Util.rubyNil();
    }

    @JRubyMethod(name = "fadeout", module = true)
    public static IRubyObject fadeout(IRubyObject self, IRubyObject arg) {
        int duration = Win32Util.getInt(arg);
        fadeout(duration);
        return Win32Util.rubyNil();
    }

    @JRubyMethod(name = "update", module = true)
    public static IRubyObject rubyUpdate(IRubyObject self) {
        update();
        return Win32Util.rubyNil();
    }

    @JRubyMethod(name = "wait", module = true)
    public static IRubyObject rubyWait(IRubyObject self, IRubyObject arg) {
        int duration = Win32Util.getInt(arg);
        wait(duration);
        return Win32Util.rubyNil();
    }

    @JRubyMethod(name = "freeze", module = true)
    public static IRubyObject rubyFreeze(IRubyObject self) {
        freeze();
        return Win32Util.rubyNil();
    }

    @JRubyMethod(name = "frame_reset", module = true)
    public static IRubyObject rubyFrameReset(IRubyObject self) {
        frameReset();
        return Win32Util.rubyNil();
    }


    @JRubyMethod(name = "snap_to_bitmap", module = true)
    public static IRubyObject snapToBitmap(IRubyObject self) {
        Gdx.app.log("Graphics", "Called Snap to Bitmap!");
        tempBuffer.begin();
        byte[] bytes = ScreenUtils.getFrameBufferPixels(0, 0, getWidth(), getHeight(), true);
        tempBuffer.end();
        Gdx.app.log("Graphics", "got " + bytes.length + " pixels");
        Pixmap p = new Pixmap(getWidth(), getHeight(), Format.RGBA8888);
        ((Buffer) p.getPixels().put(bytes)).rewind();
        Bitmap b = new Bitmap(p);
        b.setPath("snap_to_bitmap");
        return JavaEmbedUtils.javaToRuby(runtime, b);
    }

    @JRubyMethod(name = "transition", module = true, optional = 3)
    public static IRubyObject transition(IRubyObject self, IRubyObject[] args) {
        int duration = args.length > 0 ? Win32Util.getInt(args[0]) : 10;
        String filename = args.length > 1 ? args[1].asJavaString() : null;
        int vague = args.length > 2 ? Win32Util.getInt(args[2]) : 60;

        if (filename == null) {
            transition(duration);
        } else {
            transition(duration, filename, vague);
        }
        return Win32Util.rubyNil();
    }

    @JRubyMethod(name = "resize_screen", module = true)
    public static IRubyObject rubyResizeScreen(IRubyObject self, IRubyObject arg1, IRubyObject arg2) {
        int newWidth = Win32Util.getInt(arg1);
        int newHeight = Win32Util.getInt(arg2);
        resize_screen(newWidth, newHeight);
        return Win32Util.rubyNil();
    }


    public static void frameReset() {
        frameCount = 0;
    }

    public static int getFrameCount() {
        return frameCount;
    }

    public static int getFrameRate() {
        return frameRate;
    }

    public static void setFrameRate(int frame_rate) {
        Graphics.frameRate = frame_rate;
    }

    public static int getBrightness() {
        return brightness;
    }

    public static void setBrightness(int brightness) {
        Graphics.brightness = brightness;
    }

    @JRubyMethod(name = "brightness=", module = true)
    public static IRubyObject brightnessSet(IRubyObject self, IRubyObject arg) {
        brightness = Win32Util.getInt(arg);
        return Win32Util.rubyNumClamped(brightness);
    }

    @JRubyMethod(name = "brightness", module = true)
    public static IRubyObject brightness(IRubyObject self) {
        return Win32Util.rubyNum(brightness);
    }

    @JRubyMethod(name = "frame_rate=", module = true)
    public static IRubyObject frameRateSet(IRubyObject self, IRubyObject arg) {
        frameRate = Win32Util.getInt(arg);
        return Win32Util.rubyNumClamped(frameRate);
    }

    @JRubyMethod(name = "frame_rate", module = true)
    public static IRubyObject frameRate(IRubyObject self) {
        return Win32Util.rubyNum(frameRate);
    }

    @JRubyMethod(name = "frame_count=", module = true)
    public static IRubyObject frameCountSet(IRubyObject self, IRubyObject arg) {
        frameCount = Win32Util.getInt(arg);
        return Win32Util.rubyNumClamped(frameCount);
    }

    @JRubyMethod(name = "frame_count", module = true)
    public static IRubyObject frameCount(IRubyObject self) {
        return Win32Util.rubyNum(frameCount);
    }

    @JRubyMethod(name = "width", module = true)
    public static IRubyObject rubyWidth(IRubyObject self) {
        return Win32Util.rubyNum(getWidth());
    }


    @JRubyMethod(name = "height", module = true)
    public static IRubyObject rubyHeight(IRubyObject self) {
        return Win32Util.rubyNum(getHeight());
    }

}
