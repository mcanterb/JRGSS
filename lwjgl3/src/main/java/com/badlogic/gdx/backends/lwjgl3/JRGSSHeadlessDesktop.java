//package com.badlogic.gdx.backends.lwjgl3;
//
//import com.badlogic.gdx.ApplicationListener;
//import com.badlogic.gdx.ApplicationLogger;
//import com.badlogic.gdx.Audio;
//import com.badlogic.gdx.Files;
//import com.badlogic.gdx.Gdx;
//import com.badlogic.gdx.Graphics;
//import com.badlogic.gdx.Input;
//import com.badlogic.gdx.LifecycleListener;
//import com.badlogic.gdx.Net;
//import com.badlogic.gdx.Preferences;
//import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
//import com.badlogic.gdx.backends.headless.HeadlessApplicationLogger;
//import com.badlogic.gdx.backends.headless.HeadlessFiles;
//import com.badlogic.gdx.backends.headless.HeadlessNativesLoader;
//import com.badlogic.gdx.backends.headless.HeadlessNet;
//import com.badlogic.gdx.backends.headless.HeadlessPreferences;
//import com.badlogic.gdx.backends.headless.mock.audio.MockAudio;
//import com.badlogic.gdx.backends.headless.mock.graphics.MockGraphics;
//import com.badlogic.gdx.backends.headless.mock.input.MockInput;
//import com.badlogic.gdx.utils.Array;
//import com.badlogic.gdx.utils.Clipboard;
//import com.badlogic.gdx.utils.GdxRuntimeException;
//import com.badlogic.gdx.utils.ObjectMap;
//import com.badlogic.gdx.utils.TimeUtils;
//import org.jrgss.AudioUpdateFunction;
//import org.jrgss.JRGSSApplication;
//import org.jrgss.JRGSSApplicationListener;
//
//public class JRGSSHeadlessDesktop implements JRGSSApplication {
//   protected final JRGSSApplicationListener listener;
//   protected Thread mainLoopThread;
//   protected final HeadlessFiles files;
//   protected final HeadlessNet net;
//   protected final MockAudio audio;
//   protected final MockInput input;
//   protected final MockGraphics graphics;
//   protected boolean running = true;
//   protected final Array<Runnable> runnables = new Array<>();
//   protected final Array<Runnable> executedRunnables = new Array<>();
//   protected final Array<LifecycleListener> lifecycleListeners = new Array<>();
//   protected int logLevel = 2;
//   protected ApplicationLogger applicationLogger;
//   private String preferencesdir;
//   private final long renderInterval;
//   ObjectMap<String, Preferences> preferences = new ObjectMap<>();
//
//   public JRGSSHeadlessDesktop(JRGSSApplicationListener listener) {
//      HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
//      HeadlessNativesLoader.load();
//      this.setApplicationLogger(new HeadlessApplicationLogger());
//      this.listener = listener;
//      this.files = new HeadlessFiles();
//      this.net = new HeadlessNet();
//      this.graphics = new MockGraphics() {
//         @Override
//         public int getHeight() {
//            return 450;
//         }
//
//         @Override
//         public int getWidth() {
//            return 800;
//         }
//      };
//      this.audio = new MockAudio();
//      this.input = new MockInput();
//      this.preferencesdir = config.preferencesDirectory;
//      Gdx.app = this;
//      Gdx.files = this.files;
//      Gdx.net = this.net;
//      Gdx.audio = this.audio;
//      Gdx.graphics = this.graphics;
//      Gdx.input = this.input;
//      this.renderInterval = config.renderInterval > 0.0F ? (long)(config.renderInterval * 1.0E9F) : (config.renderInterval < 0.0F ? -1 : 0);
//      listener.loadScripts();
//      this.initialize();
//   }
//
//   private void initialize() {
//      try {
//         this.mainLoop();
//      } catch (Throwable var2) {
//         if (var2 instanceof RuntimeException) {
//            throw (RuntimeException)var2;
//         } else {
//            throw new GdxRuntimeException(var2);
//         }
//      }
//   }
//
//   void mainLoop() {
//      Array<LifecycleListener> lifecycleListeners = this.lifecycleListeners;
//      this.listener.create();
//      long t = TimeUtils.nanoTime() + this.renderInterval;
//      if ((float)this.renderInterval >= 0.0F) {
//         while (this.running) {
//            long n = TimeUtils.nanoTime();
//            if (t > n) {
//               try {
//                  Thread.sleep((t - n) / 1000000L);
//               } catch (InterruptedException var8) {
//               }
//
//               t = TimeUtils.nanoTime() + this.renderInterval;
//            } else {
//               t = n + this.renderInterval;
//            }
//
//            this.executeRunnables();
//            this.graphics.incrementFrameId();
//            this.listener.render();
//            this.graphics.updateTime();
//            if (!this.running) {
//               break;
//            }
//         }
//      }
//
//      synchronized (lifecycleListeners) {
//         for (LifecycleListener listener : lifecycleListeners) {
//            listener.pause();
//            listener.dispose();
//         }
//      }
//
//      this.listener.pause();
//      this.listener.dispose();
//   }
//
//   public boolean executeRunnables() {
//      synchronized (this.runnables) {
//         for (int i = this.runnables.size - 1; i >= 0; i--) {
//            this.executedRunnables.add(this.runnables.get(i));
//         }
//
//         this.runnables.clear();
//      }
//
//      if (this.executedRunnables.size == 0) {
//         return false;
//      } else {
//         for (int i = this.executedRunnables.size - 1; i >= 0; i--) {
//            this.executedRunnables.removeIndex(i).run();
//         }
//
//         return true;
//      }
//   }
//
//   @Override
//   public ApplicationListener getApplicationListener() {
//      return this.listener;
//   }
//
//   @Override
//   public Graphics getGraphics() {
//      return this.graphics;
//   }
//
//   @Override
//   public Audio getAudio() {
//      return this.audio;
//   }
//
//   @Override
//   public Input getInput() {
//      return this.input;
//   }
//
//   @Override
//   public Files getFiles() {
//      return this.files;
//   }
//
//   @Override
//   public Net getNet() {
//      return this.net;
//   }
//
//   @Override
//   public ApplicationType getType() {
//      return ApplicationType.HeadlessDesktop;
//   }
//
//   @Override
//   public int getVersion() {
//      return 0;
//   }
//
//   @Override
//   public long getJavaHeap() {
//      return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
//   }
//
//   @Override
//   public long getNativeHeap() {
//      return this.getJavaHeap();
//   }
//
//   @Override
//   public Preferences getPreferences(String name) {
//      if (this.preferences.containsKey(name)) {
//         return this.preferences.get(name);
//      } else {
//         Preferences prefs = new HeadlessPreferences(name, this.preferencesdir);
//         this.preferences.put(name, prefs);
//         return prefs;
//      }
//   }
//
//   @Override
//   public Clipboard getClipboard() {
//      return null;
//   }
//
//   @Override
//   public void postRunnable(Runnable runnable) {
//      synchronized (this.runnables) {
//         this.runnables.add(runnable);
//      }
//   }
//
//   @Override
//   public void debug(String tag, String message) {
//      if (this.logLevel >= 3) {
//         this.getApplicationLogger().debug(tag, message);
//      }
//   }
//
//   @Override
//   public void debug(String tag, String message, Throwable exception) {
//      if (this.logLevel >= 3) {
//         this.getApplicationLogger().debug(tag, message, exception);
//      }
//   }
//
//   @Override
//   public void log(String tag, String message) {
//      if (this.logLevel >= 2) {
//         this.getApplicationLogger().log(tag, message);
//      }
//   }
//
//   @Override
//   public void log(String tag, String message, Throwable exception) {
//      if (this.logLevel >= 2) {
//         this.getApplicationLogger().log(tag, message, exception);
//      }
//   }
//
//   @Override
//   public void error(String tag, String message) {
//      if (this.logLevel >= 1) {
//         this.getApplicationLogger().error(tag, message);
//      }
//   }
//
//   @Override
//   public void error(String tag, String message, Throwable exception) {
//      if (this.logLevel >= 1) {
//         this.getApplicationLogger().error(tag, message, exception);
//      }
//   }
//
//   @Override
//   public void setLogLevel(int logLevel) {
//      this.logLevel = logLevel;
//   }
//
//   @Override
//   public int getLogLevel() {
//      return this.logLevel;
//   }
//
//   @Override
//   public void setApplicationLogger(ApplicationLogger applicationLogger) {
//      this.applicationLogger = applicationLogger;
//   }
//
//   @Override
//   public ApplicationLogger getApplicationLogger() {
//      return this.applicationLogger;
//   }
//
//   @Override
//   public void exit() {
//      this.postRunnable(new Runnable() {
//         @Override
//         public void run() {
//            JRGSSHeadlessDesktop.this.running = false;
//         }
//      });
//   }
//
//   @Override
//   public void addLifecycleListener(LifecycleListener listener) {
//      synchronized (this.lifecycleListeners) {
//         this.lifecycleListeners.add(listener);
//      }
//   }
//
//   @Override
//   public void removeLifecycleListener(LifecycleListener listener) {
//      synchronized (this.lifecycleListeners) {
//         this.lifecycleListeners.removeValue(listener, true);
//      }
//   }
//
//   @Override
//   public void handlePlatform() {
//   }
//
//   @Override
//   public void fullscreen() {
//   }
//
//   @Override
//   public boolean isFocused() {
//      return true;
//   }
//
//   @Override
//   public void windowed(int width, int height) {
//   }
//
//   @Override
//   public void runWithGLContext(Runnable runnable) {
//   }
//
//   @Override
//   public void runWithGLContextPriority(Runnable runnable) {
//   }
//
//   @Override
//   public void addAudioUpdater(AudioUpdateFunction function) {
//   }
//}
