package org.jrgss.api.win32;

import com.badlogic.gdx.Gdx;
import org.jrgss.api.Graphics;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.nio.ByteBuffer;
import static org.jrgss.JRubyUtil.*;

/**
 * Created by matt on 2/6/15.
 */
public class User32 {


    //SendInput = Win32API.new('user32'  , 'SendInput' , 'ipi' , 'i')
    @Win32Function(dll = "user32", name="SendInput", spec = "ipi")
    public static final DLLImpl SendInput = new DLLImpl() {
        @Override
        public IRubyObject call(Win32API api, ThreadContext context, IRubyObject[] args) {
            int numOfEvents = getInt(args[0]);
            ByteBuffer data = getBytes(args[1]);
            int eventSize = getInt(args[2]);

            boolean alt = false;
            boolean enter = false;

            for(int i = 0; i < numOfEvents; i++) {
                data.position(i*eventSize);
                int type = data.getInt();
                if(type != 1) {
                    Gdx.app.log("User32", "SendInput does not support non keyboard events!");
                    continue;
                }
                short key = data.getShort();
                Gdx.app.log("User32", "SendInput received KeyCode "+key);
                data.getShort();
                int flags = data.getInt();
                if(key == 18 && (flags&0x2) == 0) {
                    //They want to send alt
                    alt = true;
                }
                if(key == 13 && (flags&0x2) == 0) {
                    //They want to send enter
                    enter = true;
                }
            }

            if(alt && enter) {
                Gdx.app.log("User32", "Received ALT + ENTER. Toggling Fullscreen for compatibility!");
                Graphics.toggleFullScreen();
            }

            return rubyNum(numOfEvents);
        }
    };

    public static final int ZEUS_FULLSCREEN_WINDOW = 2; //Can be anything really

    //  CreateWindowEx = Win32API.new('user32', 'CreateWindowEx', 'ippiiiiiiiii', 'i')
    @Win32Function(dll = "user32", name = "CreateWindowEx", spec = "ippiiiiiiiii")
    public static final DLLImpl CreateWindowEx = new DLLImpl() {
        @Override
        public IRubyObject call(Win32API api, ThreadContext context, IRubyObject[] args) {

            int stylesEx = getInt(args[0]);
            String windowClass = getString(args[1]);
            String windowName = getString(args[2]);
            int style = getInt(args[3]);

            //Supports Zeus Fullscreen++ script
            if(stylesEx == 0x08000008 && windowClass.equals("Static") && windowName.equals("") && style == 0x80000000) {
                return rubyNum(ZEUS_FULLSCREEN_WINDOW);
            }

            return rubyNum(0);
        }
    };

    //  ShowWindow = Win32API.new('user32'  , 'ShowWindow'               , 'ii'          , 'i')
    @Win32Function(dll = "user32", name = "ShowWindow", spec = "ii")
    public static final DLLImpl ShowWindow = new DLLImpl() {
        @Override
        public IRubyObject call(Win32API api, ThreadContext context, IRubyObject[] args) {

            int window = getInt(args[0]);
            int flags = getInt(args[1]);

            //Supports Zeus Fullscreen++ script
            if(window == ZEUS_FULLSCREEN_WINDOW) {
                if(flags == 3) {
                    Graphics.setFullscreen(true);
                }
                if(flags == 0) {
                    Graphics.setFullscreen(false);
                }
            }

            return rubyNum(0);
        }
    };

}
