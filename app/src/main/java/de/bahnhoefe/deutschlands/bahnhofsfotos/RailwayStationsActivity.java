package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.webkit.WebView;
import android.webkit.WebViewClient;

public class RailwayStationsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_railway_stations);

        WebView myWebView = (WebView) findViewById(R.id.webView1);
        myWebView.setWebViewClient(new MyWebViewClient());

        String url = "https://railway-stations.org";
        myWebView.getSettings().setJavaScriptEnabled(true);
        myWebView.loadUrl(url);
    }

    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (Uri.parse(url).getHost().equals("railway-stations.org")) {
                // This is my web site, so do not override; let my WebView load the page
                return false;
            }
            // Otherwise, the link is not for a page on my site, so launch another Activity that handles URLs
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
            return true;
        }
    }

}
