package org.jrgss.api;

import com.badlogic.gdx.Gdx;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @author matt
 * @date 8/20/14
 */
@Data
public class Win32API {

    String dll;
    String func;
    String spec;
    String ret;

    public Win32API(String dll, String func, String spec, String ret) {
        this.dll = dll;
        this.func = func;
        this.spec = spec;
        this.ret = ret;
    }

    public Object call(Object...args) {
        System.out.println("Win32API"+": STUB: "+this);
        if(ret.equals("i")) {
            return Integer.valueOf(0);
        }
        return null;
    }

}
