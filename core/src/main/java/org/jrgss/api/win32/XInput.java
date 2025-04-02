package org.jrgss.api.win32;

import org.jrgss.api.xbox.ControllerState;
import org.jrgss.api.xbox.XboxControllers;

import java.nio.ByteBuffer;

public final class XInput {
    private static final int ERROR_DEVICE_NOT_CONNECTED = 1167;
    private static final int ERROR_SUCCESS = 0;
    private static final int[] MASKS = new int[]{1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 4096, 8192, 16384, 32768};
    @Win32Function(
        dll = "xinput1_3",
        name = "XInputGetState",
        spec = "ip"
    )
    public static final DLLImpl XInputGetState = (api, ctx, args) -> {
        int controller = Win32Util.getInt(args[0]);
        ByteBuffer result = Win32Util.getBytes(args[1]);
        result.putLong(0, 0L).putLong(1, 0L);
        if (controller != 0) {
            result.putLong(0, 0L).putLong(1, 0L);
            return Win32Util.rubyNum(1167L);
        } else {
            write(XboxControllers.INSTANCE.getState(), result);
            return Win32Util.rubyNum(0L);
        }
    };

    private XInput() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    private static void write(ControllerState state, ByteBuffer buffer) {
        buffer.putInt(state.getPacket());
        short buttonState = 0;

        for (int i = 0; i < 14; i++) {
            buttonState = (short) (buttonState | (state.getButtonStates()[i] ? MASKS[i] : 0));
        }

        buffer.putShort(buttonState);
    }
}
