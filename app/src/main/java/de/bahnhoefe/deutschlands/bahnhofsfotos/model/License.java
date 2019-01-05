package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

import org.apache.commons.lang3.StringUtils;

public enum License {
    UNKNOWN(null),
    CC0("CC0 1.0 Universell (CC0 1.0)"),
    CC4("CC BY-SA 4.0");

    private final String longName;

    License(final String longName) {
        this.longName = longName;
    }

    public static License byName(String name) {
        for (License license : values()) {
            if (license.toString().equals(name)
                    || StringUtils.equals(license.longName, name)) {
                return license;
            }
        }
        return UNKNOWN;
    }

    public String getLongName() {
        return longName;
    }

}
