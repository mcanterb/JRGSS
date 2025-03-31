package io.github.mcanterb.jrgss.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.glutils.HdpiMode;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.jrgss.ConfigReader;
import org.jrgss.FileUtil;
import org.jrgss.JRGSSApplicationListener;
import org.jrgss.JRGSSGame;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.*;

/** Launches the desktop (LWJGL3) application. */
public class Lwjgl3Launcher {
    public static void main(String[] args) {
        if (StartupHelper.startNewJvmIfRequired()) return; // This handles macOS support and helps on Windows.
        ArgumentParser parser = ArgumentParsers.newFor("JRGSS").build()
            .defaultHelp(true)
            .description("JRGSS Game Engine");
        parser.addArgument("-t", "--test")
            .setDefault(false)
            .help("Run the game in test mode")
            .action(Arguments.storeTrue())
            .type(Boolean.class);
        parser.addArgument("--rtp")
            .type(String.class)
            .help("The root to the RTP files");
        parser.addArgument("game_directory")
            .type(String.class)
            .help("The target game root. Must contain a Game.ini file.");
        Namespace ns = null;
        try {
            ns = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            StringWriter out    = new StringWriter();
            PrintWriter  writer = new PrintWriter(out);
            parser.handleError(e, writer);
            TinyFileDialogs.tinyfd_messageBox("Error", out.toString(), "ok", "error", true);
            System.exit(0);
        }
        String dir = ns.getString("game_directory");
        String rtp = rtpDir(ns.getString("rtp"));
        boolean isTesting = ns.getBoolean("test");
        try {
            if (dir.charAt(dir.length() - 1) != File.separatorChar) {
                dir = dir + File.separatorChar;
            }

            Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
            cfg.setWindowIcon("logo.png");
            cfg.setTitle("JRGSS");
            cfg.setWindowedMode(800, 450);
            cfg.setHdpiMode(HdpiMode.Pixels);
            cfg.setResizable(false);
            if (System.getProperty("jrgss.icon") != null &&
                new File(System.getProperty("jrgss.icon")).exists()) {
                cfg.setWindowIcon(System.getProperty("jrgss.icon"));
            }

            if (System.getProperty("os.name").toLowerCase().contains("linux")) {
                // We need a better way to determine this. Easiest way would be
                // writing a tmp file and trying to read it
                FileUtil.onCaseSensitiveFileSystem = true;
            }
            //// Vsync limits the frames per second to what your hardware can display, and helps eliminate
            //// screen tearing. This setting doesn't always work on Linux, so the line after is a safeguard.
            cfg.useVsync(true);
            //// Limits FPS to the refresh rate of the currently active monitor, plus 1 to try to match fractional
            //// refresh rates. The Vsync setting above should limit the actual FPS to match the monitor.
            cfg.setForegroundFPS(Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate + 1);
            ConfigReader config = new ConfigReader(dir + "Game.ini");
            cfg.setTitle(config.getTitle());
            createApplication(new JRGSSGame(dir, rtp, config, isTesting), cfg);
        } catch (Exception var19) {
            Exception e = var19;
            var19.printStackTrace(System.err);
            TinyFileDialogs.tinyfd_messageBox("Error", var19.getMessage(), "ok", "error", true);

            try (FileWriter writer = new FileWriter(FileUtil.localDirectory + File.separator + "JrgssCrashLog" + System.currentTimeMillis() + ".log")) {
                e.printStackTrace(new PrintWriter(writer));
            } catch (IOException var18) {
                TinyFileDialogs.tinyfd_messageBox("Error", "Failed to write crash log! " + var18.getMessage(), "ok", "error", true);
            }
        }

    }

    private static Lwjgl3Application createApplication(JRGSSApplicationListener game,
                                                       Lwjgl3ApplicationConfiguration config) {
        return new JrgssDesktopApplication(game, config);
    }


   private static String rtpDir(String providedRtpDir) {
      //TODO: Try to detect RTP and if it doesn't exist, install it.
      return providedRtpDir;
   }
}
