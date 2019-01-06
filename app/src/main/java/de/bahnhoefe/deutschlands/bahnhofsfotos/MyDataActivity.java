package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import java.net.MalformedURLException;
import java.net.URL;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import de.bahnhoefe.deutschlands.bahnhofsfotos.Dialogs.SimpleDialogs;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.License;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Profile;
import org.apache.commons.lang3.StringUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyDataActivity extends AppCompatActivity {

    private static final String TAG = MyDataActivity.class.getSimpleName();
    private static final int RC_SIGN_IN = 1;

    private EditText etNickname, etLink, etEmail, etUploadToken;
    private CheckBox cbLicenseCC0;
    private CheckBox cbAnonymous;
    private CheckBox cbPhotoOwner;
    private License license;
    private BaseApplication baseApplication;
    private RSAPI rsapi;
    private Profile profile;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mydata);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.title_activity_my_data);

        etNickname = findViewById(R.id.etNickname);
        etUploadToken = findViewById(R.id.etUploadToken);
        etEmail = findViewById(R.id.etEmail);
        etLink = findViewById(R.id.etLinking);
        cbLicenseCC0 = findViewById(R.id.cbLicenseCC0);
        cbPhotoOwner = findViewById(R.id.cbOwnPhoto);
        cbAnonymous = findViewById(R.id.cbAnonymous);

        baseApplication = (BaseApplication) getApplication();
        rsapi = baseApplication.getRSAPI();

        setProfileToUI(baseApplication.getProfile());

        // Create GoogleSignIn
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.rsapi_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        SignInButton signInButton = findViewById(R.id.sign_in_button);
        signInButton.setSize(SignInButton.SIZE_STANDARD);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_SIGN_IN);
            }
        });

        receiveUploadToken(getIntent());
        loadRemoteProfile();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful
                GoogleSignInAccount account = task.getResult(ApiException.class);
                String idToken = account.getIdToken();
                Log.e(TAG, "idToken=" + idToken);
                rsapi.registrationWithGoogleIdToken(idToken, createProfileFromUI()).enqueue(new Callback<Profile>() {
                    @Override
                    public void onResponse(Call<Profile> call, Response<Profile> response) {
                        switch (response.code()) {
                            case 202 :
                                final Profile profile = response.body();
                                if (StringUtils.isNotBlank(profile.getUploadToken())) {
                                    saveLocalProfile(profile);
                                    new SimpleDialogs().confirm(MyDataActivity.this, R.string.upload_token_received);
                                } else {
                                    new SimpleDialogs().confirm(MyDataActivity.this, R.string.upload_token_email);
                                }
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
                    public void onFailure(Call<Profile> call, Throwable t) {
                        Log.e(TAG, "Registration via Google sign in failed", t);
                        new SimpleDialogs().confirm(MyDataActivity.this,
                                String.format(getText(R.string.registration_failed).toString(), t));
                    }
                });
            } catch (ApiException e) {
                Log.w(TAG, "Google sign in failed", e);
                new SimpleDialogs().confirm(MyDataActivity.this,
                        String.format(getText(R.string.google_sign_in_failed).toString(), e));
            }
        }
    }

    private void setProfileToUI(Profile profile) {
        etNickname.setText(profile.getNickname());
        etUploadToken.setText(profile.getUploadToken());
        etEmail.setText(profile.getEmail());
        etLink.setText(profile.getLink());
        license = profile.getLicense();
        cbLicenseCC0.setChecked(license == License.CC0);
        cbPhotoOwner.setChecked(profile.isPhotoOwner());
        cbAnonymous.setChecked(profile.isAnonymous());
        onAnonymousChecked(null);

        this.profile = profile;
    }

    private void loadRemoteProfile() {
        if (isUploadTokenAvailable()) {
            final Profile profile = createProfileFromUI();
            rsapi.getProfile(profile.getEmail(), profile.getUploadToken()).enqueue(new Callback<Profile>() {
                @Override
                public void onResponse(Call<Profile> call, Response<Profile> response) {
                    switch (response.code()) {
                        case 200 :
                            Log.i(TAG, "Successfully loaded profile");
                            final Profile remoteProfile = response.body();
                            remoteProfile.setUploadToken(profile.getUploadToken());
                            saveLocalProfile(remoteProfile);
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
                    loadRemoteProfile();
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
        if (isUploadTokenAvailable()) {
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

    private boolean isUploadTokenAvailable() {
        return StringUtils.isNotBlank(etUploadToken.getText().toString()) && StringUtils.isNotBlank(etEmail.getText().toString());
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
}
