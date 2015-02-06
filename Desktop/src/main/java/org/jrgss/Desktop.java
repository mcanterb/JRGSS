package org.jrgss;

import com.apple.eawt.Application;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.backends.lwjgl.JRGSSDesktop;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import org.lwjgl.input.Mouse;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.io.File;

/**
 * Created by mcanterb on 6/26/14.
 */
public class Desktop {
    public static void main(String[] args) {
        try {
            LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();

            cfg.title = "Title";
            cfg.useGL30 = false;
            cfg.width = 800;
            cfg.height = 450;
            cfg.vSyncEnabled = true;
            cfg.stencil = 8;
            cfg.resizable = false;
            cfg.useHDPI = true;
            cfg.fullscreen = false;
            cfg.addIcon(args[2] + File.separator + "icon.png", Files.FileType.Absolute);
            ConfigReader config = new ConfigReader(args[0] + File.separator + "Game.ini");
            cfg.title = config.getTitle();
            Application.getApplication().setDockIconImage(ImageIO.read(new File(args[2] + File.separator + "icon.png")));
            //Mouse.setNativeCursor()
            new JRGSSDesktop(new JRGSSGame(args[0], args[1], args[2], config), cfg);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Encountered an unexpected error: "+e.getMessage(),"Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
