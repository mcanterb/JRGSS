package org.jrgss.api.win32;

import com.badlogic.gdx.Gdx;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.sun.jna.Function;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import org.jrgss.OS;
import org.jruby.RubyString;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.*;

class NativeProxy {
    private static final Set<String> BLACKLISTED_DLLS = ImmutableSet.of("user32", "gdi32", "ole32", "ws232", "shell32");
    private static final Map<Character, NativeProxy.Converter> CONVERTERS = ImmutableMap.of(
        'i', NativeProxy::convertInt, 'l', Win32Util::getLong, 'p', NativeProxy::convertPointer
    );

    static Optional<DLLImpl> resolveNative(DLLEntry entry, String retFormat, String callFormat) {
        if (BLACKLISTED_DLLS.contains(entry.getDllName())) {
            return Optional.empty();
        } else {
            try {
                NativeLibrary library = NativeLibrary.getInstance(entry.getDllName());
                Function func = library.getFunction(entry.getFuncName());
                return isSupported(entry, retFormat) ? Optional.ofNullable(implFor(entry, func, convertersFor(callFormat), retFormat)) : Optional.empty();
            } catch (UnsatisfiedLinkError var7) {
                if (OS.IS_64BIT) {
                    try {
                        NativeLibrary libraryx = NativeLibrary.getInstance(entry.getDllName() + "64");
                        Function funcx = libraryx.getFunction(entry.getFuncName());
                        return isSupported(entry, retFormat) ? Optional.ofNullable(implFor(entry, funcx, convertersFor(callFormat), retFormat)) : Optional.empty();
                    } catch (UnsatisfiedLinkError var6) {
                    }
                }
                Gdx.app.log("NativeProxy", "Could not resolve native library " + entry.getDllName() + ". Cause: " + var7.getMessage());
                return Optional.empty();
            }
        }
    }

    private static boolean isSupported(DLLEntry entry, String retFormat) {
        if (!retFormat.equals("i") && !retFormat.equals("v") && !retFormat.equals("l") && !retFormat.equals("i32") && !retFormat.equals("p")) {
            return false;
        } else {
            char[] charArr = entry.getSpec().toCharArray();

            for (int i = 0; i < charArr.length; i++) {
                char c = charArr[i];
                if (c == 'i' && i < charArr.length - 2 && charArr[i + 1] == '3') {
                    i += 2;
                } else if (!CONVERTERS.containsKey(c)) {
                    return false;
                }
            }

            return true;
        }
    }

    private static DLLImpl implFor(DLLEntry entry, Function func, List<NativeProxy.Converter> converters, String retFormat) {
        DLLImpl result = null;
        switch (retFormat.charAt(0)) {
            case 'i':
            case 'l':
                if (retFormat.equals("i32")) {
                    result = forI32(converters, func);
                } else {
                    result = forI(converters, func);
                }
                break;
            case 'p':
                result = forP(converters, func);
                break;
            case 'v':
                result = forV(converters, func);
        }

        return result;
    }

    private static DLLImpl forI32(List<NativeProxy.Converter> converters, Function func) {
        return (api, context, args) -> {
            long ret = 0L;
            ret = func.invokeInt(applyConverters(converters, args));
            return Win32Util.rubyNum(ret);
        };
    }

    private static DLLImpl forI(List<NativeProxy.Converter> converters, Function func) {
        return (api, context, args) -> {
            long ret = 0L;
            if (OS.LONG_SIZE == 8) {
                ret = func.invokeLong(applyConverters(converters, args));
            } else {
                ret = func.invokeInt(applyConverters(converters, args));
            }

            return Win32Util.rubyNum(ret);
        };
    }

    private static DLLImpl forP(List<NativeProxy.Converter> converters, Function func) {
        return (api, context, args) -> {
            long ret = 0L;
            if (OS.IS_64BIT) {
                ret = func.invokeLong(applyConverters(converters, args));
            } else {
                ret = func.invokeInt(applyConverters(converters, args));
            }

            return Win32Util.rubyNum(ret);
        };
    }

    private static DLLImpl forV(List<NativeProxy.Converter> converters, Function func) {
        return (api, context, args) -> {
            func.invokeVoid(applyConverters(converters, args));
            return Win32Util.rubyNil();
        };
    }

    private static Object[] applyConverters(List<NativeProxy.Converter> converters, IRubyObject[] args) {
        Object[] result = new Object[args.length];

        for (int i = 0; i < result.length; i++) {
            result[i] = converters.get(i).convert(args[i]);
        }

        return result;
    }

    private static Object convertPointer(IRubyObject arg) {
        return arg instanceof RubyString ? Win32Util.getBytes(arg) : Pointer.createConstant(Win32Util.getLong(arg));
    }

    private static Object convertInt(IRubyObject arg) {
        return OS.LONG_SIZE == 8 ? Win32Util.getLong(arg) : Win32Util.getInt(arg);
    }

    private static List<NativeProxy.Converter> convertersFor(String format) {
        List<NativeProxy.Converter> converters = new ArrayList<>();

        for (int i = 0; i < format.length(); i++) {
            char c = format.charAt(i);
            if (c == 'i' && i < format.length() - 1 && format.charAt(i + 1) == '3') {
                converters.add(Win32Util::getInt);
                i += 2;
            } else {
                converters.add(CONVERTERS.get(c));
            }
        }

        return converters;
    }

    @FunctionalInterface
    private interface Converter {
        Object convert(IRubyObject var1);
    }
}
