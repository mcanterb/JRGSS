package org.jrgss.api.win32;

import com.badlogic.gdx.Gdx;
import com.google.common.collect.ImmutableSet;
import org.jruby.*;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@JRubyClass(
    name = {"Win32API"}
)
public class Win32API extends RubyObject {
    static final Map<DLLEntry, DLLImpl> funcEntries = new HashMap<>();
    static final Set<DLLEntry> NATIVE_OVERRIDE = ImmutableSet.of(new DLLEntry("xinput1_3", "XInputGetState", "ip"));
    private static final DLLImpl STUB_METHOD = (api, context, args) -> api.ret.equals("i")
        ? new RubyFixnum(context.runtime, 0L)
        : context.runtime.getNil();
    public static Ruby runtime;
    public static RubyClass rubyClass;
    String dll;
    String func;
    String spec;
    String ret;
    private DLLImpl impl;

    public Win32API(Ruby runtime, RubyClass rubyClass) {
        super(runtime, rubyClass);
    }

    public static void registerWin32Functions(Class<?> clazz) {
        try {
            for (Field f : clazz.getDeclaredFields()) {
                Win32Function functionMeta = f.getAnnotation(Win32Function.class);
                if (functionMeta != null) {
                    Object val = f.get(null);
                    if (val instanceof DLLImpl) {
                        DLLEntry entry = new DLLEntry(functionMeta.dll().toLowerCase(), functionMeta.name(), functionMeta.spec().toLowerCase());
                        funcEntries.put(entry, (DLLImpl) val);
                    } else {
                        Gdx.app.log("Win32API", f.getName() + " in " + clazz.getName() + " is not a DLLImpl but is annotated with @Win32Function");
                    }
                }
            }
        } catch (Exception var8) {
            throw new RuntimeException(var8);
        }
    }

    private static void registerSteamTestingShim() {
    }

    @JRubyMethod(
        required = 4
    )
    public void initialize(ThreadContext context, IRubyObject[] args) {
        this.dll = args[0].asJavaString().toLowerCase();
        this.func = args[1].asJavaString();
        this.ret = args[3].asJavaString();
        if (this.dll.endsWith(".dll")) {
            this.dll = this.dll.substring(0, this.dll.length() - 4);
        }

        if (args[2] instanceof RubyArray) {
            StringBuilder builder = new StringBuilder();
            RubyArray specArray = args[2].convertToArray();

            for (IRubyObject rubyObject : specArray.toJavaArray()) {
                if (!(rubyObject instanceof RubyString)) {
                    throw new IllegalArgumentException("Arguments in function spec array must be Strings! Was " + rubyObject.getClass());
                }

                builder.append(rubyObject.asJavaString());
            }

            this.spec = builder.toString();
        } else if (args[2] instanceof RubyString) {
            this.spec = args[2].asJavaString();
        }

        DLLEntry entry = new DLLEntry(this.dll, this.func, this.spec.toLowerCase());
        if (NATIVE_OVERRIDE.contains(entry)) {
            Gdx.app.log("Win32API", "Checking for Native Override for " + this);
            Optional<DLLImpl> resolved = NativeProxy.resolveNative(entry, this.ret.toLowerCase(), this.spec.toLowerCase());
            if (resolved.isPresent()) {
                Gdx.app.log("Win32API", "Using NativeProxy for " + this);
                this.impl = resolved.get();
                return;
            }
        }

        DLLImpl m = funcEntries.get(entry);
        if (m == null) {
            Optional<DLLImpl> resolved = NativeProxy.resolveNative(entry, this.ret.toLowerCase(), this.spec.toLowerCase());
            if (resolved.isPresent()) {
                m = resolved.get();
                Gdx.app.log("Win32API", "Using NativeProxy for " + this);
            } else {
                Gdx.app.log("Win32API", "Returning stub for " + this);
                m = STUB_METHOD;
            }
        }

        this.impl = m;
    }

    @JRubyMethod(
        name = {"call"},
        alias = {"Call"},
        optional = 16
    )
    public IRubyObject call(ThreadContext context, IRubyObject[] args) {
        return this.impl.call(this, context, args);
    }

    @Override
    public String toString() {
        return "Win32API(dll=" + this.dll + ", func=" + this.func + ", spec=" + this.spec + ", ret=" + this.ret + ", impl=" + this.impl + ")";
    }
}
