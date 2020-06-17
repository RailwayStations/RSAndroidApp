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

    public void setLink(final String link) {
        this.link = link;
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    public void setAnonymous(final boolean anonymous) {
        this.anonymous = anonymous;
    }

    public boolean isPhotoOwner() {
        return photoOwner;
    }

    public void setPhotoOwner(final boolean photoOwner) {
        this.photoOwner = photoOwner;
    }

    public License getLicense() {
        return license;
    }

    public void setLicense(final License license) {
        this.license = license;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(final String nickname) {
        this.nickname = nickname;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }
}
