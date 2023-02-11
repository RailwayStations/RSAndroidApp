package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.content.Intent;
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
        binding.myData.cbOwnPhoto.setChecked(profile.getPhotoOwner());
        binding.myData.cbAnonymous.setChecked(profile.getAnonymous());
        onAnonymousChecked(null);

        if (profile.getEmailVerified()) {
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
        binding.myData.progressBar.setVisibility(View.VISIBLE);

        rsapiClient.getProfile().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Profile> call, @NonNull Response<Profile> response) {
                binding.myData.progressBar.setVisibility(View.GONE);

                switch (response.code()) {
                    case 200:
                        Log.i(TAG, "Successfully loaded profile");
                        var remoteProfile = response.body();
                        if (remoteProfile != null) {
                            saveLocalProfile(remoteProfile);
                            showProfileView();
                        }
                        break;
                    case 401:
                        logout(null);
                        SimpleDialogs.confirmOk(MyDataActivity.this, R.string.authorization_failed);
                        break;
                    default:
                        SimpleDialogs.confirmOk(MyDataActivity.this,
                                String.format(getText(R.string.read_profile_failed).toString(), response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<Profile> call, @NonNull Throwable t) {
                binding.myData.progressBar.setVisibility(View.GONE);
                SimpleDialogs.confirmOk(MyDataActivity.this,
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
    }

    private void oauthAuthorizationCallback(Intent intent) {
        if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction())) {
            var data = intent.getData();
            if (data != null) {
                if (data.toString().startsWith(baseApplication.getRsapiClient().getRedirectUri())) {
                    var code = data.getQueryParameter("code");
                    if (code != null) {
                        baseApplication.getRsapiClient().requestAccessToken(code).enqueue(new Callback<>() {
                            @Override
                            public void onResponse(@NonNull Call<Token> call, @NonNull Response<Token> response) {
                                var token = response.body();
                                Log.d(TAG, String.valueOf(token));
                                baseApplication.setAccessToken(token.getAccessToken());
                                baseApplication.getRsapiClient().setToken(token);
                                loadRemoteProfile();
                            }

                            @Override
                            public void onFailure(@NonNull Call<Token> call, @NonNull Throwable t) {
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
            SimpleDialogs.confirmOk(this, R.string.cc0_needed);
        }
    }

    public void save(View view) {
        profile = createProfileFromUI();
        if (!isValid(profile)) {
            return;
        }
        if (rsapiClient.hasToken()) {
            binding.myData.progressBar.setVisibility(View.VISIBLE);

            rsapiClient.saveProfile(profile).enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                    binding.myData.progressBar.setVisibility(View.GONE);

                    switch (response.code()) {
                        case 200:
                            Log.i(TAG, "Successfully saved profile");
                            break;
                        case 202:
                            SimpleDialogs.confirmOk(MyDataActivity.this, R.string.password_email);
                            break;
                        case 400:
                            SimpleDialogs.confirmOk(MyDataActivity.this, R.string.profile_wrong_data);
                            break;
                        case 401:
                            logout(view);
                            SimpleDialogs.confirmOk(MyDataActivity.this, R.string.authorization_failed);
                            break;
                        case 409:
                            SimpleDialogs.confirmOk(MyDataActivity.this, R.string.profile_conflict);
                            break;
                        default:
                            SimpleDialogs.confirmOk(MyDataActivity.this,
                                    String.format(getText(R.string.save_profile_failed).toString(), response.code()));
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                    binding.myData.progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Error uploading profile", t);
                    SimpleDialogs.confirmOk(MyDataActivity.this,
                            String.format(getText(R.string.save_profile_failed).toString(), t.getMessage()));
                }
            });
        }

        saveLocalProfile(profile);
        Toast.makeText(this, R.string.preferences_saved, Toast.LENGTH_LONG).show();
    }

    private Profile createProfileFromUI() {
        var newProfile = new Profile(
                binding.myData.etNickname.getText().toString().trim(),
                license,
                binding.myData.cbOwnPhoto.isChecked(),
                binding.myData.cbAnonymous.isChecked(),
                binding.myData.etLinking.getText().toString().trim(),
                binding.myData.etEmail.getText().toString().trim()
        );

        if (this.profile != null) {
            newProfile.setEmailVerified(this.profile.getEmailVerified());
        }

        return newProfile;
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

    public boolean isValid(Profile profile) {
        if (StringUtils.isBlank(profile.getNickname())) {
            SimpleDialogs.confirmOk(this, R.string.missing_nickname);
            return false;
        }
        if (!isValidEmail(profile.getEmail())) {
            SimpleDialogs.confirmOk(this, R.string.missing_email_address);
            return false;
        }
        String url = profile.getLink();
        if (StringUtils.isNotBlank(url) && !isValidHTTPURL(url)) {
            SimpleDialogs.confirmOk(this, R.string.missing_link);
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
                rsapiClient.createAuthorizeUri());
        startActivity(intent);
    }

    public void logout(View view) {
        baseApplication.setAccessToken(null);
        rsapiClient.clearToken();
        profile = new Profile();
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
                            SimpleDialogs.confirmOk(MyDataActivity.this, R.string.password_changed);
                            break;
                        case 401:
                            SimpleDialogs.confirmOk(MyDataActivity.this, R.string.authorization_failed);
                            logout(view);
                            break;
                        default:
                            SimpleDialogs.confirmOk(MyDataActivity.this,
                                    String.format(getText(R.string.change_password_failed).toString(), response.code()));
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                    Log.e(TAG, "Error changing password", t);
                    SimpleDialogs.confirmOk(MyDataActivity.this,
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
        SimpleDialogs.confirmOkCancel(this, R.string.requestEmailVerification, (dialogInterface, i) -> rsapiClient.resendEmailVerification().enqueue(new Callback<>() {
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
