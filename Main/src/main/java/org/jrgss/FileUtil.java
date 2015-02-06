package org.jrgss;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jrgss.rgssa.EncryptedArchive;
import org.jruby.Ruby;
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
        if(archive != null) {
            String convertedPath = path.replaceAll("\\/", "\\\\");
            FileHandle f = archive.openFile(convertedPath);
            if( f != null) return new RubyString(Ruby.getGlobalRuntime(), Ruby.getGlobalRuntime().getString(), f.readBytes());
        }
        File f = new File(gameDirectory + File.separator +path.replaceAll("\\\\", File.separator));
        if(f.exists()) {
            //Gdx.app.log("FileUtil","Found "+path+" in Game folder.");
            return new RubyString(Ruby.getGlobalRuntime(), Ruby.getGlobalRuntime().getString(), new FileHandle(f).readBytes());
        }
        return null;
    }

}
