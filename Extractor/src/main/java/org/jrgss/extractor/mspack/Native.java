package org.jrgss.extractor.mspack;

import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

/**
 * @author matt
 * @date 11/9/14
 */

public class Native {

    static {
        //net.jpountz.util.Native.load(); //Make sure the shared lib is loaded first
        String path = load();
        com.sun.jna.Native.register(NativeLibrary.getInstance(path));
    }

    private static boolean loaded = false;

    static native CabDecompressor mspack_create_cab_decompressor(Pointer system);

    private static String arch() {
        return System.getProperty("os.arch");
    }

    private static OS os() {
        String osName = System.getProperty("os.name");
        if (osName.contains("Linux")) {
            return OS.LINUX;
        } else if (osName.contains("Mac")) {
            return OS.MAC;
        } else if (osName.contains("Windows")) {
            return OS.WINDOWS;
        } else if (osName.contains("Solaris")) {
            return OS.SOLARIS;
        } else {
            throw new UnsupportedOperationException("Unsupported operating system: "
                    + osName);
        }
    }

    private static String resourceName() {
        OS os = os();
        return "/" + os.name + "/" + arch() + "/libmspack." + os.libExtension;
    }

    public static synchronized boolean isLoaded() {
        return loaded;
    }

    public static synchronized String load() {
        if (loaded) {
            return null;
        }
        String resourceName = resourceName();
        InputStream is = com.sun.jna.Native.class.getResourceAsStream(resourceName);
        if (is == null) {
            throw new UnsupportedOperationException("Unsupported OS/arch, cannot find " + resourceName + ". Please try building from source.");
        }
        File tempLib;
        try {
            tempLib = File.createTempFile("libmspack", "." + os().libExtension);
            // copy to tempLib
            FileOutputStream out = new FileOutputStream(tempLib);
            try {
                byte[] buf = new byte[4096];
                while (true) {
                    int read = is.read(buf);
                    if (read == -1) {
                        break;
                    }
                    out.write(buf, 0, read);
                }
                try {
                    out.close();
                    out = null;
                } catch (IOException e) {
                    // ignore
                }
                System.load(tempLib.getAbsolutePath());
                loaded = true;
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e) {
                    // ignore
                }
                if (tempLib != null && tempLib.exists()) {
                    if (!loaded) {
                        tempLib.delete();
                    } else {
                        // try to delete on exit, does it work on Windows?
                        tempLib.deleteOnExit();
                        return tempLib.getAbsolutePath();
                    }

                }

            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError("Cannot unpack libmspack");
        }
        return null;
    }

    public static void main(String args[]) {
        CabDecompressor decompressor = mspack_create_cab_decompressor(Pointer.NULL);

        MSCabinet cabinet = decompressor.search("/Users/matt/Downloads/Vidar.exe");
        if (cabinet != null) {
            System.out.println(cabinet.filename);
        } else {
            System.out.println("Error: " + decompressor.lastError());
        }
        Iterator<CabinetFile> fileIter = cabinet.getFiles();
        while(fileIter.hasNext()) {
            CabinetFile file = fileIter.next();
            System.out.println(file.getFilename());
        }

    }


    private enum OS {
        // Even on Windows, the default compiler from cpptasks (gcc) uses .so as a shared lib extension
        WINDOWS("win32", "so"), LINUX("linux", "so"), MAC("darwin", "dylib"), SOLARIS("solaris", "so");
        public final String name, libExtension;

        private OS(String name, String libExtension) {
            this.name = name;
            this.libExtension = libExtension;
        }
    }
}
