package de.bahnhoefe.deutschlands.bahnhofsfotos.util;

import android.app.backup.BackupAgentHelper;
import android.app.backup.SharedPreferencesBackupHelper;

import de.bahnhoefe.deutschlands.bahnhofsfotos.BaseApplication;

public class BahnhofsfotosBackupAgent extends BackupAgentHelper {

    private static final String PREFS_BACKUP_KEY = "prefs";

    @Override
    public void onCreate() {
        getApplicationContext();
        final SharedPreferencesBackupHelper helper = new SharedPreferencesBackupHelper(this, BaseApplication.PREF_FILE);
        addHelper(PREFS_BACKUP_KEY, helper);
    }
}
