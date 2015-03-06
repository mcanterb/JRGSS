package org.jrgss;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import lombok.Data;
import org.jrgss.rgssa.EncryptedArchive;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyFile;
import org.jruby.RubyString;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author matt
 * @date 7/4/14
 */
@Data
public class FileUtil {

    final static String[] IMG_EXTENSIONS = new String[] {".bmp", ".png", ".jpg", ".jpeg", ".jpg.png"};
    final static String[] AUDIO_EXTENSIONS = new String[] {".ogg",".ogx", ".wav", ".mp3"};
    static EncryptedArchive archive = null;
    public static String gameDirectory = null;
    public static String rtpDirectory = null;
    public static String localDirectory;
    public static boolean onCaseSensitiveFileSystem = false;

    public static void setLocalDirectory(String title) {
        String os = System.getProperty("os.name");
        System.out.println("Running on "+os);
        if(os.contains("Mac")) {
            localDirectory = System.getProperty("user.home")+"/Library/Application Support/JRGSS/"+title+"/";
        } else if(os.contains("Windows") && System.getenv("APPDATA") != null) {
            localDirectory = System.getenv("APPDATA")+"\\JRGSS\\"+title+"\\";
        } else {
            localDirectory = System.getProperty("user.home")+File.separator+".jrgss"+File.separator
                            + title + File.separator;
        }
        File dir = new File(localDirectory);
        dir.mkdirs();
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
        return loadWithExtensions(path, AUDIO_EXTENSIONS);
    }

    public static FileHandle loadWithExtensions(String path, String[] extensions) {
        for(String ext : extensions) {
            File f = new File(caseInsensitiveLookup(localDirectory+File.separator+path + ext));
            if(f.exists()) {
                return new FileHandle(f);
            }
        }
        if(archive != null) {
            String convertedPath = path.replaceAll("/", "\\\\");
            for(String ext : extensions) {
                FileHandle f = archive.openFile(convertedPath+ext);
                if( f != null) return f;
            }
        }
        for(String ext : extensions) {
            File f = new File(caseInsensitiveLookup(gameDirectory+File.separator+path + ext));
            if(f.exists()) {
                return new FileHandle(f);
            }
        }
        if(rtpDirectory != null) {
            for(String ext : extensions) {
                File f = new File(caseInsensitiveLookup(rtpDirectory + File.separator + path + ext));
                if(f.exists()) return new FileHandle(f);
            }
        }
        Gdx.app.log("FileUtil", "Tried to open "+path+" and failed.");
        return null;
    }

    public static RubyString rawLoadFile(String path) {
        path = path.replaceAll("\\\\", File.separator);
        if(path.startsWith(File.separator)) {
            File f = new File(path);
            if(f.exists()) {
                //Gdx.app.log("FileUtil","Found "+path+" in Game folder.");
                return new RubyString(Ruby.getGlobalRuntime(), Ruby.getGlobalRuntime().getString(), new FileHandle(f).readBytes());
            }
            return null;
        }

        File file = new File(caseInsensitiveLookup(localDirectory + File.separator +path.replaceAll("\\\\", File.separator)));
        if(file.exists()) {
            return new RubyString(Ruby.getGlobalRuntime(), Ruby.getGlobalRuntime().getString(), new FileHandle(file).readBytes());
        }
        if(archive != null) {
            String convertedPath = path.replaceAll("\\/", "\\\\");
            FileHandle f = archive.openFile(convertedPath);
            if( f != null) return new RubyString(Ruby.getGlobalRuntime(), Ruby.getGlobalRuntime().getString(), f.readBytes());
        }
        file = new File(caseInsensitiveLookup(gameDirectory + File.separator +path.replaceAll("\\\\", File.separator)));
        if(file.exists()) {
            //Gdx.app.log("FileUtil","Found "+path+" in Game folder.");
            return new RubyString(Ruby.getGlobalRuntime(), Ruby.getGlobalRuntime().getString(), new FileHandle(file).readBytes());
        }
        return null;
    }


    public static String caseInsensitiveLookup(String original) {
        if(!onCaseSensitiveFileSystem) return original;
        Path p = FileSystems.getDefault().getPath(original);
        Path correct = caseInsensitiveLookupImpl(p);
        if(correct == null) return original;
        return correct.toString();
    }

    private static Map<Path, Optional<Path>> insensitiveFileNameCache = new HashMap<>();

    private static Path caseInsensitiveLookupImpl(Path originalPath) {
        Path path = originalPath.toAbsolutePath();
        Optional<Path> fromCache = insensitiveFileNameCache.get(path);
        if(fromCache != null) return fromCache.orElse(null);
        if(path.toFile().exists()) {
            insensitiveFileNameCache.put(path, Optional.of(path));
            return path;
        }
        //Gdx.app.log("FileUtil", "Looking up "+path);
        Path parent = caseInsensitiveLookupImpl(path.getParent());
        if(parent == null) {
            insensitiveFileNameCache.put(path, Optional.<Path>empty());
            return null;
        }
        String file = path.getFileName().toString().toLowerCase();
        for(String files : parent.toFile().list()) {
            if(files.toLowerCase().equals(file)) {
                Path correctPath = parent.resolve(files);
                insensitiveFileNameCache.put(path, Optional.of(correctPath));
                return correctPath;
            }
        }
        insensitiveFileNameCache.put(path, Optional.<Path>empty());
        return null;
    }

    public static void main(String []args) {
        onCaseSensitiveFileSystem = true;
        Path p = FileSystems.getDefault().getPath(args[0]);
        Path correct = caseInsensitiveLookupImpl(p);
        System.out.println(correct);
    }


}
