package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HighScore {

    private List<HighScoreItem> items = new ArrayList<>();

    public List<HighScoreItem> getItems() {
        return items;
    }


    public static class HighScoreDeserializer implements JsonDeserializer<HighScore> {
        public HighScore deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context)
                throws JsonParseException {
            final HighScore highScore = new HighScore();

            int position = 0;
            int lastPhotos = 0;
            for (final Map.Entry<String, JsonElement> entry : json.getAsJsonObject().entrySet()) {
                final String name = entry.getKey();
                final int photos = entry.getValue().getAsInt();
                if (lastPhotos == 0 || lastPhotos > photos) {
                    position++;
                }
                lastPhotos = photos;
                highScore.getItems().add(new HighScoreItem(name, photos, position));
            }

            return highScore;
        }
    }

}
