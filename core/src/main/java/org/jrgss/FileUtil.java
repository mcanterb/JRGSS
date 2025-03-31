package org.jrgss;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.jrgss.rgssa.EncryptedArchive;
import org.jruby.Ruby;
import org.jruby.RubyString;

public class FileUtil {
   static final String[] IMG_EXTENSIONS = new String[]{"", ".bmp", ".png", ".jpg", ".jpeg", ".jpg.png"};
   static final String[] AUDIO_EXTENSIONS = new String[]{"", ".ogg", ".ogx", ".wav", ".mp3"};
   static EncryptedArchive archive = null;
   public static String gameDirectory = null;
   public static String rtpDirectory = null;
   public static String localDirectory;
   public static boolean onCaseSensitiveFileSystem = false;
   private static Map<Path, Optional<Path>> insensitiveFileNameCache = new HashMap<>();

   public static String[] getBootstrapPaths() {
      return new String[]{localDirectory.equals("." + File.separator) ? "" : localDirectory, gameDirectory.equals("." + File.separator) ? "" : gameDirectory};
   }

   public static void setLocalDirectory(String title) {
      String os = System.getProperty("os.name");
      System.out.println("Running on " + os);
      if (os.contains("Mac")) {
         localDirectory = System.getProperty("user.home") + "/Library/Application Support/JRGSS/" + title + "/";
      } else {
         if (os.contains("Windows") && System.getenv("APPDATA") != null) {
            localDirectory = System.getProperty("user.dir") + "\\";
            return;
         }

         localDirectory = System.getProperty("user.home") + File.separator + ".jrgss" + File.separator + title + File.separator;
      }

      File dir = new File(localDirectory);
      dir.mkdirs();
      System.out.println("Using " + localDirectory + " as local directory");
   }

   public static void setEncryptedArchive(EncryptedArchive archive) {
      FileUtil.archive = archive;
   }

   public static void setRTPDirectory(String rtpDirectory) {
      FileUtil.rtpDirectory = rtpDirectory;
   }

   public static void setGameDirectory(String gameDirectory) {
      FileUtil.gameDirectory = gameDirectory;
   }

   public static FileHandle loadImg(String path) {
      return loadWithExtensions(path, IMG_EXTENSIONS);
   }

   public static FileHandle loadAudio(String path) {
      FileHandle f = loadWithExtensions(path, AUDIO_EXTENSIONS);
      if (f == null) {
         Gdx.app.log("FileUtil", "Tried to load " + path + " and failed!");
      }

      return f;
   }

   public static FileHandle loadWithExtensions(String path, String[] extensions) {
      if (!File.separator.equals("\\")) {
         path = path.replaceAll("\\\\", File.separator);
      }

      for (String ext : extensions) {
         File f = new File(caseInsensitiveLookup(localDirectory + File.separator + path + ext));
         if (f.exists()) {
            return new FileHandle(f);
         }
      }

      if (archive != null) {
         String convertedPath = path.replaceAll("/", "\\\\");

         for (String extx : extensions) {
            FileHandle f = archive.openFile(convertedPath + extx);
            if (f != null) {
               return f;
            }
         }
      }

      for (String extxx : extensions) {
         File f = new File(caseInsensitiveLookup(gameDirectory + File.separator + path + extxx));
         if (f.exists()) {
            return new FileHandle(f);
         }
      }

      if (rtpDirectory != null) {
         for (String extxxx : extensions) {
            File f = new File(caseInsensitiveLookup(rtpDirectory + File.separator + path + extxxx));
            if (f.exists()) {
               return new FileHandle(f);
            }
         }
      }

      Gdx.app.log("FileUtil", "Tried to open " + path + " and failed.");
      return null;
   }

   public static RubyString rawLoadFile(String path) {
      if (archive != null) {
         String convertedPath = path.replaceAll("/", "\\\\");
         FileHandle f = archive.openFile(convertedPath);
         if (f != null) {
            return new RubyString(Ruby.getGlobalRuntime(), Ruby.getGlobalRuntime().getString(), f.readBytes());
         }
      }

      if (!File.separator.equals("\\")) {
         path = path.replaceAll("\\\\", File.separator);
      }

      if (path.startsWith(File.separator)) {
         File f = new File(caseInsensitiveLookup(path));
         return f.exists() ? new RubyString(Ruby.getGlobalRuntime(), Ruby.getGlobalRuntime().getString(), new FileHandle(f).readBytes()) : null;
      } else {
         File file = new File(caseInsensitiveLookup(localDirectory + File.separator + path));
         if (file.exists()) {
            return new RubyString(Ruby.getGlobalRuntime(), Ruby.getGlobalRuntime().getString(), new FileHandle(file).readBytes());
         } else {
            file = new File(caseInsensitiveLookup(gameDirectory + File.separator + path));
            return file.exists() ? new RubyString(Ruby.getGlobalRuntime(), Ruby.getGlobalRuntime().getString(), new FileHandle(file).readBytes()) : null;
         }
      }
   }

   public static String caseInsensitiveLookup(String original) {
      if (!onCaseSensitiveFileSystem) {
         return original;
      } else {
         Path p = FileSystems.getDefault().getPath(original);
         Path correct = caseInsensitiveLookupImpl(p);
         return correct == null ? original : correct.toString();
      }
   }

   private static Path caseInsensitiveLookupImpl(Path originalPath) {
      Path path = originalPath.toAbsolutePath();
      Optional<Path> fromCache = insensitiveFileNameCache.get(path);
      if (fromCache != null) {
         return fromCache.orElse(null);
      } else if (path.toFile().exists()) {
         insensitiveFileNameCache.put(path, Optional.of(path));
         return path;
      } else {
         Path parent = caseInsensitiveLookupImpl(path.getParent());
         if (parent == null) {
            insensitiveFileNameCache.put(path, Optional.empty());
            return null;
         } else {
            String file = path.getFileName().toString().toLowerCase();

            for (String files : parent.toFile().list()) {
               if (files.toLowerCase().equals(file)) {
                  Path correctPath = parent.resolve(files);
                  insensitiveFileNameCache.put(path, Optional.of(correctPath));
                  return correctPath;
               }
            }

            insensitiveFileNameCache.put(path, Optional.empty());
            return null;
         }
      }
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof FileUtil)) {
         return false;
      } else {
         FileUtil other = (FileUtil)o;
         return other.canEqual(this);
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof FileUtil;
   }

   @Override
   public int hashCode() {
      return 1;
   }

   @Override
   public String toString() {
      return "FileUtil()";
   }
}
