package org.jrgss.api;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.JrgssBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Rectangle;
import org.jrgss.JRGSSGame;
import org.jrgss.Scissors;
import org.jrgss.shaders.AlphaBlendingShader;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public class Window extends AbstractRenderable {
    static final AtomicLong counter = new AtomicLong();
    private static JrgssBatch batch;
    int x;
    int y;
    int width;
    int height;
    int z;
    int ox;
    int oy;
    int padding = 12;
    int padding_bottom = 12;
    int opacity = 255;
    int back_opacity = 192;
    int contents_opacity = 255;
    Bitmap windowskin;
    Bitmap contents;
    Rect cursor_rect;
    Viewport viewport = null;
    boolean active = true;
    boolean visible = true;
    boolean arrows_visible = true;
    boolean pause;
    int openness = 255;
    Tone tone = new Tone(0.0F, 0.0F, 0.0F, 0.0F);
    boolean disposed = false;
    Texture baseWindowSkin;
    FrameBuffer background;
    TextureRegion upperleft;
    TextureRegion lowerleft;
    TextureRegion upperright;
    TextureRegion lowerright;
    TextureRegion horizontalTop;
    TextureRegion horizontalBottom;
    TextureRegion verticalLeft;
    TextureRegion verticalRight;
    TextureRegion upperleftCursor;
    TextureRegion lowerleftCursor;
    TextureRegion upperrightCursor;
    TextureRegion lowerrightCursor;
    TextureRegion horizontalTopCursor;
    TextureRegion horizontalBottomCursor;
    TextureRegion verticalLeftCursor;
    TextureRegion verticalRightCursor;
    TextureRegion backgroundCursor;
    TextureRegion downScroll;
    TextureRegion upScroll;
    TextureRegion leftScroll;
    TextureRegion rightScroll;
    TextureRegion[] pauses = new TextureRegion[4];
    int pauseCounter = 0;
    int lastToneHash = 0;
    float cursor_opacity = 0.5F;
    float cursor_inc = 0.033333335F;
    long id = counter.getAndIncrement();

    public Window(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.z = 100;
        this.width = width;
        this.height = height;
        this.windowskin = null;
        this.contents = new Bitmap(0, 0);
        this.cursor_rect = new Rect(0, 0, 0, 0);
        Gdx.app.log("Window", "Window @ " + x + "," + y);
    }

    public Window() {
    }

    private static JrgssBatch getBatch() {
        if (batch == null) {
            batch = new JrgssBatch();
            batch.setProjectionMatrix(JRGSSGame.camera.combined);
        }

        return batch;
    }

    private void updateBackground() {
        JRGSSGame.runWithGLContext(() -> {
            JrgssBatch batch = getBatch();
            Texture texture = this.baseWindowSkin;
            TextureRegion bg = new TextureRegion(texture, 0, 64, 64, 64);
            TextureRegion tile = new TextureRegion(texture, 0, 0, 64, 64);
            if (this.background != null) {
                this.background.dispose();
            }

            this.background = new FrameBuffer(Format.RGB888, this.width > 8 ? this.width - 8 : 1, this.height > 8 ? this.height - 8 : 1, false);
            batch.setBlendFunction(770, 771);
            batch.enableBlending();
            batch.setColor(com.badlogic.gdx.graphics.Color.WHITE);
            OrthographicCamera camera = new OrthographicCamera();
            camera.setToOrtho(false, this.width - 8, this.height - 8);
            batch.setProjectionMatrix(camera.combined);
            this.background.begin();
            Gdx.gl.glClear(16384);
            batch.begin();
            batch.draw(bg, 0.0F, 0.0F, (float) (this.width - 8), (float) (this.height - 8));

            for (int i = 0; i <= this.width - 8; i += 64) {
                for (int j = 0; j <= this.height - 8; j += 64) {
                    batch.draw(tile, (float) i, (float) j, 64.0F, 64.0F);
                }
            }

            batch.end();
            this.background.end();
        });
    }

    public boolean isOpen() {
        return this.openness >= 255;
    }

    public void update() {
        this.cursor_opacity = this.cursor_opacity + this.cursor_inc;
        if (Math.abs(this.cursor_opacity - 1.0F) < 1.0E-4F || this.cursor_opacity < 1.0E-4F) {
            this.cursor_inc = -this.cursor_inc;
        }

        this.pauseCounter++;
        if (this.pauseCounter == 60) {
            this.pauseCounter = 0;
        }
    }

    public boolean isClose() {
        return this.openness == 0;
    }

    @Override
    public void render(JrgssBatch _) {
        if (this.visible && !this.isClose() && (this.viewport == null || this.viewport.isVisible())) {
            JrgssBatch batch = getBatch();
            batch.setProjectionMatrix(JRGSSGame.camera.combined);
            batch.setBlendFunction(770, 771);
            batch.enableBlending();
            int temp_x;
            int temp_y;
            if (this.viewport != null) {
                temp_x = this.x + this.viewport.getRect().getX() - this.viewport.getOx();
                temp_y = this.y + this.viewport.getRect().getY() - this.viewport.getOy();
            } else {
                temp_y = this.getY();
                temp_x = this.getX();
            }
            final int x = temp_x;
            final int y = temp_y;
            if (this.width - 8 != this.background.getWidth() || this.height - 8 != this.background.getHeight()) {
                this.updateBackground();
            }

            float globalOpacity = this.opacity / 255.0F;
            Viewport.begin(this.viewport, batch);
            Rectangle scissors = new Rectangle();
            Rectangle clipBounds = new Rectangle(x + 4, y + 4, this.width - 8, this.height - 8);
            Scissors.calculateScissors(JRGSSGame.camera, batch.getTransformMatrix(), clipBounds, scissors);
            Scissors.pushScissors(scissors);
            batch.setColor(1.0F, 1.0F, 1.0F, this.back_opacity / 255.0F * globalOpacity);
            AlphaBlendingShader.withToneChange(tone, batch, () -> {
                batch.begin();
                batch.draw(this.background.getColorBufferTexture(), (float) (x + 4), (float) (y + 4));
                batch.flush();
            });
            batch.setShader(null);
            batch.setBlendFunction(770, 771);
            batch.setColor(1.0F, 1.0F, 1.0F, globalOpacity);
            Scissors.popScissors();
            batch.draw(this.upperleft, (float) x, (float) y);
            batch.draw(this.lowerleft, (float) x, (float) (y + this.height - 16));
            batch.draw(this.upperright, (float) (x + this.width - 16), (float) y);
            batch.draw(this.lowerright, (float) (x + this.width - 16), (float) (y + this.height - 16));
            batch.draw(this.horizontalTop, (float) (x + 16), (float) y, (float) (this.width - 32), 16.0F);
            batch.draw(this.horizontalBottom, (float) (x + 16), (float) (y + this.height - 16), (float) (this.width - 32), 16.0F);
            batch.draw(this.verticalLeft, (float) x, (float) (y + 16), 16.0F, (float) (this.height - 32));
            batch.draw(this.verticalRight, (float) (x + this.width - 16), (float) (y + 16), 16.0F, (float) (this.height - 32));
            batch.flush();
            batch.setColor(1.0F, 1.0F, 1.0F, this.contents_opacity / 255.0F);
            if (this.isOpen()) {
                this.contents.render(batch, x + this.padding, y + this.padding, this.ox, this.oy, this.width - this.padding * 2, this.height - this.padding * 2);
            }

            batch.flush();
            if (this.active) {
                batch.setColor(1.0F, 1.0F, 1.0F, this.cursor_opacity * (this.contents_opacity / 255.0F));
            } else {
                batch.setColor(1.0F, 1.0F, 1.0F, 0.6F * (this.contents_opacity / 255.0F));
            }

            this.renderCursor(batch);
            batch.setColor(com.badlogic.gdx.graphics.Color.WHITE);
            if (this.pause) {
                this.drawPause(batch, x, y);
            }

            if (this.arrows_visible) {
                if (this.contents.getHeight() - this.oy > this.height - this.padding * 2) {
                    this.drawDownArrow(batch, x, y);
                }

                if (this.oy > 0) {
                    this.drawUpArrow(batch, x, y);
                }

                if (this.contents.getWidth() - this.ox > this.width - this.padding * 2) {
                    this.drawRightArrow(batch, x, y);
                }

                if (this.ox > 0) {
                    this.drawLeftArrow(batch, x, y);
                }
            }

            batch.end();
        }
    }

    private void drawPause(Batch batch, int x, int y) {
        int pauseX = x + (this.width - 16) / 2;
        int pauseY = y + this.height - 16 - 4;
        int frame = this.pauseCounter / 15;
        batch.draw(this.pauses[frame], (float) pauseX, (float) pauseY);
    }

    private void drawDownArrow(Batch batch, int x, int y) {
        int arrowX = x + (this.width - 16) / 2;
        int arrowY = y + this.height - 8 - 4;
        batch.draw(this.downScroll, (float) arrowX, (float) arrowY);
    }

    private void drawUpArrow(Batch batch, int x, int y) {
        int arrowX = x + (this.width - 16) / 2;
        int arrowY = y + 4;
        batch.draw(this.upScroll, (float) arrowX, (float) arrowY);
    }

    private void drawLeftArrow(Batch batch, int x, int y) {
        int arrowY = y + (this.height - 16) / 2;
        int arrowX = x + 4;
        batch.draw(this.leftScroll, (float) arrowX, (float) arrowY);
    }

    private void drawRightArrow(Batch batch, int x, int y) {
        int arrowY = y + (this.height - 16) / 2;
        int arrowX = x + this.width - 8 - 4;
        batch.draw(this.rightScroll, (float) arrowX, (float) arrowY);
    }

    @Override
    public void dispose() {
        super.dispose();
        JRGSSGame.runWithGLContext(new Runnable() {
            @Override
            public void run() {
                Gdx.app.log("Window", Window.this.id + " Disposed!");
                if (!Window.this.disposed) {
                    if (Window.this.contents != null) {
                        Window.this.contents.dispose();
                    }

                    if (Window.this.background != null) {
                        Window.this.background.dispose();
                    }
                }

                Window.this.disposed = true;
            }
        });
    }

    public void move(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    protected void renderCursor(Batch batch) {
        if (this.cursor_rect.height != 0 && this.cursor_rect.width != 0) {
            int x = this.x + this.padding + this.cursor_rect.getX() - this.ox;
            int y = this.y + this.padding + this.cursor_rect.getY() - this.oy;
            if (this.viewport != null) {
                x = x + this.viewport.getRect().getX() - this.viewport.getOx();
                y = y + this.viewport.getRect().getY() - this.viewport.getOy();
            }

            int width = this.cursor_rect.getWidth();
            int height = this.cursor_rect.getHeight();
            batch.draw(this.upperleftCursor, (float) x, (float) y);
            batch.draw(this.lowerleftCursor, (float) x, (float) (y + height - 4));
            batch.draw(this.upperrightCursor, (float) (x + width - 4), (float) y);
            batch.draw(this.lowerrightCursor, (float) (x + width - 4), (float) (y + height - 4));
            batch.draw(this.horizontalTopCursor, (float) (x + 4), (float) y, (float) (width - 8), 4.0F);
            batch.draw(this.horizontalBottomCursor, (float) (x + 4), (float) (y + height - 4), (float) (width - 8), 4.0F);
            batch.draw(this.verticalLeftCursor, (float) x, (float) (y + 4), 4.0F, (float) (height - 8));
            batch.draw(this.verticalRightCursor, (float) (x + width - 4), (float) (y + 4), 4.0F, (float) (height - 8));
            batch.draw(this.backgroundCursor, (float) (x + 1), (float) (y + 1), (float) (width - 2), (float) (height - 2));
        }
    }

    public int getX() {
        return this.x;
    }

    public void setX(int x) {
        this.x = x;
    }

    @Override
    public int getY() {
        return this.y;
    }

    public void setY(int y) {
        this.y = y;
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

    @Override
    public int getZ() {
        return this.z;
    }

    public void setZ(int z) {
        this.z = z;
    }

    public int getOx() {
        return this.ox;
    }

    public void setOx(int ox) {
        this.ox = ox;
    }

    public int getOy() {
        return this.oy;
    }

    public void setOy(int oy) {
        this.oy = oy;
    }

    public int getPadding() {
        return this.padding;
    }

    public void setPadding(int padding) {
        this.padding = padding;
    }

    public int getPadding_bottom() {
        return this.padding_bottom;
    }

    public void setPadding_bottom(int padding_bottom) {
        this.padding_bottom = padding_bottom;
    }

    public int getOpacity() {
        return this.opacity;
    }

    public void setOpacity(int opacity) {
        this.opacity = Math.max(0, Math.min(255, opacity));
    }

    public int getBack_opacity() {
        return this.back_opacity;
    }

    public void setBack_opacity(int opacity) {
        this.back_opacity = Math.max(0, Math.min(255, opacity));
    }

    public int getContents_opacity() {
        return this.contents_opacity;
    }

    public void setContents_opacity(int opacity) {
        this.contents_opacity = Math.max(0, Math.min(255, opacity));
    }

    public Bitmap getWindowskin() {
        return this.windowskin;
    }

    public void setWindowskin(final Bitmap bitmap) {
        Gdx.app.log("Window", "Setting up window borders...");
        this.windowskin = bitmap;
        JRGSSGame.runWithGLContext(new Runnable() {
            @Override
            public void run() {
                Texture texture = bitmap.region.getTexture();
                Window.this.baseWindowSkin = texture;
                int tHeight = texture.getHeight();
                Window.this.upperleft = new TextureRegion(texture, 64, tHeight - 16, 16, 16);
                Window.this.lowerleft = new TextureRegion(texture, 64, tHeight - 64, 16, 16);
                Window.this.upperright = new TextureRegion(texture, 112, tHeight - 16, 16, 16);
                Window.this.lowerright = new TextureRegion(texture, 112, tHeight - 64, 16, 16);
                Window.this.horizontalTop = new TextureRegion(texture, 80, tHeight - 16, 1, 16);
                Window.this.horizontalBottom = new TextureRegion(texture, 80, tHeight - 64, 1, 16);
                Window.this.verticalLeft = new TextureRegion(texture, 64, tHeight - 16 - 1, 16, 1);
                Window.this.verticalRight = new TextureRegion(texture, 112, tHeight - 16 - 1, 16, 1);
                Window.this.upperleftCursor = new TextureRegion(texture, 64, tHeight - 4 - 64, 4, 4);
                Window.this.lowerleftCursor = new TextureRegion(texture, 64, tHeight - 64 - 32, 4, 4);
                Window.this.upperrightCursor = new TextureRegion(texture, 92, tHeight - 4 - 64, 4, 4);
                Window.this.lowerrightCursor = new TextureRegion(texture, 92, tHeight - 64 - 32, 4, 4);
                Window.this.horizontalTopCursor = new TextureRegion(texture, 68, tHeight - 4 - 64, 1, 4);
                Window.this.horizontalBottomCursor = new TextureRegion(texture, 68, tHeight - 64 - 32, 1, 4);
                Window.this.verticalLeftCursor = new TextureRegion(texture, 64, tHeight - 64 - 4 - 1, 4, 1);
                Window.this.verticalRightCursor = new TextureRegion(texture, 92, tHeight - 64 - 4 - 1, 4, 1);
                Window.this.backgroundCursor = new TextureRegion(texture, 68, 36, 24, 24);
                Window.this.upScroll = new TextureRegion(texture, 88, 104, 16, 8);
                Window.this.leftScroll = new TextureRegion(texture, 80, 88, 8, 16);
                Window.this.rightScroll = new TextureRegion(texture, 104, 88, 8, 16);
                Window.this.downScroll = new TextureRegion(texture, 88, 80, 16, 8);
                Window.this.pauses[0] = new TextureRegion(texture, 96, 48, 16, 16);
                Window.this.pauses[1] = new TextureRegion(texture, 112, 48, 16, 16);
                Window.this.pauses[2] = new TextureRegion(texture, 96, 32, 16, 16);
                Window.this.pauses[3] = new TextureRegion(texture, 112, 32, 16, 16);
                Window.this.updateBackground();
            }
        });
    }

    public Bitmap getContents() {
        return this.contents;
    }

    public void setContents(Bitmap contents) {
        this.contents = contents;
    }

    public Rect getCursor_rect() {
        return this.cursor_rect;
    }

    public void setCursor_rect(Rect cursorRect) {
        Gdx.app.log("Window", "Attempt to enable cursor. Rect is " + cursorRect);
        this.cursor_rect = cursorRect;
    }

    @Override
    public Viewport getViewport() {
        return this.viewport;
    }

    public void setViewport(Viewport viewport) {
        this.viewport = viewport;
        Gdx.app.log("Window", "set Viewport @ " + viewport.toString());
    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isVisible() {
        return this.visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isArrows_visible() {
        return this.arrows_visible;
    }

    public void setArrows_visible(boolean arrows_visible) {
        this.arrows_visible = arrows_visible;
    }

    public boolean isPause() {
        return this.pause;
    }

    public void setPause(boolean pause) {
        this.pause = pause;
    }

    public int getOpenness() {
        return this.openness;
    }

    public void setOpenness(int value) {
        if (value >= 255) {
            this.openness = 255;
        } else if (value <= 0) {
            this.openness = 0;
        } else {
            this.openness = value;
        }
    }

    public Tone getTone() {
        return this.tone;
    }

    public void setTone(Tone t) {
        this.tone = t;
    }

    public Texture getBaseWindowSkin() {
        return this.baseWindowSkin;
    }

    public void setBaseWindowSkin(Texture baseWindowSkin) {
        this.baseWindowSkin = baseWindowSkin;
    }

    public FrameBuffer getBackground() {
        return this.background;
    }

    public void setBackground(FrameBuffer background) {
        this.background = background;
    }

    public TextureRegion getUpperleft() {
        return this.upperleft;
    }

    public void setUpperleft(TextureRegion upperleft) {
        this.upperleft = upperleft;
    }

    public TextureRegion getLowerleft() {
        return this.lowerleft;
    }

    public void setLowerleft(TextureRegion lowerleft) {
        this.lowerleft = lowerleft;
    }

    public TextureRegion getUpperright() {
        return this.upperright;
    }

    public void setUpperright(TextureRegion upperright) {
        this.upperright = upperright;
    }

    public TextureRegion getLowerright() {
        return this.lowerright;
    }

    public void setLowerright(TextureRegion lowerright) {
        this.lowerright = lowerright;
    }

    public TextureRegion getHorizontalTop() {
        return this.horizontalTop;
    }

    public void setHorizontalTop(TextureRegion horizontalTop) {
        this.horizontalTop = horizontalTop;
    }

    public TextureRegion getHorizontalBottom() {
        return this.horizontalBottom;
    }

    public void setHorizontalBottom(TextureRegion horizontalBottom) {
        this.horizontalBottom = horizontalBottom;
    }

    public TextureRegion getVerticalLeft() {
        return this.verticalLeft;
    }

    public void setVerticalLeft(TextureRegion verticalLeft) {
        this.verticalLeft = verticalLeft;
    }

    public TextureRegion getVerticalRight() {
        return this.verticalRight;
    }

    public void setVerticalRight(TextureRegion verticalRight) {
        this.verticalRight = verticalRight;
    }

    public TextureRegion getUpperleftCursor() {
        return this.upperleftCursor;
    }

    public void setUpperleftCursor(TextureRegion upperleftCursor) {
        this.upperleftCursor = upperleftCursor;
    }

    public TextureRegion getLowerleftCursor() {
        return this.lowerleftCursor;
    }

    public void setLowerleftCursor(TextureRegion lowerleftCursor) {
        this.lowerleftCursor = lowerleftCursor;
    }

    public TextureRegion getUpperrightCursor() {
        return this.upperrightCursor;
    }

    public void setUpperrightCursor(TextureRegion upperrightCursor) {
        this.upperrightCursor = upperrightCursor;
    }

    public TextureRegion getLowerrightCursor() {
        return this.lowerrightCursor;
    }

    public void setLowerrightCursor(TextureRegion lowerrightCursor) {
        this.lowerrightCursor = lowerrightCursor;
    }

    public TextureRegion getHorizontalTopCursor() {
        return this.horizontalTopCursor;
    }

    public void setHorizontalTopCursor(TextureRegion horizontalTopCursor) {
        this.horizontalTopCursor = horizontalTopCursor;
    }

    public TextureRegion getHorizontalBottomCursor() {
        return this.horizontalBottomCursor;
    }

    public void setHorizontalBottomCursor(TextureRegion horizontalBottomCursor) {
        this.horizontalBottomCursor = horizontalBottomCursor;
    }

    public TextureRegion getVerticalLeftCursor() {
        return this.verticalLeftCursor;
    }

    public void setVerticalLeftCursor(TextureRegion verticalLeftCursor) {
        this.verticalLeftCursor = verticalLeftCursor;
    }

    public TextureRegion getVerticalRightCursor() {
        return this.verticalRightCursor;
    }

    public void setVerticalRightCursor(TextureRegion verticalRightCursor) {
        this.verticalRightCursor = verticalRightCursor;
    }

    public TextureRegion getBackgroundCursor() {
        return this.backgroundCursor;
    }

    public void setBackgroundCursor(TextureRegion backgroundCursor) {
        this.backgroundCursor = backgroundCursor;
    }

    public TextureRegion getDownScroll() {
        return this.downScroll;
    }

    public void setDownScroll(TextureRegion downScroll) {
        this.downScroll = downScroll;
    }

    public TextureRegion getUpScroll() {
        return this.upScroll;
    }

    public void setUpScroll(TextureRegion upScroll) {
        this.upScroll = upScroll;
    }

    public TextureRegion getLeftScroll() {
        return this.leftScroll;
    }

    public void setLeftScroll(TextureRegion leftScroll) {
        this.leftScroll = leftScroll;
    }

    public TextureRegion getRightScroll() {
        return this.rightScroll;
    }

    public void setRightScroll(TextureRegion rightScroll) {
        this.rightScroll = rightScroll;
    }

    public TextureRegion[] getPauses() {
        return this.pauses;
    }

    public void setPauses(TextureRegion[] pauses) {
        this.pauses = pauses;
    }

    public int getPauseCounter() {
        return this.pauseCounter;
    }

    public void setPauseCounter(int pauseCounter) {
        this.pauseCounter = pauseCounter;
    }

    public int getLastToneHash() {
        return this.lastToneHash;
    }

    public void setLastToneHash(int lastToneHash) {
        this.lastToneHash = lastToneHash;
    }

    public float getCursor_opacity() {
        return this.cursor_opacity;
    }

    public void setCursor_opacity(float cursor_opacity) {
        this.cursor_opacity = cursor_opacity;
    }

    public float getCursor_inc() {
        return this.cursor_inc;
    }

    public void setCursor_inc(float cursor_inc) {
        this.cursor_inc = cursor_inc;
    }

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof Window)) {
            return false;
        } else {
            Window other = (Window) o;
            if (!other.canEqual(this)) {
                return false;
            } else if (this.getX() != other.getX()) {
                return false;
            } else if (this.getY() != other.getY()) {
                return false;
            } else if (this.getWidth() != other.getWidth()) {
                return false;
            } else if (this.getHeight() != other.getHeight()) {
                return false;
            } else if (this.getZ() != other.getZ()) {
                return false;
            } else if (this.getOx() != other.getOx()) {
                return false;
            } else if (this.getOy() != other.getOy()) {
                return false;
            } else if (this.getPadding() != other.getPadding()) {
                return false;
            } else if (this.getPadding_bottom() != other.getPadding_bottom()) {
                return false;
            } else if (this.getOpacity() != other.getOpacity()) {
                return false;
            } else if (this.getBack_opacity() != other.getBack_opacity()) {
                return false;
            } else if (this.getContents_opacity() != other.getContents_opacity()) {
                return false;
            } else {
                Object this$windowskin = this.getWindowskin();
                Object other$windowskin = other.getWindowskin();
                if (Objects.equals(this$windowskin, other$windowskin)) {
                    Object this$contents = this.getContents();
                    Object other$contents = other.getContents();
                    if (Objects.equals(this$contents, other$contents)) {
                        Object this$cursor_rect = this.getCursor_rect();
                        Object other$cursor_rect = other.getCursor_rect();
                        if (Objects.equals(this$cursor_rect, other$cursor_rect)) {
                            Object this$viewport = this.getViewport();
                            Object other$viewport = other.getViewport();
                            if (Objects.equals(this$viewport, other$viewport)) {
                                if (this.isActive() != other.isActive()) {
                                    return false;
                                } else if (this.isVisible() != other.isVisible()) {
                                    return false;
                                } else if (this.isArrows_visible() != other.isArrows_visible()) {
                                    return false;
                                } else if (this.isPause() != other.isPause()) {
                                    return false;
                                } else if (this.getOpenness() != other.getOpenness()) {
                                    return false;
                                } else {
                                    Object this$tone = this.getTone();
                                    Object other$tone = other.getTone();
                                    if (Objects.equals(this$tone, other$tone)) {
                                        if (this.isDisposed() != other.isDisposed()) {
                                            return false;
                                        } else {
                                            Object this$baseWindowSkin = this.getBaseWindowSkin();
                                            Object other$baseWindowSkin = other.getBaseWindowSkin();
                                            if (Objects.equals(this$baseWindowSkin, other$baseWindowSkin)) {
                                                Object this$background = this.getBackground();
                                                Object other$background = other.getBackground();
                                                if (Objects.equals(this$background, other$background)) {
                                                    Object this$upperleft = this.getUpperleft();
                                                    Object other$upperleft = other.getUpperleft();
                                                    if (Objects.equals(this$upperleft, other$upperleft)) {
                                                        Object this$lowerleft = this.getLowerleft();
                                                        Object other$lowerleft = other.getLowerleft();
                                                        if (Objects.equals(this$lowerleft, other$lowerleft)) {
                                                            Object this$upperright = this.getUpperright();
                                                            Object other$upperright = other.getUpperright();
                                                            if (Objects.equals(this$upperright, other$upperright)) {
                                                                Object this$lowerright = this.getLowerright();
                                                                Object other$lowerright = other.getLowerright();
                                                                if (Objects.equals(this$lowerright, other$lowerright)) {
                                                                    Object this$horizontalTop = this.getHorizontalTop();
                                                                    Object other$horizontalTop = other.getHorizontalTop();
                                                                    if (Objects.equals(this$horizontalTop, other$horizontalTop)) {
                                                                        Object this$horizontalBottom = this.getHorizontalBottom();
                                                                        Object other$horizontalBottom = other.getHorizontalBottom();
                                                                        if (Objects.equals(this$horizontalBottom, other$horizontalBottom)) {
                                                                            Object this$verticalLeft = this.getVerticalLeft();
                                                                            Object other$verticalLeft = other.getVerticalLeft();
                                                                            if (Objects.equals(this$verticalLeft, other$verticalLeft)) {
                                                                                Object this$verticalRight = this.getVerticalRight();
                                                                                Object other$verticalRight = other.getVerticalRight();
                                                                                if (Objects.equals(this$verticalRight, other$verticalRight)) {
                                                                                    Object this$upperleftCursor = this.getUpperleftCursor();
                                                                                    Object other$upperleftCursor = other.getUpperleftCursor();
                                                                                    if (Objects.equals(this$upperleftCursor, other$upperleftCursor)) {
                                                                                        Object this$lowerleftCursor = this.getLowerleftCursor();
                                                                                        Object other$lowerleftCursor = other.getLowerleftCursor();
                                                                                        if (Objects.equals(this$lowerleftCursor, other$lowerleftCursor)) {
                                                                                            Object this$upperrightCursor = this.getUpperrightCursor();
                                                                                            Object other$upperrightCursor = other.getUpperrightCursor();
                                                                                            if (Objects.equals(this$upperrightCursor, other$upperrightCursor)) {
                                                                                                Object this$lowerrightCursor = this.getLowerrightCursor();
                                                                                                Object other$lowerrightCursor = other.getLowerrightCursor();
                                                                                                if (Objects.equals(this$lowerrightCursor, other$lowerrightCursor)) {
                                                                                                    Object this$horizontalTopCursor = this.getHorizontalTopCursor();
                                                                                                    Object other$horizontalTopCursor = other.getHorizontalTopCursor();
                                                                                                    if (Objects.equals(this$horizontalTopCursor, other$horizontalTopCursor)) {
                                                                                                        Object this$horizontalBottomCursor = this.getHorizontalBottomCursor();
                                                                                                        Object other$horizontalBottomCursor = other.getHorizontalBottomCursor();
                                                                                                        if (Objects.equals(this$horizontalBottomCursor, other$horizontalBottomCursor)) {
                                                                                                            Object this$verticalLeftCursor = this.getVerticalLeftCursor();
                                                                                                            Object other$verticalLeftCursor = other.getVerticalLeftCursor();
                                                                                                            if (Objects.equals(this$verticalLeftCursor, other$verticalLeftCursor)) {
                                                                                                                Object this$verticalRightCursor = this.getVerticalRightCursor();
                                                                                                                Object other$verticalRightCursor = other.getVerticalRightCursor();
                                                                                                                if (Objects.equals(this$verticalRightCursor, other$verticalRightCursor)) {
                                                                                                                    Object this$backgroundCursor = this.getBackgroundCursor();
                                                                                                                    Object other$backgroundCursor = other.getBackgroundCursor();
                                                                                                                    if (Objects.equals(this$backgroundCursor, other$backgroundCursor)) {
                                                                                                                        Object this$downScroll = this.getDownScroll();
                                                                                                                        Object other$downScroll = other.getDownScroll();
                                                                                                                        if (Objects.equals(this$downScroll, other$downScroll)) {
                                                                                                                            Object this$upScroll = this.getUpScroll();
                                                                                                                            Object other$upScroll = other.getUpScroll();
                                                                                                                            if (Objects.equals(this$upScroll, other$upScroll)) {
                                                                                                                                Object this$leftScroll = this.getLeftScroll();
                                                                                                                                Object other$leftScroll = other.getLeftScroll();
                                                                                                                                if (Objects.equals(this$leftScroll, other$leftScroll)) {
                                                                                                                                    Object this$rightScroll = this.getRightScroll();
                                                                                                                                    Object other$rightScroll = other.getRightScroll();
                                                                                                                                    if (Objects.equals(this$rightScroll, other$rightScroll)) {
                                                                                                                                        if (!Arrays.deepEquals(this.getPauses(), other.getPauses())) {
                                                                                                                                            return false;
                                                                                                                                        } else if (this.getPauseCounter() != other.getPauseCounter()) {
                                                                                                                                            return false;
                                                                                                                                        } else if (this.getLastToneHash() != other.getLastToneHash()) {
                                                                                                                                            return false;
                                                                                                                                        } else if (Float.compare(this.getCursor_opacity(), other.getCursor_opacity()) != 0) {
                                                                                                                                            return false;
                                                                                                                                        } else {
                                                                                                                                            return Float.compare(this.getCursor_inc(), other.getCursor_inc()) == 0 && this.getId() == other.getId();
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
        }
    }

    protected boolean canEqual(Object other) {
        return other instanceof Window;
    }

    @Override
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + this.getX();
        result = result * 59 + this.getY();
        result = result * 59 + this.getWidth();
        result = result * 59 + this.getHeight();
        result = result * 59 + this.getZ();
        result = result * 59 + this.getOx();
        result = result * 59 + this.getOy();
        result = result * 59 + this.getPadding();
        result = result * 59 + this.getPadding_bottom();
        result = result * 59 + this.getOpacity();
        result = result * 59 + this.getBack_opacity();
        result = result * 59 + this.getContents_opacity();
        Object $windowskin = this.getWindowskin();
        result = result * 59 + ($windowskin == null ? 43 : $windowskin.hashCode());
        Object $contents = this.getContents();
        result = result * 59 + ($contents == null ? 43 : $contents.hashCode());
        Object $cursor_rect = this.getCursor_rect();
        result = result * 59 + ($cursor_rect == null ? 43 : $cursor_rect.hashCode());
        Object $viewport = this.getViewport();
        result = result * 59 + ($viewport == null ? 43 : $viewport.hashCode());
        result = result * 59 + (this.isActive() ? 79 : 97);
        result = result * 59 + (this.isVisible() ? 79 : 97);
        result = result * 59 + (this.isArrows_visible() ? 79 : 97);
        result = result * 59 + (this.isPause() ? 79 : 97);
        result = result * 59 + this.getOpenness();
        Object $tone = this.getTone();
        result = result * 59 + ($tone == null ? 43 : $tone.hashCode());
        result = result * 59 + (this.isDisposed() ? 79 : 97);
        Object $baseWindowSkin = this.getBaseWindowSkin();
        result = result * 59 + ($baseWindowSkin == null ? 43 : $baseWindowSkin.hashCode());
        Object $background = this.getBackground();
        result = result * 59 + ($background == null ? 43 : $background.hashCode());
        Object $upperleft = this.getUpperleft();
        result = result * 59 + ($upperleft == null ? 43 : $upperleft.hashCode());
        Object $lowerleft = this.getLowerleft();
        result = result * 59 + ($lowerleft == null ? 43 : $lowerleft.hashCode());
        Object $upperright = this.getUpperright();
        result = result * 59 + ($upperright == null ? 43 : $upperright.hashCode());
        Object $lowerright = this.getLowerright();
        result = result * 59 + ($lowerright == null ? 43 : $lowerright.hashCode());
        Object $horizontalTop = this.getHorizontalTop();
        result = result * 59 + ($horizontalTop == null ? 43 : $horizontalTop.hashCode());
        Object $horizontalBottom = this.getHorizontalBottom();
        result = result * 59 + ($horizontalBottom == null ? 43 : $horizontalBottom.hashCode());
        Object $verticalLeft = this.getVerticalLeft();
        result = result * 59 + ($verticalLeft == null ? 43 : $verticalLeft.hashCode());
        Object $verticalRight = this.getVerticalRight();
        result = result * 59 + ($verticalRight == null ? 43 : $verticalRight.hashCode());
        Object $upperleftCursor = this.getUpperleftCursor();
        result = result * 59 + ($upperleftCursor == null ? 43 : $upperleftCursor.hashCode());
        Object $lowerleftCursor = this.getLowerleftCursor();
        result = result * 59 + ($lowerleftCursor == null ? 43 : $lowerleftCursor.hashCode());
        Object $upperrightCursor = this.getUpperrightCursor();
        result = result * 59 + ($upperrightCursor == null ? 43 : $upperrightCursor.hashCode());
        Object $lowerrightCursor = this.getLowerrightCursor();
        result = result * 59 + ($lowerrightCursor == null ? 43 : $lowerrightCursor.hashCode());
        Object $horizontalTopCursor = this.getHorizontalTopCursor();
        result = result * 59 + ($horizontalTopCursor == null ? 43 : $horizontalTopCursor.hashCode());
        Object $horizontalBottomCursor = this.getHorizontalBottomCursor();
        result = result * 59 + ($horizontalBottomCursor == null ? 43 : $horizontalBottomCursor.hashCode());
        Object $verticalLeftCursor = this.getVerticalLeftCursor();
        result = result * 59 + ($verticalLeftCursor == null ? 43 : $verticalLeftCursor.hashCode());
        Object $verticalRightCursor = this.getVerticalRightCursor();
        result = result * 59 + ($verticalRightCursor == null ? 43 : $verticalRightCursor.hashCode());
        Object $backgroundCursor = this.getBackgroundCursor();
        result = result * 59 + ($backgroundCursor == null ? 43 : $backgroundCursor.hashCode());
        Object $downScroll = this.getDownScroll();
        result = result * 59 + ($downScroll == null ? 43 : $downScroll.hashCode());
        Object $upScroll = this.getUpScroll();
        result = result * 59 + ($upScroll == null ? 43 : $upScroll.hashCode());
        Object $leftScroll = this.getLeftScroll();
        result = result * 59 + ($leftScroll == null ? 43 : $leftScroll.hashCode());
        Object $rightScroll = this.getRightScroll();
        result = result * 59 + ($rightScroll == null ? 43 : $rightScroll.hashCode());
        result = result * 59 + Arrays.deepHashCode(this.getPauses());
        result = result * 59 + this.getPauseCounter();
        result = result * 59 + this.getLastToneHash();
        result = result * 59 + Float.floatToIntBits(this.getCursor_opacity());
        result = result * 59 + Float.floatToIntBits(this.getCursor_inc());
        long $id = this.getId();
        return result * 59 + (int) ($id >>> 32 ^ $id);
    }

    @Override
    public String toString() {
        return "Window(x=" + this.getX() + ", y=" + this.getY() + ", width=" + this.getWidth() + ", height=" + this.getHeight() + ", z=" + this.getZ() + ", ox=" + this.getOx() + ", oy=" + this.getOy() + ", padding=" + this.getPadding() + ", padding_bottom=" + this.getPadding_bottom() + ", opacity=" + this.getOpacity() + ", back_opacity=" + this.getBack_opacity() + ", contents_opacity=" + this.getContents_opacity() + ", cursor_rect=" + this.getCursor_rect() + ", viewport=" + this.getViewport() + ", active=" + this.isActive() + ", visible=" + this.isVisible() + ", arrows_visible=" + this.isArrows_visible() + ", pause=" + this.isPause() + ", openness=" + this.getOpenness() + ", tone=" + this.getTone() + ", disposed=" + this.isDisposed() + ", baseWindowSkin=" + this.getBaseWindowSkin() + ", background=" + this.getBackground() + ", upperleft=" + this.getUpperleft() + ", lowerleft=" + this.getLowerleft() + ", upperright=" + this.getUpperright() + ", lowerright=" + this.getLowerright() + ", horizontalTop=" + this.getHorizontalTop() + ", horizontalBottom=" + this.getHorizontalBottom() + ", verticalLeft=" + this.getVerticalLeft() + ", verticalRight=" + this.getVerticalRight() + ", upperleftCursor=" + this.getUpperleftCursor() + ", lowerleftCursor=" + this.getLowerleftCursor() + ", upperrightCursor=" + this.getUpperrightCursor() + ", lowerrightCursor=" + this.getLowerrightCursor() + ", horizontalTopCursor=" + this.getHorizontalTopCursor() + ", horizontalBottomCursor=" + this.getHorizontalBottomCursor() + ", verticalLeftCursor=" + this.getVerticalLeftCursor() + ", verticalRightCursor=" + this.getVerticalRightCursor() + ", backgroundCursor=" + this.getBackgroundCursor() + ", downScroll=" + this.getDownScroll() + ", upScroll=" + this.getUpScroll() + ", leftScroll=" + this.getLeftScroll() + ", rightScroll=" + this.getRightScroll() + ", pauses=" + Arrays.deepToString(this.getPauses()) + ", pauseCounter=" + this.getPauseCounter() + ", lastToneHash=" + this.getLastToneHash() + ", cursor_opacity=" + this.getCursor_opacity() + ", cursor_inc=" + this.getCursor_inc() + ", id=" + this.getId() + ")";
    }

    @Override
    public boolean isDisposed() {
        return this.disposed;
    }

    public void setDisposed(boolean disposed) {
        this.disposed = disposed;
    }
}
