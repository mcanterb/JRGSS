package org.jrgss.api;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.jrgss.FileUtil;
import org.jrgss.JRGSSGame;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by matty on 6/27/14.
 */
@Data
public class Font {
    final static Map<FontCacheKey, BitmapFont> fontCache = new HashMap<>();
    int size;
    Color color = new Color(255, 255, 255, 255);
    boolean outline = true;
    Color out_color = new Color(0, 0, 0, 128);
    @Getter
    BitmapFont bitmapFont;
    boolean bold = false;
    boolean italic = false;
    boolean shadow = false;
    @Getter
    static int default_size = 24;
    String[] name;


    public Font() {
        this(default_name());
    }

    public Font(String[] name) {
        this(name, default_size);
    }

    public Font(String[] name, final int size) {
        this.size = size;
        this.name = name;
        if (fontCache.containsKey(new FontCacheKey(size, FileUtil.rtpDirectory + File.separator + "Fonts" + File.separator + "VL-Gothic-Regular.ttf"))) {
            bitmapFont = fontCache.get(new FontCacheKey(size, FileUtil.rtpDirectory + File.separator + "Fonts" + File.separator + "VL-Gothic-Regular.ttf"));
        } else {
            JRGSSGame.runWithGLContext(new Runnable() {
                @Override
                public void run() {
                    FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.absolute(FileUtil.rtpDirectory + File.separator + "Fonts" + File.separator + "VL-Gothic-Regular.ttf"));
                    FreeTypeFontGenerator.FreeTypeFontParameter param = new FreeTypeFontGenerator.FreeTypeFontParameter();
                    param.size = (int)(size *0.80);
                    param.flip = true;
                    param.genMipMaps = false;
                    bitmapFont = generator.generateFont(param);
                    fontCache.put(new FontCacheKey(size, FileUtil.rtpDirectory + File.separator + "Fonts" + File.separator + "VL-Gothic-Regular.ttf"), bitmapFont);
                    generator.dispose();
                }
            });
        }

    }

    public static void setDefault_size(int size) {
        Gdx.app.log("Font", "Setting default size to "+size);
        default_size = size;
    }

    public void setName(String[] name) {
        this.name = name;
    }

    public Color getColor() {
        return color;
    }



    public static String[] default_name() {
        return new String[]{"Verdana", "Arial", "Courier New"};
    }

    public static boolean default_bold() {
        return false;
    }

    public static boolean default_italic() {
        return false;
    }

    public static boolean default_shadow() {
        return false;
    }

    public static boolean default_outline() {
        return true;
    }

    public static Color default_color() {
        return new Color(255, 255, 255, 255);
    }

    public static Color default_out_color() {
        return new Color(0, 0, 0, 128);
    }

    public void setSize(final int size) {
        this.size = size;
        if (fontCache.containsKey(new FontCacheKey(size, FileUtil.rtpDirectory + File.separator + "Fonts" + File.separator + "VL-Gothic-Regular.ttf"))) {
            bitmapFont = fontCache.get(new FontCacheKey(size, FileUtil.rtpDirectory + File.separator + "Fonts" + File.separator + "VL-Gothic-Regular.ttf"));
        } else {
            JRGSSGame.runWithGLContext(new Runnable() {
                @Override
                public void run() {
                    FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.absolute(FileUtil.rtpDirectory + File.separator + "Fonts" + File.separator + "VL-Gothic-Regular.ttf"));
                    FreeTypeFontGenerator.FreeTypeFontParameter param = new FreeTypeFontGenerator.FreeTypeFontParameter();
                    param.size = (int)(size *0.80);
                    param.flip = true;
                    param.genMipMaps = false;
                    bitmapFont = generator.generateFont(param);
                    fontCache.put(new FontCacheKey(size, FileUtil.rtpDirectory + File.separator + "Fonts" + File.separator + "VL-Gothic-Regular.ttf"), bitmapFont);
                    generator.dispose();

                }
            });
        }

    }

    @Data
    private static class FontCacheKey {
        final int size;
        final String name;
    }

}
