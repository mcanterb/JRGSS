package org.jrgss.shaders;

import com.badlogic.gdx.graphics.g2d.JrgssBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import org.jrgss.api.Color;
import org.jrgss.api.Tone;

public class AlphaBlendingShader {
    private static String vertexShader =
        "#version 150\n" +
            "in vec4 a_position;\n" +
            "in vec4 a_color;\n" +
            "in vec2 a_texCoord0;\n" +
            "uniform mat4 u_projTrans;\n" +
            "out vec4 v_color;\n" +
            "out vec2 v_texCoords;\n\n" +
            "void main() {\n" +
            "   v_color = a_color;\n" +
            "   v_color.a = v_color.a * (256.0 / 255.0);\n" +
            "   v_texCoords = a_texCoord0;\n" +
            "   gl_Position =  u_projTrans * a_position;\n" +
            "}\n";

    private static String fragmentShader =
        "#version 150\n" +
            "#ifdef GL_ES\n" +
            "#define LOWP lowp\n" +
            "precision mediump float;\n" +
            "#else\n" +
            "#define LOWP \n" +
            "#endif\n" +
            "in LOWP vec4 v_color;\n" +
            "in vec2 v_texCoords;\n" +
            "out vec4 fragColor;\n" +
            "uniform sampler2D u_texture;\n" +
            "uniform vec4 blend_color;\n" +
            "uniform vec4 tone;\n\n" +
            "void main() {\n" +
            "   vec4 v_texColor = v_color * texture(u_texture, v_texCoords);\n" +
            "   if (v_texColor.a < 1.0 / 255.0) discard;\n" +
            "   v_texColor = vec4(mix(v_texColor.rgb, blend_color.rgb, blend_color.a), v_texColor.w);\n" +
            "   float gray = v_texColor.x * 0.149 + v_texColor.y * 0.29412 + v_texColor.z * 0.0588;\n" +
            "   fragColor = vec4(\n" +
            "       min(max(tone.x + v_texColor.x + (gray - v_texColor.x) * tone.w, 0.0), 1.0),\n" +
            "       min(max(tone.y + v_texColor.y + (gray - v_texColor.y) * tone.w, 0.0), 1.0),\n" +
            "       min(max(tone.z + v_texColor.z + (gray - v_texColor.z) * tone.w, 0.0), 1.0),\n" +
            "       v_texColor.w\n" +
            "   );\n" +
            "}\n";
   private static ShaderProgram INSTANCE;
   private static Tone lastTone;
   private static Color lastColor;

   public static void setTone(Tone t, JrgssBatch batch) {
      if (!t.equals(lastTone)) {
         batch.flush();
         INSTANCE.setUniformf("tone", t.getRed() / 255.0F, t.getGreen() / 255.0F, t.getBlue() / 255.0F, t.getGray() / 255.0F);
         lastTone = t;
      }
   }

   public static void setBlendColor(Color c, JrgssBatch batch) {
      if (!c.equals(lastColor)) {
         batch.flush();
         INSTANCE.setUniformf("blend_color", c.getRed() / 255.0F, c.getGreen() / 255.0F, c.getBlue() / 255.0F, c.getAlpha() / 255.0F);
         lastColor = c;
      }
   }

   public static void begin() {
      INSTANCE.begin();
      lastColor = null;
      lastTone = null;
   }

   public static void end() {
      INSTANCE.end();
   }

   public static ShaderProgram get() {
      if (INSTANCE == null) {
         INSTANCE = new ShaderProgram(vertexShader, fragmentShader);
         if (!INSTANCE.isCompiled()) {
            throw new IllegalArgumentException("Error compiling shader: " + INSTANCE.getLog());
         }

         INSTANCE.setUniformf("tone", 0.0F, 0.0F, 0.0F, 0.0F);
         INSTANCE.setUniformf("blend_color", 0.0F, 0.0F, 0.0F, 0.0F);
      }

      return INSTANCE;
   }
}
