package de.bahnhoefe.deutschlands.bahnhofsfotos.util;

public class NavItem {

    public final String text;
    public final int icon;

    public NavItem(final String text, final Integer icon) {
        this.text = text;
        this.icon = icon;
    }

    @Override
    public String toString() {
        return text;
    }

}
