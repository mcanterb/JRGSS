package org.jrgss.api.win32;

import com.badlogic.gdx.Gdx;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.UUID;

public class Ole32 {
   @Win32Function(
      dll = "ole32",
      name = "CoCreateGuid",
      spec = "p"
   )
   public static final DLLImpl CoCreateGuid = (api, ctx, args) -> {
      Gdx.app.log("Ole32", "Creating GUID");
      ByteBuffer out = Win32Util.getBytes(args[0]);
      UUID uuid = UUID.randomUUID();
      int ptr = Win32Util.newPointer(uuid);
      out.putInt(ptr);
      return Win32Util.rubyNum(0L);
   };
   @Win32Function(
      dll = "ole32",
      name = "StringFromGUID2",
      spec = "ppl"
   )
   public static final DLLImpl StringFromGUID2 = (api, ctx, args) -> {
      ByteBuffer guid = Win32Util.getBytes(args[0]);
      ByteBuffer out = Win32Util.getBytes(args[1]);
      int len = Win32Util.getInt(args[2]);
      Gdx.app.log("Ole32", "we got length " + len);
      if (len < 39) {
         return Win32Util.rubyNum(0L);
      } else {
         UUID uuid = Win32Util.getPointer(guid.getInt(0));
         byte[] str = Charset.forName("UTF-8").encode("{" + uuid.toString() + "}").array();

         for (byte b : str) {
            out.put(b);
            out.put((byte)0);
         }

         Gdx.app.log("Ole32", "UUID = " + uuid.toString());
         return Win32Util.rubyNum(39L);
      }
   };
}
