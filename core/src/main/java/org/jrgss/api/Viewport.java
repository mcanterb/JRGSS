package org.jrgss.api;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.JrgssBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Rectangle;
import java.util.concurrent.atomic.AtomicReference;
import org.jrgss.JRGSSGame;
import org.jrgss.Scissors;
import org.jrgss.shaders.AlphaBlendingShader;

public class Viewport extends AbstractRenderable {
   Rect rect = new Rect();
   boolean visible = true;
   int z = 0;
   int ox;
   int oy;
   Color color = new Color();
   Tone tone = new Tone(0.0F, 0.0F, 0.0F, 0.0F);
   Color defaultBlend = new Color(0, 0, 0, 0);
   int flashCounter = 0;
   Color flashColor = null;
   static AtomicReference<FrameBuffer> tempBuffer = new AtomicReference<>();
   static Viewport lastViewport = null;
   static boolean redraw = true;

   public Viewport() {
      this.rect.set(0, 0, Graphics.getWidth(), Graphics.getHeight());
   }

   public Viewport(Rect rect) {
      this.rect = rect;
   }

   public Viewport(int x, int y, int width, int height) {
      this.rect.set(x, y, width, height);
   }

   public static void reset() {
      lastViewport = null;
      Scissors.clearScissors();
   }

   public boolean isFullScreen() {
      int maxY = this.rect.getHeight() - this.rect.getY();
      int maxX = this.rect.getWidth() - this.rect.getX();
      return this.rect.getX() <= 0 && this.rect.getY() <= 0 && maxX > Graphics.getWidth() && maxY > Graphics.getHeight();
   }

   public static void begin(Viewport viewport, Batch batch) {
      if (viewport != lastViewport) {
         batch.flush();
         Scissors.clearScissors();
         lastViewport = viewport;
         if (viewport != null) {
            viewport.set(batch);
         }
      }
   }

   private void set(Batch batch) {
      Rectangle scissors = new Rectangle();
      Rectangle clipBounds = new Rectangle(this.rect.getX(), this.rect.getY(), this.rect.getWidth(), this.rect.getHeight());
      Scissors.calculateScissors(JRGSSGame.camera, batch.getTransformMatrix(), clipBounds, scissors);
      Scissors.pushScissors(scissors);
   }

   @Override
   public void render(JrgssBatch batch) {
      batch.begin();
      if (this.color != null && this.color.getAlpha() != 0) {
         batch.setColor(this.color.getRed() / 255.0F, this.color.getGreen() / 255.0F, this.color.getBlue() / 255.0F, this.color.getAlpha() / 255.0F);
         batch.draw(Sprite.getColorTexture(), (float)this.rect.getX(), (float)this.rect.getY(), (float)this.rect.getWidth(), (float)this.rect.getHeight());
      }

      if (this.flashColor != null && this.flashColor.getAlpha() != 0) {
         batch.setColor(
            this.flashColor.getRed() / 255.0F, this.flashColor.getGreen() / 255.0F, this.flashColor.getBlue() / 255.0F, this.flashColor.getAlpha() / 255.0F
         );
         batch.draw(Sprite.getColorTexture(), (float)this.rect.getX(), (float)this.rect.getY(), (float)this.rect.getWidth(), (float)this.rect.getHeight());
      }

      if (!this.tone.isZero()) {
         tempBuffer.set(Graphics.checkBufferSize(tempBuffer.get()));
         batch.flush();
         Graphics.tempBuffer.end();
         tempBuffer.get().begin();
         Gdx.gl20.glClear(16384);
         batch.setColor(1.0F, 1.0F, 1.0F, 1.0F);
         AlphaBlendingShader.setTone(this.tone, batch);
         AlphaBlendingShader.setBlendColor(this.defaultBlend, batch);
         batch.draw(Graphics.tempBuffer.getColorBufferTexture(), 0.0F, 0.0F, (float)Graphics.getWidth(), (float)Graphics.getHeight());
         batch.flush();
         tempBuffer.get().end();
         Graphics.tempBuffer.begin();
         Gdx.gl20.glClear(16384);
         batch.setColor(1.0F, 1.0F, 1.0F, 1.0F);
         AlphaBlendingShader.setTone(new Tone(0.0F, 0.0F, 0.0F, 0.0F), batch);
         AlphaBlendingShader.setBlendColor(this.defaultBlend, batch);
         batch.draw(tempBuffer.get().getColorBufferTexture(), 0.0F, 0.0F, (float)Graphics.getWidth(), (float)Graphics.getHeight());
         batch.flush();
      }

      batch.end();
   }

   public void update() {
      if (this.flashCounter > 0) {
         this.flashCounter--;
         if (this.flashCounter == 0) {
            this.flashColor = null;
         }
      }
   }

   public void flash(Color color, int duration) {
      Gdx.app.log("Viewport", String.format("Flash %s for %d", color, duration));
      this.flashColor = color;
      this.flashCounter = duration;
   }

   @Override
   public Viewport getViewport() {
      return null;
   }

   @Override
   public int getY() {
      return this.getRect().getY();
   }

   public void setTone(Tone tone) {
      this.tone = tone;
   }

   @Override
   public void dispose() {
      super.dispose();
   }

   public Rect getRect() {
      return this.rect;
   }

   public boolean isVisible() {
      return this.visible;
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

   public Color getColor() {
      return this.color;
   }

   public Tone getTone() {
      return this.tone;
   }

   public Color getDefaultBlend() {
      return this.defaultBlend;
   }

   public int getFlashCounter() {
      return this.flashCounter;
   }

   public Color getFlashColor() {
      return this.flashColor;
   }

   public void setRect(Rect rect) {
      this.rect = rect;
   }

   public void setVisible(boolean visible) {
      this.visible = visible;
   }

   public void setZ(int z) {
      this.z = z;
   }

   public void setOx(int ox) {
      this.ox = ox;
   }

   public void setOy(int oy) {
      this.oy = oy;
   }

   public void setColor(Color color) {
      this.color = color;
   }

   public void setDefaultBlend(Color defaultBlend) {
      this.defaultBlend = defaultBlend;
   }

   public void setFlashCounter(int flashCounter) {
      this.flashCounter = flashCounter;
   }

   public void setFlashColor(Color flashColor) {
      this.flashColor = flashColor;
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof Viewport)) {
         return false;
      } else {
         Viewport other = (Viewport)o;
         if (!other.canEqual(this)) {
            return false;
         } else {
            Object this$rect = this.getRect();
            Object other$rect = other.getRect();
            if (this$rect == null ? other$rect == null : this$rect.equals(other$rect)) {
               if (this.isVisible() != other.isVisible()) {
                  return false;
               } else if (this.getZ() != other.getZ()) {
                  return false;
               } else if (this.getOx() != other.getOx()) {
                  return false;
               } else if (this.getOy() != other.getOy()) {
                  return false;
               } else {
                  Object this$color = this.getColor();
                  Object other$color = other.getColor();
                  if (this$color == null ? other$color == null : this$color.equals(other$color)) {
                     Object this$tone = this.getTone();
                     Object other$tone = other.getTone();
                     if (this$tone == null ? other$tone == null : this$tone.equals(other$tone)) {
                        Object this$defaultBlend = this.getDefaultBlend();
                        Object other$defaultBlend = other.getDefaultBlend();
                        if (this$defaultBlend == null ? other$defaultBlend == null : this$defaultBlend.equals(other$defaultBlend)) {
                           if (this.getFlashCounter() != other.getFlashCounter()) {
                              return false;
                           } else {
                              Object this$flashColor = this.getFlashColor();
                              Object other$flashColor = other.getFlashColor();
                              return this$flashColor == null ? other$flashColor == null : this$flashColor.equals(other$flashColor);
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
            } else {
               return false;
            }
         }
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof Viewport;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = 1;
      Object $rect = this.getRect();
      result = result * 59 + ($rect == null ? 43 : $rect.hashCode());
      result = result * 59 + (this.isVisible() ? 79 : 97);
      result = result * 59 + this.getZ();
      result = result * 59 + this.getOx();
      result = result * 59 + this.getOy();
      Object $color = this.getColor();
      result = result * 59 + ($color == null ? 43 : $color.hashCode());
      Object $tone = this.getTone();
      result = result * 59 + ($tone == null ? 43 : $tone.hashCode());
      Object $defaultBlend = this.getDefaultBlend();
      result = result * 59 + ($defaultBlend == null ? 43 : $defaultBlend.hashCode());
      result = result * 59 + this.getFlashCounter();
      Object $flashColor = this.getFlashColor();
      return result * 59 + ($flashColor == null ? 43 : $flashColor.hashCode());
   }

   @Override
   public String toString() {
      return "Viewport(rect="
         + this.getRect()
         + ", visible="
         + this.isVisible()
         + ", z="
         + this.getZ()
         + ", ox="
         + this.getOx()
         + ", oy="
         + this.getOy()
         + ", color="
         + this.getColor()
         + ", tone="
         + this.getTone()
         + ", defaultBlend="
         + this.getDefaultBlend()
         + ", flashCounter="
         + this.getFlashCounter()
         + ", flashColor="
         + this.getFlashColor()
         + ")";
   }
}
