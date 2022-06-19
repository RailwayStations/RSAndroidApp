package de.bahnhoefe.deutschlands.bahnhofsfotos.util;

import androidx.annotation.NonNull;

public class NavItem {

    public final String text;
    public final int icon;

    public NavItem(String text, Integer icon) {
        this.text = text;
        this.icon = icon;
    }

    @Override
    @NonNull
    public String toString() {
        return text;
    }

}
