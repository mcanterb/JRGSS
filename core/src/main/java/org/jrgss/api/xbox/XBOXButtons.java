package org.jrgss.api.xbox;

import java.util.HashMap;
import java.util.Map;
import org.jrgss.OS;

public enum XBOXButtons {
   UP(gamepadButton(0), axisDirection(AxisDirection.NORTH), 0),
   DOWN(gamepadButton(1), axisDirection(AxisDirection.SOUTH), 1),
   LEFT(gamepadButton(2), axisDirection(AxisDirection.WEST), 2),
   RIGHT(gamepadButton(3), axisDirection(AxisDirection.EAST), 3),
   START(gamepadButton(4), gamepadButton(7), 4),
   BACK(gamepadButton(5), gamepadButton(6), 5),
   LSTICK(gamepadButton(6), gamepadButton(9), 6),
   RSTICK(gamepadButton(7), gamepadButton(10), 7),
   LBUMP(gamepadButton(8), gamepadButton(4), 8),
   RBUMP(gamepadButton(9), gamepadButton(5), 9),
   A(gamepadButton(11), gamepadButton(0), 10),
   B(gamepadButton(12), gamepadButton(1), 11),
   X(gamepadButton(13), gamepadButton(2), 12),
   Y(gamepadButton(14), gamepadButton(3), 13);

   private static final Map<Integer, XBOXButtons> BUTTONS_MAP = new HashMap<>();
   private static final Map<AxisDirection, XBOXButtons> POV_DIRECTION_MAP = new HashMap<>();
   private int key;
   private int xboxKey;
   public static final int GAMEPAD_START = 1000;
   public static final int AXIS_START = 1100;

   XBOXButtons(int mac, int linux, int xboxKey) {
      if (OS.CURRENT_OS == OS.LINUX) {
         this.key = linux;
      } else {
         this.key = mac;
      }

      this.xboxKey = xboxKey;
   }

   public int key() {
      return this.key;
   }

   public int getXboxButtonIndex() {
      return this.xboxKey;
   }

   public int getButton() {
      return this.key < AXIS_START ? this.key - GAMEPAD_START : -1;
   }

   public AxisDirection getAxisDirection() {
      return this.key >= AXIS_START ? AxisDirection.values()[this.key - AXIS_START] : null;
   }

   public static int gamepadButton(int button) {
      return button + GAMEPAD_START;
   }

   public static int axisDirection(AxisDirection direction) {
      return direction.ordinal() + AXIS_START;
   }

   public static XBOXButtons getButtonFor(int button) {
      return BUTTONS_MAP.get(button);
   }

   public static XBOXButtons getButtonFor(AxisDirection direction) {
      return POV_DIRECTION_MAP.get(direction);
   }

   static {
      for (XBOXButtons button : values()) {
         int b = button.getButton();
         if (b > -1) {
            BUTTONS_MAP.put(b, button);
         } else {
            AxisDirection direction = button.getAxisDirection();
            if (direction != null) {
               POV_DIRECTION_MAP.put(direction, button);
            }
         }
      }
   }
}
