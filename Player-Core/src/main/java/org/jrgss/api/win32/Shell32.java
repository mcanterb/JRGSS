package org.jrgss.api.win32;

import com.badlogic.gdx.Gdx;
import lombok.Data;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jrgss.api.win32.Win32Util.*;

import java.awt.*;
import java.io.File;
import java.net.URI;

/**
 * @author matt
 * @date 1/26/15
 */
@Data
public class Shell32 {


    //shell = Win32API.new("shell32", "ShellExecute", ['L', 'P', 'P', 'P', 'P', 'L'], 'L')
    //shell.Call(0, "open", filename, 0, 0, 1)
    @Win32Function(dll = "shell32", name = "ShellExecute", spec = "LPPPPL")
    public static final DLLImpl ShellExecute = new DLLImpl() {
        public IRubyObject call(Win32API api, ThreadContext context, IRubyObject[] args) {
            String command = getString(args[1]);
            String urlString = getString(args[2]);
            if (!command.equalsIgnoreCase("open")) {
                Gdx.app.error("Shell32", "ShellExecute implementation does not support anything except open!");
                return rubyNum(0);
            }
            try {
                URI uri = URI.create(urlString);
                Desktop.getDesktop().browse(uri);
            } catch (IllegalArgumentException arg) {
                try {
                    Desktop.getDesktop().open(new File(urlString));
                } catch (Exception e) {
                    Gdx.app.error("Shell32", "Failed to open " + urlString + "! " + e);
                }
                return rubyNum(0);
            } catch (Exception e) {
                Gdx.app.error("Shell32", "This platform does not seem to support browsing: " + e);
                return rubyNum(0);
            }
            Gdx.app.log("Shell32", "Stub ShellExecute behavior for " + command + ", " + urlString);
            return rubyNum(0);
        }
    };


}
