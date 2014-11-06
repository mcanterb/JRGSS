package org.jrgss.api;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.BasicLibraryService;

import java.io.IOException;

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
        RubyClass tableClass = runtime.defineClass("Table", runtime.getObject(), new ObjectAllocator() {
            @Override
            public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
                return new Table(runtime, klazz);
            }
        });
        tableClass.defineAnnotatedMethods(Table.class);
        Table.runtime = runtime;
        Table.rubyClass = tableClass;
        return true;
    }
}
