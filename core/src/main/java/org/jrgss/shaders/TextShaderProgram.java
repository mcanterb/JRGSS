package org.jrgss.shaders;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public class TextShaderProgram extends ShaderProgram {
    static String vertexShader = "attribute vec4 a_position;\n" +
        "attribute vec4 a_color;\n" +
        "attribute vec2 a_texCoord0;\n" +
        "uniform mat4 u_projTrans;\n" +
        "varying vec4 v_color;\n" +
        "varying vec2 v_texCoords;\n\n" +
        "void main()\n" +
        "{\n" +
        "   v_color = a_color;\n" +
        "   v_texCoords = a_texCoord0;\n" +
        "   gl_Position =  u_projTrans * a_position;\n" +
        "}\n";
    static String fragmentShader =
        "#ifdef GL_ES\n" +
            "#define LOWP lowp\n" +
            "precision mediump float;\n" +
            "#else\n" +
            "#define LOWP \n" +
            "#endif\n" +
            "varying LOWP vec4 v_color;\n" +
            "varying vec2 v_texCoords;\n" +
            "uniform sampler2D u_texture;\n" +
            "void main()\n" +
            "{\n" +
            "  vec4 tex_color = texture2D(u_texture, v_texCoords);\n" +
            "  gl_FragColor = v_color * tex_color;\n" +
            "  gl_FragColor.w = gl_FragColor.w * 1.3;\n" +
            "}";
   private static TextShaderProgram INSTANCE;

   private TextShaderProgram() {
      super(vertexShader, fragmentShader);
      if (!isCompiled()) {
         throw new IllegalArgumentException("Error compiling shader: " + this.getLog());
      }
   }

   public static TextShaderProgram get() {
      if (INSTANCE == null) {
         INSTANCE = new TextShaderProgram();
      }

      return INSTANCE;
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof TextShaderProgram)) {
         return false;
      } else {
         TextShaderProgram other = (TextShaderProgram)o;
         return other.canEqual(this);
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof TextShaderProgram;
   }

   @Override
   public int hashCode() {
      return 1;
   }

   @Override
   public String toString() {
      return "TextShaderProgram()";
   }
}
