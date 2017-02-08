package de.bahnhoefe.deutschlands.bahnhofsfotos;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;

import de.bahnhoefe.deutschlands.bahnhofsfotos.db.BahnhofsDbAdapter;
import de.bahnhoefe.deutschlands.bahnhofsfotos.model.Bahnhof;

public class MapsAllAcitivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.InfoWindowAdapter, GoogleMap.OnInfoWindowClickListener {

    private GoogleMap mMap;
    private static final String TAG = MapsAllAcitivity.class.getSimpleName();

    private List<Bahnhof> bahnhofMarker;
    private LatLng myPos;
    private BahnhofsDbAdapter dbAdapter;
    private static final String DEFAULT = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_maps_acitivty);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        BaseApplication baseApplication = (BaseApplication) getApplication();
        dbAdapter = baseApplication.getDbAdapter();

        bahnhofMarker = dbAdapter.getAllBahnhoefe(false);
        myPos = new LatLng(51d, 10d);

    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.clear();
        addAllMarkers(bahnhofMarker);

    }

    private void addAllMarkers(List<Bahnhof> bahnhofMarker )
    {

        for(int i=0; i< bahnhofMarker.size();i++){
            LatLng bahnhofPos = new LatLng(bahnhofMarker.get(i).getLat(), bahnhofMarker.get(i).getLon());
            mMap.addMarker(new MarkerOptions()
                    .title(bahnhofMarker.get(i).getTitle())
                    .position(bahnhofPos)
                    .snippet(String.valueOf(bahnhofMarker.get(i).getId()))
                    .icon(BitmapDescriptorFactory.defaultMarker(343)));
            mMap.setInfoWindowAdapter(this);
            mMap.setOnInfoWindowClickListener(this);

        }

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myPos, 7));

    }

    @Override
    public View getInfoWindow(Marker marker) {
        return null;
    }


    @Override
    public View getInfoContents(Marker marker) {

        LayoutInflater layoutInflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.info_window,null,false);
        ((TextView)view.findViewById(R.id.tvbahnhofname)).setText(marker.getTitle());
        if(marker.getSnippet() != null){
            ((TextView)view.findViewById(R.id.tvbahnhofnr)).setText("BahnhofNr: " + marker.getSnippet());
        }else{
            ((TextView)view.findViewById(R.id.tvbahnhofnr)).setText(" ");
        }

        ((TextView)view.findViewById(R.id.tvlatlon)).setText(marker.getPosition().toString());
        return view;
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        BaseApplication baseApplication = (BaseApplication)getApplication();
        String countryShortCode = baseApplication.getCountryShortCode();

        Class cls = DetailsActivity.class;
        Intent intent = new Intent(MapsAllAcitivity.this, cls);
        long id = Long.valueOf(marker.getSnippet());
        Bahnhof bahnhof = dbAdapter.fetchBahnhofByBahnhofId(id);
        intent.putExtra(DetailsActivity.EXTRA_BAHNHOF, bahnhof);
        startActivity(intent);
    }



}

