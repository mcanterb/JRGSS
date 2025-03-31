package org.jrgss.shaders;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import org.jrgss.api.Tone;

public class ToneShaderProgram extends ShaderProgram {
    static String vertexShader = String.join("\n",
        "attribute vec4 a_position;",
        "attribute vec4 a_color;",
        "attribute vec2 a_texCoord0;",
        "uniform mat4 u_projTrans;",
        "varying vec4 v_color;",
        "varying vec2 v_texCoords;",
        "",
        "void main()",
        "{",
        "   v_color = a_color;",
        "   v_color.a = v_color.a * (256.0/255.0);",
        "   v_texCoords = a_texCoord0;",
        "   gl_Position =  u_projTrans * a_position;",
        "}"
    );
    static String fragmentShader = String.join("\n",
        "#ifdef GL_ES",
        "#define LOWP lowp",
        "precision mediump float;",
        "#else",
        "#define LOWP",
        "#endif",
        "varying LOWP vec4 v_color;",
        "varying vec2 v_texCoords;",
        "uniform sampler2D u_texture;",
        "uniform vec4 tone;",
        "uniform float alpha;",
        "void main()",
        "{",
        "  vec4 v_texColor = v_color * texture2D(u_texture, v_texCoords);",
        "  float gray = v_texColor.x * (38.0 / 255.0) + v_texColor.y * (75.0 / 255.0) + v_texColor.z * (15.0 / 255.0);",
        "  gl_FragColor = vec4(",
        "    min(max(tone.x + v_texColor.x + (gray - v_texColor.x) * tone.w, 0.0), 1.0),",
        "    min(max(tone.y + v_texColor.y + (gray - v_texColor.y) * tone.w, 0.0), 1.0),",
        "    min(max(tone.z + v_texColor.z + (gray - v_texColor.z) * tone.w, 0.0), 1.0),",
        "    min(1.0, v_texColor.w + alpha)",
        "  );",
        "}"
    );
   private static ToneShaderProgram INSTANCE;

   private ToneShaderProgram() {
      super(vertexShader, fragmentShader);
      if (!this.isCompiled()) {
         throw new IllegalArgumentException("Error compiling shader: " + this.getLog());
      }
   }

   public static ToneShaderProgram get() {
      if (INSTANCE == null) {
         INSTANCE = new ToneShaderProgram();
      }

      return INSTANCE;
   }

   public void setTone(Tone t) {
      this.setUniformf("tone", t.getRed() / 255.0F, t.getGreen() / 255.0F, t.getBlue() / 255.0F, t.getGray() / 255.0F);
   }

   public void setAlpha(boolean alpha) {
      this.setUniformf("alpha", alpha ? 0.0F : 1.0F);
   }
}
