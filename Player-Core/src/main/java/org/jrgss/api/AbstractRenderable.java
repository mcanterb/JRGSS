package org.jrgss.api;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import lombok.Getter;
import lombok.ToString;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * @author matt
 * @date 7/2/14
 */
@ToString
public abstract class AbstractRenderable implements Comparable<AbstractRenderable>, Renderable {


    public static final Map<Long, WeakReference<AbstractRenderable>> renderQueue = new HashMap<>();
    private static long counter = 0;
    @Getter
    protected long creationTime;
    @Getter
    protected boolean disposed = false;

    private static final ReferenceQueue<AbstractRenderable> referenceQueue = new ReferenceQueue<>();

    private static class RenderableReference extends WeakReference<AbstractRenderable> {

        private final long id;

        public RenderableReference(AbstractRenderable referent, ReferenceQueue<? super AbstractRenderable> q) {
            super(referent, q);
            id = referent.getCreationTime();
        }
    }

    public AbstractRenderable() {
        synchronized (AbstractRenderable.class) {
            creationTime = counter;
            counter = counter + 1;
            addToRenderQueue();
        }
    }

    public static synchronized void cleanup() {
        RenderableReference reference;
        while((reference = (RenderableReference)referenceQueue.poll()) != null) {
            AbstractRenderable renderable = reference.get();
            if(renderable != null) {
                renderable.dispose();
                Gdx.app.log("AbstractRenderable", "Disposing of " + reference.id+". Was able to clean up!");
            }
            else if(renderQueue.containsKey(reference.id)) {
                Gdx.app.log("AbstractRenderable", "Disposing of " + reference.id);
                renderQueue.remove(reference.id);
            }
        }
    }

    protected synchronized void addToRenderQueue() {
        if(this instanceof Window) {
            Gdx.app.log("AbstractRenderable","Adding Window to queue: "+toString());
        }
        WeakReference<AbstractRenderable> renderableWeakReference = new RenderableReference(this, referenceQueue);
        renderQueue.put(creationTime, renderableWeakReference);
    }

    public synchronized void dispose() {
        renderQueue.remove(creationTime);
        this.disposed = true;
    }

    public abstract void render(SpriteBatch batch);

    @Override
    public int compareTo(AbstractRenderable other) {
        AbstractRenderable o1 = this;
        AbstractRenderable o2 = other;
        if(other == this) return 0;
        if(o1 instanceof Viewport && o1 == o2.getViewport()) {
            return 1;
        }
        if(o2 instanceof Viewport && o2 == o1.getViewport()) {
            return -1;
        }

        if(o1.getViewport() != o2.getViewport()) {
            o1 = o1.getViewport() == null ? o1 : o1.getViewport();
            o2 = o2.getViewport() == null ? o2 : o2.getViewport();
        }



        int ret = Integer.compare(o1.getZ(), o2.getZ());
        if(ret != 0) return ret;
        ret = Integer.compare(o1.getY(), o2.getY());
        if(ret != 0) return ret;
        ret = Long.compare(o1.getCreationTime(), other.getCreationTime());
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
