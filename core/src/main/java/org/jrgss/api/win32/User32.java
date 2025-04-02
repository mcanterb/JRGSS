package org.jrgss.api.win32;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import org.jrgss.JRGSSApplication;
import org.jrgss.api.Graphics;
import org.jruby.RubyString;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;

public class User32 {
    @Win32Function(
        dll = "user32",
        name = "SendInput",
        spec = "ipi"
    )
    public static final DLLImpl SendInput;
    public static final int ZEUS_FULLSCREEN_WINDOW = 2;
    @Win32Function(
        dll = "user32",
        name = "CreateWindowEx",
        spec = "ippiiiiiiiii"
    )
    public static final DLLImpl CreateWindowEx;
    @Win32Function(
        dll = "user32",
        name = "ShowWindow",
        spec = "ii"
    )
    public static final DLLImpl ShowWindow;
    @Win32Function(
        dll = "user32",
        name = "ShowWindow",
        spec = "i"
    )
    public static final DLLImpl GetSystemMetrics;
    @Win32Function(
        dll = "user32",
        name = "SystemParametersInfo",
        spec = "iipi"
    )
    public static final DLLImpl SystemParametersInfo;
    @Win32Function(
        dll = "user32",
        name = "GetKeyState",
        spec = "i"
    )
    public static final DLLImpl GetKeyState;
    @Win32Function(
        dll = "user32",
        name = "GetKeyboardState",
        spec = "i"
    )
    public static final DLLImpl GetKeyboardState;
    @Win32Function(
        dll = "user32",
        name = "MessageBox",
        spec = "ippi"
    )
    public static final DLLImpl MessageBox;
    private static final Map<Integer, User32.KeyFunc> keyMappings;

    private static final short VK_RETURN = 0x0D;

    private static final short VK_MENU = 0x12;

    private static final int EVENT_KEYUP = 0x02;

    static {
        Builder<Integer, User32.KeyFunc> builder = ImmutableMap.builder();
        builder.put(8, vals(67));
        builder.put(9, vals(61));
        builder.put(13, vals(66));
        builder.put(16, vals(59, 60));
        builder.put(17, vals(129, 130));
        builder.put(27, vals(131));
        builder.put(32, vals(62));
        builder.put(33, vals(92));
        builder.put(34, vals(93));
        builder.put(35, vals(132));
        builder.put(36, vals(3));
        builder.put(37, vals(21));
        builder.put(38, vals(19));
        builder.put(39, vals(22));
        builder.put(40, vals(20));
        builder.put(45, vals(133));
        builder.put(46, vals(67));

        for (int i = 0; i < 10; i++) {
            builder.put(48 + i, vals(7 + i));
        }

        for (int i = 0; i < 27; i++) {
            builder.put(65 + i, vals(29 + i));
        }

        for (int i = 0; i < 5; i++) {
            builder.put(116 + i, vals(248 + i));
        }

        builder.put(164, vals(57, 58));
        builder.put(186, vals(74));
        builder.put(222, vals(75));
        builder.put(190, vals(56));
        builder.put(187, vals(70));
        builder.put(188, vals(55));
        builder.put(189, vals(69));
        builder.put(191, vals(76));
        builder.put(192, vals(68));
        builder.put(219, vals(71));
        builder.put(221, vals(72));
        builder.put(220, vals(73));
        keyMappings = builder.build();
        SendInput = (api, context, args) -> {
            int numOfEvents = Win32Util.getInt(args[0]);
            ByteBuffer data = Win32Util.getBytes(args[1]);
            int eventSize = Win32Util.getInt(args[2]);
            boolean alt = false;
            boolean enter = false;

            for (int ix = 0; ix < numOfEvents; ix++) {
                ((Buffer) data).position(ix * eventSize);
                int type = data.getInt();
                if (type != 1) {
                    Gdx.app.log("User32", "SendInput does not support non keyboard events!");
                } else {
                    short key = data.getShort();
                    Gdx.app.log("User32", "SendInput received KeyCode " + key);
                    data.getShort();
                    int flags = data.getInt();
                    if (key == VK_MENU && (flags & EVENT_KEYUP) == 0) {
                        alt = true;
                    }

                    if (key == VK_RETURN && (flags & EVENT_KEYUP) == 0) {
                        enter = true;
                    }
                }
            }

            if (alt && enter) {
                Gdx.app.log("User32", "Received ALT + ENTER. Toggling Fullscreen for compatibility!");
                Graphics.toggleFullScreen();
            }

            return Win32Util.rubyNum(numOfEvents);
        };
        CreateWindowEx = (api, context, args) -> {
            int stylesEx = Win32Util.getInt(args[0]);
            String windowClass = Win32Util.getString(args[1]);
            String windowName = Win32Util.getString(args[2]);
            int style = Win32Util.getInt(args[3]);
            return stylesEx == 134217736 && windowClass.equals("Static") && windowName.equals("") && style == Integer.MIN_VALUE
                ? Win32Util.rubyNum(2L)
                : Win32Util.rubyNum(0L);
        };
        ShowWindow = (api, context, args) -> {
            int window = Win32Util.getInt(args[0]);
            int flags = Win32Util.getInt(args[1]);
            if (window == 2) {
                if (flags == 3) {
                    Graphics.setFullscreen(true);
                }

                if (flags == 0) {
                    Graphics.setFullscreen(false);
                }
            }

            return Win32Util.rubyNum(0L);
        };
        GetSystemMetrics = (api, context, args) -> {
            int input = Win32Util.getInt(args[0]);
            switch (input) {
                case 0:
                    if (Graphics.isFullscreen()) {
                        return Win32Util.rubyNum(Graphics.getWidth());
                    }

                    return Win32Util.rubyNum(1920L);
                case 1:
                    if (Graphics.isFullscreen()) {
                        return Win32Util.rubyNum(Graphics.getHeight());
                    }

                    return Win32Util.rubyNum(1080L);
                default:
                    return Win32Util.rubyNum(0L);
            }
        };
        SystemParametersInfo = (api, context, args) -> {
            DisplayMode mode = Gdx.graphics.getDisplayMode();
            int action = Win32Util.getInt(args[0]);
            RubyString buffer = (RubyString) args[2];
            if (action == 48) {
                ByteBuffer bb = ByteBuffer.wrap(buffer.getByteList().getUnsafeBytes()).order(ByteOrder.LITTLE_ENDIAN);
                bb.putInt(0).putInt(0).putInt(mode.width).putInt(mode.height);
                return Win32Util.rubyNum(1L);
            } else {
                Gdx.app.log("User32", "Unsupported SystemParametersInfo call: " + action);
                return Win32Util.rubyNum(0L);
            }
        };
        GetKeyState = (api, context, args) -> {
            int vk = Win32Util.getInt(args[0]);
            User32.KeyFunc func = keyMappings.get(vk);
            return func == null ? Win32Util.rubyNum(0L) : Win32Util.rubyNum(func.isPressed());
        };
        GetKeyboardState = (api, context, args) -> {
            ByteBuffer buffer = Win32Util.getPointer(Win32Util.getInt(args[0]));

            for (int ix = 0; ix < 256; ix++) {
                User32.KeyFunc func = keyMappings.get(ix);
                if (func == null) {
                    buffer.put(ix, (byte) 0);
                } else {
                    buffer.put(ix, (byte) (func.isPressed() >> 8));
                }
            }

            return Win32Util.rubyNum(1L);
        };
        MessageBox = (api, context, args) -> {
            String text = Win32Util.getString(args[1]);
            String title = Win32Util.getString(args[2]);
            return Win32Util.rubyNum(0L);
        };
    }

    private static User32.KeyFunc vals(Integer... vals) {
        return () -> {
            if (!((JRGSSApplication) Gdx.app).isFocused()) {
                return (short) 0;
            } else {
                int size = vals.length;

                for (int i = 0; i < size; i++) {
                    int val = vals[i];
                    if (Gdx.input.isKeyPressed(val)) {
                        return (short) Short.MIN_VALUE;
                    }
                }

                return (short) 0;
            }
        };
    }

    interface KeyFunc {
        short isPressed();
    }
}
