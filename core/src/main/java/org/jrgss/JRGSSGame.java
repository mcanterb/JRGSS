package org.jrgss;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.FPSLogger;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.JrgssBatch;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.FutureTask;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.jcodings.Encoding;
import org.jrgss.api.Graphics;
import org.jrgss.api.xbox.XboxControllers;
import org.jrgss.rgssa.EncryptedArchive;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyMarshal;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.RubyInstanceConfig.CompileMode;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.embed.ScriptingContainer;
import org.jruby.runtime.ThreadContext;

public class JRGSSGame implements JRGSSApplicationListener {
   private static final String LOADING_IMAGE = "Graphics\\Titles2\\Logo";
   private ScriptingContainer scriptingContainer;
   private final String[] BUILTIN_MODULES = new String[]{"Audio", "Graphics"};
   private final String[] BUILTIN_CLASSES = new String[]{
      "Bitmap", "Plane", "Rect", "RGSSError", "Sprite", "Tilemap", "Tone", "Viewport", "Window", "RGSSReset"
   };
   public static final Queue<FutureTask<?>> glRunnables = new ConcurrentLinkedQueue<>();
   boolean testing = false;
   static JRGSSGame.JRGSSMain mainBlock;
   JrgssBatch batch;
   public static OrthographicCamera camera;
   FPSLogger fpsLogger;
   static Thread glThread;
   static ConfigReader ini;

   public static boolean isRenderThread() {
      return Thread.currentThread() == glThread;
   }

   public JRGSSGame(String gameDirectory, String rtpDirectory, ConfigReader ini, boolean test) {
      JRGSSGame.ini = ini;
      this.testing = test;
      FileUtil.setLocalDirectory(ini.getTitle());
      RGSSVersion rgss = ini.getRGSSVersion();
      FileUtil.setGameDirectory(gameDirectory);
      FileUtil.setRTPDirectory(rtpDirectory);
      File encryptedArchiveFile = new File(gameDirectory + File.separator + "Game.rgss3a");
      if (encryptedArchiveFile.exists()) {
         try {
            EncryptedArchive archive = new EncryptedArchive(gameDirectory + File.separator + "Game.rgss3a");
            FileUtil.setEncryptedArchive(archive);
         } catch (Exception var8) {
            System.err.println("Could not load archive!");
            var8.printStackTrace(System.err);
         }
      }
   }

   public void loadScriptsFromDirectory(String path) {
      File[] fileList = new File(path).listFiles();
      File[] orderedList = new File[fileList.length];

      for (File f : fileList) {
         String fileName = f.getName();
         String[] nameParts = fileName.split("@@__@@");
         orderedList[Integer.parseInt(nameParts[0])] = f;
      }

      for (File f : orderedList) {
         try {
            this.scriptingContainer.runScriptlet(new FileReader(f), f.getName().split("@@__@@")[1]);
         } catch (Exception var10) {
            throw new RuntimeException(var10);
         }
      }
   }

   public void loadScriptData(String path) {
      FileHandle f;
      if (FileUtil.archive != null) {
         f = FileUtil.archive.openFile(path);
      } else {
         Gdx.app.log("JRGSSGame", "Separator is " + File.separator);
         if (!File.separator.equals("\\")) {
            path = path.replaceAll("\\\\", File.separator);
         }

         f = new FileHandle(FileUtil.gameDirectory + File.separator + path);
      }

      byte[] bytes = f.readBytes();
      RubyString str = RubyString.newString(Ruby.getGlobalRuntime(), bytes);
      RubyArray arr = (RubyArray)RubyMarshal.load(ThreadContext.newContext(Ruby.getGlobalRuntime()), null, new RubyObject[]{str}, null);
      this.scriptingContainer.put("$RGSS_SCRIPTS", arr);

      for (Object o : arr) {
         RubyArray item = (RubyArray)o;
         String name = (String)item.get(1);
         if (!name.equals("Object Permanence")) {
            this.scriptingContainer.put("$__obj", item);
            String str2 = (String)this.scriptingContainer.runScriptlet("Zlib::Inflate.inflate($__obj[2]).force_encoding(\"utf-8\")");

            try {
               String script = "# encoding: UTF-8\n" + str2;
               this.scriptingContainer.setScriptFilename(name);
               this.scriptingContainer.runScriptlet("# encoding: UTF-8\r\n" + script);
            } catch (Exception var12) {
               Gdx.app.error("JRGSSGame", "Failed to load: " + name);
               throw new RuntimeException(var12);
            }

            this.scriptingContainer.put("$__obj", null);
         }
      }
   }

   private void loadRPGModule() {
      try {
         String[] files = new File(JRGSSGame.class.getResource("/rpg/").toURI()).list();

         for (String file : files) {
            InputStream stream = JRGSSGame.class.getResourceAsStream("/rpg/" + file);
            this.scriptingContainer.runScriptlet(stream, file);
         }
      } catch (Exception var8) {
         try {
            String jarFileLocation = JRGSSGame.class.getProtectionDomain().getCodeSource().getLocation().getFile();
            jarFileLocation = jarFileLocation.replace("%20", " ");
            JarFile file = new JarFile(jarFileLocation);
            Enumeration<JarEntry> entries = file.entries();

            while (entries.hasMoreElements()) {
               JarEntry entry = entries.nextElement();
               if (entry.getName().startsWith("rpg")) {
                  this.scriptingContainer.runScriptlet(file.getInputStream(entry), entry.getName());
               }
            }
         } catch (Exception var7) {
            throw new RuntimeException("Failed to load built in ruby scripts!", var7);
         }
      }
   }

   public static void jrgssMain(JRGSSGame.JRGSSMain rubyBlock) {
      mainBlock = rubyBlock;
   }

   public void loadRGSSModule(String name) {
      this.scriptingContainer.runScriptlet("java_import org.jrgss.api." + name);
      if (Arrays.binarySearch(this.BUILTIN_CLASSES, name) >= 0) {
         this.scriptingContainer.runScriptlet("class " + name + "\ndef _dump level\nself.dump\nend\nend");
      }
   }

   @Override
   public void create() {
      glThread = Thread.currentThread();
      camera = new OrthographicCamera(Graphics.getWidth(), Graphics.getHeight());
      camera.setToOrtho(true, Graphics.getWidth(), Graphics.getHeight());
      camera.update();
      this.fpsLogger = new FPSLogger();
      Gdx.graphics.setVSync(true);
      this.batch = new JrgssBatch();
      this.batch.enableBlending();
      XboxControllers.initialize();
   }

   @Override
   public void resize(int width, int height) {
      Graphics.resize_screen(Graphics.getWidth(), Graphics.getHeight());
      camera = new OrthographicCamera(Graphics.getWidth(), Graphics.getHeight());
      camera.setToOrtho(true, Graphics.getWidth(), Graphics.getHeight());
      camera.update();
   }

   @Override
   public void render() {
      this.batch.setProjectionMatrix(camera.combined);
      this.batch.setColor(1.0F, 1.0F, 1.0F, 1.0F);
      Graphics.render(this.batch);
   }

   @Override
   public void pause() {
   }

   @Override
   public void resume() {
   }

   @Override
   public void dispose() {
   }

   public static void runWithGLContext(Runnable runnable) {
      if (Thread.currentThread() != glThread) {
         ((JRGSSApplication)Gdx.app).runWithGLContext(runnable);
      } else {
         runnable.run();
      }
   }

   @Override
   public void loadSplashScreen() {
      FileHandle file = FileUtil.loadImg("Graphics\\Titles2\\Logo");
      if (file != null) {
         Pixmap img = new Pixmap(file);
         Texture t = new Texture(img);
         this.batch.setProjectionMatrix(camera.combined);
         this.batch.setColor(1.0F, 1.0F, 1.0F, 1.0F);
         this.batch.begin();
         this.batch.draw(t, 0.0F, (float)Graphics.getHeight(), (float)Graphics.getWidth(), (float)(-Graphics.getHeight()));
         this.batch.end();
      }
   }

   @Override
   public void loadScripts() {
      System.out.println("Using Build " + ((JRGSSApplication)Gdx.app).getBuildId());
      this.scriptingContainer = new ScriptingContainer(LocalContextScope.SINGLETON, LocalVariableBehavior.TRANSIENT);
      this.scriptingContainer.getProvider().getRuntime().setDefaultInternalEncoding(Encoding.load("UTF8"));
      this.scriptingContainer.getProvider().getRuntime().setDefaultExternalEncoding(Encoding.load("UTF8"));
      this.scriptingContainer.setCompileMode(CompileMode.JIT);
      this.scriptingContainer.setRunRubyInProcess(true);
      this.scriptingContainer.setAttribute("rewrite.java.trace", true);
      if (this.testing) {
         this.scriptingContainer.runScriptlet("$TEST=true");
      }

      this.scriptingContainer.runScriptlet("require 'java'");
      this.scriptingContainer.runScriptlet("require 'org/jrgss/api/RGSSBuiltin'");

      for (String module : this.BUILTIN_MODULES) {
         this.loadRGSSModule(module);
      }

      for (String module : this.BUILTIN_CLASSES) {
         this.loadRGSSModule(module);
      }

      this.loadRPGModule();
      this.scriptingContainer.put("$_jrgss_home", FileUtil.gameDirectory);
      this.scriptingContainer.put("$_jrgss_paths", FileUtil.getBootstrapPaths());
      this.scriptingContainer.put("$_jrgss_os", System.getProperty("os.name"));
      this.scriptingContainer.put("$_jrgss_build", ((JRGSSApplication)Gdx.app).getBuildId());
      this.loadScriptData(ini.getScripts());
   }

   @Override
   public JRGSSGame.JRGSSMain getMain() {
      return mainBlock;
   }

   public interface JRGSSMain {
      void main();
   }
}
