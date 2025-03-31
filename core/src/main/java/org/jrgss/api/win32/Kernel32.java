package org.jrgss.api.win32;

import com.badlogic.gdx.Gdx;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import org.ini4j.Wini;
import org.jrgss.FileUtil;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;

public class Kernel32 {
   @Win32Function(
      dll = "kernel32",
      name = "GetPrivateProfileString",
      spec = "ppppip"
   )
   public static final DLLImpl GetPrivateProfileString = (api, context, args) -> {
      String section = Win32Util.getString(args[0]);
      String key = Win32Util.getString(args[1]);
      RubyString defaultValue = (RubyString)args[2];
      RubyString buffer = (RubyString)args[3];
      int bufferSize = Win32Util.getInt(args[4]);
      String fileName = Win32Util.getString(args[5]);
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
            if (ini.get(section, key) != null) {
               result = ini.get(section, key);
            }
         } catch (Exception var13) {
            Gdx.app.log("Kernel32", "Failed to read from ini!", var13);
         }
      }

      buffer.getByteList().unshare();
      ByteBuffer bb = ByteBuffer.wrap(buffer.getByteList().getUnsafeBytes()).order(ByteOrder.LITTLE_ENDIAN);
      byte[] defaultBytes = Charset.forName("UTF-8").encode(result).array();
      bb.put(defaultBytes);
      return Win32Util.rubyNum(Math.min(defaultBytes.length, bufferSize));
   };
   @Win32Function(
      dll = "kernel32",
      name = "WritePrivateProfileString",
      spec = "pppp"
   )
   public static final DLLImpl WritePrivateProfileString = (api, context, args) -> {
      String section = Win32Util.getString(args[0]);
      String key = Win32Util.getString(args[1]);
      RubyString value = (RubyString)args[2];
      String fileName = Win32Util.getString(args[3]);
      File f = new File(FileUtil.localDirectory + File.separator + fileName);
      File out = new File(FileUtil.localDirectory + File.separator + fileName);
      if (!out.exists()) {
         try {
            out.createNewFile();
         } catch (Exception var11) {
            Gdx.app.log("Kernel32", "Failed to create empty file. This is likely a huge problem!", var11);
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
      } catch (Exception var10) {
         Gdx.app.log("Kernel32", "Failed to Write to ini!", var10);
      }

      return Win32Util.rubyNum(0L);
   };
   @Win32Function(
      dll = "kernel32",
      name = "RtlMoveMemory",
      spec = "ppl"
   )
   public static final DLLImpl RtlMoveMemory = (api, context, args) -> {
      long len = Win32Util.getLong(args[2]);
      ByteBuffer destBuffer = Win32Util.getBytes(args[0]);
      ByteBuffer srcBuffer;
      if (args[1] instanceof RubyNumeric) {
         srcBuffer = Win32Util.getBytes(Win32Util.getPointer(Win32Util.getInt(args[1])));
      } else {
         srcBuffer = Win32Util.getBytes(args[1]);
      }

      destBuffer.put(srcBuffer.array(), 0, (int)Math.min((long)srcBuffer.capacity(), len));
      return Win32Util.rubyNil();
   };
   @Win32Function(
      dll = "kernel32",
      name = "ExitProcess",
      spec = "i"
   )
   public static final DLLImpl ExitProcess = (api, context, args) -> {
      int result = Win32Util.getInt(args[0]);
      System.exit(result);
      return Win32Util.rubyNil();
   };
}
