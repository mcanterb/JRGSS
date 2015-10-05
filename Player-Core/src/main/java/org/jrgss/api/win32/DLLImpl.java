package org.jrgss.api.win32;

import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Created by matt on 2/7/15.
 */
public interface DLLImpl {
    public IRubyObject call(Win32API api, ThreadContext context, IRubyObject[] args);
}
