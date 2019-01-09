package com.map.to_in.cabmapbox;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineListener;
import com.mapbox.android.core.location.LocationEnginePriority;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class CustomerMapActivity extends AppCompatActivity implements OnMapReadyCallback, LocationEngineListener, PermissionsListener {

    private Button logout, requestBtn;
    private MapView mapView;
    private MapboxMap map;
    private PermissionsManager permissionsManager;
    private LocationEngine locationEngine;
    private LocationLayerPlugin locationLayerPlugin;
    private Location myLocation;
    private LatLng pickupLocation;
    private LatLng destinationLatLng = new LatLng(0.0,0.0);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.activity_customer_map);
        mapView = (MapView) findViewById(R.id.mapViewCustomer);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        logout = findViewById(R.id.logout);
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(CustomerMapActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });

        requestBtn = findViewById(R.id.request);
        requestBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userID = FirebaseAuth.getInstance().getCurrentUser().getUid(); //A small change in order for bug fixing, as app crashed as i logout

                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("CustomerRequest");
                GeoFire geoFire = new GeoFire(ref);
                geoFire.setLocation(userID, new GeoLocation(myLocation.getLatitude(), myLocation.getLongitude()));

                pickupLocation = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
                map.addMarker(new MarkerOptions().position(pickupLocation).title("Pickup Here"));
                destinationLatLng = pickupLocation;
                requestBtn.setText(getString(R.string.getting_your_driver));

                getClosestDriver();
            }
        });
    }

    private int radius = 1;
    private Boolean driverFound = false;
    private String driverFoundID;

    private void getClosestDriver(){
        DatabaseReference driverLocation = FirebaseDatabase.getInstance().getReference().child("DriverAvailable");

        GeoFire geoFire = new GeoFire(driverLocation);

        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(pickupLocation.getLatitude(), pickupLocation.getLongitude()), radius);
        geoQuery.removeAllListeners();  //Removes all data stored

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if(!driverFound){
                    driverFound = true;
                    driverFoundID = key;
                    Log.d("Key : ", driverFoundID);

                    DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Driver").child(driverFoundID);
                    String customerID = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    HashMap<String, Object> dataMap = new HashMap<String, Object>();
                    dataMap.put("CustomerRideID", customerID);
                    dataMap.put("destinationLat", destinationLatLng.getLatitude());
                    dataMap.put("destinationLng", destinationLatLng.getLongitude());
                    driverRef.updateChildren(dataMap);
                    Toast.makeText(getApplicationContext(),"Driver Found, Wait.", Toast.LENGTH_LONG).show();
                    requestBtn.setText(getString(R.string.DriverSearch));
                    getDriverLocation();
                }
            }

            @Override
            public void onKeyExited(String key) {
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if(!driverFound){
                    radius++;
                    getClosestDriver();
                    Log.d("Radius : ", String.valueOf(radius));
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    private void getDriverLocation(){
        requestBtn.setText(getString(R.string.DriverSearch));
        DatabaseReference driverLocationRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Customer").child(driverFoundID);
        driverLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {

                    Map<String, Object> dataMap = (Map<String, Object>) dataSnapshot.getValue();
                    double locationLat = 25.625818;
                    double locationLng = 85.106596;

                    if (dataMap.get("destinationLat") != null && dataMap.get("destinationLng") != null) {
                        String customerID = dataMap.get("DriverRideID").toString();
                        locationLat = Double.parseDouble(dataMap.get("destinationLat").toString());
                        locationLng = Double.parseDouble(dataMap.get("destinationLng").toString());
                        Toast.makeText(getApplicationContext(), "Destination to Driver Found, Driver Approaching Here.", Toast.LENGTH_LONG).show();
                        requestBtn.setText(getString(R.string.driverFound));
                    } else {
                        Toast.makeText(getApplicationContext(), "Destination to Driver Not Found.", Toast.LENGTH_LONG).show();
                        requestBtn.setText(getString(R.string.noDirection));
                    }
                    LatLng driverLatLng = new LatLng(locationLat, locationLng);
                    map.addMarker(new MarkerOptions().position(driverLatLng).title("Your Driver"));
//                    getRouteToMarker(driverLatLng);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        map = mapboxMap;
        enableLocation();
    }

    private void enableLocation() {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            initializeLocationEngine();
            initializeLocationLayer();
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    @SuppressLint("MissingPermission")
    private void initializeLocationEngine() {
        locationEngine = new LocationEngineProvider(this).obtainBestLocationEngineAvailable();
        locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
        locationEngine.activate();

        Location lastLocation = locationEngine.getLastLocation();
        if(lastLocation != null){
            myLocation = lastLocation;
            setCameraPosition(myLocation);
        } else {
            locationEngine.addLocationEngineListener(this);
        }
    }

    @SuppressLint("WrongConstant")
    private void initializeLocationLayer(){
        locationLayerPlugin = new LocationLayerPlugin(mapView, map, locationEngine);
        locationLayerPlugin.setLocationLayerEnabled(true);
        locationLayerPlugin.setCameraMode(CameraMode.TRACKING);
        locationLayerPlugin.setRenderMode(RenderMode.NORMAL);
    }

    private void setCameraPosition(Location location){
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(),
                location.getLongitude()), 13.0));
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onConnected() {
        locationEngine.requestLocationUpdates();
        locationEngine.setInterval(1000);
        locationEngine.setFastestInterval(1000);
        locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY); //high Accuracy decrease if needed
    }

    @Override
    public void onLocationChanged(Location location) {
        if(location != null){
            myLocation = location;
            setCameraPosition(location);
        }
    }

    @Override // When User Denies the Permissions
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(getApplicationContext(),"Without these permissions, you Wont be able to use location Services.", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if(granted){
            enableLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onStart() {
        super.onStart();
        if(locationEngine != null){
            locationEngine.removeLocationUpdates();
        }
        if(locationLayerPlugin != null){
            locationLayerPlugin.onStart();
        }
        mapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        if(locationEngine != null){
            locationEngine.removeLocationUpdates();
        } if(locationLayerPlugin != null){
            locationLayerPlugin.onStop();
        }
        mapView.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(locationEngine != null){
            locationEngine.deactivate();
        }
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}
