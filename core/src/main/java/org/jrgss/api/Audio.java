package org.jrgss.api;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.audio.Music.OnCompletionListener;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.GdxRuntimeException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.jrgss.FileUtil;
import org.jrgss.JRGSSApplication;

public class Audio {
   static Music bgm;
   static String bgmFilename;
   static Music bgs;
   static String bgsFilename;
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

   public static synchronized void bgm_play(String filename, int volume, int pitch, int pos) {
      if (bgm != null && !bgmFilename.equals(filename)) {
         bgm_stop();
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

   public static void bgs_play(String filename) {
      bgs_play(filename, 100, 100, 0);
   }

   public static void bgs_play(String filename, int volume) {
      bgs_play(filename, volume, 100, 0);
   }

   public static void bgs_play(String filename, int volume, int pitch) {
      bgs_play(filename, volume, pitch, 0);
   }

   public static synchronized void bgs_play(String filename, int volume, int pitch, int pos) {
      if (bgs != null && !bgsFilename.equals(filename)) {
         bgs_stop();
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

   public static void me_play(String filename) {
      me_play(filename, 100, 100);
   }

   public static void me_play(String filename, int volume) {
      me_play(filename, volume, 100);
   }

   public static synchronized void me_play(String filename, int volume, int pitch) {
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

   public static void se_play(String filename) {
      se_play(filename, 100, 100);
   }

   public static void se_play(String filename, int volume) {
      se_play(filename, volume, 100);
   }

   public static synchronized void se_play(String filename, int volume, int pitch) {
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

   public static synchronized void se_stop() {
      se.stop();
   }

   public static synchronized void bgm_fade(int millis) {
      if (bgm != null) {
         long start = System.nanoTime();
         float startingVolume = bgm.getVolume();
         ((JRGSSApplication)Gdx.app).addAudioUpdater(() -> {
            if (bgm == null) {
               return true;
            } else {
               float fadeStep = Math.max(0.0F, (millis - (float)TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)) / millis);
               bgm.setVolume(fadeStep * startingVolume);
               if (fadeStep == 0.0F) {
                  bgm.stop();
                  bgm.dispose();
                  bgm = null;
                  return true;
               } else {
                  return false;
               }
            }
         });
      }
   }

   public static synchronized void bgm_stop() {
      if (bgm != null) {
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

   public static synchronized void bgs_stop() {
      if (bgs != null) {
         bgs.stop();
         bgs.dispose();
         bgs = null;
      }
   }

   public static synchronized void me_stop() {
      if (me != null) {
         me.stop();
         me.dispose();
         me = null;
      }
   }

   public static synchronized void bgs_fade(int millis) {
      if (bgs != null) {
         long start = System.nanoTime();
         float startingVolume = bgs.getVolume();
         ((JRGSSApplication)Gdx.app).addAudioUpdater(() -> {
            if (bgs == null) {
               return true;
            } else {
               float fadeStep = Math.max(0.0F, (millis - (float)TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)) / millis);
               bgs.setVolume(fadeStep * startingVolume);
               if (fadeStep == 0.0F) {
                  bgs.stop();
                  bgs.dispose();
                  bgs = null;
                  return true;
               } else {
                  return false;
               }
            }
         });
      }
   }

   public static synchronized void me_fade(int millis) {
      if (me != null) {
         long start = System.nanoTime();
         float startingVolume = me.getVolume();
         ((JRGSSApplication)Gdx.app).addAudioUpdater(() -> {
            if (me == null) {
               return true;
            } else {
               float fadeStep = Math.max(0.0F, (millis - (float)TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)) / millis);
               me.setVolume(fadeStep * startingVolume);
               if (fadeStep == 0.0F) {
                  me.stop();
                  me.dispose();
                  me = null;
                  return true;
               } else {
                  return false;
               }
            }
         });
      }
   }
}
