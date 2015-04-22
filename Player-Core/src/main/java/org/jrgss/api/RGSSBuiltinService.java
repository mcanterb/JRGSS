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
import java.lang.reflect.Field;

/**
 * @author matt
 * @date 8/26/14
 */
@Data
public class RGSSBuiltinService implements BasicLibraryService {
    private Ruby runtime;


    @Override
    public boolean basicLoad(Ruby runtime) throws IOException {
        this.runtime = runtime;
        RubyClass tableClass = runtime.defineClass("Table", runtime.getObject(), Table::new);
        tableClass.defineAnnotatedMethods(Table.class);
        Table.runtime = runtime;
        Table.rubyClass = tableClass;

        RubyModule inputClass = runtime.defineModule("Input");
        inputClass.defineAnnotatedMethods(Input.class);
        betterLoadConstants(Input.class, inputClass);
        Input.runtime = runtime;
        Input.rubyClass = inputClass;

        RubyClass colorClass = runtime.defineClass("Color", runtime.getObject(), Color::new);
        colorClass.defineAnnotatedMethods(Color.class);
        Color.runtime = runtime;
        Color.rubyClass = colorClass;

        RubyClass fontClass = runtime.defineClass("Font", runtime.getObject(), Font::new);
        fontClass.defineAnnotatedMethods(Font.class);
        Font.runtime = runtime;
        Font.rubyClass = fontClass;
        Font.init();

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

}
