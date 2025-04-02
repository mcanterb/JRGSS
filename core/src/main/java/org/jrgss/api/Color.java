package org.jrgss.api;

import org.jrgss.api.win32.Win32Util;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static com.badlogic.gdx.math.MathUtils.clamp;

@JRubyClass(
    name = {"Color"}
)
public class Color extends RubyObject implements Serializable, Cloneable {
    public static Color WHITE;
    static Ruby runtime;
    static RubyClass rubyClass;
    int red = 0;
    int green = 0;
    int blue = 0;
    int alpha = 0;

    public Color(Ruby runtime, RubyClass rubyClass) {
        super(runtime, rubyClass);
        if (Color.runtime == null) {
            Color.runtime = runtime;
            Color.rubyClass = rubyClass;
        }
    }

    public Color() {
        super(runtime, rubyClass);
    }

    public Color(int packed) {
        super(runtime, rubyClass);
        this.red = packed >>> 24;
        this.green = (packed & 0xFF0000) >>> 16;
        this.blue = (packed & 0xFF00) >>> 8;
        this.alpha = packed & 0xFF;
    }

    public Color(int[] colorArr) {
        this(colorArr[0], colorArr[1], colorArr[2], colorArr[3]);
    }

    public Color(int red, int green, int blue, int alpha) {
        super(runtime, rubyClass);
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
    }

    @JRubyMethod(
        module = true
    )
    public static IRubyObject _load(ThreadContext context, IRubyObject self, IRubyObject rubyObj) {
        Color c = new Color();
        c.marshal_load(context, rubyObj);
        return c;
    }

    public static void init() {
        WHITE = new Color(255, 255, 255, 255);
    }

    @JRubyMethod(
        required = 0,
        optional = 4
    )
    public void initialize(ThreadContext context, IRubyObject[] args) {
        if (args.length > 0 && args.length < 3) {
            this.throwArgumentError(args.length, 3);
        }

        if (args.length != 0) {
            this.red = clamp(Win32Util.getInt(args[0]), 0, 255);
            this.green = clamp(Win32Util.getInt(args[1]), 0, 255);
            this.blue = clamp(Win32Util.getInt(args[2]), 0, 255);
            this.alpha = args.length == 4 ? clamp(Win32Util.getInt(args[3]), 0, 255) : 255;
        }
    }

    @JRubyMethod(
        required = 1,
        optional = 4
    )
    public void set(ThreadContext context, IRubyObject[] args) {
        switch (args.length) {
            case 1:
                this.set((Color) args[0]);
                break;
            case 2:
                this.throwArgumentError(args.length, 3);
                break;
            case 3:
            case 4:
                this.set(Win32Util.getInt(args[0]), Win32Util.getInt(args[1]), Win32Util.getInt(args[2]), args.length == 3 ? this.alpha : Win32Util.getInt(args[3]));
        }
    }

    public void set(Color c) {
        this.set(c.red, c.green, c.blue, c.alpha);
    }

    public void set(int r, int g, int b) {
        this.set(r, g, b, this.alpha);
    }

    public void set(int r, int g, int b, int alpha) {
        this.red = clamp(r, 0, 255);
        this.green = clamp(g, 0, 255);
        this.blue = clamp(b, 0, 255);
        this.alpha = clamp(alpha, 0, 255);
    }

    @JRubyMethod(
        module = true
    )
    public IRubyObject marshal_load(ThreadContext context, IRubyObject rubyObj) {
        RubyString rubyString = rubyObj.asString();
        ByteBuffer colorLoader = ByteBuffer.wrap(rubyString.getBytes());
        if (colorLoader.capacity() != 32) {
            throw new IllegalArgumentException("Trying to load a corrupt color! Color must be 32 bytes!");
        } else {
            colorLoader.order(ByteOrder.LITTLE_ENDIAN);
            this.red = (int) colorLoader.getDouble();
            this.green = (int) colorLoader.getDouble();
            this.blue = (int) colorLoader.getDouble();
            this.alpha = (int) colorLoader.getDouble();
            return runtime.getNil();
        }
    }

    @JRubyMethod
    public IRubyObject _dump(IRubyObject level) {
        ByteBuffer buffer = ByteBuffer.allocate(32);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putDouble(this.red);
        buffer.putDouble(this.green);
        buffer.putDouble(this.blue);
        buffer.putDouble(this.alpha);
        return new RubyString(Ruby.getGlobalRuntime(), Ruby.getGlobalRuntime().getString(), buffer.array());
    }

    public int toPackedInt() {
        int packed = this.alpha;
        packed |= this.blue << 8;
        packed |= this.green << 16;
        return packed | this.red << 24;
    }

    @JRubyMethod
    public IRubyObject clone() {
        return new Color(this.red, this.green, this.blue, this.alpha);
    }

    protected com.badlogic.gdx.graphics.Color toGDX() {
        return new com.badlogic.gdx.graphics.Color(this.red / 255.0F, this.green / 255.0F, this.blue / 255.0F, this.alpha / 255.0F);
    }

    @JRubyMethod
    public IRubyObject red() {
        return Win32Util.rubyNum(this.red);
    }

    @JRubyMethod
    public IRubyObject green() {
        return Win32Util.rubyNum(this.green);
    }

    @JRubyMethod
    public IRubyObject blue() {
        return Win32Util.rubyNum(this.blue);
    }

    @JRubyMethod
    public IRubyObject alpha() {
        return Win32Util.rubyNum(this.alpha);
    }

    @JRubyMethod(
        name = {"red="},
        required = 1
    )
    public IRubyObject red_set(IRubyObject arg) {
        return Win32Util.rubyNum(this.red = clamp(Win32Util.getInt(arg), 0, 255));
    }

    @JRubyMethod(
        name = {"green="},
        required = 1
    )
    public IRubyObject green_set(IRubyObject arg) {
        return Win32Util.rubyNum(this.green = clamp(Win32Util.getInt(arg), 0, 255));
    }

    @JRubyMethod(
        name = {"blue="},
        required = 1
    )
    public IRubyObject blue_set(IRubyObject arg) {
        return Win32Util.rubyNum(this.blue = clamp(Win32Util.getInt(arg), 0, 255));
    }

    @JRubyMethod(
        name = {"alpha="},
        required = 1
    )
    public IRubyObject alpha_set(IRubyObject arg) {
        return Win32Util.rubyNum(this.alpha = clamp(Win32Util.getInt(arg), 0, 255));
    }

    @JRubyMethod(
        name = {"to_s"}
    )
    public IRubyObject toS() {
        return Win32Util.rubyString(this.toString());
    }

    private void throwArgumentError(int got, int correct) {
        throw runtime.newRaiseException(runtime.getArgumentError(), String.format("wrong number of arguments (%d for %d)", got, correct));
    }

    public int getRed() {
        return this.red;
    }

    public void setRed(int red) {
        this.red = red;
    }

    public int getGreen() {
        return this.green;
    }

    public void setGreen(int green) {
        this.green = green;
    }

    public int getBlue() {
        return this.blue;
    }

    public void setBlue(int blue) {
        this.blue = blue;
    }

    public int getAlpha() {
        return this.alpha;
    }

    public void setAlpha(int alpha) {
        this.alpha = alpha;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof Color)) {
            return false;
        } else {
            Color other = (Color) o;
            if (!other.canEqual(this)) {
                return false;
            } else if (this.getRed() != other.getRed()) {
                return false;
            } else if (this.getGreen() != other.getGreen()) {
                return false;
            } else {
                return this.getBlue() == other.getBlue() && this.getAlpha() == other.getAlpha();
            }
        }
    }

    protected boolean canEqual(Object other) {
        return other instanceof Color;
    }

    @Override
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + this.getRed();
        result = result * 59 + this.getGreen();
        result = result * 59 + this.getBlue();
        return result * 59 + this.getAlpha();
    }

    @Override
    public String toString() {
        return "Color(red=" + this.getRed() + ", green=" + this.getGreen() + ", blue=" + this.getBlue() + ", alpha=" + this.getAlpha() + ")";
    }
}
