package org.jrgss;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.sun.corba.se.impl.orbutil.graph.Graph;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jrgss.api.Graphics;

/**
 * @author matt
 * @date 8/10/14
 */
public class Scissors {
    @Getter
    private static ThreadLocal<Array<Rectangle>> scissors = new ThreadLocal<Array<Rectangle>>() {

        @Override
        public Array<Rectangle> initialValue() {
            return new Array<>();
        }


    };
    static Vector3 tmp = new Vector3();
    static final Rectangle viewport = new Rectangle();

    /** Pushes a new scissor {@link Rectangle} onto the stack, merging it with the current top of the stack. The minimal area of
     * overlap between the top of stack rectangle and the provided rectangle is pushed onto the stack. This will invoke
     * {@link com.badlogic.gdx.graphics.GL20#glScissor(int, int, int, int)} with the final top of stack rectangle. In case no scissor is yet on the stack
     * this will also enable {@link com.badlogic.gdx.graphics.GL20#GL_SCISSOR_TEST} automatically.
     * <p>
     * Any drawing should be flushed before pushing scissors.
     * @return true if the scissors were pushed. false if the scissor area was zero, in this case the scissors were not pushed and
     *         no drawing should occur. */
    public static boolean pushScissors (Rectangle scissor) {
        fix(scissor);

        if (scissors.get().size == 0) {
            if (scissor.width < 1 || scissor.height < 1) return false;
            Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
        }
        scissors.get().add(scissor);
        Gdx.gl.glScissor((int)scissor.x, (int)scissor.y, (int)scissor.width, (int)scissor.height);
        return true;
    }

    /** Pops the current scissor rectangle from the stack and sets the new scissor area to the new top of stack rectangle. In case
     * no more rectangles are on the stack, {@link GL20#GL_SCISSOR_TEST} is disabled.
     * <p>
     * Any drawing should be flushed before popping scissors. */
    public static Rectangle popScissors () {
        Rectangle old=null;
        if(scissors.get().size > 0) {
            old = scissors.get().pop();
        }
        if (scissors.get().size == 0)
            Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
        else {
            Rectangle scissor = scissors.get().peek();
            Gdx.gl.glScissor((int)scissor.x, (int)scissor.y, (int)scissor.width, (int)scissor.height);
        }
        return old;
    }

    private static void fix (Rectangle rect) {
        rect.x = Math.round(rect.x);
        rect.y = Math.round(rect.y);
        rect.width = Math.round(rect.width);
        rect.height = Math.round(rect.height);
        if (rect.width < 0) {
            rect.width = -rect.width;
            rect.x -= rect.width;
        }
        if (rect.height < 0) {
            rect.height = -rect.height;
            rect.y -= rect.height;
        }
    }

    /** Calculates a scissor rectangle using 0,0,Gdx.graphics.getWidth(),Gdx.graphics.getHeight() as the viewport.
     * @see #calculateScissors(com.badlogic.gdx.graphics.Camera, float, float, float, float, com.badlogic.gdx.math.Matrix4, Rectangle, Rectangle) */
    public static void calculateScissors (Camera camera, Matrix4 batchTransform, Rectangle area, Rectangle scissor) {
        calculateScissors(camera, 0, 0, Graphics.getWidth(), Graphics.getHeight(), batchTransform, area, scissor);
    }

    /** Calculates a scissor rectangle in OpenGL ES window coordinates from a {@link Camera}, a transformation {@link Matrix4} and
     * an axis aligned {@link Rectangle}. The rectangle will get transformed by the camera and transform matrices and is then
     * projected to screen coordinates. Note that only axis aligned rectangles will work with this method. If either the Camera or
     * the Matrix4 have rotational components, the output of this method will not be suitable for
     * {@link GL20#glScissor(int, int, int, int)}.
     * @param camera the {@link Camera}
     * @param batchTransform the transformation {@link Matrix4}
     * @param area the {@link Rectangle} to transform to window coordinates
     * @param scissor the Rectangle to store the result in */
    public static void calculateScissors (Camera camera, float viewportX, float viewportY, float viewportWidth,
                                          float viewportHeight, Matrix4 batchTransform, Rectangle area, Rectangle scissor) {
        tmp.set(area.x, area.y, 0);
        tmp.mul(batchTransform);
        camera.project(tmp, viewportX, viewportY, viewportWidth, viewportHeight);
        scissor.x = tmp.x;
        scissor.y = tmp.y;

        tmp.set(area.x + area.width, area.y + area.height, 0);
        tmp.mul(batchTransform);
        camera.project(tmp, viewportX, viewportY, viewportWidth, viewportHeight);
        scissor.width = tmp.x - scissor.x;
        scissor.height = tmp.y - scissor.y;
    }

    /** @return the current viewport in OpenGL ES window coordinates based on the currently applied scissor */
    public static Rectangle getViewport () {
        if (scissors.get().size == 0) {
            viewport.set(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            return viewport;
        } else {
            Rectangle scissor = scissors.get().peek();
            viewport.set(scissor);
            return viewport;
        }
    }
}
