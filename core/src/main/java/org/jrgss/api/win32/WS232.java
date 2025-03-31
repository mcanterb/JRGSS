package org.jrgss.api.win32;

import com.badlogic.gdx.Gdx;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import org.jruby.runtime.builtin.IRubyObject;

public class WS232 {
   private static long lastError = 0L;
   @Win32Function(
      dll = "ws2_32",
      name = "socket",
      spec = "lll"
   )
   public static final DLLImpl socket = (api, ctx, args) -> {
      resetError();
      System.out.printf("For Socket we got %d, %d, %d\n", Win32Util.getInt(args[0]), Win32Util.getInt(args[1]), Win32Util.getInt(args[2]));
      if (Win32Util.getInt(args[0]) != 2) {
         throw new UnsupportedOperationException("not implemented");
      } else if (Win32Util.getInt(args[1]) != 1) {
         throw new UnsupportedOperationException("not implemented");
      } else if (Win32Util.getInt(args[2]) != 6) {
         throw new UnsupportedOperationException("not implemented");
      } else {
         return Win32Util.rubyNum(Win32Util.newPointer(new WS232.Winsocket()).intValue());
      }
   };
   @Win32Function(
      dll = "ws2_32",
      name = "ioctlsocket",
      spec = "llp"
   )
   public static final DLLImpl ioctlsocket = (api, ctx, args) -> {
      resetError();
      return Win32Util.rubyNum(0L);
   };
   @Win32Function(
      dll = "ws2_32",
      name = "gethostbyname",
      spec = "p"
   )
   public static final DLLImpl gethostbyname = (api, ctx, args) -> {
      resetError();

      try {
         String hostname = Win32Util.getString(args[0]);
         InetAddress address = InetAddress.getByName(hostname);
         System.out.printf("We got ip %s, for host %s\n", address.getHostAddress(), hostname);
         return buildHostStruct(hostname, address);
      } catch (UnknownHostException var5) {
         lastError = 11001L;
         return Win32Util.rubyNum(0L);
      }
   };
   @Win32Function(
      dll = "ws2_32",
      name = "connect",
      spec = "ppl"
   )
   public static final DLLImpl connect = (api, ctx, args) -> {
      resetError();
      WS232.Winsocket winsocket = Win32Util.getPointer(Win32Util.getInt(args[0]));
      ByteBuffer name = Win32Util.getBytes(args[1]);
      int len = Win32Util.getInt(args[2]);
      if (name.getShort() != 2) {
         throw new UnsupportedOperationException("Only IPv4 is Supported");
      } else {
         name.order(ByteOrder.BIG_ENDIAN);
         int port = name.getShort();
         name.order(ByteOrder.LITTLE_ENDIAN);
         byte[] addr = new byte[4];
         name.get(addr);

         try {
            winsocket.connect(Inet4Address.getByAddress(addr), port);
         } catch (UnknownHostException var9) {
            lastError = 10065L;
            return Win32Util.rubyNum(-1L);
         }

         lastError = 10035L;
         return Win32Util.rubyNum(-1L);
      }
   };
   @Win32Function(
      dll = "ws2_32",
      name = "select",
      spec = "lpppp"
   )
   public static final DLLImpl select = (api, ctx, args) -> {
      resetError();
      int result = 0;
      ByteBuffer timeoutBuff = Win32Util.getBytes(args[4]);
      long timeout = timeoutBuff.getInt() * 1000;
      timeout += timeoutBuff.getInt() / 1000;
      if (!Win32Util.isNull(args[1])) {
         ByteBuffer fds = Win32Util.getBytes(args[1]);
         int len = fds.getInt();

         for (int i = 0; i < len; i++) {
            WS232.Winsocket winsocket = Win32Util.getPointer(fds.getInt());
            if (winsocket.selectRead(timeout)) {
               result++;
            }
         }
      }

      if (!Win32Util.isNull(args[2])) {
         ByteBuffer fds = Win32Util.getBytes(args[2]);
         int len = fds.getInt();

         for (int ix = 0; ix < len; ix++) {
            WS232.Winsocket winsocket = Win32Util.getPointer(fds.getInt());
            if (winsocket.selectWrite(timeout)) {
               result++;
            }
         }
      }

      return Win32Util.rubyNum(result);
   };
   @Win32Function(
      dll = "ws2_32",
      name = "send",
      spec = "ppll"
   )
   public static final DLLImpl send = (api, ctx, args) -> {
      resetError();
      WS232.Winsocket winsocket = Win32Util.getPointer(Win32Util.getInt(args[0]));
      ByteBuffer data = Win32Util.getBytes(args[1]);
      int len = Win32Util.getInt(args[2]);
      ((Buffer)data).limit(data.position() + len);

      try {
         int ret = winsocket.send(data);
         Gdx.app.log("WS232", "Sent " + ret + "/" + len + " bytes.");
         return Win32Util.rubyNum(ret);
      } catch (IOException var7) {
         throw new RuntimeException(var7);
      }
   };
   @Win32Function(
      dll = "ws2_32",
      name = "WSAGetLastError",
      spec = ""
   )
   public static final DLLImpl WSAGetLastError = (api, ctx, args) -> Win32Util.rubyNum(lastError);
   @Win32Function(
      dll = "ws2_32",
      name = "closesocket",
      spec = "p"
   )
   public static final DLLImpl closesocket = (api, ctx, args) -> {
      resetError();
      WS232.Winsocket winsocket = Win32Util.getPointer(Win32Util.getInt(args[0]));
      winsocket.close();
      return Win32Util.rubyNum(0L);
   };

   private static void resetError() {
      lastError = 0L;
   }

   private static IRubyObject buildHostStruct(String hostName, InetAddress addr) {
      ByteBuffer out = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);
      out.putInt(Win32Util.newPointer(Win32Util.rubyString(hostName)));
      out.putInt(0);
      out.putShort((short)0);
      out.putShort((short)4);
      ByteBuffer addrList = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
      addrList.putInt(Win32Util.newPointer(Win32Util.rubyString(addr.getAddress())));
      addrList.putInt(0);
      out.putInt(Win32Util.newPointer(Win32Util.rubyString(addrList)));
      return Win32Util.rubyNum(Win32Util.newPointer(Win32Util.rubyString(out)).intValue());
   }

   private static class Winsocket {
      SocketChannel socketChannel;
      Selector readSelector;
      Selector writeSelector;

      public Winsocket() {
         try {
            this.socketChannel = SocketChannel.open();
            this.socketChannel.configureBlocking(false);
            this.readSelector = Selector.open();
            this.writeSelector = Selector.open();
            this.socketChannel.register(this.writeSelector, 12);
            this.socketChannel.register(this.readSelector, 1);
         } catch (IOException var2) {
            WS232.lastError = 20L;
         }
      }

      public void close() {
         try {
            this.readSelector.close();
            this.writeSelector.close();
            this.socketChannel.close();
         } catch (Exception var2) {
            Gdx.app.error("WS232", "Failed to close socket!", var2);
         }
      }

      public boolean selectWrite(long timeout) {
         int val;
         try {
            if (timeout != 0L) {
               val = this.writeSelector.select(timeout);
            } else {
               val = this.writeSelector.selectNow();
            }
         } catch (IOException var5) {
            throw new RuntimeException(var5);
         }

         return val != 0;
      }

      public boolean selectRead(long timeout) {
         int val;
         try {
            if (timeout != 0L) {
               val = this.readSelector.select(timeout);
            } else {
               val = this.readSelector.selectNow();
            }
         } catch (IOException var5) {
            throw new RuntimeException(var5);
         }

         return val != 0;
      }

      public int send(ByteBuffer data) throws IOException {
         Iterator<SelectionKey> iterator = this.writeSelector.selectedKeys().iterator();

         while (iterator.hasNext()) {
            SelectionKey key = iterator.next();
            if (key.isConnectable()) {
               if (this.socketChannel.isConnectionPending()) {
                  this.socketChannel.finishConnect();
               }

               iterator.remove();
            }

            if (key.isWritable()) {
               iterator.remove();
            }
         }

         return this.socketChannel.write(data);
      }

      public int recv(ByteBuffer data) throws IOException {
         Iterator<SelectionKey> iterator = this.readSelector.selectedKeys().iterator();

         while (iterator.hasNext()) {
            SelectionKey key = iterator.next();
            if (key.isReadable()) {
               iterator.remove();
            }
         }

         return this.socketChannel.read(data);
      }

      public void connect(InetAddress ip, int port) {
         try {
            this.socketChannel.connect(new InetSocketAddress(ip, port));
         } catch (IOException var4) {
            throw new RuntimeException(var4);
         }
      }
   }
}
