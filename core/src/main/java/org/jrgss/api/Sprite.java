package org.jrgss.api;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.g2d.JrgssBatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import org.jrgss.shaders.AlphaBlendingShader;

public class Sprite extends AbstractRenderable {
   public static Texture colorTexture;
   public static ShaderProgram alphaBlendingShader = null;
   Bitmap bitmap;
   Rect src_rect = new Rect();
   Viewport viewport;
   boolean visible = true;
   int x;
   int y;
   int z;
   int ox;
   int oy;
   double zoom_x = 1.0;
   double zoom_y = 1.0;
   double angle;
   double wave_amp;
   double wave_length;
   double wave_speed;
   double wave_phase;
   boolean mirror;
   double bush_depth;
   double bush_opacity = 128.0;
   int opacity = 255;
   int blend_type;
   Color color = new Color(0, 0, 0, 0);
   Tone tone = new Tone();
   private static SpriteBatch batch;

   public Sprite() {
   }

   public Sprite(Viewport viewport) {
      this.viewport = viewport;
   }

   public static Texture getColorTexture() {
      if (colorTexture == null) {
         Pixmap p = new Pixmap(1, 1, Format.RGBA8888);
         p.setColor(1.0F, 1.0F, 1.0F, 1.0F);
         p.fill();
         colorTexture = new Texture(p);
         p.dispose();
      }

      return colorTexture;
   }

   @Override
   public void dispose() {
      super.dispose();
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
   public void render(JrgssBatch batch) {
      if (this.isDisposed()) {
         Gdx.app.log("Sprite", "Disposed, but still drawing! ");
      }

      if (this.bitmap != null && this.visible && this.opacity > 0 && (this.viewport == null || this.viewport.isVisible() && !this.viewport.isDisposed())) {
         Viewport.begin(this.viewport, batch);
         int viewportX = this.viewport == null ? 0 : this.viewport.rect.x - this.viewport.ox;
         int viewportY = this.viewport == null ? 0 : this.viewport.rect.y - this.viewport.oy;
         batch.setColor(1.0F, 1.0F, 1.0F, this.opacity / 255.0F);
         AlphaBlendingShader.setTone(this.tone, batch);
         AlphaBlendingShader.setBlendColor(this.color, batch);
         boolean reset = false;
         switch (this.blend_type) {
            case 0:
               batch.setBlendEquation(32774, 32774);
               batch.setBlendFunction(770, 771);
               break;
            case 1:
               batch.setBlendEquation(32774, 32774);
               batch.setBlendFunction(770, 1);
               break;
            case 2:
               batch.setBlendFunction(770, 1);
               batch.setBlendEquation(32779, 32774);
               reset = true;
         }

         int bush_depth = (int)Math.min(this.bush_depth, (double)this.src_rect.getHeight());
         Rect top = new Rect(this.src_rect.x, this.src_rect.y, this.src_rect.getWidth(), this.src_rect.getHeight() - bush_depth);
         Rect bottom = new Rect(this.src_rect.x, this.src_rect.y + this.src_rect.getHeight() - bush_depth, this.src_rect.getWidth(), bush_depth);
         this.bitmap
            .render(
               batch,
               this.x - (int)(this.ox * this.zoom_x) + viewportX,
               this.y - (int)(this.oy * this.zoom_y) + viewportY,
               (int)(top.getWidth() * this.zoom_x),
               (int)(top.getHeight() * this.zoom_y),
               top
            );
         batch.setColor(1.0F, 1.0F, 1.0F, this.opacity / 255.0F * ((int)this.bush_opacity / 255.0F));
         this.bitmap
            .render(
               batch,
               this.x - (int)(this.ox * this.zoom_x) + viewportX,
               this.y - (int)(this.oy * this.zoom_y) + viewportY + (int)(top.getHeight() * this.zoom_y),
               (int)(bottom.getWidth() * this.zoom_x),
               (int)(bottom.getHeight() * this.zoom_y),
               bottom
            );
         if (reset) {
            batch.setBlendFunction(770, 771);
            batch.setBlendEquation(32774, 32774);
         }
      }
   }

   public void setBitmap(Bitmap b) {
      this.bitmap = b;
      if (b != null) {
         this.src_rect.set(0, 0, b.getWidth(), b.getHeight());
      }
   }

   public void update() {
   }

   public void flash(Color color, int duration) {
      Gdx.app.log("Sprite", "Flash!");
   }

   public int getWidth() {
      return this.src_rect.getWidth();
   }

   public int getHeight() {
      return this.src_rect.getHeight();
   }

   public void setOpacity(int opacity) {
      this.opacity = Math.max(0, Math.min(255, opacity));
   }

   public Bitmap getBitmap() {
      return this.bitmap;
   }

   public Rect getSrc_rect() {
      return this.src_rect;
   }

   @Override
   public Viewport getViewport() {
      return this.viewport;
   }

   public boolean isVisible() {
      return this.visible;
   }

   public int getX() {
      return this.x;
   }

   @Override
   public int getY() {
      return this.y;
   }

   @Override
   public int getZ() {
      return this.z;
   }

   public int getOx() {
      return this.ox;
   }

   public int getOy() {
      return this.oy;
   }

   public double getZoom_x() {
      return this.zoom_x;
   }

   public double getZoom_y() {
      return this.zoom_y;
   }

   public double getAngle() {
      return this.angle;
   }

   public double getWave_amp() {
      return this.wave_amp;
   }

   public double getWave_length() {
      return this.wave_length;
   }

   public double getWave_speed() {
      return this.wave_speed;
   }

   public double getWave_phase() {
      return this.wave_phase;
   }

   public boolean isMirror() {
      return this.mirror;
   }

   public double getBush_depth() {
      return this.bush_depth;
   }

   public double getBush_opacity() {
      return this.bush_opacity;
   }

   public int getOpacity() {
      return this.opacity;
   }

   public int getBlend_type() {
      return this.blend_type;
   }

   public Color getColor() {
      return this.color;
   }

   public Tone getTone() {
      return this.tone;
   }

   public void setSrc_rect(Rect src_rect) {
      this.src_rect = src_rect;
   }

   public void setViewport(Viewport viewport) {
      this.viewport = viewport;
   }

   public void setVisible(boolean visible) {
      this.visible = visible;
   }

   public void setZ(int z) {
      this.z = z;
   }

   public void setZoom_x(double zoom_x) {
      this.zoom_x = zoom_x;
   }

   public void setZoom_y(double zoom_y) {
      this.zoom_y = zoom_y;
   }

   public void setAngle(double angle) {
      this.angle = angle;
   }

   public void setWave_amp(double wave_amp) {
      this.wave_amp = wave_amp;
   }

   public void setWave_length(double wave_length) {
      this.wave_length = wave_length;
   }

   public void setWave_speed(double wave_speed) {
      this.wave_speed = wave_speed;
   }

   public void setWave_phase(double wave_phase) {
      this.wave_phase = wave_phase;
   }

   public void setMirror(boolean mirror) {
      this.mirror = mirror;
   }

   public void setBush_depth(double bush_depth) {
      this.bush_depth = bush_depth;
   }

   public void setBush_opacity(double bush_opacity) {
      this.bush_opacity = bush_opacity;
   }

   public void setBlend_type(int blend_type) {
      this.blend_type = blend_type;
   }

   public void setColor(Color color) {
      this.color = color;
   }

   public void setTone(Tone tone) {
      this.tone = tone;
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof Sprite)) {
         return false;
      } else {
         Sprite other = (Sprite)o;
         if (!other.canEqual(this)) {
            return false;
         } else {
            Object this$bitmap = this.getBitmap();
            Object other$bitmap = other.getBitmap();
            if (this$bitmap == null ? other$bitmap == null : this$bitmap.equals(other$bitmap)) {
               Object this$src_rect = this.getSrc_rect();
               Object other$src_rect = other.getSrc_rect();
               if (this$src_rect == null ? other$src_rect == null : this$src_rect.equals(other$src_rect)) {
                  Object this$viewport = this.getViewport();
                  Object other$viewport = other.getViewport();
                  if (this$viewport == null ? other$viewport == null : this$viewport.equals(other$viewport)) {
                     if (this.isVisible() != other.isVisible()) {
                        return false;
                     } else if (this.getX() != other.getX()) {
                        return false;
                     } else if (this.getY() != other.getY()) {
                        return false;
                     } else if (this.getZ() != other.getZ()) {
                        return false;
                     } else if (this.getOx() != other.getOx()) {
                        return false;
                     } else if (this.getOy() != other.getOy()) {
                        return false;
                     } else if (Double.compare(this.getZoom_x(), other.getZoom_x()) != 0) {
                        return false;
                     } else if (Double.compare(this.getZoom_y(), other.getZoom_y()) != 0) {
                        return false;
                     } else if (Double.compare(this.getAngle(), other.getAngle()) != 0) {
                        return false;
                     } else if (Double.compare(this.getWave_amp(), other.getWave_amp()) != 0) {
                        return false;
                     } else if (Double.compare(this.getWave_length(), other.getWave_length()) != 0) {
                        return false;
                     } else if (Double.compare(this.getWave_speed(), other.getWave_speed()) != 0) {
                        return false;
                     } else if (Double.compare(this.getWave_phase(), other.getWave_phase()) != 0) {
                        return false;
                     } else if (this.isMirror() != other.isMirror()) {
                        return false;
                     } else if (Double.compare(this.getBush_depth(), other.getBush_depth()) != 0) {
                        return false;
                     } else if (Double.compare(this.getBush_opacity(), other.getBush_opacity()) != 0) {
                        return false;
                     } else if (this.getOpacity() != other.getOpacity()) {
                        return false;
                     } else if (this.getBlend_type() != other.getBlend_type()) {
                        return false;
                     } else {
                        Object this$color = this.getColor();
                        Object other$color = other.getColor();
                        if (this$color == null ? other$color == null : this$color.equals(other$color)) {
                           Object this$tone = this.getTone();
                           Object other$tone = other.getTone();
                           return this$tone == null ? other$tone == null : this$tone.equals(other$tone);
                        } else {
                           return false;
                        }
                     }
                  } else {
                     return false;
                  }
               } else {
                  return false;
               }
            } else {
               return false;
            }
         }
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof Sprite;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = 1;
      Object $bitmap = this.getBitmap();
      result = result * 59 + ($bitmap == null ? 43 : $bitmap.hashCode());
      Object $src_rect = this.getSrc_rect();
      result = result * 59 + ($src_rect == null ? 43 : $src_rect.hashCode());
      Object $viewport = this.getViewport();
      result = result * 59 + ($viewport == null ? 43 : $viewport.hashCode());
      result = result * 59 + (this.isVisible() ? 79 : 97);
      result = result * 59 + this.getX();
      result = result * 59 + this.getY();
      result = result * 59 + this.getZ();
      result = result * 59 + this.getOx();
      result = result * 59 + this.getOy();
      long $zoom_x = Double.doubleToLongBits(this.getZoom_x());
      result = result * 59 + (int)($zoom_x >>> 32 ^ $zoom_x);
      long $zoom_y = Double.doubleToLongBits(this.getZoom_y());
      result = result * 59 + (int)($zoom_y >>> 32 ^ $zoom_y);
      long $angle = Double.doubleToLongBits(this.getAngle());
      result = result * 59 + (int)($angle >>> 32 ^ $angle);
      long $wave_amp = Double.doubleToLongBits(this.getWave_amp());
      result = result * 59 + (int)($wave_amp >>> 32 ^ $wave_amp);
      long $wave_length = Double.doubleToLongBits(this.getWave_length());
      result = result * 59 + (int)($wave_length >>> 32 ^ $wave_length);
      long $wave_speed = Double.doubleToLongBits(this.getWave_speed());
      result = result * 59 + (int)($wave_speed >>> 32 ^ $wave_speed);
      long $wave_phase = Double.doubleToLongBits(this.getWave_phase());
      result = result * 59 + (int)($wave_phase >>> 32 ^ $wave_phase);
      result = result * 59 + (this.isMirror() ? 79 : 97);
      long $bush_depth = Double.doubleToLongBits(this.getBush_depth());
      result = result * 59 + (int)($bush_depth >>> 32 ^ $bush_depth);
      long $bush_opacity = Double.doubleToLongBits(this.getBush_opacity());
      result = result * 59 + (int)($bush_opacity >>> 32 ^ $bush_opacity);
      result = result * 59 + this.getOpacity();
      result = result * 59 + this.getBlend_type();
      Object $color = this.getColor();
      result = result * 59 + ($color == null ? 43 : $color.hashCode());
      Object $tone = this.getTone();
      return result * 59 + ($tone == null ? 43 : $tone.hashCode());
   }

   @Override
   public String toString() {
      return "Sprite(super="
         + super.toString()
         + ", bitmap="
         + this.getBitmap()
         + ", src_rect="
         + this.getSrc_rect()
         + ", viewport="
         + this.getViewport()
         + ", visible="
         + this.isVisible()
         + ", x="
         + this.getX()
         + ", y="
         + this.getY()
         + ", z="
         + this.getZ()
         + ", ox="
         + this.getOx()
         + ", oy="
         + this.getOy()
         + ", zoom_x="
         + this.getZoom_x()
         + ", zoom_y="
         + this.getZoom_y()
         + ", angle="
         + this.getAngle()
         + ", wave_amp="
         + this.getWave_amp()
         + ", wave_length="
         + this.getWave_length()
         + ", wave_speed="
         + this.getWave_speed()
         + ", wave_phase="
         + this.getWave_phase()
         + ", mirror="
         + this.isMirror()
         + ", bush_depth="
         + this.getBush_depth()
         + ", bush_opacity="
         + this.getBush_opacity()
         + ", opacity="
         + this.getOpacity()
         + ", blend_type="
         + this.getBlend_type()
         + ", color="
         + this.getColor()
         + ", tone="
         + this.getTone()
         + ")";
   }
}
