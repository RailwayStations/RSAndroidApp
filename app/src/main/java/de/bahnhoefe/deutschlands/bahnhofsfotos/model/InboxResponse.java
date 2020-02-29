package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.annotations.JsonAdapter;

import java.lang.reflect.Type;

import de.bahnhoefe.deutschlands.bahnhofsfotos.R;

public class InboxResponse {

    private InboxResponseState state;
    private String message;
    private int id;
    private String filename;
    private String inboxUrl;

    public InboxResponseState getState() {
        return state;
    }

    public void setState(InboxResponseState state) {
        this.state = state;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getInboxUrl() {
        return inboxUrl;
    }

    public void setInboxUrl(String inboxUrl) {
        this.inboxUrl = inboxUrl;
    }

    @JsonAdapter(InboxResponseState.Serializer.class)
    public enum InboxResponseState {
        REVIEW(R.string.upload_completed),
        LAT_LON_OUT_OF_RANGE(R.string.upload_bad_request), // TODO: better messages
        NOT_ENOUGH_DATA(R.string.upload_bad_request),
        UNSUPPORTED_CONTENT_TYPE(R.string.upload_bad_request),
        PHOTO_TOO_LARGE(R.string.upload_too_big),
        CONFLICT(R.string.upload_conflict),
        UNAUTHORIZED(R.string.upload_too_big),
        ERROR(R.string.upload_failed);

        private final int messageId;

        InboxResponseState(int messageId) {
            this.messageId = messageId;
        }

        public int getMessageId() {
            return messageId;
        }

        static class Serializer implements JsonSerializer<InboxResponseState>, JsonDeserializer<InboxResponseState> {
            @Override
            public JsonElement serialize(InboxResponseState src, Type typeOfSrc, JsonSerializationContext context) {
                return context.serialize(src);
            }

            @Override
            public InboxResponseState deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
                try {
                    return InboxResponseState.valueOf(json.getAsString());
                } catch (Exception e) {
                    return ERROR;
                }
            }
        }
    }

}
