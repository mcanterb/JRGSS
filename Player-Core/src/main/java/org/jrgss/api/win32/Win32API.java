package org.jrgss.api.win32;

import com.badlogic.gdx.Gdx;
import lombok.ToString;
import org.jruby.*;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.lang.reflect.Field;
import java.util.HashMap;

/**
 * @author matt
 * @date 8/20/14
 */
@ToString
@JRubyClass(name = "Win32API")
public class Win32API extends RubyObject{

    static public Ruby runtime;
    static public RubyClass rubyClass;
    static final HashMap<DLLEntry, DLLImpl> funcEntries = new HashMap<>();

    String dll;
    String func;
    String spec;
    String ret;

    private DLLImpl impl;

    public Win32API(final Ruby runtime, RubyClass rubyClass) {
        super(runtime, rubyClass);
    }

    @JRubyMethod(required = 4)
    public void initialize(ThreadContext context, IRubyObject[] args) {
        this.dll = args[0].asJavaString();
        this.func = args[1].asJavaString();
        this.ret = args[3].asJavaString();
        if(args[2] instanceof RubyArray) {
            StringBuilder builder = new StringBuilder();
            RubyArray specArray = args[2].convertToArray();
            for(IRubyObject rubyObject : specArray.toJavaArray()) {
                if(!(rubyObject instanceof RubyString)) {
                    throw new IllegalArgumentException("Arguments in function spec array must be Strings! Was "+rubyObject.getClass());
                }
                builder.append(rubyObject.asJavaString());
            }
            this.spec = builder.toString();
        } else if(args[2] instanceof RubyString) {
            this.spec = args[2].asJavaString();
        }
        DLLImpl m = funcEntries.get(new DLLEntry(dll, func, spec));
        if(m == null) {
            Gdx.app.log("Win32API","Returning stub for "+this.toString());
            m = STUB_METHOD;
        }
        impl = m;
    }


    @JRubyMethod(name = "call", alias = "Call", optional = 16)
    public IRubyObject call(ThreadContext context, IRubyObject[] args) {
        return impl.call(this, context, args);
    }




    static final DLLImpl STUB_METHOD = new DLLImpl() {
        @Override
        public IRubyObject call(Win32API api, ThreadContext context, IRubyObject[] args) {
            if(api.ret.equals("i")) {
                return new RubyFixnum(context.runtime, 0);
            }
            return context.runtime.getNil();
        }
    };

    public static void registerWin32Functions(Class<?> clazz) {
        try {
            for(Field f : clazz.getDeclaredFields()) {
                Win32Function functionMeta = f.getAnnotation(Win32Function.class);
                if(functionMeta == null) continue;
                Object val = f.get(null);
                if(val instanceof DLLImpl) {
                    DLLEntry entry = new DLLEntry(functionMeta.dll(), functionMeta.name(), functionMeta.spec());
                    funcEntries.put(entry, (DLLImpl)val);
                } else {
                    Gdx.app.log("Win32API", f.getName()+" in "+clazz.getName()+" is not a DLLImpl but is annotated with @Win32Function");
                }
            }
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
