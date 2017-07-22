package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
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
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.ConnectionUtil;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.Constants;
import org.json.JSONException;
import org.json.JSONObject;

public class MyDataActivity extends AppCompatActivity {
    private final String TAG = getClass().getSimpleName();
    private EditText etNickname, etLink, etEmail, etUploadToken;
    private RadioGroup rgLicence, rgPhotoOwner, rgLinking;
    private String licence, photoOwner, nickname, email, link, linking, uploadToken;
    private BaseApplication baseApplication;

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

        licence = baseApplication.getLicense();
        if (licence.equals("CC0")) {
            rgLicence.check(R.id.rbCC0);
        } else if (licence.equals("CC4")) {
            rgLicence.check(R.id.rbCC40);
        }
        photoOwner = baseApplication.getPhotoOwner();
        if (photoOwner.equals("YES")) {
            rgPhotoOwner.check(R.id.rbOwnPhotoYes);
        } else if (photoOwner.equals("NO")) {
            rgPhotoOwner.check(R.id.rbOwnPhotoNo);
        }
        linking = baseApplication.getLinking();
        switch (linking) {
            case "XING":
                rgLinking.check(R.id.rbLinkingXing);
                break;
            case "SNAPCHAT":
                rgLinking.check(R.id.rbLinkingSnapchat);
                break;
            case "TWITTER":
                rgLinking.check(R.id.rbLinkingTwitter);
                break;
            case "WEBPAGE":
                rgLinking.check(R.id.rbLinkingWebpage);
                break;
            case "INSTAGRAM":
                rgLinking.check(R.id.rbLinkingInstagram);
                break;
            case "NO":
                rgLinking.check(R.id.rbLinkingNo);
                break;
        }

        link = baseApplication.getPhotographerLink();
        etLink.setText(link);

        nickname = baseApplication.getNickname();
        etNickname.setText(nickname);

        email = baseApplication.getEmail();
        etEmail.setText(email);

        uploadToken = baseApplication.getUploadToken();
        etUploadToken.setText(uploadToken);

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
        switch (view.getId()) {
            case R.id.rbCC0:
                licence = "CC0";
                break;
            case R.id.rbCC40:
                licence = "CC4";
                break;
        }

    }

    public void selectPhotoOwner(View view) {

        switch (view.getId()) {
            case R.id.rbOwnPhotoYes:
                photoOwner = "YES";
                break;
            case R.id.rbOwnPhotoNo:
                photoOwner = "NO";
                break;
        }

    }

    public void linkToPhotographer(View view) {

        switch (view.getId()) {
            case R.id.rbLinkingInstagram:
                linking = "INSTAGRAM";
                break;
            case R.id.rbLinkingSnapchat:
                linking = "SNAPCHAT";
                break;
            case R.id.rbLinkingNo:
                linking = "NO";
                break;
            case R.id.rbLinkingTwitter:
                linking = "TWITTER";
                break;
            case R.id.rbLinkingXing:
                linking = "XING";
                break;
            case R.id.rbLinkingWebpage:
                linking = "WEBPAGE";
                break;
        }
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
        baseApplication.setLicense(licence);
        baseApplication.setPhotoOwner(photoOwner);
        baseApplication.setLinking(linking);
        baseApplication.setPhotographerLink(etLink.getText().toString().trim());
        baseApplication.setNickname(etNickname.getText().toString().trim());
        baseApplication.setEmail(etEmail.getText().toString().trim());
        baseApplication.setUploadToken(etUploadToken.getText().toString().trim());
        Toast.makeText(this, R.string.preferences_saved, Toast.LENGTH_LONG).show();
    }

    public void clearSettings(View viewButtonClear) {
        baseApplication.setLicense(null);
        baseApplication.setPhotoOwner(null);
        baseApplication.setLinking(null);
        baseApplication.setPhotographerLink(null);
        baseApplication.setNickname(null);
        baseApplication.setEmail(null);
        baseApplication.setUploadToken(null);
        Toast.makeText(this, R.string.preferences_cleared, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    public boolean isValid() {
        if (TextUtils.isEmpty(licence)) {
            new SimpleDialogs().confirm(this, R.string.missing_licence);
            return false;
        }
        if (TextUtils.isEmpty(photoOwner)) {
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
        if (TextUtils.isEmpty(linking)) {
            new SimpleDialogs().confirm(this, R.string.missing_linking);
            return false;
        }
        String url = etLink.getText().toString();
        if (!"NO".equals(linking) && !isValidHTTPURL(url)) {
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
                registrationData.put("license", licence);
                registrationData.put("photoOwner", "YES".equals(photoOwner));
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
                new SimpleDialogs().confirm(MyDataActivity.this, R.string.registration_failed);
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
