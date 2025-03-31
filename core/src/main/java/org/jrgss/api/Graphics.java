package org.jrgss.api;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.JrgssBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.google.common.collect.Lists;
import java.lang.ref.Reference;
import java.nio.Buffer;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.jrgss.FileUtil;
import org.jrgss.JRGSSApplication;
import org.jrgss.JRGSSGame;
import org.jrgss.Timer;
import org.jrgss.shaders.AlphaBlendingShader;
import org.jrgss.shaders.TransitionShaderProgram;
import org.jruby.Ruby;
import org.jruby.ext.fiber.ThreadFiber;
import org.jruby.runtime.builtin.IRubyObject;

public class Graphics {
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
   public static int frame_count = 0;
   public static int frame_rate = 60;
   public static int brightness = 255;
   public static FrameBuffer backBuffer;
   public static float backBufferOpacity = 0.0F;
   public static float transitionFade = 1.0F;
   public static boolean transitioning = false;
   public static Texture transitionTexture = null;
   public static float vague;
   public static FrameBuffer tempBuffer;
   public static int desiredWidth = 800;
   public static int desiredHeight = 450;
   private static int x = 0;
   private static int y = 0;
   private static int width = 0;
   private static int height = 0;
   public static boolean fullscreen = false;
   public static double scale = 1.0;
   public static double physicalScale = 1.0;
   public static float hiResScale = 1.0F;
   private static int lastBlendMode = 32774;
   private static long totalTime = 0L;
   private static int times = 0;
   private static List<AbstractRenderable> renderables = Lists.newArrayListWithCapacity(256);
   private static JrgssBatch spritesBatch;
   private static long lastFrame = 0L;
   private static JrgssBatch finalBatch;
   private static final JrgssBatch freezeBatch = new JrgssBatch();

   public static int getViewportWidth() {
      return tempBuffer.getWidth();
   }

   public static int getViewportHeight() {
      return tempBuffer.getHeight();
   }

   public static int getWidth() {
      return desiredWidth;
   }

   public static int getHeight() {
      return desiredHeight;
   }

   public static void setWidth(int width) {
      desiredWidth = width;
      DisplayMode mode = Gdx.graphics.getDisplayMode();
      updateDisplayParams(mode.width, mode.height);
   }

   public static void setHeight(int height) {
      desiredHeight = height;
      DisplayMode mode = Gdx.graphics.getDisplayMode();
      updateDisplayParams(mode.width, mode.height);
   }

   public static FrameBuffer checkBufferSize(FrameBuffer buffer) {
      if (buffer == null || buffer.getWidth() != (int)(getWidth() * hiResScale) || buffer.getHeight() != (int)(getHeight() * hiResScale)) {
         if (buffer != null) {
            buffer.dispose();
         }

         buffer = new FrameBuffer(Format.RGB888, (int)(getWidth() * hiResScale), (int)(getHeight() * hiResScale), false);
         buffer.getColorBufferTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
         Gdx.app.log("Graphics", "new buffer @ size = " + buffer.getWidth() + ", " + buffer.getHeight());
      }

      return buffer;
   }

   public static void toggleFullScreen() {
      ((JRGSSApplication)Gdx.app).runWithGLContextPriority(() -> {
         fullscreen = !fullscreen;
         resize_screen(desiredWidth, desiredHeight);
      });
   }

   public static void setFullscreen(final boolean fullscreen) {
      ((JRGSSApplication)Gdx.app).runWithGLContextPriority(() -> {
         if (fullscreen != Graphics.fullscreen) {
             Graphics.fullscreen = fullscreen;
            resize_screen(desiredWidth, desiredHeight);
         }
      });
   }

   public static boolean isFullscreen() {
      return fullscreen;
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
         ((JRGSSApplication)Gdx.app).windowed(width, height);
         x = 0;
         y = 0;
         Graphics.width = Gdx.graphics.getWidth();
         Graphics.height = Gdx.graphics.getHeight();
         physicalScale = 1.0;
         scale = (float)Gdx.graphics.getWidth() / width;
         hiResScale = (float)height / desiredHeight * (float)scale;
         Gdx.app.log("Graphics", "High Resolution scale set to " + hiResScale);
         Font.resetCache();
         Bitmap.reloadAll();
         tempBuffer = checkBufferSize(tempBuffer);
      } else {
         DisplayMode mode = Gdx.graphics.getDisplayMode();
         Gdx.app.log("Graphics", "Fullscreen resolution is " + mode.width + "x" + mode.height);
         if (!Gdx.graphics.isFullscreen()) {
            ((JRGSSApplication)Gdx.app).fullscreen();
            updateDisplayParams(mode.width, mode.height);
         }

         scale = (float)Gdx.graphics.getWidth() / mode.width;
         physicalScale = (float)Gdx.graphics.getBackBufferWidth() / mode.width;
      }

      if (scale < 0.01) {
         scale = 1.0;
         physicalScale = 1.0;
      }

      Gdx.app.log("Graphics", "We are now at " + getWidth() + "x" + getHeight() + "@(" + scale + "," + physicalScale + ")");
   }

   private static void updateDisplayParams(int width, int height) {
      float ratio = (float)width / height;
      float desiredRatio = (float)desiredWidth / desiredHeight;
      if (ratio < desiredRatio) {
         x = 0;
         Graphics.width = width;
         Graphics.height = (int)(width / desiredRatio);
         y = (height - Graphics.height) / 2;
         hiResScale = (float)width / desiredWidth;
      } else {
         y = 0;
         Graphics.height = height;
         Graphics.width = (int)(height * desiredRatio);
         x = (width - Graphics.width) / 2;
         hiResScale = (float)height / desiredHeight;
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
         AbstractRenderable.renderQueue
            .values()
            .stream()
            .filter(renderableWeakReference -> renderableWeakReference.get() != null)
            .map(Reference::get)
            .forEach(renderables::add);
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
         batch.setShader(TransitionShaderProgram.get());
         batch.begin();
         TransitionShaderProgram.get().begin();
         TransitionShaderProgram.get().setFade(transitionFade);
         TransitionShaderProgram.get().setVague(vague);
         batch.draw(transitionTexture, 0.0F, 0.0F, (float)getWidth(), (float)getHeight());
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
         finalBatch.draw(
            tempBuffer.getColorBufferTexture(),
            (float)x,
            (float)y,
            (float)(tempBuffer.getWidth() * physicalScale),
            (float)(tempBuffer.getHeight() * physicalScale)
         );
         if (backBuffer != null) {
            finalBatch.setColor(1.0F, 1.0F, 1.0F, backBufferOpacity);
            finalBatch.draw(
               backBuffer.getColorBufferTexture(),
               (float)x,
               (float)y,
               (float)(backBuffer.getWidth() * physicalScale),
               (float)(backBuffer.getHeight() * physicalScale)
            );
         }

         if (brightness != 255) {
            finalBatch.setColor(0.0F, 0.0F, 0.0F, (255.0F - brightness) / 255.0F);
            finalBatch.draw(
               Sprite.getColorTexture(), (float)x, (float)y, (float)(tempBuffer.getWidth() * physicalScale), (float)(tempBuffer.getHeight() * physicalScale)
            );
            finalBatch.setColor(1.0F, 1.0F, 1.0F, 1.0F);
         }

         finalBatch.end();
      }

      frame_count++;
      lastFrame = System.nanoTime();
   }

   public static void update() {
      ((JRGSSApplication)Gdx.app).handlePlatform();
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
         brightness = (int)(i * fadeStep);
         update();
      }

      System.gc();
   }

   public static void fadein(int duration) {
      Gdx.app.log("Graphics", "Fadein Called with duration " + duration);
      float fadeStep = 255.0F / duration;
      Gdx.app.log("Graphics", "fade Step = " + fadeStep);

      for (int i = 0; i <= duration; i++) {
         brightness = (int)(i * fadeStep);
         update();
      }

      System.gc();
   }

   public static Bitmap snap_to_bitmap() {
      Gdx.app.log("Graphics", "Called Snap to Bitmap!");
      tempBuffer.begin();
      byte[] bytes = ScreenUtils.getFrameBufferPixels(0, 0, getWidth(), getHeight(), true);
      tempBuffer.end();
      Gdx.app.log("Graphics", "got " + bytes.length + " pixels");
      Pixmap p = new Pixmap(getWidth(), getHeight(), Format.RGBA8888);
      ((Buffer)p.getPixels().put(bytes)).rewind();
      Bitmap b = new Bitmap(p);
      b.setPath("snap_to_bitmap");
      return b;
   }

   public static void frame_reset() {
      frame_count = 0;
   }

   public static int getFrame_count() {
      return frame_count;
   }

   public static int getFrame_rate() {
      return frame_rate;
   }

   public static void setFrame_rate(int frame_rate) {
      Graphics.frame_rate = frame_rate;
   }

   public static int getBrightness() {
      return brightness;
   }

   public static void setBrightness(int brightness) {
      Graphics.brightness = brightness;
   }
}
