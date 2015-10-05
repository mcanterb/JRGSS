package org.jrgss.api.win32;

import com.badlogic.gdx.Gdx;
import org.ini4j.Wini;
import org.jrgss.FileUtil;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;

import static org.jrgss.api.win32.Win32Util.*;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

/**
 * Created by matt on 2/4/15.
 */
public class Kernel32 {


    //GetPrivateProfileString   = Win32API.new('kernel32', 'GetPrivateProfileString'  , 'ppppip'      , 'i')
    //GetPrivateProfileString.call(section, key.to_s, "100", buffer, buffer.size, filename)
    @Win32Function(dll = "kernel32", name = "GetPrivateProfileString", spec = "ppppip")
    public static final DLLImpl GetPrivateProfileString = (api, context, args) -> {
        String section = getString(args[0]);
        String key = getString(args[1]);
        RubyString defaultValue = (RubyString) args[2];
        RubyString buffer = (RubyString) args[3];
        int bufferSize = getInt(args[4]);
        String fileName = getString(args[5]);
        File f = new File(FileUtil.localDirectory + File.separator + fileName);
        if (!f.exists()) {
            f = new File(FileUtil.gameDirectory + File.separator + fileName);
            if (!f.exists()) {
                f = null;
            }
        }
        String result = defaultValue.asJavaString();
        if (f != null) {
            try {
                Wini ini = new Wini(f);
                if(ini.get(section, key)!=null) {
                    result = ini.get(section, key);
                }
            } catch (Exception e) {
                Gdx.app.log("Kernel32", "Failed to read from ini!", e);
            }
        }

        ByteBuffer bb = ByteBuffer.wrap(buffer.getByteList().getUnsafeBytes()).order(ByteOrder.LITTLE_ENDIAN);
        byte[] defaultBytes = Charset.forName("UTF-8").encode(result).array();

        bb.put(defaultBytes);
        return rubyNum(Math.min(defaultBytes.length, bufferSize));
    };

    //  WritePrivateProfileString = Win32API.new('kernel32', 'WritePrivateProfileString', 'pppp'        , 'i')
    @Win32Function(dll = "kernel32", name = "WritePrivateProfileString", spec = "pppp")
    public static final DLLImpl WritePrivateProfileString = (api, context, args) -> {
        String section = getString(args[0]);
        String key = getString(args[1]);
        RubyString value = (RubyString) args[2];
        String fileName = getString(args[3]);
        File f = new File(FileUtil.localDirectory + File.separator + fileName);
        File out = new File(FileUtil.localDirectory + File.separator + fileName);
        if (!out.exists()) {
            try{
                out.createNewFile();
            }catch (Exception e) {
                Gdx.app.log("Kernel32", "Failed to create empty file. This is likely a huge problem!", e);
            }
            f = new File(FileUtil.gameDirectory + File.separator + fileName);
            if (!f.exists()) {
                f = out;
            }
        }
        try {
            Wini ini = new Wini(f);
            ini.put(section, key, value.asJavaString());
            ini.store(out);
        } catch (Exception e) {
            Gdx.app.log("Kernel32", "Failed to Write to ini!", e);
        }

        return rubyNum(0);
    };

    @Win32Function(dll="kernel32", name="RtlMoveMemory", spec="ppl")
    public static final DLLImpl RtlMoveMemory = (api, context, args) -> {
        long len = getLong(args[2]);

        ByteBuffer destBuffer = getBytes(args[0]);
        ByteBuffer srcBuffer;
        if(args[1] instanceof RubyNumeric) {
            srcBuffer = getBytes(getPointer(getInt(args[1])));
        } else {
            srcBuffer = getBytes(args[1]);
        }
        destBuffer.put(srcBuffer.array(), 0, (int)Math.min(srcBuffer.capacity(), len));
        return rubyNil();
    };

}
