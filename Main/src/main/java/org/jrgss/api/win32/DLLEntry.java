package org.jrgss.api.win32;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jruby.RubyFixnum;
import org.jruby.RubyString;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * @author matt
 * @date 1/26/15
 */
@Data
public class DLLEntry {
    final String dllName;
    final String funcName;
    final String spec;
}

