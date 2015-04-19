package org.jrgss.shaders;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import org.jrgss.api.Tone;

/**
 * @author matt
 * @date 8/12/14
 */
public class ToneShaderProgram extends ShaderProgram {

    static String vertexShader = "attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" //
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
    static String fragmentShader = "#ifdef GL_ES\n" //
            + "#define LOWP lowp\n" //
            + "precision mediump float;\n" //
            + "#else\n" //
            + "#define LOWP \n" //
            + "#endif\n" //
            + "varying LOWP vec4 v_color;\n" //
            + "varying vec2 v_texCoords;\n" //
            + "uniform sampler2D u_texture;\n" //
            + "uniform vec4 tone;\n"
            + "uniform float alpha;\n"
            + "void main()\n"//
            + "{\n" //
            + "  vec4 v_texColor =  v_color * texture2D(u_texture, v_texCoords);\n"
            + "  float gray = v_texColor.x*(38.0/255.0) + v_texColor.y*(75.0/255.0) + v_texColor.z*(15.0/255.0);\n"
            + "  gl_FragColor = vec4( min(max(tone.x + v_texColor.x + (gray - v_texColor.x)*tone.w, 0.0 ), 1.0),\n"
            + "                         min(max(tone.y + v_texColor.y + (gray - v_texColor.y)*tone.w, 0.0 ), 1.0),\n"
            + "                         min(max(tone.z + v_texColor.z + (gray - v_texColor.z)*tone.w, 0.0 ), 1.0),\n"
            + "                         min(1.0, v_texColor.w+alpha) );\n"
            + "}";

    /*
    int gray = ((c.red*38+c.green*75+c.blue*15)/128)%256;
                c.red = (int)Math.min(Math.max(t.red+c.red+(gray-c.red)*grayScalar,0),255);
                c.green = (int)Math.min(Math.max(t.green+c.green+(gray-c.green)*grayScalar,0),255);
                c.blue = (int)Math.min(Math.max(t.blue+c.blue+(gray-c.blue)*grayScalar,0),255);
     */
    private static ToneShaderProgram INSTANCE;


    private ToneShaderProgram() {
        super(vertexShader, fragmentShader);
        if (!isCompiled()) throw new IllegalArgumentException("Error compiling shader: " + getLog());
    }

    public static ToneShaderProgram get() {
        if (INSTANCE == null) {
            INSTANCE = new ToneShaderProgram();
        }
        return INSTANCE;
    }

    public void setTone(Tone t) {
        setUniformf("tone", t.getRed() / 255f, t.getGreen() / 255f, t.getBlue() / 255f, t.getGray() / 255f);
    }

    public void setAlpha(boolean alpha) {
        setUniformf("alpha", alpha?0.0f:1.0f);
    }

}
