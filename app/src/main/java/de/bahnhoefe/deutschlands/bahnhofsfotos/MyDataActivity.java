package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.net.MalformedURLException;
import java.net.URL;

import de.bahnhoefe.deutschlands.bahnhofsfotos.Dialogs.SimpleDialogs;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.License;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Profile;
import org.apache.commons.lang3.StringUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyDataActivity extends AppCompatActivity {

    private static final String TAG = MyDataActivity.class.getSimpleName();

    private EditText etNickname, etLink, etEmail, etUploadToken;
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
    private EditText etLoginUploadToken;
    private Button btProfileSave;
    private TextView tvUploadToken;
    private Button btLogout;

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
        etUploadToken = findViewById(R.id.etUploadToken);
        etLoginUploadToken = findViewById(R.id.etLoginUploadToken);
        etEmail = findViewById(R.id.etEmail);
        etEmailOrNickname = findViewById(R.id.etEmailOrNickname);
        etLink = findViewById(R.id.etLinking);
        cbLicenseCC0 = findViewById(R.id.cbLicenseCC0);
        cbPhotoOwner = findViewById(R.id.cbOwnPhoto);
        cbAnonymous = findViewById(R.id.cbAnonymous);

        btProfileSave = findViewById(R.id.btProfileSave);
        btLogout = findViewById(R.id.bt_logout);
        tvUploadToken = findViewById(R.id.tvUploadToken);

        baseApplication = (BaseApplication) getApplication();
        rsapi = baseApplication.getRSAPI();

        setProfileToUI(baseApplication.getProfile());

        receiveUploadToken(getIntent());
        loadRemoteProfile(profile.getEmail(), profile.getUploadToken());
    }

    private void setProfileToUI(Profile profile) {
        etNickname.setText(profile.getNickname());
        etUploadToken.setText(profile.getUploadToken());
        etLoginUploadToken.setText(profile.getUploadToken());
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

    private void loadRemoteProfile(String email, final String uploadToken) {
        if (isUploadTokenAvailable(email, uploadToken)) {
            loginForm.setVisibility(View.VISIBLE);
            profileForm.setVisibility(View.GONE);

            rsapi.getProfile(email, uploadToken).enqueue(new Callback<Profile>() {
                @Override
                public void onResponse(Call<Profile> call, Response<Profile> response) {
                    switch (response.code()) {
                        case 200 :
                            Log.i(TAG, "Successfully loaded profile");
                            final Profile remoteProfile = response.body();
                            remoteProfile.setUploadToken(uploadToken);
                            saveLocalProfile(remoteProfile);
                            loginForm.setVisibility(View.GONE);
                            profileForm.setVisibility(View.VISIBLE);
                            getSupportActionBar().setTitle(R.string.tvProfile);
                            btProfileSave.setText(R.string.bt_mydata_commit);
                            btLogout.setVisibility(View.VISIBLE);
                            etUploadToken.setVisibility(View.VISIBLE);
                            tvUploadToken.setVisibility(View.VISIBLE);
                            break;
                        case 401 :
                            new SimpleDialogs().confirm(MyDataActivity.this, R.string.upload_token_invalid);
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

    private void receiveUploadToken(Intent intent) {
        if (intent != null) {
            if (Intent.ACTION_VIEW.equals(intent.getAction())) {
                Uri data = intent.getData();
                if (data != null) {
                    profile.setUploadToken(data.getLastPathSegment());
                    etUploadToken.setText(profile.getUploadToken());
                    baseApplication.setUploadToken(profile.getUploadToken());
                    loadRemoteProfile(profile.getEmail(), profile.getUploadToken());
                }
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        receiveUploadToken(intent);
    }

    public void selectLicense(View view) {
        license = cbLicenseCC0.isChecked() ? License.CC0 : License.UNKNOWN;
        if (license != License.CC0) {
            new SimpleDialogs().confirm(this, R.string.cc0_needed);
        }
    }

    public void register(View view) {
        if (btProfileSave.getText().equals(getResources().getText(R.string.bt_mydata_commit))) {
            saveSettings(view);
            return;
        }
        if (!isValid()) {
            return;
        }
        saveSettings(view);
        rsapi.registration(createProfileFromUI()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                switch (response.code()) {
                    case 202 :
                        new SimpleDialogs().confirm(MyDataActivity.this, R.string.upload_token_email);
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
        profile.setUploadToken(etUploadToken.getText().toString().trim());
        return profile;
    }

    public void saveSettings(View view) {
        if (isUploadTokenAvailable(etEmail.getText().toString(), etUploadToken.getText().toString())) {
            if (!isValid()) {
                return;
            }
            // TODO: email must be the old one if changed
            rsapi.saveProfile(etEmail.getText().toString(), etUploadToken.getText().toString(), createProfileFromUI()).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    switch (response.code()) {
                        case 200 :
                            Log.i(TAG, "Successfully saved profile");
                            break;
                        case 202 :
                            new SimpleDialogs().confirm(MyDataActivity.this, R.string.upload_token_email);
                            break;
                        case 400 :
                            new SimpleDialogs().confirm(MyDataActivity.this, R.string.profile_wrong_data);
                            break;
                        case 401 :
                            new SimpleDialogs().confirm(MyDataActivity.this, R.string.upload_token_invalid);
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

        saveLocalProfile(createProfileFromUI());
        Toast.makeText(this, R.string.preferences_saved, Toast.LENGTH_LONG).show();
    }

    private void saveLocalProfile(Profile profile) {
        baseApplication.setProfile(profile);
        setProfileToUI(profile);
    }

    private boolean isUploadTokenAvailable(String email, String uploadToken) {
        return StringUtils.isNotBlank(uploadToken) && StringUtils.isNotBlank(email);
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
        etUploadToken.setVisibility(View.GONE);
        tvUploadToken.setVisibility(View.GONE);
        getSupportActionBar().setTitle(R.string.tvRegistration);
        btProfileSave.setText(R.string.bt_register);
        btLogout.setVisibility(View.GONE);
    }

    public void login(View view) {
        String email = etEmailOrNickname.getText().toString();
        String uploadToken = etLoginUploadToken.getText().toString();
        if (isUploadTokenAvailable(email, uploadToken)) {
            baseApplication.setEmail(email);
            baseApplication.setUploadToken(uploadToken);
            loadRemoteProfile(email, uploadToken);
        } else {
            new SimpleDialogs().confirm(this, R.string.missing_login_data);
        }
    }

    public void logout(View view) {
        profile.setNickname(null);;
        profile.setEmail(null);
        profile.setUploadToken(null);
        saveLocalProfile(profile);
        profileForm.setVisibility(View.GONE);
        loginForm.setVisibility(View.VISIBLE);
        getSupportActionBar().setTitle(R.string.login);
    }

    public void newUploadToken(View view) {
        String emailOrNickname = etEmailOrNickname.getText().toString();
        if (StringUtils.isBlank(emailOrNickname)) {
            new SimpleDialogs().confirm(this, R.string.missing_email_or_nickname);
            return;
        }
        profile.setEmail(emailOrNickname);
        saveLocalProfile(profile);
        rsapi.newUploadToken(emailOrNickname).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                switch (response.code()) {
                    case 202 :
                        new SimpleDialogs().confirm(MyDataActivity.this, R.string.upload_token_email);
                        break;
                    case 400 :
                        new SimpleDialogs().confirm(MyDataActivity.this, R.string.profile_wrong_data);
                        break;
                    case 404 :
                        new SimpleDialogs().confirm(MyDataActivity.this, R.string.profile_not_found);
                        break;
                    default :
                        new SimpleDialogs().confirm(MyDataActivity.this,
                                String.format(getText(R.string.request_uploadtoken_failed).toString(), response.code()));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Request new UploadToken failed", t);
                new SimpleDialogs().confirm(MyDataActivity.this,
                        String.format(getText(R.string.request_uploadtoken_failed).toString(), t));
            }
        });
    }
}
