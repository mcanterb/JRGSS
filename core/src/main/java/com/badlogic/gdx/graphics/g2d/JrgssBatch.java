package com.badlogic.gdx.graphics.g2d;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.Mesh.VertexDataType;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.NumberUtils;
import java.nio.Buffer;
import org.jrgss.shaders.AlphaBlendingShader;

public class JrgssBatch implements Batch {
   @Deprecated
   public static VertexDataType defaultVertexDataType = VertexDataType.VertexArray;
   private final Mesh mesh;
   final float[] vertices;
   int idx = 0;
   Texture lastTexture = null;
   float invTexWidth = 0.0F;
   float invTexHeight = 0.0F;
   boolean drawing = false;
   private final Matrix4 transformMatrix = new Matrix4();
   private final Matrix4 projectionMatrix = new Matrix4();
   private final Matrix4 combinedMatrix = new Matrix4();
   private boolean blendingDisabled = false;
   private int blendSrcFunc = GL20.GL_SRC_ALPHA;
   private int blendDstFunc = GL20.GL_ONE_MINUS_SRC_ALPHA;
   private int rgbEquation = GL20.GL_FUNC_ADD;
   private int alphaEquation = GL20.GL_FUNC_ADD;
   private final ShaderProgram shader;
   float color = Color.WHITE.toFloatBits();
   private Color tempColor = new Color(1.0F, 1.0F, 1.0F, 1.0F);
   public int renderCalls = 0;
   public int totalRenderCalls = 0;
   public int maxSpritesInBatch = 0;

   public JrgssBatch() {
      this(1000);
   }

   public JrgssBatch(int size) {
      if (size > 8191) {
         throw new IllegalArgumentException("Can't have more than 8191 sprites per batch: " + size);
      } else {
         VertexDataType vertexDataType = Gdx.gl30 != null ? VertexDataType.VertexBufferObjectWithVAO : defaultVertexDataType;
         this.mesh = new Mesh(
            vertexDataType,
            false,
            size * 4,
            size * 6,
            new VertexAttribute(1, 2, "a_position"),
            new VertexAttribute(4, 4, "a_color"),
            new VertexAttribute(16, 2, "a_texCoord0")
         );
         this.projectionMatrix.setToOrtho2D(0.0F, 0.0F, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
         this.vertices = new float[size * 20];
         int len = size * 6;
         short[] indices = new short[len];
         short j = 0;

         for (int i = 0; i < len; j = (short)(j + 4)) {
            indices[i] = j;
            indices[i + 1] = (short)(j + 1);
            indices[i + 2] = (short)(j + 2);
            indices[i + 3] = (short)(j + 2);
            indices[i + 4] = (short)(j + 3);
            indices[i + 5] = j;
            i += 6;
         }

         this.mesh.setIndices(indices);
         this.shader = AlphaBlendingShader.get();
      }
   }

    /**
     * Default vertex shader for the standard rendering pipeline.
     */
    private static final String DEFAULT_VERTEX_SHADER =
        "#version 150\n" +
            "in vec4 a_position;\n" +
            "in vec4 a_color;\n" +
            "in vec2 a_texCoord0;\n" +
            "uniform mat4 u_projTrans;\n" +
            "out vec4 v_color;\n" +
            "out vec2 v_texCoords;\n\n" +
            "void main()\n{\n" +
            "   v_color = a_color;\n" +
            "   v_color.a = v_color.a * (255.0/254.0);\n" +
            "   v_texCoords = a_texCoord0;\n" +
            "   gl_Position = u_projTrans * a_position;\n" +
            "}\n";

    /**
     * Default fragment shader for the standard rendering pipeline.
     */
    private static final String DEFAULT_FRAGMENT_SHADER =
        "#version 150\n" +
            "#ifdef GL_ES\n" +
            "#define LOWP lowp\n" +
            "precision mediump float;\n" +
            "#else\n" +
            "#define LOWP \n" +
            "#endif\n" +
            "in LOWP vec4 v_color;\n" +
            "in vec2 v_texCoords;\n" +
            "uniform sampler2D u_texture;\n" +
            "out vec4 fragColor;\n" +
            "void main()\n{\n" +
            "  fragColor = v_color * texture2D(u_texture, v_texCoords);\n" +
            "}\n";

    /**
     * Creates and returns a default shader program.
     *
     * @return A compiled shader program with default vertex and fragment shaders
     * @throws IllegalArgumentException If the shader compilation fails
     */
    public static ShaderProgram createDefaultShader() {
        ShaderProgram shader = new ShaderProgram(DEFAULT_VERTEX_SHADER, DEFAULT_FRAGMENT_SHADER);

        if (!shader.isCompiled()) {
            throw new IllegalArgumentException("Error compiling default shader: " + shader.getLog());
        }

        return shader;
    }

   @Override
   public void begin() {
      if (this.drawing) {
         throw new IllegalStateException("JrgssBatch.end must be called before begin.");
      } else {
         this.renderCalls = 0;
         Gdx.gl.glDepthMask(false);
         this.shader.begin();
         this.setupMatrices();
         Gdx.gl.glBlendEquationSeparate(this.rgbEquation, this.alphaEquation);
         Gdx.gl.glBlendFunc(this.blendSrcFunc, this.blendDstFunc);
         this.drawing = true;
      }
   }

   @Override
   public void end() {
      if (!this.drawing) {
         throw new IllegalStateException("JrgssBatch.begin must be called before end.");
      } else {
         if (this.idx > 0) {
            this.flush();
         }

         this.lastTexture = null;
         this.drawing = false;
         GL20 gl = Gdx.gl;
         gl.glDepthMask(true);
         if (this.isBlendingEnabled()) {
            gl.glDisable(3042);
         }
      }
   }

   @Override
   public void setColor(Color tint) {
      this.color = tint.toFloatBits();
   }

   @Override
   public void setColor(float r, float g, float b, float a) {
      int intBits = (int)(255.0F * a) << 24 | (int)(255.0F * b) << 16 | (int)(255.0F * g) << 8 | (int)(255.0F * r);
      this.color = NumberUtils.intToFloatColor(intBits);
   }

   public void setColor(float color) {
      this.color = color;
   }

   @Override
   public Color getColor() {
      int intBits = NumberUtils.floatToIntColor(this.color);
      Color color = this.tempColor;
      color.r = (intBits & 0xFF) / 255.0F;
      color.g = (intBits >>> 8 & 0xFF) / 255.0F;
      color.b = (intBits >>> 16 & 0xFF) / 255.0F;
      color.a = (intBits >>> 24 & 0xFF) / 255.0F;
      return color;
   }

    @Override
    public void setPackedColor(float packedColor) {
        this.color = packedColor;
    }

    @Override
   public float getPackedColor() {
      return color;
   }

   @Override
   public void draw(
      Texture texture,
      float x,
      float y,
      float originX,
      float originY,
      float width,
      float height,
      float scaleX,
      float scaleY,
      float rotation,
      int srcX,
      int srcY,
      int srcWidth,
      int srcHeight,
      boolean flipX,
      boolean flipY
   ) {
      if (!this.drawing) {
         throw new IllegalStateException("JrgssBatch.begin must be called before draw.");
      } else {
         float[] vertices = this.vertices;
         if (texture != this.lastTexture) {
            this.switchTexture(texture);
         } else if (this.idx == vertices.length) {
            this.flush();
         }

         float worldOriginX = x + originX;
         float worldOriginY = y + originY;
         float fx = -originX;
         float fy = -originY;
         float fx2 = width - originX;
         float fy2 = height - originY;
         if (scaleX != 1.0F || scaleY != 1.0F) {
            fx *= scaleX;
            fy *= scaleY;
            fx2 *= scaleX;
            fy2 *= scaleY;
         }

         float x1;
         float y1;
         float x2;
         float y2;
         float x3;
         float y3;
         float x4;
         float y4;
         if (rotation != 0.0F) {
            float cos = MathUtils.cosDeg(rotation);
            float sin = MathUtils.sinDeg(rotation);
            x1 = cos * fx - sin * fy;
            y1 = sin * fx + cos * fy;
            x2 = cos * fx - sin * fy2;
            y2 = sin * fx + cos * fy2;
            x3 = cos * fx2 - sin * fy2;
            y3 = sin * fx2 + cos * fy2;
            x4 = x1 + (x3 - x2);
            y4 = y3 - (y2 - y1);
         } else {
            x1 = fx;
            y1 = fy;
            x2 = fx;
            y2 = fy2;
            x3 = fx2;
            y3 = fy2;
            x4 = fx2;
            y4 = fy;
         }

         x1 += worldOriginX;
         y1 += worldOriginY;
         x2 += worldOriginX;
         y2 += worldOriginY;
         x3 += worldOriginX;
         y3 += worldOriginY;
         x4 += worldOriginX;
         y4 += worldOriginY;
         float u = srcX * this.invTexWidth;
         float v = (srcY + srcHeight) * this.invTexHeight;
         float u2 = (srcX + srcWidth) * this.invTexWidth;
         float v2 = srcY * this.invTexHeight;
         if (flipX) {
            float tmp = u;
            u = u2;
            u2 = tmp;
         }

         if (flipY) {
            float tmp = v;
            v = v2;
            v2 = tmp;
         }

         float color = this.color;
         int idx = this.idx;
         vertices[idx] = x1;
         vertices[idx + 1] = y1;
         vertices[idx + 2] = color;
         vertices[idx + 3] = u;
         vertices[idx + 4] = v;
         vertices[idx + 5] = x2;
         vertices[idx + 6] = y2;
         vertices[idx + 7] = color;
         vertices[idx + 8] = u;
         vertices[idx + 9] = v2;
         vertices[idx + 10] = x3;
         vertices[idx + 11] = y3;
         vertices[idx + 12] = color;
         vertices[idx + 13] = u2;
         vertices[idx + 14] = v2;
         vertices[idx + 15] = x4;
         vertices[idx + 16] = y4;
         vertices[idx + 17] = color;
         vertices[idx + 18] = u2;
         vertices[idx + 19] = v;
         this.idx = idx + 20;
      }
   }

   @Override
   public void draw(Texture texture, float x, float y, float width, float height, int srcX, int srcY, int srcWidth, int srcHeight, boolean flipX, boolean flipY) {
      if (!this.drawing) {
         throw new IllegalStateException("JrgssBatch.begin must be called before draw.");
      } else {
         float[] vertices = this.vertices;
         if (texture != this.lastTexture) {
            this.switchTexture(texture);
         } else if (this.idx == vertices.length) {
            this.flush();
         }

         float u = srcX * this.invTexWidth;
         float v = (srcY + srcHeight) * this.invTexHeight;
         float u2 = (srcX + srcWidth) * this.invTexWidth;
         float v2 = srcY * this.invTexHeight;
         float fx2 = x + width;
         float fy2 = y + height;
         if (flipX) {
            float tmp = u;
            u = u2;
            u2 = tmp;
         }

         if (flipY) {
            float tmp = v;
            v = v2;
            v2 = tmp;
         }

         float color = this.color;
         int idx = this.idx;
         vertices[idx] = x;
         vertices[idx + 1] = y;
         vertices[idx + 2] = color;
         vertices[idx + 3] = u;
         vertices[idx + 4] = v;
         vertices[idx + 5] = x;
         vertices[idx + 6] = fy2;
         vertices[idx + 7] = color;
         vertices[idx + 8] = u;
         vertices[idx + 9] = v2;
         vertices[idx + 10] = fx2;
         vertices[idx + 11] = fy2;
         vertices[idx + 12] = color;
         vertices[idx + 13] = u2;
         vertices[idx + 14] = v2;
         vertices[idx + 15] = fx2;
         vertices[idx + 16] = y;
         vertices[idx + 17] = color;
         vertices[idx + 18] = u2;
         vertices[idx + 19] = v;
         this.idx = idx + 20;
      }
   }

   @Override
   public void draw(Texture texture, float x, float y, int srcX, int srcY, int srcWidth, int srcHeight) {
      if (!this.drawing) {
         throw new IllegalStateException("JrgssBatch.begin must be called before draw.");
      } else {
         float[] vertices = this.vertices;
         if (texture != this.lastTexture) {
            this.switchTexture(texture);
         } else if (this.idx == vertices.length) {
            this.flush();
         }

         float u = srcX * this.invTexWidth;
         float v = (srcY + srcHeight) * this.invTexHeight;
         float u2 = (srcX + srcWidth) * this.invTexWidth;
         float v2 = srcY * this.invTexHeight;
         float fx2 = x + srcWidth;
         float fy2 = y + srcHeight;
         float color = this.color;
         int idx = this.idx;
         vertices[idx] = x;
         vertices[idx + 1] = y;
         vertices[idx + 2] = color;
         vertices[idx + 3] = u;
         vertices[idx + 4] = v;
         vertices[idx + 5] = x;
         vertices[idx + 6] = fy2;
         vertices[idx + 7] = color;
         vertices[idx + 8] = u;
         vertices[idx + 9] = v2;
         vertices[idx + 10] = fx2;
         vertices[idx + 11] = fy2;
         vertices[idx + 12] = color;
         vertices[idx + 13] = u2;
         vertices[idx + 14] = v2;
         vertices[idx + 15] = fx2;
         vertices[idx + 16] = y;
         vertices[idx + 17] = color;
         vertices[idx + 18] = u2;
         vertices[idx + 19] = v;
         this.idx = idx + 20;
      }
   }

   @Override
   public void draw(Texture texture, float x, float y, float width, float height, float u, float v, float u2, float v2) {
      if (!this.drawing) {
         throw new IllegalStateException("JrgssBatch.begin must be called before draw.");
      } else {
         float[] vertices = this.vertices;
         if (texture != this.lastTexture) {
            this.switchTexture(texture);
         } else if (this.idx == vertices.length) {
            this.flush();
         }

         float fx2 = x + width;
         float fy2 = y + height;
         float color = this.color;
         int idx = this.idx;
         vertices[idx] = x;
         vertices[idx + 1] = y;
         vertices[idx + 2] = color;
         vertices[idx + 3] = u;
         vertices[idx + 4] = v;
         vertices[idx + 5] = x;
         vertices[idx + 6] = fy2;
         vertices[idx + 7] = color;
         vertices[idx + 8] = u;
         vertices[idx + 9] = v2;
         vertices[idx + 10] = fx2;
         vertices[idx + 11] = fy2;
         vertices[idx + 12] = color;
         vertices[idx + 13] = u2;
         vertices[idx + 14] = v2;
         vertices[idx + 15] = fx2;
         vertices[idx + 16] = y;
         vertices[idx + 17] = color;
         vertices[idx + 18] = u2;
         vertices[idx + 19] = v;
         this.idx = idx + 20;
      }
   }

   @Override
   public void draw(Texture texture, float x, float y) {
      this.draw(texture, x, y, (float)texture.getWidth(), (float)texture.getHeight());
   }

   @Override
   public void draw(Texture texture, float x, float y, float width, float height) {
      if (!this.drawing) {
         throw new IllegalStateException("JrgssBatch.begin must be called before draw.");
      } else {
         float[] vertices = this.vertices;
         if (texture != this.lastTexture) {
            this.switchTexture(texture);
         } else if (this.idx == vertices.length) {
            this.flush();
         }

         float fx2 = x + width;
         float fy2 = y + height;
         float u = 0.0F;
         float v = 1.0F;
         float u2 = 1.0F;
         float v2 = 0.0F;
         float color = this.color;
         int idx = this.idx;
         vertices[idx] = x;
         vertices[idx + 1] = y;
         vertices[idx + 2] = color;
         vertices[idx + 3] = 0.0F;
         vertices[idx + 4] = 1.0F;
         vertices[idx + 5] = x;
         vertices[idx + 6] = fy2;
         vertices[idx + 7] = color;
         vertices[idx + 8] = 0.0F;
         vertices[idx + 9] = 0.0F;
         vertices[idx + 10] = fx2;
         vertices[idx + 11] = fy2;
         vertices[idx + 12] = color;
         vertices[idx + 13] = 1.0F;
         vertices[idx + 14] = 0.0F;
         vertices[idx + 15] = fx2;
         vertices[idx + 16] = y;
         vertices[idx + 17] = color;
         vertices[idx + 18] = 1.0F;
         vertices[idx + 19] = 1.0F;
         this.idx = idx + 20;
      }
   }

   @Override
   public void draw(Texture texture, float[] spriteVertices, int offset, int count) {
      if (!this.drawing) {
         throw new IllegalStateException("JrgssBatch.begin must be called before draw.");
      } else {
         int verticesLength = this.vertices.length;
         int remainingVertices = verticesLength;
         if (texture != this.lastTexture) {
            this.switchTexture(texture);
         } else {
            remainingVertices = verticesLength - this.idx;
            if (remainingVertices == 0) {
               this.flush();
               remainingVertices = verticesLength;
            }
         }

         int copyCount = Math.min(remainingVertices, count);
         System.arraycopy(spriteVertices, offset, this.vertices, this.idx, copyCount);
         this.idx += copyCount;

         for (int var8 = count - copyCount; var8 > 0; var8 -= copyCount) {
            offset += copyCount;
            this.flush();
            copyCount = Math.min(verticesLength, var8);
            System.arraycopy(spriteVertices, offset, this.vertices, 0, copyCount);
            this.idx += copyCount;
         }
      }
   }

   @Override
   public void draw(TextureRegion region, float x, float y) {
      this.draw(region, x, y, (float)region.getRegionWidth(), (float)region.getRegionHeight());
   }

   @Override
   public void draw(TextureRegion region, float x, float y, float width, float height) {
      if (!this.drawing) {
         throw new IllegalStateException("JrgssBatch.begin must be called before draw.");
      } else {
         float[] vertices = this.vertices;
         Texture texture = region.texture;
         if (texture != this.lastTexture) {
            this.switchTexture(texture);
         } else if (this.idx == vertices.length) {
            this.flush();
         }

         float fx2 = x + width;
         float fy2 = y + height;
         float u = region.u;
         float v = region.v2;
         float u2 = region.u2;
         float v2 = region.v;
         float color = this.color;
         int idx = this.idx;
         vertices[idx] = x;
         vertices[idx + 1] = y;
         vertices[idx + 2] = color;
         vertices[idx + 3] = u;
         vertices[idx + 4] = v;
         vertices[idx + 5] = x;
         vertices[idx + 6] = fy2;
         vertices[idx + 7] = color;
         vertices[idx + 8] = u;
         vertices[idx + 9] = v2;
         vertices[idx + 10] = fx2;
         vertices[idx + 11] = fy2;
         vertices[idx + 12] = color;
         vertices[idx + 13] = u2;
         vertices[idx + 14] = v2;
         vertices[idx + 15] = fx2;
         vertices[idx + 16] = y;
         vertices[idx + 17] = color;
         vertices[idx + 18] = u2;
         vertices[idx + 19] = v;
         this.idx = idx + 20;
      }
   }

   @Override
   public void draw(TextureRegion region, float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation) {
      if (!this.drawing) {
         throw new IllegalStateException("JrgssBatch.begin must be called before draw.");
      } else {
         float[] vertices = this.vertices;
         Texture texture = region.texture;
         if (texture != this.lastTexture) {
            this.switchTexture(texture);
         } else if (this.idx == vertices.length) {
            this.flush();
         }

         float worldOriginX = x + originX;
         float worldOriginY = y + originY;
         float fx = -originX;
         float fy = -originY;
         float fx2 = width - originX;
         float fy2 = height - originY;
         if (scaleX != 1.0F || scaleY != 1.0F) {
            fx *= scaleX;
            fy *= scaleY;
            fx2 *= scaleX;
            fy2 *= scaleY;
         }

         float x1;
         float y1;
         float x2;
         float y2;
         float x3;
         float y3;
         float x4;
         float y4;
         if (rotation != 0.0F) {
            float cos = MathUtils.cosDeg(rotation);
            float sin = MathUtils.sinDeg(rotation);
            x1 = cos * fx - sin * fy;
            y1 = sin * fx + cos * fy;
            x2 = cos * fx - sin * fy2;
            y2 = sin * fx + cos * fy2;
            x3 = cos * fx2 - sin * fy2;
            y3 = sin * fx2 + cos * fy2;
            x4 = x1 + (x3 - x2);
            y4 = y3 - (y2 - y1);
         } else {
            x1 = fx;
            y1 = fy;
            x2 = fx;
            y2 = fy2;
            x3 = fx2;
            y3 = fy2;
            x4 = fx2;
            y4 = fy;
         }

         x1 += worldOriginX;
         y1 += worldOriginY;
         x2 += worldOriginX;
         y2 += worldOriginY;
         x3 += worldOriginX;
         y3 += worldOriginY;
         x4 += worldOriginX;
         y4 += worldOriginY;
         float u = region.u;
         float v = region.v2;
         float u2 = region.u2;
         float v2 = region.v;
         float color = this.color;
         int idx = this.idx;
         vertices[idx] = x1;
         vertices[idx + 1] = y1;
         vertices[idx + 2] = color;
         vertices[idx + 3] = u;
         vertices[idx + 4] = v;
         vertices[idx + 5] = x2;
         vertices[idx + 6] = y2;
         vertices[idx + 7] = color;
         vertices[idx + 8] = u;
         vertices[idx + 9] = v2;
         vertices[idx + 10] = x3;
         vertices[idx + 11] = y3;
         vertices[idx + 12] = color;
         vertices[idx + 13] = u2;
         vertices[idx + 14] = v2;
         vertices[idx + 15] = x4;
         vertices[idx + 16] = y4;
         vertices[idx + 17] = color;
         vertices[idx + 18] = u2;
         vertices[idx + 19] = v;
         this.idx = idx + 20;
      }
   }

   @Override
   public void draw(
      TextureRegion region,
      float x,
      float y,
      float originX,
      float originY,
      float width,
      float height,
      float scaleX,
      float scaleY,
      float rotation,
      boolean clockwise
   ) {
      if (!this.drawing) {
         throw new IllegalStateException("JrgssBatch.begin must be called before draw.");
      } else {
         float[] vertices = this.vertices;
         Texture texture = region.texture;
         if (texture != this.lastTexture) {
            this.switchTexture(texture);
         } else if (this.idx == vertices.length) {
            this.flush();
         }

         float worldOriginX = x + originX;
         float worldOriginY = y + originY;
         float fx = -originX;
         float fy = -originY;
         float fx2 = width - originX;
         float fy2 = height - originY;
         if (scaleX != 1.0F || scaleY != 1.0F) {
            fx *= scaleX;
            fy *= scaleY;
            fx2 *= scaleX;
            fy2 *= scaleY;
         }

         float x1;
         float y1;
         float x2;
         float y2;
         float x3;
         float y3;
         float x4;
         float y4;
         if (rotation != 0.0F) {
            float cos = MathUtils.cosDeg(rotation);
            float sin = MathUtils.sinDeg(rotation);
            x1 = cos * fx - sin * fy;
            y1 = sin * fx + cos * fy;
            x2 = cos * fx - sin * fy2;
            y2 = sin * fx + cos * fy2;
            x3 = cos * fx2 - sin * fy2;
            y3 = sin * fx2 + cos * fy2;
            x4 = x1 + (x3 - x2);
            y4 = y3 - (y2 - y1);
         } else {
            x1 = fx;
            y1 = fy;
            x2 = fx;
            y2 = fy2;
            x3 = fx2;
            y3 = fy2;
            x4 = fx2;
            y4 = fy;
         }

         x1 += worldOriginX;
         y1 += worldOriginY;
         x2 += worldOriginX;
         y2 += worldOriginY;
         x3 += worldOriginX;
         y3 += worldOriginY;
         x4 += worldOriginX;
         y4 += worldOriginY;
         float u2;
         float v2;
         float u3;
         float v3;
         float u4;
         float v4;
         float u1;
         float v1;
         if (clockwise) {
            u1 = region.u2;
            v1 = region.v2;
            u2 = region.u;
            v2 = region.v2;
            u3 = region.u;
            v3 = region.v;
            u4 = region.u2;
            v4 = region.v;
         } else {
            u1 = region.u;
            v1 = region.v;
            u2 = region.u2;
            v2 = region.v;
            u3 = region.u2;
            v3 = region.v2;
            u4 = region.u;
            v4 = region.v2;
         }

         float color = this.color;
         int idx = this.idx;
         vertices[idx] = x1;
         vertices[idx + 1] = y1;
         vertices[idx + 2] = color;
         vertices[idx + 3] = u1;
         vertices[idx + 4] = v1;
         vertices[idx + 5] = x2;
         vertices[idx + 6] = y2;
         vertices[idx + 7] = color;
         vertices[idx + 8] = u2;
         vertices[idx + 9] = v2;
         vertices[idx + 10] = x3;
         vertices[idx + 11] = y3;
         vertices[idx + 12] = color;
         vertices[idx + 13] = u3;
         vertices[idx + 14] = v3;
         vertices[idx + 15] = x4;
         vertices[idx + 16] = y4;
         vertices[idx + 17] = color;
         vertices[idx + 18] = u4;
         vertices[idx + 19] = v4;
         this.idx = idx + 20;
      }
   }

   @Override
   public void draw(TextureRegion region, float width, float height, Affine2 transform) {
      if (!this.drawing) {
         throw new IllegalStateException("JrgssBatch.begin must be called before draw.");
      } else {
         float[] vertices = this.vertices;
         Texture texture = region.texture;
         if (texture != this.lastTexture) {
            this.switchTexture(texture);
         } else if (this.idx == vertices.length) {
            this.flush();
         }

         float x1 = transform.m02;
         float y1 = transform.m12;
         float x2 = transform.m01 * height + transform.m02;
         float y2 = transform.m11 * height + transform.m12;
         float x3 = transform.m00 * width + transform.m01 * height + transform.m02;
         float y3 = transform.m10 * width + transform.m11 * height + transform.m12;
         float x4 = transform.m00 * width + transform.m02;
         float y4 = transform.m10 * width + transform.m12;
         float u = region.u;
         float v = region.v2;
         float u2 = region.u2;
         float v2 = region.v;
         float color = this.color;
         int idx = this.idx;
         vertices[idx] = x1;
         vertices[idx + 1] = y1;
         vertices[idx + 2] = color;
         vertices[idx + 3] = u;
         vertices[idx + 4] = v;
         vertices[idx + 5] = x2;
         vertices[idx + 6] = y2;
         vertices[idx + 7] = color;
         vertices[idx + 8] = u;
         vertices[idx + 9] = v2;
         vertices[idx + 10] = x3;
         vertices[idx + 11] = y3;
         vertices[idx + 12] = color;
         vertices[idx + 13] = u2;
         vertices[idx + 14] = v2;
         vertices[idx + 15] = x4;
         vertices[idx + 16] = y4;
         vertices[idx + 17] = color;
         vertices[idx + 18] = u2;
         vertices[idx + 19] = v;
         this.idx = idx + 20;
      }
   }

   @Override
   public void flush() {
      if (this.idx != 0) {
         this.renderCalls++;
         this.totalRenderCalls++;
         int spritesInBatch = this.idx / 20;
         if (spritesInBatch > this.maxSpritesInBatch) {
            this.maxSpritesInBatch = spritesInBatch;
         }

         int count = spritesInBatch * 6;
         this.lastTexture.bind();
         Mesh mesh = this.mesh;
         mesh.setVertices(this.vertices, 0, this.idx);
         ((Buffer)mesh.getIndicesBuffer()).position(0);
         ((Buffer)mesh.getIndicesBuffer()).limit(count);
         if (this.blendingDisabled) {
            Gdx.gl.glDisable(3042);
         } else {
            Gdx.gl.glEnable(3042);
            if (this.blendSrcFunc != -1) {
               Gdx.gl.glBlendFunc(this.blendSrcFunc, this.blendDstFunc);
            }

            Gdx.gl.glBlendEquationSeparate(this.rgbEquation, this.alphaEquation);
         }

         mesh.render(this.shader, 4, 0, count);
         this.idx = 0;
      }
   }

   @Override
   public void disableBlending() {
      if (!this.blendingDisabled) {
         this.flush();
         this.blendingDisabled = true;
      }
   }

   @Override
   public void enableBlending() {
      if (this.blendingDisabled) {
         this.flush();
         this.blendingDisabled = false;
      }
   }

   @Override
   public void setBlendFunction(int srcFunc, int dstFunc) {
      if (this.blendSrcFunc != srcFunc || this.blendDstFunc != dstFunc) {
         this.flush();
         this.blendSrcFunc = srcFunc;
         this.blendDstFunc = dstFunc;
      }
   }

    @Override
    public void setBlendFunctionSeparate(int srcFuncColor, int dstFuncColor, int srcFuncAlpha, int dstFuncAlpha) {

    }

    @Override
   public int getBlendSrcFunc() {
      return blendSrcFunc;
   }

   @Override
   public int getBlendDstFunc() {
      return blendDstFunc;
   }

    @Override
    public int getBlendSrcFuncAlpha() {
        return blendSrcFunc;
    }

    @Override
    public int getBlendDstFuncAlpha() {
        return blendDstFunc;
    }

    @Override
   public void dispose() {
      this.mesh.dispose();
   }

   @Override
   public Matrix4 getProjectionMatrix() {
      return this.projectionMatrix;
   }

   @Override
   public Matrix4 getTransformMatrix() {
      return this.transformMatrix;
   }

   @Override
   public void setProjectionMatrix(Matrix4 projection) {
      if (this.drawing) {
         this.flush();
      }

      this.projectionMatrix.set(projection);
      if (this.drawing) {
         this.setupMatrices();
      }
   }

   @Override
   public void setTransformMatrix(Matrix4 transform) {
      if (this.drawing) {
         this.flush();
      }

      this.transformMatrix.set(transform);
      if (this.drawing) {
         this.setupMatrices();
      }
   }

   private void setupMatrices() {
      this.combinedMatrix.set(this.projectionMatrix).mul(this.transformMatrix);
      this.shader.setUniformMatrix("u_projTrans", this.combinedMatrix);
      this.shader.setUniformi("u_texture", 0);
   }

   protected void switchTexture(Texture texture) {
      this.flush();
      this.lastTexture = texture;
      this.invTexWidth = 1.0F / texture.getWidth();
      this.invTexHeight = 1.0F / texture.getHeight();
   }

   @Override
   public void setShader(ShaderProgram shader) {
      if (shader != null && shader != AlphaBlendingShader.get()) {
         throw new UnsupportedOperationException("Cannot set shader on JrgssBatch");
      }
   }

   @Override
   public ShaderProgram getShader() {
      return this.shader;
   }

   @Override
   public boolean isBlendingEnabled() {
      return !this.blendingDisabled;
   }

   @Override
   public boolean isDrawing() {
      return this.drawing;
   }

   public void setBlendEquation(int rgbEquation, int alphaEquation) {
      if (rgbEquation != this.rgbEquation || alphaEquation != this.alphaEquation) {
         this.flush();
         this.rgbEquation = rgbEquation;
         this.alphaEquation = alphaEquation;
      }
   }
}
