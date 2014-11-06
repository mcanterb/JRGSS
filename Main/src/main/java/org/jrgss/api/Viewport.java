package org.jrgss.api;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import lombok.Data;
import org.jrgss.JRGSSGame;
import org.jrgss.Scissors;

/**
 * Created by matty on 6/27/14.
 */
@Data
public class Viewport extends AbstractRenderable {
    Rect rect = new Rect();
    boolean visible = true;
    int z=0;
    int ox;
    int oy;
    Color color = new Color();
    Tone tone = new Tone();

    int flashCounter = 0;
    Color flashColor  = null;

    public Viewport() {
        this.rect.set(0,0,Graphics.getWidth(), Graphics.getHeight());
    }

    public Viewport(Rect rect) {
        this.rect = rect;
    }

    public Viewport(int x, int y, int width, int height) {
        this.rect.set(x, y, width, height);
    }


    public void begin(SpriteBatch batch) {
        Rectangle scissors = new Rectangle();
        Rectangle clipBounds = new Rectangle(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
        Scissors.calculateScissors(JRGSSGame.camera, batch.getTransformMatrix(), clipBounds, scissors);
        Scissors.pushScissors(scissors);
    }

    public void end() {

        Scissors.popScissors();

    }

    @Override
    public void render(SpriteBatch batch) {
        if(flashColor != null) {
            batch.setColor(flashColor.getRed() / 255f, flashColor.getGreen() / 255f, flashColor.getBlue() / 255f, flashColor.getAlpha()/255f);
            batch.enableBlending();
            batch.begin();
            batch.draw(Sprite.getColorTexture(), rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
            batch.end();
        }
    }

    public void update() {
        if(flashCounter > 0) {
            flashCounter--;
            if(flashCounter == 0) {
                flashColor = null;
            }
        }
    }

    public void flash(Color color, int duration) {
        Gdx.app.log("Viewport", String.format("Flash %s for %d", color, duration));
        flashColor = color;
        flashCounter = duration;
    }

    @Override
    public Viewport getViewport() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getY() {
        return getRect().getY();
    }
}
/*
rect
The box (Rect) defining the viewport.

visible
The viewport's visibility. If TRUE, the viewport is visible. The default is TRUE.

z
The viewport's z-coordinate. The larger the value, the closer to the player the plane will be displayed.

If multiple objects share the same z-coordinate, the more recently created object will be displayed closest to the player.

ox
The x-coordinate of the viewport's starting point. Change this value to shake the screen, etc.

oy
The y-coordinate of the viewport's starting point. Change this value to shake the screen, etc.

color
The color (Color) to be blended with the viewport. Alpha values are used in the blending ratio.

Handled separately from the color blended into a flash effect.

tone
The viewport's color tone (Tone).
 */
