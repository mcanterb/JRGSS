package org.jrgss.rgssa;

import com.badlogic.gdx.files.FileHandle;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class EncryptedArchive {
   private MappedByteBuffer buffer;
   private static final byte[] RGSSAV3 = new byte[]{82, 71, 83, 83, 65, 68, 0, 3};
   private int key;
   private Map<String, EncryptedArchive.ArchivedFile> files = new HashMap<>();

   public EncryptedArchive(String fileName) throws IOException {
      RandomAccessFile raf = new RandomAccessFile(fileName, "r");
      this.buffer = raf.getChannel().map(MapMode.READ_ONLY, 0L, raf.length());
      this.buffer.order(ByteOrder.LITTLE_ENDIAN);

      for (int i = 0; i < RGSSAV3.length; i++) {
         if (RGSSAV3[i] != this.buffer.get()) {
            throw new IOException("Invalid RGSSAD v3 file!");
         }
      }

      this.key = this.buffer.getInt();
      this.key = this.key * 9 + 3;

      EncryptedArchive.ArchivedFile file;
      while ((file = this.getFileInfoFromTOC(this.buffer)) != null) {
         this.files.put(file.name.toLowerCase(), file);
      }
   }

   public FileHandle openFile(String name) {
      EncryptedArchive.ArchivedFile f = this.files.get(name.toLowerCase());
      if (f == null) {
         return null;
      } else {
         ByteBuffer fileBuffer = this.buffer.duplicate();
         ((Buffer)fileBuffer).position(f.offset);
         ((Buffer)fileBuffer).limit(f.offset + f.size);
         return new EncryptedFileHandle(fileBuffer, f);
      }
   }

   public static void main(String[] args) throws Exception {
      EncryptedArchive archive = new EncryptedArchive("/Users/matt/Desktop/Vidar - 0700.app/Contents/Resources/Vidar/Game.rgss3a");
      String prefix = "/Users/matt/Vidar-contents/";

      for (Entry<String, EncryptedArchive.ArchivedFile> file : archive.files.entrySet()) {
         System.out.println(file.getKey() + ": " + file.getValue());
         String path = prefix + file.getKey().replaceAll("\\\\", "/");
         File file1 = new File(path);
         file1.mkdirs();
         file1.delete();
         FileOutputStream stream = new FileOutputStream(prefix + file.getKey().replaceAll("\\\\", "/"));
         stream.write(archive.openFile(file.getKey()).readBytes());
         stream.close();
      }
   }

   private EncryptedArchive.ArchivedFile getFileInfoFromTOC(ByteBuffer buffer) {
      EncryptedArchive.ArchivedFile ret = new EncryptedArchive.ArchivedFile();
      ret.offset = buffer.getInt() ^ this.key;
      if (ret.offset == 0) {
         ((Buffer)buffer).position(buffer.position() + 12);
         return null;
      } else {
         ret.size = buffer.getInt() ^ this.key;
         ret.key = buffer.getInt() ^ this.key;
         int nameLength = buffer.getInt() ^ this.key;
         ret.name = this.getName(buffer, nameLength);
         return ret;
      }
   }

   private String getName(ByteBuffer buffer, int count) {
      ByteBuffer result = ByteBuffer.allocate(count);
      int keyIndex = 0;

      for (int i = 0; i < count; i++) {
         result.put((byte)((255 << keyIndex * 8 & this.key) >>> keyIndex * 8 ^ buffer.get()));
         if (++keyIndex == 4) {
            keyIndex = 0;
         }
      }

      ((Buffer)result).flip();
      return Charset.forName("UTF-8").decode(result).toString();
   }

   public MappedByteBuffer getBuffer() {
      return this.buffer;
   }

   public int getKey() {
      return this.key;
   }

   public Map<String, EncryptedArchive.ArchivedFile> getFiles() {
      return this.files;
   }

   public void setBuffer(MappedByteBuffer buffer) {
      this.buffer = buffer;
   }

   public void setKey(int key) {
      this.key = key;
   }

   public void setFiles(Map<String, EncryptedArchive.ArchivedFile> files) {
      this.files = files;
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof EncryptedArchive)) {
         return false;
      } else {
         EncryptedArchive other = (EncryptedArchive)o;
         if (!other.canEqual(this)) {
            return false;
         } else {
            Object this$buffer = this.getBuffer();
            Object other$buffer = other.getBuffer();
            if (this$buffer == null ? other$buffer == null : this$buffer.equals(other$buffer)) {
               if (this.getKey() != other.getKey()) {
                  return false;
               } else {
                  Object this$files = this.getFiles();
                  Object other$files = other.getFiles();
                  return this$files == null ? other$files == null : this$files.equals(other$files);
               }
            } else {
               return false;
            }
         }
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof EncryptedArchive;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = 1;
      Object $buffer = this.getBuffer();
      result = result * 59 + ($buffer == null ? 43 : $buffer.hashCode());
      result = result * 59 + this.getKey();
      Object $files = this.getFiles();
      return result * 59 + ($files == null ? 43 : $files.hashCode());
   }

   @Override
   public String toString() {
      return "EncryptedArchive(buffer=" + this.getBuffer() + ", key=" + this.getKey() + ", files=" + this.getFiles() + ")";
   }

   public static class ArchivedFile {
      int offset;
      int size;
      int key;
      String name;

      @Override
      public String toString() {
         return "EncryptedArchive.ArchivedFile(offset=" + this.offset + ", size=" + this.size + ", key=" + this.key + ", name=" + this.name + ")";
      }
   }
}
