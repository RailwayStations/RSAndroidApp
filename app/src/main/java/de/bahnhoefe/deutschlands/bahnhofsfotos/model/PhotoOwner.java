package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

import de.bahnhoefe.deutschlands.bahnhofsfotos.R;

public enum PhotoOwner {
    UNKNOWN(-1, false),
    YES(R.id.rbOwnPhotoYes, true),
    NO(R.id.rbOwnPhotoNo, false);

    private final int id;
    private boolean owner;

    PhotoOwner(final int id, final boolean owner) {
        this.id = id;
        this.owner = owner;
    }

    public int getId() {
        return id;
    }

    public static PhotoOwner byId(int id) {
        for (PhotoOwner photoOwner : values()) {
            if (photoOwner.getId() == id) {
                return photoOwner;
            }
        }
        return UNKNOWN;
    }

    public static PhotoOwner byName(String name) {
        for (PhotoOwner photoOwner : values()) {
            if (photoOwner.toString().equals(name)) {
                return photoOwner;
            }
        }
        return UNKNOWN;
    }

    public boolean isOwner() {
        return owner;
    }
}
