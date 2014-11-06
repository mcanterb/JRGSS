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
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Tone implements Serializable {
    int red;
    int green;
    int blue;
    int gray;

    public Tone(int red, int green, int blue) {
        this(red, green, blue, 0);
    }

    public static Tone _load(RubyString rubyString) {
        ByteBuffer colorLoader = ByteBuffer.wrap(rubyString.getBytes());
        colorLoader.order(ByteOrder.LITTLE_ENDIAN);
        Tone c = new Tone();
        c.red = (int)colorLoader.getDouble();
        c.green = (int)colorLoader.getDouble();
        c.blue = (int)colorLoader.getDouble();
        c.gray = (int)colorLoader.getDouble();
        Gdx.app.log("Tone", "Loaded Tone "+c);
        return c;
    }

    public RubyString dump() {
        ByteBuffer buffer = ByteBuffer.allocate(32);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putDouble(red);
        buffer.putDouble(green);
        buffer.putDouble(blue);
        buffer.putDouble(gray);
        return new RubyString(Ruby.getGlobalRuntime(), Ruby.getGlobalRuntime().getString(), buffer.array());
    }

    public void set(int red, int green, int blue) {
        set(red, green, blue, this.gray);
    }

    public void set(int red, int green, int blue, int gray) {
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
        return new Tone(red, green, blue, gray);
    }


}
