package org.jrgss;

import com.badlogic.gdx.ApplicationListener;

public interface JRGSSApplicationListener extends ApplicationListener {
   void loadSplashScreen();

   void loadScripts();

   JRGSSGame.JRGSSMain getMain();
}
