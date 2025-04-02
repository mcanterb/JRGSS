package org.jrgss.api;

import org.jrgss.api.win32.Win32Util;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.nio.ByteBuffer;

public class CPtr extends RubyObject {
    static Ruby runtime;
    static RubyClass rubyClass;
    private Integer ptr;

    public CPtr(Ruby runtime, RubyClass rubyClass) {
        super(runtime, rubyClass);
    }

    @JRubyMethod(
        required = 1,
        optional = 2
    )
    public void initialize(ThreadContext context, IRubyObject[] args) {
        this.ptr = Win32Util.getInt(args[0]);
    }

    @JRubyMethod(
        name = {"to_i"}
    )
    public IRubyObject toInteger(ThreadContext context) {
        return Win32Util.rubyNum(this.ptr.intValue());
    }

    @JRubyMethod(
        name = {"[]"},
        required = 1
    )
    public IRubyObject arrGet(ThreadContext context, IRubyObject arg0) {
        ByteBuffer buffer = Win32Util.getPointer(this.ptr);
        return Win32Util.rubyNum(buffer.get(Win32Util.getInt(arg0)));
    }
}
