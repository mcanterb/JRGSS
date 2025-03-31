package org.jrgss.api;

import com.badlogic.gdx.graphics.g2d.JrgssBatch;
import org.jrgss.shaders.AlphaBlendingShader;

public class Plane extends Sprite {
   public Plane() {
   }

   public Plane(Viewport viewport) {
      super(viewport);
   }

   @Override
   public void render(JrgssBatch batch) {
      if (this.bitmap != null && this.visible && this.opacity > 0 && (this.viewport == null || this.viewport.isVisible())) {
         Viewport.begin(this.viewport, batch);
         int viewportX = this.viewport == null ? 0 : this.viewport.rect.x - this.viewport.ox;
         int viewportY = this.viewport == null ? 0 : this.viewport.rect.y - this.viewport.oy;
         int viewportWidth = this.viewport == null ? Graphics.getWidth() : this.viewport.rect.getWidth();
         int viewportHeight = this.viewport == null ? Graphics.getHeight() : this.viewport.rect.getHeight();
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
               batch.setBlendFunction(32779, 32774);
               reset = true;
         }

         int tileWidth = (int)(this.src_rect.getWidth() * this.zoom_x);
         int tileHeight = (int)(this.src_rect.getHeight() * this.zoom_y);
         int startX = viewportX - this.ox;
         int startY = viewportY - this.oy;

         for (int x = startX; x < viewportWidth + viewportX; x += tileWidth) {
            for (int y = startY; y < viewportHeight + viewportY; y += tileHeight) {
               this.bitmap.render(batch, x, y, (int)(this.src_rect.getWidth() * this.zoom_x), (int)(this.src_rect.getHeight() * this.zoom_y), this.src_rect);
            }
         }

         for (int x = startX; x > -tileWidth; x -= tileWidth) {
            for (int y = startY; y > -tileHeight; y -= tileHeight) {
               if (x != startX || y != startY) {
                  this.bitmap.render(batch, x, y, (int)(this.src_rect.getWidth() * this.zoom_x), (int)(this.src_rect.getHeight() * this.zoom_y), this.src_rect);
               }
            }
         }

         if (reset) {
            batch.setBlendEquation(32774, 32774);
            batch.setBlendFunction(770, 771);
         }
      }
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof Plane)) {
         return false;
      } else {
         Plane other = (Plane)o;
         return other.canEqual(this);
      }
   }

   @Override
   protected boolean canEqual(Object other) {
      return other instanceof Plane;
   }

   @Override
   public int hashCode() {
      return 1;
   }

   @Override
   public String toString() {
      return "Plane(super=" + super.toString() + ")";
   }
}
