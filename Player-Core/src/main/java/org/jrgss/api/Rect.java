package org.jrgss.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by matty on 6/27/14.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Rect {
    int x, y, width, height;

    public void empty() {
        x = 0;
        y = 0;
        width = 0;
        height = 0;
    }

    public void set(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void set(Rect r) {
        x = r.x;
        y = r.y;
        width = r.width;
        height = r.height;
    }

    public Rect clone() {
        return new Rect(x, y, width, height);
    }

}
