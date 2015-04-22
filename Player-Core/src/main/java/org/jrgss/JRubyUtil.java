package org.jrgss;

import com.sun.org.apache.xpath.internal.operations.Bool;
import org.jrgss.api.win32.Win32API;
import org.jruby.Ruby;
import org.jruby.RubyBoolean;
import org.jruby.RubyFixnum;
import org.jruby.RubyString;
import org.jruby.runtime.builtin.IRubyObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by matt on 4/21/15.
 */
public interface JRubyUtil {
    static String getString(IRubyObject obj) {
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

    static long getLong(IRubyObject obj) {
        if(obj instanceof RubyFixnum) {
            return ((RubyFixnum) obj).getLongValue();
        }
        throw new IllegalArgumentException("Cannot convert "+obj.getClass()+" to Long!");
    }

    static int getInt(IRubyObject obj) {
        if(obj instanceof RubyFixnum) {
            return (int)((RubyFixnum) obj).getLongValue();
        }
        throw new IllegalArgumentException("Cannot convert "+obj.getClass()+" to Integer!");
    }

    static ByteBuffer getBytes(IRubyObject obj) {
        if(obj instanceof RubyString) {
            return ByteBuffer.wrap(((RubyString) obj).getByteList().getUnsafeBytes()).order(ByteOrder.LITTLE_ENDIAN);
        }
        throw new IllegalArgumentException("Cannot convert "+obj.getClass()+" to Bytes!");
    }

    static boolean getBool(IRubyObject obj) {
        if(obj instanceof RubyBoolean) {
            return obj.isTrue();
        }
        throw new IllegalArgumentException("Cannot convert "+obj.getClass()+" to Boolean!");
    }

    static IRubyObject rubyNum(long value) {
        return new RubyFixnum(Ruby.getGlobalRuntime(), value);
    }

    static IRubyObject rubyBool(boolean value) {
        return RubyBoolean.newBoolean(Ruby.getGlobalRuntime(), value);
    }
}
