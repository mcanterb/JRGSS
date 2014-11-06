package org.jrgss;

import java.util.regex.Pattern;

/**
 * @author matt
 * @date 8/25/14
 */
public enum RGSSVersion {
    VXAce("RGSS3.*\\.dll"),
    VX("RGSS2.*\\.dll"),
    XP("RGSS1.*\\.dll");

    Pattern dllID;

    private RGSSVersion(String dllIDRegex) {
        this.dllID = Pattern.compile(dllIDRegex,Pattern.CASE_INSENSITIVE);
    }

    public static RGSSVersion parse(String dllID) {
        for(RGSSVersion version : RGSSVersion.values()) {
            if(version.dllID.matcher(dllID).matches()) {
                return version;
            }
        }
        return null;
    }

    public static RGSSVersion defaultVersion() {
        return VXAce;
    }


}
