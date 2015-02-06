package org.jrgss.api;

import com.badlogic.gdx.Gdx;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.controllers.PovDirection;
import com.badlogic.gdx.math.Vector3;
import org.jruby.*;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyConstant;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Created by matty on 6/27/14.
 */
@JRubyClass(name = "Input")
public class Input extends RubyObject {

    static Ruby runtime;
    static RubyModule rubyClass;

    static Map<Integer, Boolean> triggerStatus = new HashMap<>();
    static Map<Integer, Boolean> repeatStatus = new HashMap<>();
    static Map<Integer, Long> repeatTimestamps = new HashMap<>();
    static Map<Integer, Boolean> isPressed = new HashMap<>();

    public Input(final Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass);
    }

    private static void updateKey(int i, boolean current, boolean previous) {
        isPressed.put(i, current);
        if (!previous && current) {
            triggerStatus.put(i, true);
            repeatStatus.put(i, true);
            repeatTimestamps.put(i, System.currentTimeMillis());
        } else {
            triggerStatus.put(i, false);
        }
        if (!current) {
            repeatStatus.put(i, false);
        }
        if (current && previous) {
            if (System.currentTimeMillis() - repeatTimestamps.get(i) < 200) {
                repeatStatus.put(i, false);
            } else {
                repeatStatus.put(i, true);
                repeatTimestamps.put(i, System.currentTimeMillis());
            }
        }
    }

    @JRubyMethod(module = true)
    public static void update(IRubyObject self) {
        for (int i = 1; i < 256; i++) {
            Boolean previous = isPressed.get(i);
            if (previous == null) {
                previous = Boolean.FALSE;
            }
            boolean current = Gdx.input.isKeyPressed(i);
            updateKey(i, current, previous);
        }
        if (Controllers.getControllers().size > 0) {
            for (int i = 0; i < 16; i++) {
                Boolean previous = isPressed.get(gamepadButton(i));
                if (previous == null) {
                    previous = Boolean.FALSE;
                }
                boolean current = Controllers.getControllers().get(0).getButton(i);
                updateKey(gamepadButton(i), current, previous);
            }
        }

    }

    @JRubyMethod(module = true, name = "press?")
    public static IRubyObject isPress_p(ThreadContext context, IRubyObject self, IRubyObject sym) {
        boolean result = isPress(sym.asJavaString());
        return RubyBoolean.newBoolean(context.getRuntime(), result);
    }

    public static boolean isPress(String sym) {
        for (int i : convertSymToKey(sym)) {
            Boolean result = isPressed.get(i);
            if (result != null && result) return true;
        }
        return false;
    }

    @JRubyMethod(module = true, name = "trigger?")
    public static IRubyObject isTrigger_p(ThreadContext context, IRubyObject self, IRubyObject sym) {
        boolean result = isTrigger(sym.asJavaString());
        return RubyBoolean.newBoolean(context.getRuntime(), result);
    }

    public static boolean isTrigger(String sym) {
        for (int i : convertSymToKey(sym)) {
            Boolean result = triggerStatus.get(i);
            if (result != null && result) return true;
        }
        return false;
    }


    @JRubyMethod(module = true, name = "repeat?")
    public static IRubyObject isRepeat_p(ThreadContext context, IRubyObject self, IRubyObject sym) {
        boolean result = isRepeat(sym.asJavaString());
        return RubyBoolean.newBoolean(context.getRuntime(), result);
    }

    public static boolean isRepeat(String sym) {
        for (int i : convertSymToKey(sym)) {
            Boolean result = repeatStatus.get(i);
            if (result != null && result) return true;
        }
        return false;
    }

    @JRubyMethod(module = true)
    public static IRubyObject dir4(IRubyObject self) {
        int ret = 0;
        if (isPress(DOWN)) {
            ret = 2;
        }
        if (isPress(LEFT)) {
            ret = 4;
        }
        if (isPress(RIGHT)) {
            ret = 6;
        }
        if (isPress(UP)) {
            ret = 8;
        }
        return new RubyFixnum(runtime, ret);
    }

    @JRubyMethod(module = true)
    public static IRubyObject dir8(IRubyObject self) {
        return new RubyFixnum(runtime, 0);
    }

    private static int[] convertSymToKey(String sym) {
        int[] ret = bindings.get(sym);
        if (ret != null) {
            return ret;
        }
        return new int[]{0};
    }

    @JRubyConstant
    public static final String DOWN = "DOWN";
    @JRubyConstant
    public static final String LEFT = "LEFT";
    @JRubyConstant
    public static final String RIGHT = "RIGHT";
    @JRubyConstant
    public static final String UP = "UP";

    @JRubyConstant
    public static final String A = "A",
            B = "B", C = "C", X = "X", Y = "Y", Z = "Z", L = "L", R = "R";
    @JRubyConstant
    public static final String SHIFT = "SHIFT", CTRL = "CTRL", ALT = "ALT";
    @JRubyConstant
    public static final String F5 = "F5", F6 = "F6", F7 = "F7", F8 = "F8", F9 = "F9";


    //Default bindings. These can be overwritten later when bindings are read from preferences
    private final static HashMap<String, int[]> bindings = new HashMap<>();

    static {
        bindings.put(DOWN, new int[]{Keys.DOWN, XBOXButtons.DOWN.key()});
        bindings.put(LEFT, new int[]{Keys.LEFT, XBOXButtons.LEFT.key()});
        bindings.put(RIGHT, new int[]{Keys.RIGHT, XBOXButtons.RIGHT.key()});
        bindings.put(UP, new int[]{Keys.UP, XBOXButtons.UP.key()});
        bindings.put(C, new int[]{Keys.ENTER, XBOXButtons.A.key()});
        bindings.put(B, new int[]{Keys.ESCAPE, XBOXButtons.B.key()});
        bindings.put(L, new int[]{Keys.Q, XBOXButtons.LBUMP.key()});
        bindings.put(R, new int[]{Keys.W, XBOXButtons.RBUMP.key()});
        bindings.put(X, new int[]{Keys.A, XBOXButtons.X.key()});
        bindings.put(Y, new int[]{Keys.S, XBOXButtons.Y.key()});
        bindings.put(Z, new int[]{Keys.D, XBOXButtons.START.key()});
        bindings.put(SHIFT, new int[]{Keys.SHIFT_LEFT, Keys.SHIFT_RIGHT});
        bindings.put(ALT, new int[]{Keys.ALT_LEFT, Keys.ALT_RIGHT});
        bindings.put(CTRL, new int[]{Keys.CONTROL_LEFT, Keys.CONTROL_RIGHT});
        bindings.put(F5, new int[]{Keys.F5});
        bindings.put(F6, new int[]{Keys.F6});
        bindings.put(F7, new int[]{Keys.F7});
        bindings.put(F8, new int[]{Keys.F8});
        bindings.put(F9, new int[]{Keys.F9});
    }

    public static final int GAMEPAD_START = 1000;

    private static int gamepadButton(int button) {
        return button + GAMEPAD_START;
    }

    public enum XBOXButtons {
        UP,
        DOWN,
        LEFT,
        RIGHT,
        START,
        BACK,
        LSTICK,
        RSTICK,
        LBUMP,
        RBUMP,
        XBOX,
        A,
        B,
        X,
        Y;

        public int key() {
            return ordinal() + GAMEPAD_START;
        }
    }

}
/*
Input.update
Updates input data. As a general rule, this method is called once per frame.

Input.press?(sym) (RGSS3)
Determines whether the button corresponding to the symbol sym is currently being pressed.

If the button is being pressed, returns TRUE. If not, returns FALSE.

if Input.press?(:C)
  do_something
end
Input.trigger?(sym) (RGSS3)
Determines whether the button corresponding to the symbol sym is currently being pressed again.

"Pressed again" is seen as time having passed between the button being not pressed and being pressed.

If the button is being pressed, returns TRUE. If not, returns FALSE.

Input.repeat?(sym) (RGSS3)
Determines whether the button corresponding to the symbol sym is currently being pressed again.

Unlike trigger?, takes into account the repeated input of a button being held down continuously.

If the button is being pressed, returns TRUE. If not, returns FALSE.

Input.dir4
Checks the status of the directional buttons, translates the data into a specialized 4-direction input format, and returns the number pad equivalent (2, 4, 6, 8).

If no directional buttons are being pressed (or the equivalent), returns 0.

Input.dir8
Checks the status of the directional buttons, translates the data into a specialized 8-direction input format, and returns the number pad equivalent (1, 2, 3, 4, 6, 7, 8, 9).

If no directional buttons are being pressed (or the equivalent), returns 0.
*/