package org.jrgss;

import com.badlogic.gdx.Gdx;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Timer {
    public static final int THRESHOLD = 300;
    static Map<String, Timer> TIMERS = new HashMap<>();
    private final String name;
    private long totalTime;
    private int count;

    private Timer(String name) {
        this.name = name;
    }

    public static Timer timerFor(String ident) {
        if (TIMERS.containsKey(ident)) {
            return TIMERS.get(ident);
        } else {
            Timer t = new Timer(ident);
            TIMERS.put(ident, t);
            return t;
        }
    }

    public static void increment() {
        TIMERS.values().forEach(Timer::incrementInternal);
    }

    public void time(Runnable runnable) {
        long start = System.nanoTime();
        runnable.run();
        this.totalTime = this.totalTime + (System.nanoTime() - start);
    }

    private void incrementInternal() {
        this.count++;
        if (this.count == 300) {
            Gdx.app.log(this.name, "Total time is " + this.totalTime + "ns");
            Gdx.app.log(this.name, "Took avg of " + TimeUnit.NANOSECONDS.toMicros(this.totalTime / this.count) + "us");
            this.totalTime = 0L;
            this.count = 0;
        }
    }
}
