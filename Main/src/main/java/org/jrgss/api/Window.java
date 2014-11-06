package org.jrgss.api;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.jrgss.JRGSSGame;
import org.jrgss.Scissors;
import org.jrgss.shaders.ToneShaderProgram;

/**
 * Created by matty on 6/27/14.
 */
@Data
@NoArgsConstructor
@ToString(exclude = {"windowskin", "contents"})
public class Window extends AbstractRenderable {
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
    Tone tone = new Tone(0, 0, 0, 0);
    @Getter
    boolean disposed = false;
    SpriteBatch batch;
    Texture baseWindowSkin;
    TextureRegion background;
    TextureRegion upperleft;
    TextureRegion lowerleft;
    TextureRegion upperright;
    TextureRegion lowerright;
    TextureRegion horizontalTop;
    TextureRegion horizontalBottom;
    TextureRegion verticalLeft;
    TextureRegion verticalRight;
    TextureRegion tile;
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
    TextureRegion pauses[] = new TextureRegion[4];
    int pauseCounter = 0;
    int lastToneHash = 0;
    float cursor_opacity = 0.5f;
    float cursor_inc = 0.033333333333333f;

    public Window(final int x, final int y, int width, int height) {
        super();
        this.x = x;
        this.y = y;
        this.z = 100;
        this.width = width;
        this.height = height;
        this.windowskin = new Bitmap(width, height);
        this.contents = new Bitmap(width, height);
        this.cursor_rect = new Rect(0, 0, 0, 0);
        Gdx.app.log("Window", "Window @ " + x + "," + y);
        JRGSSGame.runWithGLContext(new Runnable() {
            @Override
            public void run() {
                batch = new SpriteBatch();
                batch.setProjectionMatrix(JRGSSGame.camera.combined);
            }
        });

    }

    public void setWindowskin(final Bitmap bitmap) {
        Gdx.app.log("Window", "Setting up window borders...");
        this.windowskin = bitmap;
        JRGSSGame.runWithGLContext(new Runnable() {
            @Override
            public void run() {
                Texture texture = bitmap.region.getTexture();
                baseWindowSkin = texture;
                int tHeight = texture.getHeight();
                background = new TextureRegion(texture, 0, 64, 64, 64);
                tile = new TextureRegion(texture, 0, 0, 64, 64);
                upperleft = new TextureRegion(texture, 64, tHeight - 16, 16, 16);
                lowerleft = new TextureRegion(texture, 64, tHeight - 64, 16, 16);
                upperright = new TextureRegion(texture, 128 - 16, tHeight - 16, 16, 16);
                lowerright = new TextureRegion(texture, 128 - 16, tHeight - 64, 16, 16);
                horizontalTop = new TextureRegion(texture, 64 + 16, tHeight - 16, 1, 16);
                horizontalBottom = new TextureRegion(texture, 64 + 16, tHeight - 64, 1, 16);
                verticalLeft = new TextureRegion(texture, 64, tHeight - 16 - 1, 16, 1);
                verticalRight = new TextureRegion(texture, 128 - 16, tHeight - 16 - 1, 16, 1);

                upperleftCursor = new TextureRegion(texture, 64, tHeight - 4 - 64, 4, 4);
                lowerleftCursor = new TextureRegion(texture, 64, tHeight - 64 - 32, 4, 4);
                upperrightCursor = new TextureRegion(texture, 128 - 4 - 32, tHeight - 4 - 64, 4, 4);
                lowerrightCursor = new TextureRegion(texture, 128 - 4 - 32, tHeight - 64 - 32, 4, 4);
                horizontalTopCursor = new TextureRegion(texture, 64 + 4, tHeight - 4 - 64, 1, 4);
                horizontalBottomCursor = new TextureRegion(texture, 64 + 4, tHeight - 64 - 32, 1, 4);
                verticalLeftCursor = new TextureRegion(texture, 64, tHeight - 64 - 4 - 1, 4, 1);
                verticalRightCursor = new TextureRegion(texture, 128 - 4 - 32, tHeight - 64 - 4 - 1, 4, 1);
                backgroundCursor = new TextureRegion(texture, 64 + 4, 32 + 4, 32 - 8, 32 - 8);

                upScroll = new TextureRegion(texture, 88, 104, 16, 8);
                leftScroll = new TextureRegion(texture, 80, 88, 8, 16);
                rightScroll = new TextureRegion(texture, 104, 88, 8, 16);
                downScroll = new TextureRegion(texture, 88, 80, 16, 8);

                pauses[0] = new TextureRegion(texture, 96, 48, 16, 16);
                pauses[1] = new TextureRegion(texture, 112, 48, 16, 16);
                pauses[2] = new TextureRegion(texture, 96, 32, 16, 16);
                pauses[3] = new TextureRegion(texture, 112, 32, 16, 16);

            }
        });

    }

    public void setCursor_rect(Rect cursorRect) {
        Gdx.app.log("Window", "Attempt to enable cursor. Rect is " + cursorRect);
        this.cursor_rect = cursorRect;
    }

    public void setTone(final Tone t) {
        this.tone = t;
    }

    public void setViewport(Viewport viewport) {
        this.viewport = viewport;
        Gdx.app.log("Window", "set Viewport @ " + viewport.toString());
    }

    public void setOpacity(int value) {
        this.opacity = value;
    }

    public boolean isOpen() {
        return openness >= 255;
    }

    public void setOpenness(int value) {
        if (value >= 255) {
            openness = 255;
        } else if (value <= 0) {
            openness = 0;
        } else {
            openness = value;
        }

    }

    public void update() {
        cursor_opacity += cursor_inc;
        if (Math.abs(cursor_opacity - 1.0f) < 0.0001f || cursor_opacity < 0.0001f) {
            cursor_inc = -cursor_inc;
        }
        pauseCounter++;
        if (pauseCounter == 60) pauseCounter = 0;
    }

    public boolean isClose() {
        return openness == 0;
    }

    @Override
    public void render(SpriteBatch _) {
        if (!visible || isClose() || (viewport != null && !viewport.isVisible())) return;
        //Gdx.app.log("Window", "Drawing a window!");
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.enableBlending();

        int x = this.getX();
        int y = this.getY();

        if (viewport != null) {
            x = x + viewport.getRect().getX() - viewport.getOx();
            y = y + viewport.getRect().getY() - viewport.getOy();
        }

        float globalOpacity = (opacity / 255f);
        //Gdx.app.log("Window", "rendering "+this.toString());
        if (viewport != null) viewport.begin(batch);
        batch.setShader(ToneShaderProgram.get());
        ToneShaderProgram.get().begin();
        ToneShaderProgram.get().setTone(tone);
        this.batch.begin();
        batch.setColor(1.0f, 1.0f, 1.0f, (back_opacity / 255f) * globalOpacity);
        batch.draw(background, x + 4, y + 4, width - 4 * 2, height - 4 * 2);
        batch.end();
        ToneShaderProgram.get().end();
        batch.setShader(null);
        batch.begin();
        batch.setColor(1.0f, 1.0f, 1.0f, globalOpacity);
        Rectangle scissors = new Rectangle();
        Rectangle clipBounds = new Rectangle(x + 4, y + 4, width - 8, height - 8);
        Scissors.calculateScissors(JRGSSGame.camera, batch.getTransformMatrix(), clipBounds, scissors);
        Scissors.pushScissors(scissors);
        //batch.setBlendFunction(GL20.GL_ONE_MINUS_SRC_ALPHA, GL20.GL_SRC_ALPHA);
        for (int i = x + 4; i < x + (width - 4); i += 64) {
            for (int j = y + 4; j < y + (height - 4); j += 64) {
                batch.draw(tile, i, j, 64, 64);
            }
        }
        batch.flush();
        // batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        Scissors.popScissors();
        batch.draw(upperleft, x, y);
        batch.draw(lowerleft, x, y + height - 16);
        batch.draw(upperright, x + width - 16, y);
        batch.draw(lowerright, x + width - 16, y + height - 16);

        batch.draw(horizontalTop, x + 16, y, width - 2 * 16, 16);
        batch.draw(horizontalBottom, x + 16, y + height - 16, width - 2 * 16, 16);

        batch.draw(verticalLeft, x, y + 16, 16, height - 2 * 16);
        batch.draw(verticalRight, x + width - 16, y + 16, 16, height - 2 * 16);
        batch.setColor(1.0f, 1.0f, 1.0f, contents_opacity / 255f);
        batch.flush();
        //Scissors.pushScissors(scissors);
        batch.setColor(com.badlogic.gdx.graphics.Color.WHITE);
        if (isOpen()) {
            contents.render(this.batch, x + padding, y + padding, ox, oy, width - (padding * 2), height - (padding * 2));
        }
        batch.flush();
        //Scissors.popScissors();

        if (active) {
            batch.setColor(1.0f, 1.0f, 1.0f, cursor_opacity * globalOpacity);
        } else {
            batch.setColor(1.0f, 1.0f, 1.0f, 0.6f * globalOpacity);
        }
        renderCursor(batch);
        batch.setColor(com.badlogic.gdx.graphics.Color.WHITE);

        if (pause) {
            drawPause(batch, x, y);
        }

        if (arrows_visible) {
            if (contents.getHeight() - oy > (height - padding * 2)) {
                drawDownArrow(batch, x, y);
            }
            if (oy > 0) {
                drawUpArrow(batch, x, y);
            }
            if (contents.getWidth() - ox > (width - padding * 2)) {
                drawRightArrow(batch, x, y);
            }
            if (ox > 0) {
                drawLeftArrow(batch, x, y);
            }
        }


        this.batch.end();
        if (viewport != null) viewport.end();
    }

    private void drawPause(SpriteBatch batch, int x, int y) {

        int pauseX = x + ((width - 16) / 2);
        int pauseY = (y + height) - 16 - 4;

        int frame = (pauseCounter / 15);

        batch.draw(pauses[frame], pauseX, pauseY);
    }

    private void drawDownArrow(SpriteBatch batch, int x, int y) {

        int arrowX = x + ((width - 16) / 2);
        int arrowY = (y + height) - 8 - 4;

        batch.draw(downScroll, arrowX, arrowY);
    }

    private void drawUpArrow(SpriteBatch batch, int x, int y) {

        int arrowX = x + ((width - 16) / 2);
        int arrowY = y + 4;

        batch.draw(upScroll, arrowX, arrowY);
    }

    private void drawLeftArrow(SpriteBatch batch, int x, int y) {

        int arrowY = y + ((height - 16) / 2);
        int arrowX = x + 4;

        batch.draw(leftScroll, arrowX, arrowY);
    }

    private void drawRightArrow(SpriteBatch batch, int x, int y) {

        int arrowY = y + ((height - 16) / 2);
        int arrowX = (x + width) - 8 - 4;

        batch.draw(rightScroll, arrowX, arrowY);
    }

    public void dispose() {
        super.dispose();
        JRGSSGame.runWithGLContext(new Runnable() {
            @Override
            public void run() {

                if (!disposed) {
                    if (contents != null) contents.dispose();
                    if (batch != null) batch.dispose();
                }
                disposed = true;
            }
        });

    }

    protected void renderCursor(SpriteBatch batch) {
        if (cursor_rect.height == 0 || cursor_rect.width == 0) return;

        int x = this.x + padding + cursor_rect.getX() - ox;
        int y = this.y + padding + cursor_rect.getY() - oy;

        if (viewport != null) {
            x = x + viewport.getRect().getX() - viewport.getOx();
            y = y + viewport.getRect().getY() - viewport.getOy();
        }

        int width = cursor_rect.getWidth();
        int height = cursor_rect.getHeight();

        batch.draw(upperleftCursor, x, y);
        batch.draw(lowerleftCursor, x, y + height - 4);
        batch.draw(upperrightCursor, x + width - 4, y);
        batch.draw(lowerrightCursor, x + width - 4, y + height - 4);

        batch.draw(horizontalTopCursor, x + 4, y, width - 2 * 4, 4);
        batch.draw(horizontalBottomCursor, x + 4, y + height - 4, width - 2 * 4, 4);

        batch.draw(verticalLeftCursor, x, y + 4, 4, height - 2 * 4);
        batch.draw(verticalRightCursor, x + width - 4, y + 4, 4, height - 2 * 4);
        batch.draw(backgroundCursor, x + 1, y + 1, width - 2, height - 2);
    }
}


/*
windowskin
Refers to the bitmap (Bitmap) used as a window skin.

Skin specifications are nearly identical to those in the previous version (VX). Resource standards: See the detailed information on window skins.

contents
Refers to the bitmap (Bitmap) used for the window's contents.

cursor_rect
The cursor box (Rect).

Specifies a rectangle with coordinates based on the window's contents. (RGSS3)

viewport
Refers to the viewport (Viewport) associated with the window.

active
The cursor's blink status. If TRUE, the cursor is blinking. The default is TRUE.

visible
The window's visibility. If TRUE, the window is visible. The default is TRUE.

arrows_visible (RGSS3)
The visibility of scrolling arrows. If TRUE, the arrows are visible. The default is TRUE.

pause
The pause graphic's visibility. This is a symbol that appears in the message window when waiting for the player to press a button. If TRUE, the graphic is visible. The default is FALSE.

x
The window's x-coordinate.

y
The window's y-coordinate.

width
The window's width.

height
The window's height.

z
The window's z-coordinate. The larger the value, the closer to the player the window will be displayed.

If multiple objects share the same z-coordinate, the more recently created object will be displayed closest to the player.

The default is 100 (RGSS3).

ox
The x-coordinate of the starting point of the window's contents. Change this value to scroll the window's contents.

Also affects the cursor. (RGSS3)

oy
The y-coordinate of the starting point of the window's contents. Change this value to scroll the window's contents.

Also affects the cursor. (RGSS3)

padding (RGSS3)
The size of the padding between the window's frame and contents. The default value is 12. (RGSS3)

padding_bottom (RGSS3)
The padding for the bottom. Must be set after padding because it is changed along with it.

opacity
The window's opacity (0-255). Out-of-range values are automatically corrected. The default value is 255.

back_opacity
The window background's opacity (0-255). Out-of-range values are automatically corrected. The default value is 192 (RGSS3).

contents_opacity
The opacity of the window's contents (0-255). Out-of-range values are automatically corrected. The default value is 255.

openness
The openness of the window (from 0 to 255). Out-of-range values are automatically corrected.

By changing this value in stages from 0 (completely closed) to 255 (completely open), it is possible to create an animation of the window opening and closing. If the openness is less than 255, the contents of the window will not be displayed. The default value is 255.

tone (RGSS3)
The color (Tone) of the window's background.
*/