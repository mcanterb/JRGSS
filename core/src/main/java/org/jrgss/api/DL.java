package org.jrgss.api;

import org.jrgss.api.win32.Win32Util;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.builtin.IRubyObject;

import java.nio.ByteBuffer;

public class DL {
    @JRubyMethod(
        required = 1,
        module = true
    )
    public static IRubyObject malloc(IRubyObject self, IRubyObject arg0) {
        Integer val = Win32Util.getInt(arg0);
        ByteBuffer buffer = ByteBuffer.allocate(val);
        return Win32Util.rubyNum(Win32Util.newPointer(buffer).intValue());
    }
}
