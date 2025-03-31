package org.jrgss.api.xbox;

public enum AxisDirection {
    NORTH,
    SOUTH,
    EAST,
    WEST;
    private static final AxisDirection[] center = {};
    private static final AxisDirection[] north = {NORTH};
    private static final AxisDirection[] south = {SOUTH};
    private static final AxisDirection[] east = {EAST};
    private static final AxisDirection[] west = {WEST};
    private static final AxisDirection[] northEast = {NORTH, EAST};
    private static final AxisDirection[] northWest = {NORTH, WEST};
    private static final AxisDirection[] southEast = {SOUTH, EAST};
    private static final AxisDirection[] southWest = {SOUTH, WEST};
    private static final float DEFAULT_DEADZONE = 0.10F;


    public static AxisDirection[] fromAnalog(float x, float y) {
        return fromAnalog(x, y, DEFAULT_DEADZONE, DEFAULT_DEADZONE);
    }

    public static AxisDirection[] fromAnalog(float x, float y, float deadzoneX, float deadzoneY) {
        if (Math.abs(x) < deadzoneX) {
            if (Math.abs(y) < deadzoneY) {
                return AxisDirection.center;
            } else {
                return y < 0 ? AxisDirection.north : AxisDirection.south;
            }
        } else if (x < 0) {
            if (Math.abs(y) < deadzoneY) {
                return AxisDirection.west;
            } else {
                return y < 0 ? AxisDirection.northWest : AxisDirection.southWest;
            }
        } else if (Math.abs(y) < deadzoneY) {
            return AxisDirection.east;
        } else {
            return y < 0 ? AxisDirection.northEast : AxisDirection.southEast;
        }
    }
}

