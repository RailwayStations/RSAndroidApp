package de.bahnhoefe.deutschlands.bahnhofsfotos.model;

public class ChangePassword {

    private final String newPassword;

    public ChangePassword(final String newPassword) {
        this.newPassword = newPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }
}
