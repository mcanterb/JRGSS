package org.jrgss.api;

import com.badlogic.gdx.Gdx;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jruby.Ruby;
import org.jruby.RubyString;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by matty on 6/27/14.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Color implements Serializable,Cloneable{
    int red=0,green=0,blue=0,alpha=0;

    public Color(int packed) {
        red = packed>>>24;
        green = (packed & 0xFF0000)>>>16;
        blue = (packed & 0xFF00) >>> 8;
        alpha = (packed & 0xFF);
    }

    public void set(Color c) {
        set(c.red, c.green, c.blue, c.alpha);
    }

    public void set(int r, int g, int b) {
        set(r, g, b, this.alpha);
    }

    public void set(int r, int g, int b, int alpha) {
        this.red = clamp(r, 0, 255);
        this.green = clamp(g, 0, 255);
        this.blue = clamp(b, 0, 255);
        this.alpha = clamp(alpha, 0, 255);
    }

    public static Color _load(RubyString rubyString) {
        ByteBuffer colorLoader = ByteBuffer.wrap(rubyString.getBytes());
        colorLoader.order(ByteOrder.LITTLE_ENDIAN);
        Color c = new Color();
        c.red = (int)colorLoader.getDouble();
        c.green = (int)colorLoader.getDouble();
        c.blue = (int)colorLoader.getDouble();
        c.alpha = (int)colorLoader.getDouble();
        return c;
    }

    public RubyString dump() {
        ByteBuffer buffer = ByteBuffer.allocate(32);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putDouble(red);
        buffer.putDouble(green);
        buffer.putDouble(blue);
        buffer.putDouble(alpha);
        return new RubyString(Ruby.getGlobalRuntime(), Ruby.getGlobalRuntime().getString(), buffer.array());
    }

    public int toPackedInt() {
        int packed = alpha;
        packed |= (blue<<8);
        packed |= (green<<16);
        packed |= (red<<24);
        return packed;
    }

    public Color clone() {
        return new Color(red, green, blue, alpha);
    }

    protected com.badlogic.gdx.graphics.Color toGDX() {
        return new com.badlogic.gdx.graphics.Color(red/255f, green/255f, blue/255f, alpha/255f);
    }

    int clamp(int x, int low, int high) {
        return Math.min(Math.max(x, low), high);
    }
}
