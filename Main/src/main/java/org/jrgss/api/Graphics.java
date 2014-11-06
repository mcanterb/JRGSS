package org.jrgss.api;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.ScreenUtils;
import lombok.Getter;
import lombok.Setter;
import org.jrgss.FileUtil;
import org.jrgss.JRGSSApplication;
import org.jrgss.JRGSSGame;
import org.jrgss.shaders.TransitionShaderProgram;
import org.jruby.Ruby;
import org.jruby.ext.fiber.ThreadFiber;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by matty on 6/27/14.
 */
public class Graphics {
    public static final Comparator<AbstractRenderable> alternateComparator = new Comparator<AbstractRenderable>() {
        @Override
        public int compare(AbstractRenderable o1, AbstractRenderable o2) {
            if (o2 instanceof Window && !(o1 instanceof Window)) {
                return -1;
            }
            if ((o1 instanceof Window) && !(o2 instanceof Window)) {
                return 1;
            }
            int ret = Integer.compare(o1.getZ(), o2.getZ());
            if (ret != 0) return ret;
            ret = Integer.compare(o1.getY(), o2.getY());
            if (ret != 0) return ret;
            ret = Long.compare(o1.getCreationTime(), o2.getCreationTime());
            return ret;
        }
    };
    @Getter
    public static int frame_count = 0;
    @Getter
    @Setter
    public static int frame_rate = 60;
    @Getter
    @Setter
    public static int brightness = 255;
    public static Texture backBuffer;
    public static float backBufferOpacity = 0;
    public static float transitionFade = 1f;
    public static boolean transitioning = false;
    public static Texture transitionTexture = null;
    public static float vague;

    public static int getWidth() {
        if (Gdx.graphics == null) {
            return 544;
        }
        return Gdx.graphics.getWidth();
    }

    public static int getHeight() {
        if (Gdx.graphics == null) {
            return 416;
        }
        return Gdx.graphics.getHeight();
    }

    public static void resize_screen(int width, int height) {
        Gdx.app.log("Graphics", "Requested a larger screen size. " + width + "x" + height + ". Let's give it a shot!");
    }

    public static void render(SpriteBatch batch) {
        GL20 gl = Gdx.gl;
        gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_STENCIL_BUFFER_BIT);
        if (transitioning) {


            batch.begin();
            batch.draw(backBuffer, 0, 0);
            batch.end();
            batch.setShader(TransitionShaderProgram.get());
            batch.begin();
            TransitionShaderProgram.get().begin();
            TransitionShaderProgram.get().setFade(transitionFade);
            TransitionShaderProgram.get().setVague(vague);
            batch.draw(transitionTexture, 0, 0, getWidth(), getHeight());
            batch.end();
            batch.setShader(null);

        } else {

            ArrayList<AbstractRenderable> renderables = new ArrayList<>();
            ArrayList<AbstractRenderable> viewportLessRenderables = new ArrayList<>();
            for (AbstractRenderable renderable : AbstractRenderable.renderQueue.values()) {
                if (renderable.getViewport() == null) {
                    viewportLessRenderables.add(renderable);
                } else {
                    renderables.add(renderable);
                }
            }
            Collections.sort(renderables);
            Collections.sort(viewportLessRenderables, alternateComparator);
            Iterator<AbstractRenderable> iter;
            for (AbstractRenderable renderable : renderables) {
                iter = viewportLessRenderables.iterator();
                AbstractRenderable r;
                while (iter.hasNext() && alternateComparator.compare((r = iter.next()), renderable) < 0) {
                    r.render(batch);
                    iter.remove();
                }
                renderable.render(batch);
            }
            for (AbstractRenderable renderable : viewportLessRenderables) {
                renderable.render(batch);
            }
            batch.begin();
            if (backBuffer != null) {
                batch.setColor(1f, 1f, 1f, backBufferOpacity);
                batch.draw(backBuffer, 0, 0);
            }

            if (brightness != 255) {
                batch.setColor(0f, 0f, 0f, (255f - brightness) / 255f);
                batch.draw(Sprite.getColorTexture(), 0f, 0f, getWidth(), getHeight());
                batch.setColor(1f, 1f, 1f, 1f);
            }
            batch.end();
        }
        frame_count++;

    }

    public static void update() {

        ((JRGSSApplication) Gdx.app).handlePlatform();
    }

    public static void wait(final int duration) {
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
            for (int i = 0; i < duration; i++) {
                update();
            }
        }
    }

    public static void freeze() {
        Gdx.app.log("Graphics", "Freeze called! Brightness is " + brightness);
        SpriteBatch batch = new SpriteBatch();
        batch.setProjectionMatrix(JRGSSGame.camera.combined);
        batch.setColor(1f, 1f, 1f, 1f);
        FrameBuffer frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, getWidth(), getHeight(), false);
        frameBuffer.begin();
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        render(batch);
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f);
        byte[] bytes = ScreenUtils.getFrameBufferPixels(false);
        frameBuffer.end();
        Pixmap p = new Pixmap(getWidth(), getHeight(), Pixmap.Format.RGBA8888);
        p.getPixels().put(bytes).rewind();
        backBuffer = new Texture(p);
        backBufferOpacity = 1;
    }

    public static void transition() {
        transition(10);
    }

    public static void transition(int duration) {
        Gdx.app.log("Graphics", "Transition called for duration " + duration);
        if (backBuffer == null) {
            Gdx.app.log("Graphics", "WARN: Calling transition without a freeze!");
            return;
        }
        brightness = 255;
        float fadeStep = (1f / duration);
        for (int i = duration; i >= 0; i--) {
            backBufferOpacity = (i * fadeStep);
            update();
        }
        if (backBuffer != null) backBuffer.dispose();
        backBuffer = null;
    }

    public static void transition(int duration, String filename) {

        transition(duration, filename, 60);
    }

    public static void transition(int duration, String filename, int vague) {
        if (backBuffer == null) {
            Gdx.app.log("Graphics", "WARN: Calling transition without a freeze!");
            return;
        }
        if (duration < 2) {
            backBuffer.dispose();
            backBuffer = null;
            return;
        }
        brightness = 255;
        FileHandle file = FileUtil.loadImg(filename);
        Pixmap img = new Pixmap(file);
        transitionTexture = new Texture(img);
        transitioning = true;
        Graphics.vague = vague / 255f;
        float step = 1f / (duration - 1);
        for (int i = 0; i < duration; i++) {
            transitionFade = i * step;
            update();
        }
        transitioning = false;
        transitionTexture.dispose();
        transitionTexture = null;
        if (backBuffer == null) backBuffer.dispose();
        backBuffer = null;
    }

    public static void fadeout(int duration) {
        Gdx.app.log("Graphics", "Fadeout Called with duration " + duration);
        float fadeStep = (255f / duration);
        for (int i = duration; i >= 0; i--) {
            brightness = (int) (i * fadeStep);
            update();
        }
    }

    public static void fadein(int duration) {
        Gdx.app.log("Graphics", "Fadein Called with duration " + duration);
        float fadeStep = (255f / duration);
        Gdx.app.log("Graphics", "fade Step = " + fadeStep);
        for (int i = 0; i <= duration; i++) {
            brightness = (int) (i * fadeStep);
            update();
        }
    }

    public static Bitmap snap_to_bitmap() {
        Gdx.app.log("Graphics", "Called Snap to Bitmap!");
        byte[] bytes = ScreenUtils.getFrameBufferPixels(true);
        Pixmap p = new Pixmap(getWidth(), getHeight(), Pixmap.Format.RGBA8888);
        p.getPixels().put(bytes).rewind();

        Bitmap b = new Bitmap(p);
        b.setPath("snap_to_bitmap");
        return b;
    }

    public static void frame_reset() {
        frame_count = 0;
    }


}

/*
Graphics.update
Refreshes the game screen and advances time by 1 frame. This method must be called at set intervals.

loop do
  Graphics.update
  Input.update
  do_something
end
Graphics.wait(duration)
Waits for the specified number of frames. Equivalent to the following:

duration.times do
  Graphics.update
end
Graphics.fadeout(duration)
Performs a fade-out of the screen.

duration is the number of frames to spend on the fade-out.

Graphics.fadein(duration)
Performs a fade-in of the screen.

duration is the number of frames to spend on the fade-in.

Graphics.freeze
Freezes the current screen in preparation for transitions.

Screen rewrites are prohibited until the transition method is called.

Graphics.transition([duration[, filename[, vague]]])
Carries out a transition from the screen frozen by Graphics.freeze to the current screen.

duration is the number of frames the transition will last. The default is 10.

filename specifies the file name of the transition graphic. When not specified, a standard fade will be used. Also automatically searches files included in RGSS-RTP and encrypted archives. File extensions may be omitted.

vague sets the ambiguity of the borderline between the graphic's starting and ending points. The larger the value, the greater the ambiguity. The default is 40.

Graphics.snap_to_bitmap
Gets the current game screen image as a bitmap object.

This reflects the graphics that should be displayed at that point in time, without relation to the use of the freeze method.

The created bitmap must be freed when it is no longer needed.

Graphics.frame_reset
Resets the screen refresh timing. Call this method after a time-consuming process to prevent excessive frame skipping.

Graphics.width
Graphics.height
Gets the width and height of the game screen.

These are normally 544 and 416, respectively.

Graphics.resize_screen(width, height)
Changes the size of the game screen.

Specify a value up to 640 × 480 for width and height.

Graphics.play_movie(filename) (RGSS3)
Plays the movie specified by filename.

Returns process after waiting for playback to end.

*/