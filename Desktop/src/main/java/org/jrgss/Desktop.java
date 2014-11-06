package org.jrgss;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.backends.lwjgl.JRGSSDesktop;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

import javax.swing.*;
import java.io.File;

/**
 * Created by mcanterb on 6/26/14.
 */
public class Desktop {
    public static void main(String[] args) {
        LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();
        cfg.title = "Title";
        cfg.useGL30 = false;
        cfg.width = 544;
        cfg.height = 416;
        cfg.vSyncEnabled = true;
        cfg.stencil = 8;
        cfg.resizable = false;
        cfg.addIcon(JRGSSGame.JRGSS_DIR + File.separator + "icon.png", Files.FileType.Absolute);
        ConfigReader config = new ConfigReader(args[0]+File.separator+"Game.ini");
        cfg.title = config.getTitle();

        new JRGSSDesktop(new JRGSSGame(args[0], args[1], config), cfg);
    }
}
