package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import de.bahnhoefe.deutschlands.bahnhofsfotos.Dialogs.SimpleDialogs;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.License;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Profile;
import org.apache.commons.lang3.StringUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyDataActivity extends AppCompatActivity {

    private static final String TAG = MyDataActivity.class.getSimpleName();

    private EditText etNickname, etLink, etEmail;
    private CheckBox cbLicenseCC0;
    private CheckBox cbAnonymous;
    private CheckBox cbPhotoOwner;
    private License license;
    private BaseApplication baseApplication;
    private RSAPI rsapi;
    private Profile profile;
    private View loginForm;
    private View profileForm;
    private EditText etEmailOrNickname;
    private EditText etPassword;
    private Button btProfileSave;
    private Button btLogout;
    private String authorizationHeader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mydata);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.login);

        loginForm = findViewById(R.id.login_form);
        profileForm = findViewById(R.id.profile_form);
        profileForm.setVisibility(View.INVISIBLE);

        etNickname = findViewById(R.id.etNickname);
        etPassword = findViewById(R.id.etPassword);
        etEmail = findViewById(R.id.etEmail);
        etEmailOrNickname = findViewById(R.id.etEmailOrNickname);
        etLink = findViewById(R.id.etLinking);
        cbLicenseCC0 = findViewById(R.id.cbLicenseCC0);
        cbPhotoOwner = findViewById(R.id.cbOwnPhoto);
        cbAnonymous = findViewById(R.id.cbAnonymous);

        btProfileSave = findViewById(R.id.btProfileSave);
        btLogout = findViewById(R.id.bt_logout);

        baseApplication = (BaseApplication) getApplication();
        rsapi = baseApplication.getRSAPI();

        setProfileToUI(baseApplication.getProfile());

        receiveInitialPassword(getIntent());
        if (isLoginDataAvailable(profile.getEmail(), profile.getPassword())) {
            authorizationHeader = RSAPI.Helper.getAuthorizationHeader(profile.getEmail(), profile.getPassword());
            loadRemoteProfile();
        }
    }

    private void setProfileToUI(Profile profile) {
        etNickname.setText(profile.getNickname());
        etPassword.setText(profile.getPassword());
        etEmail.setText(profile.getEmail());
        etEmailOrNickname.setText(profile.getEmail());
        etLink.setText(profile.getLink());
        license = profile.getLicense();
        cbLicenseCC0.setChecked(license == License.CC0);
        cbPhotoOwner.setChecked(profile.isPhotoOwner());
        cbAnonymous.setChecked(profile.isAnonymous());
        onAnonymousChecked(null);

        this.profile = profile;
    }

    private void loadRemoteProfile() {
        if (authorizationHeader != null) {
            loginForm.setVisibility(View.VISIBLE);
            profileForm.setVisibility(View.GONE);

            rsapi.getProfile(authorizationHeader).enqueue(new Callback<Profile>() {
                @Override
                public void onResponse(Call<Profile> call, Response<Profile> response) {
                    switch (response.code()) {
                        case 200 :
                            Log.i(TAG, "Successfully loaded profile");
                            final Profile remoteProfile = response.body();
                            remoteProfile.setPassword(etPassword.getText().toString());
                            saveLocalProfile(remoteProfile);
                            loginForm.setVisibility(View.GONE);
                            profileForm.setVisibility(View.VISIBLE);
                            getSupportActionBar().setTitle(R.string.tvProfile);
                            btProfileSave.setText(R.string.bt_mydata_commit);
                            btLogout.setVisibility(View.VISIBLE);
                            break;
                        case 401 :
                            authorizationHeader = null;
                            new SimpleDialogs().confirm(MyDataActivity.this, R.string.authorization_failed);
                            break;
                        default :
                            new SimpleDialogs().confirm(MyDataActivity.this,
                                    String.format(getText(R.string.read_profile_failed).toString(), response.code()));
                    }
                }

                @Override
                public void onFailure(Call<Profile> call, Throwable t) {
                    new SimpleDialogs().confirm(MyDataActivity.this,
                            String.format(getText(R.string.read_profile_failed).toString(), t.getMessage()));
                }
            });
        }
    }

    private void receiveInitialPassword(Intent intent) {
        if (intent != null) {
            if (Intent.ACTION_VIEW.equals(intent.getAction())) {
                Uri data = intent.getData();
                if (data != null) {
                    profile.setPassword(data.getLastPathSegment());
                    etPassword.setText(profile.getPassword());
                    baseApplication.setPassword(profile.getPassword());
                    if (isLoginDataAvailable(profile.getEmail(), profile.getPassword())) {
                        authorizationHeader = RSAPI.Helper.getAuthorizationHeader(profile.getEmail(), profile.getPassword());
                        loadRemoteProfile();
                    }
                }
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        receiveInitialPassword(intent);
    }

    public void selectLicense(View view) {
        license = cbLicenseCC0.isChecked() ? License.CC0 : License.UNKNOWN;
        if (license != License.CC0) {
            new SimpleDialogs().confirm(this, R.string.cc0_needed);
        }
    }

    public void register(View view) {
        if (btProfileSave.getText().equals(getResources().getText(R.string.bt_mydata_commit))) {
            saveProfile(view);
            return;
        }
        if (!isValid()) {
            return;
        }
        saveProfile(view);
        rsapi.registration(createProfileFromUI()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                switch (response.code()) {
                    case 202 :
                        new SimpleDialogs().confirm(MyDataActivity.this, R.string.password_email);
                        break;
                    case 400 :
                        new SimpleDialogs().confirm(MyDataActivity.this, R.string.profile_wrong_data);
                        break;
                    case 409 :
                        new SimpleDialogs().confirm(MyDataActivity.this, R.string.profile_conflict);
                        break;
                    case 422 :
                        new SimpleDialogs().confirm(MyDataActivity.this, R.string.registration_data_incomplete);
                        break;
                    default :
                        new SimpleDialogs().confirm(MyDataActivity.this,
                            String.format(getText(R.string.registration_failed).toString(), response.code()));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Registration failed", t);
                new SimpleDialogs().confirm(MyDataActivity.this,
                        String.format(getText(R.string.registration_failed).toString(), t));
            }
        });
    }

    private Profile createProfileFromUI() {
        Profile profile = new Profile();
        profile.setNickname(etNickname.getText().toString().trim());
        profile.setEmail(etEmail.getText().toString().trim());
        profile.setLicense(license);
        profile.setPhotoOwner(cbPhotoOwner.isChecked());
        profile.setAnonymous(cbAnonymous.isChecked());
        profile.setLink(etLink.getText().toString().trim());
        profile.setPassword(etPassword.getText().toString().trim());
        return profile;
    }

    public void saveProfile(View view) {
        if (authorizationHeader != null) {
            if (!isValid()) {
                return;
            }
            profile = createProfileFromUI();
            rsapi.saveProfile(authorizationHeader, profile).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    switch (response.code()) {
                        case 200 :
                            // create new authorization header in case email changed
                            authorizationHeader = RSAPI.Helper.getAuthorizationHeader(profile.getEmail(), profile.getPassword());
                            Log.i(TAG, "Successfully saved profile");
                            break;
                        case 202 :
                            new SimpleDialogs().confirm(MyDataActivity.this, R.string.password_email);
                            break;
                        case 400 :
                            new SimpleDialogs().confirm(MyDataActivity.this, R.string.profile_wrong_data);
                            break;
                        case 401 :
                            authorizationHeader = null;
                            new SimpleDialogs().confirm(MyDataActivity.this, R.string.authorization_failed);
                            break;
                        case 409 :
                            new SimpleDialogs().confirm(MyDataActivity.this, R.string.profile_conflict);
                            break;
                        default :
                            new SimpleDialogs().confirm(MyDataActivity.this,
                                    String.format(getText(R.string.save_profile_failed).toString(), response.code()));
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Log.e(TAG, "Error uploading profile", t);
                    new SimpleDialogs().confirm(MyDataActivity.this,
                            String.format(getText(R.string.save_profile_failed).toString(), t.getMessage()));
                }
            });
        }

        saveLocalProfile(profile);
        Toast.makeText(this, R.string.preferences_saved, Toast.LENGTH_LONG).show();
    }

    private void saveLocalProfile(Profile profile) {
        baseApplication.setProfile(profile);
        setProfileToUI(profile);
    }

    private boolean isLoginDataAvailable(String email, String password) {
        return StringUtils.isNotBlank(password) && StringUtils.isNotBlank(email);
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    public boolean isValid() {
        if (license == License.UNKNOWN) {
            new SimpleDialogs().confirm(this, R.string.cc0_needed);
            return false;
        }
        if (!cbPhotoOwner.isChecked()) {
            new SimpleDialogs().confirm(this, R.string.missing_photoOwner);
            return false;
        }
        if (StringUtils.isBlank(etNickname.getText())) {
            new SimpleDialogs().confirm(this, R.string.missing_nickname);
            return false;
        }
        if (!isValidEmail(etEmail.getText())) {
            new SimpleDialogs().confirm(this, R.string.missing_email_address);
            return false;
        }
        String url = etLink.getText().toString();
        if (StringUtils.isNotBlank(url)&& !isValidHTTPURL(url)) {
            new SimpleDialogs().confirm(this, R.string.missing_link);
            return false;
        }

        return true;
    }

    private boolean isValidHTTPURL(String urlString) {
        try {
            URL url = new URL(urlString);
            if (!"http".equals(url.getProtocol()) && !"https".equals(url.getProtocol())) {
                return false;
            }
        } catch (MalformedURLException e) {
            return false;
        }
        return true;
    }

    public boolean isValidEmail(CharSequence target) {
        return target != null && android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();

    }

    public void onAnonymousChecked(View view) {
        if (cbAnonymous.isChecked()) {
            etLink.setVisibility(View.GONE);
            findViewById(R.id.tvLinking).setVisibility(View.GONE);
        } else {
            etLink.setVisibility(View.VISIBLE);
            findViewById(R.id.tvLinking).setVisibility(View.VISIBLE);
        }
    }

    public void newRegister(View view) {
        etEmail.setText(etEmailOrNickname.getText());
        profileForm.setVisibility(View.VISIBLE);
        loginForm.setVisibility(View.GONE);
        getSupportActionBar().setTitle(R.string.tvRegistration);
        btProfileSave.setText(R.string.bt_register);
        btLogout.setVisibility(View.GONE);
    }

    public void login(View view) {
        String email = etEmailOrNickname.getText().toString();
        String password = etPassword.getText().toString();
        if (isLoginDataAvailable(email, password)) {
            baseApplication.setEmail(email);
            baseApplication.setPassword(password);
            authorizationHeader = RSAPI.Helper.getAuthorizationHeader(email, password);
            loadRemoteProfile();
        } else {
            new SimpleDialogs().confirm(this, R.string.missing_login_data);
        }
    }

    public void logout(View view) {
        profile.setNickname(null);;
        profile.setEmail(null);
        profile.setPassword(null);
        saveLocalProfile(profile);
        profileForm.setVisibility(View.GONE);
        loginForm.setVisibility(View.VISIBLE);
        getSupportActionBar().setTitle(R.string.login);
    }

    public void resetPassword(View view) {
        String emailOrNickname = etEmailOrNickname.getText().toString();
        if (StringUtils.isBlank(emailOrNickname)) {
            new SimpleDialogs().confirm(this, R.string.missing_email_or_nickname);
            return;
        }
        profile.setEmail(emailOrNickname);
        saveLocalProfile(profile);
        rsapi.resetPassword(emailOrNickname).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                switch (response.code()) {
                    case 202 :
                        new SimpleDialogs().confirm(MyDataActivity.this, R.string.password_email);
                        break;
                    case 400 :
                        new SimpleDialogs().confirm(MyDataActivity.this, R.string.profile_wrong_data);
                        break;
                    case 404 :
                        new SimpleDialogs().confirm(MyDataActivity.this, R.string.profile_not_found);
                        break;
                    default :
                        new SimpleDialogs().confirm(MyDataActivity.this,
                                String.format(getText(R.string.request_password_failed).toString(), response.code()));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Request new password failed", t);
                new SimpleDialogs().confirm(MyDataActivity.this,
                        String.format(getText(R.string.request_password_failed).toString(), t));
            }
        });
    }

    public void changePassword(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogCustom));
        // Get the layout inflater
        LayoutInflater inflater = this.getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        View dialogView = inflater.inflate(R.layout.change_password, null);
        final EditText etNewPassword = dialogView.findViewById(R.id.password);
        final EditText etPasswordRepeat = dialogView.findViewById(R.id.passwordRepeat);

        builder.setTitle(R.string.bt_change_password)
               .setView(dialogView)
               .setIcon(R.mipmap.ic_launcher)
               // Add action buttons
               .setPositiveButton(android.R.string.ok, null)
               .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
               });

        final AlertDialog alertDialog = builder.create();
        alertDialog.show();

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newPassword = etNewPassword.getText().toString();

                if (newPassword.length() < 8) {
                    Toast.makeText(MyDataActivity.this, R.string.password_too_short, Toast.LENGTH_LONG).show();
                    return;
                }
                if (!newPassword.equals(etPasswordRepeat.getText().toString())) {
                    Toast.makeText(MyDataActivity.this, R.string.password_repeat_fail, Toast.LENGTH_LONG).show();
                    return;
                }

                alertDialog.dismiss();

                try {
                    newPassword = URLEncoder.encode(etNewPassword.getText().toString(), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    Log.e(TAG, "Error encoding new password", e);
                }

                rsapi.changePassword(authorizationHeader, newPassword).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        switch (response.code()) {
                            case 200:
                                Log.i(TAG, "Successfully changed password");
                                etPassword.setText(etNewPassword.getText());
                                baseApplication.setPassword(etNewPassword.getText().toString());
                                authorizationHeader = RSAPI.Helper.getAuthorizationHeader(profile.getEmail(), etNewPassword.getText().toString());
                                new SimpleDialogs().confirm(MyDataActivity.this, R.string.password_changed);
                                break;
                            case 401:
                                authorizationHeader = null;
                                new SimpleDialogs().confirm(MyDataActivity.this, R.string.authorization_failed);
                                break;
                            default:
                                new SimpleDialogs().confirm(MyDataActivity.this,
                                        String.format(getText(R.string.change_password_failed).toString(), response.code()));
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Log.e(TAG, "Error changing password", t);
                        new SimpleDialogs().confirm(MyDataActivity.this,
                                String.format(getText(R.string.change_password_failed).toString(), t.getMessage()));
                    }
                });
            }
        });

    }
}
