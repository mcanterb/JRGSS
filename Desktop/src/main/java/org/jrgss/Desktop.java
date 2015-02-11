package org.jrgss;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.backends.lwjgl.JRGSSDesktop;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import org.lwjgl.input.Mouse;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Method;

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
            setDockIconIfOnOSX(args[2] + File.separator + "icon.png");
            new JRGSSDesktop(new JRGSSGame(args[0], args[1], args[2], config), cfg);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Encountered an unexpected error: "+e.getMessage(),"Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void setDockIconIfOnOSX(String iconFile) {
        if(System.getProperty("os.name").toLowerCase().contains("mac")) {
            try {
                Class<?> clazz = Class.forName("com.apple.eawt.Application");
                Method getApplication = clazz.getDeclaredMethod("getApplication");
                Method setDockIconImage = clazz.getDeclaredMethod("setDockIconImage", Image.class);
                Object application = getApplication.invoke(null);
                BufferedImage icon = ImageIO.read(new File(iconFile));
                setDockIconImage.invoke(application, icon);
            } catch (Exception e) {
                //Failed to set Dock Icon. Probably not on OSX
                System.err.println("Failed to set OS X Dock icon! ");
                e.printStackTrace(System.err);
            }
        }
    }


}
