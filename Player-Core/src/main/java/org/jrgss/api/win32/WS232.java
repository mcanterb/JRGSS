package org.jrgss.api.win32;

import com.badlogic.gdx.Gdx;
import org.jruby.Ruby;
import org.jruby.RubyString;
import org.jruby.runtime.builtin.IRubyObject;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.*;
import java.util.Iterator;

import static org.jrgss.api.win32.Win32Util.*;

/**
 * Created by matt on 10/1/15.
 */
public class WS232 {

    private static long lastError = 0;

    private static void resetError() {
        lastError = 0;
    }

    @Win32Function(dll="ws2_32", name="socket", spec="lll")
    public static final DLLImpl socket = (api, ctx, args) -> {
        resetError();
        System.out.printf("For Socket we got %d, %d, %d\n", getInt(args[0]), getInt(args[1]), getInt(args[2]));
        if(getInt(args[0]) != 2) throw new UnsupportedOperationException("not implemented");
        if(getInt(args[1]) != 1) throw new UnsupportedOperationException("not implemented");
        if(getInt(args[2]) != 6) throw new UnsupportedOperationException("not implemented");
        return rubyNum(newPointer(new Winsocket()));
    };

    @Win32Function(dll="ws2_32", name="ioctlsocket", spec="llp")
    public static final DLLImpl ioctlsocket = (api, ctx, args) -> {
        resetError();
        return rubyNum(0);
    };

    @Win32Function(dll="ws2_32", name="gethostbyname", spec="p")
    public static final DLLImpl gethostbyname = (api, ctx, args) -> {
        resetError();
        try{
            String hostname = getString(args[0]);
            InetAddress address = InetAddress.getByName(hostname);
            System.out.printf("We got ip %s, for host %s\n", address.getHostAddress(), hostname);
            return buildHostStruct(hostname, address);
        } catch(UnknownHostException e) {
            lastError = 11001;
            return rubyNum(0);
        }
    };

    @Win32Function(dll = "ws2_32", name="connect", spec="ppl")
    public static final DLLImpl connect = (api, ctx, args) -> {
        resetError();
        Winsocket winsocket = getPointer(getInt(args[0]));
        ByteBuffer name = getBytes(args[1]);
        int len = getInt(args[2]);

        if(name.getShort() != 2) {
            throw new UnsupportedOperationException("Only IPv4 is Supported");
        }
        name.order(ByteOrder.BIG_ENDIAN);
        int port = name.getShort();
        name.order(ByteOrder.LITTLE_ENDIAN);
        byte[] addr = new byte[4];
        name.get(addr);
        try {
            winsocket.connect(Inet4Address.getByAddress(addr), port);
        }catch (UnknownHostException e) {
            lastError = 10065;
            return rubyNum(-1);
        }
        lastError = 10035;
        return rubyNum(-1);
    };

    //Win32API: Returning stub for Win32API(dll=ws2_32, func=select, spec=lpppp, ret=l, impl=null)
    @Win32Function(dll = "ws2_32", name = "select", spec="lpppp")
    public static final DLLImpl select = (api, ctx, args) -> {
        resetError();
        int result = 0;

        ByteBuffer timeoutBuff = getBytes(args[4]);
        long timeout = timeoutBuff.getInt()*1000;
        timeout+=(timeoutBuff.getInt()/1000);
        if(!isNull(args[1])) {
            ByteBuffer fds = getBytes(args[1]);
            int len = fds.getInt();
            for (int i = 0; i < len; i++) {
                Winsocket winsocket = getPointer(fds.getInt());
                if (winsocket.selectRead(timeout)) result++;
            }
        }
        if(!isNull(args[2])) {
            ByteBuffer fds = getBytes(args[2]);
            int len = fds.getInt();
            for (int i = 0; i < len; i++) {
                Winsocket winsocket = getPointer(fds.getInt());
                if (winsocket.selectWrite(timeout)) result++;
            }
        }
        return rubyNum(result);
    };

    //Win32API: Returning stub for Win32API(dll=ws2_32, func=send, spec=ppll, ret=l, impl=null)
    @Win32Function(dll = "ws2_32", name="send", spec="ppll")
    public static final DLLImpl send = (api, ctx, args) -> {
        resetError();
        Winsocket winsocket = getPointer(getInt(args[0]));
        ByteBuffer data = getBytes(args[1]);
        int len = getInt(args[2]);
        data.limit(data.position()+len);
        try {
            int ret = winsocket.send(data);
            Gdx.app.log("WS232", "Sent "+ret+"/"+len+" bytes.");
            return rubyNum(ret);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    };

    //Win32API: Returning stub for Win32API(dll=ws2_32, func=WSAGetLastError, spec=, ret=l, impl=null)
    @Win32Function(dll = "ws2_32", name = "WSAGetLastError", spec = "")
    public static final DLLImpl WSAGetLastError = (api, ctx, args) -> {
        return rubyNum(lastError);
    };

    //Win32API: Returning stub for Win32API(dll=ws2_32, func=closesocket, spec=p, ret=l, impl=null)
    @Win32Function(dll="ws2_32", name = "closesocket", spec="p")
    public static final DLLImpl closesocket = (api, ctx, args) -> {
        resetError();
        Winsocket winsocket = getPointer(getInt(args[0]));
        winsocket.close();
        return rubyNum(0);
    };

    private static IRubyObject buildHostStruct(String hostName, InetAddress addr) {
        ByteBuffer out = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);
        out.putInt(newPointer(rubyString(hostName)));
        out.putInt(0); //Dont care about h_aliases
        out.putShort((short) 0); //Dont care about h_addrtype
        out.putShort((short) 4); //We will always use IPv4

        ByteBuffer addrList = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);;
        addrList.putInt(newPointer(rubyString(addr.getAddress())));
        addrList.putInt(0);
        out.putInt(newPointer(rubyString(addrList)));
        return rubyNum(newPointer(rubyString(out)));
    }


    private static class Winsocket {

        SocketChannel socketChannel;
        Selector readSelector;
        Selector writeSelector;

        public Winsocket() {
            try{
                socketChannel = SocketChannel.open();
                socketChannel.configureBlocking(false);
                readSelector = Selector.open();
                writeSelector = Selector.open();
                socketChannel.register(writeSelector, SelectionKey.OP_CONNECT|SelectionKey.OP_WRITE);
                socketChannel.register(readSelector, SelectionKey.OP_READ);
            }catch (IOException io) {
                lastError = 20;
            }
        }

        public void close() {
            try {
                readSelector.close();
                writeSelector.close();
                socketChannel.close();
            }catch (Exception e) {
                Gdx.app.error("WS232", "Failed to close socket!", e);
            }
        }

        public boolean selectWrite(long timeout) {
            int val;
            try{
                if(timeout != 0) {
                    val = writeSelector.select(timeout);
                } else {
                    val = writeSelector.selectNow();
                }
            } catch (IOException io) {
                throw new RuntimeException(io);
            }
            return val != 0;
        }

        public boolean selectRead(long timeout) {
            int val;
            try{
                if(timeout != 0) {
                    val = readSelector.select(timeout);
                } else {
                    val = readSelector.selectNow();
                }
            } catch (IOException io) {
                throw new RuntimeException(io);
            }
            return val != 0;
        }

        public int send(ByteBuffer data) throws IOException {
            Iterator<SelectionKey> iterator = writeSelector.selectedKeys().iterator();
            while(iterator.hasNext()) {
                SelectionKey key = iterator.next();
                if(key.isConnectable()) {
                    if(socketChannel.isConnectionPending()) {
                        socketChannel.finishConnect();
                    }
                    iterator.remove();
                }
                if(key.isWritable()) {
                    iterator.remove();
                }
            }
            return socketChannel.write(data);
        }

        public int recv(ByteBuffer data) throws IOException {
            Iterator<SelectionKey> iterator = readSelector.selectedKeys().iterator();
            while(iterator.hasNext()) {
                SelectionKey key = iterator.next();
                if(key.isReadable()) {
                    iterator.remove();
                }
            }
            return socketChannel.read(data);
        }

        public void connect(final InetAddress ip, final int port) {
            try {
                socketChannel.connect(new InetSocketAddress(ip, port));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
