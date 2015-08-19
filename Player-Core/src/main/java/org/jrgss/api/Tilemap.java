package org.jrgss.api;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;
import org.jrgss.shaders.ToneShaderProgram;
import org.jruby.Ruby;
import org.jruby.RubyArray;

import java.util.HashMap;
import java.util.Map;

/**
 * Borrowed a lot of the auto-tiling from OpenRGSS (http://openrgss.org/)
 * Created by matt on 6/27/14.
 */
@Data
public class Tilemap {

    /*
     * Borrowed from OpenRGSS (http://openrgss.org/)
     */
    private static final int[][] AUTOTILE_PARTS = new int[][]{
            new int[]{18, 17, 14, 13},
            new int[]{2, 14, 17, 18},
            new int[]{13, 3, 17, 18},
            new int[]{2, 3, 17, 18},
            new int[]{13, 14, 17, 7},
            new int[]{2, 14, 17, 7},
            new int[]{13, 3, 17, 7},
            new int[]{2, 3, 17, 7},
            new int[]{13, 14, 6, 18},
            new int[]{2, 14, 6, 18},
            new int[]{13, 3, 6, 18},
            new int[]{2, 3, 6, 18},
            new int[]{13, 14, 6, 7},
            new int[]{2, 14, 6, 7},
            new int[]{13, 3, 6, 7},
            new int[]{2, 3, 6, 7},
            new int[]{16, 17, 12, 13},
            new int[]{16, 3, 12, 13},
            new int[]{16, 17, 12, 7},
            new int[]{12, 3, 16, 7},
            new int[]{10, 9, 14, 13},
            new int[]{10, 9, 14, 7},
            new int[]{10, 9, 6, 13},
            new int[]{10, 9, 6, 7},
            new int[]{18, 19, 14, 15},
            new int[]{18, 19, 6, 15},
            new int[]{2, 19, 14, 15},
            new int[]{2, 19, 6, 15},
            new int[]{18, 17, 22, 21},
            new int[]{2, 17, 22, 21},
            new int[]{18, 3, 22, 21},
            new int[]{2, 3, 21, 22},
            new int[]{16, 19, 12, 15},
            new int[]{10, 9, 22, 21},
            new int[]{8, 9, 12, 13},
            new int[]{8, 9, 12, 7},
            new int[]{10, 11, 14, 15},
            new int[]{10, 11, 6, 15},
            new int[]{18, 19, 22, 23},
            new int[]{2, 19, 22, 23},
            new int[]{16, 17, 20, 21},
            new int[]{16, 3, 20, 21},
            new int[]{8, 11, 12, 15},
            new int[]{8, 9, 20, 21},
            new int[]{16, 19, 20, 23},
            new int[]{10, 11, 22, 23},
            new int[]{8, 11, 20, 23},
            new int[]{0, 1, 4, 5},
    };
    private static final int[][] WATERFALL_PIECES = new int[][]{
            new int[]{2, 1, 5, 6},
            new int[]{0, 1, 4, 5},
            new int[]{2, 3, 6, 7},
    };
    private static final int[][] WALL_PIECES = new int[][]{
            new int[]{10, 9, 6, 5}, new int[]{8, 9, 4, 5}, new int[]{2, 1, 6, 5}, new int[]{0, 1, 4, 5},
            new int[]{10, 11, 6, 7}, new int[]{8, 11, 4, 7}, new int[]{2, 3, 6, 7}, new int[]{0, 3, 4, 7},
            new int[]{10, 9, 14, 13}, new int[]{8, 9, 12, 13}, new int[]{2, 1, 14, 13}, new int[]{0, 1, 12, 13},
            new int[]{10, 11, 14, 15}, new int[]{10, 11, 6, 7}, new int[]{2, 3, 14, 15}, new int[]{0, 3, 12, 15}
    };
    private static final int[][] WALL_RANGES = new int[][]{
            new int[]{2288, 2335},
            new int[]{2384, 2431},
            new int[]{2480, 2527},
            new int[]{2576, 2623},
            new int[]{2672, 2719},
            new int[]{2768, 2815},
            new int[]{4736, 5119},
            new int[]{5504, 5887},
            new int[]{6272, 6655},
            new int[]{7040, 7423},
    };
    private static final int[][] ROOF_RANGES = new int[][]{
            new int[]{4352, 4735},
            new int[]{5120, 5503},
            new int[]{5888, 6271},
            new int[]{6656, 7039},
            new int[]{7424, 7807},
    };
    private static final int[][] STAIR_RANGES = new int[][]{
            new int[]{1541, 1542},
            new int[]{1549, 1550},
            new int[]{1600, 1615},
    };
    private static final int[][] TABLE_RANGES = new int[][]{
            new int[]{3152, 3199},
            new int[]{3536, 3583},
            new int[]{3920, 3967},
            new int[]{4304, 4351},
    };
    private static final Color SHADOW_COLOR = new Color(0,0,0,0.25f);

    final Map<Integer, TextureRegion>[] tiles = new Map[3];
    Viewport viewport;
    boolean disposed = false;
    RubyArray bitmaps;
    Table map_data;
    Table flash_data;
    Table flags;
    boolean visible = true;
    int ox, oy;
    int frame_interval = 30;
    int frameCount = 0;
    int frame = 0;
    int frameInc = 1;
    boolean needRefresh = false;
    SpriteBatch batch;
    TileMapLayer layers[] = new TileMapLayer[6];

    public Tilemap() {
        this(null);
    }

    public Tilemap(Viewport viewport) {
        this.viewport = viewport;
        bitmaps = RubyArray.newArray(Ruby.getGlobalRuntime(), 9);
        for (int i = 0; i < tiles.length; i++) {
            tiles[i] = new HashMap<>();
        }

        batch = new SpriteBatch();
        batch.enableBlending();
        OrthographicCamera camera = new OrthographicCamera();
        camera.setToOrtho(true, Graphics.getWidth(), Graphics.getHeight());
        batch.setProjectionMatrix(camera.combined);

        for(int i = 0; i < 6; i++) {
            layers[i] = i!=5?new TileMapLayer(i):new ShadowLayer();
        }

        Gdx.app.log("Tilemap", "New Tilemap has been created.");
    }

    private static boolean inRange(int[][] ranges, int data) {
        for (int[] range : ranges) {
            if (data >= range[0] && data <= range[1]) return true;
        }
        return false;
    }

    private static boolean isWall(int data) {
        if (data > 7807) return true;
        return inRange(WALL_RANGES, data);
    }

    private static boolean isRoof(int data) {
        return inRange(ROOF_RANGES, data);
    }

    private static boolean isSoil(int data) {
        if (data >= 2816 && data <= 4351) return !isTable(data);
        if (data > 1663) return !isStair(data);
        return false;
    }

    private static boolean isStair(int data) {
        return inRange(STAIR_RANGES, data);
    }

    private static boolean isTable(int data) {
        return inRange(TABLE_RANGES, data);
    }


    public void dispose() {
        batch.dispose();
        for(TileMapLayer layer : layers) {
            layer.dispose();
        }

        Gdx.app.log("Tilemap","Disposed of Tilemap");
    }

    public void setMap_data(Table data) {
        this.map_data = data;

    }

    public void setFlags(Table flags) {
        this.flags = flags;
        Gdx.app.log("Tilemap", "Flags have "+flags.dim1+ " elements");
        for(int x = 0; x < 6; x++) {
            Gdx.app.log("Tilemap", "Flags are "+flags.get(map_data.get(x, 0, 2), 0, 0));
        }
    }




    public void update() {
        frameCount = (frameCount + 1) % frame_interval;
        if (frameCount == 0) {
            frame = frame + frameInc;
            if (frame == 2 || frame == 0) {
                frameInc = -frameInc;
            }
        }
    }


    public void renderTile(int id, int frame, int x, int y) {
        //TextureRegion existing = tiles[frame].get(id);
        //if (existing != null) return existing;
        Texture tilemap;
        int tilemapHeight;
        int subId;
        boolean wasDrawing = this.batch.isDrawing();
        if (id < 1024) {
            subId = id % 256;
            tilemap = ((Bitmap) bitmaps.get((id / 256) + 5)).getRegion().getTexture();
            tilemapHeight = tilemap.getHeight();
            if (subId < 128) {
                batch.draw(tilemap,x, y, (subId % 8) * 32, (tilemapHeight - 32) - ((subId / 8) * 32), 32, 32);
            } else {
                subId = subId - 128;
                batch.draw(tilemap,x, y,  (subId % 8) * 32 + 256, (tilemapHeight - 32) - (subId / 8) * 32, 32, 32);
            }
        } else if (id < 1664) {
            tilemap = ((Bitmap) bitmaps.get(4)).getRegion().getTexture();
            tilemapHeight = tilemap.getHeight();
            subId = id - 1536;
            batch.draw(tilemap,x, y,  (subId % 8) * 32, (tilemapHeight - 32) - (subId / 8) * 32, 32, 32);
        } else if (id < 2816) {
            subId = id - 2048;
            int autotile = subId / 48;
            int autoId = subId % 48;
            tilemap = ((Bitmap) bitmaps.get(0)).getRegion().getTexture();
            tilemapHeight = tilemap.getHeight();
            int sx = 0, sy = 0;
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
                    sx = 256 + (frame * 64);
                    sy = 0;
                    break;
                case 5:
                    sx = 448;
                    sy = frame * 32;
                    break;
                case 6:
                    sx = 256 + (frame * 64);
                    sy = 96;
                    break;
                case 7:
                    sx = 448;
                    sy = 96 + (frame * 32);
                    break;
                case 8:
                    sx = frame * 64;
                    sy = 192;
                    break;
                case 9:
                    sx = 192;
                    sy = 192 + (frame * 32);
                    break;
                case 10:
                    sx = frame * 64;
                    sy = 288;
                    break;
                case 11:
                    sx = 192;
                    sy = 288 + (frame * 32);
                    break;
                case 12:
                    sx = 256 + (frame * 64);
                    sy = 192;
                    break;
                case 13:
                    sx = 448;
                    sy = 192 + (frame * 32);
                    break;
                case 14:
                    sx = 256 + (frame * 64);
                    sy = 288;
                    break;
                case 15:
                    sx = 448;
                    sy = 288 + (frame * 32);
                    break;
                default:
                    Gdx.app.log("Tilemap", "Unsupported Autotile value: " + autotile);
            }
            //newRegion = new TextureRegion(tilemap, sx, (tilemapHeight - 32) - sy, 32, 32);


            int[] autoTilePieces = isWall(id) ? WATERFALL_PIECES[autoId] : AUTOTILE_PARTS[autoId];

            for (int i = 0; i < 4; i++) {
                batch.draw(tilemap,x+(i % 2) * 16, y + (i / 2) * 16, (autoTilePieces[i] % 4) * 16 + sx, (tilemapHeight - 16) - ((autoTilePieces[i] / 4) * 16 + sy), 16, 16);
            }

        } else if (id < 4352) {
            subId = id - 2816;
            int autotile = subId / 48;
            int autoId = subId % 48;
            tilemap = ((Bitmap) bitmaps.get(1)).getRegion().getTexture();
            tilemapHeight = tilemap.getHeight();
            int sx = (autotile % 8) * 64;
            int sy = (autotile / 8) * 96;
            int[] autoTilePieces = AUTOTILE_PARTS[autoId];

            for (int i = 0; i < 4; i++) {
                batch.draw(tilemap,x+(i % 2) * 16, y + (i / 2) * 16, (autoTilePieces[i] % 4) * 16 + sx, (tilemapHeight - 16) - ((autoTilePieces[i] / 4) * 16 + sy), 16, 16);
            }

        } else if (id < 5888) {
            subId = id - 4352;
            int autotile = subId / 48;
            int autoId = subId % 48;
            tilemap = ((Bitmap) bitmaps.get(2)).getRegion().getTexture();
            tilemapHeight = tilemap.getHeight();
            int sx = (autotile % 8) * 64;
            int sy = (autotile / 8) * 64;
            int[] autoTilePieces = WALL_PIECES[autoId];

            for (int i = 0; i < 4; i++) {
                batch.draw(tilemap,x+(i % 2) * 16, y + (i / 2) * 16, (autoTilePieces[i] % 4) * 16 + sx, (tilemapHeight - 16) - ((autoTilePieces[i] / 4) * 16 + sy), 16, 16);
            }

        } else {
            subId = id - 5888;
            int autotile = subId / 48;
            int autoId = subId % 48;
            tilemap = ((Bitmap) bitmaps.get(3)).getRegion().getTexture();
            tilemapHeight = tilemap.getHeight();
            int sx = (autotile % 8) * 64;
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
                batch.draw(tilemap,x+(i % 2) * 16, y + (i / 2) * 16, (autoTilePieces[i] % 4) * 16 + sx, (tilemapHeight - 16) - ((autoTilePieces[i] / 4) * 16 + sy), 16, 16);
            }
        }

    }

    public RubyArray getBitmaps() {
        needRefresh = true;
        return bitmaps;
    }

    @AllArgsConstructor
    private class TileMapLayer extends AbstractRenderable {

        @Getter
        final int layer;

        @Override
        public void render(SpriteBatch _) {

            if(map_data.dim3 <= layer && layer != 3) return;



            batch.begin();
            for (int x = Math.min(ox/32, map_data.dim1); x < Math.min(map_data.dim1, (ox+32+viewport.getRect().getWidth())/32); x++) {
                for (int y = Math.min(oy/32, map_data.dim2); y < Math.min(map_data.dim2, (oy+32+viewport.getRect().getHeight())/32); y++) {

                    Short tile = map_data.get(x, y, layer==3?2:layer);
                    Short flags = getFlags().get(tile, 0,0);

                    if(layer == 2) {
                        if((flags&0b10000) != 0) {
                            continue;
                        }
                    } else if(layer == 3) {
                        if((flags&0b10000) == 0) {
                            continue;
                        }
                    }

                    renderTile(tile, frame, (x * 32) -ox, (y * 32) - oy );
                }
            }
            batch.end();



        }

        @Override
        public int getZ() {
            switch (layer) {
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
                    return -2;
                default:
                    return 0;
            }
        }

        @Override
        public Viewport getViewport() {
            return viewport;
        }

        @Override
        public int getY() {
            return 0;
        }

        public String toString() {
            return "Tilemap(layer="+layer+", "+viewport.toString()+")";
        }
    }

    @ToString(callSuper = true)
    private class ShadowLayer extends TileMapLayer {

        public ShadowLayer() {
            super(5);
        }

        @Override
        public void render(SpriteBatch _) {
            batch.begin();
            batch.setColor(SHADOW_COLOR);
            for (int x = Math.min(ox/32, map_data.dim1); x < Math.min(map_data.dim1, (ox+32+viewport.getRect().getWidth())/32); x++) {
                for (int y = Math.min(oy/32, map_data.dim2); y < Math.min(map_data.dim2, (oy+32+viewport.getRect().getHeight())/32); y++) {

                    Short tile = map_data.get(x, y, 3);
                    if(tile != 0) {
                        if((tile&0b1) != 0) {
                            batch.draw(Sprite.getColorTexture(), (x * 32) - ox, (y * 32) - oy, 16, 16);
                        }
                        if((tile&0b10) != 0) {
                            batch.draw(Sprite.getColorTexture(), (x * 32) - ox + 16, (y * 32) - oy, 16, 16);
                        }
                        if((tile&0b100) != 0) {
                            batch.draw(Sprite.getColorTexture(), (x * 32) - ox, (y * 32) - oy + 16, 16, 16);
                        }
                        if((tile&0b1000) != 0) {
                            batch.draw(Sprite.getColorTexture(), (x * 32) - ox + 16, (y * 32) - oy + 16, 16, 16);
                        }
                    }
                }
            }
            batch.end();
            batch.setColor(1f,1f,1f,1f);
        }
    }


}
