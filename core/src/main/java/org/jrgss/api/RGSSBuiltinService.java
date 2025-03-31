package org.jrgss.api;

import com.badlogic.gdx.Gdx;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.jrgss.api.win32.Kernel32;
import org.jrgss.api.win32.Ole32;
import org.jrgss.api.win32.Shell32;
import org.jrgss.api.win32.User32;
import org.jrgss.api.win32.WS232;
import org.jrgss.api.win32.Win32API;
import org.jrgss.api.win32.XInput;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubySymbol;
import org.jruby.anno.JRubyConstant;
import org.jruby.runtime.load.BasicLibraryService;

public class RGSSBuiltinService implements BasicLibraryService {
   private static final List<Class<? extends RubyObject>> builtInClasses = new ArrayList<Class<? extends RubyObject>>() {
      {
         this.add(Color.class);
         this.add(Font.class);
         this.add(Table.class);
      }
   };
   private Ruby runtime;

   @Override
   public boolean basicLoad(Ruby runtime) throws IOException {
      this.runtime = runtime;
      RubyClass table = runtime.defineClass("Table", runtime.getObject(), Table::new);
      table.defineAnnotatedMethods(Table.class);
      Table.runtime = runtime;
      Table.rubyClass = table;
      builtInClasses.forEach(this::setupClass);
      RubyModule inputClass = runtime.defineModule("Input");
      inputClass.defineAnnotatedMethods(Input.class);
      this.betterLoadConstants(Input.class, inputClass);
      Input.runtime = runtime;
      Input.rubyClass = inputClass;
      RubyModule dlModule = runtime.defineModule("DL");
      RubyClass cptrClass = dlModule.defineClassUnder("CPtr", runtime.getObject(), CPtr::new);
      cptrClass.defineAnnotatedMethods(CPtr.class);
      dlModule.defineAnnotatedMethods(DL.class);
      CPtr.runtime = runtime;
      CPtr.rubyClass = cptrClass;
      RubyClass win32Class = runtime.defineClass("Win32API", runtime.getObject(), Win32API::new);
      win32Class.defineAnnotatedMethods(Win32API.class);
      Win32API.runtime = runtime;
      Win32API.rubyClass = win32Class;
      Win32API.registerWin32Functions(Shell32.class);
      Win32API.registerWin32Functions(Kernel32.class);
      Win32API.registerWin32Functions(XInput.class);
      Win32API.registerWin32Functions(User32.class);
      Win32API.registerWin32Functions(Ole32.class);
      Win32API.registerWin32Functions(WS232.class);
      return true;
   }

   private void betterLoadConstants(Class klazz, RubyModule rubyClass) {
      try {
         for (Field f : klazz.getDeclaredFields()) {
            if (f.getAnnotation(JRubyConstant.class) != null) {
               rubyClass.defineConstant(f.getName(), RubySymbol.newSymbol(this.runtime, (String)f.get(null)));
            }
         }
      } catch (Exception var7) {
         throw new RuntimeException(var7);
      }
   }

   private <T extends RubyObject> void setupClass(Class<T> clazz) {
      try {
         Constructor<T> c = clazz.getDeclaredConstructor(Ruby.class, RubyClass.class);
         RubyClass rubyClass = this.runtime.defineClass(clazz.getSimpleName(), this.runtime.getObject(), (ruby, rClass) -> {
            try {
               return c.newInstance(ruby, rClass);
            } catch (Exception var5) {
               throw new RuntimeException("Could not instantiate " + clazz.getSimpleName() + "!", var5);
            }
         });
         rubyClass.defineAnnotatedMethods(clazz);
         this.setOptionalField(clazz, "runtime", this.runtime);
         this.setOptionalField(clazz, "rubyClass", rubyClass);
         this.callOptionalMethod(clazz, "init");
      } catch (Exception var4) {
         throw new RuntimeException("Failed to load " + clazz.getSimpleName(), var4);
      }
   }

   private void setOptionalField(Class<?> clazz, String fieldName, Object value) {
      try {
         Field f = clazz.getDeclaredField(fieldName);
         f.setAccessible(true);
         f.set(null, value);
      } catch (NoSuchFieldException var5) {
         Gdx.app.log("RGSSBuiltinService", fieldName + " does not exist for " + clazz);
      } catch (Exception var6) {
         throw new RuntimeException("Unknown exception setting " + fieldName + " in " + clazz.getSimpleName(), var6);
      }
   }

   private void callOptionalMethod(Class<?> clazz, String methodName) {
      try {
         Method m = clazz.getDeclaredMethod(methodName);
         m.setAccessible(true);
         m.invoke(null);
      } catch (NoSuchMethodException var4) {
      } catch (Exception var5) {
         throw new RuntimeException("Unknown exception invoking " + methodName + " in " + clazz.getSimpleName(), var5);
      }
   }

   public Ruby getRuntime() {
      return this.runtime;
   }

   public void setRuntime(Ruby runtime) {
      this.runtime = runtime;
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof RGSSBuiltinService)) {
         return false;
      } else {
         RGSSBuiltinService other = (RGSSBuiltinService)o;
         if (!other.canEqual(this)) {
            return false;
         } else {
            Object this$runtime = this.getRuntime();
            Object other$runtime = other.getRuntime();
            return this$runtime == null ? other$runtime == null : this$runtime.equals(other$runtime);
         }
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof RGSSBuiltinService;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = 1;
      Object $runtime = this.getRuntime();
      return result * 59 + ($runtime == null ? 43 : $runtime.hashCode());
   }

   @Override
   public String toString() {
      return "RGSSBuiltinService(runtime=" + this.getRuntime() + ")";
   }
}
