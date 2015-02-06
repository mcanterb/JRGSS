package org.jrgss.extractor.mspack;

import com.sun.jna.Function;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

/**
 * @author matt
 * @date 11/9/14
 */
public class CabDecompressor extends Structure {

    public Pointer open;
    public Pointer close;
    public Pointer search;
    public Pointer append;
    public Pointer prepend;
    public Pointer extract;
    public Pointer setParam;
    public Pointer lastError;


    public MSCabinet open(String filename) {
        Function f = Function.getFunction(open);
        return (MSCabinet)f.invoke(MSCabinet.class, new Object[]{this,filename});
    }

    public MSCabinet search(String filename) {
        Function f = Function.getFunction(search);
        return (MSCabinet)f.invoke(MSCabinet.class, new Object[]{this,filename});
    }

    public int lastError() {
        Function f = Function.getFunction(lastError);
        return f.invokeInt(new Object[]{this});
    }

    /*

Data Fields

struct mscabd_cabinet *(* 	open )(struct mscab_decompressor *self, const char *filename)
 	Opens a cabinet file and reads its contents.
void(* 	close )(struct mscab_decompressor *self, struct mscabd_cabinet *cab)
 	Closes a previously opened cabinet or cabinet set.
struct mscabd_cabinet *(* 	search )(struct mscab_decompressor *self, const char *filename)
 	Searches a regular file for embedded cabinets.
int(* 	append )(struct mscab_decompressor *self, struct mscabd_cabinet *cab, struct mscabd_cabinet *nextcab)
 	Appends one mscabd_cabinet to another, forming or extending a cabinet set.
int(* 	prepend )(struct mscab_decompressor *self, struct mscabd_cabinet *cab, struct mscabd_cabinet *prevcab)
 	Prepends one mscabd_cabinet to another, forming or extending a cabinet set.
int(* 	extract )(struct mscab_decompressor *self, struct mscabd_file *file, const char *filename)
 	Extracts a file from a cabinet or cabinet set.
int(* 	set_param )(struct mscab_decompressor *self, int param, int value)
 	Sets a CAB decompression engine parameter.
int(* 	last_error )(struct mscab_decompressor *self)
 	Returns the error code set by the most recently called method.
     */


    public CabDecompressor() {
        super();
    }

    public CabDecompressor(Pointer p) {
        super(p);
        read();
    }

    @Override
    protected List getFieldOrder() {
        return Arrays.asList(
                "open",
                "close",
                "search",
                "append",
                "prepend",
                "extract",
                "setParam",
                "lastError"
        );
    }

    public static class ByReference extends CabDecompressor implements Structure.ByReference {
        public ByReference() {
            super();
        }

        public ByReference(Pointer p) {
            super(p);
        }
    }
}
