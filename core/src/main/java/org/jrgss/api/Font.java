package org.jrgss.api;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeType;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeType.Face;
import com.badlogic.gdx.graphics.g2d.freetype.FreeType.Library;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.utils.BufferUtils;
import com.sun.jna.NativeLong;
import java.beans.ConstructorProperties;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.jrgss.FileUtil;
import org.jrgss.JRGSSGame;
import org.jrgss.OS;
import org.jrgss.api.win32.Win32Util;
import org.jrgss.util.SystemArchitecture;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyException;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.builtin.IRubyObject;

@JRubyClass(
   name = {"Font"}
)
public class Font extends RubyObject {
   public static final char MISSING_CHARACTER = 'ÿ';
   private static final int DEFAULT_SIZE = 24;
   private static final String[] DEFAULT_NAME = new String[]{"VL Gothic"};
   private static final boolean DEFAULT_BOLD = false;
   private static final boolean DEFAULT_ITALIC = false;
   private static final boolean DEFAULT_SHADOW = false;
   private static final boolean DEFAULT_OUTLINE = true;
   private static final int[] DEFAULT_COLOR = new int[]{255, 255, 255, 255};
   private static final int[] DEFAULT_OUT_COLOR = new int[]{0, 0, 0, 128};
   private static final Map<String, FileHandle> INSTALLED_FONTS = new HashMap<>();
   private static final Map<Font.FontCacheKey, AtomicReference<BitmapFont>[]> fontCache = new HashMap<>();
   private static Ruby runtime;
   private static RubyClass rubyClass;
   private static int defaultSize;
   private static RubyArray defaultName;
   private static boolean defaultBold;
   private static boolean defaultItalic;
   private static boolean defaultShadow;
   private static boolean defaultOutline;
   private static Color defaultColor;
   private static Color defaultOutColor;
   AtomicReference<BitmapFont> bitmapFont = new AtomicReference<>();
   AtomicReference<BitmapFont> outlineFont = new AtomicReference<>();
   int size;
   Color color;
   boolean outline;
   Color outColor;
   private boolean bold = false;
   private boolean italic = false;
   boolean shadow = false;
   private RubyArray name;
   private double loadedResolution = 1.0;
   private static Field addressField;

   public static void init() {
      defaultName = runtime.newArray();

      for (String n : DEFAULT_NAME) {
         defaultName.append(runtime.newString(n));
      }

      defaultSize = 24;
      defaultBold = false;
      defaultItalic = false;
      defaultShadow = false;
      defaultOutline = true;
      defaultColor = new Color(DEFAULT_COLOR);
      defaultOutColor = new Color(DEFAULT_OUT_COLOR);

      try {
         addressField = Face.class.getSuperclass().getDeclaredField("address");
         addressField.setAccessible(true);
      } catch (Exception var4) {
         throw new RuntimeException(var4);
      }

      findFonts();
   }

   private static String getOSFontDir() {
      String osName = System.getProperty("os.name");
      return osName.toLowerCase().contains("mac") ? "/Library/Fonts" : null;
   }

   private static String getOSFont(String name, boolean bold, boolean italic) {
      String ret = getOSFontDir() + File.separator + name;
      if (bold) {
         ret = ret + " Bold";
      }

      if (italic) {
         ret = ret + " Italic";
      }

      return ret + ".ttf";
   }

   public Font() {
      super(runtime, rubyClass);
      this.initialize(new IRubyObject[0]);
   }

   public Font(Font f) {
      this();
      this.name = f.name;
      this.size = f.size;
      this.bold = f.bold;
      this.italic = f.italic;
      this.outline = f.outline;
      this.shadow = f.shadow;
      this.color = (Color)f.color.clone();
      this.outColor = (Color)f.outColor.clone();
      this.update();
   }

   public Font(Ruby runtime, RubyClass rubyClass) {
      super(runtime, rubyClass);
   }

   @JRubyMethod(
      required = 0,
      optional = 2
   )
   public void initialize(IRubyObject[] args) {
      this.name = args.length > 0 ? (RubyArray)args[0] : defaultName;
      this.size = args.length > 1 ? Win32Util.getInt(args[1]) : defaultSize;
      this.bold = defaultBold;
      this.italic = defaultItalic;
      this.outline = defaultOutline;
      this.shadow = defaultShadow;
      this.color = (Color)defaultColor.clone();
      this.outColor = (Color)defaultOutColor.clone();
      this.update();
   }

   @JRubyMethod(
      name = {"exist?"},
      module = true
   )
   public static IRubyObject isExist(IRubyObject self, IRubyObject name) {
      String fontName = Win32Util.getString(name);
      return Win32Util.rubyBool(INSTALLED_FONTS.containsKey(fontName));
   }

   @JRubyMethod(
      name = {"name"}
   )
   public IRubyObject name() {
      return this.name;
   }

   @JRubyMethod(
      name = {"size"}
   )
   public IRubyObject size() {
      return Win32Util.rubyNum(this.size);
   }

   @JRubyMethod(
      name = {"bold"}
   )
   public IRubyObject bold() {
      return Win32Util.rubyBool(this.bold);
   }

   @JRubyMethod(
      name = {"italic"}
   )
   public IRubyObject italic() {
      return Win32Util.rubyBool(this.italic);
   }

   @JRubyMethod(
      name = {"outline"}
   )
   public IRubyObject outline() {
      return Win32Util.rubyBool(this.outline);
   }

   @JRubyMethod(
      name = {"shadow"}
   )
   public IRubyObject shadow() {
      return Win32Util.rubyBool(this.shadow);
   }

   @JRubyMethod(
      name = {"color"}
   )
   public IRubyObject color() {
      return this.color;
   }

   @JRubyMethod(
      name = {"out_color"}
   )
   public IRubyObject outColor() {
      return this.outColor;
   }

   @JRubyMethod(
      name = {"name="},
      required = 1
   )
   public IRubyObject nameSet(IRubyObject arg) {
      if (arg != null && arg != arg.getRuntime().getNil()) {
         this.name = arg.convertToArray();
         this.update();
         return this.name;
      } else {
         return Win32Util.rubyNil();
      }
   }

   @JRubyMethod(
      name = {"size="},
      required = 1
   )
   public IRubyObject sizeSet(IRubyObject arg) {
      int argSize = Win32Util.getInt(arg);
      if (this.size != argSize && argSize > 0) {
         this.size = argSize;
         this.update();
      }

      return Win32Util.rubyNum(this.size);
   }

   @JRubyMethod(
      name = {"bold="},
      required = 1
   )
   public IRubyObject boldSet(IRubyObject arg) {
      boolean value = Win32Util.getBool(arg);
      boolean diff = value != this.bold;
      if (diff) {
         this.bold = value;
         this.update();
      }

      return Win32Util.rubyBool(value);
   }

   @JRubyMethod(
      name = {"italic="},
      required = 1
   )
   public IRubyObject italicSet(IRubyObject arg) {
      boolean value = Win32Util.getBool(arg);
      boolean diff = value != this.italic;
      if (diff) {
         this.italic = value;
         this.update();
      }

      return Win32Util.rubyBool(value);
   }

   @JRubyMethod(
      name = {"outline="},
      required = 1
   )
   public IRubyObject outlineSet(IRubyObject arg) {
      return Win32Util.rubyBool(this.outline = Win32Util.getBool(arg));
   }

   @JRubyMethod(
      name = {"shadow="},
      required = 1
   )
   public IRubyObject shadowSet(IRubyObject arg) {
      return Win32Util.rubyBool(this.shadow = Win32Util.getBool(arg));
   }

   @JRubyMethod(
      name = {"color="},
      required = 1
   )
   public IRubyObject colorSet(IRubyObject arg) {
      return this.color = (Color)arg;
   }

   @JRubyMethod(
      name = {"out_color="},
      required = 1
   )
   public IRubyObject outColorSet(IRubyObject arg) {
      return this.outColor = (Color)arg;
   }

   public boolean isOutline() {
      return !this.outline ? false : this.outColor.getAlpha() != 0;
   }

   private void update() {
      String fontName = this.name.first().asJavaString();
      this.loadedResolution = Graphics.hiResScale;
      if (fontCache.containsKey(new Font.FontCacheKey(this.size, fontName, this.bold))) {
         AtomicReference<BitmapFont>[] cached = fontCache.get(new Font.FontCacheKey(this.size, fontName, this.bold));
         this.bitmapFont = cached[0];
         this.outlineFont = cached[1];
      } else {
         Gdx.app.log("Font", "Generating new font for " + new Font.FontCacheKey(this.size, fontName, this.bold));
         AtomicReference<BitmapFont>[] newVals = new AtomicReference[]{new AtomicReference(), new AtomicReference()};
         this.bitmapFont = newVals[0];
         this.outlineFont = newVals[1];
         fontCache.put(new Font.FontCacheKey(this.size, fontName, this.bold), newVals);
         JRGSSGame.runWithGLContext(() -> {
            FreeTypeFontGenerator generator = new FreeTypeFontGenerator(INSTALLED_FONTS.get(fontName));
            FreeTypeFontParameter param = new FreeTypeFontParameter();
            param.size = (int)(this.size * 0.85 * Graphics.hiResScale);
            param.flip = true;
            param.genMipMaps = true;
            param.borderColor = com.badlogic.gdx.graphics.Color.WHITE;
            param.borderWidth = this.bold ? 0.2F * Graphics.hiResScale : 0.0F;
            param.gamma = 1.0F;
            param.magFilter = TextureFilter.Nearest;
            param.minFilter = TextureFilter.Nearest;
            newVals[0].set(generator.generateFont(param));
            param.magFilter = TextureFilter.Nearest;
            param.minFilter = TextureFilter.Nearest;
            param.borderWidth = (this.bold ? 1.2F : 1.0F) * Graphics.hiResScale;
            param.borderGamma = 0.7F;
            newVals[1].set(generator.generateFont(param));
            Gdx.app.log("Font", "Values set for " + new Font.FontCacheKey(this.size, fontName, this.bold));
            generator.dispose();
         });
      }
   }

   public BitmapFont getOutlineFont() {
      if (Math.abs(this.loadedResolution - Graphics.hiResScale) > 0.001 || this.outlineFont.get() == null) {
         this.update();
      }

      return this.outlineFont.get();
   }

   public BitmapFont getBitmapFont() {
      if (Math.abs(this.loadedResolution - Graphics.hiResScale) > 0.001 || this.bitmapFont.get() == null) {
         Gdx.app.log("Font", "Reloading font for " + this);
         this.update();
      }

      return this.bitmapFont.get();
   }

   @JRubyMethod(
      module = true,
      name = {"default_name"}
   )
   public static IRubyObject defaultName(IRubyObject self) {
      return defaultName;
   }

   @JRubyMethod(
      module = true,
      name = {"default_size"}
   )
   public static IRubyObject defaultSize(IRubyObject self) {
      return Win32Util.rubyNum(defaultSize);
   }

   @JRubyMethod(
      module = true,
      name = {"default_bold"}
   )
   public static IRubyObject defaultBold(IRubyObject self) {
      return Win32Util.rubyBool(defaultBold);
   }

   @JRubyMethod(
      module = true,
      name = {"default_italic"}
   )
   public static IRubyObject defaultItalic(IRubyObject self) {
      return Win32Util.rubyBool(defaultItalic);
   }

   @JRubyMethod(
      module = true,
      name = {"default_outline"}
   )
   public static IRubyObject defaultOutline(IRubyObject self) {
      return Win32Util.rubyBool(defaultOutline);
   }

   @JRubyMethod(
      module = true,
      name = {"default_shadow"}
   )
   public static IRubyObject defaultShadow(IRubyObject self) {
      return Win32Util.rubyBool(defaultShadow);
   }

   @JRubyMethod(
      module = true,
      name = {"default_color"}
   )
   public static IRubyObject defaultColor(IRubyObject self) {
      return defaultColor;
   }

   @JRubyMethod(
      module = true,
      name = {"default_out_color"}
   )
   public static IRubyObject defaultOutColor(IRubyObject self) {
      return defaultOutColor;
   }

   @JRubyMethod(
      module = true,
      name = {"default_name="},
      required = 1
   )
   public static IRubyObject defaultNameSet(IRubyObject self, IRubyObject arg) {
      return defaultName = arg.convertToArray();
   }

   @JRubyMethod(
      module = true,
      name = {"default_size="},
      required = 1
   )
   public static IRubyObject defaultSizeSet(IRubyObject self, IRubyObject arg) {
      int val = Win32Util.getInt(arg);
      if (val == 0) {
         throw new RaiseException(
            RubyException.newException(Ruby.getGlobalRuntime(), Ruby.getGlobalRuntime().getClass("Exception"), "Cannot set default size to 0!")
         );
      } else {
         defaultSize = val;
         return Win32Util.rubyNum(val);
      }
   }

   @JRubyMethod(
      module = true,
      name = {"default_bold="},
      required = 1
   )
   public static IRubyObject defaultBoldSet(IRubyObject self, IRubyObject arg) {
      return Win32Util.rubyBool(defaultBold = Win32Util.getBool(arg));
   }

   @JRubyMethod(
      module = true,
      name = {"default_italic="},
      required = 1
   )
   public static IRubyObject defaultItalicSet(IRubyObject self, IRubyObject arg) {
      return Win32Util.rubyBool(defaultItalic = Win32Util.getBool(arg));
   }

   @JRubyMethod(
      module = true,
      name = {"default_outline="},
      required = 1
   )
   public static IRubyObject defaultOutlineSet(IRubyObject self, IRubyObject arg) {
      return Win32Util.rubyBool(defaultOutline = Win32Util.getBool(arg));
   }

   @JRubyMethod(
      module = true,
      name = {"default_shadow="},
      required = 1
   )
   public static IRubyObject defaultShadowSet(IRubyObject self, IRubyObject arg) {
      return Win32Util.rubyBool(defaultShadow = Win32Util.getBool(arg));
   }

   @JRubyMethod(
      module = true,
      name = {"default_color="},
      required = 1
   )
   public static IRubyObject defaultColorSet(IRubyObject self, IRubyObject arg) {
      return defaultColor = (Color)arg;
   }

   @JRubyMethod(
      module = true,
      name = {"default_out_color="},
      required = 1
   )
   public static IRubyObject defaultOutColorSet(IRubyObject self, IRubyObject arg) {
      return defaultOutColor = (Color)arg;
   }

   public static void resetCache() {
      for (AtomicReference<BitmapFont>[] bf : fontCache.values()) {
         bf[0].get().dispose();
         bf[0].set(null);
         bf[1].get().dispose();
         bf[1].set(null);
      }

      fontCache.clear();
   }

   private static void findFonts() {
      findFontsInFolder(new File(FileUtil.rtpDirectory + File.separator + "Fonts"));
      if (!FileUtil.rtpDirectory.equals(FileUtil.gameDirectory)) {
         findFontsInFolder(new File(FileUtil.gameDirectory + File.separator + "Fonts"));
      }
   }

   private static void findFontsInFolder(File folder) {
      System.out.println("Loading fonts in " + folder);
      File[] listOfFiles = folder.listFiles();
      if (listOfFiles != null) {
         ByteBuffer bb = BufferUtils.newUnsafeByteBuffer(256);

         for (File listOfFile : listOfFiles) {
            if (listOfFile.isFile()) {
               try {
                  System.out.println("Loading Font " + listOfFile);
                  Library library = FreeType.initFreeType();
                  FileHandle handle = new FileHandle(folder.getAbsolutePath() + File.separator + listOfFile.getName());
                  Face face = library.newFace(handle, 0);
                  long address = addressField.getLong(face);
                  String font;
                  if (OS.CURRENT_OS == OS.WINDOWS && SystemArchitecture.IS_64BIT) {
                     font = SystemArchitecture.getStringForAddress(SystemArchitecture.getPtr(address + 6 * NativeLong.SIZE), bb);
                  } else {
                     font = SystemArchitecture.getStringForAddress(SystemArchitecture.getPtr(address + 5 * NativeLong.SIZE), bb);
                  }

                  INSTALLED_FONTS.put(font, handle);
                  System.out.println("Found font " + font);
               } catch (Exception var13) {
                  Gdx.app.log("Font", "Failed to load font: " + var13.getMessage());
                  var13.printStackTrace(System.err);
               }
            }
         }
      }
   }

   public int getSize() {
      return this.size;
   }

   public Color getColor() {
      return this.color;
   }

   public Color getOutColor() {
      return this.outColor;
   }

   public boolean isShadow() {
      return this.shadow;
   }

   private static class FontCacheKey {
      final int size;
      final String name;
      final boolean bold;

      public int getSize() {
         return this.size;
      }

      public String getName() {
         return this.name;
      }

      public boolean isBold() {
         return this.bold;
      }

      @Override
      public boolean equals(Object o) {
         if (o == this) {
            return true;
         } else if (!(o instanceof Font.FontCacheKey)) {
            return false;
         } else {
            Font.FontCacheKey other = (Font.FontCacheKey)o;
            if (!other.canEqual(this)) {
               return false;
            } else if (this.getSize() != other.getSize()) {
               return false;
            } else {
               Object this$name = this.getName();
               Object other$name = other.getName();
               return (this$name == null ? other$name == null : this$name.equals(other$name)) ? this.isBold() == other.isBold() : false;
            }
         }
      }

      protected boolean canEqual(Object other) {
         return other instanceof Font.FontCacheKey;
      }

      @Override
      public int hashCode() {
         int PRIME = 59;
         int result = 1;
         result = result * 59 + this.getSize();
         Object $name = this.getName();
         result = result * 59 + ($name == null ? 43 : $name.hashCode());
         return result * 59 + (this.isBold() ? 79 : 97);
      }

      @Override
      public String toString() {
         return "Font.FontCacheKey(size=" + this.getSize() + ", name=" + this.getName() + ", bold=" + this.isBold() + ")";
      }

      @ConstructorProperties({"size", "name", "bold"})
      public FontCacheKey(int size, String name, boolean bold) {
         this.size = size;
         this.name = name;
         this.bold = bold;
      }
   }
}
