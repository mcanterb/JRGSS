package org.jrgss;

import com.badlogic.gdx.Gdx;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jrgss.api.AbstractRenderable;
import org.jrgss.api.Graphics;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @author matt
 * @date 1/26/15
 */
@Data
public class DebugFrame extends JFrame {

    List<AbstractRenderable> renderables;
    JList<AbstractRenderable> listView;
    JScrollPane pane;

    public DebugFrame() {
        setTitle("Debug");
        pane = new JScrollPane();
        getContentPane().add(pane);
        listView = new JList<>();
    }

    public void refresh() {
        renderables = sort();
        Gdx.app.log("Debug", renderables.size()+ " items");
        //remove(listView);
        pane.getViewport().setView(listView = new JList<>(renderables.toArray(new AbstractRenderable[]{})));
        pack();
        setVisible(true);
    }

    private ArrayList<AbstractRenderable> sort() {
        ArrayList<AbstractRenderable> ret = new ArrayList<>();
        ArrayList<AbstractRenderable> renderables = new ArrayList<>();
        ArrayList<AbstractRenderable> viewportLessRenderables = new ArrayList<>();
        Gdx.app.log("Debug",AbstractRenderable.renderQueue.size() + " items in queue");
        /*for (AbstractRenderable renderable : AbstractRenderable.renderQueue.values()) {
            if (renderable.getViewport() == null) {
                viewportLessRenderables.add(renderable);
            } else {
                renderables.add(renderable);
            }
        }*/
        Collections.sort(renderables);
        Collections.sort(viewportLessRenderables, Graphics.alternateComparator);
        Iterator<AbstractRenderable> iter;
        for (AbstractRenderable renderable : renderables) {
            iter = viewportLessRenderables.iterator();
            AbstractRenderable r;
            while (iter.hasNext() && Graphics.alternateComparator.compare((r = iter.next()), renderable) < 0) {
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

}
