package org.jrgss.api;

import java.beans.ConstructorProperties;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.jruby.Ruby;
import org.jruby.RubyString;

public class Tone implements Serializable {
   float red;
   float green;
   float blue;
   float gray;

   public Tone(int red, int green, int blue) {
      this(red, green, blue, 0.0F);
   }

   public boolean isZero() {
      return this.red == 0.0F && this.green == 0.0F && this.blue == 0.0F && this.gray == 0.0F;
   }

   public static Tone _load(RubyString rubyString) {
      ByteBuffer colorLoader = ByteBuffer.wrap(rubyString.getBytes());
      colorLoader.order(ByteOrder.LITTLE_ENDIAN);
      Tone c = new Tone();
      c.red = (int)colorLoader.getDouble();
      c.green = (int)colorLoader.getDouble();
      c.blue = (int)colorLoader.getDouble();
      c.gray = (int)colorLoader.getDouble();
      return c;
   }

   public RubyString dump() {
      ByteBuffer buffer = ByteBuffer.allocate(32);
      buffer.order(ByteOrder.LITTLE_ENDIAN);
      buffer.putDouble(this.red);
      buffer.putDouble(this.green);
      buffer.putDouble(this.blue);
      buffer.putDouble(this.gray);
      return new RubyString(Ruby.getGlobalRuntime(), Ruby.getGlobalRuntime().getString(), buffer.array());
   }

   public void setRed(float red) {
      this.red = red;
   }

   public void set(float red, float green, float blue) {
      this.set(red, green, blue, this.gray);
   }

   public void set(float red, float green, float blue, float gray) {
      this.red = red;
      this.green = green;
      this.blue = blue;
      this.gray = gray;
   }

   public void set(Tone tone) {
      this.red = tone.red;
      this.green = tone.green;
      this.blue = tone.blue;
      this.gray = tone.gray;
   }

   public Tone clone() {
      return new Tone(this.red, this.green, this.blue, this.gray);
   }

   public String inspect() {
      return this.toString();
   }

   public float getRed() {
      return this.red;
   }

   public float getGreen() {
      return this.green;
   }

   public float getBlue() {
      return this.blue;
   }

   public float getGray() {
      return this.gray;
   }

   public void setGreen(float green) {
      this.green = green;
   }

   public void setBlue(float blue) {
      this.blue = blue;
   }

   public void setGray(float gray) {
      this.gray = gray;
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof Tone)) {
         return false;
      } else {
         Tone other = (Tone)o;
         if (!other.canEqual(this)) {
            return false;
         } else if (Float.compare(this.getRed(), other.getRed()) != 0) {
            return false;
         } else if (Float.compare(this.getGreen(), other.getGreen()) != 0) {
            return false;
         } else {
            return Float.compare(this.getBlue(), other.getBlue()) != 0 ? false : Float.compare(this.getGray(), other.getGray()) == 0;
         }
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof Tone;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = 1;
      result = result * 59 + Float.floatToIntBits(this.getRed());
      result = result * 59 + Float.floatToIntBits(this.getGreen());
      result = result * 59 + Float.floatToIntBits(this.getBlue());
      return result * 59 + Float.floatToIntBits(this.getGray());
   }

   @Override
   public String toString() {
      return "Tone(red=" + this.getRed() + ", green=" + this.getGreen() + ", blue=" + this.getBlue() + ", gray=" + this.getGray() + ")";
   }

   @ConstructorProperties({"red", "green", "blue", "gray"})
   public Tone(float red, float green, float blue, float gray) {
      this.red = red;
      this.green = green;
      this.blue = blue;
      this.gray = gray;
   }

   public Tone() {
   }
}
