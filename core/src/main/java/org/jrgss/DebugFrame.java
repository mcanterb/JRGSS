package org.jrgss;

import com.badlogic.gdx.Gdx;
import org.jrgss.api.AbstractRenderable;
import org.jrgss.api.Graphics;

import javax.swing.*;
import java.util.*;

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

    public void setRenderables(List<AbstractRenderable> renderables) {
        this.renderables = renderables;
    }

    public JList<AbstractRenderable> getListView() {
        return this.listView;
    }

    public void setListView(JList<AbstractRenderable> listView) {
        this.listView = listView;
    }

    public JScrollPane getPane() {
        return this.pane;
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
            DebugFrame other = (DebugFrame) o;
            if (!other.canEqual(this)) {
                return false;
            } else {
                Object this$renderables = this.getRenderables();
                Object other$renderables = other.getRenderables();
                if (Objects.equals(this$renderables, other$renderables)) {
                    Object this$listView = this.getListView();
                    Object other$listView = other.getListView();
                    if (Objects.equals(this$listView, other$listView)) {
                        Object this$pane = this.getPane();
                        Object other$pane = other.getPane();
                        return Objects.equals(this$pane, other$pane);
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
