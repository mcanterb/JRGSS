package org.jrgss.api;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.JrgssBatch;
import com.badlogic.gdx.utils.Disposable;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractRenderable implements Comparable<AbstractRenderable> {
   public static final Map<Long, WeakReference<AbstractRenderable>> renderQueue = new HashMap<>();
   private static long counter = 0L;
   protected long creationTime;
   protected boolean disposed = false;
   private static final ReferenceQueue<AbstractRenderable> referenceQueue = new ReferenceQueue<>();
   private static Thread cleanupThread;

   public AbstractRenderable() {
      synchronized (AbstractRenderable.class) {
         this.creationTime = counter++;
         this.addToRenderQueue();
      }
   }

   public static void startCleanupThread() {
      cleanupThread = new Thread(() -> {
         while (true) {
            try {
               Thread.sleep(100L);
            } catch (InterruptedException var1) {
            }

            cleanup();
         }
      });
      cleanupThread.setDaemon(true);
      cleanupThread.start();
   }

   private static void cleanup() {
      AbstractRenderable.RenderableReference reference;
      while ((reference = (AbstractRenderable.RenderableReference)referenceQueue.poll()) != null) {
         AbstractRenderable renderable = reference.get();
         if (renderable != null) {
            renderable.dispose();
            Gdx.app.log("AbstractRenderable", "Disposing of " + reference.id + ". Was able to clean up!");
         } else if (renderQueue.containsKey(reference.id)) {
            Gdx.app.log("AbstractRenderable", "Disposing of " + reference.id);
            synchronized (AbstractRenderable.class) {
               renderQueue.remove(reference.id);

               for (Disposable disposable : reference.disposables) {
                  disposable.dispose();
               }
            }
         }

         Gdx.app.log("AbstractRenderable", "Got a renderable " + reference.id);
      }

      Bitmap.cleanup();
   }

   protected synchronized void addToRenderQueue() {
      WeakReference<AbstractRenderable> renderableWeakReference = new AbstractRenderable.RenderableReference(this, referenceQueue);
      renderQueue.put(this.creationTime, renderableWeakReference);
   }

   public synchronized void dispose() {
      renderQueue.remove(this.creationTime);
      this.disposed = true;
   }

   public abstract void render(JrgssBatch var1);

   public int compareTo(AbstractRenderable other) {
      AbstractRenderable o1 = this;
      AbstractRenderable o2 = other;
      if (other == this) {
         return 0;
      } else if (this instanceof Viewport && this == other.getViewport()) {
         return 1;
      } else if (other instanceof Viewport && other == this.getViewport()) {
         return -1;
      } else {
         if (this.getViewport() != other.getViewport()) {
            o1 = (AbstractRenderable)(this.getViewport() == null ? this : this.getViewport());
            o2 = (AbstractRenderable)(other.getViewport() == null ? other : other.getViewport());
         }

         int ret = Integer.compare(o1.getZ(), o2.getZ());
         if (ret != 0) {
            return ret;
         } else {
            ret = Integer.compare(o1.getY(), o2.getY());
            return ret != 0 ? ret : Long.compare(o1.getCreationTime(), o2.getCreationTime());
         }
      }
   }

   public List<Disposable> getDisposables() {
      return Collections.emptyList();
   }

   public abstract int getZ();

   public abstract Viewport getViewport();

   public abstract int getY();

   @Override
   public String toString() {
      return "AbstractRenderable(creationTime=" + this.getCreationTime() + ", disposed=" + this.isDisposed() + ")";
   }

   public long getCreationTime() {
      return this.creationTime;
   }

   public boolean isDisposed() {
      return this.disposed;
   }

   private static class RenderableReference extends WeakReference<AbstractRenderable> {
      private final long id;
      private final List<Disposable> disposables;

      public RenderableReference(AbstractRenderable referent, ReferenceQueue<? super AbstractRenderable> q) {
         super(referent, q);
         this.id = referent.getCreationTime();
         this.disposables = referent.getDisposables();
      }
   }
}
