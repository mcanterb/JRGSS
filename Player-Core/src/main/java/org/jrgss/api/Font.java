package org.jrgss.api;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import static org.jrgss.api.win32.Win32Util.*;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jrgss.FileUtil;
import org.jrgss.JRGSSGame;
import org.jruby.*;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.builtin.IRubyObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by matty on 6/27/14.
 */
@JRubyClass(name = "Font")
public class Font extends RubyObject{
    private static final int DEFAULT_SIZE = 24;
    private static final String[] DEFAULT_NAME = new String[]{"VL Gothic"};
    private static final boolean DEFAULT_BOLD = false;
    private static final boolean DEFAULT_ITALIC = false;
    private static final boolean DEFAULT_SHADOW = false;
    private static final boolean DEFAULT_OUTLINE = true;
    private static final int[] DEFAULT_COLOR = new int[] {255,255,255,255};
    private static final int[] DEFAULT_OUT_COLOR = new int[] {0,0,0,128};


    final static Map<FontCacheKey, BitmapFont[]> fontCache = new HashMap<>();
    static Ruby runtime;
    static RubyClass rubyClass;

    private static int defaultSize;
    private static RubyArray defaultName;
    private static boolean defaultBold;
    private static boolean defaultItalic;
    private static boolean defaultShadow;
    private static boolean defaultOutline;
    private static Color defaultColor;
    private static Color defaultOutColor;


    @Getter
    BitmapFont bitmapFont;
    @Getter
    BitmapFont outlineFont;
    @Getter
    int size;
    @Getter
    Color color;
    @Getter
    boolean outline;
    @Getter
    Color outColor;
    boolean bold = false;
    boolean italic = false;
    @Getter
    boolean shadow = false;
    RubyArray name;


    public static void init() {
        defaultName = runtime.newArray();
        for(String n : DEFAULT_NAME) {
            defaultName.append(runtime.newString(n));
        }
        defaultSize = DEFAULT_SIZE;
        defaultBold = DEFAULT_BOLD;
        defaultItalic = DEFAULT_ITALIC;
        defaultShadow = DEFAULT_SHADOW;
        defaultOutline = DEFAULT_OUTLINE;
        defaultColor = new Color(DEFAULT_COLOR);
        defaultOutColor = new Color(DEFAULT_OUT_COLOR);

    }


    private static String getOSFontDir() {
        String osName = System.getProperty("os.name");
        if(osName.toLowerCase().contains("mac")) {
            return "/Library/Fonts";
        }
        return null;
    }

    private static String getOSFont(String name, boolean bold, boolean italic) {
        String ret = getOSFontDir()+File.separator+name;
        if(bold) {
            ret = ret+" Bold";
        }
        if(italic) {
            ret = ret+" Italic";
        }
        return ret+".ttf";
    }

    public Font() {
        super(runtime, rubyClass);
        initialize(new IRubyObject[]{});
    }

    public Font(final Ruby runtime, RubyClass rubyClass) {
        super(runtime, rubyClass);
    }

    @JRubyMethod(required = 0, optional = 2)
    public void initialize(IRubyObject[] args) {
        this.name = args.length > 0?((RubyArray)args[0]):defaultName;
        this.size = args.length > 1?getInt(args[1]):defaultSize;
        this.bold = defaultBold;
        this.italic = defaultItalic;
        this.outline = defaultOutline;
        this.shadow = defaultShadow;
        this.color = defaultColor.clone();
        this.outColor = defaultOutColor.clone();
        update();
    }

    //We only support VL Gothic for now...
    @JRubyMethod(name = "exist?",module = true)
    public static IRubyObject isExist(IRubyObject self, IRubyObject name) {
        String fontName = getString(name);
        return rubyBool("VL Gothic".equalsIgnoreCase(fontName));
    }

    @JRubyMethod(name = "name")
    public IRubyObject name() {
        return name;
    }

    @JRubyMethod(name = "size")
    public IRubyObject size() {
        return rubyNum(size);
    }

    @JRubyMethod(name = "bold")
    public IRubyObject bold() {
        return rubyBool(bold);
    }

    @JRubyMethod(name = "italic")
    public IRubyObject italic() {
        return rubyBool(italic);
    }

    @JRubyMethod(name = "outline")
    public IRubyObject outline() {
        return rubyBool(outline);
    }

    @JRubyMethod(name = "shadow")
    public IRubyObject shadow() {
        return rubyBool(shadow);
    }

    @JRubyMethod(name = "color")
    public IRubyObject color() {
        return color;
    }

    @JRubyMethod(name = "out_color")
    public IRubyObject outColor() {
        return outColor;
    }

    @JRubyMethod(name="name=", required = 1)
    public IRubyObject nameSet(IRubyObject arg) {
        return this.name = arg.convertToArray();
    }

    @JRubyMethod(name="size=", required = 1)
    public IRubyObject sizeSet(IRubyObject arg) {
        int argSize = getInt(arg);
        if(this.size != argSize) {
            this.size = argSize;
            update();
        }
        return rubyNum(this.size);
    }

    @JRubyMethod(name="bold=", required = 1)
    public IRubyObject boldSet(IRubyObject arg) {
        boolean value = getBool(arg);
        boolean diff = value != this.bold;
        update();
        if(diff) {
            this.bold = value;
        }
        return rubyBool(value);
    }

    @JRubyMethod(name="italic=", required = 1)
    public IRubyObject italicSet(IRubyObject arg) {
        boolean value = getBool(arg);
        boolean diff = value != this.italic;
        update();
        if(diff) {
            this.italic = value;
        }
        return rubyBool(value);
    }

    @JRubyMethod(name="outline=", required = 1)
    public IRubyObject outlineSet(IRubyObject arg) {
        return rubyBool(this.outline = getBool(arg));
    }

    @JRubyMethod(name="shadow=", required = 1)
    public IRubyObject shadowSet(IRubyObject arg) {
        return rubyBool(this.shadow = getBool(arg));
    }

    @JRubyMethod(name="color=", required = 1)
    public IRubyObject colorSet(IRubyObject arg) {
        Gdx.app.log("Font", "Set color to "+arg);
        return this.color = (Color)arg;
    }

    @JRubyMethod(name="out_color=", required = 1)
    public IRubyObject outColorSet(IRubyObject arg) {
        return this.outColor = (Color)arg;
    }



    private void update() {
        final String fontPath = FileUtil.rtpDirectory+File.separator+"Fonts"+File.separator+"VL-Gothic-Regular.ttf";
        if(fontCache.containsKey(new FontCacheKey(size, fontPath, bold))) {
            BitmapFont[] cached = fontCache.get(new FontCacheKey(size, fontPath, bold));
            bitmapFont = cached[0];
            outlineFont = cached[1];
        } else {
            Gdx.app.log("Font", "Generating new font");
            JRGSSGame.runWithGLContext(() -> {
                FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.absolute(fontPath));

                FreeTypeFontGenerator.FreeTypeFontParameter param = new FreeTypeFontGenerator.FreeTypeFontParameter();
                param.size = (int)(size*(0.833));


                Gdx.app.log("Font", String.format("%d versus %d", param.size, size));

                param.flip = true;
                param.genMipMaps = true;
                param.borderColor = com.badlogic.gdx.graphics.Color.WHITE;
                param.borderWidth = 0.2f;

                param.magFilter = Texture.TextureFilter.MipMapNearestLinear;
                param.minFilter = Texture.TextureFilter.MipMapNearestLinear;
                bitmapFont = generator.generateFont(param);

                param.magFilter = Texture.TextureFilter.MipMapNearestLinear;
                param.minFilter = Texture.TextureFilter.MipMapNearestLinear;
                param.borderWidth = 1.2f;
                outlineFont = generator.generateFont(param);

                fontCache.put(new FontCacheKey(size, fontPath, bold), new BitmapFont[]{bitmapFont, outlineFont});
                generator.dispose();

            });
        }
    }


    @JRubyMethod(module = true, name = "default_name")
    public static IRubyObject defaultName(IRubyObject self) {
        return defaultName;
    }

    @JRubyMethod(module = true, name = "default_size")
    public static IRubyObject defaultSize(IRubyObject self) {
        return rubyNum(defaultSize);
    }

    @JRubyMethod(module = true, name = "default_bold")
    public static IRubyObject defaultBold(IRubyObject self) {
        return rubyBool(defaultBold);
    }

    @JRubyMethod(module = true, name = "default_italic")
    public static IRubyObject defaultItalic(IRubyObject self) {
        return rubyBool(defaultItalic);
    }

    @JRubyMethod(module = true, name = "default_outline")
    public static IRubyObject defaultOutline(IRubyObject self) {
        return rubyBool(defaultOutline);
    }

    @JRubyMethod(module = true, name = "default_shadow")
    public static IRubyObject defaultShadow(IRubyObject self) {
        return rubyBool(defaultShadow);
    }

    @JRubyMethod(module = true, name = "default_color")
    public static IRubyObject defaultColor(IRubyObject self) {
        return defaultColor;
    }

    @JRubyMethod(module = true, name = "default_out_color")
    public static IRubyObject defaultOutColor(IRubyObject self) {
        return defaultOutColor;
    }

    @JRubyMethod(module = true, name="default_name=", required = 1)
    public static IRubyObject defaultNameSet(IRubyObject self, IRubyObject arg) {
        return defaultName = arg.convertToArray();
    }

    @JRubyMethod(module = true, name="default_size=", required = 1)
    public static IRubyObject defaultSizeSet(IRubyObject self, IRubyObject arg) {
        return rubyNum(defaultSize = getInt(arg));
    }

    @JRubyMethod(module = true, name="default_bold=", required = 1)
    public static IRubyObject defaultBoldSet(IRubyObject self, IRubyObject arg) {
        return rubyBool(defaultBold = getBool(arg));
    }

    @JRubyMethod(module = true, name="default_italic=", required = 1)
    public static IRubyObject defaultItalicSet(IRubyObject self, IRubyObject arg) {
        return rubyBool(defaultItalic = getBool(arg));
    }

    @JRubyMethod(module = true, name="default_outline=", required = 1)
    public static IRubyObject defaultOutlineSet(IRubyObject self, IRubyObject arg) {
        return rubyBool(defaultOutline = getBool(arg));
    }

    @JRubyMethod(module = true, name="default_shadow=", required = 1)
    public static IRubyObject defaultShadowSet(IRubyObject self, IRubyObject arg) {
        return rubyBool(defaultShadow = getBool(arg));
    }

    @JRubyMethod(module = true, name="default_color=", required = 1)
    public static IRubyObject defaultColorSet(IRubyObject self, IRubyObject arg) {
        return defaultColor = (Color)arg;
    }

    @JRubyMethod(module = true, name="default_out_color=", required = 1)
    public static IRubyObject defaultOutColorSet(IRubyObject self, IRubyObject arg) {
        return defaultOutColor = (Color)arg;
    }


    @lombok.Data
    @AllArgsConstructor
    private static class FontCacheKey {
        final int size;
        final String name;
        final boolean bold;
    }

}
