package org.jrgss.api;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import lombok.Data;
import lombok.ToString;
import org.jrgss.JRGSSGame;

/**
 * Created by matty on 6/27/14.
 */
@Data
@ToString(callSuper = true)
public class Sprite extends AbstractRenderable {
    public static Texture colorTexture;
    public static ShaderProgram alphaBlendingShader = null;
    Bitmap bitmap;
    Rect src_rect = new Rect();
    Viewport viewport;
    boolean visible = true;
    int x, y, z, ox, oy;
    double zoom_x = 1.0, zoom_y = 1.0;
    double angle;
    double wave_amp;
    double wave_length;
    double wave_speed;
    double wave_phase;
    boolean mirror;
    double bush_depth;
    double bush_opacity;
    int opacity = 255;
    int blend_type;
    Color color = new Color();
    Tone tone = new Tone();
    SpriteBatch batch;

    public Sprite() {
        JRGSSGame.runWithGLContext(new Runnable() {
            @Override
            public void run() {
                batch = new SpriteBatch();
                batch.setProjectionMatrix(JRGSSGame.camera.combined);
                batch.setShader(getAlphaBlendingShader());
            }
        });

    }

    public Sprite(Viewport viewport) {
        this.viewport = viewport;
        JRGSSGame.runWithGLContext(new Runnable() {
            @Override
            public void run() {
                batch = new SpriteBatch();
                batch.setProjectionMatrix(JRGSSGame.camera.combined);
                batch.setShader(getAlphaBlendingShader());
            }
        });
    }

    public static Texture getColorTexture() {
        if (colorTexture == null) {
            Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            p.setColor(1f, 1f, 1f, 1f);
            p.fill();
            colorTexture = new Texture(p);
            p.dispose();
        }
        return colorTexture;
    }

    public static ShaderProgram getAlphaBlendingShader() {
        if (alphaBlendingShader != null) return alphaBlendingShader;
        String vertexShader = "attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" //
                + "attribute vec4 " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" //
                + "attribute vec2 " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" //
                + "uniform mat4 u_projTrans;\n" //
                + "varying vec4 v_color;\n" //
                + "varying vec2 v_texCoords;\n" //
                + "\n" //
                + "void main()\n" //
                + "{\n" //
                + "   v_color = " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" //
                + "   v_color.a = v_color.a * (256.0/255.0);\n" //
                + "   v_texCoords = " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" //
                + "   gl_Position =  u_projTrans * " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" //
                + "}\n";
        String fragmentShader = "#ifdef GL_ES\n" //
                + "#define LOWP lowp\n" //
                + "precision mediump float;\n" //
                + "#else\n" //
                + "#define LOWP \n" //
                + "#endif\n" //
                + "varying LOWP vec4 v_color;\n" //
                + "varying vec2 v_texCoords;\n" //
                + "uniform sampler2D u_texture;\n" //
                + "uniform vec4 blend_color;\n"
                + "uniform int blend_mode;\n"
                + "uniform vec4 tone;\n"
                + "void main()\n"//
                + "{\n" //
                + "  vec4 v_texColor =  v_color * texture2D(u_texture, v_texCoords);"
                + "  v_texColor = vec4( (blend_color.x*blend_color.w)+(v_texColor.x*(1.0-blend_color.w)), \n"
                + "                       (blend_color.y*blend_color.w)+(v_texColor.y*(1.0-blend_color.w)), \n"
                + "                       (blend_color.z*blend_color.w)+(v_texColor.z*(1.0-blend_color.w)), \n"
                + "                        v_texColor.w );\n"
                + "  float gray = v_texColor.x*0.149 + v_texColor.y*0.29412 + v_texColor.z*0.0588;\n"
                + "  gl_FragColor = vec4( min(max(tone.x + v_texColor.x + (gray - v_texColor.x)*tone.w, 0.0 ), 1.0),\n"
                + "                         min(max(tone.y + v_texColor.y + (gray - v_texColor.y)*tone.w, 0.0 ), 1.0),\n"
                + "                         min(max(tone.z + v_texColor.z + (gray - v_texColor.z)*tone.w, 0.0 ), 1.0),\n"
                + "                         v_texColor.w );\n"
                + "}";
        ShaderProgram program = new ShaderProgram(vertexShader, fragmentShader);
        if (!program.isCompiled()) throw new IllegalArgumentException("Error compiling shader: " + program.getLog());
        alphaBlendingShader = program;
        return program;
    }

    protected void setShaderTone(Tone t) {
        alphaBlendingShader.setUniformf("tone", t.getRed() / 255f, t.getGreen() / 255f, t.getBlue() / 255f, t.getGray() / 255f);
    }

    public void dispose() {
        super.dispose();
        Gdx.app.log("Sprint", "Dispose called");
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setOx(int x) {
        this.ox = x;
    }

    public void setOy(int y) {
        this.oy = y;
    }

    @Override
    public void render(SpriteBatch _) {
        if(isDisposed()) {
            Gdx.app.log("Sprite","Disposed, but still drawing! ");
        }
        if (bitmap != null && visible && opacity > 0 && (viewport == null || (viewport.isVisible() && !viewport.isDisposed()))) {
            //Gdx.app.log("Sprite", String.format("Rendering: %s, %d, %d, %d, %d", viewport, x, y, ox, oy));
            if(batch == null) {
                batch = new SpriteBatch();
                batch.setShader(getAlphaBlendingShader());
            }
            batch.setProjectionMatrix(JRGSSGame.camera.combined);
            batch.enableBlending();
            if(viewport != null) viewport.begin(batch);
            int viewportX = viewport == null?0:(viewport.rect.x - viewport.ox);
            int viewportY = viewport == null?0:(viewport.rect.y - viewport.oy);
            com.badlogic.gdx.graphics.Color gdxBlend = color.toGDX();
            batch.setColor(1f , 1f, 1f, (opacity / 255f));
            getAlphaBlendingShader().begin();
            getAlphaBlendingShader().setUniformf("blend_color", gdxBlend.r, gdxBlend.g, gdxBlend.b, gdxBlend.a);
            setShaderTone(tone);

            switch (blend_type) {
                case 0:
                    Gdx.gl.glBlendEquation(GL20.GL_FUNC_ADD);
                    batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
                    break;
                case 1:
                    Gdx.gl.glBlendEquation(GL20.GL_FUNC_ADD);
                    batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
                    break;
                case 2:
                    batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
                    Gdx.gl.glBlendEquation(GL20.GL_FUNC_REVERSE_SUBTRACT);
                    break;
            }
            batch.begin();
            bitmap.render(batch, x - (int)(ox*zoom_x) + viewportX, y - (int)(oy*zoom_y) + viewportY,
                    (int)(src_rect.getWidth()*zoom_x), (int)(src_rect.getHeight()*zoom_y),src_rect);
            batch.end();
            Gdx.gl.glBlendEquation(GL20.GL_FUNC_ADD);
            getAlphaBlendingShader().end();
            if(viewport != null) viewport.end();
        }
    }

    public void setBitmap(Bitmap b) {

        this.bitmap = b;
        if (b != null) {
            this.src_rect.set(0, 0, b.getWidth(), b.getHeight());
        }
    }

    /*public int compareTo(Orderable orderable) {
        if (orderable instanceof Sprite) {
            Sprite other = (Sprite) orderable;
            int ret = super.compareTo(orderable);
            if (ret != 0) return ret;
            ret = Long.compare(y, other.getY());
            if (ret != 0) return ret;
            return Long.compare(creationTime, other.getCreationTime());
        }
        return super.compareTo(orderable);
    }*/

    public void update() {

    }

    public void flash(Color color, int duration) {
        Gdx.app.log("Sprite", "Flash!");
    }

    public int getWidth() {
        return src_rect.getWidth();
    }

    public int getHeight() {
        return src_rect.getHeight();
    }

    public void setOpacity(int opacity) {
        this.opacity = Math.max(0, Math.min(255, opacity));
    }

}

/*
    bitmap
Refers to the bitmap (Bitmap) used for the sprite's starting point.

src_rect
The box (Rect) taken from a bitmap.

viewport
Refers to the viewport (Viewport) associated with the sprite.

visible
The sprite's visibility. If TRUE, the sprite is visible. The default value is TRUE.

x
The sprite's x-coordinate.

y
The sprite's y-coordinate.

z
The sprite's z-coordinate. The larger the value, the closer to the player the sprite will be displayed.

If two sprites have the same z-coordinates, the one with the larger y-coordinate will be displayed closer to the player, and if the y-coordinates are the same, the one that was generated later will be displayed closer to the player.

ox
The x-coordinate of the sprite's starting point.

oy
The y-coordinate of the sprite's starting point.

zoom_x
The sprite's x-axis zoom level. 1.0 denotes actual pixel size.

zoom_y
The sprite's y-axis zoom level. 1.0 denotes actual pixel size.

angle
The sprite's angle of rotation. Specifies up to 360 degrees of counterclockwise rotation. However, drawing a rotated sprite is time-consuming, so avoid overuse.

wave_amp
wave_length
wave_speed
wave_phase
Defines the amplitude, frequency, speed, and phase of the wave effect. A raster scroll effect is achieved by using a sinusoidal function to draw the sprite with each line's horizontal position slightly different from the last.

wave_amp is the wave amplitude and wave_length is the wave frequency, and each is specified by a number of pixels.

wave_speed specifies the speed of the wave animation. The default is 360, and the larger the value, the faster the effect.

wave_phase specifies the phase of the top line of the sprite using an angle of up to 360 degrees. This is updated each time the update method is called. It is not necessary to use this property unless it is required for two sprites to have their wave effects synchronized.

mirror
A flag denoting the sprite has been flipped horizontally. If TRUE, the sprite will be drawn flipped. The default is false.

bush_depth
bush_opacity
The bush depth and opacity of a sprite. This can be used to represent a situation such as the character's legs being hidden by bushes.

For bush_depth, the number of pixels for the bush section is specified. The default value is 0.

For bush_opacity, the opacity of the bush section from 0 to 255 is specified. Out-of-range values will be corrected automatically. The default value is 128.

The bush_opacity value will be multiplied by opacity. For example, if both opacity and bush_opacity are set to 128, it will be handled as a transparency on top of a transparency, for an actual opacity of 64.

opacity
The sprite's opacity (0-255). Out-of-range values are automatically corrected.

blend_type
The sprite's blending mode (0: normal, 1: addition, 2: subtraction).

color
The color (Color) to be blended with the sprite. Alpha values are used in the blending ratio.

Handled separately from the color blended into a flash effect. However, the color with the higher alpha value when displayed will have the higher priority when blended.

tone
The sprite's color tone (Tone).
*/