//package org.jrgss;
//
//import lwjgl3.JRGSSHeadlessDesktop;
//import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
//import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration.HdpiMode;
//import java.io.File;
//import org.lwjgl.util.tinyfd.TinyFileDialogs;
//
//public class SaveEditor {
//   public static void main(String[] args) {
//      String dir = args.length == 0 ? System.getProperty("user.dir") : args[0];
//
//      try {
//         Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
//         cfg.setTitle("Title");
//         cfg.setWindowedMode(800, 450);
//         cfg.setHdpiMode(HdpiMode.Logical);
//         cfg.setResizable(false);
//         if (System.getProperty("jrgss.icon") != null) {
//         }
//
//         if (System.getProperty("os.name").toLowerCase().contains("linux")) {
//            FileUtil.onCaseSensitiveFileSystem = true;
//            System.out.println("Using Case Insensitive File lookups!");
//         }
//
//         System.out.println("Reading INI from " + dir + File.separator + "Game.ini");
//         ConfigReader config = new ConfigReader(dir + File.separator + "Game.ini");
//         cfg.setTitle(config.getTitle());
//         int logLevel = 2;
//         new JRGSSHeadlessDesktop(new JRGSSGame(dir, args[1], config, true));
//      } catch (Exception var6) {
//         var6.printStackTrace(System.err);
//         TinyFileDialogs.tinyfd_messageBox("Error", var6.getMessage(), "ok", "error", true);
//      }
//   }
//}
