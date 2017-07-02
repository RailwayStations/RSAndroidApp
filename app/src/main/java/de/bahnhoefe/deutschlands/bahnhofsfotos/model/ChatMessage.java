package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ChatMessage {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy @ HH:mm");

    private String id;
    private String text;
    private String name;
    private String photoUrl;
    private String chatTimeStamp;

    public ChatMessage() {
    }

    public ChatMessage(String text, String name, String photoUrl, String chatTimeStamp) {
        this.text = text;
        this.name = name;
        this.photoUrl = photoUrl;
        this.chatTimeStamp = chatTimeStamp;
    }

    public ChatMessage(String text, String name, String photoUrl) {
        this(text, name, photoUrl, createChatTimeStamp());
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

    public String getChatTimeStamp() {
        return chatTimeStamp;
    }

    private static String createChatTimeStamp() {
        return DATE_FORMAT.format(new Date());
    }

}
