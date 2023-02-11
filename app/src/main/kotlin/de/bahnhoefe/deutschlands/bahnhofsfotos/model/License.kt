package de.bahnhoefe.deutschlands.bahnhofsfotos.model

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import org.apache.commons.lang3.StringUtils
import java.lang.reflect.Type
import java.util.*

enum class License(val longName: String) {
    UNKNOWN("Unknown license"),
    CC0("CC0 1.0 Universell (CC0 1.0)"),
    CC4("CC BY-SA 4.0");

    class LicenseDeserializer : JsonDeserializer<License> {
        @Throws(JsonParseException::class)
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext
        ): License {
            return byName(json.asString)
        }
    }

    companion object {
        @JvmStatic
        fun byName(name: String): License {
            return Arrays.stream(values())
                .filter { license: License ->
                    license.toString() == name || StringUtils.equals(
                        license.longName,
                        name
                    )
                }
                .findFirst()
                .orElse(UNKNOWN)
        }
    }
}