package org.jrgss.rgssa;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.StreamUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jruby.Ruby;
import org.jruby.RubyString;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import static org.jrgss.rgssa.EncryptedArchive.ArchivedFile;

/**
 * @author matt
 * @date 8/19/14
 */
public class EncryptedFileHandle extends FileHandle {
    protected ByteBuffer buffer;
    protected ArchivedFile fileInfo;

    public EncryptedFileHandle(ByteBuffer buffer, ArchivedFile fileInfo) {
        this.file = new File(fileInfo.name);
        this.buffer = buffer;
        this.type = Files.FileType.External;
        this.fileInfo = fileInfo;
    }

    public InputStream read () {
        return new InputStream() {
            ByteBuffer buffer = EncryptedFileHandle.this.buffer.duplicate();
            int keyIndex = 0;
            int runningKey = fileInfo.key;
            @Override
            public int read() throws IOException {
                if(buffer.remaining() == 0) return -1;
                int ret = ((((((0xFF)<<(keyIndex*8)) & runningKey) >>> (keyIndex*8)) ^ buffer.get())&0xFF);
                keyIndex++;
                if(keyIndex == 4) {
                    keyIndex = 0;
                    runningKey = runningKey*7 + 3;
                }
                return ret;
            }
        };
    }

    public byte[] readBytes () {
        InputStream input = read();
        try {
            return StreamUtils.copyStreamToByteArray(input, fileInfo.size);
        } catch (IOException ex) {
            throw new GdxRuntimeException("Error reading file: " + this, ex);
        } finally {
            StreamUtils.closeQuietly(input);
        }
    }

    public RubyString to_str() {
        return new RubyString(Ruby.getGlobalRuntime(), Ruby.getGlobalRuntime().getString(), readBytes());
    }

}
