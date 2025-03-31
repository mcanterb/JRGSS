package org.jrgss;

import com.badlogic.gdx.Gdx;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import org.jrgss.api.AbstractRenderable;
import org.jrgss.api.Graphics;

public class DebugFrame extends JFrame {
   List<AbstractRenderable> renderables;
   JList<AbstractRenderable> listView;
   JScrollPane pane;

   public DebugFrame() {
      this.setTitle("Debug");
      this.pane = new JScrollPane();
      this.getContentPane().add(this.pane);
      this.listView = new JList<>();
   }

   public void refresh() {
      this.renderables = this.sort();
      Gdx.app.log("Debug", this.renderables.size() + " items");
      this.pane.getViewport().setView(this.listView = new JList<>(this.renderables.toArray(new AbstractRenderable[0])));
      this.pack();
      this.setVisible(true);
   }

   private ArrayList<AbstractRenderable> sort() {
      ArrayList<AbstractRenderable> ret = new ArrayList<>();
      ArrayList<AbstractRenderable> renderables = new ArrayList<>();
      ArrayList<AbstractRenderable> viewportLessRenderables = new ArrayList<>();
      Gdx.app.log("Debug", AbstractRenderable.renderQueue.size() + " items in queue");
      Collections.sort(renderables);
      Collections.sort(viewportLessRenderables, Graphics.alternateComparator);

      for (AbstractRenderable renderable : renderables) {
         Iterator<AbstractRenderable> iter = viewportLessRenderables.iterator();

         AbstractRenderable r;
         while (iter.hasNext() && Graphics.alternateComparator.compare(r = iter.next(), renderable) < 0) {
            ret.add(r);
            iter.remove();
         }

         ret.add(renderable);
      }

      for (AbstractRenderable renderable : viewportLessRenderables) {
         ret.add(renderable);
      }

      return ret;
   }

   public List<AbstractRenderable> getRenderables() {
      return this.renderables;
   }

   public JList<AbstractRenderable> getListView() {
      return this.listView;
   }

   public JScrollPane getPane() {
      return this.pane;
   }

   public void setRenderables(List<AbstractRenderable> renderables) {
      this.renderables = renderables;
   }

   public void setListView(JList<AbstractRenderable> listView) {
      this.listView = listView;
   }

   public void setPane(JScrollPane pane) {
      this.pane = pane;
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof DebugFrame)) {
         return false;
      } else {
         DebugFrame other = (DebugFrame)o;
         if (!other.canEqual(this)) {
            return false;
         } else {
            Object this$renderables = this.getRenderables();
            Object other$renderables = other.getRenderables();
            if (this$renderables == null ? other$renderables == null : this$renderables.equals(other$renderables)) {
               Object this$listView = this.getListView();
               Object other$listView = other.getListView();
               if (this$listView == null ? other$listView == null : this$listView.equals(other$listView)) {
                  Object this$pane = this.getPane();
                  Object other$pane = other.getPane();
                  return this$pane == null ? other$pane == null : this$pane.equals(other$pane);
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
      return other instanceof DebugFrame;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = 1;
      Object $renderables = this.getRenderables();
      result = result * 59 + ($renderables == null ? 43 : $renderables.hashCode());
      Object $listView = this.getListView();
      result = result * 59 + ($listView == null ? 43 : $listView.hashCode());
      Object $pane = this.getPane();
      return result * 59 + ($pane == null ? 43 : $pane.hashCode());
   }

   @Override
   public String toString() {
      return "DebugFrame(renderables=" + this.getRenderables() + ", listView=" + this.getListView() + ", pane=" + this.getPane() + ")";
   }
}
