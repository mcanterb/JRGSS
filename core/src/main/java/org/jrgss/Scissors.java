package org.jrgss;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import org.jrgss.api.Graphics;

public class Scissors {
    static final Rectangle viewport = new Rectangle();
    private static final ThreadLocal<Array<Rectangle>> scissors = new ThreadLocal<Array<Rectangle>>() {
        public Array<Rectangle> initialValue() {
            return new Array<>();
        }
    };
    static Vector3 tmp = new Vector3();

    public static boolean pushScissors(Rectangle scissor) {
        fix(scissor);
        if (scissors.get().size == 0) {
            if (scissor.width < 1.0F || scissor.height < 1.0F) {
                return false;
            }

            Gdx.gl.glEnable(3089);
        }

        scissors.get().add(scissor);
        Gdx.gl.glScissor((int) scissor.x, (int) scissor.y, (int) scissor.width, (int) scissor.height);
        return true;
    }

    public static void clearScissors() {
        scissors.get().clear();
        Gdx.gl.glDisable(3089);
    }

    public static Rectangle popScissors() {
        Rectangle old = null;
        if (scissors.get().size > 0) {
            old = scissors.get().pop();
        }

        if (scissors.get().size == 0) {
            Gdx.gl.glDisable(3089);
        } else {
            Rectangle scissor = scissors.get().peek();
            Gdx.gl.glScissor((int) scissor.x, (int) scissor.y, (int) scissor.width, (int) scissor.height);
        }

        return old;
    }

    private static void fix(Rectangle rect) {
        rect.x = Math.round(rect.x);
        rect.y = Math.round(rect.y);
        rect.width = Math.round(rect.width);
        rect.height = Math.round(rect.height);
        if (rect.width < 0.0F) {
            rect.width = -rect.width;
            rect.x = rect.x - rect.width;
        }

        if (rect.height < 0.0F) {
            rect.height = -rect.height;
            rect.y = rect.y - rect.height;
        }
    }

    public static void calculateScissors(Camera camera, Matrix4 batchTransform, Rectangle area, Rectangle scissor) {
        calculateScissors(camera, 0.0F, 0.0F, Graphics.getViewportWidth(), Graphics.getViewportHeight(), batchTransform, area, scissor);
    }

    public static void calculateScissors(
        Camera camera, float viewportX, float viewportY, float viewportWidth, float viewportHeight, Matrix4 batchTransform, Rectangle area, Rectangle scissor
    ) {
        tmp.set(area.x, area.y, 0.0F);
        tmp.mul(batchTransform);
        camera.project(tmp, viewportX, viewportY, viewportWidth, viewportHeight);
        scissor.x = tmp.x;
        scissor.y = tmp.y;
        tmp.set(area.x + area.width, area.y + area.height, 0.0F);
        tmp.mul(batchTransform);
        camera.project(tmp, viewportX, viewportY, viewportWidth, viewportHeight);
        scissor.width = tmp.x - scissor.x;
        scissor.height = tmp.y - scissor.y;
    }

    public static Rectangle getViewport() {
        if (scissors.get().size == 0) {
            viewport.set(0.0F, 0.0F, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            return viewport;
        } else {
            Rectangle scissor = scissors.get().peek();
            viewport.set(scissor);
            return viewport;
        }
    }

    public static ThreadLocal<Array<Rectangle>> getScissors() {
        return scissors;
    }
}
