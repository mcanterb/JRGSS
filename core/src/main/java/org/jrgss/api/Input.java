package org.jrgss.api;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.Controllers;
import org.jrgss.JRGSSApplication;
import org.jrgss.api.xbox.AxisDirection;
import org.jrgss.api.xbox.XBOXButtons;
import org.jruby.*;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyConstant;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.*;
import java.util.Map.Entry;

@JRubyClass(
    name = {"Input"}
)
public class Input extends RubyObject {
    @JRubyConstant
    public static final String DOWN = "DOWN";
    @JRubyConstant
    public static final String LEFT = "LEFT";
    @JRubyConstant
    public static final String RIGHT = "RIGHT";
    @JRubyConstant
    public static final String UP = "UP";
    @JRubyConstant
    public static final String A = "A";
    @JRubyConstant
    public static final String B = "B";
    @JRubyConstant
    public static final String C = "C";
    @JRubyConstant
    public static final String X = "X";
    @JRubyConstant
    public static final String Y = "Y";
    @JRubyConstant
    public static final String Z = "Z";
    @JRubyConstant
    public static final String L = "L";
    @JRubyConstant
    public static final String R = "R";
    @JRubyConstant
    public static final String SHIFT = "SHIFT";
    @JRubyConstant
    public static final String CTRL = "CTRL";
    @JRubyConstant
    public static final String ALT = "ALT";
    @JRubyConstant
    public static final String F5 = "F5";
    @JRubyConstant
    public static final String F6 = "F6";
    @JRubyConstant
    public static final String F7 = "F7";
    @JRubyConstant
    public static final String F8 = "F8";
    @JRubyConstant
    public static final String F9 = "F9";
    public static final int VK_FULLSCREEN = Integer.MAX_VALUE;
    public static final int VK_FULLSCREEN_COMBO_ALT = Integer.MAX_VALUE - 1;
    public static final int VK_FULLSCREEN_COMBO_ENTER = Integer.MAX_VALUE - 2;
    private static final HashMap<String, int[]> bindings = new HashMap<>();
    static Ruby runtime;
    static RubyModule rubyClass;
    static Map<Integer, Boolean> triggerStatus = new HashMap<>();
    static Map<Integer, Boolean> repeatStatus = new HashMap<>();
    static Map<Integer, Long> repeatTimestamps = new HashMap<>();
    static Map<Integer, Boolean> isPressed = new HashMap<>();

    static {
        bindings.put(DOWN, new int[]{Keys.DOWN, XBOXButtons.DOWN.key()});
        bindings.put(LEFT, new int[]{Keys.LEFT, XBOXButtons.LEFT.key()});
        bindings.put(RIGHT, new int[]{Keys.RIGHT, XBOXButtons.RIGHT.key()});
        bindings.put(UP, new int[]{Keys.UP, XBOXButtons.UP.key()});
        bindings.put(C, new int[]{Keys.ENTER, Keys.SPACE, Keys.Z, XBOXButtons.A.key()});
        bindings.put(B, new int[]{Keys.ESCAPE, Keys.X, Keys.NUM_0, XBOXButtons.B.key()});
        bindings.put(L, new int[]{Keys.Q, XBOXButtons.LBUMP.key()});
        bindings.put(R, new int[]{Keys.W, XBOXButtons.RBUMP.key()});
        bindings.put(X, new int[]{Keys.A, XBOXButtons.X.key()});
        bindings.put(Y, new int[]{Keys.S, XBOXButtons.Y.key()});
        bindings.put(Z, new int[]{Keys.D, XBOXButtons.START.key()});
        bindings.put(A, new int[]{Keys.SHIFT_LEFT, Keys.SHIFT_RIGHT});
        bindings.put(SHIFT, new int[]{Keys.SHIFT_LEFT, Keys.SHIFT_RIGHT});
        bindings.put(ALT, new int[]{Keys.ALT_LEFT, Keys.ALT_RIGHT});
        bindings.put(CTRL, new int[]{Keys.CONTROL_LEFT, Keys.CONTROL_RIGHT});
        bindings.put(F5, new int[]{Keys.F5});
        bindings.put(F6, new int[]{Keys.F6});
        bindings.put(F7, new int[]{Keys.F7});
        bindings.put(F8, new int[]{Keys.F8});
        bindings.put(F9, new int[]{Keys.F9});
    }

    public Input(Ruby runtime, RubyClass metaClass) {
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
            if (System.currentTimeMillis() - repeatTimestamps.get(i) < 200L) {
                repeatStatus.put(i, false);
            } else {
                repeatStatus.put(i, true);
                repeatTimestamps.put(i, System.currentTimeMillis());
            }
        }
    }

    @JRubyMethod(
        module = true
    )
    public static void update(IRubyObject self) {
        boolean isAltPressed = Gdx.input.isKeyPressed(Keys.ALT_RIGHT) || Gdx.input.isKeyPressed(Keys.ALT_LEFT);
        boolean isEnterPressed = Gdx.input.isKeyPressed(Keys.ENTER);
        boolean isFullScreenPressed = isAltPressed && isEnterPressed;
        if (((JRGSSApplication) Gdx.app).isFocused()) {
            for (int i = 0; i < 256; i++) {
                if (i == Keys.ENTER && ((isPressed.get(VK_FULLSCREEN) != null && isPressed.get(VK_FULLSCREEN)) || isFullScreenPressed)) {
                    continue;
                }
                Boolean previous = isPressed.get(i);
                if (previous == null) {
                    previous = Boolean.FALSE;
                }

                boolean current = Gdx.input.isKeyPressed(i);
                updateKey(i, current, previous);
            }

            if (Controllers.getControllers().size == 0) {
                return;
            }
            final Controller controller = Controllers.getControllers().get(0);
            for (int i = 0; i < 16; i++) {
                Boolean previous = isPressed.get(XBOXButtons.gamepadButton(i));
                if (previous == null) {
                    previous = Boolean.FALSE;
                }

                boolean current = controller.getButton(i);
                updateKey(XBOXButtons.gamepadButton(i), current, previous);
            }
            Set<AxisDirection> pushed = new HashSet<>();
            float analogx = controller.getAxis(0);
            float analogy = controller.getAxis(1);
            Collections.addAll(pushed, AxisDirection.fromAnalog(analogx, analogy));
            for (AxisDirection AxisDirection : AxisDirection.values()) {
                Boolean previous = isPressed.get(XBOXButtons.axisDirection(AxisDirection));
                if (previous == null) {
                    previous = Boolean.FALSE;
                }

                boolean current = pushed.contains(AxisDirection);
                updateKey(XBOXButtons.axisDirection(AxisDirection), current, previous);
            }

        } else {
            for (Entry<Integer, Boolean> entry : isPressed.entrySet()) {
                updateKey(entry.getKey(), Boolean.FALSE, entry.getValue());
            }
        }
    }

    @JRubyMethod(
        module = true,
        name = {"press?"}
    )
    public static IRubyObject isPress_p(ThreadContext context, IRubyObject self, IRubyObject sym) {
        boolean result = isPress(sym.asJavaString());
        return RubyBoolean.newBoolean(context.getRuntime(), result);
    }

    public static boolean isPress(String sym) {
        for (int i : convertSymToKey(sym)) {
            Boolean result = isPressed.get(i);
            if (result != null && result) {
                return true;
            }
        }

        return false;
    }

    @JRubyMethod(
        module = true,
        name = {"trigger?"}
    )
    public static IRubyObject isTrigger_p(ThreadContext context, IRubyObject self, IRubyObject sym) {
        boolean result = isTrigger(sym.asJavaString());
        return RubyBoolean.newBoolean(context.getRuntime(), result);
    }

    public static boolean isFullscreenTriggered() {
        boolean isAltPressed = Gdx.input.isKeyPressed(Keys.ALT_RIGHT) || Gdx.input.isKeyPressed(Keys.ALT_LEFT);
        boolean isEnterPressed = Gdx.input.isKeyPressed(Keys.ENTER);
        boolean isFullScreenPressed = isAltPressed && isEnterPressed;
        boolean previousFullScreenPressed = isPressed.get(VK_FULLSCREEN) != null && isPressed.get(VK_FULLSCREEN);
        if(isFullScreenPressed) {
            updateKey(VK_FULLSCREEN, true, previousFullScreenPressed);
        } else if(previousFullScreenPressed && !isEnterPressed) {
            updateKey(VK_FULLSCREEN, false, true);
        } else if(previousFullScreenPressed) {
            updateKey(VK_FULLSCREEN, true, true);
        } else {
            updateKey(VK_FULLSCREEN, false, false);
        }
        return triggerStatus.get(VK_FULLSCREEN) != null && triggerStatus.get(VK_FULLSCREEN);
    }

    public static boolean isTrigger(String sym) {
        for (int i : convertSymToKey(sym)) {
            Boolean result = triggerStatus.get(i);
            if (result != null && result) {
                return true;
            }
        }

        return false;
    }

    @JRubyMethod(
        module = true,
        name = {"repeat?"}
    )
    public static IRubyObject isRepeat_p(ThreadContext context, IRubyObject self, IRubyObject sym) {
        boolean result = isRepeat(sym.asJavaString());
        return RubyBoolean.newBoolean(context.getRuntime(), result);
    }

    public static boolean isRepeat(String sym) {
        for (int i : convertSymToKey(sym)) {
            Boolean result = repeatStatus.get(i);
            if (result != null && result) {
                return true;
            }
        }

        return false;
    }

    @JRubyMethod(
        module = true
    )
    public static IRubyObject dir4(IRubyObject self) {
        int ret = 0;
        if (isPress("DOWN")) {
            ret = 2;
        }

        if (isPress("LEFT")) {
            ret = 4;
        }

        if (isPress("RIGHT")) {
            ret = 6;
        }

        if (isPress("UP")) {
            ret = 8;
        }

        return new RubyFixnum(runtime, ret);
    }

    @JRubyMethod(
        module = true
    )
    public static IRubyObject dir8(IRubyObject self) {
        return new RubyFixnum(runtime, 0L);
    }

    private static int[] convertSymToKey(String sym) {
        int[] ret = bindings.get(sym);
        return ret != null ? ret : new int[]{0};
    }
}
