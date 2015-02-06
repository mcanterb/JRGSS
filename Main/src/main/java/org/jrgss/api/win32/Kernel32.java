package org.jrgss.api.win32;

import com.badlogic.gdx.Gdx;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyString;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

import java.awt.*;
import java.io.File;
import java.net.URI;
import java.nio.ByteBuffer;

/**
 * Created by matt on 2/4/15.
 */
public class Kernel32 {


    //GetPrivateProfileString   = Win32API.new('kernel32', 'GetPrivateProfileString'  , 'ppppip'      , 'i')
    //GetPrivateProfileString.call(section, key.to_s, "100", buffer, buffer.size, filename)
    @Win32Function(dll="kernel32", name="GetPrivateProfileString", spec = "ppppip")
    public static final DLLImpl GetPrivateProfileString = new DLLImpl() {
        public IRubyObject call(Win32API api, ThreadContext context, IRubyObject[] args) {
            String section = getString(args[0]);
            String key = getString(args[1]);
            RubyString defaultValue = (RubyString)args[2];
            RubyString buffer = (RubyString)args[3];
            int bufferSize = getInt(args[4]);
            String fileName = getString(args[5]);

            ByteList backingList = buffer.getByteList();
            byte[] defaultBytes = defaultValue.getBytes();

            backingList.replace(0, Math.min(defaultBytes.length, bufferSize), defaultBytes);
            return rubyNum(Math.min(defaultBytes.length, bufferSize));
        }
    };

}
