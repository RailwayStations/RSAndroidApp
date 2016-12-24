package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import static android.R.attr.id;

public class MyDataActivity extends AppCompatActivity {
    private static final String DEFAULT = "N/A";
    private final String TAG = getClass().getSimpleName();
    EditText etNickname,etLink;
    RadioGroup rgLicence, rgPhotoOwner, rgLinking;
    RadioButton rbLinkingXing, rbLinkingTwitter, rbLinkingSnapchat, rbLinkingInstagram, rbLinkingWebpage, rbLinkingNo;
    RadioButton rbLicenceCC0, rbLicenceCC4;
    RadioButton rbPhotoOwnerYes, rbPhotoOwnerNo;
    String licence, photoOwner, nickname, link, linking;
    Button btCommit, btClear;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mydata);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        etNickname = (EditText) findViewById(R.id.etNickname);
        etLink = (EditText) findViewById(R.id.etLinking);
        btCommit = (Button) findViewById(R.id.bt_mydata_commit);
        btClear = (Button) findViewById(R.id.bt_mydata_clear);

        rgLicence = (RadioGroup) findViewById(R.id.rgLicence);
        rgPhotoOwner = (RadioGroup) findViewById(R.id.rgOwnPhoto);
        rgLinking = (RadioGroup) findViewById(R.id.rgLinking);

        rbLinkingXing = (RadioButton)findViewById(R.id.rbLinkingXing);
        rbLinkingTwitter = (RadioButton)findViewById(R.id.rbLinkingTwitter);
        rbLinkingSnapchat = (RadioButton)findViewById(R.id.rbLinkingSnapchat);
        rbLinkingInstagram = (RadioButton)findViewById(R.id.rbLinkingInstagram);
        rbLinkingWebpage = (RadioButton)findViewById(R.id.rbLinkingWebpage);
        rbLinkingNo = (RadioButton)findViewById(R.id.rbLinkingNo);

        rbLicenceCC0 = (RadioButton)findViewById(R.id.rbCC0);
        rbLicenceCC4 = (RadioButton)findViewById(R.id.rbCC40);

        rbPhotoOwnerNo = (RadioButton)findViewById(R.id.rbOwnPhotoNo);
        rbPhotoOwnerYes = (RadioButton)findViewById(R.id.rbOwnPhotoYes);



        SharedPreferences sharedPreferences = MyDataActivity.this.getSharedPreferences(getString(R.string.PREF_FILE),Context.MODE_PRIVATE);

        licence = sharedPreferences.getString(getString(R.string.LICENCE),DEFAULT);
        if(licence.equals("CC0")){
            rgLicence.check(R.id.rbCC0);
        }else if(licence.equals("CC4")){
            rgLicence.check(R.id.rbCC40);
        }
        photoOwner = sharedPreferences.getString(getString(R.string.PHOTO_OWNER),DEFAULT);
        if(photoOwner.equals("YES")){
            rgPhotoOwner.check(R.id.rbOwnPhotoYes);
        }else if(photoOwner.equals("NO")){
            rgPhotoOwner.check(R.id.rbOwnPhotoNo);
        }
        linking = sharedPreferences.getString(getString(R.string.LINKING),DEFAULT);
        if(linking.equals("XING")){
            rgLinking.check(R.id.rbLinkingXing);
        }else if(linking.equals("SNAPCHAT")){
            rgLinking.check(R.id.rbLinkingSnapchat);
        }else if(linking.equals("TWITTER")){
            rgLinking.check(R.id.rbLinkingTwitter);
        }else if(linking.equals("WEBPAGE")){
            rgLinking.check(R.id.rbLinkingWebpage);
        }else if(linking.equals("INSTAGRAM")){
            rgLinking.check(R.id.rbLinkingInstagram);
        }else if(linking.equals("NO")){
            rgLinking.check(R.id.rbLinkingNo);
        }

        link = sharedPreferences.getString(getString(R.string.LINK_TO_PHOTOGRAPHER),DEFAULT);
        nickname = sharedPreferences.getString(getString(R.string.NICKNAME),DEFAULT);



        if(link.equals(DEFAULT) || nickname.equals(DEFAULT)){
            Toast.makeText(this,"Keine Daten vorhanden",Toast.LENGTH_LONG).show();
        }else{
            etNickname.setText(nickname);
            etLink.setText(link);
        }
    }

    public void selectLicence(View view){
        switch (view.getId()){
            case R.id.rbCC0:
                licence = "CC0";
                break;
            case R.id.rbCC40:
                licence = "CC4";
                break;
        }

    }

    public void selectPhotoOwner(View view){

        switch (view.getId()){
            case R.id.rbOwnPhotoYes:
                photoOwner = "YES";
                break;
            case R.id.rbOwnPhotoNo:
                photoOwner = "NO";
                break;
        }

    }

    public void linkToPhotographer(View view){

        switch (view.getId()){
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

    public void saveSettings(View view){
        SharedPreferences sharedPreferences = MyDataActivity.this.getSharedPreferences(getString(R.string.PREF_FILE),Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(getString(R.string.LICENCE),licence);
        editor.putString(getString(R.string.PHOTO_OWNER),photoOwner);
        editor.putString(getString(R.string.LINKING),linking);
        editor.putString(getString(R.string.LINK_TO_PHOTOGRAPHER),etLink.getText().toString());
        editor.putString(getString(R.string.NICKNAME),etNickname.getText().toString());
        editor.commit();
        Toast.makeText(this,R.string.preferences_saved,Toast.LENGTH_LONG).show();

    }

    public void clearSettings(View viewButtonClear){

        SharedPreferences sharedPreferences = MyDataActivity.this.getSharedPreferences(getString(R.string.PREF_FILE), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.commit();
        Toast.makeText(this,R.string.preferences_cleared,Toast.LENGTH_LONG).show();
    }

}
