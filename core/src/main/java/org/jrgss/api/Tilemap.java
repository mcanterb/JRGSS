package org.jrgss.api;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.JrgssBatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.google.common.collect.ImmutableSet;
import java.beans.ConstructorProperties;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.jrgss.JRGSSGame;
import org.jrgss.shaders.AlphaBlendingShader;
import org.jruby.Ruby;
import org.jruby.RubyArray;

public class Tilemap {
   private static final int[][] AUTOTILE_PARTS = new int[][]{
      {18, 17, 14, 13},
      {2, 14, 17, 18},
      {13, 3, 17, 18},
      {2, 3, 17, 18},
      {13, 14, 17, 7},
      {2, 14, 17, 7},
      {13, 3, 17, 7},
      {2, 3, 17, 7},
      {13, 14, 6, 18},
      {2, 14, 6, 18},
      {13, 3, 6, 18},
      {2, 3, 6, 18},
      {13, 14, 6, 7},
      {2, 14, 6, 7},
      {13, 3, 6, 7},
      {2, 3, 6, 7},
      {16, 17, 12, 13},
      {16, 3, 12, 13},
      {16, 17, 12, 7},
      {12, 3, 16, 7},
      {10, 9, 14, 13},
      {10, 9, 14, 7},
      {10, 9, 6, 13},
      {10, 9, 6, 7},
      {18, 19, 14, 15},
      {18, 19, 6, 15},
      {2, 19, 14, 15},
      {2, 19, 6, 15},
      {18, 17, 22, 21},
      {2, 17, 22, 21},
      {18, 3, 22, 21},
      {2, 3, 21, 22},
      {16, 19, 12, 15},
      {10, 9, 22, 21},
      {8, 9, 12, 13},
      {8, 9, 12, 7},
      {10, 11, 14, 15},
      {10, 11, 6, 15},
      {18, 19, 22, 23},
      {2, 19, 22, 23},
      {16, 17, 20, 21},
      {16, 3, 20, 21},
      {8, 11, 12, 15},
      {8, 9, 20, 21},
      {16, 19, 20, 23},
      {10, 11, 22, 23},
      {8, 11, 20, 23},
      {0, 1, 4, 5}
   };
   private static final Set<Integer> WATERFALL_IDS = ImmutableSet.of(5, 7, 9, 11, 13, 15);
   private static final int[][] WATERFALL_OFFSETS = new int[][]{{32, 16}, {0, 16}, {32, 48}, {0, 48}};
   private static final int[][] WALL_PIECES = new int[][]{
      {10, 9, 6, 5},
      {8, 9, 4, 5},
      {2, 1, 6, 5},
      {0, 1, 4, 5},
      {10, 11, 6, 7},
      {8, 11, 4, 7},
      {2, 3, 6, 7},
      {0, 3, 4, 7},
      {10, 9, 14, 13},
      {8, 9, 12, 13},
      {2, 1, 14, 13},
      {0, 1, 12, 13},
      {10, 11, 14, 15},
      {10, 11, 6, 7},
      {2, 3, 14, 15},
      {0, 3, 12, 15}
   };
   private static final int[][] WALL_RANGES = new int[][]{{4736, 5119}, {5504, 5887}, {6272, 6655}, {7040, 7423}};
   private static final int[][] ROOF_RANGES = new int[][]{{4352, 4735}, {5120, 5503}, {5888, 6271}, {6656, 7039}, {7424, 7807}};
   private static final int[][] STAIR_RANGES = new int[][]{{1541, 1542}, {1549, 1550}, {1600, 1615}};
   private static final int[][] TABLE_RANGES = new int[][]{{3152, 3199}, {3536, 3583}, {3920, 3967}, {4304, 4351}};
   private static final com.badlogic.gdx.graphics.Color SHADOW_COLOR = new com.badlogic.gdx.graphics.Color(0.0F, 0.0F, 0.0F, 0.5F);
   final Map<Integer, TextureRegion>[] tiles = new Map[3];
   Viewport viewport;
   boolean disposed = false;
   RubyArray bitmaps;
   Table map_data;
   Table flash_data;
   Table flags;
   boolean visible = true;
   int ox;
   int oy;
   int frame_interval = 30;
   int frameCount = 0;
   int frame = 0;
   int waterfallFrame = 0;
   int frameInc = 1;
   boolean needRefresh = false;
   static SpriteBatch batch;
   Tilemap.TileMapLayer[] layers = new Tilemap.TileMapLayer[6];

   public Tilemap() {
      this(null);
   }

   public Tilemap(Viewport viewport) {
      this.viewport = viewport;
      this.bitmaps = RubyArray.newArray(Ruby.getGlobalRuntime(), 9);

      for (int i = 0; i < this.tiles.length; i++) {
         this.tiles[i] = new HashMap<>();
      }

      for (int i = 0; i < 6; i++) {
         this.layers[i] = (Tilemap.TileMapLayer)(i != 5 ? new Tilemap.TileMapLayer(i) : new Tilemap.ShadowLayer());
      }
   }

   private static SpriteBatch getBatch() {
      if (batch == null) {
         batch = new SpriteBatch();
         batch.enableBlending();
         batch.setProjectionMatrix(JRGSSGame.camera.combined);
      }

      return batch;
   }

   private static boolean inRange(int[][] ranges, int data) {
      for (int[] range : ranges) {
         if (data >= range[0] && data <= range[1]) {
            return true;
         }
      }

      return false;
   }

   private static boolean isWall(int data) {
      return data > 7807 ? true : inRange(WALL_RANGES, data);
   }

   private static boolean isWaterfall(int autotileId) {
      return WATERFALL_IDS.contains(autotileId);
   }

   private static boolean isRoof(int data) {
      return inRange(ROOF_RANGES, data);
   }

   private static boolean isSoil(int data) {
      if (data >= 2816 && data <= 4351) {
         return !isTable(data);
      } else {
         return data > 1663 ? !isStair(data) : false;
      }
   }

   private static boolean isStair(int data) {
      return inRange(STAIR_RANGES, data);
   }

   private static boolean isTable(int data) {
      return inRange(TABLE_RANGES, data);
   }

   public void dispose() {
      for (Tilemap.TileMapLayer layer : this.layers) {
         layer.dispose();
      }

      Gdx.app.log("Tilemap", "Disposed of Tilemap");
   }

   public void setMap_data(Table data) {
      this.map_data = data;
   }

   public void setFlags(Table flags) {
      this.flags = flags;
   }

   public void update() {
      this.frameCount = (this.frameCount + 1) % this.frame_interval;
      if (this.frameCount == 0) {
         this.frame = this.frame + this.frameInc;
         if (this.frame == 2 || this.frame == 0) {
            this.frameInc = -this.frameInc;
         }

         this.waterfallFrame = (this.waterfallFrame + 1) % 3;
      }
   }

   private void renderTile(JrgssBatch batch, int id, int frame, int waterfallFrame, boolean counter, int x, int y) {
      if (id < 1024) {
         int subId = id % 256;
         Texture tilemap = ((Bitmap)this.bitmaps.get(id / 256 + 5)).getRegion().getTexture();
         int tilemapHeight = tilemap.getHeight();
         if (subId < 128) {
            batch.draw(tilemap, x, y, subId % 8 * 32, tilemapHeight - 32 - subId / 8 * 32, 32, 32);
         } else {
            subId -= 128;
            batch.draw(tilemap, x, y, subId % 8 * 32 + 256, tilemapHeight - 32 - subId / 8 * 32, 32, 32);
         }
      } else if (id < 1664) {
         Texture tilemap = ((Bitmap)this.bitmaps.get(4)).getRegion().getTexture();
         int tilemapHeight = tilemap.getHeight();
         int subId = id - 1536;
         batch.draw(tilemap, x, y, subId % 8 * 32, tilemapHeight - 32 - subId / 8 * 32, 32, 32);
      } else if (id < 2816) {
         int subId = id - 2048;
         int autotile = subId / 48;
         int autoId = subId % 48;
         Texture tilemap = ((Bitmap)this.bitmaps.get(0)).getRegion().getTexture();
         int tilemapHeight = tilemap.getHeight();
         int sx = 0;
         int sy = 0;
         if (isWaterfall(autotile)) {
            switch (autotile) {
               case 5:
                  sx = 448;
                  sy = waterfallFrame * 32;
               case 6:
               case 8:
               case 10:
               case 12:
               case 14:
               default:
                  break;
               case 7:
                  sx = 448;
                  sy = 96 + waterfallFrame * 32;
                  break;
               case 9:
                  sx = 192;
                  sy = 192 + waterfallFrame * 32;
                  break;
               case 11:
                  sx = 192;
                  sy = 288 + waterfallFrame * 32;
                  break;
               case 13:
                  sx = 448;
                  sy = 192 + waterfallFrame * 32;
                  break;
               case 15:
                  sx = 448;
                  sy = 288 + waterfallFrame * 32;
            }

            int[] offsets = WATERFALL_OFFSETS[autoId % 4];

            for (int i = 0; i < 2; i++) {
               batch.draw(tilemap, x + i * 16, y, sx + offsets[i], tilemapHeight - 32 - sy, 16, 32);
            }
         } else {
            switch (autotile) {
               case 0:
                  sx = frame * 64;
                  sy = 0;
                  break;
               case 1:
                  sx = frame * 64;
                  sy = 96;
                  break;
               case 2:
                  sx = 192;
                  sy = 0;
                  break;
               case 3:
                  sx = 192;
                  sy = 96;
                  break;
               case 4:
                  sx = 256 + frame * 64;
                  sy = 0;
                  break;
               case 5:
               case 7:
               case 9:
               case 11:
               case 13:
               default:
                  Gdx.app.log("Tilemap", "Unsupported Autotile value: " + autotile);
                  break;
               case 6:
                  sx = 256 + frame * 64;
                  sy = 96;
                  break;
               case 8:
                  sx = frame * 64;
                  sy = 192;
                  break;
               case 10:
                  sx = frame * 64;
                  sy = 288;
                  break;
               case 12:
                  sx = 256 + frame * 64;
                  sy = 192;
                  break;
               case 14:
                  sx = 256 + frame * 64;
                  sy = 288;
            }

            int[] autoTilePieces = AUTOTILE_PARTS[autoId];

            for (int i = 0; i < 4; i++) {
               batch.draw(
                  tilemap, x + i % 2 * 16, y + i / 2 * 16, autoTilePieces[i] % 4 * 16 + sx, tilemapHeight - 16 - (autoTilePieces[i] / 4 * 16 + sy), 16, 16
               );
            }
         }
      } else if (id < 4352) {
         int subId = id - 2816;
         int autotile = subId / 48;
         int autoId = subId % 48;
         Texture tilemap = ((Bitmap)this.bitmaps.get(1)).getRegion().getTexture();
         int tilemapHeight = tilemap.getHeight();
         int sx = autotile % 8 * 64;
         int sy = autotile / 8 * 96;
         int[] autoTilePieces = AUTOTILE_PARTS[autoId];
         boolean counterBottom = Arrays.stream(autoTilePieces).anyMatch(ix -> ix > 19);

         for (int i = 0; i < 2; i++) {
            batch.draw(tilemap, x + i % 2 * 16, y + i / 2 * 16, autoTilePieces[i] % 4 * 16 + sx, tilemapHeight - 16 - (autoTilePieces[i] / 4 * 16 + sy), 16, 16);
         }

         if (counterBottom && counter) {
            int[] counterPieces = new int[]{autoTilePieces[2] - 8, autoTilePieces[3] - 8};

            for (int i = 0; i < 2; i++) {
               batch.draw(
                  tilemap, x + i % 2 * 16, y + 16 + i / 2 * 16, counterPieces[i] % 4 * 16 + sx, tilemapHeight - 16 - (counterPieces[i] / 4 * 16 + sy), 16, 8
               );
            }

            y += 8;
         }

         for (int i = 2; i < 4; i++) {
            batch.draw(tilemap, x + i % 2 * 16, y + i / 2 * 16, autoTilePieces[i] % 4 * 16 + sx, tilemapHeight - 16 - (autoTilePieces[i] / 4 * 16 + sy), 16, 16);
         }
      } else if (id < 5888) {
         int subId = id - 4352;
         int autotile = subId / 48;
         int autoId = subId % 48;
         Texture tilemap = ((Bitmap)this.bitmaps.get(2)).getRegion().getTexture();
         int tilemapHeight = tilemap.getHeight();
         int sx = autotile % 8 * 64;
         int sy = autotile / 8 * 64;
         int[] autoTilePieces = WALL_PIECES[autoId];

         for (int i = 0; i < 4; i++) {
            batch.draw(tilemap, x + i % 2 * 16, y + i / 2 * 16, autoTilePieces[i] % 4 * 16 + sx, tilemapHeight - 16 - (autoTilePieces[i] / 4 * 16 + sy), 16, 16);
         }
      } else {
         int subId = id - 5888;
         int autotile = subId / 48;
         int autoId = subId % 48;
         Texture tilemap = ((Bitmap)this.bitmaps.get(3)).getRegion().getTexture();
         int tilemapHeight = tilemap.getHeight();
         int sx = autotile % 8 * 64;
         int sy;
         switch (autotile / 8) {
            case 0:
               sy = 0;
               break;
            case 1:
               sy = 96;
               break;
            case 2:
               sy = 160;
               break;
            case 3:
               sy = 256;
               break;
            case 4:
               sy = 320;
               break;
            default:
               sy = 416;
         }

         int[] autoTilePieces = isWall(id) ? WALL_PIECES[autoId] : AUTOTILE_PARTS[autoId];

         for (int i = 0; i < autoTilePieces.length; i++) {
            batch.draw(tilemap, x + i % 2 * 16, y + i / 2 * 16, autoTilePieces[i] % 4 * 16 + sx, tilemapHeight - 16 - (autoTilePieces[i] / 4 * 16 + sy), 16, 16);
         }
      }
   }

   public RubyArray getBitmaps() {
      this.needRefresh = true;
      return this.bitmaps;
   }

   public Map<Integer, TextureRegion>[] getTiles() {
      return this.tiles;
   }

   public Viewport getViewport() {
      return this.viewport;
   }

   public boolean isDisposed() {
      return this.disposed;
   }

   public Table getMap_data() {
      return this.map_data;
   }

   public Table getFlash_data() {
      return this.flash_data;
   }

   public Table getFlags() {
      return this.flags;
   }

   public boolean isVisible() {
      return this.visible;
   }

   public int getOx() {
      return this.ox;
   }

   public int getOy() {
      return this.oy;
   }

   public int getFrame_interval() {
      return this.frame_interval;
   }

   public int getFrameCount() {
      return this.frameCount;
   }

   public int getFrame() {
      return this.frame;
   }

   public int getWaterfallFrame() {
      return this.waterfallFrame;
   }

   public int getFrameInc() {
      return this.frameInc;
   }

   public boolean isNeedRefresh() {
      return this.needRefresh;
   }

   public Tilemap.TileMapLayer[] getLayers() {
      return this.layers;
   }

   public void setViewport(Viewport viewport) {
      this.viewport = viewport;
   }

   public void setDisposed(boolean disposed) {
      this.disposed = disposed;
   }

   public void setBitmaps(RubyArray bitmaps) {
      this.bitmaps = bitmaps;
   }

   public void setFlash_data(Table flash_data) {
      this.flash_data = flash_data;
   }

   public void setVisible(boolean visible) {
      this.visible = visible;
   }

   public void setOx(int ox) {
      this.ox = ox;
   }

   public void setOy(int oy) {
      this.oy = oy;
   }

   public void setFrame_interval(int frame_interval) {
      this.frame_interval = frame_interval;
   }

   public void setFrameCount(int frameCount) {
      this.frameCount = frameCount;
   }

   public void setFrame(int frame) {
      this.frame = frame;
   }

   public void setWaterfallFrame(int waterfallFrame) {
      this.waterfallFrame = waterfallFrame;
   }

   public void setFrameInc(int frameInc) {
      this.frameInc = frameInc;
   }

   public void setNeedRefresh(boolean needRefresh) {
      this.needRefresh = needRefresh;
   }

   public void setLayers(Tilemap.TileMapLayer[] layers) {
      this.layers = layers;
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof Tilemap)) {
         return false;
      } else {
         Tilemap other = (Tilemap)o;
         if (!other.canEqual(this)) {
            return false;
         } else if (!Arrays.deepEquals(this.getTiles(), other.getTiles())) {
            return false;
         } else {
            Object this$viewport = this.getViewport();
            Object other$viewport = other.getViewport();
            if (this$viewport == null ? other$viewport == null : this$viewport.equals(other$viewport)) {
               if (this.isDisposed() != other.isDisposed()) {
                  return false;
               } else {
                  Object this$bitmaps = this.getBitmaps();
                  Object other$bitmaps = other.getBitmaps();
                  if (this$bitmaps == null ? other$bitmaps == null : this$bitmaps.equals(other$bitmaps)) {
                     Object this$map_data = this.getMap_data();
                     Object other$map_data = other.getMap_data();
                     if (this$map_data == null ? other$map_data == null : this$map_data.equals(other$map_data)) {
                        Object this$flash_data = this.getFlash_data();
                        Object other$flash_data = other.getFlash_data();
                        if (this$flash_data == null ? other$flash_data == null : this$flash_data.equals(other$flash_data)) {
                           Object this$flags = this.getFlags();
                           Object other$flags = other.getFlags();
                           if (this$flags == null ? other$flags == null : this$flags.equals(other$flags)) {
                              if (this.isVisible() != other.isVisible()) {
                                 return false;
                              } else if (this.getOx() != other.getOx()) {
                                 return false;
                              } else if (this.getOy() != other.getOy()) {
                                 return false;
                              } else if (this.getFrame_interval() != other.getFrame_interval()) {
                                 return false;
                              } else if (this.getFrameCount() != other.getFrameCount()) {
                                 return false;
                              } else if (this.getFrame() != other.getFrame()) {
                                 return false;
                              } else if (this.getWaterfallFrame() != other.getWaterfallFrame()) {
                                 return false;
                              } else if (this.getFrameInc() != other.getFrameInc()) {
                                 return false;
                              } else {
                                 return this.isNeedRefresh() != other.isNeedRefresh() ? false : Arrays.deepEquals(this.getLayers(), other.getLayers());
                              }
                           } else {
                              return false;
                           }
                        } else {
                           return false;
                        }
                     } else {
                        return false;
                     }
                  } else {
                     return false;
                  }
               }
            } else {
               return false;
            }
         }
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof Tilemap;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = 1;
      result = result * 59 + Arrays.deepHashCode(this.getTiles());
      Object $viewport = this.getViewport();
      result = result * 59 + ($viewport == null ? 43 : $viewport.hashCode());
      result = result * 59 + (this.isDisposed() ? 79 : 97);
      Object $bitmaps = this.getBitmaps();
      result = result * 59 + ($bitmaps == null ? 43 : $bitmaps.hashCode());
      Object $map_data = this.getMap_data();
      result = result * 59 + ($map_data == null ? 43 : $map_data.hashCode());
      Object $flash_data = this.getFlash_data();
      result = result * 59 + ($flash_data == null ? 43 : $flash_data.hashCode());
      Object $flags = this.getFlags();
      result = result * 59 + ($flags == null ? 43 : $flags.hashCode());
      result = result * 59 + (this.isVisible() ? 79 : 97);
      result = result * 59 + this.getOx();
      result = result * 59 + this.getOy();
      result = result * 59 + this.getFrame_interval();
      result = result * 59 + this.getFrameCount();
      result = result * 59 + this.getFrame();
      result = result * 59 + this.getWaterfallFrame();
      result = result * 59 + this.getFrameInc();
      result = result * 59 + (this.isNeedRefresh() ? 79 : 97);
      return result * 59 + Arrays.deepHashCode(this.getLayers());
   }

   @Override
   public String toString() {
      return "Tilemap(tiles="
         + Arrays.deepToString(this.getTiles())
         + ", viewport="
         + this.getViewport()
         + ", disposed="
         + this.isDisposed()
         + ", bitmaps="
         + this.getBitmaps()
         + ", map_data="
         + this.getMap_data()
         + ", flash_data="
         + this.getFlash_data()
         + ", flags="
         + this.getFlags()
         + ", visible="
         + this.isVisible()
         + ", ox="
         + this.getOx()
         + ", oy="
         + this.getOy()
         + ", frame_interval="
         + this.getFrame_interval()
         + ", frameCount="
         + this.getFrameCount()
         + ", frame="
         + this.getFrame()
         + ", waterfallFrame="
         + this.getWaterfallFrame()
         + ", frameInc="
         + this.getFrameInc()
         + ", needRefresh="
         + this.isNeedRefresh()
         + ", layers="
         + Arrays.deepToString(this.getLayers())
         + ")";
   }

   private class ShadowLayer extends Tilemap.TileMapLayer {
      ShadowLayer() {
         super(5);
      }

      @Override
      public void render(JrgssBatch batch) {
         batch.begin();
         AlphaBlendingShader.setTone(new Tone(0.0F, 0.0F, 0.0F, 0.0F), batch);
         AlphaBlendingShader.setBlendColor(new Color(255, 255, 255, 0), batch);
         batch.setBlendEquation(32774, 32774);
         batch.setBlendFunction(770, 771);
         batch.setColor(Tilemap.SHADOW_COLOR);
         Viewport.begin(Tilemap.this.viewport, batch);
         int viewportX = Tilemap.this.viewport == null ? 0 : Tilemap.this.viewport.rect.x - Tilemap.this.viewport.ox;
         int viewportY = Tilemap.this.viewport == null ? 0 : Tilemap.this.viewport.rect.y - Tilemap.this.viewport.oy;
         int maxX = Math.min(Tilemap.this.map_data.dim1, (Tilemap.this.ox + 32 + Tilemap.this.viewport.getRect().getWidth()) / 32);
         int maxY = Math.min(Tilemap.this.map_data.dim2, (Tilemap.this.oy + 32 + Tilemap.this.viewport.getRect().getHeight()) / 32);

         for (int x = Math.min(Tilemap.this.ox / 32, Tilemap.this.map_data.dim1); x < maxX; x++) {
            for (int y = Math.min(Tilemap.this.oy / 32, Tilemap.this.map_data.dim2); y < maxY; y++) {
               if (x >= 0 && y >= 0) {
                  Short tile = Tilemap.this.map_data.get(x, y, 3);
                  if (tile != 0) {
                     if ((tile & 1) != 0) {
                        batch.draw(
                           Sprite.getColorTexture(), (float)(x * 32 - Tilemap.this.ox + viewportX), (float)(y * 32 - Tilemap.this.oy + viewportY), 16.0F, 16.0F
                        );
                     }

                     if ((tile & 2) != 0) {
                        batch.draw(
                           Sprite.getColorTexture(),
                           (float)(x * 32 - Tilemap.this.ox + 16 + viewportX),
                           (float)(y * 32 - Tilemap.this.oy + viewportY),
                           16.0F,
                           16.0F
                        );
                     }

                     if ((tile & 4) != 0) {
                        batch.draw(
                           Sprite.getColorTexture(),
                           (float)(x * 32 - Tilemap.this.ox + viewportX),
                           (float)(y * 32 - Tilemap.this.oy + 16 + viewportY),
                           16.0F,
                           16.0F
                        );
                     }

                     if ((tile & 8) != 0) {
                        batch.draw(
                           Sprite.getColorTexture(),
                           (float)(x * 32 - Tilemap.this.ox + 16 + viewportX),
                           (float)(y * 32 - Tilemap.this.oy + 16 + viewportY),
                           16.0F,
                           16.0F
                        );
                     }
                  }
               }
            }
         }

         batch.end();
         batch.setColor(1.0F, 1.0F, 1.0F, 1.0F);
      }

      @Override
      public String toString() {
         return "Tilemap.ShadowLayer(super=" + super.toString() + ")";
      }
   }

   private class TileMapLayer extends AbstractRenderable {
      final int layer;

      @Override
      public void render(JrgssBatch batch) {
         if (Tilemap.this.map_data.dim3 > this.layer || this.layer == 3) {
            Viewport.begin(Tilemap.this.viewport, batch);
            int viewportX = Tilemap.this.viewport == null ? 0 : Tilemap.this.viewport.rect.x - Tilemap.this.viewport.ox;
            int viewportY = Tilemap.this.viewport == null ? 0 : Tilemap.this.viewport.rect.y - Tilemap.this.viewport.oy;
            batch.begin();
            AlphaBlendingShader.setTone(new Tone(0.0F, 0.0F, 0.0F, 0.0F), batch);
            AlphaBlendingShader.setBlendColor(new Color(255, 255, 255, 0), batch);
            batch.setBlendEquation(32774, 32774);
            batch.setBlendFunction(770, 771);
            int startX = Math.min(Tilemap.this.ox / 32, Tilemap.this.map_data.dim1);
            int startY = Math.min(Tilemap.this.oy / 32, Tilemap.this.map_data.dim2);
            int maxX = Math.min(Tilemap.this.map_data.dim1, (Tilemap.this.ox + 32 + Tilemap.this.viewport.getRect().getWidth()) / 32);
            int maxY = Math.min(Tilemap.this.map_data.dim2, (Tilemap.this.oy + 32 + Tilemap.this.viewport.getRect().getHeight()) / 32);

            for (int x = startX; x < maxX; x++) {
               for (int y = startY; y < maxY; y++) {
                  if (x >= 0 && y >= 0) {
                     Short tile = Tilemap.this.map_data.get(x, y, this.layer == 3 ? 2 : this.layer);
                     Short flags = Tilemap.this.getFlags().get(tile, 0, 0);
                     if (this.layer == 2 ? (flags & 16) == 0 : this.layer != 3 || (flags & 16) != 0) {
                        boolean isCounter = (flags & 128) != 0;
                        Tilemap.this.renderTile(
                           batch,
                           tile,
                           Tilemap.this.frame,
                           Tilemap.this.waterfallFrame,
                           isCounter,
                           x * 32 - Tilemap.this.ox + viewportX,
                           y * 32 - Tilemap.this.oy + viewportY
                        );
                     }
                  }
               }
            }

            batch.end();
         }
      }

      @Override
      public int getZ() {
         switch (this.layer) {
            case 0:
               return -3;
            case 1:
               return -1;
            case 2:
               return 0;
            case 3:
               return 200;
            case 4:
               return 201;
            case 5:
               return -1;
            default:
               return 0;
         }
      }

      @Override
      public Viewport getViewport() {
         return Tilemap.this.viewport;
      }

      @Override
      public int getY() {
         return 0;
      }

      @Override
      public String toString() {
         return "Tilemap(layer=" + this.layer + ", " + Tilemap.this.viewport.toString() + ")";
      }

      @ConstructorProperties({"layer"})
      public TileMapLayer(int layer) {
         this.layer = layer;
      }

      public int getLayer() {
         return this.layer;
      }
   }
}
