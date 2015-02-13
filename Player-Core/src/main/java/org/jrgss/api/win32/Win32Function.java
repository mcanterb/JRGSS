package org.jrgss.api.win32;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author matt
 * @date 2/5/15
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Win32Function {
    public String dll();
    public String name();
    public String spec();
}
