package org.jrgss.api.xbox;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.math.Vector3;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

public enum XboxControllers implements ControllerListener {
    INSTANCE;

    private Controller currentController;
    private ControllerState state = new ControllerState();

    @Override
    public void connected(Controller controller) {
        Gdx.app.log("Controller", controller.getName() + " connected.");
        Gdx.app.log("Controllers", "# of controllers plugged in: " + Controllers.getControllers().size);
        this.currentController = Controllers.getControllers().first();
        this.update();
    }

    @Override
    public void disconnected(Controller controller) {
        Gdx.app.log("Controller", controller.getName() + " disconnected.");
        Gdx.app.log("Controllers", "# of controllers plugged in: " + Controllers.getControllers().size);
        if (Controllers.getControllers().size == 0) {
            this.currentController = null;
            this.state = new ControllerState();
        } else {
            this.currentController = Controllers.getControllers().first();
        }

        this.update();
    }

    @Override
    public boolean buttonDown(Controller controller, int i) {
        if (controller == this.currentController) {
            XBOXButtons button = XBOXButtons.getButtonFor(i);
            if (button != null) {
                this.state = this.state.withNewButtonStatus(button.getXboxButtonIndex(), true);
            }
        }

        return true;
    }

    @Override
    public boolean buttonUp(Controller controller, int i) {
        if (controller == this.currentController) {
            XBOXButtons button = XBOXButtons.getButtonFor(i);
            if (button != null) {
                this.state = this.state.withNewButtonStatus(button.getXboxButtonIndex(), false);
            }
        }

        return true;
    }

    @Override
    public boolean axisMoved(Controller controller, int i, float v) {
        if (controller == this.currentController && (i == 7 || i == 6)) {
            float x = controller.getAxis(6);
            float y = controller.getAxis(7);
            if (i == 6) {
                x = v;
            }

            if (i == 7) {
                y = v;
            }

            AxisDirection[] axisDirection = AxisDirection.fromAnalog(x, y);
            this.update(axisDirection);
        }

        return true;
    }

    private void update() {
        this.update(null);
    }

    private void update(AxisDirection[] directions) {
        if (this.currentController != null) {
            boolean[] buttons = new boolean[14];

            for (XBOXButtons button : XBOXButtons.values()) {
                int padButton = button.getButton();
                if (padButton != -1) {
                    buttons[button.getXboxButtonIndex()] = this.currentController.getButton(padButton);
                }

                AxisDirection axisDirection = button.getAxisDirection();
                if (axisDirection != null) {
                    buttons[button.getXboxButtonIndex()] = false;
                }
            }

            AxisDirection[] axisDirections = directions != null ?
                directions :
                AxisDirection.fromAnalog(
                    this.currentController.getAxis(6),
                    this.currentController.getAxis(7));

            for (AxisDirection pov : axisDirections) {
                buttons[XBOXButtons.getButtonFor(pov).getXboxButtonIndex()] = true;
            }
            this.state = this.state.withButtonStates(buttons).withPacket(this.state.getPacket() + 1);
        }
    }

    public static void initialize() {
        Controllers.addListener(INSTANCE);
        Gdx.app.log("Controllers", "# of controllers plugged in: " + Controllers.getControllers().size);
        if (Controllers.getControllers().size > 0) {
            INSTANCE.currentController = Controllers.getControllers().first();
        }
    }

    public ControllerState getState() {
        return this.state;
    }
}
