package org.jrgss;

import com.badlogic.gdx.ApplicationListener;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @author matt
 * @date 7/5/14
 */
public interface JRGSSApplicationListener extends ApplicationListener {
    public void loadScripts();
    public JRGSSGame.JRGSSMain getMain();
}
