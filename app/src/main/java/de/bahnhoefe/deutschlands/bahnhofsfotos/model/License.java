package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

public enum License {
    UNKNOWN,
    CC0,
    CC4;

    public static License byName(String name) {
        for (License license : values()) {
            if (license.toString().equals(name)) {
                return license;
            }
        }
        return UNKNOWN;
    }

}
