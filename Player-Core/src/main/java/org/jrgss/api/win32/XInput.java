package org.jrgss.api.win32;

import org.jruby.Ruby;
import org.jruby.RubyClass;

/**
 * Created by matt on 2/4/15.
 */
public class XInput extends Win32API{
    public XInput(Ruby runtime, RubyClass rubyClass) {
        super(runtime, rubyClass);
    }




   /* XBox mappings:

    up = 0
    down = 1
    left = 2
    right = 3

    start = 4
    back = 5

    LStick = 6
    RStick = 7

    LBump = 8
    RBump = 9

    XBox Button = 10

    A = 11
    B = 12
    X = 13
    Y = 14*/
}
