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
import java.util.Objects;

import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.ActivityMydataBinding;
import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.ChangePasswordBinding;
import de.bahnhoefe.deutschlands.bahnhofsfotos.dialogs.SimpleDialogs;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.License;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Profile;
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

        receiveInitialPassword(getIntent());
        if (isLoginDataAvailable(profile.getEmail(), profile.getPassword())) {
            loadRemoteProfile();
        }
    }

    private void setProfileToUI(Profile profile) {
        binding.myData.etNickname.setText(profile.getNickname());
        binding.myData.etPassword.setText(profile.getPassword());
        binding.myData.etEmail.setText(profile.getEmail());
        binding.myData.etEmailOrNickname.setText(profile.getEmail());
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
                            remoteProfile.setPassword(binding.myData.etPassword.getText().toString());
                            saveLocalProfile(remoteProfile);
                            showProfileView();
                        }
                        break;
                    case 401:
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

    private void receiveInitialPassword(Intent intent) {
        if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction())) {
            var data = intent.getData();
            if (data != null) {
                profile.setPassword(data.getLastPathSegment());
                binding.myData.etPassword.setText(profile.getPassword());
                baseApplication.setPassword(profile.getPassword());
                if (isLoginDataAvailable(profile.getEmail(), profile.getPassword())) {
                    loadRemoteProfile();
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
        license = binding.myData.cbLicenseCC0.isChecked() ? License.CC0 : License.UNKNOWN;
        if (license != License.CC0) {
            SimpleDialogs.confirm(this, R.string.cc0_needed);
        }
    }

    public void registerOrSave(View view) {
        boolean register = false;
        if (binding.myData.btProfileSave.getText().equals(getResources().getText(R.string.bt_register))) {
            rsapiClient.setCredentials(null, null);
            register = true;
        }
        if (!saveProfile(register)) {
            return;
        }
        if (!rsapiClient.hasCredentials()) {
            profile = createProfileFromUI(register);
            rsapiClient.registration(profile).enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                    switch (response.code()) {
                        case 202:
                            rsapiClient.setCredentials(profile.getEmail(), profile.getNewPassword());
                            SimpleDialogs.confirm(MyDataActivity.this, R.string.new_registration);
                            showProfileView();
                            saveLocalProfile(profile);
                            break;
                        case 400:
                            SimpleDialogs.confirm(MyDataActivity.this, R.string.profile_wrong_data);
                            break;
                        case 409:
                            SimpleDialogs.confirm(MyDataActivity.this, R.string.profile_conflict);
                            break;
                        case 422:
                            SimpleDialogs.confirm(MyDataActivity.this, R.string.registration_data_incomplete);
                            break;
                        default:
                            SimpleDialogs.confirm(MyDataActivity.this,
                                    String.format(getText(R.string.registration_failed).toString(), response.code()));
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                    Log.e(TAG, "Registration failed", t);
                    SimpleDialogs.confirm(MyDataActivity.this,
                            String.format(getText(R.string.registration_failed).toString(), t));
                }
            });
        }
    }

    private Profile createProfileFromUI(boolean register) {
        var profileBuilder = Profile.builder()
                .nickname(binding.myData.etNickname.getText().toString().trim())
                .email(binding.myData.etEmail.getText().toString().trim())
                .license(license)
                .photoOwner(binding.myData.cbOwnPhoto.isChecked())
                .anonymous(binding.myData.cbAnonymous.isChecked())
                .link(binding.myData.etLinking.getText().toString().trim());

        if (register) {
            profileBuilder.password(binding.myData.etInitPassword.getText().toString().trim())
                    .newPassword(binding.myData.etInitPassword.getText().toString().trim());
        } else {
            if (this.profile != null) {
                profileBuilder.emailVerified(this.profile.isEmailVerified());
            }
            profileBuilder.password(binding.myData.etPassword.getText().toString().trim());
        }
        return profileBuilder.build();
    }

    public boolean saveProfile(boolean registration) {
        profile = createProfileFromUI(false);
        if (!isValid(registration)) {
            return false;
        }
        if (rsapiClient.hasCredentials()) {
            rsapiClient.saveProfile(profile).enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                    switch (response.code()) {
                        case 200:
                            rsapiClient.setCredentials(profile.getEmail(), profile.getPassword());
                            Log.i(TAG, "Successfully saved profile");
                            break;
                        case 202:
                            SimpleDialogs.confirm(MyDataActivity.this, R.string.password_email);
                            break;
                        case 400:
                            SimpleDialogs.confirm(MyDataActivity.this, R.string.profile_wrong_data);
                            break;
                        case 401:
                            rsapiClient.clearCredentials();
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
        if (!registration) {
            Toast.makeText(this, R.string.preferences_saved, Toast.LENGTH_LONG).show();
        }
        return true;
    }

    private void saveLocalProfile(Profile profile) {
        baseApplication.setProfile(profile);
        setProfileToUI(profile);
    }

    private boolean isLoginDataAvailable(String username, String password) {
        return StringUtils.isNotBlank(password) && StringUtils.isNotBlank(username);
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    public boolean isValid(boolean register) {
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

        if (register) {
            var newPassword = getValidPassword(binding.myData.etInitPassword, binding.myData.etInitPasswordRepeat);
            return newPassword != null;
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

    public void newRegister(View view) {
        rsapiClient.clearCredentials();
        binding.myData.etEmail.setText(binding.myData.etEmailOrNickname.getText());
        binding.myData.profileForm.setVisibility(View.VISIBLE);
        binding.myData.initPasswordLayout.setVisibility(View.VISIBLE);
        binding.myData.loginForm.setVisibility(View.GONE);
        Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.tvRegistration);
        binding.myData.btProfileSave.setText(R.string.bt_register);
        binding.myData.btLogout.setVisibility(View.GONE);
        binding.myData.btChangePassword.setVisibility(View.GONE);
    }

    public void login(View view) {
        var username = binding.myData.etEmailOrNickname.getText().toString();
        var password = binding.myData.etPassword.getText().toString();
        if (isLoginDataAvailable(username, password)) {
            baseApplication.setEmail(username);
            baseApplication.setPassword(password);
            rsapiClient.setCredentials(username, password);
            loadRemoteProfile();
        } else {
            SimpleDialogs.confirm(this, R.string.missing_login_data);
        }
    }

    public void logout(View view) {
        profile = Profile.builder().build();
        saveLocalProfile(profile);
        binding.myData.profileForm.setVisibility(View.GONE);
        binding.myData.loginForm.setVisibility(View.VISIBLE);
        Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.login);
    }

    public void resetPassword(View view) {
        rsapiClient.clearCredentials();
        var emailOrNickname = binding.myData.etEmailOrNickname.getText().toString();
        if (StringUtils.isBlank(emailOrNickname)) {
            SimpleDialogs.confirm(this, R.string.missing_email_or_nickname);
            return;
        }
        profile.setEmail(emailOrNickname);
        saveLocalProfile(profile);
        rsapiClient.resetPassword(emailOrNickname).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                switch (response.code()) {
                    case 202:
                        SimpleDialogs.confirm(MyDataActivity.this, R.string.password_email);
                        break;
                    case 400:
                        SimpleDialogs.confirm(MyDataActivity.this, R.string.profile_wrong_data);
                        break;
                    case 404:
                        SimpleDialogs.confirm(MyDataActivity.this, R.string.profile_not_found);
                        break;
                    default:
                        SimpleDialogs.confirm(MyDataActivity.this,
                                String.format(getText(R.string.request_password_failed).toString(), response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Log.e(TAG, "Request new password failed", t);
                SimpleDialogs.confirm(MyDataActivity.this,
                        String.format(getText(R.string.request_password_failed).toString(), t));
            }
        });
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
                            binding.myData.etPassword.setText(passwordBinding.password.getText());
                            baseApplication.setPassword(passwordBinding.password.getText().toString());
                            rsapiClient.setCredentials(profile.getEmail(), passwordBinding.password.getText().toString());
                            SimpleDialogs.confirm(MyDataActivity.this, R.string.password_changed);
                            break;
                        case 401:
                            rsapiClient.clearCredentials();
                            SimpleDialogs.confirm(MyDataActivity.this, R.string.authorization_failed);
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
