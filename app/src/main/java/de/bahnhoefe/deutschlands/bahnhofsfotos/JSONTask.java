package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import de.bahnhoefe.deutschlands.bahnhofsfotos.db.BahnhofsDbAdapter;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Bahnhof;
import de.bahnhoefe.deutschlands.bahnhofsfotos.util.Constants;

import static android.content.Context.MODE_PRIVATE;
import static de.bahnhoefe.deutschlands.bahnhofsfotos.R.id.tvUpdate;
import static java.lang.Integer.parseInt;

/**
 * Created by android_oma on 25.07.16.
 */

public class JSONTask extends AsyncTask<String, String, List<Bahnhof>> {
    private BahnhofsDbAdapter dbAdapter;
    private ProgressDialog progressDialog;
    private MainActivity mainActivity;
    public JSONTask(MainActivity mainActivity) {
        this.mainActivity=mainActivity;
        dbAdapter = new BahnhofsDbAdapter(mainActivity);
        dbAdapter.open();
    }

    @Override
    protected List<Bahnhof> doInBackground(String... params) {
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        Date date = new Date();
        long aktuellesDatum = date.getTime();
        List<Bahnhof> bahnhoefe = new ArrayList<Bahnhof>();

        try {
            URL url = new URL(params[0]);
            connection = (HttpURLConnection)url.openConnection();
            connection.connect();
            InputStream stream = connection.getInputStream();
            reader = new BufferedReader(new InputStreamReader(stream));
            StringBuffer buffer = new StringBuffer();
            String line = "";
            while((line = reader.readLine()) != null){
                buffer.append(line);
            }
            String finalJson =  buffer.toString();

            try {
                JSONArray bahnhofList = new JSONArray(finalJson);

                for (int i = 0; i < bahnhofList.length(); i++){
                    JSONObject jsonObj = (JSONObject) bahnhofList.get(i);
                    publishProgress((i + " von " + bahnhofList.length()));

                    String title = jsonObj.getString(Constants.DB_JSON_CONSTANTS.KEY_TITLE);
                    String id = jsonObj.getString(Constants.DB_JSON_CONSTANTS.KEY_ID);
                    String lat = jsonObj.getString(Constants.DB_JSON_CONSTANTS.KEY_LAT);
                    String lon = jsonObj.getString(Constants.DB_JSON_CONSTANTS.KEY_LON);

                    Bahnhof bahnhof = new Bahnhof();
                    bahnhof.setTitle(title);
                    bahnhof.setId(parseInt(id));
                    bahnhof.setLat(Float.parseFloat(lat));
                    bahnhof.setLon(Float.parseFloat(lon));
                    bahnhof.setDatum(aktuellesDatum);


                    bahnhoefe.add(bahnhof);
                    Log.d("DatenbankInsertOk ...", bahnhof.toString());
                }


                dbAdapter.insertBahnhoefe(bahnhoefe,this);
                publishProgress("Datenbank aktualisiert");
                return bahnhoefe;

            } catch (JSONException e) {
                e.printStackTrace();
            }


        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally{
            if(connection != null){
                connection.disconnect();
            }
            try {
                if(reader != null){
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(List<Bahnhof> result) {
        progressDialog.dismiss();
        writeUpdateDateInFile();
        TextView tvUpdate = (TextView) mainActivity.findViewById(R.id.tvUpdate);
        try {
            tvUpdate.setText("Letzte Aktualisierung am: " + mainActivity.loadUpdateDateFromFile("updatedate.txt") );
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onPreExecute() {
        progressDialog = new ProgressDialog(mainActivity.getApplicationContext());
        progressDialog.setIndeterminate(false);
        //progressDialog.setMessage("Lade Daten ...");


        // show it
        //progressDialog.show();

    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
        progressDialog.setMessage("Lade Daten ..." + values[0]);


        // show it
        progressDialog.show();

    }

    private void writeUpdateDateInFile() {

        try {
            Calendar c = Calendar.getInstance();
            SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
            final String lastUpdateDate = df.format(c.getTime());
            FileOutputStream updateDate = mainActivity.openFileOutput("updatedate.txt",MODE_PRIVATE);
            OutputStreamWriter osw = new OutputStreamWriter(updateDate);
            try {
                osw.write(lastUpdateDate);
                osw.flush();
                osw.close();
                Toast.makeText(mainActivity,"Aktualisierungsdatum gespeichert", Toast.LENGTH_LONG).show();
            } catch (IOException ioe) {
                Log.e(mainActivity.TAG, ioe.toString());
            }
        } catch (FileNotFoundException fnfe) {
            Log.e(mainActivity.TAG,fnfe.toString());
        }

    }

    public void pub(String text){
        publishProgress(text);
    }
}
