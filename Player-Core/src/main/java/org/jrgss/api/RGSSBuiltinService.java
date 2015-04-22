package org.jrgss.api;

import lombok.Data;
import org.jrgss.api.win32.*;
import org.jruby.*;
import org.jruby.anno.JRubyConstant;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.BasicLibraryService;

import java.awt.image.Kernel;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @author matt
 * @date 8/26/14
 */
@Data
public class RGSSBuiltinService implements BasicLibraryService {
    private static final List<Class<? extends RubyObject>> builtInClasses =
            new ArrayList<Class<? extends RubyObject>>() {{
                add(Table.class);
                add(Color.class);
                add(Font.class);
            }};

    private Ruby runtime;


    @Override
    public boolean basicLoad(Ruby runtime) throws IOException {
        this.runtime = runtime;

        builtInClasses.forEach(this::setupClass);

        RubyModule inputClass = runtime.defineModule("Input");
        inputClass.defineAnnotatedMethods(Input.class);
        betterLoadConstants(Input.class, inputClass);
        Input.runtime = runtime;
        Input.rubyClass = inputClass;


        RubyClass win32Class = runtime.defineClass("Win32API", runtime.getObject(), Win32API::new);
        win32Class.defineAnnotatedMethods(Win32API.class);
        Win32API.runtime = runtime;
        Win32API.rubyClass = win32Class;

        Win32API.registerWin32Functions(Shell32.class);
        Win32API.registerWin32Functions(Kernel32.class);
        Win32API.registerWin32Functions(XInput.class);
        Win32API.registerWin32Functions(User32.class);

        return true;
    }

    private void betterLoadConstants(Class klazz, RubyModule rubyClass) {
        try{
            for(Field f : klazz.getDeclaredFields()) {
                if(f.getAnnotation(JRubyConstant.class) != null) {
                    rubyClass.defineConstant(f.getName(), RubySymbol.newSymbol(runtime, (String)f.get(null)));
                }
            }
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private <T extends RubyObject> void setupClass(final Class<T> clazz) {
        try{
            final Constructor<T> c = clazz.getDeclaredConstructor(Ruby.class, RubyClass.class);
            final RubyClass rubyClass = runtime.defineClass(clazz.getSimpleName(), runtime.getObject(),
                    (ruby, rClass)->{
                        try {
                            return c.newInstance(ruby, rClass);
                        }catch(Exception e) {
                            throw new RuntimeException("Could not instantiate "+clazz.getSimpleName()+"!",e);
                        }
                    });
            rubyClass.defineAnnotatedMethods(clazz);
            setOptionalField(clazz, "runtime", runtime);
            setOptionalField(clazz, "rubyClass", rubyClass);
            callOptionalMethod(clazz, "init");
        }catch(Exception e) {
            throw new RuntimeException("Failed to load "+clazz.getSimpleName(),e);
        }
    }

    private void setOptionalField(Class<?> clazz, String fieldName, Object value) {
        try{
            Field f = clazz.getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(null, value);
        }catch(NoSuchFieldException e) {

        }catch (Exception e) {
            throw new RuntimeException("Unknown exception setting "+fieldName+" in "+clazz.getSimpleName(),e);
        }
    }

    private void callOptionalMethod(Class<?> clazz, String methodName) {
        try{
            Method m = clazz.getDeclaredMethod(methodName);
            m.setAccessible(true);
            m.invoke(null);
        }catch(NoSuchMethodException e) {

        }catch (Exception e) {
            throw new RuntimeException("Unknown exception invoking "+methodName+" in "+clazz.getSimpleName(),e);
        }
    }

}
