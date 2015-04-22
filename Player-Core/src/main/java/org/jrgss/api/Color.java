package org.jrgss.api;

import com.badlogic.gdx.Gdx;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import static org.jrgss.JRubyUtil.*;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


/**
 * Created by matty on 6/27/14.
 */
@Data
@JRubyClass(name="Color")
public class Color extends RubyObject implements Serializable,Cloneable {
    static Ruby runtime;
    static RubyClass rubyClass;

    int red=0,green=0,blue=0,alpha=0;



    public Color(final Ruby runtime, final RubyClass rubyClass) {
        super(runtime, rubyClass);
    }

    public Color() {
        super(runtime, rubyClass);
    }

    public Color(int packed) {
        super(runtime, rubyClass);
        red = packed>>>24;
        green = (packed & 0xFF0000)>>>16;
        blue = (packed & 0xFF00) >>> 8;
        alpha = (packed & 0xFF);
    }

    public Color(int[] colorArr) {
        this(colorArr[0], colorArr[1], colorArr[2], colorArr[3]);
    }

    public Color(final int red, final int green, final int blue, final int alpha) {
        super(runtime, rubyClass);
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
    }

    @JRubyMethod(required = 0, optional = 4)
    public void initialize(ThreadContext context, IRubyObject[] args) {
        if(args.length > 0 && args.length < 3) {
            throwArgumentError(args.length, 3);
        }
        if(args.length == 0) return;
        this.red = getInt(args[0]);
        this.green = getInt(args[1]);
        this.blue = getInt(args[2]);
        this.alpha = args.length == 4?getInt(args[3]):255;
    }

    @JRubyMethod(required = 1, optional = 4)
    public void set(ThreadContext context, IRubyObject[] args) {
        switch (args.length) {
            case 1:
                set((Color)args[0]);
                break;
            case 2:
                throwArgumentError(args.length, 3);
                break;
            case 3:
            case 4:
                set(getInt(args[0]), getInt(args[1]), getInt(args[2]), args.length == 3?this.alpha:getInt(args[3]));
        }
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

    @JRubyMethod( module = true)
    public static IRubyObject _load(ThreadContext context, IRubyObject self, IRubyObject rubyObj) {
        RubyString rubyString = rubyObj.asString();
        ByteBuffer colorLoader = ByteBuffer.wrap(rubyString.getBytes());
        colorLoader.order(ByteOrder.LITTLE_ENDIAN);
        Color c = new Color();
        c.red = (int)colorLoader.getDouble();
        c.green = (int)colorLoader.getDouble();
        c.blue = (int)colorLoader.getDouble();
        c.alpha = (int)colorLoader.getDouble();
        return c;
    }

    @JRubyMethod( module = true)
    public IRubyObject marshal_load(ThreadContext context, IRubyObject rubyObj) {
        RubyString rubyString = rubyObj.asString();
        ByteBuffer colorLoader = ByteBuffer.wrap(rubyString.getBytes());
        colorLoader.order(ByteOrder.LITTLE_ENDIAN);
        red = (int)colorLoader.getDouble();
        green = (int)colorLoader.getDouble();
        blue = (int)colorLoader.getDouble();
        alpha = (int)colorLoader.getDouble();
        return runtime.getNil();
    }

    @JRubyMethod
    public IRubyObject _dump(IRubyObject level) {
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


    @JRubyMethod
    public IRubyObject red() {
        return rubyNum(red);
    }

    @JRubyMethod
    public IRubyObject green() {
        return rubyNum(green);
    }

    @JRubyMethod
    public IRubyObject blue() {
        return rubyNum(blue);
    }

    @JRubyMethod
    public IRubyObject alpha() {
        return rubyNum(alpha);
    }

    @JRubyMethod(name="red=", required = 1)
    public IRubyObject red_set(IRubyObject arg) {
        return rubyNum(this.red = clamp(getInt(arg), 0, 255));
    }

    @JRubyMethod(name="green=", required = 1)
    public IRubyObject green_set(IRubyObject arg) {
        return rubyNum(this.green = clamp(getInt(arg), 0, 255));
    }

    @JRubyMethod(name="blue=", required = 1)
    public IRubyObject blue_set(IRubyObject arg) {
        return rubyNum(this.blue = clamp(getInt(arg), 0, 255));
    }

    @JRubyMethod(name="alpha=", required = 1)
    public IRubyObject alpha_set(IRubyObject arg) {
        return rubyNum(this.alpha = clamp(getInt(arg), 0, 255));
    }

    private void throwArgumentError(int got, int correct) {
        throw runtime.newRaiseException(runtime.getArgumentError(),
                String.format("wrong number of arguments (%d for %d)",got, correct));
    }
}
