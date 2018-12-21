package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

import org.json.JSONException;
import org.json.JSONObject;

public class Profile {
    private String nickname;
    private String email;
    private License license;
    private boolean photoOwner;
    private boolean anonymous;
    private String link;
    private String uploadToken;

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    public void setAnonymous(boolean anonymous) {
        this.anonymous = anonymous;
    }

    public boolean isPhotoOwner() {
        return photoOwner;
    }

    public void setPhotoOwner(boolean photoOwner) {
        this.photoOwner = photoOwner;
    }

    public License getLicense() {
        return license;
    }

    public void setLicense(License license) {
        this.license = license;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("nickname", nickname);
            json.put("email", email);
            json.put("license", license);
            json.put("photoOwner", photoOwner);
            json.put("anonymous", anonymous);
            json.put("link", link);
        } catch (JSONException e) {
            throw new RuntimeException("Error creating Json from Profile", e);
        }
        return json;
    }

    public String getUploadToken() {
        return uploadToken;
    }

    public void setUploadToken(String uploadToken) {
        this.uploadToken = uploadToken;
    }
}
