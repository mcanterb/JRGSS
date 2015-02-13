package org.jrgss.api;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import org.jrgss.FileUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by matty on 6/27/14.
 */
public class Audio {

    public static final Object sync = new Object();

    static Music bgm;
    static String bgmFilename;
    static Music bgs;
    static Music me;
    static Sound se;
    static boolean mePlaying = false;

    static Map<String, Sound> se_cache = new HashMap<>();

    public static void bgm_play(String filename) {
        bgm_play(filename, 100, 100, 0);
    }

    public static void bgm_play(String filename, int volume) {
        bgm_play(filename, volume, 100, 0);
    }

    public static void bgm_play(String filename, int volume, int pitch) {
        bgm_play(filename, volume, pitch, 0);
    }

    public synchronized static void bgm_play(String filename, int volume, int pitch, int pos) {
        Gdx.app.log("Audio", "Play BGM "+filename+" @ volume ="+ volume+", pitch ="+pitch+", pos = "+pos);

        if(bgm != null && !bgmFilename.equals(filename)) {
            bgm_stop();
        }
        if(bgm == null) {
            bgm = Gdx.audio.newMusic(FileUtil.loadAudio(filename));
            bgm.setLooping(true);
            bgmFilename = filename;
            if(!mePlaying) {
                synchronized (sync) {
                    bgm.play();
                }
            }
        }
        bgm.setVolume(Math.min(1.0f,volume/100f));

    }

    public static void bgs_play(String filename) {
        bgs_play(filename, 100, 100, 0);
    }

    public static void bgs_play(String filename, int volume) {
        bgs_play(filename, volume, 100, 0);
    }

    public static void bgs_play(String filename, int volume, int pitch) {
        bgs_play(filename, volume, pitch, 0);
    }

    public synchronized static void bgs_play(String filename, int volume, int pitch, int pos) {
        if(bgs != null) {
            bgs_stop();
        }
        Gdx.app.log("Audio", "Play BGS "+filename+" @ volume ="+ volume+", pitch ="+pitch+", pos = "+pos);
        bgs = Gdx.audio.newMusic(FileUtil.loadAudio(filename));
        bgs.setLooping(true);
        bgs.setVolume(volume/100f);
        if(!mePlaying) {
            synchronized (sync) {
                bgs.play();
            }
        }
    }

    public static void me_play(String filename) {
        me_play(filename, 100,100);
    }

    public static void me_play(String filename, int volume) {
        me_play(filename, volume, 100);
    }

    public synchronized static void me_play(String filename, int volume, int pitch) {
        if(bgm != null) {
            bgm.pause();
        }
        me = Gdx.audio.newMusic(FileUtil.loadAudio(filename));
        me.setOnCompletionListener(new Music.OnCompletionListener() {
            @Override
            public void onCompletion(Music music) {
                if(bgm != null) {
                    bgm.play();
                }
                mePlaying = false;
            }
        });
        me.setLooping(false);
        me.setVolume(volume/100f);
        synchronized (sync) {
            me.play();
        }
        mePlaying = true;
    }

    public static void se_play(String filename) {
        se_play(filename, 100, 100);
    }

    public static void se_play(String filename, int volume) {
        se_play(filename, volume, 100);
    }

    public synchronized static void se_play(String filename, int volume, int pitch) {
        Gdx.app.log("Audio", "Play SE "+filename+" @ volume ="+ volume+", pitch ="+pitch);
        Sound se = se_cache.get(filename);
        if(se == null) {
            se = Gdx.audio.newSound(FileUtil.loadAudio(filename));
            se_cache.put(filename, se);
        }
        Audio.se = se;
        synchronized (sync) {
            se.play(volume / 100f, pitch / 100f, 0);
        }

    }

    public static void se_stop() {
        se.stop();
    }

    public static void bgm_fade(int millis) {
        bgm_stop();
    }

    public static void bgm_stop() {
        if(bgm != null) {
            bgm.stop();
            bgm.dispose();
            bgm = null;
        }
    }

    public static int bgm_pos() {
        return 0;
    }

    public static int bgs_pos() {
        return 0;
    }



    public static void bgs_stop() {
        if(bgs != null) {
            bgs.stop();
            bgs.dispose();
            bgs = null;
        }
    }
    public static void me_stop() {}

    public static void bgs_fade(int millis) {}
    public static void me_fade(int millis) {}

}
