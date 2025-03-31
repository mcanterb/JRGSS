package org.jrgss.shaders;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public class FontShaders {
   private static ShaderProgram LINE_SHADER;
   private static ShaderProgram CURVE_SHADER;
   private static ShaderProgram FONT_SHADER;
   public static final String MODELVIEW_MATRIX_UNIFORM = "u_modelview";
   public static final String PROJECTION_MATRIX_UNIFORM = "u_projTrans";
   public static final String ACCUMULATION_UNIFORM = "u_accumulation";

   public static ShaderProgram getLineShader() {
      if (LINE_SHADER == null) {
         LINE_SHADER = new FontShaders.LineShader();
      }

      return LINE_SHADER;
   }

   public static ShaderProgram getCurveShader() {
      if (CURVE_SHADER == null) {
         CURVE_SHADER = new FontShaders.CurveShader();
      }

      return CURVE_SHADER;
   }

   public static ShaderProgram getFontShader() {
      if (FONT_SHADER == null) {
         FONT_SHADER = new FontShaders.FontShader();
      }

      return FONT_SHADER;
   }

   private static class CurveShader extends ShaderProgram {
       static String vertexShader =
           "attribute vec2 a_position;\n" +
               "attribute vec2 a_texCoord0;\n" +
               "uniform mat4 " + FontShaders.PROJECTION_MATRIX_UNIFORM + ";\n" +
               "uniform mat4 " + FontShaders.MODELVIEW_MATRIX_UNIFORM + ";\n" +
               "varying vec2 v_coords;\n" +
               "\n" +
               "void main()\n" +
               "{\n" +
               "   v_coords = a_texCoord0;\n" +
               "   gl_Position = " + FontShaders.PROJECTION_MATRIX_UNIFORM + " * " +
               FontShaders.MODELVIEW_MATRIX_UNIFORM + " * vec4(a_position, 0.0, 1.0);\n" +
               "}\n";
       static String fragmentShader =
           "#ifdef GL_ES\n" +
               "#define LOWP lowp\n" +
               "precision mediump float;\n" +
               "#else\n" +
               "#define LOWP \n" +
               "#endif\n" +
               "uniform vec3 " + FontShaders.ACCUMULATION_UNIFORM + ";\n" +
               "varying vec2 v_coords;\n" +
               "float val;\n" +
               "void main() {\n" +
               "  val = (v_coords.x * v_coords.x) - v_coords.y;\n" +
               "  if (val >= 0.0) {\n" +
               "    discard;\n" +
               "  }\n" +
               "  gl_FragColor = vec4(" + FontShaders.ACCUMULATION_UNIFORM + ", 1.0);\n" +
               "}";

      CurveShader() {
         super(vertexShader, fragmentShader);
         if (!isCompiled()) {
            throw new IllegalStateException("Error compiling Curve Shader: " + getLog());
         } else {
            System.out.println("Compiled!");
         }
      }
   }

   private static class FontShader extends ShaderProgram {
       static String vertexShader =
           "attribute vec4 a_position;\n" +
               "attribute vec4 a_color;\n" +
               "attribute vec2 a_texCoord0;\n" +
               "uniform mat4 u_projTrans;\n" +
               "varying vec4 v_color;\n" +
               "varying vec2 v_texCoords;\n\n" +
               "void main()\n" +
               "{\n" +
               "   v_color = a_color;\n" +
               "   v_color.a = v_color.a * (255.0 / 254.0);\n" +
               "   v_texCoords = a_texCoord0;\n" +
               "   gl_Position = u_projTrans * a_position;\n" +
               "}\n";

       static String fragmentShader =
           "#ifdef GL_ES\n" +
               "#define LOWP lowp\n" +
               "precision mediump float;\n" +
               "#else\n" +
               "#define LOWP\n" +
               "#endif\n" +
               "varying LOWP vec4 v_color;\n" +
               "varying vec2 v_texCoords;\n" +
               "uniform sampler2D u_texture;\n" +
               "float result;\n" +
               "float result2;\n" +
               "vec4 tex_val;\n\n" +
               "float font_pixel(vec4 tex_val) {\n" +
               "    result2 = 0.0;\n" +
               "    result = mod(mod(tex_val.r * 65535.0, 256.0), 2.0);\n" +
               "    if (result > 0.5) {\n" +
               "        result2 += 1.0;\n" +
               "    }\n" +
               "    result = mod(floor(tex_val.r * 65535.0 / 256.0), 2.0);\n" +
               "    if (result > 0.5) {\n" +
               "        result2 += 1.0;\n" +
               "    }\n" +
               "    result = mod(mod(tex_val.g * 65535.0, 256.0), 2.0);\n" +
               "    if (result > 0.5) {\n" +
               "        result2 += 1.0;\n" +
               "    }\n" +
               "    result = mod(floor(tex_val.g * 65535.0 / 256.0), 2.0);\n" +
               "    if (result > 0.5) {\n" +
               "        result2 += 1.0;\n" +
               "    }\n" +
               "    result = mod(mod(tex_val.b * 65535.0, 256.0), 2.0);\n" +
               "    if (result > 0.5) {\n" +
               "        result2 += 1.0;\n" +
               "    }\n" +
               "    result = mod(floor(tex_val.b * 65535.0 / 256.0), 2.0);\n" +
               "    if (result > 0.5) {\n" +
               "        result2 += 1.0;\n" +
               "    }\n" +
               "    return result2;\n" +
               "}\n\n" +
               "void main() {\n" +
               "    tex_val = texture2D(u_texture, v_texCoords);\n" +
               "    result2 = font_pixel(tex_val);\n" +
               "    if (result2 < 1.0) {\n" +
               "        discard;\n" +
               "    }\n\n" +
               "    gl_FragColor = vec4(1, 1, 1, result2 / 6.0);\n" +
               "}\n";

      FontShader() {
         super(vertexShader, fragmentShader);
         if (!isCompiled()) {
            throw new IllegalStateException("Error compiling Font Shader: " + getLog());
         } else {
            System.out.println("Compiled!");
         }
      }
   }

   private static class LineShader extends ShaderProgram {
       static String vertexShader =
           "attribute vec2 a_position;\n" +
               "uniform mat4 " + FontShaders.PROJECTION_MATRIX_UNIFORM + ";\n" +
               "uniform mat4 " + FontShaders.MODELVIEW_MATRIX_UNIFORM + ";\n" +
               "\n" +
               "void main()\n" +
               "{\n" +
               "   gl_Position = " + FontShaders.PROJECTION_MATRIX_UNIFORM +
               " * " + FontShaders.MODELVIEW_MATRIX_UNIFORM +
               " * vec4(a_position, 0.0, 1.0);\n" +
               "}\n";
       static String fragmentShader = "#ifdef GL_ES\n" +
           "#define LOWP lowp\n" +
           "precision mediump float;\n" +
           "#else\n" +
           "#define LOWP \n" +
           "#endif\n" +
           "uniform vec3 " + FontShaders.ACCUMULATION_UNIFORM + ";\n" +
           "void main()\n" +
           "{\n" +
           "  gl_FragColor = vec4(" + FontShaders.ACCUMULATION_UNIFORM + ", 1.0);\n" +
           "}";

      LineShader() {
         super(vertexShader, fragmentShader);
         if (!isCompiled()) {
            throw new IllegalStateException("Error compiling Font Shader: " + getLog());
         }
      }
   }
}
