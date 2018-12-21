package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
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
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.UpdatePolicy;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.ConnectionUtil;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.Constants;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

public class MyDataActivity extends AppCompatActivity {

    private static final String TAG = MyDataActivity.class.getSimpleName();
    private static final int RC_SIGN_IN = 1;

    private EditText etNickname, etLink, etEmail, etUploadToken;
    private CheckBox cbLicenseCC0;
    private CheckBox cbAnonymous;
    private CheckBox cbPhotoOwner;
    private License license;
    private BaseApplication baseApplication;
    private UpdatePolicy updatePolicy;
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
        RadioGroup rgUpdatePolicy = findViewById(R.id.rgUpdatePolicy);

        baseApplication = (BaseApplication) getApplication();
        updatePolicy = baseApplication.getUpdatePolicy();
        setProfileToUI(baseApplication.getProfile());
        rgUpdatePolicy.check(updatePolicy.getId());

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
                new RegisterTask(this, createProfileFromUI(), idToken).execute();
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e);
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

        this.profile = profile;
    }

    private void loadRemoteProfile() {
        if (isUploadTokenAvailable() && ConnectionUtil.checkInternetConnection(this)) {
            final Profile profile = createProfileFromUI();
            new ReadProfileTask(this, profile.getEmail(), profile.getUploadToken()).execute();
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
                    new ReadProfileTask(this, profile.getEmail(), profile.getUploadToken());
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

    public void selectUpdatePolicy(View view) {
        updatePolicy = UpdatePolicy.byId(view.getId());
    }

    public void register(View view) {
        if (!isValid()) {
            return;
        }
        saveSettings(view);
        if (ConnectionUtil.checkInternetConnection(this)) {
            new RegisterTask(this, createProfileFromUI() , null).execute();
        }
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
        if (isUploadTokenAvailable() && ConnectionUtil.checkInternetConnection(this)) {
            // TODO: email must be the old one if changed
            new SaveProfileTask(this, createProfileFromUI(), etEmail.getText().toString().trim()).execute();
        }

        saveLocalProfile(createProfileFromUI());
        baseApplication.setUpdatePolicy(updatePolicy);
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
        if (etNickname.getText().toString().isEmpty()) {
            new SimpleDialogs().confirm(this, R.string.missing_nickname);
            return false;
        }
        if (!isValidEmail(etEmail.getText())) {
            new SimpleDialogs().confirm(this, R.string.missing_email_address);
            return false;
        }
        String url = etLink.getText().toString();
        if (url != null && url.trim().length() > 0 && !isValidHTTPURL(url)) {
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

    public static class RegisterTask extends AsyncTask<Void, String, Profile> {

        private final JSONObject registrationData;
        private final WeakReference<MyDataActivity> activityRef;
        private final Resources resource;
        private final String googleIdToken;
        private int status = -1;

        public RegisterTask(MyDataActivity activity, Profile profile, String googleIdToken) {
            this.activityRef = new WeakReference<>(activity);
            this.resource = activity.getResources();
            this.registrationData = profile.toJson();
            this.googleIdToken = googleIdToken;
        }

        @Override
        protected Profile doInBackground(Void... params) {
            HttpURLConnection conn = null;
            DataOutputStream wr = null;
            Profile profile = new Profile();

            try {
                URL url = new URL(String.format("%s/registration%s", Constants.API_START_URL, googleIdToken != null ? "/withGoogleIdToken" : ""));
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoOutput( true );
                conn.setInstanceFollowRedirects( false );
                conn.setRequestMethod( "POST" );
                conn.setRequestProperty( "Content-Type", "application/json");
                if (googleIdToken != null) {
                    conn.setRequestProperty( "Google-Id-Token", googleIdToken);
                }
                conn.setUseCaches( false );

                wr = new DataOutputStream( conn.getOutputStream());
                wr.writeChars( registrationData.toString() );
                wr.flush();

                status = conn.getResponseCode();
                Log.i(TAG, "Registration response: " + status);
                if (status == 202) {
                    InputStream stream = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                    StringBuffer buffer = new StringBuffer();
                    String line = "";
                    while ((line = reader.readLine()) != null) {
                        buffer.append(line);
                    }
                    String content = buffer.toString();
                    JSONObject jsonObj = new JSONObject(content);
                    profile.setUploadToken(jsonObj.getString("uploadToken"));
                    profile.setEmail(jsonObj.getString("email"));
                    profile.setNickname(jsonObj.getString("nickname"));
                    profile.setLink(jsonObj.getString("link"));
                    profile.setAnonymous(jsonObj.getBoolean("anonymous"));
                    profile.setPhotoOwner(jsonObj.getBoolean("photoOwner"));
                    profile.setLicense(License.byName(jsonObj.getString("license")));
                }
            } catch ( Exception e) {
                Log.e(TAG, "Could not register", e);
                throw new RuntimeException("Error sending registration", e);
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
                try {
                    if (wr != null) {
                        wr.close();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Cannot close stream", e);
                }
            }

            return profile;
        }

        @Override
        protected void onPostExecute(Profile result) {
            MyDataActivity activity = activityRef.get();
            if (activity == null || activity.isDestroyed()) {
                return;
            }
            if (status == 202) {
                if (StringUtils.isNotBlank(result.getUploadToken())) {
                    activity.saveLocalProfile(result);
                    new SimpleDialogs().confirm(activity, R.string.upload_token_received);
                } else {
                    new SimpleDialogs().confirm(activity, R.string.upload_token_email);
                }
            } else if (status == 400) {
                new SimpleDialogs().confirm(activity, R.string.profile_wrong_data);
            } else if (status == 409) {
                new SimpleDialogs().confirm(activity, R.string.profile_conflict);
            } else if (status == 422) {
                new SimpleDialogs().confirm(activity, R.string.registration_data_incomplete);
            } else {
                new SimpleDialogs().confirm(activity,
                        String.format(resource.getText(R.string.registration_failed).toString(), result));
            }
        }

    }

    public static class SaveProfileTask extends AsyncTask<Void, String, Integer> {

        private final WeakReference<MyDataActivity> activityRef;
        private final JSONObject registrationData;
        private final String email;
        private final String uploadToken;

        public SaveProfileTask(MyDataActivity activity, Profile profile, String email) {
            this.activityRef = new WeakReference<>(activity);
            this.registrationData = profile.toJson();
            this.uploadToken = profile.getUploadToken();
            this.email = email;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            HttpURLConnection conn = null;
            DataOutputStream wr = null;
            final int status;

            try {
                URL url = new URL(String.format("%s/myProfile", Constants.API_START_URL));
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoOutput( true );
                conn.setInstanceFollowRedirects( false );
                conn.setRequestMethod( "POST" );
                conn.setRequestProperty( "Content-Type", "application/json");
                conn.setRequestProperty( "Upload-Token", uploadToken);
                conn.setRequestProperty( "Email", email);
                conn.setUseCaches( false );

                wr = new DataOutputStream( conn.getOutputStream());
                wr.writeChars( registrationData.toString() );
                wr.flush();

                status = conn.getResponseCode();
                Log.i(TAG, "Profile response: " + status);
            } catch ( Exception e) {
                Log.e(TAG, "Couldn't save profile", e);
                throw new RuntimeException("Error sending profile", e);
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
                try {
                    if (wr != null) {
                        wr.close();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Cannot close stream", e);
                }
            }

            return status;
        }

        @Override
        protected void onPostExecute(Integer result) {
            MyDataActivity activity = activityRef.get();
            if (activity == null || activity.isDestroyed()) {
                return;
            }
            if (result == 200) {
                Log.i(TAG, "Successfully saved profile");
            } else if (result == 202) {
                new SimpleDialogs().confirm(activity, R.string.upload_token_email);
            } else if (result == 400) {
                new SimpleDialogs().confirm(activity, R.string.profile_wrong_data);
            } else if (result == 401) {
                new SimpleDialogs().confirm(activity, R.string.upload_token_invalid);
            } else if (result == 409) {
                new SimpleDialogs().confirm(activity, R.string.profile_conflict);
            } else {
                new SimpleDialogs().confirm(activity,
                        String.format(activity.getText(R.string.save_profile_failed).toString(), result));
            }
        }

    }

    public static class ReadProfileTask extends AsyncTask<Void, String, Profile> {

        private final WeakReference<MyDataActivity> activityRef;
        private final String email;
        private final String uploadToken;
        private int status = -1;

        public ReadProfileTask(MyDataActivity activity, String email, String uploadToken) {
            this.activityRef = new WeakReference<>(activity);
            this.uploadToken = uploadToken;
            this.email = email;
        }

        @Override
        protected Profile doInBackground(Void... params) {
            HttpURLConnection conn = null;
            Profile profile = new Profile();

            try {
                URL url = new URL(String.format("%s/myProfile", Constants.API_START_URL));
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty( "Content-Type", "application/json");
                conn.setRequestProperty( "Upload-Token", uploadToken);
                conn.setRequestProperty( "Email", email);
                conn.connect();
                status = conn.getResponseCode();
                Log.i(TAG, "Profile response: " + status);

                if (status == 200) {
                    InputStream stream = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                    StringBuffer buffer = new StringBuffer();
                    String line = "";
                    while ((line = reader.readLine()) != null) {
                        buffer.append(line);
                    }
                    String content = buffer.toString();
                    JSONObject jsonObj = new JSONObject(content);
                    profile.setUploadToken(uploadToken);
                    profile.setEmail(email);
                    profile.setNickname(jsonObj.getString("nickname"));
                    profile.setLink(jsonObj.getString("link"));
                    profile.setAnonymous(jsonObj.getBoolean("anonymous"));
                    profile.setPhotoOwner(jsonObj.getBoolean("photoOwner"));
                    profile.setLicense(License.byName(jsonObj.getString("license")));
                }
            } catch ( Exception e) {
                Log.e(TAG, "Couldn't read profile", e);
                throw new RuntimeException("Error reading profile", e);
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }

            return profile;
        }

        @Override
        protected void onPostExecute(Profile result) {
            MyDataActivity activity = activityRef.get();
            if (activity == null || activity.isDestroyed()) {
                return;
            }
            if (status == 200) {
                Log.i(TAG, "Successfully loaded profile");
                activity.saveLocalProfile(result);
            } else if (status == 401) {
                new SimpleDialogs().confirm(activity, R.string.upload_token_invalid);
            } else {
                new SimpleDialogs().confirm(activity,
                        String.format(activity.getText(R.string.read_profile_failed).toString(), result));
            }
        }

    }

}
