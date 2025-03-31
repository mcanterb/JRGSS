package org.jrgss.api.win32;

import com.badlogic.gdx.Gdx;
import com.google.common.collect.ImmutableSet;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

@JRubyClass(
   name = {"Win32API"}
)
public class Win32API extends RubyObject {
   public static Ruby runtime;
   public static RubyClass rubyClass;
   static final Map<DLLEntry, DLLImpl> funcEntries = new HashMap<>();
   static final Set<DLLEntry> NATIVE_OVERRIDE = ImmutableSet.of(new DLLEntry("xinput1_3", "XInputGetState", "ip"));
   String dll;
   String func;
   String spec;
   String ret;
   private DLLImpl impl;
   private static final DLLImpl STUB_METHOD = (api, context, args) -> (IRubyObject)(api.ret.equals("i")
      ? new RubyFixnum(context.runtime, 0L)
      : context.runtime.getNil());

   public Win32API(Ruby runtime, RubyClass rubyClass) {
      super(runtime, rubyClass);
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
         Gdx.app.log("Win32API", "Checking for Native Override for " + this.toString());
         Optional<DLLImpl> resolved = NativeProxy.resolveNative(entry, this.ret.toLowerCase(), this.spec.toLowerCase());
         if (resolved.isPresent()) {
            Gdx.app.log("Win32API", "Using NativeProxy for " + this.toString());
            this.impl = resolved.get();
            return;
         }
      }

      DLLImpl m = funcEntries.get(entry);
      if (m == null) {
         Optional<DLLImpl> resolved = NativeProxy.resolveNative(entry, this.ret.toLowerCase(), this.spec.toLowerCase());
         if (resolved.isPresent()) {
            m = resolved.get();
            Gdx.app.log("Win32API", "Using NativeProxy for " + this.toString());
         } else {
            Gdx.app.log("Win32API", "Returning stub for " + this.toString());
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

   public static void registerWin32Functions(Class<?> clazz) {
      try {
         for (Field f : clazz.getDeclaredFields()) {
            Win32Function functionMeta = f.getAnnotation(Win32Function.class);
            if (functionMeta != null) {
               Object val = f.get(null);
               if (val instanceof DLLImpl) {
                  DLLEntry entry = new DLLEntry(functionMeta.dll().toLowerCase(), functionMeta.name(), functionMeta.spec().toLowerCase());
                  funcEntries.put(entry, (DLLImpl)val);
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

   @Override
   public String toString() {
      return "Win32API(dll=" + this.dll + ", func=" + this.func + ", spec=" + this.spec + ", ret=" + this.ret + ", impl=" + this.impl + ")";
   }
}
