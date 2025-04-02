package org.jrgss.api;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Music.OnCompletionListener;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.GdxRuntimeException;
import org.jrgss.FileUtil;
import org.jrgss.JRGSSApplication;
import org.jrgss.api.win32.Win32Util;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@JRubyModule(name = "Audio")
public class Audio extends RubyObject {
    private static final Map<String, Sound> se_cache = new HashMap<>();
    static Ruby runtime;
    static RubyModule rubyModule;
    private static Music bgm;
    private static final Runnable SET_BGM_NULL = () -> bgm = null;
    private static String bgmFilename;
    private static Music bgs;
    private static final Runnable SET_BGS_NULL = () -> bgs = null;
    private static String bgsFilename;
    private static Music me;
    private static final Runnable SET_ME_NULL = () -> me = null;
    private static Sound se;
    private static boolean mePlaying = false;

    public Audio(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass);
    }

    public Audio(RubyClass metaClass) {
        super(metaClass);
    }

    public static synchronized void bgmPlay(String filename, int volume, int pitch, int pos) {
        if (bgm != null && !bgmFilename.equals(filename)) {
            bgmStop();
        }

        if (bgm == null) {
            bgm = Gdx.audio.newMusic(FileUtil.loadAudio(filename));
            bgm.setLooping(true);
            bgmFilename = filename;
            if (!mePlaying) {
                bgm.play();
            }
        }

        bgm.setVolume(Math.min(1.0F, volume / 100.0F));
    }

    @JRubyMethod(
        name = {"bgm_play"},
        required = 1,
        optional = 4,
        module = true
    )
    public static IRubyObject bgmPlay(IRubyObject self, IRubyObject[] args) {
        runPlayFunc(args, Audio::bgmPlay);
        return runtime.getNil();
    }

    public static synchronized void bgsPlay(String filename, int volume, int pitch, int pos) {
        if (bgs != null && !bgsFilename.equals(filename)) {
            bgsStop();
        }

        if (bgs == null) {
            bgs = Gdx.audio.newMusic(FileUtil.loadAudio(filename));
            bgs.setLooping(true);
            bgsFilename = filename;
            if (!mePlaying) {
                try {
                    bgs.play();
                } catch (GdxRuntimeException var5) {
                    var5.printStackTrace(System.err);
                }
            }
        }

        bgs.setVolume(Math.min(1.0F, volume / 100.0F));
    }

    @JRubyMethod(
        name = {"bgs_play"},
        required = 1,
        optional = 4,
        module = true
    )
    public static IRubyObject bgsPlay(IRubyObject self, IRubyObject[] args) {
        runPlayFunc(args, Audio::bgsPlay);
        return runtime.getNil();
    }

    public static synchronized void mePlay(String filename, int volume, int pitch) {
        if (bgm != null) {
            bgm.pause();
        }

        me = Gdx.audio.newMusic(FileUtil.loadAudio(filename));
        me.setOnCompletionListener(new OnCompletionListener() {
            @Override
            public void onCompletion(Music music) {
                if (Audio.bgm != null) {
                    Audio.bgm.play();
                }

                Audio.mePlaying = false;
            }
        });
        me.setLooping(false);
        me.setVolume(volume / 100.0F);
        me.play();
        mePlaying = true;
    }

    @JRubyMethod(
        name = {"me_play"},
        required = 1,
        optional = 3,
        module = true
    )
    public static IRubyObject mePlay(IRubyObject self, IRubyObject[] args) {
        runPlayFunc(args, (fname, vol, pitch, ignored) -> mePlay(fname, vol, pitch));
        return runtime.getNil();
    }

    public static synchronized void sePlay(String filename, int volume, int pitch) {
        Sound se = se_cache.get(filename);
        if (se == null) {
            FileHandle fileHandle = FileUtil.loadAudio(filename);
            if (fileHandle == null) {
                throw new RuntimeException("Failed to load sound file: " + filename);
            }

            se = Gdx.audio.newSound(FileUtil.loadAudio(filename));
            se_cache.put(filename, se);
            Gdx.app.log("Audio", "Loaded new sound " + filename);
        }

        Audio.se = se;
        se.play(volume / 100.0F, pitch / 100.0F, 0.0F);
    }

    @JRubyMethod(
        name = {"se_play"},
        required = 1,
        optional = 3,
        module = true
    )
    public static IRubyObject sePlay(IRubyObject self, IRubyObject[] args) {
        runPlayFunc(args, (fname, vol, pitch, ignored) -> sePlay(fname, vol, pitch));
        return runtime.getNil();
    }

    public static synchronized void seStop() {
        se.stop();
    }

    @JRubyMethod(name = "se_stop", module = true)
    public static synchronized IRubyObject seStop(IRubyObject self) {
        seStop();
        return runtime.getNil();
    }

    public static synchronized void bgmFade(int millis) {
        runFade(millis, bgm, SET_BGM_NULL);
    }

    @JRubyMethod(name = "bgm_fade", module = true)
    public static synchronized IRubyObject bgmFade(IRubyObject self, IRubyObject arg) {
        bgmFade(Win32Util.getInt(arg));
        return runtime.getNil();
    }

    public static synchronized void bgmStop() {
        if (bgm != null) {
            bgm.stop();
            bgm.dispose();
            bgm = null;
        }
    }

    @JRubyMethod(name = "bgm_stop", module = true)
    public static synchronized IRubyObject bgmStop(IRubyObject self) {
        bgmStop();
        return runtime.getNil();
    }

    @JRubyMethod(name = "bgm_pos", module = true)
    public static IRubyObject bgmPos(IRubyObject self) {
        return Win32Util.rubyNum(0);
    }

    @JRubyMethod(name = "bgs_pos", module = true)
    public static IRubyObject bgsPos(IRubyObject self) {
        return Win32Util.rubyNum(0);
    }

    public static synchronized void bgsStop() {
        if (bgs != null) {
            bgs.stop();
            bgs.dispose();
            bgs = null;
        }
    }

    @JRubyMethod(name = "bgs_stop", module = true)
    public static synchronized IRubyObject bgsStop(IRubyObject self) {
        bgsStop();
        return runtime.getNil();
    }

    public static synchronized void meStop() {
        if (me != null) {
            me.stop();
            me.dispose();
            me = null;
        }
    }

    @JRubyMethod(name = "me_stop", module = true)
    public static synchronized IRubyObject meStop(IRubyObject self) {
        meStop();
        return runtime.getNil();
    }

    public static synchronized void bgsFade(int millis) {
        runFade(millis, bgs, SET_BGS_NULL);
    }

    @JRubyMethod(name = "bgs_fade", module = true)
    public static synchronized IRubyObject bgsFade(IRubyObject self, IRubyObject arg) {
        bgsFade(Win32Util.getInt(arg));
        return runtime.getNil();
    }

    public static synchronized void meFade(int millis) {
        runFade(millis, me, SET_ME_NULL);
    }

    @JRubyMethod(name = "me_fade", module = true)
    public static synchronized IRubyObject meFade(IRubyObject self, IRubyObject arg) {
        meFade(Win32Util.getInt(arg));
        return runtime.getNil();
    }

    private static void runFade(int millis, Music music, Runnable nullSetter) {
        if (music != null) {
            long start = System.nanoTime();
            float startingVolume = music.getVolume();
            ((JRGSSApplication) Gdx.app).addAudioUpdater(() -> {
                if (music == null) {
                    return true;
                } else {
                    float fadeStep = Math.max(0.0F, (millis - (float) TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)) / millis);
                    music.setVolume(fadeStep * startingVolume);
                    if (fadeStep == 0.0F) {
                        music.stop();
                        music.dispose();
                        nullSetter.run();
                        return true;
                    } else {
                        return false;
                    }
                }
            });
        }
    }

    private static void runPlayFunc(IRubyObject[] args, AudioPlayFunction audioPlayFunc) {
        String filename = Win32Util.getString(args[0]);
        int volume = 100;
        int pitch = 100;
        int pos = 0;
        if (args.length > 1) {
            volume = Win32Util.getInt(args[1]);
        }
        if (args.length > 2) {
            pitch = Win32Util.getInt(args[2]);
        }
        if (args.length > 3) {
            pos = Win32Util.getInt(args[3]);
        }
        audioPlayFunc.play(filename, volume, pitch, pos);
    }

    private interface AudioPlayFunction {
        void play(String filename, int volume, int pitch, int pos);
    }
}
