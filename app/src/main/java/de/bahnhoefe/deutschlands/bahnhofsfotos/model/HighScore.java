package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class HighScore {

    private final List<HighScoreItem> items = new ArrayList<>();

    public List<HighScoreItem> getItems() {
        return items;
    }


    public static class HighScoreDeserializer implements JsonDeserializer<HighScore> {
        public HighScore deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context)
                throws JsonParseException {
            final var highScore = new HighScore();

            final AtomicInteger position = new AtomicInteger(0);
            final AtomicInteger lastPhotos = new AtomicInteger(0);
            json.getAsJsonObject().entrySet().stream()
                    .map(entry -> toHighScoreItem(position, lastPhotos, entry))
                    .forEach(highScore.items::add);

            return highScore;
        }

        private HighScoreItem toHighScoreItem(final AtomicInteger position, final AtomicInteger lastPhotos, final Map.Entry<String, JsonElement> entry) {
            final int photos = entry.getValue().getAsInt();
            if (lastPhotos.get() == 0 || lastPhotos.get() > photos) {
                position.incrementAndGet();
            }
            lastPhotos.set(photos);
            return new HighScoreItem(entry.getKey(), photos, position.get());
        }
    }

}
