package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

import de.bahnhoefe.deutschlands.bahnhofsfotos.R;

public enum UpdatePolicy {
    MANUAL(R.id.rb_update_manual),
    NOTIFY(R.id.rb_update_notify),
    AUTOMATIC(R.id.rb_update_automatic);

    private final int id;

    UpdatePolicy(final int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static UpdatePolicy byId(final int id) {
        for (final UpdatePolicy updatePolicy : values()) {
            if (updatePolicy.getId() == id) {
                return updatePolicy;
            }
        }
        return NOTIFY;
    }


    public static UpdatePolicy byName(final String name) {
        for (final UpdatePolicy updatePolicy : values()) {
            if (updatePolicy.toString().equals(name)) {
                return updatePolicy;
            }
        }
        return NOTIFY;
    }

}
