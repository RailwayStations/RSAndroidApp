package de.bahnhoefe.deutschlands.bahnhofsfotos.util;

public class KeyValueSpinnerItem {

    private final String spinnerText;
    private final String value;

    public KeyValueSpinnerItem(final String spinnerText, final String value) {
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
    public String toString() {
        return spinnerText;
    }

}
