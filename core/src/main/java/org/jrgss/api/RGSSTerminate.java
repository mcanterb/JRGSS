package org.jrgss.api;

public class RGSSTerminate extends RuntimeException {
    public RGSSTerminate() {
        super("Application terminated");
    }
}
