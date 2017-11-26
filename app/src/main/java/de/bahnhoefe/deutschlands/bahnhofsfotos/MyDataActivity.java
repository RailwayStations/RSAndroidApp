package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import de.bahnhoefe.deutschlands.bahnhofsfotos.Dialogs.SimpleDialogs;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.License;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Linking;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.PhotoOwner;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.UpdatePolicy;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.ConnectionUtil;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.Constants;
import org.json.JSONException;
import org.json.JSONObject;

public class MyDataActivity extends AppCompatActivity {
    private final String TAG = getClass().getSimpleName();
    private EditText etNickname, etLink, etEmail, etUploadToken;
    private RadioGroup rgLicence, rgPhotoOwner, rgLinking, rgUpdatePolicy;
    private License license;
    private PhotoOwner photoOwner;
    private String nickname;
    private String email;
    private String link;
    private Linking linking;
    private String uploadToken;
    private BaseApplication baseApplication;
    private UpdatePolicy updatePolicy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mydata);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.title_activity_my_data);
        baseApplication = (BaseApplication) getApplication();

        etNickname = (EditText) findViewById(R.id.etNickname);
        etUploadToken = (EditText) findViewById(R.id.etUploadToken);
        etEmail = (EditText) findViewById(R.id.etEmail);
        etLink = (EditText) findViewById(R.id.etLinking);

        rgLicence = (RadioGroup) findViewById(R.id.rgLicence);
        rgPhotoOwner = (RadioGroup) findViewById(R.id.rgOwnPhoto);
        rgLinking = (RadioGroup) findViewById(R.id.rgLinking);
        rgUpdatePolicy = (RadioGroup) findViewById(R.id.rgUpdatePolicy);

        license = baseApplication.getLicense();
        rgLicence.check(license.getId());

        photoOwner = baseApplication.getPhotoOwner();
        rgPhotoOwner.check(photoOwner.getId());

        linking = baseApplication.getLinking();
        rgLinking.check(linking.getId());

        link = baseApplication.getPhotographerLink();
        etLink.setText(link);

        nickname = baseApplication.getNickname();
        etNickname.setText(nickname);

        email = baseApplication.getEmail();
        etEmail.setText(email);

        uploadToken = baseApplication.getUploadToken();
        etUploadToken.setText(uploadToken);

        updatePolicy = baseApplication.getUpdatePolicy();
        rgUpdatePolicy.check(updatePolicy.getId());

        receiveUploadToken(getIntent());
    }

    private void receiveUploadToken(Intent intent) {
        if (intent != null) {
            if (Intent.ACTION_VIEW.equals(intent.getAction())) {
                Uri data = intent.getData();
                if (data != null) {
                    uploadToken = data.getLastPathSegment();
                    etUploadToken.setText(uploadToken);
                    saveSettings(null);
                }
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        receiveUploadToken(intent);
    }

    public void selectLicence(View view) {
        license = License.byId(view.getId());
        if (license == License.CC4) {
            new SimpleDialogs().confirm(this, R.string.cc4_warning);
        }
    }

    public void selectUpdatePolicy(View view) {
        updatePolicy = UpdatePolicy.byId(view.getId());
    }

    public void selectPhotoOwner(View view) {
        photoOwner = PhotoOwner.byId(view.getId());
    }

    public void linkToPhotographer(View view) {
        linking = Linking.byId(view.getId());
    }

    public void register(View view) {
        if (!isValid()) {
            return;
        }
        saveSettings(view);
        if (ConnectionUtil.checkInternetConnection(this)) {
            new RegisterTask(getString(R.string.rs_api_key)).execute();
        }
    }

    public void saveSettings(View view) {
        baseApplication.setLicense(license);
        baseApplication.setPhotoOwner(photoOwner);
        baseApplication.setLinking(linking);
        baseApplication.setPhotographerLink(etLink.getText().toString().trim());
        baseApplication.setNickname(etNickname.getText().toString().trim());
        baseApplication.setEmail(etEmail.getText().toString().trim());
        baseApplication.setUploadToken(etUploadToken.getText().toString().trim());
        baseApplication.setUpdatePolicy(updatePolicy);
        Toast.makeText(this, R.string.preferences_saved, Toast.LENGTH_LONG).show();
    }

    public void clearSettings(View viewButtonClear) {
        baseApplication.setLicense(License.UNKNOWN);
        baseApplication.setPhotoOwner(PhotoOwner.UNKNOWN);
        baseApplication.setLinking(Linking.UNKNOWN);
        baseApplication.setPhotographerLink(null);
        baseApplication.setNickname(null);
        baseApplication.setEmail(null);
        baseApplication.setUploadToken(null);
        baseApplication.setUpdatePolicy(UpdatePolicy.NOTIFY);
        Toast.makeText(this, R.string.preferences_cleared, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    public boolean isValid() {
        if (license == License.UNKNOWN) {
            new SimpleDialogs().confirm(this, R.string.missing_licence);
            return false;
        }
        if (photoOwner == PhotoOwner.UNKNOWN) {
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
        if (linking == Linking.UNKNOWN) {
            new SimpleDialogs().confirm(this, R.string.missing_linking);
            return false;
        }
        String url = etLink.getText().toString();
        if (linking != Linking.NO && !isValidHTTPURL(url)) {
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

    public class RegisterTask extends AsyncTask<Void, String, Integer> {

        private final String apiKey;
        private final JSONObject registrationData;
        private ProgressDialog progressDialog;

        public RegisterTask(String apiKey) {
            this.apiKey = apiKey;
            registrationData = new JSONObject();
            try {
                registrationData.put("nickname", etNickname.getText().toString().trim());
                registrationData.put("email", etEmail.getText().toString().trim());
                registrationData.put("license", license);
                registrationData.put("photoOwner", photoOwner.isOwner());
                registrationData.put("linking", linking);
                registrationData.put("link", etLink.getText().toString().trim());
            } catch (JSONException e) {
                throw new RuntimeException("Error creating RegistrationData", e);
            }
        }


        @Override
        protected Integer doInBackground(Void... params) {
            HttpURLConnection conn = null;
            DataOutputStream wr = null;
            int status = -1;

            publishProgress("Verbinde...");
            try {
                URL url = new URL(String.format("%s/registration", Constants.API_START_URL));
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoOutput( true );
                conn.setInstanceFollowRedirects( false );
                conn.setRequestMethod( "POST" );
                conn.setRequestProperty( "Content-Type", "application/json");
                conn.setRequestProperty( "API-Key", apiKey);
                conn.setUseCaches( false );

                wr = new DataOutputStream( conn.getOutputStream());
                wr.writeChars( registrationData.toString() );
                wr.flush();

                status = conn.getResponseCode();
                Log.i(TAG, "Registration response: " + status);
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

            return status;
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (MyDataActivity.this.isDestroyed()) {
                return;
            }
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            if (result == 202) {
                new SimpleDialogs().confirm(MyDataActivity.this, R.string.registration_completed);
            } else if (result == 422) {
                new SimpleDialogs().confirm(MyDataActivity.this, R.string.registration_data_incomplete);
            } else {
                new SimpleDialogs().confirm(MyDataActivity.this,
                        String.format(getText(R.string.registration_failed).toString(), result));
            }
        }

        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(MyDataActivity.this);
            progressDialog.setIndeterminate(false);
            progressDialog.show();
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            progressDialog.setMessage("Sende Daten ... " + values[0]);
        }

    }

}
