package de.bahnhoefe.deutschlands.bahnhofsfotos.util;

/**
 * Created by android_oma on 19.02.17.
 */

public class NavItem {

        public final String text;
        public final int icon;
        public NavItem(String text, Integer icon) {
            this.text = text;
            this.icon = icon;
        }
        @Override
        public String toString() {
            return text;
        }

}
