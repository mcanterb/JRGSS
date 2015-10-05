package org.jrgss.api.win32;

import com.sun.org.apache.xpath.internal.operations.Bool;
import org.jrgss.api.win32.Win32API;
import org.jruby.Ruby;
import org.jruby.RubyBoolean;
import org.jruby.RubyFixnum;
import org.jruby.RubyString;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by matt on 4/21/15.
 */
public class Win32Util {

    private static final WeakHashMap<Integer, Object> pointerTable = new WeakHashMap<>();
    private static final AtomicInteger nextPtr = new AtomicInteger(1);



    public static Integer newPointer(Object val) {
        Integer ptr = nextPtr.getAndIncrement();
        pointerTable.put(ptr, val);
        return ptr;
    }

    public static <T> T getPointer(Integer ptr) {
        return (T)pointerTable.get(ptr);
    }

    public static <T> void setPointer(Integer ptr, T obj) {
        pointerTable.put(ptr, obj);
    }

    public static String getString(IRubyObject obj) {
        if(obj instanceof RubyFixnum) {
            if(((RubyFixnum) obj).getLongValue() != 0) {
                throw new IllegalArgumentException("Tried passing a raw pointer that is not null!");
            }
            return null;
        }
        if(obj instanceof RubyString) {
            return obj.asJavaString();
        }
        throw new IllegalArgumentException("Cannot convert "+obj.getClass()+" to String!");
    }

    public static long getLong(IRubyObject obj) {
        if(obj instanceof RubyFixnum) {
            return ((RubyFixnum) obj).getLongValue();
        }
        throw new IllegalArgumentException("Cannot convert "+obj.getClass()+" to Long!");
    }

    public static int getInt(IRubyObject obj) {
        if(obj instanceof RubyFixnum) {
            return (int)((RubyFixnum) obj).getLongValue();
        }
        throw new IllegalArgumentException("Cannot convert "+obj.getClass()+" to Integer!");
    }

    public static boolean isNull(IRubyObject obj) {
        return obj instanceof RubyFixnum && getInt(obj) == 0;
    }

    public static ByteBuffer getBytes(IRubyObject obj) {
        if(obj instanceof RubyString) {
            return ByteBuffer.wrap(((RubyString) obj).getByteList().getUnsafeBytes()).order(ByteOrder.LITTLE_ENDIAN);
        }
        throw new IllegalArgumentException("Cannot convert "+obj.getClass()+" to Bytes!");
    }

    public static boolean getBool(IRubyObject obj) {
        if(obj instanceof RubyBoolean) {
            return obj.isTrue();
        }
        throw new IllegalArgumentException("Cannot convert "+obj.getClass()+" to Boolean!");
    }

    public static IRubyObject rubyString(String str) {
        return Ruby.getGlobalRuntime().newString(str);
    }

    public static IRubyObject rubyString(byte[] str) {
        ByteList byteList = new ByteList(str.length);
        byteList.setUnsafeBytes(str);
        byteList.setRealSize(str.length);
        return Ruby.getGlobalRuntime().newString(byteList);
    }

    public static IRubyObject rubyString(ByteBuffer str) {
        ByteList byteList = new ByteList(str.array().length);
        byteList.setUnsafeBytes(str.array());
        byteList.setRealSize(str.array().length);
        RubyString result = Ruby.getGlobalRuntime().newString(byteList);
        return result;
    }

    public static IRubyObject rubyNum(long value) {
        return new RubyFixnum(Ruby.getGlobalRuntime(), value);
    }

    public static IRubyObject rubyNil() {
        return Ruby.getGlobalRuntime().getNil();
    }

    public static IRubyObject rubyBool(boolean value) {
        return RubyBoolean.newBoolean(Ruby.getGlobalRuntime(), value);
    }
}
