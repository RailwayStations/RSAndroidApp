package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.apache.commons.lang3.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.ActivityMydataBinding;
import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.ChangePasswordBinding;
import de.bahnhoefe.deutschlands.bahnhofsfotos.dialogs.SimpleDialogs;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.License;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Profile;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Token;
import de.bahnhoefe.deutschlands.bahnhofsfotos.rsapi.RSAPIClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyDataActivity extends AppCompatActivity {

    private static final String TAG = MyDataActivity.class.getSimpleName();

    private License license;
    private BaseApplication baseApplication;
    private RSAPIClient rsapiClient;
    private Profile profile;
    private ActivityMydataBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMydataBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.login);

        binding.myData.profileForm.setVisibility(View.INVISIBLE);

        baseApplication = (BaseApplication) getApplication();
        rsapiClient = baseApplication.getRsapiClient();

        setProfileToUI(baseApplication.getProfile());

        oauthAuthorizationCallback(getIntent());
        if (isLoginDataAvailable()) {
            loadRemoteProfile();
        }
    }

    private void setProfileToUI(Profile profile) {
        binding.myData.etNickname.setText(profile.getNickname());
        binding.myData.etEmail.setText(profile.getEmail());
        binding.myData.etLinking.setText(profile.getLink());
        license = profile.getLicense();
        binding.myData.cbLicenseCC0.setChecked(license == License.CC0);
        binding.myData.cbOwnPhoto.setChecked(profile.isPhotoOwner());
        binding.myData.cbAnonymous.setChecked(profile.isAnonymous());
        onAnonymousChecked(null);

        if (profile.isEmailVerified()) {
            binding.myData.tvEmailVerification.setText(R.string.emailVerified);
            binding.myData.tvEmailVerification.setTextColor(getResources().getColor(R.color.emailVerified, null));
        } else {
            binding.myData.tvEmailVerification.setText(R.string.emailUnverified);
            binding.myData.tvEmailVerification.setTextColor(getResources().getColor(R.color.emailUnverified, null));
        }
        this.profile = profile;
    }

    private void loadRemoteProfile() {
        binding.myData.loginForm.setVisibility(View.VISIBLE);
        binding.myData.profileForm.setVisibility(View.GONE);

        rsapiClient.getProfile().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Profile> call, @NonNull Response<Profile> response) {
                switch (response.code()) {
                    case 200:
                        Log.i(TAG, "Successfully loaded profile");
                        var remoteProfile = response.body();
                        if (remoteProfile != null) {
                            //TODO: remoteProfile.setPassword(binding.myData.etPassword.getText().toString());
                            saveLocalProfile(remoteProfile);
                            showProfileView();
                        }
                        break;
                    case 401:
                        logout(null);
                        SimpleDialogs.confirm(MyDataActivity.this, R.string.authorization_failed);
                        break;
                    default:
                        SimpleDialogs.confirm(MyDataActivity.this,
                                String.format(getText(R.string.read_profile_failed).toString(), response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<Profile> call, @NonNull Throwable t) {
                SimpleDialogs.confirm(MyDataActivity.this,
                        String.format(getText(R.string.read_profile_failed).toString(), t.getMessage()));
            }
        });
    }

    private void showProfileView() {
        binding.myData.loginForm.setVisibility(View.GONE);
        binding.myData.profileForm.setVisibility(View.VISIBLE);
        Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.tvProfile);
        binding.myData.btProfileSave.setText(R.string.bt_mydata_commit);
        binding.myData.btLogout.setVisibility(View.VISIBLE);
        binding.myData.btChangePassword.setVisibility(View.VISIBLE);
        binding.myData.initPasswordLayout.setVisibility(View.GONE);
    }

    private void oauthAuthorizationCallback(Intent intent) {
        if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction())) {
            var data = intent.getData();
            if (data != null) {
                var redirectUri = baseApplication.getRsapiRedirectUri();
                if (data.toString().startsWith(redirectUri)) {
                    var code = data.getQueryParameter("code");
                    if (code != null) {
                        var clientId = getString(R.string.rsapiClientId);
                        baseApplication.getRsapiClient().requestAccessToken(code, clientId, redirectUri, baseApplication.getPkceCodeVerifier()).enqueue(new Callback<>() {
                            @Override
                            public void onResponse(Call<Token> call, Response<Token> response) {
                                var token = response.body();
                                Log.d(TAG, String.valueOf(token));
                                baseApplication.setAccessToken(token.getAccessToken());
                                baseApplication.getRsapiClient().setToken(token);
                                loadRemoteProfile();
                            }

                            @Override
                            public void onFailure(final Call<Token> call, final Throwable t) {
                                Toast.makeText(MyDataActivity.this, getString(R.string.authorization_error, t.getMessage()), Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        String error = data.getQueryParameter("error");
                        if (error != null) {
                            Toast.makeText(this, getString(R.string.authorization_error, error), Toast.LENGTH_LONG).show();
                        }
                    }

                    if (isLoginDataAvailable()) {
                        loadRemoteProfile();
                    }
                }
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        oauthAuthorizationCallback(intent);
    }

    public void selectLicense(View view) {
        license = binding.myData.cbLicenseCC0.isChecked() ? License.CC0 : License.UNKNOWN;
        if (license != License.CC0) {
            SimpleDialogs.confirm(this, R.string.cc0_needed);
        }
    }

    public void save(View view) {
        profile = createProfileFromUI();
        if (!isValid()) {
            return;
        }
        if (rsapiClient.hasToken()) {
            rsapiClient.saveProfile(profile).enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                    switch (response.code()) {
                        case 200:
                            Log.i(TAG, "Successfully saved profile");
                            break;
                        case 202:
                            SimpleDialogs.confirm(MyDataActivity.this, R.string.password_email);
                            break;
                        case 400:
                            SimpleDialogs.confirm(MyDataActivity.this, R.string.profile_wrong_data);
                            break;
                        case 401:
                            logout(view);
                            SimpleDialogs.confirm(MyDataActivity.this, R.string.authorization_failed);
                            break;
                        case 409:
                            SimpleDialogs.confirm(MyDataActivity.this, R.string.profile_conflict);
                            break;
                        default:
                            SimpleDialogs.confirm(MyDataActivity.this,
                                    String.format(getText(R.string.save_profile_failed).toString(), response.code()));
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                    Log.e(TAG, "Error uploading profile", t);
                    SimpleDialogs.confirm(MyDataActivity.this,
                            String.format(getText(R.string.save_profile_failed).toString(), t.getMessage()));
                }
            });
        }

        saveLocalProfile(profile);
        Toast.makeText(this, R.string.preferences_saved, Toast.LENGTH_LONG).show();

    }

    private Profile createProfileFromUI() {
        var profileBuilder = Profile.builder()
                .nickname(binding.myData.etNickname.getText().toString().trim())
                .email(binding.myData.etEmail.getText().toString().trim())
                .license(license)
                .photoOwner(binding.myData.cbOwnPhoto.isChecked())
                .anonymous(binding.myData.cbAnonymous.isChecked())
                .link(binding.myData.etLinking.getText().toString().trim());

        if (this.profile != null) {
            profileBuilder.emailVerified(this.profile.isEmailVerified());
        }

        return profileBuilder.build();
    }

    private void saveLocalProfile(Profile profile) {
        baseApplication.setProfile(profile);
        setProfileToUI(profile);
    }

    private boolean isLoginDataAvailable() {
        return baseApplication.getAccessToken() != null;
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    public boolean isValid() {
        if (license == License.UNKNOWN) {
            SimpleDialogs.confirm(this, R.string.cc0_needed);
            return false;
        }
        if (!binding.myData.cbOwnPhoto.isChecked()) {
            SimpleDialogs.confirm(this, R.string.missing_photoOwner);
            return false;
        }
        if (StringUtils.isBlank(binding.myData.etNickname.getText())) {
            SimpleDialogs.confirm(this, R.string.missing_nickname);
            return false;
        }
        if (!isValidEmail(binding.myData.etEmail.getText())) {
            SimpleDialogs.confirm(this, R.string.missing_email_address);
            return false;
        }
        String url = binding.myData.etLinking.getText().toString();
        if (StringUtils.isNotBlank(url) && !isValidHTTPURL(url)) {
            SimpleDialogs.confirm(this, R.string.missing_link);
            return false;
        }

        return true;
    }

    private boolean isValidHTTPURL(String urlString) {
        try {
            var url = new URL(urlString);
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
        if (binding.myData.cbAnonymous.isChecked()) {
            binding.myData.etLinking.setVisibility(View.GONE);
            binding.myData.tvLinking.setVisibility(View.GONE);
        } else {
            binding.myData.etLinking.setVisibility(View.VISIBLE);
            binding.myData.tvLinking.setVisibility(View.VISIBLE);
        }
    }

    public void login(View view) throws NoSuchAlgorithmException {
        Intent intent = new Intent(
                Intent.ACTION_VIEW,
                Uri.parse(baseApplication.getApiUrl() + "oauth2/authorize" + "?client_id=" + baseApplication.getRsapiClientId() + "&code_challenge=" + baseApplication.getPkceCodeChallenge() + "&code_challenge_method=S256&scope=all&response_type=code&redirect_uri=" + baseApplication.getRsapiRedirectUri()));
        startActivity(intent);
    }

    public void logout(View view) {
        baseApplication.setAccessToken(null);
        rsapiClient.clearToken();
        profile = Profile.builder().build();
        saveLocalProfile(profile);
        binding.myData.profileForm.setVisibility(View.GONE);
        binding.myData.loginForm.setVisibility(View.VISIBLE);
        Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.login);
    }

    public void changePassword(View view) {
        var builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogCustom));
        var passwordBinding = ChangePasswordBinding.inflate(getLayoutInflater());

        builder.setTitle(R.string.bt_change_password)
                .setView(passwordBinding.getRoot())
                .setIcon(R.mipmap.ic_launcher)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, (dialog, id) -> dialog.cancel());

        var alertDialog = builder.create();
        alertDialog.show();

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String newPassword = getValidPassword(passwordBinding.password, passwordBinding.passwordRepeat);
            if (newPassword == null) {
                return;
            }
            alertDialog.dismiss();

            try {
                newPassword = URLEncoder.encode(newPassword, String.valueOf(StandardCharsets.UTF_8));
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "Error encoding new password", e);
            }

            rsapiClient.changePassword(newPassword).enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                    switch (response.code()) {
                        case 200:
                            Log.i(TAG, "Successfully changed password");
                            logout(view);
                            SimpleDialogs.confirm(MyDataActivity.this, R.string.password_changed);
                            break;
                        case 401:
                            SimpleDialogs.confirm(MyDataActivity.this, R.string.authorization_failed);
                            logout(view);
                            break;
                        default:
                            SimpleDialogs.confirm(MyDataActivity.this,
                                    String.format(getText(R.string.change_password_failed).toString(), response.code()));
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                    Log.e(TAG, "Error changing password", t);
                    SimpleDialogs.confirm(MyDataActivity.this,
                            String.format(getText(R.string.change_password_failed).toString(), t.getMessage()));
                }
            });
        });

    }

    private String getValidPassword(EditText etNewPassword, EditText etPasswordRepeat) {
        var newPassword = etNewPassword.getText().toString().trim();

        if (newPassword.length() < 8) {
            Toast.makeText(MyDataActivity.this, R.string.password_too_short, Toast.LENGTH_LONG).show();
            return null;
        }
        if (!newPassword.equals(etPasswordRepeat.getText().toString().trim())) {
            Toast.makeText(MyDataActivity.this, R.string.password_repeat_fail, Toast.LENGTH_LONG).show();
            return null;
        }
        return newPassword;
    }

    public void requestEmailVerification(View view) {
        SimpleDialogs.confirm(this, R.string.requestEmailVerification, (dialogInterface, i) -> rsapiClient.resendEmailVerification().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.code() == 200) {
                    Log.i(TAG, "Successfully requested email verification");
                    Toast.makeText(MyDataActivity.this, R.string.emailVerificationRequested, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MyDataActivity.this, R.string.emailVerificationRequestFailed, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Log.e(TAG, "Error requesting email verification", t);
                Toast.makeText(MyDataActivity.this, R.string.emailVerificationRequestFailed, Toast.LENGTH_LONG).show();
            }
        }));
    }

}
