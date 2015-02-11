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

    public static void setLocalDirectory(String title) {
        String os = System.getProperty("os.name");
        System.out.println(os);
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
            File f = new File(localDirectory+File.separator+path + ext);
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
            File f = new File(gameDirectory+File.separator+path + ext);
            if(f.exists()) {
                return new FileHandle(f);
            }
        }
        if(rtpDirectory != null) {
            for(String ext : extensions) {
                File f = new File(rtpDirectory + File.separator + path + ext);
                if(f.exists()) return new FileHandle(f);
            }
        }
        Gdx.app.log("FileUtil", "Tried to open "+path+" and failed.");
        return null;
    }

    public static RubyString rawLoadFile(String path) {
        path = path.replaceAll("\\\\", "/");
        if(path.startsWith("/")) {
            File f = new File(path);
            if(f.exists()) {
                //Gdx.app.log("FileUtil","Found "+path+" in Game folder.");
                return new RubyString(Ruby.getGlobalRuntime(), Ruby.getGlobalRuntime().getString(), new FileHandle(f).readBytes());
            }
            return null;
        }
        File file = new File(localDirectory + File.separator +path.replaceAll("\\\\", File.separator));
        if(file.exists()) {
            return new RubyString(Ruby.getGlobalRuntime(), Ruby.getGlobalRuntime().getString(), new FileHandle(file).readBytes());
        }
        if(archive != null) {
            String convertedPath = path.replaceAll("\\/", "\\\\");
            FileHandle f = archive.openFile(convertedPath);
            if( f != null) return new RubyString(Ruby.getGlobalRuntime(), Ruby.getGlobalRuntime().getString(), f.readBytes());
        }
        file = new File(gameDirectory + File.separator +path.replaceAll("\\\\", File.separator));
        if(file.exists()) {
            //Gdx.app.log("FileUtil","Found "+path+" in Game folder.");
            return new RubyString(Ruby.getGlobalRuntime(), Ruby.getGlobalRuntime().getString(), new FileHandle(file).readBytes());
        }
        return null;
    }

}
