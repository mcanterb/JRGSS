package org.jrgss.api;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

@JRubyClass(
   name = {"Table"}
)
public class Table extends RubyObject {
   static Ruby runtime;
   static RubyClass rubyClass;
   ShortBuffer values;
   int dim1;
   int dim2 = 1;
   int dim3 = 1;
   private static final byte[] charToByte = new byte[256];
   static final char[] byteToChar = new char[16];

   @JRubyMethod(
      module = true
   )
   public static IRubyObject _load(ThreadContext context, IRubyObject self, IRubyObject rubyObj) {
      RubyString rubyString = rubyObj.asString();
      byte[] b = rubyString.getBytes();
      ByteBuffer buffer = ByteBuffer.wrap(b);
      buffer.order(ByteOrder.LITTLE_ENDIAN);
      buffer.getInt();
      int dim1 = buffer.getInt();
      int dim2 = buffer.getInt();
      int dim3 = buffer.getInt();
      buffer.getInt();
      Table t = new Table(context.getRuntime(), context.getRuntime().getClass("Table"));
      t.initialize(
         context,
         new RubyObject[]{new RubyFixnum(context.getRuntime(), dim1), new RubyFixnum(context.getRuntime(), dim2), new RubyFixnum(context.getRuntime(), dim3)}
      );
      t.values.put(buffer.asShortBuffer());
      ((Buffer)t.values).rewind();
      return t;
   }

   @JRubyMethod
   public IRubyObject _dump(IRubyObject level) {
      ByteBuffer b = ByteBuffer.allocate(20 + this.dim1 * this.dim2 * this.dim3 * 2);
      b.order(ByteOrder.LITTLE_ENDIAN);
      b.putInt(20 + this.dim1 * this.dim2 * this.dim3 * 2);
      b.putInt(this.dim1);
      b.putInt(this.dim2);
      b.putInt(this.dim3);
      b.putInt(this.dim1 * this.dim2 * this.dim3);
      ShortBuffer shortBuffer = b.asShortBuffer();
      shortBuffer.put(this.values.duplicate());
      ((Buffer)b).rewind();
      return new RubyString(runtime, runtime.getString(), b.array());
   }

   @JRubyMethod(
      name = {"[]"},
      required = 1,
      optional = 2
   )
   public IRubyObject arrGet(ThreadContext context, IRubyObject[] args) {
      int dim1 = (int)args[0].convertToInteger().getLongValue();
      int dim2 = (int)(args.length > 1 ? args[1].convertToInteger().getLongValue() : 0L);
      int dim3 = (int)(args.length > 2 ? args[2].convertToInteger().getLongValue() : 0L);
      Short val = this.get(dim1, dim2, dim3);
      return (IRubyObject)(val == null ? context.getRuntime().getNil() : new RubyFixnum(context.getRuntime(), this.get(dim1, dim2, dim3).shortValue()));
   }

   @JRubyMethod(
      name = {"[]="},
      required = 2,
      optional = 3
   )
   public IRubyObject arrSet(ThreadContext context, IRubyObject[] args) {
      int dim1 = (int)args[0].convertToInteger().getLongValue();
      int dim2 = (int)(args.length > 2 ? args[1].convertToInteger().getLongValue() : 0L);
      int dim3 = (int)(args.length > 3 ? args[2].convertToInteger().getLongValue() : 0L);
      int value = (int)args[args.length - 1].convertToInteger().getLongValue();
      this.set(dim1, dim2, dim3, (short)value);
      return context.getRuntime().getNil();
   }

   public Table(Ruby runtime, RubyClass rubyClass) {
      super(runtime, rubyClass);
   }

   @JRubyMethod(
      required = 1,
      optional = 2
   )
   public void initialize(ThreadContext context, IRubyObject[] args) {
      this.dim1 = (int)args[0].convertToInteger().getLongValue();
      this.dim2 = (int)(args.length > 1 ? args[1].convertToInteger().getLongValue() : 1L);
      this.dim3 = (int)(args.length > 2 ? args[2].convertToInteger().getLongValue() : 1L);
      this.values = ShortBuffer.allocate(this.dim1 * this.dim2 * this.dim3);
   }

   public Short get(int i, int j, int k) {
      return i < this.dim1 && j < this.dim2 && k < this.dim3 ? this.values.get(i + j * this.dim1 + k * this.dim1 * this.dim2) : null;
   }

   public void set(int i, int j, int k, short value) {
      this.values.put(i + j * this.dim1 + k * this.dim1 * this.dim2, value);
   }

   public static byte[] hexToBytes(String str) {
      str = str.replace(" ", "");
      if (str.length() % 2 == 1) {
         throw new NumberFormatException("An hex string representing bytes must have an even length");
      } else {
         byte[] bytes = new byte[str.length() / 2];

         for (int i = 0; i < bytes.length; i++) {
            byte halfByte1 = charToByte[str.charAt(i * 2)];
            byte halfByte2 = charToByte[str.charAt(i * 2 + 1)];
            if (halfByte1 == -1 || halfByte2 == -1) {
               throw new NumberFormatException("Non-hex characters in " + str);
            }

            bytes[i] = (byte)(halfByte1 << 4 | halfByte2);
         }

         return bytes;
      }
   }

   public static String bytesToHex(byte... bytes) {
      char[] c = new char[bytes.length * 2];

      for (int i = 0; i < bytes.length; i++) {
         int bint = bytes[i];
         c[i * 2] = byteToChar[(bint & 240) >> 4];
         c[1 + i * 2] = byteToChar[bint & 15];
      }

      return new String(c);
   }

   @Override
   public String toString() {
      return "Table(values=" + this.values + ", dim1=" + this.dim1 + ", dim2=" + this.dim2 + ", dim3=" + this.dim3 + ")";
   }

   static {
      for (char c = 0; c < charToByte.length; c++) {
         if (c >= '0' && c <= '9') {
            charToByte[c] = (byte)(c - '0');
         } else if (c >= 'A' && c <= 'F') {
            charToByte[c] = (byte)(c - 'A' + 10);
         } else if (c >= 'a' && c <= 'f') {
            charToByte[c] = (byte)(c - 'a' + 10);
         } else {
            charToByte[c] = -1;
         }
      }

      for (int i = 0; i < 16; i++) {
         byteToChar[i] = Integer.toHexString(i).charAt(0);
      }
   }
}
