package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import de.bahnhoefe.deutschlands.bahnhofsfotos.util.Constants;
import org.json.JSONException;
import org.json.JSONObject;

public class MyDataActivity extends AppCompatActivity {
    private static final String DEFAULT = "N/A";
    private final String TAG = getClass().getSimpleName();
    private EditText etNickname, etLink, etEmail, etUploadToken;
    private RadioGroup rgLicence, rgPhotoOwner, rgLinking;
    private RadioButton rbLinkingXing, rbLinkingTwitter, rbLinkingSnapchat, rbLinkingInstagram, rbLinkingWebpage, rbLinkingNo;
    private RadioButton rbLicenceCC0, rbLicenceCC4;
    private RadioButton rbPhotoOwnerYes, rbPhotoOwnerNo;
    private String licence, photoOwner, nickname, email, link, linking, uploadToken;
    private Button btCommit, btClear;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mydata);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        etNickname = (EditText) findViewById(R.id.etNickname);
        etUploadToken = (EditText) findViewById(R.id.etUploadToken);
        etEmail = (EditText) findViewById(R.id.etEmail);
        etLink = (EditText) findViewById(R.id.etLinking);
        btCommit = (Button) findViewById(R.id.bt_mydata_commit);
        btClear = (Button) findViewById(R.id.bt_mydata_clear);

        rgLicence = (RadioGroup) findViewById(R.id.rgLicence);
        rgPhotoOwner = (RadioGroup) findViewById(R.id.rgOwnPhoto);
        rgLinking = (RadioGroup) findViewById(R.id.rgLinking);

        rbLinkingXing = (RadioButton) findViewById(R.id.rbLinkingXing);
        rbLinkingTwitter = (RadioButton) findViewById(R.id.rbLinkingTwitter);
        rbLinkingSnapchat = (RadioButton) findViewById(R.id.rbLinkingSnapchat);
        rbLinkingInstagram = (RadioButton) findViewById(R.id.rbLinkingInstagram);
        rbLinkingWebpage = (RadioButton) findViewById(R.id.rbLinkingWebpage);
        rbLinkingNo = (RadioButton) findViewById(R.id.rbLinkingNo);

        rbLicenceCC0 = (RadioButton) findViewById(R.id.rbCC0);
        rbLicenceCC4 = (RadioButton) findViewById(R.id.rbCC40);

        rbPhotoOwnerNo = (RadioButton) findViewById(R.id.rbOwnPhotoNo);
        rbPhotoOwnerYes = (RadioButton) findViewById(R.id.rbOwnPhotoYes);

        SharedPreferences sharedPreferences = MyDataActivity.this.getSharedPreferences(getString(R.string.PREF_FILE), Context.MODE_PRIVATE);

        licence = sharedPreferences.getString(getString(R.string.LICENCE), DEFAULT);
        if (licence.equals("CC0")) {
            rgLicence.check(R.id.rbCC0);
        } else if (licence.equals("CC4")) {
            rgLicence.check(R.id.rbCC40);
        }
        photoOwner = sharedPreferences.getString(getString(R.string.PHOTO_OWNER), DEFAULT);
        if (photoOwner.equals("YES")) {
            rgPhotoOwner.check(R.id.rbOwnPhotoYes);
        } else if (photoOwner.equals("NO")) {
            rgPhotoOwner.check(R.id.rbOwnPhotoNo);
        }
        linking = sharedPreferences.getString(getString(R.string.LINKING), DEFAULT);
        if (linking.equals("XING")) {
            rgLinking.check(R.id.rbLinkingXing);
        } else if (linking.equals("SNAPCHAT")) {
            rgLinking.check(R.id.rbLinkingSnapchat);
        } else if (linking.equals("TWITTER")) {
            rgLinking.check(R.id.rbLinkingTwitter);
        } else if (linking.equals("WEBPAGE")) {
            rgLinking.check(R.id.rbLinkingWebpage);
        } else if (linking.equals("INSTAGRAM")) {
            rgLinking.check(R.id.rbLinkingInstagram);
        } else if (linking.equals("NO")) {
            rgLinking.check(R.id.rbLinkingNo);
        }

        link = sharedPreferences.getString(getString(R.string.LINK_TO_PHOTOGRAPHER), DEFAULT);
        nickname = sharedPreferences.getString(getString(R.string.NICKNAME), DEFAULT);

        if (link.equals(DEFAULT) || nickname.equals(DEFAULT)) {
            Toast.makeText(this, "Keine Daten vorhanden", Toast.LENGTH_LONG).show();
        } else {
            etNickname.setText(nickname);
            etLink.setText(link);
        }

        email = sharedPreferences.getString(getString(R.string.EMAIL), DEFAULT);
        if (!DEFAULT.equals(email)) {
            etEmail.setText(email);
        }

        uploadToken = sharedPreferences.getString(getString(R.string.UPLOAD_TOKEN), DEFAULT);
        if (!DEFAULT.equals(uploadToken)) {
            etUploadToken.setText(uploadToken);
        }

        Intent intent = getIntent();
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
        saveSettings(view);
        new RegisterTask(getString(R.string.rs_api_key)).execute();
    }

    public void saveSettings(View view) {
        SharedPreferences sharedPreferences = MyDataActivity.this.getSharedPreferences(getString(R.string.PREF_FILE), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(getString(R.string.LICENCE), licence);
        editor.putString(getString(R.string.PHOTO_OWNER), photoOwner);
        editor.putString(getString(R.string.LINKING), linking);
        editor.putString(getString(R.string.LINK_TO_PHOTOGRAPHER), etLink.getText().toString());
        editor.putString(getString(R.string.NICKNAME), etNickname.getText().toString());
        editor.putString(getString(R.string.EMAIL), etEmail.getText().toString());
        editor.putString(getString(R.string.UPLOAD_TOKEN), etUploadToken.getText().toString());
        editor.apply();
        Toast.makeText(this, R.string.preferences_saved, Toast.LENGTH_LONG).show();
    }

    public void clearSettings(View viewButtonClear) {
        SharedPreferences sharedPreferences = MyDataActivity.this.getSharedPreferences(getString(R.string.PREF_FILE), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
        Toast.makeText(this, R.string.preferences_cleared, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(MyDataActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    public class RegisterTask extends AsyncTask<Void, String, Integer> {

        private final String apiKey;
        private final JSONObject registrationData;
        private ProgressDialog progressDialog;

        public RegisterTask(String apiKey) {
            this.apiKey = apiKey;
            registrationData = new JSONObject();
            try {
                registrationData.put("nickname", etNickname.getText().toString());
                registrationData.put("email", etEmail.getText().toString());
                registrationData.put("license", licence);
                registrationData.put("photoOwner", "YES".equals(photoOwner));
                registrationData.put("linking", linking);
                registrationData.put("link", etLink.getText().toString());
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
                Toast.makeText(MyDataActivity.this, R.string.registration_completed, Toast.LENGTH_LONG).show();
            } else if (result == 422) {
                    Toast.makeText(MyDataActivity.this, R.string.registration_data_incomplete, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(MyDataActivity.this, getString(R.string.registration_failed, result), Toast.LENGTH_LONG).show();
            }
        }

        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(MyDataActivity.this);
            progressDialog.setIndeterminate(false);

            // show it
            progressDialog.show();
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            progressDialog.setMessage("Sende Daten ... " + values[0]);

        }

    }

}
