package org.jrgss.extractor.mspack;


import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import lombok.Data;

import java.util.Arrays;
import java.util.List;

/**
 * @author matt
 * @date 11/10/14
 */
@Data
public class CabinetFolder extends Structure{

    public ByReference next;
    public int compressionType;
    public int numBlocks;

    /*
    struct mscabd_folder * 	next
 	A pointer to the next folder in this cabinet or cabinet set, or NULL if this is the final folder.
int 	comp_type
 	The compression format used by this folder.
unsigned int 	num_blocks
 	The total number of data blocks used by this folder.
     */

    public CabinetFolder() {
        super();
    }

    public CabinetFolder(Pointer p) {
        super(p);
        read();
    }

    public static class ByReference extends CabinetFolder implements Structure.ByReference {
        public ByReference() {
            super();
        }
        public ByReference(Pointer p) { super(p);}
    }

    @Override
    protected List getFieldOrder() {
        return Arrays.asList(
                "next",
                "compressionType",
                "numBlocks"
        );
    }
}
