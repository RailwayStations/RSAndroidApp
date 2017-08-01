package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

import de.bahnhoefe.deutschlands.bahnhofsfotos.R;

public enum License {
    UNKNOWN(-1),
    CC0(R.id.rbCC0),
    CC4(R.id.rbCC40);

    private final int id;

    License(final int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static License byId(int id) {
        for (License license : values()) {
            if (license.getId() == id) {
                return license;
            }
        }
        return UNKNOWN;
    }

    public static License byName(String name) {
        for (License license : values()) {
            if (license.toString().equals(name)) {
                return license;
            }
        }
        return UNKNOWN;
    }

}
