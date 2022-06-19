package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Type;
import java.util.Arrays;

public enum License {
    UNKNOWN(null),
    CC0("CC0 1.0 Universell (CC0 1.0)"),
    CC4("CC BY-SA 4.0");

    private final String longName;

    License(String longName) {
        this.longName = longName;
    }

    public static License byName(String name) {
        return Arrays.stream(values())
                .filter(license -> license.toString().equals(name)
                        || StringUtils.equals(license.longName, name))
                .findFirst()
                .orElse(UNKNOWN);
    }

    public String getLongName() {
        return longName;
    }

    public static class LicenseDeserializer implements JsonDeserializer<License> {
        public License deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            return License.byName(json.getAsString());
        }
    }

}
