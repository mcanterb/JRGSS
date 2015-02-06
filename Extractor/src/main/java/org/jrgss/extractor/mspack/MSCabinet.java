package org.jrgss.extractor.mspack;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import lombok.Data;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * @author matt
 * @date 11/9/14
 */
public class MSCabinet extends Structure{

    public ByReference next;
    public String filename;
    public long baseOffset;
    public int length;
    public ByReference prevcab;
    public ByReference nextcab;
    public String prevname;
    public String nextname;
    public String previnfo;
    public String nextinfo;
    public CabinetFile.ByReference files;
    public Pointer folders;
    public short setID;
    public short setIndex;
    public short reserved;
    public int flags;

    /*
    struct mscabd_cabinet * 	next
 	The next cabinet in a chained list, if this cabinet was opened with mscab_decompressor::search().
const char * 	filename
 	The filename of the cabinet.
off_t 	base_offset
 	The file offset of cabinet within the physical file it resides in.
unsigned int 	length
 	The length of the cabinet file in bytes.
struct mscabd_cabinet * 	prevcab
 	The previous cabinet in a cabinet set, or NULL.
struct mscabd_cabinet * 	nextcab
 	The next cabinet in a cabinet set, or NULL.
char * 	prevname
 	The filename of the previous cabinet in a cabinet set, or NULL.
char * 	nextname
 	The filename of the next cabinet in a cabinet set, or NULL.
char * 	previnfo
 	The name of the disk containing the previous cabinet in a cabinet set, or NULL.
char * 	nextinfo
 	The name of the disk containing the next cabinet in a cabinet set, or NULL.
struct mscabd_file * 	files
 	A list of all files in the cabinet or cabinet set.
struct mscabd_folder * 	folders
 	A list of all folders in the cabinet or cabinet set.
unsigned short 	set_id
 	The set ID of the cabinet.
unsigned short 	set_index
 	The index number of the cabinet within the set.
unsigned short 	header_resv
 	The number of bytes reserved in the header area of the cabinet.
int 	flags
 	Header flags.
     */

    public Iterator<CabinetFile> getFiles() {
        return new Iterator<CabinetFile>() {
            CabinetFile current = files;
            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public CabinetFile next() {
                CabinetFile ret = current;
                if(current.next == null) {
                    current = null;
                } else {
                    current = new CabinetFile(current.next);
                }
                return ret;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Don't do this");
            }
        };
    }


    public MSCabinet() {
        super();
    }

    public MSCabinet(Pointer p) {
        super(p);
        read();
    }

    public static class ByReference extends MSCabinet implements Structure.ByReference {
        public ByReference() { super(); }
        public ByReference(Pointer p) { super(p);}
    }

    @Override
    protected List getFieldOrder() {
        return Arrays.asList(
                "next",
                "filename",
                "baseOffset",
                "length",
                "prevcab",
                "nextcab",
                "prevname",
                "nextname",
                "previnfo",
                "nextinfo",
                "files",
                "folders",
                "setID",
                "setIndex",
                "reserved",
                "flags"
        );
    }

}
