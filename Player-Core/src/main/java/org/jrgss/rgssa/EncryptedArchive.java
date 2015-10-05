package org.jrgss.rgssa;

import com.badlogic.gdx.files.FileHandle;
import lombok.Data;
import lombok.ToString;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * @author matt
 * @date 8/19/14
 */
@Data
public class EncryptedArchive {

    private MappedByteBuffer buffer;
    private static final byte[] RGSSAV3 = new byte[] {0x52,0x47,0x53,0x53,0x41,0x44,0x00,0x03}; //RGSSAD v3 identifier
    private int key;
    private Map<String, ArchivedFile> files = new HashMap<>();

    public EncryptedArchive(String fileName) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(fileName, "r");
        buffer = raf.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, raf.length());
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        for(int i = 0; i < RGSSAV3.length; i++) {
            if(RGSSAV3[i] != buffer.get()) throw new IOException("Invalid RGSSAD v3 file!");
        }
        key = buffer.getInt();
        System.out.println("Key is "+Integer.toHexString(key));
        key = (key*9) + 3;
        System.out.println("Offset is "+buffer.position());
        ArchivedFile file;
        while((file = getFileInfoFromTOC(buffer)) != null) {
            files.put(file.name.toLowerCase(), file);
        }
    }

    public FileHandle openFile(String name) {
        ArchivedFile f = files.get(name.toLowerCase());
        if(f == null) return null;
        ByteBuffer fileBuffer = buffer.duplicate();
        fileBuffer.position(f.offset);
        fileBuffer.limit(f.offset+f.size);
        System.out.println(fileBuffer.remaining());
        return new EncryptedFileHandle(fileBuffer, f);
    }

    public static void main(String [] args) throws Exception {
        EncryptedArchive archive = new EncryptedArchive("/Users/matt/Downloads/ALWAYS_SOMETIMES_MONSTERS_121_NO_DRM/Game.rgss3a");
        String prefix = "/Users/matt/Dropbox/ASM/";
        for(Map.Entry<String, ArchivedFile> file : archive.files.entrySet()) {
            System.out.println(file.getKey() + ": "+file.getValue());
            String path = prefix+file.getKey().replaceAll("\\\\","/");
            File file1 = new File(path);
            file1.mkdirs();
            file1.delete();
            FileOutputStream stream = new FileOutputStream(prefix+file.getKey().replaceAll("\\\\","/"));
            stream.write(archive.openFile(file.getKey()).readBytes());
            stream.close();
        }

    }

    private ArchivedFile getFileInfoFromTOC(ByteBuffer buffer) {
        ArchivedFile ret = new ArchivedFile();
        ret.offset = buffer.getInt() ^ key;
        if(ret.offset == 0) { //We're done with this record in the TOC
            buffer.position(buffer.position()+12);
            return null;
        }
        ret.size = buffer.getInt() ^ key;
        ret.key = buffer.getInt() ^ key;
        int nameLength = buffer.getInt() ^ key;
        ret.name = getName(buffer, nameLength);
        return ret;
    }

    private String getName(ByteBuffer buffer, int count) {
        ByteBuffer result = ByteBuffer.allocate(count);
        int keyIndex = 0;
        for(int i = 0; i < count; i++) {
            result.put((byte)(((((0xFF)<<(keyIndex*8)) & key) >>> (keyIndex*8)) ^ buffer.get()));
            keyIndex++;
            if(keyIndex == 4) keyIndex = 0;
        }
        result.flip();
        return Charset.forName("UTF-8").decode(result).toString();
    }


    @ToString
    public static class ArchivedFile {
        int offset;
        int size;
        int key;
        String name;


    }


}
