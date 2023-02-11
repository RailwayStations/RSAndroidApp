package de.bahnhoefe.deutschlands.bahnhofsfotos.model

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type
import java.util.concurrent.atomic.AtomicInteger

class HighScore {
    private val items: MutableList<HighScoreItem> = ArrayList()
    fun getItems(): List<HighScoreItem> {
        return items
    }

    class HighScoreDeserializer : JsonDeserializer<HighScore> {
        @Throws(JsonParseException::class)
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext
        ): HighScore {
            val highScore = HighScore()
            val position = AtomicInteger(0)
            val lastPhotos = AtomicInteger(0)
            json.asJsonObject.entrySet().stream()
                .map { entry: Map.Entry<String, JsonElement> ->
                    toHighScoreItem(
                        position,
                        lastPhotos,
                        entry
                    )
                }
                .forEach { e: HighScoreItem -> highScore.items.add(e) }
            return highScore
        }

        private fun toHighScoreItem(
            position: AtomicInteger,
            lastPhotos: AtomicInteger,
            entry: Map.Entry<String, JsonElement>
        ): HighScoreItem {
            val photos = entry.value.asInt
            if (lastPhotos.get() == 0 || lastPhotos.get() > photos) {
                position.incrementAndGet()
            }
            lastPhotos.set(photos)
            return HighScoreItem(entry.key, photos, position.get())
        }
    }
}