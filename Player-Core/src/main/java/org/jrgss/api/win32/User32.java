package org.jrgss.api.win32;

import com.badlogic.gdx.Gdx;
import static com.badlogic.gdx.Input.Keys.*;

import com.badlogic.gdx.Input;
import com.google.common.collect.ImmutableMap;
import com.sun.org.apache.xpath.internal.operations.Bool;
import org.jrgss.JRGSSApplication;
import org.jrgss.api.Graphics;
import org.jruby.RubyString;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.function.Function;

import static org.jrgss.api.win32.Win32Util.*;

/**
 * Created by matt on 2/6/15.
 */
public class User32 {

    interface KeyFunc {
        short isPressed();
    }

    static KeyFunc vals(final Integer...vals) {
        return () -> {
            if (!((JRGSSApplication)Gdx.app).isFocused()) {
                return (short)0;
            }
            for(int val : vals) {
                if(Gdx.input.isKeyPressed(val)) return (short)0x8000;
            }
            return (short)0;
        };
    }

    final static Map<Integer, KeyFunc> keyMappings;

    static {
        ImmutableMap.Builder<Integer, KeyFunc> builder = ImmutableMap.builder();

        builder.put(0x08, vals(BACKSPACE));
        builder.put(0x09, vals(TAB));
        builder.put(0x0C, vals(CLEAR));
        builder.put(0x0D, vals(ENTER));
        builder.put(0x10, vals(SHIFT_LEFT, SHIFT_RIGHT));
        builder.put(0x11, vals(CONTROL_LEFT, CONTROL_RIGHT));
        builder.put(0x12, vals(ALT_LEFT, ALT_RIGHT));
        builder.put(0x14, () -> (short)(Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_CAPS_LOCK)?0x1:0x0) );
        builder.put(0x1B, vals(ESCAPE));
        builder.put(0x20, vals(SPACE));
        builder.put(0x21, vals(PAGE_UP));
        builder.put(0x22, vals(PAGE_DOWN));
        builder.put(0x23, vals(END));
        builder.put(0x24, vals(HOME));
        builder.put(0x25, vals(LEFT));
        builder.put(0x26, vals(UP));
        builder.put(0x27, vals(RIGHT));
        builder.put(0x28, vals(DOWN));
        builder.put(0x2D, vals(INSERT));
        builder.put(0x2E, vals(DEL));
        for(int i = 0; i < 10; i++) {
            builder.put(0x30+i, vals(NUM_0+i));
        }
        for(int i = 0; i < 27; i++) {
            builder.put(0x41+i, vals(A+i));
        }
        builder.put(0xBA, vals(SEMICOLON));
        builder.put(0xDE, vals(APOSTROPHE));
        builder.put(0xBE, vals(PERIOD));
        builder.put(0xBB, vals(EQUALS));
        builder.put(0xBC, vals(COMMA));
        builder.put(0xBD, vals(MINUS));
        builder.put(0xBF, vals(SLASH));
        builder.put(0xC0, vals(GRAVE));
        builder.put(0xDB, vals(LEFT_BRACKET));
        builder.put(0xDD, vals(RIGHT_BRACKET));
        builder.put(0xDC, vals(BACKSLASH));


        keyMappings = builder.build();
    }



    //SendInput = Win32API.new('user32'  , 'SendInput' , 'ipi' , 'i')
    @Win32Function(dll = "user32", name="SendInput", spec = "ipi")
    public static final DLLImpl SendInput = (api, context, args) -> {
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
    };

    public static final int ZEUS_FULLSCREEN_WINDOW = 2; //Can be anything really

    //  CreateWindowEx = Win32API.new('user32', 'CreateWindowEx', 'ippiiiiiiiii', 'i')
    @Win32Function(dll = "user32", name = "CreateWindowEx", spec = "ippiiiiiiiii")
    public static final DLLImpl CreateWindowEx = (api, context, args) -> {

        int stylesEx = getInt(args[0]);
        String windowClass = getString(args[1]);
        String windowName = getString(args[2]);
        int style = getInt(args[3]);

        //Supports Zeus Fullscreen++ script
        if(stylesEx == 0x08000008 && windowClass.equals("Static") && windowName.equals("") && style == 0x80000000) {
            return rubyNum(ZEUS_FULLSCREEN_WINDOW);
        }

        return rubyNum(0);
    };

    //  ShowWindow = Win32API.new('user32'  , 'ShowWindow'               , 'ii'          , 'i')
    @Win32Function(dll = "user32", name = "ShowWindow", spec = "ii")
    public static final DLLImpl ShowWindow = (api, context, args) -> {

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
    };

    //  GetSystemMetrics = Win32API.new('user32'  , 'GetSystemMetrics'         , 'i'           , 'i')
    @Win32Function(dll = "user32", name = "ShowWindow", spec = "i")
    public static final DLLImpl GetSystemMetrics = (api, context, args) -> {

        int input = getInt(args[0]);

        //The guess here is the script is trying to detect whether we are in rpgmaker fullscreen mode.
        //If so, then the width will be the same width set in the Graphics module. Otherwise, it's probably 1920x1080
        switch (input) {
            case 0:
                if(Graphics.isFullscreen()) {
                    return rubyNum(Graphics.getWidth());
                } else {
                    return rubyNum(1920);
                }
            case 1:
                if(Graphics.isFullscreen()) {
                    return rubyNum(Graphics.getHeight());
                } else {
                    return rubyNum(1080);
                }
        }
        return rubyNum(0);
    };

    @Win32Function(dll = "user32", name = "SystemParametersInfo", spec = "iipi")
    public static final DLLImpl SystemParametersInfo = (api, context, args) -> {
        final Insets insets = getScreenInsets(null);
        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        int action = getInt(args[0]);
        RubyString buffer = (RubyString) args[2];

        if(action == 0x30) {
            ByteBuffer bb = ByteBuffer.wrap(buffer.getByteList().getUnsafeBytes()).order(ByteOrder.LITTLE_ENDIAN);
            bb      .putInt(insets.left)
                    .putInt(insets.top)
                    .putInt((int)screenSize.getWidth() - insets.right)
                    .putInt((int)screenSize.getHeight() - insets.bottom);
            return rubyNum(1);
        }
        Gdx.app.log("User32", "Unsupported SystemParametersInfo call: "+action);
        return rubyNum(0);

    };

    //Win32API.new("user32","GetKeyState", 'i', 'i')
    @Win32Function(dll="user32", name="GetKeyState", spec = "i")
    public static final DLLImpl GetKeyState = (api, context, args) -> {
        int vk = getInt(args[0]);
        KeyFunc func = keyMappings.get(vk);
        if(func == null) return rubyNum(0);
        return rubyNum(func.isPressed());
    };


    static public Insets getScreenInsets(Window wnd) {
        Insets                              si;

        if(wnd==null) {
            si=Toolkit.getDefaultToolkit().getScreenInsets(GraphicsEnvironment
                    .getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice()
                    .getDefaultConfiguration());
        }
        else {
            si=wnd.getToolkit().getScreenInsets(wnd.getGraphicsConfiguration());
        }
        return si;
    }

}
