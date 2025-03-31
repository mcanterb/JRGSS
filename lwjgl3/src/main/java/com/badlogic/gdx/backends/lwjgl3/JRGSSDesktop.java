//package com.badlogic.gdx.backends.lwjgl3;
//
//import com.badlogic.gdx.ApplicationListener;
//import com.badlogic.gdx.ApplicationLogger;
//import com.badlogic.gdx.Audio;
//import com.badlogic.gdx.Files;
//import com.badlogic.gdx.Gdx;
//import com.badlogic.gdx.Input;
//import com.badlogic.gdx.LifecycleListener;
//import com.badlogic.gdx.Net;
//import com.badlogic.gdx.Preferences;
//import com.badlogic.gdx.backends.lwjgl3.audio.Ogg;
//import com.badlogic.gdx.backends.lwjgl3.audio.OpenALAudioDevice;
//import com.badlogic.gdx.backends.lwjgl3.audio.OpenALLwjgl3Audio;
//import com.badlogic.gdx.backends.lwjgl3.audio.mock.MockAudio;
//import com.badlogic.gdx.graphics.GL20;
//import com.badlogic.gdx.graphics.Pixmap;
//import com.badlogic.gdx.graphics.glutils.GLVersion;
//import com.badlogic.gdx.utils.Array;
//import com.badlogic.gdx.utils.Clipboard;
//import com.badlogic.gdx.utils.GdxRuntimeException;
//import com.badlogic.gdx.utils.ObjectMap;
//import com.badlogic.gdx.utils.SharedLibraryLoader;
//import java.io.File;
//import java.io.PrintStream;
//import java.net.JarURLConnection;
//import java.net.URL;
//import java.nio.IntBuffer;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.atomic.AtomicBoolean;
//import java.util.jar.Attributes;
//import java.util.jar.Manifest;
//import org.jrgss.AudioUpdateFunction;
//import org.jrgss.JRGSSApplication;
//import org.jrgss.JRGSSApplicationListener;
//import org.jrgss.api.Graphics;
//import org.lwjgl.glfw.GLFW;
//import org.lwjgl.glfw.GLFWErrorCallback;
//import org.lwjgl.glfw.GLFWImage;
//import org.lwjgl.glfw.GLFWVidMode;
//import org.lwjgl.glfw.GLFWImage.Buffer;
//import org.lwjgl.opengl.AMDDebugOutput;
//import org.lwjgl.opengl.ARBDebugOutput;
//import org.lwjgl.opengl.GL;
//import org.lwjgl.opengl.GL11;
//import org.lwjgl.opengl.GL43;
//import org.lwjgl.opengl.GLCapabilities;
//import org.lwjgl.opengl.GLUtil;
//import org.lwjgl.opengl.KHRDebug;
//import org.lwjgl.system.Callback;
//
//public class JRGSSDesktop implements JRGSSApplication {
//   JRGSSApplicationListener jrgssApplicationListener;
//   private final Lwjgl3ApplicationConfiguration config;
//   private final Array<Lwjgl3Window> windows = new Array<>();
//   private volatile Lwjgl3Window currentWindow;
//   private Audio audio;
//   private final Files files;
//   private final Net net;
//   private final ObjectMap<String, Preferences> preferences = new ObjectMap<>();
//   private final Lwjgl3Clipboard clipboard;
//   private final Array<Lwjgl3Window> closedWindows = new Array<>();
//   private int logLevel = 2;
//   private volatile boolean running = true;
//   private final Array<Runnable> runnables = new Array<>();
//   private final Array<Runnable> executedRunnables = new Array<>();
//   private final Array<LifecycleListener> lifecycleListeners = new Array<>();
//   private static GLFWErrorCallback errorCallback;
//   private static GLVersion glVersion;
//   private static Callback glDebugCallback;
//   private final AtomicBoolean isFocused = new AtomicBoolean(true);
//   private ApplicationLogger applicationLogger;
//   private final PrintStream outputLog;
//   private final Array<AudioUpdateFunction> audioUpdaters = new Array<>();
//   private boolean windowed = true;
//   int lastWidth;
//   int lastHeight;
//   volatile boolean killAudio = false;
//   Thread audioUpdateThread = new Thread(new Runnable() {
//      Array<AudioUpdateFunction> toRemove = new Array<>();
//
//      @Override
//      public void run() {
//         while (!JRGSSDesktop.this.killAudio) {
//            if (JRGSSDesktop.this.audio != null) {
//               synchronized (org.jrgss.api.Audio.class) {
//                  for (int i = 0; i < JRGSSDesktop.this.audioUpdaters.size; i++) {
//                     if (JRGSSDesktop.this.audioUpdaters.get(i).update()) {
//                        this.toRemove.add(JRGSSDesktop.this.audioUpdaters.get(i));
//                     }
//                  }
//
//                  if (JRGSSDesktop.this.audio instanceof OpenALLwjgl3Audio) {
//                     ((OpenALLwjgl3Audio)JRGSSDesktop.this.audio).update();
//                  }
//
//                  JRGSSDesktop.this.audioUpdaters.removeAll(this.toRemove, true);
//                  this.toRemove.clear();
//               }
//            }
//
//            try {
//               Thread.sleep(50L);
//            } catch (Exception var4) {
//            }
//         }
//      }
//   });
//   long lastTime = 0L;
//   long variableYieldTime = 0L;
//
//   static void initializeGlfw() {
//      if (errorCallback == null) {
//         Lwjgl3NativesLoader.load();
//         errorCallback = GLFWErrorCallback.createPrint(System.err);
//         GLFW.glfwSetErrorCallback(errorCallback);
//         if (!GLFW.glfwInit()) {
//            throw new GdxRuntimeException("Unable to initialize GLFW");
//         }
//      }
//   }
//
//   public JRGSSDesktop(JRGSSApplicationListener jrgssApplicationListener, Lwjgl3ApplicationConfiguration config, int logLevel) {
//      initializeGlfw();
//      this.logLevel = logLevel;
//      this.config = Lwjgl3ApplicationConfiguration.copy(config);
//      this.jrgssApplicationListener = jrgssApplicationListener;
//      Gdx.app = this;
//      if (!config.disableAudio) {
//         try {
//            this.audio = Gdx.audio = new OpenALLwjgl3Audio(config.audioDeviceSimultaneousSources, config.audioDeviceBufferCount, config.audioDeviceBufferSize);
//            ((OpenALLwjgl3Audio)this.audio).registerMusic("ogx", Ogg.Music.class);
//            ((OpenALLwjgl3Audio)this.audio).registerSound("ogx", Ogg.Sound.class);
//         } catch (Throwable var12) {
//            this.log("Lwjgl3Application", "Couldn't initialize audio, disabling audio", var12);
//            this.audio = Gdx.audio = new MockAudio();
//         }
//      } else {
//         this.audio = Gdx.audio = new MockAudio();
//      }
//
//      this.outputLog = System.out;
//      files = new Lwjgl3Files();
//      net = new Lwjgl3Net(config);
//      this.clipboard = new Lwjgl3Clipboard();
//      config.setWindowListener(new Lwjgl3WindowListener() {
//          @Override
//          public void created(Lwjgl3Window lwjgl3Window) {
//
//          }
//
//          @Override
//         public void iconified(boolean b) {
//         }
//
//         @Override
//         public void maximized(boolean b) {
//         }
//
//         @Override
//         public void focusLost() {
//            JRGSSDesktop.this.isFocused.set(false);
//         }
//
//         @Override
//         public void focusGained() {
//            JRGSSDesktop.this.isFocused.set(true);
//         }
//
//         @Override
//         public boolean closeRequested() {
//            return true;
//         }
//
//         @Override
//         public void filesDropped(String[] files) {
//         }
//
//         @Override
//         public void refreshRequested() {
//            Lwjgl3Window window = JRGSSDesktop.this.getCurrentWindow();
//            int width = window.getGraphics().getWidth();
//            int height = window.getGraphics().getHeight();
//            Gdx.gl.glViewport(0, 0, width, height);
//            GLFW.glfwPollEvents();
//         }
//      });
//      config.setInitialVisible(true);
//      Lwjgl3Window window = this.createWindow(config, jrgssApplicationListener, 0L);
//      this.windows.add(window);
//      this.handlePlatform();
//
//      try {
//         Graphics.init();
//      } catch (Exception var11) {
//         throw new GdxRuntimeException(var11);
//      }
//
//      try {
//         jrgssApplicationListener.loadSplashScreen();
//         GLFW.glfwSwapBuffers(this.getCurrentWindow().getWindowHandle());
//         jrgssApplicationListener.loadScripts();
//         this.loop();
//      } catch (Throwable var13) {
//         if (var13 instanceof RuntimeException) {
//            throw (RuntimeException)var13;
//         }
//
//         throw new GdxRuntimeException(var13);
//      } finally {
//         this.cleanupWindows();
//         this.cleanup();
//      }
//   }
//
//   private void loop() {
//      this.audioUpdateThread.setDaemon(true);
//      this.audioUpdateThread.start();
//
//      try {
//         this.jrgssApplicationListener.getMain().main();
//      } catch (Exception var2) {
//         var2.printStackTrace(System.err);
//         throw new RuntimeException(var2);
//      }
//   }
//
//   private void cleanupWindows() {
//      synchronized (this.lifecycleListeners) {
//         for (LifecycleListener lifecycleListener : this.lifecycleListeners) {
//            lifecycleListener.pause();
//            lifecycleListener.dispose();
//         }
//      }
//
//      for (Lwjgl3Window window : this.windows) {
//         window.dispose();
//      }
//
//      this.windows.clear();
//   }
//
//   private void cleanup() {
//      Lwjgl3Cursor.disposeSystemCursors();
//      if (this.audio instanceof OpenALLwjgl3Audio) {
//         ((OpenALLwjgl3Audio)this.audio).dispose();
//      }
//
//      errorCallback.free();
//      if (glDebugCallback != null) {
//         glDebugCallback.free();
//      }
//
//      GLFW.glfwTerminate();
//   }
//
//   @Override
//   public ApplicationListener getApplicationListener() {
//      return this.currentWindow.getListener();
//   }
//
//   @Override
//   public com.badlogic.gdx.Graphics getGraphics() {
//      return this.currentWindow.getGraphics();
//   }
//
//   @Override
//   public Audio getAudio() {
//      return this.audio;
//   }
//
//   @Override
//   public Input getInput() {
//      return this.currentWindow.getInput();
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
//   public void debug(String tag, String message) {
//      if (this.logLevel >= 3) {
//         this.outputLog.println(tag + ": " + message);
//      }
//   }
//
//   @Override
//   public void debug(String tag, String message, Throwable exception) {
//      if (this.logLevel >= 3) {
//         this.outputLog.println(tag + ": " + message);
//         exception.printStackTrace(this.outputLog);
//      }
//   }
//
//   @Override
//   public void log(String tag, String message) {
//      if (this.logLevel >= 2) {
//         this.outputLog.println(tag + ": " + message);
//      }
//   }
//
//   @Override
//   public void log(String tag, String message, Throwable exception) {
//      if (this.logLevel >= 2) {
//         this.outputLog.println(tag + ": " + message);
//         exception.printStackTrace(this.outputLog);
//      }
//   }
//
//   @Override
//   public void error(String tag, String message) {
//      if (this.logLevel >= 1) {
//         this.outputLog.println(tag + ": " + message);
//      }
//   }
//
//   @Override
//   public void error(String tag, String message, Throwable exception) {
//      if (this.logLevel >= 1) {
//         this.outputLog.println(tag + ": " + message);
//         exception.printStackTrace(this.outputLog);
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
//   public ApplicationType getType() {
//      return ApplicationType.Desktop;
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
//         Preferences prefs = new Lwjgl3Preferences(new Lwjgl3FileHandle(new File(this.config.preferencesDirectory, name), this.config.preferencesFileType));
//         this.preferences.put(name, prefs);
//         return prefs;
//      }
//   }
//
//   @Override
//   public Clipboard getClipboard() {
//      return this.clipboard;
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
//   public void exit() {
//      this.running = false;
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
//   public Lwjgl3Window newWindow(ApplicationListener listener, Lwjgl3WindowConfiguration config) {
//      Lwjgl3ApplicationConfiguration appConfig = Lwjgl3ApplicationConfiguration.copy(this.config);
//      appConfig.setWindowedMode(config.windowWidth, config.windowHeight);
//      appConfig.setWindowPosition(config.windowX, config.windowY);
//      appConfig.setWindowSizeLimits(config.windowMinWidth, config.windowMinHeight, config.windowMaxWidth, config.windowMaxHeight);
//      appConfig.setResizable(config.windowResizable);
//      appConfig.setDecorated(config.windowDecorated);
//      appConfig.setWindowListener(config.windowListener);
//      appConfig.setFullscreenMode(config.fullscreenMode);
//      appConfig.setTitle(config.title);
//      appConfig.setInitialBackgroundColor(config.initialBackgroundColor);
//      appConfig.setInitialVisible(config.initialVisible);
//      Lwjgl3Window window = this.createWindow(appConfig, listener, this.windows.get(0).getWindowHandle());
//      this.windows.add(window);
//      return window;
//   }
//
//   private Lwjgl3Window createWindow(Lwjgl3ApplicationConfiguration config, ApplicationListener listener, long sharedContext) {
//      long windowHandle = createGlfwWindow(config, sharedContext);
//      Lwjgl3Window window = new Lwjgl3Window(windowHandle, listener, config);
//      Buffer arr = GLFWImage.create(config.windowIconPaths.length);
//      List<Pixmap> icons = new ArrayList<>();
//
//      for (String iconPath : config.windowIconPaths) {
//         Pixmap icon = new Pixmap(this.files.getFileHandle(iconPath, config.windowIconFileType));
//         arr.height(icon.getHeight()).width(icon.getWidth()).pixels(icon.getPixels());
//         arr.position(arr.position() + 1);
//         icons.add(icon);
//      }
//
//      arr.flip();
//      GLFW.glfwSetWindowIcon(windowHandle, arr);
//      GLFW.glfwShowWindow(windowHandle);
//      icons.forEach(Pixmap::dispose);
//      return window;
//   }
//
//   static long createGlfwWindow(Lwjgl3ApplicationConfiguration config, long sharedContextWindow) {
//      GLFW.glfwDefaultWindowHints();
//      GLFW.glfwWindowHint(131075, config.windowResizable ? 1 : 0);
//      GLFW.glfwWindowHint(131073, 1);
//      if (sharedContextWindow == 0L) {
//         GLFW.glfwWindowHint(135169, config.r);
//         GLFW.glfwWindowHint(135170, config.g);
//         GLFW.glfwWindowHint(135171, config.b);
//         GLFW.glfwWindowHint(135172, config.a);
//         GLFW.glfwWindowHint(135174, config.stencil);
//         GLFW.glfwWindowHint(135173, config.depth);
//         GLFW.glfwWindowHint(135181, config.samples);
//      }
//
//      if (config.useGL30) {
//         GLFW.glfwWindowHint(139266, config.gles30ContextMajorVersion);
//         GLFW.glfwWindowHint(139267, config.gles30ContextMinorVersion);
//         if (SharedLibraryLoader.isMac) {
//            GLFW.glfwWindowHint(139270, 1);
//            GLFW.glfwWindowHint(139272, 204801);
//         }
//      }
//
//      if (config.debug) {
//         GLFW.glfwWindowHint(139271, 1);
//      }
//
//      long windowHandle = 0L;
//      GLFW.glfwWindowHint(135183, 60);
//      if (config.fullscreenMode != null) {
//         windowHandle = GLFW.glfwCreateWindow(
//            config.fullscreenMode.width, config.fullscreenMode.height, config.title, config.fullscreenMode.getMonitor(), sharedContextWindow
//         );
//      } else {
//         GLFW.glfwWindowHint(131077, config.windowDecorated ? 1 : 0);
//         windowHandle = GLFW.glfwCreateWindow(config.windowWidth, config.windowHeight, config.title, 0L, 0L);
//      }
//
//      if (windowHandle == 0L) {
//         throw new GdxRuntimeException("Couldn't create window");
//      } else {
//         GLFW.glfwSetWindowSizeLimits(
//            windowHandle,
//            config.windowMinWidth > -1 ? config.windowMinWidth : -1,
//            config.windowMinHeight > -1 ? config.windowMinHeight : -1,
//            config.windowMaxWidth > -1 ? config.windowMaxWidth : -1,
//            config.windowMaxHeight > -1 ? config.windowMaxHeight : -1
//         );
//         if (config.fullscreenMode == null) {
//            if (config.windowX == -1 && config.windowY == -1) {
//               int windowWidth = Math.max(config.windowWidth, config.windowMinWidth);
//               int windowHeight = Math.max(config.windowHeight, config.windowMinHeight);
//               if (config.windowMaxWidth > -1) {
//                  windowWidth = Math.min(windowWidth, config.windowMaxWidth);
//               }
//
//               if (config.windowMaxHeight > -1) {
//                  windowHeight = Math.min(windowHeight, config.windowMaxHeight);
//               }
//
//               GLFWVidMode vidMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
//               GLFW.glfwSetWindowPos(windowHandle, vidMode.width() / 2 - windowWidth / 2, vidMode.height() / 2 - windowHeight / 2);
//            } else {
//               GLFW.glfwSetWindowPos(windowHandle, config.windowX, config.windowY);
//            }
//         }
//
//         GLFW.glfwMakeContextCurrent(windowHandle);
//         GLFW.glfwSwapInterval(1);
//         GL.createCapabilities();
//         initiateGL();
//         if (!glVersion.isVersionEqualToOrHigher(2, 0)) {
//            throw new GdxRuntimeException(
//               "OpenGL 2.0 or higher with the FBO extension is required. OpenGL version: " + GL11.glGetString(7938) + "\n" + glVersion.getDebugVersionString()
//            );
//         } else if (!supportsFBO()) {
//            throw new GdxRuntimeException(
//               "OpenGL 2.0 or higher with the FBO extension is required. OpenGL version: "
//                  + GL11.glGetString(7938)
//                  + ", FBO extension: false\n"
//                  + glVersion.getDebugVersionString()
//            );
//         } else {
//            if (config.debug) {
//               glDebugCallback = GLUtil.setupDebugMessageCallback(config.debugStream);
//               setGLDebugMessageControl(Lwjgl3Application.GLDebugMessageSeverity.NOTIFICATION, false);
//            }
//
//            GL11.glClearColor(
//               config.initialBackgroundColor.r, config.initialBackgroundColor.g, config.initialBackgroundColor.b, config.initialBackgroundColor.a
//            );
//            GL11.glClear(16384);
//            GLFW.glfwSwapBuffers(windowHandle);
//            return windowHandle;
//         }
//      }
//   }
//
//   private static void initiateGL() {
//      String versionString = GL11.glGetString(7938);
//      String vendorString = GL11.glGetString(7936);
//      String rendererString = GL11.glGetString(7937);
//      glVersion = new GLVersion(ApplicationType.Desktop, versionString, vendorString, rendererString);
//   }
//
//   private static boolean supportsFBO() {
//      return glVersion.isVersionEqualToOrHigher(3, 0)
//         || GLFW.glfwExtensionSupported("GL_EXT_framebuffer_object")
//         || GLFW.glfwExtensionSupported("GL_ARB_framebuffer_object");
//   }
//
//   @Override
//   public void fullscreen() {
//      long monitor = GLFW.glfwGetPrimaryMonitor();
//      GLFWVidMode mode = GLFW.glfwGetVideoMode(monitor);
//      Gdx.app.log("JRGSSDesktop", "Switching to " + mode);
//      this.getGraphics().setFullscreenMode(this.getGraphics().getDisplayMode());
//      GLFW.glfwSetWindowPos(this.currentWindow.getWindowHandle(), 0, 0);
//      GLFW.glfwSetInputMode(this.currentWindow.getWindowHandle(), 208897, 212994);
//      GLFW.glfwSwapInterval(1);
//      this.windowed = false;
//   }
//
//   @Override
//   public void windowed(int width, int height) {
//      this.windowed = true;
//      if (this.getGraphics().isFullscreen() || width != this.getGraphics().getWidth() || height != this.getGraphics().getHeight()) {
//         this.getGraphics().setWindowedMode(width, height);
//         GLFW.glfwSetInputMode(this.currentWindow.getWindowHandle(), 208897, 212993);
//         GLFW.glfwSwapInterval(1);
//      }
//   }
//
//   @Override
//   public void runWithGLContext(Runnable runnable) {
//      this.runnables.add(runnable);
//   }
//
//   @Override
//   public void runWithGLContextPriority(Runnable runnable) {
//      this.runnables.insert(0, runnable);
//   }
//
//   @Override
//   public void addAudioUpdater(AudioUpdateFunction function) {
//      synchronized (org.jrgss.api.Audio.class) {
//         this.audioUpdaters.add(function);
//      }
//   }
//
//   @Override
//   public void handlePlatform() {
//      this.closedWindows.clear();
//      synchronized (this.runnables) {
//         this.executedRunnables.addAll(this.runnables);
//         this.runnables.clear();
//      }
//
//      for (Runnable runnable : this.executedRunnables) {
//         if (runnable != null) {
//            runnable.run();
//         } else {
//            Gdx.app.log("JRGSSDesktop", "runWithGLContextRunnable was null.");
//         }
//      }
//
//      this.executedRunnables.clear();
//
//      for (Lwjgl3Window window : this.windows) {
//         Gdx.graphics = window.getGraphics();
//         Gdx.gl30 = window.getGraphics().getGL30();
//         Gdx.gl20 = (GL20)(Gdx.gl30 != null ? Gdx.gl30 : window.getGraphics().getGL20());
//         Gdx.gl = (GL20)(Gdx.gl30 != null ? Gdx.gl30 : Gdx.gl20);
//         Gdx.input = window.getInput();
//         GLFW.glfwMakeContextCurrent(window.getWindowHandle());
//         this.currentWindow = window;
//         synchronized (this.lifecycleListeners) {
//            int width = window.getGraphics().getWidth();
//            int height = window.getGraphics().getHeight();
//            if (this.lastWidth != width || this.lastHeight != height) {
//               this.lastWidth = width;
//               this.lastHeight = height;
//               Gdx.gl.glViewport(0, 0, this.lastWidth, this.lastHeight);
//            }
//
//            window.update();
//         }
//
//         if (window.shouldClose()) {
//            this.closedWindows.add(window);
//         }
//      }
//
//      GLFW.glfwPollEvents();
//      this.sync(60);
//
//      for (Lwjgl3Window closedWindow : this.closedWindows) {
//         if (this.windows.size == 1) {
//            for (int i = this.lifecycleListeners.size - 1; i >= 0; i--) {
//               LifecycleListener l = this.lifecycleListeners.get(i);
//               l.pause();
//               l.dispose();
//            }
//
//            this.lifecycleListeners.clear();
//         }
//
//         closedWindow.dispose();
//         this.windows.removeValue(closedWindow, false);
//      }
//
//      if (this.windows.size == 0) {
//         System.exit(0);
//      }
//   }
//
//   @Override
//   public boolean isFocused() {
//      return this.isFocused.get();
//   }
//
//   public static boolean setGLDebugMessageControl(Lwjgl3Application.GLDebugMessageSeverity severity, boolean enabled) {
//      GLCapabilities caps = GL.getCapabilities();
//      int GL_DONT_CARE = 4352;
//      if (caps.OpenGL43) {
//         GL43.glDebugMessageControl(4352, 4352, severity.gl43, (IntBuffer)null, enabled);
//         return true;
//      } else if (caps.GL_KHR_debug) {
//         KHRDebug.glDebugMessageControl(4352, 4352, severity.khr, (IntBuffer)null, enabled);
//         return true;
//      } else if (caps.GL_ARB_debug_output && severity.arb != -1) {
//         ARBDebugOutput.glDebugMessageControlARB(4352, 4352, severity.arb, (IntBuffer)null, enabled);
//         return true;
//      } else if (caps.GL_AMD_debug_output && severity.amd != -1) {
//         AMDDebugOutput.glDebugMessageEnableAMD(4352, severity.amd, (IntBuffer)null, enabled);
//         return true;
//      } else {
//         return false;
//      }
//   }
//
//   private void sync(int fps) {
//      if (fps > 0) {
//         long sleepTime = 1000000000 / fps;
//         long yieldTime = Math.min(sleepTime, this.variableYieldTime + sleepTime % 1000000L);
//         long overSleep = 0L;
//
//         try {
//            while (true) {
//               long t = System.nanoTime() - this.lastTime;
//               if (t >= sleepTime - yieldTime) {
//                  if (t >= sleepTime) {
//                     overSleep = t - sleepTime;
//                     break;
//                  }
//
//                  Thread.yield();
//               } else {
//                  Thread.sleep(1L);
//               }
//            }
//         } catch (InterruptedException var13) {
//            var13.printStackTrace();
//         } finally {
//            this.lastTime = System.nanoTime() - Math.min(overSleep, sleepTime);
//            if (overSleep > this.variableYieldTime) {
//               this.variableYieldTime = Math.min(this.variableYieldTime + 200000L, sleepTime);
//            } else if (overSleep < this.variableYieldTime - 200000L) {
//               this.variableYieldTime = Math.max(this.variableYieldTime - 2000L, 0L);
//            }
//         }
//      }
//   }
//
//   @Override
//   public boolean equals(Object other) {
//      return this == other;
//   }
//
//   @Override
//   public int hashCode() {
//      return System.identityHashCode(this);
//   }
//
//   @Override
//   public String getBuildId() {
//      try {
//         URL jarURL = this.getClass().getResource("/org/jrgss/Desktop.class");
//         JarURLConnection jurlConn = (JarURLConnection)jarURL.openConnection();
//         Manifest mf = jurlConn.getManifest();
//         Attributes attributes = mf.getMainAttributes();
//         if (attributes != null) {
//            String val = attributes.getValue("JRGSS-Build");
//            if (val != null) {
//               return val;
//            }
//         }
//      } catch (Exception var6) {
//      }
//
//      return "Unknown";
//   }
//
//   public JRGSSApplicationListener getJrgssApplicationListener() {
//      return this.jrgssApplicationListener;
//   }
//
//   public Lwjgl3ApplicationConfiguration getConfig() {
//      return this.config;
//   }
//
//   public Array<Lwjgl3Window> getWindows() {
//      return this.windows;
//   }
//
//   public Lwjgl3Window getCurrentWindow() {
//      return this.currentWindow;
//   }
//
//   public ObjectMap<String, Preferences> getPreferences() {
//      return this.preferences;
//   }
//
//   public Array<Lwjgl3Window> getClosedWindows() {
//      return this.closedWindows;
//   }
//
//   public boolean isRunning() {
//      return this.running;
//   }
//
//   public Array<Runnable> getRunnables() {
//      return this.runnables;
//   }
//
//   public Array<Runnable> getExecutedRunnables() {
//      return this.executedRunnables;
//   }
//
//   public Array<LifecycleListener> getLifecycleListeners() {
//      return this.lifecycleListeners;
//   }
//
//   public AtomicBoolean getIsFocused() {
//      return this.isFocused;
//   }
//
//   public PrintStream getOutputLog() {
//      return this.outputLog;
//   }
//
//   public Array<AudioUpdateFunction> getAudioUpdaters() {
//      return this.audioUpdaters;
//   }
//
//   public boolean isWindowed() {
//      return this.windowed;
//   }
//
//   public int getLastWidth() {
//      return this.lastWidth;
//   }
//
//   public int getLastHeight() {
//      return this.lastHeight;
//   }
//
//   public boolean isKillAudio() {
//      return this.killAudio;
//   }
//
//   public Thread getAudioUpdateThread() {
//      return this.audioUpdateThread;
//   }
//
//   public long getLastTime() {
//      return this.lastTime;
//   }
//
//   public long getVariableYieldTime() {
//      return this.variableYieldTime;
//   }
//
//   public void setJrgssApplicationListener(JRGSSApplicationListener jrgssApplicationListener) {
//      this.jrgssApplicationListener = jrgssApplicationListener;
//   }
//
//   public void setCurrentWindow(Lwjgl3Window currentWindow) {
//      this.currentWindow = currentWindow;
//   }
//
//   public void setAudio(Audio audio) {
//      this.audio = audio;
//   }
//
//   public void setRunning(boolean running) {
//      this.running = running;
//   }
//
//   public void setWindowed(boolean windowed) {
//      this.windowed = windowed;
//   }
//
//   public void setLastWidth(int lastWidth) {
//      this.lastWidth = lastWidth;
//   }
//
//   public void setLastHeight(int lastHeight) {
//      this.lastHeight = lastHeight;
//   }
//
//   public void setKillAudio(boolean killAudio) {
//      this.killAudio = killAudio;
//   }
//
//   public void setAudioUpdateThread(Thread audioUpdateThread) {
//      this.audioUpdateThread = audioUpdateThread;
//   }
//
//   public void setLastTime(long lastTime) {
//      this.lastTime = lastTime;
//   }
//
//   public void setVariableYieldTime(long variableYieldTime) {
//      this.variableYieldTime = variableYieldTime;
//   }
//
//   @Override
//   public String toString() {
//      return "JRGSSDesktop(jrgssApplicationListener="
//         + this.getJrgssApplicationListener()
//         + ", config="
//         + this.getConfig()
//         + ", windows="
//         + this.getWindows()
//         + ", currentWindow="
//         + this.getCurrentWindow()
//         + ", audio="
//         + this.getAudio()
//         + ", files="
//         + this.getFiles()
//         + ", net="
//         + this.getNet()
//         + ", preferences="
//         + this.getPreferences()
//         + ", clipboard="
//         + this.getClipboard()
//         + ", closedWindows="
//         + this.getClosedWindows()
//         + ", logLevel="
//         + this.getLogLevel()
//         + ", running="
//         + this.isRunning()
//         + ", runnables="
//         + this.getRunnables()
//         + ", executedRunnables="
//         + this.getExecutedRunnables()
//         + ", lifecycleListeners="
//         + this.getLifecycleListeners()
//         + ", isFocused="
//         + this.getIsFocused()
//         + ", applicationLogger="
//         + this.getApplicationLogger()
//         + ", outputLog="
//         + this.getOutputLog()
//         + ", audioUpdaters="
//         + this.getAudioUpdaters()
//         + ", windowed="
//         + this.isWindowed()
//         + ", lastWidth="
//         + this.getLastWidth()
//         + ", lastHeight="
//         + this.getLastHeight()
//         + ", killAudio="
//         + this.isKillAudio()
//         + ", audioUpdateThread="
//         + this.getAudioUpdateThread()
//         + ", lastTime="
//         + this.getLastTime()
//         + ", variableYieldTime="
//         + this.getVariableYieldTime()
//         + ")";
//   }
//
//   public static enum GLDebugMessageSeverity {
//      HIGH(37190, 37190, 37190, 37190),
//      MEDIUM(37191, 37191, 37191, 37191),
//      LOW(37192, 37192, 37192, 37192),
//      NOTIFICATION(33387, 33387, -1, -1);
//
//      final int gl43;
//      final int khr;
//      final int arb;
//      final int amd;
//
//      private GLDebugMessageSeverity(int gl43, int khr, int arb, int amd) {
//         this.gl43 = gl43;
//         this.khr = khr;
//         this.arb = arb;
//         this.amd = amd;
//      }
//   }
//}
