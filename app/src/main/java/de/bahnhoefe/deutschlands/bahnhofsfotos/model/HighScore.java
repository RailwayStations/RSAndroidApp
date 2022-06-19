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
        public HighScore deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            var highScore = new HighScore();

            AtomicInteger position = new AtomicInteger(0);
            AtomicInteger lastPhotos = new AtomicInteger(0);
            json.getAsJsonObject().entrySet().stream()
                    .map(entry -> toHighScoreItem(position, lastPhotos, entry))
                    .forEach(highScore.items::add);

            return highScore;
        }

        private HighScoreItem toHighScoreItem(AtomicInteger position, AtomicInteger lastPhotos, Map.Entry<String, JsonElement> entry) {
            int photos = entry.getValue().getAsInt();
            if (lastPhotos.get() == 0 || lastPhotos.get() > photos) {
                position.incrementAndGet();
            }
            lastPhotos.set(photos);
            return new HighScoreItem(entry.getKey(), photos, position.get());
        }
    }

}
