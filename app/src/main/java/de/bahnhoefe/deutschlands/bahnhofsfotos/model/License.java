package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import org.apache.commons.lang3.StringUtils;

public enum License {
    UNKNOWN(null),
    CC0("CC0 1.0 Universell (CC0 1.0)"),
    CC4("CC BY-SA 4.0");

    private final String longName;

    License(final String longName) {
        this.longName = longName;
    }

    public static License byName(final String name) {
        for (final License license : values()) {
            if (license.toString().equals(name)
                    || StringUtils.equals(license.longName, name)) {
                return license;
            }
        }
        return UNKNOWN;
    }

    public String getLongName() {
        return longName;
    }

    public static class LicenseDeserializer implements JsonDeserializer<License> {
        public License deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context)
                throws JsonParseException {
            return License.byName(json.getAsString());
        }
    }

}
