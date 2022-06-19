package de.bahnhoefe.deutschlands.bahnhofsfotos.util;

import androidx.annotation.NonNull;

public class KeyValueSpinnerItem {

    private final String spinnerText;
    private final String value;

    public KeyValueSpinnerItem(String spinnerText, String value) {
        this.spinnerText = spinnerText;
        this.value = value;
    }

    public String getSpinnerText() {
        return spinnerText;
    }

    public String getValue() {
        return value;
    }

    @Override
    @NonNull
    public String toString() {
        return spinnerText;
    }

}
