package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

import java.util.Arrays;

import de.bahnhoefe.deutschlands.bahnhofsfotos.R;

public enum UpdatePolicy {
    MANUAL(R.id.rb_update_manual),
    NOTIFY(R.id.rb_update_notify),
    AUTOMATIC(R.id.rb_update_automatic);

    private final int id;

    UpdatePolicy(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static UpdatePolicy byId(int id) {
        return Arrays.stream(values())
                .filter(updatePolicy -> updatePolicy.getId() == id)
                .findFirst()
                .orElse(NOTIFY);
    }

    public static UpdatePolicy byName(String name) {
        return Arrays.stream(values())
                .filter(updatePolicy -> updatePolicy.toString().equals(name))
                .findFirst()
                .orElse(NOTIFY);
    }

}
