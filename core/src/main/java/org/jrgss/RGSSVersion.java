package org.jrgss;

import java.util.regex.Pattern;

public enum RGSSVersion {
   VXAce("RGSS3.*\\.dll"),
   VX("RGSS2.*\\.dll"),
   XP("RGSS1.*\\.dll");

   Pattern dllID;

   private RGSSVersion(String dllIDRegex) {
      this.dllID = Pattern.compile(dllIDRegex, 2);
   }

   public static RGSSVersion parse(String dllID) {
      for (RGSSVersion version : values()) {
         if (version.dllID.matcher(dllID).matches()) {
            return version;
         }
      }

      return null;
   }

   public static RGSSVersion defaultVersion() {
      return VXAce;
   }
}
