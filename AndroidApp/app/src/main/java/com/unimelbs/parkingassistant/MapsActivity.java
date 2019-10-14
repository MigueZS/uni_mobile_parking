package com.unimelbs.parkingassistant;

import androidx.fragment.app.FragmentActivity;

import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.common.api.Status;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.libraries.places.api.model.Place;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import com.unimelbs.parkingassistant.model.Bay;
import com.unimelbs.parkingassistant.model.DataFeed;
import com.unimelbs.parkingassistant.model.ExtendedClusterManager;
import com.unimelbs.parkingassistant.util.*;

public class MapsActivity extends FragmentActivity
        implements OnMapReadyCallback,
        ClusterManager.OnClusterItemClickListener<Bay>
{

    private GoogleMap mMap;
    private static final String TAG = "MapActivity";
    private static String apiKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //todo: Add check if data exists
        setContentView(R.layout.activity_maps);


        initializeGoogleMapsPlacesApis();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        PermissionManager.reqPermission(this,this);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        activateAutoCompleteFragment();



    }

    private void activateAutoCompleteFragment(){

        List<Place.Field> placeFields = Arrays.asList(Place.Field.ID, Place.Field.NAME,Place.Field.LAT_LNG);

        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);



        try {
            autocompleteFragment.setPlaceFields(placeFields);
        } catch (NullPointerException e) {
            Log.d(TAG, "Null Pointer Exception while setting Place Fields to Retrieve");
            e.printStackTrace();
        }

        // Set up a PlaceSelectionListener to handle the response.
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {

                String placeName = place.getName();
                LatLng placeLatLng = place.getLatLng();
                mMap.addMarker(new MarkerOptions().position(placeLatLng).title(placeName));
                mMap.moveCamera(CameraUpdateFactory.newLatLng(placeLatLng));
                mMap.moveCamera(CameraUpdateFactory.zoomTo(18));
                Log.i(TAG, "Place: " + placeName + ", " + place.getId()+ ", " + placeLatLng);
            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                Log.i(TAG, "An error occurred: " + status);
            }
        });

    }

    private void initializeGoogleMapsPlacesApis(){
        MapsActivity.apiKey = getResources().getString(R.string.google_maps_key);
        // Initialize the Places SDK
        Places.initialize(getApplicationContext(), apiKey);
        // Create a new Places client instance
        PlacesClient placesClient = Places.createClient(this);
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
        Log.d(TAG, "onMapReady: ");
        DataFeed data = new DataFeed(this, getApplicationContext());
        //UserSession userSession = new UserSession(getApplicationContext());


        //LatLng zoomPoint = new LatLng(userSession.getCurrentLocation().getLatitude(),userSession.getCurrentLocation().getLongitude());
        //Log.d(TAG, "onMapReady: "+zoomPoint.latitude+","+zoomPoint.longitude);

        data.fetchBays();
        ExtendedClusterManager<Bay> extendedClusterManager = new ExtendedClusterManager<>(this,mMap,data);

        mMap.setOnCameraIdleListener(extendedClusterManager);
        mMap.setOnMarkerClickListener(extendedClusterManager);
        extendedClusterManager.addItems(data.getItems());
        //extendedClusterManager.setOnClusterItemClickListener(this);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(data.getItems().get(0).getPosition(),15));






    }

    @Override
    public boolean onClusterItemClick(Bay bay) {

        Log.d(TAG, "onClusterItemClick: bay clicked:"+bay.getBayId());
        return false;
    }
}
