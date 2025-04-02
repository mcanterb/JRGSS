package org.jrgss.util;

import com.google.common.base.Charsets;
import org.jrgss.OS;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.ByteBuffer;

public final class SystemArchitecture {
    public static final boolean IS_64BIT;
    public static final int LONG_SIZE;

    static {
        IS_64BIT = System.getProperty("sun.arch.data.model").equals("64");

        if (IS_64BIT && OS.CURRENT_OS != OS.WINDOWS) {
            LONG_SIZE = 8;
        } else {
            LONG_SIZE = 4;
        }
    }

    private SystemArchitecture() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static Unsafe getUnsafe() {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            return (Unsafe) theUnsafe.get(null);
        } catch (Exception var1) {
            throw new RuntimeException(var1);
        }
    }

    public static long getPtr(long address) {
        Unsafe unsafe = getUnsafe();
        return IS_64BIT ? unsafe.getLong(address) : unsafe.getInt(address);
    }

    public static String getStringForAddress(long address, ByteBuffer bb) {
        Unsafe unsafe = getUnsafe();
        ((Buffer) bb).clear();

        byte b;
        while ((b = unsafe.getByte(address)) != 0 && bb.hasRemaining()) {
            bb.put(b);
            address++;
        }

        ((Buffer) bb).limit(bb.position()).rewind();
        return Charsets.US_ASCII.decode(bb).toString();
    }
}
