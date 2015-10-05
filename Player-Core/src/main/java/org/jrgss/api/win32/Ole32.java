package org.jrgss.api.win32;

import com.badlogic.gdx.Gdx;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.UUID;
import java.util.WeakHashMap;
import static org.jrgss.api.win32.Win32Util.*;

/**
 * Created by matt on 10/1/15.
 */
public class Ole32 {

    //Win32API(dll=ole32, func=CoCreateGuid, spec=p, ret=l, impl=null)
    @Win32Function(dll = "ole32", name="CoCreateGuid", spec = "p")
    public static final DLLImpl CoCreateGuid = (api, ctx, args) -> {
        Gdx.app.log("Ole32", "Creating GUID");
        ByteBuffer out = getBytes(args[0]);

        UUID uuid = UUID.randomUUID();
        int ptr = newPointer(uuid);
        out.putInt(ptr);
        return rubyNum(0);
    };


    //Win32API(dll=ole32, func=StringFromGUID2, spec=ppl, ret=l, impl=null)
    @Win32Function(dll = "ole32", name="StringFromGUID2", spec = "ppl")
    public static final DLLImpl StringFromGUID2 = (api, ctx, args) -> {
        ByteBuffer guid = getBytes(args[0]);
        ByteBuffer out = getBytes(args[1]);
        int len = getInt(args[2]);
        Gdx.app.log("Ole32", "we got length "+len);
        if(len < 39) return rubyNum(0);

        UUID uuid = getPointer(guid.getInt(0));
        byte[] str = Charset.forName("UTF-8").encode("{"+uuid.toString()+"}").array();
        for(byte b : str) {
            out.put(b);
            out.put((byte)0);
        }
        Gdx.app.log("Ole32", "UUID = "+uuid.toString());
        return rubyNum(39);
    };
}
