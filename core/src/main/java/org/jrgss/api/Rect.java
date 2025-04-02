package org.jrgss.api;

import java.beans.ConstructorProperties;

public class Rect {
    int x;
    int y;
    int width;
    int height;

    @ConstructorProperties({"x", "y", "width", "height"})
    public Rect(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public Rect() {
    }

    public void empty() {
        this.x = 0;
        this.y = 0;
        this.width = 0;
        this.height = 0;
    }

    public void set(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void set(Rect r) {
        this.x = r.x;
        this.y = r.y;
        this.width = r.width;
        this.height = r.height;
    }

    public Rect clone() {
        return new Rect(this.x, this.y, this.width, this.height);
    }

    public int getX() {
        return this.x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return this.y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getWidth() {
        return this.width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return this.height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof Rect)) {
            return false;
        } else {
            Rect other = (Rect) o;
            if (!other.canEqual(this)) {
                return false;
            } else if (this.getX() != other.getX()) {
                return false;
            } else if (this.getY() != other.getY()) {
                return false;
            } else {
                return this.getWidth() == other.getWidth() && this.getHeight() == other.getHeight();
            }
        }
    }

    protected boolean canEqual(Object other) {
        return other instanceof Rect;
    }

    @Override
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + this.getX();
        result = result * 59 + this.getY();
        result = result * 59 + this.getWidth();
        return result * 59 + this.getHeight();
    }

    @Override
    public String toString() {
        return "Rect(x=" + this.getX() + ", y=" + this.getY() + ", width=" + this.getWidth() + ", height=" + this.getHeight() + ")";
    }
}
