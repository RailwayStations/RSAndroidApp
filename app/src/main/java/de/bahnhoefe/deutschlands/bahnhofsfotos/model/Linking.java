package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

import de.bahnhoefe.deutschlands.bahnhofsfotos.R;

public enum Linking {
    UNKNOWN(-1),
    NO(R.id.rbLinkingNo),
    INSTAGRAM(R.id.rbLinkingInstagram),
    SNAPCHAT(R.id.rbLinkingSnapchat),
    TWITTER(R.id.rbLinkingTwitter),
    XING(R.id.rbLinkingXing),
    WEBPAGE(R.id.rbLinkingWebpage);

    private final int id;

    Linking(final int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static Linking byId(int id) {
        for (Linking linking : values()) {
            if (linking.getId() == id) {
                return linking;
            }
        }
        return UNKNOWN;
    }

    public static Linking byName(String name) {
        for (Linking linking : values()) {
            if (linking.toString().equals(name)) {
                return linking;
            }
        }
        return UNKNOWN;
    }

}
