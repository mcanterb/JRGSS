//package org.jrgss;
//
//import lwjgl3.JRGSSDesktop;
//import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
//import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration.HdpiMode;
//import java.io.File;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.io.PrintWriter;
//import org.lwjgl.util.tinyfd.TinyFileDialogs;
//
//public class Desktop {
//   public static void main(String[] args) {
//      String dir = gameDir(args);
//      String rtp = rtpDir(args);
//
//      try {
//         if (dir.charAt(dir.length() - 1) != File.separatorChar) {
//            dir = dir + File.separatorChar;
//         }
//
//         Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
//         cfg.setWindowIcon(new String[]{"icon/icon16.png", "icon/icon32.png", "icon/icon48.png"});
//         cfg.setTitle("Title");
//         cfg.setWindowedMode(800, 450);
//         cfg.setHdpiMode(HdpiMode.Pixels);
//         cfg.setResizable(false);
//         if (System.getProperty("jrgss.icon") != null) {
//         }
//
//         if (System.getProperty("os.name").toLowerCase().contains("linux")) {
//            FileUtil.onCaseSensitiveFileSystem = true;
//            System.out.println("Using Case Insensitive File lookups!");
//         }
//
//         System.out.println("Reading INI from " + dir + "Game.ini");
//         ConfigReader config = new ConfigReader(dir + "Game.ini");
//         cfg.setTitle(config.getTitle());
//         cfg.useOpenGL3(true, 3, 2);
//         int logLevel = 2;
//         if (shouldOpenConsole(args)) {
//            logLevel = 0;
//         }
//
//         new JRGSSDesktop(new JRGSSGame(dir, rtp, config, isTesting(args)), cfg, logLevel);
//      } catch (Exception var19) {
//         Exception e = var19;
//         var19.printStackTrace(System.err);
//         TinyFileDialogs.tinyfd_messageBox("Error", var19.getMessage(), "ok", "error", true);
//
//         try (FileWriter writer = new FileWriter(FileUtil.localDirectory + File.separator + "JrgssCrashLog" + System.currentTimeMillis() + ".log")) {
//            e.printStackTrace(new PrintWriter(writer));
//         } catch (IOException var18) {
//            TinyFileDialogs.tinyfd_messageBox("Error", "Failed to write crash log! " + var18.getMessage(), "ok", "error", true);
//         }
//      }
//   }
//
//   private static String gameDir(String[] args) {
//      String result = System.getProperty("user.dir");
//
//      for (String arg : args) {
//         if (!arg.toLowerCase().equals("test") && !arg.toLowerCase().equals("console")) {
//            return arg;
//         }
//      }
//
//      return result;
//   }
//
//   private static String rtpDir(String[] args) {
//      String result = System.getProperty("user.dir");
//
//      for (String arg : args) {
//         if (!arg.toLowerCase().equals("test") && !arg.toLowerCase().equals("console")) {
//            result = arg;
//         }
//      }
//
//      return result;
//   }
//
//   private static boolean isTesting(String[] args) {
//      for (String arg : args) {
//         if (arg.toLowerCase().equals("test")) {
//            return true;
//         }
//      }
//
//      return false;
//   }
//
//   private static boolean shouldOpenConsole(String[] args) {
//      for (String arg : args) {
//         if (arg.toLowerCase().equals("console")) {
//            return true;
//         }
//      }
//
//      return false;
//   }
//}
