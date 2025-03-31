package org.jrgss.util;

import com.badlogic.gdx.utils.Disposable;
import java.beans.ConstructorProperties;
import java.util.concurrent.atomic.AtomicReference;

public class ReferenceDisposable implements Disposable {
   private final AtomicReference<? extends Disposable> disposableAtomicReference;

   @Override
   public void dispose() {
      if (this.disposableAtomicReference.get() != null) {
         this.disposableAtomicReference.get().dispose();
      }
   }

   @ConstructorProperties({"disposableAtomicReference"})
   public ReferenceDisposable(AtomicReference<? extends Disposable> disposableAtomicReference) {
      this.disposableAtomicReference = disposableAtomicReference;
   }
}
