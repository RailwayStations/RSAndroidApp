package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.apache.commons.lang3.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import de.bahnhoefe.deutschlands.bahnhofsfotos.Dialogs.SimpleDialogs;
import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.ActivityMydataBinding;
import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.ChangePasswordBinding;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.License;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Profile;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyDataActivity extends AppCompatActivity {

    private static final String TAG = MyDataActivity.class.getSimpleName();

    private License license;
    private BaseApplication baseApplication;
    private RSAPI rsapi;
    private Profile profile;
    private String authorizationHeader;
    private ActivityMydataBinding binding;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMydataBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.login);

        binding.myData.profileForm.setVisibility(View.INVISIBLE);

        baseApplication = (BaseApplication) getApplication();
        rsapi = baseApplication.getRSAPI();

        setProfileToUI(baseApplication.getProfile());

        receiveInitialPassword(getIntent());
        if (isLoginDataAvailable(profile.getEmail(), profile.getPassword())) {
            authorizationHeader = RSAPI.Helper.getAuthorizationHeader(profile.getEmail(), profile.getPassword());
            loadRemoteProfile();
        }
    }

    private void setProfileToUI(final Profile profile) {
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
            binding.myData.tvEmailVerification.setTextColor(getResources().getColor(R.color.emailVerified));
        } else {
            binding.myData.tvEmailVerification.setText(R.string.emailUnverified);
            binding.myData.tvEmailVerification.setTextColor(getResources().getColor(R.color.emailUnverified));
        }
        this.profile = profile;
    }

    private void loadRemoteProfile() {
        if (authorizationHeader != null) {
            binding.myData.loginForm.setVisibility(View.VISIBLE);
            binding.myData.profileForm.setVisibility(View.GONE);

            rsapi.getProfile(authorizationHeader).enqueue(new Callback<Profile>() {
                @Override
                public void onResponse(final Call<Profile> call, final Response<Profile> response) {
                    switch (response.code()) {
                        case 200 :
                            Log.i(TAG, "Successfully loaded profile");
                            final Profile remoteProfile = response.body();
                            remoteProfile.setPassword(binding.myData.etPassword.getText().toString());
                            saveLocalProfile(remoteProfile);
                            showProfileView();
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
                public void onFailure(final Call<Profile> call, final Throwable t) {
                    new SimpleDialogs().confirm(MyDataActivity.this,
                            String.format(getText(R.string.read_profile_failed).toString(), t.getMessage()));
                }
            });
        }
    }

    private void showProfileView() {
        binding.myData.loginForm.setVisibility(View.GONE);
        binding.myData.profileForm.setVisibility(View.VISIBLE);
        getSupportActionBar().setTitle(R.string.tvProfile);
        binding.myData.btProfileSave.setText(R.string.bt_mydata_commit);
        binding.myData.btLogout.setVisibility(View.VISIBLE);
        binding.myData.btChangePassword.setVisibility(View.VISIBLE);
        binding.myData.initPasswordLayout.setVisibility(View.GONE);
    }

    private void receiveInitialPassword(final Intent intent) {
        if (intent != null) {
            if (Intent.ACTION_VIEW.equals(intent.getAction())) {
                final Uri data = intent.getData();
                if (data != null) {
                    profile.setPassword(data.getLastPathSegment());
                    binding.myData.etPassword.setText(profile.getPassword());
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
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        receiveInitialPassword(intent);
    }

    public void selectLicense(final View view) {
        license = binding.myData.cbLicenseCC0.isChecked() ? License.CC0 : License.UNKNOWN;
        if (license != License.CC0) {
            new SimpleDialogs().confirm(this, R.string.cc0_needed);
        }
    }

    public void registerOrSave(final View view) {
        boolean register = false;
        if (binding.myData.btProfileSave.getText().equals(getResources().getText(R.string.bt_register))) {
            authorizationHeader = null;
            register = true;
        }
        if (!saveProfile(view, register)) {
            return;
        }
        if (authorizationHeader == null) {
            profile = createProfileFromUI(register);
            rsapi.registration(profile).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(final Call<Void> call, final Response<Void> response) {
                    switch (response.code()) {
                        case 202:
                            authorizationHeader = RSAPI.Helper.getAuthorizationHeader(profile.getEmail(), profile.getNewPassword());
                            new SimpleDialogs().confirm(MyDataActivity.this, R.string.new_registration);
                            showProfileView();
                            saveLocalProfile(profile);
                            break;
                        case 400:
                            new SimpleDialogs().confirm(MyDataActivity.this, R.string.profile_wrong_data);
                            break;
                        case 409:
                            new SimpleDialogs().confirm(MyDataActivity.this, R.string.profile_conflict);
                            break;
                        case 422:
                            new SimpleDialogs().confirm(MyDataActivity.this, R.string.registration_data_incomplete);
                            break;
                        default:
                            new SimpleDialogs().confirm(MyDataActivity.this,
                                    String.format(getText(R.string.registration_failed).toString(), response.code()));
                    }
                }

                @Override
                public void onFailure(final Call<Void> call, final Throwable t) {
                    Log.e(TAG, "Registration failed", t);
                    new SimpleDialogs().confirm(MyDataActivity.this,
                            String.format(getText(R.string.registration_failed).toString(), t));
                }
            });
        }
    }

    private Profile createProfileFromUI(final boolean register) {
        final Profile profile = new Profile();
        profile.setNickname(binding.myData.etNickname.getText().toString().trim());
        profile.setEmail(binding.myData.etEmail.getText().toString().trim());
        profile.setLicense(license);
        profile.setPhotoOwner(binding.myData.cbOwnPhoto.isChecked());
        profile.setAnonymous(binding.myData.cbAnonymous.isChecked());
        profile.setLink(binding.myData.etLinking.getText().toString().trim());
        if (register) {
            profile.setPassword(binding.myData.etInitPassword.getText().toString().trim());
            profile.setNewPassword(binding.myData.etInitPassword.getText().toString().trim());
        } else {
            if (this.profile != null) {
                profile.setEmailVerified(this.profile.isEmailVerified());
            }
            profile.setPassword(binding.myData.etPassword.getText().toString().trim());
        }
        return profile;
    }

    public boolean saveProfile(final View view, final boolean registration) {
        profile = createProfileFromUI(false);
        if (!isValid(registration)) {
            return false;
        }
        if (authorizationHeader != null) {
            rsapi.saveProfile(authorizationHeader, profile).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(final Call<Void> call, final Response<Void> response) {
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
                public void onFailure(final Call<Void> call, final Throwable t) {
                    Log.e(TAG, "Error uploading profile", t);
                    new SimpleDialogs().confirm(MyDataActivity.this,
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

    private void saveLocalProfile(final Profile profile) {
        baseApplication.setProfile(profile);
        setProfileToUI(profile);
    }

    private boolean isLoginDataAvailable(final String email, final String password) {
        return StringUtils.isNotBlank(password) && StringUtils.isNotBlank(email);
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    public boolean isValid(final boolean register) {
        if (license == License.UNKNOWN) {
            new SimpleDialogs().confirm(this, R.string.cc0_needed);
            return false;
        }
        if (!binding.myData.cbOwnPhoto.isChecked()) {
            new SimpleDialogs().confirm(this, R.string.missing_photoOwner);
            return false;
        }
        if (StringUtils.isBlank(binding.myData.etNickname.getText())) {
            new SimpleDialogs().confirm(this, R.string.missing_nickname);
            return false;
        }
        if (!isValidEmail(binding.myData.etEmail.getText())) {
            new SimpleDialogs().confirm(this, R.string.missing_email_address);
            return false;
        }
        final String url = binding.myData.etLinking.getText().toString();
        if (StringUtils.isNotBlank(url)&& !isValidHTTPURL(url)) {
            new SimpleDialogs().confirm(this, R.string.missing_link);
            return false;
        }

        if (register) {
            final String newPassword = getValidPassword(binding.myData.etInitPassword, binding.myData.etInitPasswordRepeat);
            return newPassword != null;
        }

        return true;
    }

    private boolean isValidHTTPURL(final String urlString) {
        try {
            final URL url = new URL(urlString);
            if (!"http".equals(url.getProtocol()) && !"https".equals(url.getProtocol())) {
                return false;
            }
        } catch (final MalformedURLException e) {
            return false;
        }
        return true;
    }

    public boolean isValidEmail(final CharSequence target) {
        return target != null && android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();

    }

    public void onAnonymousChecked(final View view) {
        if (binding.myData.cbAnonymous.isChecked()) {
            binding.myData.etLinking.setVisibility(View.GONE);
            binding.myData.tvLinking.setVisibility(View.GONE);
        } else {
            binding.myData.etLinking.setVisibility(View.VISIBLE);
            binding.myData.tvLinking.setVisibility(View.VISIBLE);
        }
    }

    public void newRegister(final View view) {
        authorizationHeader = null;
        binding.myData.etEmail.setText(binding.myData.etEmailOrNickname.getText());
        binding.myData.profileForm.setVisibility(View.VISIBLE);
        binding.myData.initPasswordLayout.setVisibility(View.VISIBLE);
        binding.myData.loginForm.setVisibility(View.GONE);
        getSupportActionBar().setTitle(R.string.tvRegistration);
        binding.myData.btProfileSave.setText(R.string.bt_register);
        binding.myData.btLogout.setVisibility(View.GONE);
        binding.myData.btChangePassword.setVisibility(View.GONE);
    }

    public void login(final View view) {
        final String email = binding.myData.etEmailOrNickname.getText().toString();
        final String password = binding.myData.etPassword.getText().toString();
        if (isLoginDataAvailable(email, password)) {
            baseApplication.setEmail(email);
            baseApplication.setPassword(password);
            authorizationHeader = RSAPI.Helper.getAuthorizationHeader(email, password);
            loadRemoteProfile();
        } else {
            new SimpleDialogs().confirm(this, R.string.missing_login_data);
        }
    }

    public void logout(final View view) {
        profile.setNickname(null);;
        profile.setEmail(null);
        profile.setPassword(null);
        saveLocalProfile(profile);
        binding.myData.profileForm.setVisibility(View.GONE);
        binding.myData.loginForm.setVisibility(View.VISIBLE);
        getSupportActionBar().setTitle(R.string.login);
    }

    public void resetPassword(final View view) {
        authorizationHeader = null;
        final String emailOrNickname = binding.myData.etEmailOrNickname.getText().toString();
        if (StringUtils.isBlank(emailOrNickname)) {
            new SimpleDialogs().confirm(this, R.string.missing_email_or_nickname);
            return;
        }
        profile.setEmail(emailOrNickname);
        saveLocalProfile(profile);
        rsapi.resetPassword(emailOrNickname).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(final Call<Void> call, final Response<Void> response) {
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
            public void onFailure(final Call<Void> call, final Throwable t) {
                Log.e(TAG, "Request new password failed", t);
                new SimpleDialogs().confirm(MyDataActivity.this,
                        String.format(getText(R.string.request_password_failed).toString(), t));
            }
        });
    }

    public void changePassword(final View view) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogCustom));
        final ChangePasswordBinding passwordBinding = ChangePasswordBinding.inflate(getLayoutInflater());

        builder.setTitle(R.string.bt_change_password)
               .setView(passwordBinding.getRoot())
               .setIcon(R.mipmap.ic_launcher)
               // Add action buttons
               .setPositiveButton(android.R.string.ok, null)
               .setNegativeButton(android.R.string.cancel, (dialog, id) -> dialog.cancel());

        final AlertDialog alertDialog = builder.create();
        alertDialog.show();

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String newPassword = getValidPassword(passwordBinding.password, passwordBinding.passwordRepeat);
            if (newPassword == null) {
                return;
            }
            alertDialog.dismiss();

            try {
                newPassword = URLEncoder.encode(newPassword, "UTF-8");
            } catch (final UnsupportedEncodingException e) {
                Log.e(TAG, "Error encoding new password", e);
            }

            rsapi.changePassword(authorizationHeader, newPassword).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(final Call<Void> call, final Response<Void> response) {
                    switch (response.code()) {
                        case 200:
                            Log.i(TAG, "Successfully changed password");
                            binding.myData.etPassword.setText(passwordBinding.password.getText());
                            baseApplication.setPassword(passwordBinding.password.getText().toString());
                            authorizationHeader = RSAPI.Helper.getAuthorizationHeader(profile.getEmail(), passwordBinding.password.getText().toString());
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
                public void onFailure(final Call<Void> call, final Throwable t) {
                    Log.e(TAG, "Error changing password", t);
                    new SimpleDialogs().confirm(MyDataActivity.this,
                            String.format(getText(R.string.change_password_failed).toString(), t.getMessage()));
                }
            });
        });

    }

    private String getValidPassword(final EditText etNewPassword, final EditText etPasswordRepeat) {
        final String newPassword = etNewPassword.getText().toString().trim();

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

    public void requestEmailVerification(final View view) {
        new SimpleDialogs().confirm(this, R.string.requestEmailVerification, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialogInterface, final int i) {
                rsapi.resendEmailVerification(authorizationHeader).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(final Call<Void> call, final Response<Void> response) {
                        switch (response.code()) {
                            case 200:
                                Log.i(TAG, "Successfully requested email verification");
                                Toast.makeText(MyDataActivity.this, R.string.emailVerificationRequested, Toast.LENGTH_LONG).show();
                                break;
                            default:
                                Toast.makeText(MyDataActivity.this, R.string.emailVerificationRequestFailed, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(final Call<Void> call, final Throwable t) {
                        Log.e(TAG, "Error requesting email verification", t);
                        Toast.makeText(MyDataActivity.this, R.string.emailVerificationRequestFailed, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

}
