package org.jrgss;

import java.io.File;
import org.ini4j.Wini;

public class ConfigReader {
   Wini ini;

   public ConfigReader(String path) {
      try {
         this.ini = new Wini(new File(path));
      } catch (Exception var3) {
         throw new RuntimeException("Could not read from ini file!");
      }
   }

   public String getTitle() {
      return this.ini.get("Game", "Title");
   }

   public String getScripts() {
      return this.ini.get("Game", "Scripts");
   }

   public RGSSVersion getRGSSVersion() {
      String library = this.ini.get("Game", "Library");
      if (library == null) {
         System.out.println("Could not determine RGSS Version. Using default!");
         return RGSSVersion.defaultVersion();
      } else {
         int dllIndex = library.toLowerCase().indexOf("rgss");
         if (dllIndex == -1) {
            System.out.println("Could not determine RGSS Version. Using default!");
            return RGSSVersion.defaultVersion();
         } else {
            RGSSVersion version = RGSSVersion.parse(library.substring(dllIndex));
            if (version == null) {
               System.out.println("Could not determine RGSS Version. Using default!");
               return RGSSVersion.defaultVersion();
            } else {
               return version;
            }
         }
      }
   }

   public RTPVersion getRTPVersion() {
      String rtp = this.ini.get("Game", "RTP");
      if (rtp == null) {
         return RTPVersion.None;
      } else {
         try {
            return RTPVersion.valueOf(rtp);
         } catch (IllegalArgumentException var3) {
            return RTPVersion.None;
         }
      }
   }

   public Wini getIni() {
      return this.ini;
   }

   public void setIni(Wini ini) {
      this.ini = ini;
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof ConfigReader)) {
         return false;
      } else {
         ConfigReader other = (ConfigReader)o;
         if (!other.canEqual(this)) {
            return false;
         } else {
            Object this$ini = this.getIni();
            Object other$ini = other.getIni();
            return this$ini == null ? other$ini == null : this$ini.equals(other$ini);
         }
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof ConfigReader;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = 1;
      Object $ini = this.getIni();
      return result * 59 + ($ini == null ? 43 : $ini.hashCode());
   }

   @Override
   public String toString() {
      return "ConfigReader(ini=" + this.getIni() + ")";
   }
}
