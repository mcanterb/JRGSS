package org.jrgss.shaders;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @author matt
 * @date 1/23/15
 */
@Data
public class TextShaderProgram extends ShaderProgram {
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
            + "void main()\n"//
            + "{\n" //
            + "  vec4 tex_color = texture2D(u_texture, v_texCoords);\n"
            + "  gl_FragColor = v_color * tex_color;\n"
            + "  gl_FragColor.w = gl_FragColor.w*1.3;\n"
            + "}";


    private static TextShaderProgram INSTANCE;


    private TextShaderProgram() {
        super(vertexShader, fragmentShader);
        if (!isCompiled()) throw new IllegalArgumentException("Error compiling shader: " + getLog());
    }

    public static TextShaderProgram get() {
        if (INSTANCE == null) {
            INSTANCE = new TextShaderProgram();
        }
        return INSTANCE;
    }


}
