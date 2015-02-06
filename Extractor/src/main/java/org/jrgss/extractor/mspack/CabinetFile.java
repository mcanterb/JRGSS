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
public class CabinetFile extends Structure {
    public Pointer next;
    public String filename;
    public int length;
    public int attribs;
    public byte hour;
    public byte minute;
    public byte second;
    public byte day;
    public byte month;
    public int year;
    public CabinetFolder.ByReference folder;
    public int offset;

    /*
    struct mscabd_file * 	next
 	The next file in the cabinet or cabinet set, or NULL if this is the final file.
char * 	filename
 	The filename of the file.
unsigned int 	length
 	The uncompressed length of the file, in bytes.
int 	attribs
 	File attributes.
char 	time_h
 	File's last modified time, hour field.
char 	time_m
 	File's last modified time, minute field.
char 	time_s
 	File's last modified time, second field.
char 	date_d
 	File's last modified date, day field.
char 	date_m
 	File's last modified date, month field.
int 	date_y
 	File's last modified date, year field.
struct mscabd_folder * 	folder
 	A pointer to the folder that contains this file.
unsigned int 	offset
 	The uncompressed offset of this file in its folder.
     */



    @Override
    protected List getFieldOrder() {
        return Arrays.asList(
                "next",
                "filename",
                "length",
                "attribs",
                "hour",
                "minute",
                "second",
                "day",
                "month",
                "year",
                "folder",
                "offset"
        );
    }

    public CabinetFile() {
        super();
    }

    public CabinetFile(Pointer p) {
        super(p);
        read();

    }

    public static class ByReference extends CabinetFile implements Structure.ByReference {
    }
}
