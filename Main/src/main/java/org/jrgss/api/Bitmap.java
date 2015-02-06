package org.jrgss.api;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import lombok.Data;
import lombok.ToString;
import org.jrgss.FileUtil;
import org.jrgss.JRGSSGame;
import org.jrgss.shaders.TextShaderProgram;

import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Queue;

import static org.jrgss.JRGSSGame.runWithGLContext;

/**
 * Created by matty on 6/27/14.
 */
@Data
@ToString(exclude = {"camera", "font", "frameBuffer", "region", "pixmap"})
public class Bitmap {
    static HashMap<String, Pixmap> cache = new HashMap<>();
    String path;
    Font font = new Font();
    int width;
    int height;
    FrameBuffer frameBuffer;
    TextureRegion region;
    Pixmap pixmap;
    OrthographicCamera camera;
    boolean isDisposed;


    public Bitmap(Pixmap img) {
        frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, img.getWidth(), img.getHeight(), false);
        OrthographicCamera camera = new OrthographicCamera();
        camera.setToOrtho(false, img.getWidth(), img.getHeight());
        img.getPixels().rewind();
        SpriteBatch batch = new SpriteBatch();
        batch.setProjectionMatrix(camera.combined);
        frameBuffer.begin();
        batch.begin();
        Gdx.gl.glClearColor(1, 1, 1, 0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.draw(new Texture(img), 0, 0);
        batch.end();
        frameBuffer.end();
        region = new TextureRegion(new Texture(img));
        batch.dispose();

        this.width = img.getWidth();
        this.height = img.getHeight();
        this.camera = new OrthographicCamera(width, height);
        this.camera.setToOrtho(true, width, height);
    }

    public Bitmap(String path) {
        long t = System.currentTimeMillis();
        final Pixmap img;
        if (cache.containsKey(path)) {
            img = cache.get(path);
        } else {
            FileHandle file = FileUtil.loadImg(path);
            img = new Pixmap(file);
            cache.put(path, img);
        }
        long endTime = System.currentTimeMillis();
        this.path = path;
        runWithGLContext(new Runnable() {
            @Override
            public void run() {
                frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, img.getWidth(), img.getHeight(), false);
                OrthographicCamera camera = new OrthographicCamera();
                camera.setToOrtho(false, img.getWidth(), img.getHeight());
                SpriteBatch batch = new SpriteBatch();
                batch.setProjectionMatrix(camera.combined);
                frameBuffer.begin();
                batch.begin();
                Gdx.gl.glClearColor(1, 1, 1, 0f);
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
                batch.draw(new Texture(img), 0, 0);
                batch.end();
                frameBuffer.end();
                region = new TextureRegion(frameBuffer.getColorBufferTexture());
                batch.dispose();
            }
        });
        this.width = img.getWidth();
        this.height = img.getHeight();
        endTime = System.currentTimeMillis();
        this.camera = new OrthographicCamera(width, height);
        this.camera.setToOrtho(true, width, height);
    }

    public Bitmap(final int width, final int height) {
        this.width = width;
        this.height = height;
        this.camera = new OrthographicCamera(width, height);
        this.camera.setToOrtho(true, width, height);
        runWithGLContext(new Runnable() {
            @Override
            public void run() {
                if (width == 0 || height == 0) {
                    frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, 1, 1, false);
                } else {
                    frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, false);
                }
                frameBuffer.begin();
                Gdx.gl.glClearColor(1, 1, 1, 0f);
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
                frameBuffer.end();
                region = new TextureRegion(frameBuffer.getColorBufferTexture());
            }
        });
        clear();
    }

    public static int clamp(int x, int a, int b) {
        return (x < a) ? a : (x > b) ? b : x;
    }

    public static void blur(IntBuffer in, IntBuffer out, int width, int height, int radius) {
        int widthMinus1 = width - 1;
        int tableSize = 2 * radius + 1;
        int divide[] = new int[256 * tableSize];

        for (int i = 0; i < 256 * tableSize; i++)
            divide[i] = i / tableSize;

        int inIndex = 0;

        for (int y = 0; y < height; y++) {
            int outIndex = y;
            int ta = 0, tr = 0, tg = 0, tb = 0;

            for (int i = -radius; i <= radius; i++) {
                int rgb = in.get(inIndex + clamp(i, 0, width - 1));
                ta += (rgb >> 24) & 0xff;
                tr += (rgb >> 16) & 0xff;
                tg += (rgb >> 8) & 0xff;
                tb += rgb & 0xff;
            }

            for (int x = 0; x < width; x++) {
                out.put(outIndex, (divide[ta] << 24) | (divide[tr] << 16) | (divide[tg] << 8) | divide[tb]);

                int i1 = x + radius + 1;
                if (i1 > widthMinus1)
                    i1 = widthMinus1;
                int i2 = x - radius;
                if (i2 < 0)
                    i2 = 0;
                int rgb1 = in.get(inIndex + i1);
                int rgb2 = in.get(inIndex + i2);

                ta += ((rgb1 >> 24) & 0xff) - ((rgb2 >> 24) & 0xff);
                tr += ((rgb1 & 0xff0000) - (rgb2 & 0xff0000)) >> 16;
                tg += ((rgb1 & 0xff00) - (rgb2 & 0xff00)) >> 8;
                tb += (rgb1 & 0xff) - (rgb2 & 0xff);
                outIndex += height;
            }
            inIndex += width;
        }
    }

    public Bitmap clone() {
        return new Bitmap(getPixmap());
    }

    public void hue_change(float hue) {
        hue = (hue / 360f) - ((int) (hue / 360f));
        if (hue < 0) hue = 1 - hue;
        final float finalHue = hue;
        Gdx.app.log("Bitmap", "Hue change to " + hue);
        JRGSSGame.runWithGLContext(new Runnable() {
            @Override
            public void run() {
                Pixmap p = getPixmap();
                IntBuffer inPixels = p.getPixels().asIntBuffer();
                IntBuffer outPixels = IntBuffer.allocate(inPixels.remaining());
                while (inPixels.remaining() > 0) {
                    Color c = new Color(inPixels.get());
                    float[] hsv = rgbToHSV(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f);
                    hsv[0] = finalHue;
                    outPixels.put(hsvToRGB(hsv, c.getAlpha()));
                }
                inPixels.rewind();
                inPixels.put(outPixels);
                inPixels.rewind();
                OrthographicCamera camera = new OrthographicCamera();
                camera.setToOrtho(false, getWidth(), getHeight());
                SpriteBatch batch = new SpriteBatch();
                batch.setProjectionMatrix(camera.combined);
                frameBuffer.begin();
                batch.begin();
                Gdx.gl.glClearColor(1.0f, 1.0f, 1.0f, 0.0f);
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
                batch.draw(new Texture(p), 0, 0);
                batch.end();
                frameBuffer.end();
                region = new TextureRegion(frameBuffer.getColorBufferTexture());
                batch.dispose();
                pixmap = null;
            }
        });
    }

    private int hsvToRGB(float[] hsv, int alpha) {
        float r, g, b;
        if (hsv[1] == 0) {
            return new Color((int) (hsv[2] * 255f), (int) (hsv[2] * 255f), (int) (hsv[2] * 255f), alpha).toPackedInt();
        }
        float h = hsv[0] * 6;
        if (h >= 6) h = 0;
        int i = (int) h;
        float v1 = hsv[2] * (1 - hsv[1]);
        float v2 = hsv[2] * (1 - (hsv[1] * (h - i)));
        float v3 = hsv[2] * (1 - (hsv[1] * (1 - h + i)));

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
                break;
        }
        return new Color((int) (r * 255), (int) (g * 255), (int) (b * 255), alpha).toPackedInt();


    }

    private float[] rgbToHSV(float r, float g, float b) {
        float min = Math.min(Math.min(r, g), b);
        float max = Math.max(Math.max(r, g), b);
        float delta = max - min;
        float[] hsv = new float[3];
        hsv[2] = max;
        if (delta == 0) {
            return hsv;
        }

        hsv[1] = delta / max;

        float deltaR = (((max - r) / 6) + (max / 2)) / max;
        float deltaG = (((max - g) / 6) + (max / 2)) / max;
        float deltaB = (((max - b) / 6) + (max / 2)) / max;

        if (r == max) hsv[0] = deltaB - deltaG;
        if (g == max) hsv[0] = (1f / 3f) + r - b;
        if (b == max) hsv[0] = (2f / 3f) + g - r;

        if (hsv[0] < 0) hsv[0] += 1;
        if (hsv[0] > 1) hsv[0] -= 1;

        return hsv;
    }

    public Rect text_size(final String str) {
        BitmapFont.TextBounds bounds = font.bitmapFont.getBounds(str);
        return new Rect(0, 0, (int) bounds.width, (int) bounds.height);
    }

    public Rect rect() {
        return new Rect(0, 0, width, height);
    }

    public Pixmap getPixmap() {
        if (pixmap == null) {
            Gdx.app.log("Bitmap", "Generating Pixmap. Loaded from " + path);
            frameBuffer.begin();
            Pixmap ret = new Pixmap(getWidth(), getHeight(), Pixmap.Format.RGBA8888);
            byte[] bytes = ScreenUtils.getFrameBufferPixels(0, 0, getWidth(), getHeight(), true);
            ret.getPixels().put(bytes).rewind();
            Gdx.app.log("Bitmap", "Returned Pixmap is " + ret.getFormat());
            frameBuffer.end();
            pixmap = ret;
            return ret;
        }
        return pixmap;
    }

    public void blt(final int x, final int y, final Bitmap src_bitmap, final Rect src_rect) {
        blt(x, y, src_bitmap, src_rect, 255);
    }

    public void blt(final int x, final int y, final Bitmap src_bitmap, final Rect src_rect, final int opacity) {
        internalBlit(x, y, src_rect.getWidth(), src_rect.getHeight(), src_bitmap, src_rect, opacity);
    }

    private void internalBlit(final int x, final int y, final int width, final int height,
                              final Bitmap src_bitmap, final Rect src_rect, final int opacity) {

        JRGSSGame.runWithGLContext(new Runnable() {
            @Override
            public void run() {
                SpriteBatch batch = new SpriteBatch();
                batch.enableBlending();
                batch.setProjectionMatrix(camera.combined);
                frameBuffer.begin();
                batch.setColor(1.0f, 1.0f, 1.0f, (opacity / 255f));
                batch.begin();
                src_bitmap.render(batch, x, y, width, height, src_rect);
                batch.end();
                frameBuffer.end();
                batch.dispose();
                pixmap = null;
            }
        });

    }

    public void stretch_blt(final Rect dest_rect, final Bitmap src_bitmap, final Rect src_rect, final int opacity) {
        internalBlit(dest_rect.getX(), dest_rect.getY(), dest_rect.getWidth(), dest_rect.getHeight(), src_bitmap, src_rect, opacity);
    }

    public void stretch_blt(final Rect dest_rect, final Bitmap src_bitmap, final Rect src_rect) {
        internalBlit(dest_rect.getX(), dest_rect.getY(), dest_rect.getWidth(), dest_rect.getHeight(), src_bitmap, src_rect, 255);
    }

    public void dispose() {
        isDisposed = true;
        if (frameBuffer != null) {
            runWithGLContext(new Runnable() {
                FrameBuffer fb = frameBuffer;

                @Override
                public void run() {
                    fb.dispose();
                }
            });
            frameBuffer = null;
        }
    }

    public void blur() {
        JRGSSGame.runWithGLContext(new Runnable() {
            @Override
            public void run() {
                Pixmap p = getPixmap();
                IntBuffer inPixels = p.getPixels().asIntBuffer();
                IntBuffer outPixels = IntBuffer.allocate(inPixels.remaining());
                blur(inPixels, outPixels, getWidth(), getHeight(), 2);
                blur(outPixels, inPixels, getHeight(), getWidth(), 2);
                OrthographicCamera camera = new OrthographicCamera();
                camera.setToOrtho(false, getWidth(), getHeight());
                SpriteBatch batch = new SpriteBatch();
                batch.setProjectionMatrix(camera.combined);
                frameBuffer.begin();
                batch.begin();
                Gdx.gl.glClearColor(1.0f, 1.0f, 1.0f, 0.0f);
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
                batch.draw(new Texture(p), 0, 0);
                batch.end();
                frameBuffer.end();
                region = new TextureRegion(frameBuffer.getColorBufferTexture());
                batch.dispose();
                pixmap = null;
            }
        });

    }

    public void gradient_fill_rect(Rect rect, Color color1, Color color2) {
        gradient_fill_rect(rect, color1, color2, false);
    }

    public void gradient_fill_rect(Rect rect, Color color1, Color color2, boolean vertical) {
        gradient_fill_rect(rect.x, rect.y, rect.width, rect.height, color1, color2, vertical);
    }

    public void gradient_fill_rect(int x, int y, int width, int height, Color color1, Color color2) {
        gradient_fill_rect(x, y, width, height, color1, color2, false);
    }



    public void gradient_fill_rect(final int x, final int y, final int width, final int height, final Color color1, final Color color2, final boolean vertical) {
        runWithGLContext(new Runnable() {
            @Override
            public void run() {
                ShapeRenderer shapeRenderer = new ShapeRenderer();
                shapeRenderer.setColor(1.0f, 1.0f, 1.0f, 1.0f);
                shapeRenderer.setProjectionMatrix(camera.combined);
                frameBuffer.begin();
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                if(vertical) {
                    shapeRenderer.rect(x, y, width, height, color1.toGDX(), color1.toGDX(), color2.toGDX(), color2.toGDX());
                } else {
                    shapeRenderer.rect(x, y, width, height, color1.toGDX(), color2.toGDX(), color2.toGDX(), color1.toGDX());
                }
                shapeRenderer.end();
                frameBuffer.end();
                pixmap = null;
            }
        });

    }

    public void fill_rect(Rect rect, Color color) {
        fill_rect(rect.x, rect.y, rect.width, rect.height, color);
    }

    public void fill_rect(final int x, final int y, final int width, final int height, final Color color) {
        runWithGLContext(new Runnable() {
            @Override
            public void run() {
                SpriteBatch batch = new SpriteBatch();
                batch.disableBlending();
                batch.setProjectionMatrix(camera.combined);
                frameBuffer.begin();
                batch.begin();
                batch.setColor(color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f);
                //(height - srcRect.getHeight()) - srcRect.getY()
                batch.draw(Sprite.getColorTexture(), x, y, width, height);
                batch.end();
                frameBuffer.end();
                pixmap = null;
            }
        });
    }

    public void render(SpriteBatch batch, int x, int y) {
        batch.draw(region, x, y);
    }

    public void render(SpriteBatch batch, int x, int y, int srcx, int srcy, int srcwidth, int srcheight) {
        if (isDisposed) {
            Gdx.app.log("Bitmap", "Trying to draw a disposed Bitmap. BUG");
            throw new RuntimeException("Trying to draw a disposed Bitmap");
        }
        batch.draw(frameBuffer.getColorBufferTexture(), x, y, srcx, (height - srcheight) - srcy,
                srcwidth, srcheight);
    }

    public void clear_rect(final int x, final int y, final int width, final int height) {
        runWithGLContext(new Runnable() {
            @Override
            public void run() {
                SpriteBatch batch = new SpriteBatch();
                batch.disableBlending();
                batch.setProjectionMatrix(camera.combined);
                frameBuffer.begin();
                batch.begin();
                batch.setColor(0f, 0f, 0f, 0f);
                batch.draw(Sprite.getColorTexture(), x, y, width, height);
                batch.end();
                frameBuffer.end();
                pixmap = null;
            }
        });
    }

    public void clear_rect(Rect rect) {
        clear_rect(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
    }

    public void render(SpriteBatch batch, int x, int y, Rect srcRect) {
        render(batch, x, y, srcRect.getWidth(), srcRect.getHeight(), srcRect);
    }

    public void render(SpriteBatch batch, int x, int y, int width, int height, Rect srcRect) {
        if (frameBuffer != null) {
            batch.draw(frameBuffer.getColorBufferTexture(), x, y, width, height, srcRect.getX(), (this.height - srcRect.getHeight()) - srcRect.getY(),
                    srcRect.getWidth(), srcRect.getHeight(), false, false);
        }

    }

    public boolean isDisposed() {
        return frameBuffer == null;
    }

    public void clear() {
        runWithGLContext(new Runnable() {
            @Override
            public void run() {
                frameBuffer.begin();
                Gdx.gl.glClearColor(0, 0, 0, 0f);
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
                frameBuffer.end();
            }
        });
        pixmap = null;
    }

    public Color get_pixel(int x, int y) {
        Pixmap p = getPixmap();
        return new Color(p.getPixel(x, y));
    }

    public void set_pixel(final int x, final int y, Color c) {
        final Pixmap temp = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        runWithGLContext(new Runnable() {
            @Override
            public void run() {
                final Texture texture = new Texture(temp);
                SpriteBatch batch = new SpriteBatch();
                //batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA_ALPHA);
                //batch.enableBlending();
                frameBuffer.begin();
                batch.begin();
                batch.draw(texture, x, y);
                batch.end();
                frameBuffer.end();
            }
        });
        pixmap = null;
    }

    public void draw_text(final int x, final int y, final int width, final int height, final Object obj,
                          final int align) {
        final String string = obj.toString();
        JRGSSGame.runWithGLContext(new Runnable() {
            @Override
            public void run() {
                BitmapFont f = font.getBitmapFont();
                BitmapFont.TextBounds bounds = f.getBounds(string);
                int drawX = x;
                if (align == 2) {
                    drawX += width - bounds.width;
                } else if (align == 1) {
                    drawX += (width - bounds.width) / 2;
                }
                int drawY = y + (int) (height - bounds.height) / 2;
                //Gdx.app.log("Bitmap", "Drawing text " + string + ". Colors " + font.getColor() + " " + f.getScaleX());
                SpriteBatch batch = new SpriteBatch();
                batch.setProjectionMatrix(camera.combined);
                Color outColor = font.getOut_color();
                Color innerColor = font.getColor();
                //f.setColor(outColor.red / 255f, outColor.green / 255f, outColor.blue / 255f, outColor.alpha / 255f);
                frameBuffer.begin();
                batch.begin();
                //batch.disableBlending();
                f.setColor(outColor.red / 255f, outColor.green / 255f, outColor.blue / 255f, outColor.alpha / 255f);
                f.draw(batch, string, drawX - 1.3f, drawY);
                f.draw(batch, string, drawX + 1.3f, drawY);
                f.draw(batch, string, drawX, drawY + 1.3f);
                f.draw(batch, string, drawX, drawY - 1.3f);
                f.draw(batch, string, drawX + 1.3f, drawY + 1.3f);
                f.draw(batch, string, drawX - 1.3f, drawY - 1.3f);
                f.draw(batch, string, drawX + 1.3f, drawY - 1.3f);
                f.draw(batch, string, drawX - 1.3f, drawY + 1.3f);
                batch.flush();
                batch.setShader(TextShaderProgram.get());
                //batch.setBlendFunction(GL20.GL_ONE, GL20.GL_ZERO);
                f.setColor(innerColor.red / 255f, innerColor.green / 255f, innerColor.blue / 255f, innerColor.alpha / 255f);
                f.draw(batch, string, drawX, drawY);
                batch.end();
                batch.setShader(null);
                batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
                frameBuffer.end();
                if (path == null) {
                    path = string;
                } else {
                    path += string;
                }
            }
        });
        pixmap = null;

    }

    public void draw_text(int x, int y, int width, int height, Object string) {
        draw_text(x, y, width, height, string, 0);
    }

    public void draw_text(Rect rect, Object str, int align) {
        draw_text(rect.x, rect.y, rect.width, rect.height, str, align);
    }

    public void draw_text(Rect rect, Object str) {
        draw_text(rect.x, rect.y, rect.width, rect.height, str, 0);
    }

}
