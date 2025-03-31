package org.jrgss.rgssa;

import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.StreamUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import org.jruby.Ruby;
import org.jruby.RubyString;

public class EncryptedFileHandle extends FileHandle {
   protected ByteBuffer buffer;
   protected EncryptedArchive.ArchivedFile fileInfo;

   public EncryptedFileHandle(ByteBuffer buffer, EncryptedArchive.ArchivedFile fileInfo) {
      this.file = new File(fileInfo.name);
      this.buffer = buffer;
      this.type = FileType.External;
      this.fileInfo = fileInfo;
   }

   @Override
   public InputStream read() {
      return new InputStream() {
         ByteBuffer buffer;
         int keyIndex;
         int runningKey;

         {
            this.buffer = EncryptedFileHandle.this.buffer.duplicate();
            this.keyIndex = 0;
            this.runningKey = EncryptedFileHandle.this.fileInfo.key;
         }

         @Override
         public int read() throws IOException {
            if (this.buffer.remaining() == 0) {
               return -1;
            } else {
               int ret = ((255 << this.keyIndex * 8 & this.runningKey) >>> this.keyIndex * 8 ^ this.buffer.get()) & 0xFF;
               this.keyIndex++;
               if (this.keyIndex == 4) {
                  this.keyIndex = 0;
                  this.runningKey = this.runningKey * 7 + 3;
               }

               return ret;
            }
         }
      };
   }

   @Override
   public byte[] readBytes() {
      InputStream input = this.read();

      byte[] ex;
      try {
         ex = StreamUtils.copyStreamToByteArray(input, this.fileInfo.size);
      } catch (IOException var6) {
         throw new GdxRuntimeException("Error reading file: " + this, var6);
      } finally {
         StreamUtils.closeQuietly(input);
      }

      return ex;
   }

   public RubyString to_str() {
      return new RubyString(Ruby.getGlobalRuntime(), Ruby.getGlobalRuntime().getString(), this.readBytes());
   }
}
