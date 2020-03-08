package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.annotations.JsonAdapter;

import java.lang.reflect.Type;

import de.bahnhoefe.deutschlands.bahnhofsfotos.R;

public class InboxResponse {

    private InboxResponseState state;
    private String message;
    private long id;
    private String filename;
    private String inboxUrl;

    public InboxResponseState getState() {
        return state;
    }

    public void setState(final InboxResponseState state) {
        this.state = state;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public long getId() {
        return id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(final String filename) {
        this.filename = filename;
    }

    public String getInboxUrl() {
        return inboxUrl;
    }

    public void setInboxUrl(final String inboxUrl) {
        this.inboxUrl = inboxUrl;
    }

    @JsonAdapter(InboxResponseState.Serializer.class)
    public enum InboxResponseState {
        REVIEW(R.string.upload_completed, UploadState.REVIEW),
        LAT_LON_OUT_OF_RANGE(R.string.upload_lat_lon_out_of_range, UploadState.UNKNOWN),
        NOT_ENOUGH_DATA(R.string.upload_not_enough_data, UploadState.UNKNOWN),
        UNSUPPORTED_CONTENT_TYPE(R.string.upload_unsupported_content_type, UploadState.UNKNOWN),
        PHOTO_TOO_LARGE(R.string.upload_too_big, UploadState.UNKNOWN),
        CONFLICT(R.string.upload_conflict, UploadState.CONFLICT),
        UNAUTHORIZED(R.string.upload_too_big, UploadState.UNKNOWN),
        ERROR(R.string.upload_failed, UploadState.UNKNOWN);

        private final int messageId;

        private final UploadState uploadState;

        InboxResponseState(final int messageId, final UploadState uploadState) {
            this.messageId = messageId;
            this.uploadState = uploadState;
        }

        public int getMessageId() {
            return messageId;
        }

        public UploadState getUploadState() {
            return uploadState;
        }

        static class Serializer implements JsonDeserializer<InboxResponseState> {

            @Override
            public InboxResponseState deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) {
                try {
                    return InboxResponseState.valueOf(json.getAsString());
                } catch (final Exception e) {
                    return ERROR;
                }
            }
        }
    }

}
