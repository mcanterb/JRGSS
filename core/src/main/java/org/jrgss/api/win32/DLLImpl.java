package org.jrgss.api.win32;

import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public interface DLLImpl {
   IRubyObject call(Win32API var1, ThreadContext var2, IRubyObject[] var3);
}
