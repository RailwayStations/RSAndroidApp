package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.annotations.JsonAdapter;

import java.lang.reflect.Type;

import de.bahnhoefe.deutschlands.bahnhofsfotos.R;

@JsonAdapter(UploadState.Serializer.class)
public enum UploadState {
    NOT_YET_SENT(R.string.upload_not_yet_sent, R.color.gridItem, true),
    UNKNOWN(R.string.upload_state_unknown, R.color.gridItem, true),
    REVIEW(R.string.upload_state_in_review, R.color.gridItemReview, true),
    CONFLICT(R.string.upload_state_conflict, R.color.gridItemError, true),
    ACCEPTED(R.string.upload_state_accepted, R.color.gridItemGood, false),
    REJECTED(R.string.upload_state_rejected, R.color.gridItemError, false);

    private final int textId;

    private final int colorId;

    private final boolean pending;

    UploadState(int textId, int colorId, boolean pending) {
        this.textId = textId;
        this.colorId = colorId;
        this.pending = pending;
    }

    public int getTextId() {
        return textId;
    }

    public int getColorId() {
        return colorId;
    }

    public boolean isPending() {
        return pending;
    }

    static class Serializer implements JsonDeserializer<UploadState> {
        @Override
        public UploadState deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            try {
                return UploadState.valueOf(json.getAsString());
            } catch (Exception e) {
                return UNKNOWN;
            }
        }
    }
}
