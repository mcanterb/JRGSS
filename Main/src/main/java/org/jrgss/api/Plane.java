package org.jrgss.api;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import lombok.Data;
import lombok.ToString;

/**
 * Created by matty on 6/27/14.
 */
@Data
@ToString(callSuper = true)
public class Plane extends Sprite {


    public Plane() {
        super(new Viewport());
    }

    public Plane(Viewport viewport) {
        super(viewport);
    }

    @Override
    public void render(SpriteBatch _) {
        if (bitmap != null && visible && opacity > 0 && (viewport == null || viewport.isVisible())) {
            //Gdx.app.log("Sprite", String.format("Rendering: %s, %d, %d, %d, %d", viewport, x, y, ox, oy));

            batch.enableBlending();
            if(viewport != null) viewport.begin(batch);
            int viewportX = viewport == null?0:(viewport.rect.x - viewport.ox);
            int viewportY = viewport == null?0:(viewport.rect.y - viewport.oy);
            batch.setColor(1f, 1f, 1f, (opacity / 255f));
            getAlphaBlendingShader().begin();
            getAlphaBlendingShader().setUniformf("blend_color", color.getRed()/255f, color.getGreen()/255f, color.getBlue()/255f, (color.getAlpha()/255f));
            getAlphaBlendingShader().setUniformi("blend_mode", blend_type);
            batch.begin();
            bitmap.render(batch, x - ox + viewportX, y - oy + viewportY, src_rect);


            batch.end();
            getAlphaBlendingShader().end();
            if(viewport != null) viewport.end();
        }
    }

}
