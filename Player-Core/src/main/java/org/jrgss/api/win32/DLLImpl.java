package org.jrgss.api.win32;

import org.jruby.RubyFixnum;
import org.jruby.RubyString;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by matt on 2/7/15.
 */
public abstract class DLLImpl {
    public abstract IRubyObject call(Win32API api, ThreadContext context, IRubyObject[] args);

    protected static String getString(IRubyObject obj) {
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

    protected static long getLong(IRubyObject obj) {
        if(obj instanceof RubyFixnum) {
            return ((RubyFixnum) obj).getLongValue();
        }
        throw new IllegalArgumentException("Cannot convert "+obj.getClass()+" to Long!");
    }

    protected static int getInt(IRubyObject obj) {
        if(obj instanceof RubyFixnum) {
            return (int)((RubyFixnum) obj).getLongValue();
        }
        throw new IllegalArgumentException("Cannot convert "+obj.getClass()+" to Integer!");
    }

    protected static ByteBuffer getBytes(IRubyObject obj) {
        if(obj instanceof RubyString) {
            return ByteBuffer.wrap(((RubyString) obj).getByteList().getUnsafeBytes()).order(ByteOrder.LITTLE_ENDIAN);
        }
        throw new IllegalArgumentException("Cannot convert "+obj.getClass()+" to Bytes!");
    }

    protected static IRubyObject rubyNum(long value) {
        return new RubyFixnum(Win32API.runtime, value);
    }
}
