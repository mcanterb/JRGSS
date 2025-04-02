package org.jrgss.api;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.BitmapFontData;
import com.badlogic.gdx.graphics.g2d.BitmapFont.Glyph;
import com.badlogic.gdx.graphics.g2d.JrgssBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.utils.LongMap;
import com.badlogic.gdx.utils.ScreenUtils;
import org.jrgss.FileUtil;
import org.jrgss.JRGSSGame;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.CharBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class Bitmap {
    private static final List<Bitmap> dependencies = new ArrayList<>();
    private static final AtomicLong counter = new AtomicLong(0L);
    private static final ReferenceQueue<Bitmap> references = new ReferenceQueue<>();
    private static final LongMap<Bitmap.BitmapReference> table = new LongMap<>();
    private static final Field isDisposedPixmapField;
    static JrgssBatch batch;
    static CharBuffer charBuffer = CharBuffer.allocate(1);

    static {
        try {
            isDisposedPixmapField = Pixmap.class.getDeclaredField("disposed");
            isDisposedPixmapField.setAccessible(true);
        } catch (Exception var1) {
            throw new RuntimeException(var1);
        }
    }

    private final long id;
    private final List<Runnable> reloadRunnables;
    String path;
    Font font = new Font();
    int width;
    int height;
    FrameBuffer frameBuffer;
    TextureRegion region;
    AtomicReference<Pixmap> pixmap = new AtomicReference<>();
    OrthographicCamera camera;
    AtomicBoolean isDisposed = new AtomicBoolean(false);
    double loadedScale;
    private boolean hiRes;

    public Bitmap(Pixmap img) {
        this.id = counter.getAndIncrement();
        this.loadedScale = 0.0;
        this.reloadRunnables = new ArrayList<>();
        this.hiRes = false;
        this.frameBuffer = new FrameBuffer(Format.RGBA8888, img.getWidth(), img.getHeight(), false);
        this.frameBuffer.getColorBufferTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        this.camera = new OrthographicCamera(this.width, this.height);
        this.camera.setToOrtho(true, this.width, this.height);
        OrthographicCamera camera = new OrthographicCamera();
        camera.setToOrtho(false, img.getWidth(), img.getHeight());
        ((Buffer) img.getPixels()).rewind();
        JrgssBatch batch = this.getBatch();
        batch.setProjectionMatrix(camera.combined);
        Texture t = new Texture(img);
        this.frameBuffer.begin();
        batch.begin();
        Gdx.gl.glClearColor(1.0F, 1.0F, 1.0F, 0.0F);
        Gdx.gl.glClear(16384);
        batch.draw(t, 0.0F, 0.0F);
        batch.end();
        this.frameBuffer.end();
        this.region = new TextureRegion(this.frameBuffer.getColorBufferTexture());
        this.width = img.getWidth();
        this.height = img.getHeight();
        t.dispose();
        this.register();
    }

    public Bitmap(String path) {
        this.id = counter.getAndIncrement();
        this.loadedScale = 0.0;
        this.reloadRunnables = new ArrayList<>();
        this.hiRes = false;
        FileHandle file = FileUtil.loadImg(path);
        Pixmap img = new Pixmap(file);
        long endTime = System.currentTimeMillis();
        this.path = path;
        this.width = img.getWidth();
        this.height = img.getHeight();
        this.camera = new OrthographicCamera(this.width, this.height);
        this.camera.setToOrtho(true, this.width, this.height);
        JRGSSGame.runWithGLContext(() -> {
            this.frameBuffer = new FrameBuffer(Format.RGBA8888, img.getWidth(), img.getHeight(), false);
            OrthographicCamera camera1 = new OrthographicCamera();
            camera1.setToOrtho(false, img.getWidth(), img.getHeight());
            JrgssBatch batch = this.getBatch();
            batch.disableBlending();
            batch.setProjectionMatrix(camera1.combined);
            this.frameBuffer.begin();
            batch.begin();
            Gdx.gl.glClearColor(1.0F, 1.0F, 1.0F, 0.0F);
            Gdx.gl.glClear(16384);
            Texture t = new Texture(img);
            batch.draw(t, 0.0F, 0.0F);
            batch.end();
            this.frameBuffer.end();
            this.region = new TextureRegion(this.frameBuffer.getColorBufferTexture());
            this.frameBuffer.getColorBufferTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
            disposeImg(img);
            t.dispose();
            this.register();
        });
    }

    public Bitmap(int width, int height) {
        this.id = counter.getAndIncrement();
        this.loadedScale = 0.0;
        this.reloadRunnables = new ArrayList<>();
        this.hiRes = false;
        this.width = Math.min(width, 2048);
        this.height = Math.min(height, 4096);
        this.camera = new OrthographicCamera(width, height);
        this.camera.setToOrtho(true, width, height);
        this.hiRes = true;
        this.loadedScale = Graphics.getHiResScale();
        this.runAndRecord(() -> {
            if (width != 0 && height != 0) {
                this.frameBuffer = new FrameBuffer(Format.RGBA8888, this.scale(this.width), this.scale(this.height), false);
            } else {
                this.frameBuffer = new FrameBuffer(Format.RGBA8888, 2, 2, false);
            }

            this.hiRes = true;
            this.frameBuffer.getColorBufferTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
            this.frameBuffer.begin();
            Gdx.gl.glClearColor(1.0F, 1.0F, 1.0F, 0.0F);
            Gdx.gl.glClear(16384);
            this.frameBuffer.end();
            this.region = new TextureRegion(this.frameBuffer.getColorBufferTexture());
            this.register();
        });
    }

    public static void cleanup() {
        Bitmap.BitmapReference ref;
        while ((ref = (Bitmap.BitmapReference) references.poll()) != null) {
            if (table.containsKey(ref.id)) {
                JRGSSGame.runWithGLContext(ref::dispose);
                Gdx.app.log("Bitmap", "Cleaned up an old bitmap");
            }
        }
    }

    public static void reloadAll() {
        for (Bitmap.BitmapReference ref : table.values()) {
            Bitmap b = ref.get();
            if (b != null) {
                b.reload();
            }
        }
    }

    public static int clamp(int x, int a, int b) {
        return x < a ? a : (Math.min(x, b));
    }

    public static void blur(IntBuffer in, IntBuffer out, int width, int height, int radius) {
        int widthMinus1 = width - 1;
        int tableSize = 2 * radius + 1;
        int[] divide = new int[256 * tableSize];

        for (int i = 0; i < 256 * tableSize; i++) {
            divide[i] = i / tableSize;
        }

        int inIndex = 0;

        for (int y = 0; y < height; y++) {
            int outIndex = y;
            int ta = 0;
            int tr = 0;
            int tg = 0;
            int tb = 0;

            for (int i = -radius; i <= radius; i++) {
                int rgb = in.get(inIndex + clamp(i, 0, width - 1));
                ta += rgb >> 24 & 0xFF;
                tr += rgb >> 16 & 0xFF;
                tg += rgb >> 8 & 0xFF;
                tb += rgb & 0xFF;
            }

            for (int x = 0; x < width; x++) {
                out.put(outIndex, divide[ta] << 24 | divide[tr] << 16 | divide[tg] << 8 | 0xFF);
                int i1 = x + radius + 1;
                if (i1 > widthMinus1) {
                    i1 = widthMinus1;
                }

                int i2 = x - radius;
                if (i2 < 0) {
                    i2 = 0;
                }

                int rgb1 = in.get(inIndex + i1);
                int rgb2 = in.get(inIndex + i2);
                ta += (rgb1 >> 24 & 0xFF) - (rgb2 >> 24 & 0xFF);
                tr += (rgb1 >> 16 & 0xFF) - (rgb2 >> 16 & 0xFF);
                tg += (rgb1 >> 8 & 0xFF) - (rgb2 >> 8 & 0xFF);
                tb += (rgb1 & 0xFF) - (rgb2 & 0xFF);
                outIndex += height;
            }

            inIndex += width;
        }
    }

    private static Glyph getGlyph(BitmapFontData data, char c) {
        Glyph glyph = data.getGlyph(c);
        return glyph == null ? data.getGlyph('ÿ') : glyph;
    }

    private static void disposeImg(Pixmap img) {
        try {
            boolean isDisposed = isDisposedPixmapField.getBoolean(img);
            if (!isDisposed) {
                img.dispose();
            } else {
                img.dispose();
            }
        } catch (Exception var2) {
            throw new RuntimeException(var2);
        }
    }

    private void register() {
        table.put(this.id, new Bitmap.BitmapReference(this, references));
    }

    private void runAndRecord(Runnable r) {
        JRGSSGame.runWithGLContext(r);
        if (this.hiRes) {
            this.reloadRunnables.add(r);
        }
    }

    private void reload() {
        if (this.hiRes && !(Math.abs(Graphics.getHiResScale() - this.loadedScale) < 1.0E-4)) {
            Gdx.app.log("Bitmap", "Reloading bitmap " + this.id + ". It has " + this.reloadRunnables.size() + " runnables");
            this.loadedScale = Graphics.getHiResScale();

            for (Bitmap b : dependencies) {
                b.reload();
            }

            table.remove(this.id);
            if (this.pixmap.get() != null) {
                disposeImg(this.pixmap.get());
            }

            this.frameBuffer.dispose();

            for (Runnable r : this.reloadRunnables) {
                r.run();
            }
        }
    }

    private JrgssBatch getBatch() {
        if (batch == null) {
            batch = new JrgssBatch();
        }

        batch.setBlendFunction(770, 771);
        batch.enableBlending();
        batch.setProjectionMatrix(this.camera.combined);
        batch.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        return batch;
    }

    public Bitmap clone() {
        return new Bitmap(this.getPixmap());
    }

    public void hue_change(float hue) {
        hue = hue / 360.0F - (int) (hue / 360.0F);
        if (hue < 0.0F) {
            hue = 1.0F - hue;
        }
        final float finalHue = hue;
        Gdx.app.log("Bitmap", "Hue change to " + hue);
        this.runAndRecord(() -> {
            Pixmap p = Bitmap.this.getPixmap();
            IntBuffer inPixels = p.getPixels().duplicate().asIntBuffer();
            IntBuffer outPixels = IntBuffer.allocate(inPixels.remaining());

            while (inPixels.remaining() > 0) {
                Color c = new Color(inPixels.get());
                float[] hsv = Bitmap.this.rgbToHSV(c.getRed() / 255.0F, c.getGreen() / 255.0F, c.getBlue() / 255.0F);
                hsv[0] = finalHue;
                outPixels.put(Bitmap.this.hsvToRGB(hsv, c.getAlpha()));
            }

            ((Buffer) inPixels).rewind();
            inPixels.put(outPixels);
            ((Buffer) inPixels).rewind();
            OrthographicCamera camera = new OrthographicCamera();
            camera.setToOrtho(false, Bitmap.this.getWidth(), Bitmap.this.getHeight());
            JrgssBatch batch = Bitmap.this.getBatch();
            batch.setProjectionMatrix(camera.combined);
            Bitmap.this.frameBuffer.begin();
            batch.begin();
            Gdx.gl.glClearColor(1.0F, 1.0F, 1.0F, 0.0F);
            Gdx.gl.glClear(16384);
            batch.draw(new Texture(p), 0.0F, 0.0F);
            batch.end();
            Bitmap.this.frameBuffer.end();
            Bitmap.this.region = new TextureRegion(Bitmap.this.frameBuffer.getColorBufferTexture());
            if (Bitmap.this.pixmap.get() != null) {
                Bitmap.disposeImg(Bitmap.this.pixmap.get());
            }

            Bitmap.this.pixmap.set(null);
        });
    }

    private int hsvToRGB(float[] hsv, int alpha) {
        if (hsv[1] == 0.0F) {
            return new Color((int) (hsv[2] * 255.0F), (int) (hsv[2] * 255.0F), (int) (hsv[2] * 255.0F), alpha).toPackedInt();
        } else {
            float h = hsv[0] * 6.0F;
            if (h >= 6.0F) {
                h = 0.0F;
            }

            int i = (int) h;
            float v1 = hsv[2] * (1.0F - hsv[1]);
            float v2 = hsv[2] * (1.0F - hsv[1] * (h - i));
            float v3 = hsv[2] * (1.0F - hsv[1] * (1.0F - h + i));
            float r;
            float g;
            float b;
            switch (i) {
                case 0:
                    r = hsv[2];
                    g = v3;
                    b = v1;
                    break;
                case 1:
                    r = v2;
                    g = hsv[2];
                    b = v1;
                    break;
                case 2:
                    r = v1;
                    g = hsv[2];
                    b = v3;
                    break;
                case 3:
                    r = v1;
                    g = v2;
                    b = hsv[2];
                    break;
                case 4:
                    r = v3;
                    g = v1;
                    b = hsv[2];
                    break;
                default:
                    r = hsv[2];
                    g = v1;
                    b = v2;
            }

            return new Color((int) (r * 255.0F), (int) (g * 255.0F), (int) (b * 255.0F), alpha).toPackedInt();
        }
    }

    private float[] rgbToHSV(float r, float g, float b) {
        float min = Math.min(Math.min(r, g), b);
        float max = Math.max(Math.max(r, g), b);
        float delta = max - min;
        float[] hsv = new float[]{0.0F, 0.0F, max};
        if (delta == 0.0F) {
            return hsv;
        } else {
            hsv[1] = delta / max;
            float deltaR = ((max - r) / 6.0F + max / 2.0F) / max;
            float deltaG = ((max - g) / 6.0F + max / 2.0F) / max;
            float deltaB = ((max - b) / 6.0F + max / 2.0F) / max;
            if (r == max) {
                hsv[0] = deltaB - deltaG;
            }

            if (g == max) {
                hsv[0] = 0.33333334F + r - b;
            }

            if (b == max) {
                hsv[0] = 0.6666667F + g - r;
            }

            if (hsv[0] < 0.0F) {
                hsv[0]++;
            }

            if (hsv[0] > 1.0F) {
                hsv[0]--;
            }

            return hsv;
        }
    }

    public Rect rect() {
        return new Rect(0, 0, this.width, this.height);
    }

    public Pixmap getPixmap() {
        if (this.isDisposed()) {
            Gdx.app.log("Bitmap", "Calling GetPixmap on Disposed Bitmap!");
            throw new IllegalArgumentException("Bitmap is disposed. Cannot get pixmap");
        } else if (this.pixmap.get() == null) {
            this.frameBuffer.begin();
            Pixmap ret = new Pixmap(this.getWidth(), this.getHeight(), Format.RGBA8888);
            byte[] bytes = ScreenUtils.getFrameBufferPixels(0, 0, this.getWidth(), this.getHeight(), true);
            ((Buffer) ret.getPixels().put(bytes)).rewind();
            this.frameBuffer.end();
            this.pixmap.set(ret);
            return ret;
        } else {
            return this.pixmap.get();
        }
    }

    public void setPixmap(AtomicReference<Pixmap> pixmap) {
        this.pixmap = pixmap;
    }

    public void blt(int x, int y, Bitmap src_bitmap, Rect src_rect) {
        this.blt(x, y, src_bitmap, src_rect, 255);
    }

    public void blt(int x, int y, Bitmap src_bitmap, Rect src_rect, int opacity) {
        this.internalBlit(x, y, src_rect.getWidth(), src_rect.getHeight(), src_bitmap, src_rect, opacity);
    }

    private void internalBlit(int x, int y, int width, int height, Bitmap srcBitmap, Rect srcRect, int opacity) {
        dependencies.add(srcBitmap);
        this.runAndRecord(() -> {
            JrgssBatch batch = this.getBatch();
            batch.enableBlending();
            this.frameBuffer.begin();
            batch.setBlendFunction(-1, -1);
            batch.enableBlending();
            batch.setColor(1.0F, 1.0F, 1.0F, opacity / 255.0F);
            Gdx.gl20.glBlendFuncSeparate(770, 771, 1, 1);
            Gdx.gl20.glBlendEquationSeparate(32774, 32774);
            if (this.hiRes) {
                OrthographicCamera camera = new OrthographicCamera(this.scale(this.width), this.scale(this.height));
                camera.setToOrtho(true, this.scale(this.width), this.scale(this.height));
                batch.setProjectionMatrix(camera.combined);
                batch.begin();
                srcBitmap.render(batch, this.scale(x), this.scale(y), this.scale(width), this.scale(height), srcRect);
            } else {
                batch.begin();
                srcBitmap.render(batch, x, y, width, height, srcRect);
            }

            batch.end();
            this.frameBuffer.end();
            if (this.pixmap.get() != null) {
                disposeImg(this.pixmap.get());
            }

            this.pixmap.set(null);
        });
    }

    public void stretch_blt(Rect dest_rect, Bitmap src_bitmap, Rect src_rect, int opacity) {
        this.internalBlit(dest_rect.getX(), dest_rect.getY(), dest_rect.getWidth(), dest_rect.getHeight(), src_bitmap, src_rect, opacity);
    }

    public void stretch_blt(Rect dest_rect, Bitmap src_bitmap, Rect src_rect) {
        this.internalBlit(dest_rect.getX(), dest_rect.getY(), dest_rect.getWidth(), dest_rect.getHeight(), src_bitmap, src_rect, 255);
    }

    public void dispose() {
        if (!this.isDisposed.get()) {
            this.isDisposed.set(true);
            JRGSSGame.runWithGLContext(() -> {
                this.frameBuffer.dispose();
                if (this.pixmap.get() != null) {
                    disposeImg(this.pixmap.get());
                }

                table.remove(this.id);
            });
        }
    }

    public void blur() {
        this.runAndRecord(() -> {
            Pixmap p = this.getPixmap();
            IntBuffer inPixels = p.getPixels().asIntBuffer();
            IntBuffer outPixels = IntBuffer.allocate(inPixels.remaining());
            blur(inPixels, outPixels, this.getWidth(), this.getHeight(), 2);
            blur(outPixels, inPixels, this.getHeight(), this.getWidth(), 2);
            OrthographicCamera camera1 = new OrthographicCamera();
            camera1.setToOrtho(false, this.getWidth(), this.getHeight());
            Batch batch1 = new JrgssBatch();
            batch1.setProjectionMatrix(camera1.combined);
            this.frameBuffer.begin();
            batch1.begin();
            Gdx.gl.glClearColor(1.0F, 1.0F, 1.0F, 0.0F);
            Gdx.gl.glClear(16384);
            Texture t = new Texture(p);
            batch1.draw(t, 0.0F, 0.0F);
            batch1.end();
            this.frameBuffer.end();
            this.region = new TextureRegion(this.frameBuffer.getColorBufferTexture());
            batch1.dispose();
            if (this.pixmap.get() != null) {
                disposeImg(this.pixmap.get());
            }

            this.pixmap.set(null);
            t.dispose();
        });
    }

    public void gradient_fill_rect(Rect rect, Color color1, Color color2) {
        this.gradient_fill_rect(rect, color1, color2, false);
    }

    public void gradient_fill_rect(Rect rect, Color color1, Color color2, boolean vertical) {
        this.gradient_fill_rect(rect.x, rect.y, rect.width, rect.height, color1, color2, vertical);
    }

    public void gradient_fill_rect(int x, int y, int width, int height, Color color1, Color color2) {
        this.gradient_fill_rect(x, y, width, height, color1, color2, false);
    }

    public void gradient_fill_rect(int x, int y, int width, int height, Color color1, Color color2, boolean vertical) {
        this.runAndRecord(() -> {
            ShapeRenderer shapeRenderer = new ShapeRenderer();
            shapeRenderer.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            shapeRenderer.setProjectionMatrix(this.camera.combined);
            this.frameBuffer.begin();
            shapeRenderer.begin(ShapeType.Filled);
            if (vertical) {
                shapeRenderer.rect(x, y, width, height, color1.toGDX(), color1.toGDX(), color2.toGDX(), color2.toGDX());
            } else {
                shapeRenderer.rect(x, y, width, height, color1.toGDX(), color2.toGDX(), color2.toGDX(), color1.toGDX());
            }

            shapeRenderer.end();
            shapeRenderer.dispose();
            this.frameBuffer.end();
            if (this.pixmap.get() != null) {
                disposeImg(this.pixmap.get());
            }

            this.pixmap.set(null);
        });
    }

    public void fill_rect(Rect rect, Color color) {
        this.fill_rect(rect.x, rect.y, rect.width, rect.height, color);
    }

    public void fill_rect(int x, int y, int width, int height, Color color) {
        this.runAndRecord(() -> {
            JrgssBatch batch = this.getBatch();
            batch.setBlendFunction(-1, -1);
            batch.enableBlending();
            Gdx.gl20.glBlendFuncSeparate(770, 771, 1, 1);
            Gdx.gl20.glBlendEquationSeparate(32774, 32774);
            batch.setProjectionMatrix(this.camera.combined);
            this.frameBuffer.begin();
            batch.begin();
            batch.setColor(color.toGDX());
            batch.draw(Sprite.getColorTexture(), (float) x, (float) y, (float) width, (float) height);
            batch.end();
            batch.enableBlending();
            batch.setBlendFunction(770, 771);
            this.frameBuffer.end();
            if (this.pixmap.get() != null) {
                disposeImg(this.pixmap.get());
            }

            this.pixmap.set(null);
        });
    }

    public void clear_rect(int x, int y, int width, int height) {
        this.runAndRecord(() -> {
            JrgssBatch batch = this.getBatch();
            batch.disableBlending();
            batch.setProjectionMatrix(this.camera.combined);
            this.frameBuffer.begin();
            batch.begin();
            batch.setColor(1.0F, 1.0F, 1.0F, 0.0F);
            batch.draw(Sprite.getColorTexture(), (float) x, (float) y, (float) width, (float) height);
            batch.end();
            batch.enableBlending();
            this.frameBuffer.end();
            if (this.pixmap.get() != null) {
                disposeImg(this.pixmap.get());
            }

            this.pixmap.set(null);
        });
    }

    public void clear_rect(Rect rect) {
        this.clear_rect(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
    }

    public void render(Batch batch, int x, int y) {
        if (this.hiRes) {
            batch.draw(this.region, (float) x, (float) y, (float) this.width, (float) this.height);
        } else {
            batch.draw(this.region, (float) x, (float) y);
        }
    }

    public void render(Batch batch, int x, int y, int srcx, int srcy, int srcwidth, int srcheight) {
        if (this.isDisposed.get()) {
            Gdx.app.log("Bitmap", "Trying to draw a disposed Bitmap. BUG");
            throw new RuntimeException("Trying to draw a disposed Bitmap");
        } else {
            if (this.frameBuffer != null) {
                if (!this.hiRes) {
                    batch.draw(
                        this.frameBuffer.getColorBufferTexture(), x, y, srcwidth, srcheight, srcx, this.height - srcheight - srcy, srcwidth, srcheight, false, false
                    );
                } else {
                    batch.draw(
                        this.frameBuffer.getColorBufferTexture(),
                        x,
                        y,
                        srcwidth,
                        srcheight,
                        this.scale(srcx),
                        this.scale(this.height - srcheight - srcy),
                        this.scale(srcwidth),
                        this.scale(srcheight),
                        false,
                        false
                    );
                }
            }
        }
    }

    public void render(Batch batch, int x, int y, Rect srcRect) {
        this.render(batch, x, y, srcRect.getWidth(), srcRect.getHeight(), srcRect);
    }

    public void render(Batch batch, int x, int y, int width, int height, Rect srcRect) {
        if (this.frameBuffer != null) {
            if (!this.hiRes) {
                batch.draw(
                    this.frameBuffer.getColorBufferTexture(),
                    x,
                    y,
                    width,
                    height,
                    srcRect.getX(),
                    this.height - srcRect.getHeight() - srcRect.getY(),
                    srcRect.getWidth(),
                    srcRect.getHeight(),
                    false,
                    false
                );
            } else {
                batch.draw(
                    this.frameBuffer.getColorBufferTexture(),
                    x,
                    y,
                    width,
                    height,
                    this.scale(srcRect.getX()),
                    this.scale(this.height - srcRect.getHeight() - srcRect.getY()),
                    this.scale(srcRect.getWidth()),
                    this.scale(srcRect.getHeight()),
                    false,
                    false
                );
            }
        }
    }

    private int scale(int value) {
        return (int) (value * this.loadedScale);
    }

    public boolean isDisposed() {
        return this.isDisposed.get();
    }

    public void clear() {
        this.runAndRecord(new Runnable() {
            @Override
            public void run() {
                Bitmap.this.frameBuffer.begin();
                Gdx.gl.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
                Gdx.gl.glClear(16384);
                Bitmap.this.frameBuffer.end();
            }
        });
        if (this.pixmap.get() != null) {
            disposeImg(this.pixmap.get());
        }

        this.pixmap.set(null);
    }

    public Color get_pixel(int x, int y) {
        Pixmap p = this.getPixmap();
        return new Color(p.getPixel(x, y));
    }

    public void set_pixel(int x, int y, Color c) {
        Pixmap temp = new Pixmap(1, 1, Format.RGBA8888);
        this.runAndRecord(() -> {
            Texture texture = new Texture(temp);
            JrgssBatch batch = this.getBatch();
            this.frameBuffer.begin();
            batch.begin();
            batch.draw(texture, (float) x, (float) y);
            batch.end();
            this.frameBuffer.end();
        });
        if (this.pixmap.get() != null) {
            disposeImg(this.pixmap.get());
        }

        this.pixmap.set(null);
    }

    public Rect text_size(String str) {
        BitmapFont bf = this.font.getBitmapFont();
        if (bf == null) {
            Gdx.app.log("Bitmap", "Bitmap font is null.");
            Graphics.wait(1);
            bf = this.font.getBitmapFont();
        }

        return this.text_size(str, bf);
    }

    private Rect text_size(String str, BitmapFont font) {
        int width = 0;
        BitmapFontData data = font.getData();

        for (char c : str.toCharArray()) {
            Glyph glyph = getGlyph(data, c);
            width = (int) (width + (c == ' ' ? font.getData().spaceXadvance : glyph.xadvance));
        }

        return new Rect(0, 0, (int) (width / this.loadedScale), (int) Math.ceil(font.getCapHeight() / this.loadedScale));
    }

    private Rect text_size_real(String str, BitmapFont font) {
        int width = 0;
        BitmapFontData data = font.getData();

        for (char c : str.toCharArray()) {
            Glyph glyph = getGlyph(data, c);
            width = (int) (width + (c == ' ' ? font.getData().spaceXadvance : glyph.xadvance));
        }

        return new Rect(0, 0, width, (int) font.getCapHeight());
    }

    public void draw_text(int x1, int y1, int width1, int height1, Object obj, int align) {
        String string = obj.toString();
        Font font = new Font(this.font);
        this.runAndRecord(() -> {
            JrgssBatch batch = this.getBatch();
            int x = this.scale(x1);
            int y = this.scale(y1);
            int width = this.scale(width1);
            int height = this.scale(height1);
            OrthographicCamera camera = new OrthographicCamera(this.scale(this.width), this.scale(this.height));
            camera.setToOrtho(true, this.scale(this.width), this.scale(this.height));
            batch.setProjectionMatrix(camera.combined);
            BitmapFont f = font.getBitmapFont();
            BitmapFont outline = font.getOutlineFont();
            Color innerColor = font.getColor();
            Color outerColor = font.getOutColor();
            f.setColor(innerColor.toGDX());
            outline.setColor(outerColor.toGDX());
            Rect fontRect = this.text_size_real(string, font.getBitmapFont());
            if (!this.hiRes) {
                throw new RuntimeException("Cannot draw font on a low res bitmap");
            } else {
                if (fontRect.width > width) {
                    float xScale = Math.max(0.6F, (float) width / fontRect.width);
                    batch.setProjectionMatrix(camera.combined.cpy().scale(xScale, 1.0F, 1.0F));
                } else {
                    batch.setProjectionMatrix(camera.combined);
                }

                float drawX = x;
                if (align == 2) {
                    drawX += width - fontRect.width;
                } else if (align == 1) {
                    drawX += (width - fontRect.width) / 2;
                }

                int drawY = y + (int) ((float) height - fontRect.height) / 2;
                drawY--;
                this.frameBuffer.begin();
                batch.setBlendFunction(-1, -1);
                batch.enableBlending();
                batch.setColor(1.0F, 1.0F, 1.0F, 0.0F);
                Gdx.gl20.glBlendFuncSeparate(773, 772, 0, 1);
                Gdx.gl20.glBlendEquationSeparate(32774, 32774);
                batch.begin();
                float fontDrawX = drawX;
                BitmapFontData fontData = f.getData();
                BitmapFontData outlineData = outline.getData();
                if (font.isOutline()) {
                    for (char c : string.toCharArray()) {
                        if (c == ' ') {
                            fontDrawX += f.getData().spaceXadvance;
                        } else {
                            ((Buffer) charBuffer).rewind();
                            ((Buffer) charBuffer.put(c)).flip();
                            float fontWidth = getGlyph(font.getOutlineFont().getData(), c).xadvance;
                            outline.draw(batch, charBuffer, fontDrawX - (float) (1.0f * loadedScale), (float) drawY);
                            fontDrawX += fontWidth;
                        }
                    }
                } else {
                    for (char cx : string.toCharArray()) {
                        if (cx == ' ') {
                            fontDrawX += f.getData().spaceXadvance;
                        } else {
                            ((Buffer) charBuffer).rewind();
                            ((Buffer) charBuffer.put(cx)).flip();
                            float fontWidth = getGlyph(fontData, cx).xadvance;
                            f.draw(batch, charBuffer, fontDrawX, (float) drawY);
                            fontDrawX += fontWidth;
                        }
                    }
                }

                batch.flush();
                batch.setBlendFunction(-1, -1);
                batch.setColor(com.badlogic.gdx.graphics.Color.WHITE);
                Gdx.gl20.glBlendFuncSeparate(770, 771, 1, 1);
                Gdx.gl20.glBlendEquationSeparate(32774, 32774);
                fontDrawX = drawX;
                if (font.isOutline()) {
                    for (char cxx : string.toCharArray()) {
                        if (cxx == ' ') {
                            fontDrawX += f.getData().spaceXadvance;
                        } else {
                            ((Buffer) charBuffer).rewind();
                            ((Buffer) charBuffer.put(cxx)).flip();
                            float fontWidth = getGlyph(fontData, cxx).xadvance;
                            outline.draw(batch, charBuffer, fontDrawX - (float) (1.0f * loadedScale), (float) drawY);
                            fontDrawX += fontWidth;
                        }
                    }
                }

                fontDrawX = drawX;

                for (char cxxx : string.toCharArray()) {
                    if (cxxx == ' ') {
                        fontDrawX += f.getData().spaceXadvance;
                    } else {
                        ((Buffer) charBuffer).rewind();
                        ((Buffer) charBuffer.put(cxxx)).flip();
                        float fontWidth = getGlyph(fontData, cxxx).xadvance;
                        f.draw(batch, charBuffer, fontDrawX, (float) drawY);
                        fontDrawX += fontWidth;
                    }
                }

                batch.enableBlending();
                batch.end();
                batch.setBlendFunction(770, 771);
                this.frameBuffer.end();
                batch.setProjectionMatrix(camera.combined);
                if (this.path == null) {
                    this.path = string;
                } else {
                    this.path = this.path + string;
                }
            }
        });
        if (this.pixmap.get() != null) {
            disposeImg(this.pixmap.get());
        }

        this.pixmap.set(null);
    }

    public void draw_text(int x, int y, int width, int height, Object string) {
        this.draw_text(x, y, width, height, string, 0);
    }

    public void draw_text(Rect rect, Object str, int align) {
        this.draw_text(rect.x, rect.y, rect.width, rect.height, str, align);
    }

    public void draw_text(Rect rect, Object str) {
        this.draw_text(rect.x, rect.y, rect.width, rect.height, str, 0);
    }

    public String getPath() {
        return this.path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Font getFont() {
        return this.font;
    }

    public void setFont(Font font) {
        if (font.getBitmapFont() == null) {
            Gdx.app.log("Bitmap", "Font is null!");
        }
    }

    public int getWidth() {
        return this.width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return this.height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public FrameBuffer getFrameBuffer() {
        return this.frameBuffer;
    }

    public void setFrameBuffer(FrameBuffer frameBuffer) {
        this.frameBuffer = frameBuffer;
    }

    public TextureRegion getRegion() {
        return this.region;
    }

    public void setRegion(TextureRegion region) {
        this.region = region;
    }

    public OrthographicCamera getCamera() {
        return this.camera;
    }

    public void setCamera(OrthographicCamera camera) {
        this.camera = camera;
    }

    public AtomicBoolean getIsDisposed() {
        return this.isDisposed;
    }

    public void setIsDisposed(AtomicBoolean isDisposed) {
        this.isDisposed = isDisposed;
    }

    public long getId() {
        return this.id;
    }

    public double getLoadedScale() {
        return this.loadedScale;
    }

    public void setLoadedScale(double loadedScale) {
        this.loadedScale = loadedScale;
    }

    public List<Runnable> getReloadRunnables() {
        return this.reloadRunnables;
    }

    public boolean isHiRes() {
        return this.hiRes;
    }

    public void setHiRes(boolean hiRes) {
        this.hiRes = hiRes;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof Bitmap)) {
            return false;
        } else {
            Bitmap other = (Bitmap) o;
            if (!other.canEqual(this)) {
                return false;
            } else {
                Object this$path = this.getPath();
                Object other$path = other.getPath();
                if (Objects.equals(this$path, other$path)) {
                    Object this$font = this.getFont();
                    Object other$font = other.getFont();
                    if (Objects.equals(this$font, other$font)) {
                        if (this.getWidth() != other.getWidth()) {
                            return false;
                        } else if (this.getHeight() != other.getHeight()) {
                            return false;
                        } else {
                            Object this$frameBuffer = this.getFrameBuffer();
                            Object other$frameBuffer = other.getFrameBuffer();
                            if (Objects.equals(this$frameBuffer, other$frameBuffer)) {
                                Object this$region = this.getRegion();
                                Object other$region = other.getRegion();
                                if (Objects.equals(this$region, other$region)) {
                                    Object this$pixmap = this.getPixmap();
                                    Object other$pixmap = other.getPixmap();
                                    if (Objects.equals(this$pixmap, other$pixmap)) {
                                        Object this$camera = this.getCamera();
                                        Object other$camera = other.getCamera();
                                        if (Objects.equals(this$camera, other$camera)) {
                                            Object this$isDisposed = this.getIsDisposed();
                                            Object other$isDisposed = other.getIsDisposed();
                                            if (Objects.equals(this$isDisposed, other$isDisposed)) {
                                                if (this.getId() != other.getId()) {
                                                    return false;
                                                } else if (Double.compare(this.getLoadedScale(), other.getLoadedScale()) != 0) {
                                                    return false;
                                                } else {
                                                    Object this$reloadRunnables = this.getReloadRunnables();
                                                    Object other$reloadRunnables = other.getReloadRunnables();
                                                    return Objects.equals(this$reloadRunnables, other$reloadRunnables) && this.isHiRes() == other.isHiRes();
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
                            } else {
                                return false;
                            }
                        }
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        }
    }

    protected boolean canEqual(Object other) {
        return other instanceof Bitmap;
    }

    @Override
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Object $path = this.getPath();
        result = result * 59 + ($path == null ? 43 : $path.hashCode());
        Object $font = this.getFont();
        result = result * 59 + ($font == null ? 43 : $font.hashCode());
        result = result * 59 + this.getWidth();
        result = result * 59 + this.getHeight();
        Object $frameBuffer = this.getFrameBuffer();
        result = result * 59 + ($frameBuffer == null ? 43 : $frameBuffer.hashCode());
        Object $region = this.getRegion();
        result = result * 59 + ($region == null ? 43 : $region.hashCode());
        Object $pixmap = this.getPixmap();
        result = result * 59 + ($pixmap == null ? 43 : $pixmap.hashCode());
        Object $camera = this.getCamera();
        result = result * 59 + ($camera == null ? 43 : $camera.hashCode());
        Object $isDisposed = this.getIsDisposed();
        result = result * 59 + ($isDisposed == null ? 43 : $isDisposed.hashCode());
        long $id = this.getId();
        result = result * 59 + (int) ($id >>> 32 ^ $id);
        long $loadedScale = Double.doubleToLongBits(this.getLoadedScale());
        result = result * 59 + (int) ($loadedScale >>> 32 ^ $loadedScale);
        Object $reloadRunnables = this.getReloadRunnables();
        result = result * 59 + ($reloadRunnables == null ? 43 : $reloadRunnables.hashCode());
        return result * 59 + (this.isHiRes() ? 79 : 97);
    }

    @Override
    public String toString() {
        return "Bitmap(path="
            + this.getPath()
            + ", width="
            + this.getWidth()
            + ", height="
            + this.getHeight()
            + ", isDisposed="
            + this.getIsDisposed()
            + ", id="
            + this.getId()
            + ")";
    }

    private static class BitmapReference extends WeakReference<Bitmap> {
        private final AtomicBoolean isDisposed;
        private final AtomicReference<Pixmap> pixmapReference;
        private final FrameBuffer frameBuffer;
        private final long id;

        public BitmapReference(Bitmap referent, ReferenceQueue<Bitmap> q) {
            super(referent, q);
            this.isDisposed = referent.isDisposed;
            this.pixmapReference = referent.pixmap;
            this.frameBuffer = referent.frameBuffer;
            this.id = referent.id;
        }

        public void dispose() {
            if (Bitmap.table.containsKey(this.id) && !this.isDisposed.get()) {
                if (this.frameBuffer != null) {
                    this.frameBuffer.dispose();
                }

                if (this.pixmapReference.get() != null) {
                    Bitmap.disposeImg(this.pixmapReference.get());
                }

                Bitmap.table.remove(this.id);
                Thread.yield();
            }
        }
    }
}
