package org.jrgss;

import lombok.Data;
import org.ini4j.Wini;

import java.io.File;

/**
 * @author matt
 * @date 8/25/14
 */
@Data
public class ConfigReader {

    Wini ini;

    public ConfigReader(String path) {
        try {
            this.ini = new Wini(new File(path));
        } catch (Exception io) {
            throw new RuntimeException("Could not read from ini file!");
        }
    }

    public String getTitle() {
        return ini.get("Game", "Title");
    }

    public String getScripts() {
        return ini.get("Game", "Scripts");
    }

    public RGSSVersion getRGSSVersion() {
        String library = ini.get("Game", "Library");
        if (library == null) {
            System.out.println("Could not determine RGSS Version. Using default!");
            return RGSSVersion.defaultVersion();
        }
        int dllIndex = library.toLowerCase().indexOf("rgss");
        if (dllIndex == -1) {
            System.out.println("Could not determine RGSS Version. Using default!");
            return RGSSVersion.defaultVersion();
        }
        RGSSVersion version = RGSSVersion.parse(library.substring(dllIndex));
        if(version == null) {
            System.out.println("Could not determine RGSS Version. Using default!");
            return RGSSVersion.defaultVersion();
        }
        return version;
    }

    public RTPVersion getRTPVersion() {
        String rtp = ini.get("Game", "RTP");
        if(rtp == null) {
            return RTPVersion.None;
        }
        try{
            RTPVersion version = RTPVersion.valueOf(rtp);
            return version;
        } catch (IllegalArgumentException ex) {
            return RTPVersion.None;
        }
    }

}
