package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

public class Profile {
    private String nickname;
    private String email;
    private License license;
    private boolean photoOwner;
    private boolean anonymous;
    private String link;

    private transient String password;

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

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }
}
