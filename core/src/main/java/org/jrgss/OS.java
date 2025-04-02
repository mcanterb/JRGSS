package org.jrgss;

public enum OS {
    MAC,
    LINUX,
    WINDOWS,
    OTHER;

    public static final OS CURRENT_OS;
    public static final boolean IS_64BIT;
    public static final int LONG_SIZE;

    static {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("nux") || osName.contains("nix")) {
            CURRENT_OS = LINUX;
        } else if (osName.contains("mac")) {
            CURRENT_OS = MAC;
        } else if (osName.contains("win")) {
            CURRENT_OS = WINDOWS;
        } else {
            CURRENT_OS = OTHER;
        }

        IS_64BIT = System.getProperty("sun.arch.data.model").equals("64");

        if (IS_64BIT && CURRENT_OS != WINDOWS) {
            LONG_SIZE = 8;
        } else {
            LONG_SIZE = 4;
        }
    }
}
