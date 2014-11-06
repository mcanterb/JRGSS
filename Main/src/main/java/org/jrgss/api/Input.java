package org.jrgss.api;

import com.badlogic.gdx.Gdx;

import java.util.HashMap;
import java.util.Map;
import com.badlogic.gdx.Input.Keys;

/**
 * Created by matty on 6/27/14.
 */
public class Input {

    static Map<Integer, Boolean> triggerStatus = new HashMap<>();
    static Map<Integer, Boolean> repeatStatus = new HashMap<>();
    static Map<Integer, Long> repeatTimestamps = new HashMap<>();
    static Map<Integer, Boolean> isPressed = new HashMap<>();
    static Integer[] keys = new Integer[] {
            Keys.UP, Keys.DOWN, Keys.LEFT, Keys.RIGHT, Keys.ENTER, Keys.ESCAPE
    };

    public static void update() {
        for(Integer i : keys) {
            Boolean previous = isPressed.get(i);
            if(previous == null) {
                previous = Boolean.FALSE;
            }
            boolean current = Gdx.input.isKeyPressed(i);
            isPressed.put(i, current);
            if(!previous && current) {
                triggerStatus.put(i, true);
                repeatStatus.put(i, true);
                repeatTimestamps.put(i, System.currentTimeMillis());
            } else {
                triggerStatus.put(i, false);
            }
            if(!current) {
                repeatStatus.put(i, false);
            }
            if(current && previous) {
                if(System.currentTimeMillis() - repeatTimestamps.get(i) < 200) {
                    repeatStatus.put(i, false);
                } else {
                    repeatStatus.put(i, true);
                    repeatTimestamps.put(i, System.currentTimeMillis());
                }
            }
        }
    }

    public static boolean isPress(String sym) {
        Boolean result = isPressed.get(convertSymToKey(sym));
        if (result == null) return false;
        return result;
    }

    public static boolean isTrigger(String sym) {
        Boolean result = triggerStatus.get(convertSymToKey(sym));
        if (result == null) return false;
        return result;
    }

    public static boolean isRepeat(String sym) {
        Boolean result = repeatStatus.get(convertSymToKey(sym));
        if (result == null) return false;
        return result;
    }

    public static int dir4() {
        if(isPressed.get(Keys.DOWN)) {
            return 2;
        }
        if(isPressed.get(Keys.LEFT)) {
            return 4;
        }
        if(isPressed.get(Keys.RIGHT)) {
            return 6;
        }
        if(isPressed.get(Keys.UP)) {
            return 8;
        }
        return 0;
    }

    public static int dir8() {
        return 0;
    }

    private static int convertSymToKey(String sym) {
        if(sym.equals("DOWN")) {
            return Keys.DOWN;
        }
        if(sym.equals("LEFT")) {
            return Keys.LEFT;
        }
        if(sym.equals("RIGHT")) {
            return Keys.RIGHT;
        }
        if(sym.equals("UP")) {
            return Keys.UP;
        }
        if(sym.equals("C")) {
            return Keys.ENTER;
        }
        if(sym.equals("B")) {
            return Keys.ESCAPE;
        }
        return 0;
    }

    public static final String DOWN = "DOWN";
    public static final String LEFT = "LEFT";
    public static final String RIGHT = "RIGHT";
    public static final String UP = "UP";

    public static final String A = "A",B = "B", C="C",X="X",Y="Y",Z = "Z", L = "L", R="R";
    public static final String SHIFT = "SHIFT", CTRL = "CTRL", ALT = "ALT";
    public static final String F5 = "F5", F6 = "F6", F7 = "F7", F8 = "F8", F9 = "F9";
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