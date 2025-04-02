package org.jrgss.shaders;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public class TransitionShaderProgram extends ShaderProgram {
    static String vertexShader = "#version 150\n"
        + "in vec4 a_position;\n"
        + "in vec4 a_color;\n"
        + "in vec2 a_texCoord0;\n"
        + "uniform mat4 u_projTrans;\n"
        + "out vec4 v_color;\n"
        + "out vec2 v_texCoords;\n\n"
        + "void main()\n"
        + "{\n"
        + "   v_color = a_color;\n"
        + "   v_color.a = v_color.a;\n"
        + "   v_texCoords = a_texCoord0;\n"
        + "   gl_Position =  u_projTrans * a_position;\n"
        + "}\n";
    static String fragmentShader = "#version 150\n" +
        "#ifdef GL_ES\n" +
        "#define LOWP lowp\n" +
        "precision mediump float;\n" +
        "#else\n" +
        "#define LOWP \n" +
        "#endif\n" +
        "in LOWP vec4 v_color;\n" +
        "in vec2 v_texCoords;\n" +
        "uniform sampler2D u_texture;\n" +
        "uniform float fade;\n" +
        "uniform float vague;\n" +
        "out vec4 fragColor;\n" +
        "void main()\n" +
        "{\n" +
        "  vec4 v_texColor =  v_color * texture(u_texture, v_texCoords);\n" +
        "  float start = min(v_texColor.x - vague, 0.0);\n" +
        "  float end = v_texColor.x;\n" +
        "  if(fade < start) {\n" +
        "    discard;\n" +
        "  } else {\n" +
        "    fragColor = vec4(\n" +
        "      0.0,\n" +
        "      0.0,\n" +
        "      0.0,\n" +
        "      min(((fade - start)/end), 1.0)\n" +
        "    );\n" +
        "  }\n" +
        "}";
    private static TransitionShaderProgram INSTANCE;

    private TransitionShaderProgram() {
        super(vertexShader, fragmentShader);
        if (!this.isCompiled()) {
            throw new IllegalArgumentException("Error compiling shader: " + this.getLog());
        }
    }

    public static TransitionShaderProgram get() {
        if (INSTANCE == null) {
            INSTANCE = new TransitionShaderProgram();
            INSTANCE.setUniformf("fade", 0.0F);
            INSTANCE.setUniformf("vague", 0.0F);
        }

        return INSTANCE;
    }

    public void setFade(float fade) {
        this.setUniformf("fade", fade);
    }

    public void setVague(float vague) {
        this.setUniformf("vague", vague);
    }
}
