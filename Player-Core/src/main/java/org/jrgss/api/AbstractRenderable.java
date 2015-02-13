package org.jrgss.api;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import lombok.Getter;
import lombok.ToString;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * @author matt
 * @date 7/2/14
 */
@ToString
public abstract class AbstractRenderable implements Comparable<AbstractRenderable>, Renderable {


    public static final Map<AbstractRenderable, AbstractRenderable> renderQueue = new IdentityHashMap<>();
    private static long counter = 0;
    @Getter
    protected long creationTime;
    @Getter
    protected boolean disposed = false;

    public AbstractRenderable() {
        synchronized (AbstractRenderable.class) {
            creationTime = counter;
            counter = counter + 1;
            addToRenderQueue();
        }
    }

    protected synchronized void addToRenderQueue() {
        if(this instanceof Window) {
            Gdx.app.log("AbstractRenderable","Adding Window to queue: "+toString());
        }
        renderQueue.put(this, this);
    }

    public synchronized void dispose() {
        renderQueue.remove(this);
        this.disposed = true;
    }

    public abstract void render(SpriteBatch batch);

    @Override
    public int compareTo(AbstractRenderable other) {
        Viewport otherVP = other.getViewport();
        Viewport vp = getViewport();


        int ret = Integer.compare(vp.getZ(), otherVP.getZ());
        if(ret != 0) return ret;

        ret = Integer.compare(getZ(), other.getZ());
        if(ret != 0) return ret;
        ret = Integer.compare(getY(), other.getY());
        if(ret != 0) return ret;
        ret = Long.compare(getCreationTime(), other.getCreationTime());
        return ret;
    }

    public long getOrder() {
        Viewport vp = getViewport();

        if(vp == null) {
            return getZ()*1000000000000000L + getZ()*1000000000000L + getY()*1000000000L + getCreationTime();
        }
        return vp.getZ()*1000000000000000L + getZ()*1000000000000L + getY()*1000000000L + getCreationTime();


    }

    public abstract int getZ();

    public abstract Viewport getViewport();

    public abstract int getY();


}
