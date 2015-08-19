package org.jrgss.api.win32;

import org.jrgss.JRubyUtil;
import org.jruby.RubyFixnum;
import org.jruby.RubyString;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by matt on 2/7/15.
 */
public interface DLLImpl {
    public IRubyObject call(Win32API api, ThreadContext context, IRubyObject[] args);
}
