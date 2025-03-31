package org.jrgss.font;

import com.google.common.collect.ImmutableList;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import java.util.List;

public class FTGlyphSlot extends Structure {
   public Pointer library;
   public Pointer face;
   public Pointer next;
   public int reserved;
   public Pointer genericData;
   public Pointer genericFinalizer;
   public FTGlyphSlot.GlyphMetrics metrics;
   public long linearHoriAdvance;
   public long linearVertAdvance;
   public FTGlyphSlot.FTVector advance;
   public int format;
   public FTGlyphSlot.FTBitmap bitmap;
   public int bitmapLeft;
   public int bitmapTop;
   public FTGlyphSlot.FTOutline outline;

   public FTGlyphSlot(Pointer p) {
      super(p);
      this.read();
   }

   @Override
   protected List getFieldOrder() {
      return ImmutableList.of(
         "library",
         "face",
         "next",
         "reserved",
         "genericData",
         "genericFinalizer",
         "metrics",
         "linearHoriAdvance",
         "linearVertAdvance",
         "advance",
         "format",
         "bitmap",
         "bitmapLeft",
         "bitmapTop",
         "outline"
      );
   }

   public static class FTBitmap extends Structure {
      public int rows;
      public int width;
      public int pitch;
      public Pointer buffer;
      public short numGrays;
      public byte pixelMode;
      public byte paletteMode;
      public Pointer palette;

      public FTBitmap(Pointer p) {
         super(p);
         this.read();
      }

      @Override
      protected List getFieldOrder() {
         return ImmutableList.of("rows", "width", "pitch", "buffer", "numGrays", "pixelMode", "paletteMode", "palette");
      }
   }

   public static class FTOutline extends Structure {
      public short numContours;
      public short numPoints;
      public Pointer points;
      public Pointer tags;
      public Pointer contours;
      public int flags;

      public FTOutline(Pointer p) {
         super(p);
         this.read();
      }

      @Override
      protected List getFieldOrder() {
         return ImmutableList.of("numContours", "numPoints", "points", "tags", "contours", "flags");
      }
   }

   public static class FTVector extends Structure {
      public long x;
      public long y;
      public static final int SIZE = new FTGlyphSlot.FTVector().calculateSize(true);

      private FTVector() {
      }

      public FTVector(Pointer p) {
         super(p);
         this.read();
      }

      public FTVector(long x, long y) {
         this.x = x;
         this.y = y;
      }

      @Override
      protected List getFieldOrder() {
         return ImmutableList.of("x", "y");
      }

      @Override
      public String toString() {
         return "FTGlyphSlot.FTVector(x=" + this.x + ", y=" + this.y + ")";
      }
   }

   public static class GlyphMetrics extends Structure {
      public long width;
      public long height;
      public long horiBearingX;
      public long horiBearingY;
      public long horiAdvance;
      public long vertBearingX;
      public long vertBearingY;
      public long vertAdvance;

      @Override
      protected List getFieldOrder() {
         return ImmutableList.of("width", "height", "horiBearingX", "horiBearingY", "horiAdvance", "vertBearingX", "vertBearingY", "vertAdvance");
      }
   }
}
