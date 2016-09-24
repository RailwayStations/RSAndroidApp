package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

import java.util.Date;

/**
 * Created by android_oma on 04.09.16.
 */

public class ChatMessage {
    private String id;
    private String text;
    private String name;
    private String photoUrl;

    public ChatMessage() {
    }

    public ChatMessage(String text, String name, String photoUrl) {
        this.text = text;
        this.name = name;
        this.photoUrl = photoUrl;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }
}
