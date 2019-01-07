package com.map.to_in.cabmapbox;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
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
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
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
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DriverMapActivity extends AppCompatActivity implements OnMapReadyCallback, LocationEngineListener,
        PermissionsListener, LocationListener {

    private Button logout, startNavigation;
    private MapView mapView;
    private MapboxMap map;
    private PermissionsManager permissionsManager;
    private LocationEngine locationEngine;
    private LocationLayerPlugin locationLayerPlugin;
    private NavigationMapRoute navigationMapRoute;
    private Location myLocation;
    private DirectionsRoute currentRoute;
    private String customerID;
    private static final String TAG = "DriverMapActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.activity_driver_map);
        mapView = (MapView) findViewById(R.id.mapViewDriver);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        logout = findViewById(R.id.logout);
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(DriverMapActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });

        startNavigation = findViewById(R.id.startnav);
        startNavigation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getAssignedCustomer();
            }
        });

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    private void shareDriverLocation(String customerID){
        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Customer").child(customerID);
        String driverID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        HashMap map = new HashMap<String, String>();
        map.put("DriverRideID", driverID);
        map.put("destinationLat", myLocation.getLatitude());
        map.put("destinationLng", myLocation.getLongitude());
        driverRef.updateChildren(map);
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
        if(getApplicationContext() != null){

//            myLocation = location;
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
//            map.moveCamera(CameraUpdateFactory.newLatLng(latLng));
//            map.animateCamera(CameraUpdateFactory.zoomTo(14));

            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference("DriverAvailable");
            DatabaseReference refWorking = FirebaseDatabase.getInstance().getReference("DriverWorking");
            GeoFire geoFireAvailable = new GeoFire(refAvailable);
            GeoFire geoFireWorking = new GeoFire(refWorking);

            switch (customerID){
                case "":
                    geoFireWorking.removeLocation(userId);
                    geoFireAvailable.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));
                    break;
                default:
                    geoFireAvailable.removeLocation(userId);
                    geoFireWorking.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));
                    break;
            }
        }
    }
    private void getAssignedCustomer(){
        String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Driver").child(driverId);
        assignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Toast.makeText(getApplicationContext(),"Customer Found.", Toast.LENGTH_LONG).show();
//                    customerID = dataSnapshot.getValue().toString();
//                    getAssignedCustomerPickupLocation();

                    Map<String, Object> dataMap = (Map<String, Object>) dataSnapshot.getValue();
                    double locationLat = 25.625818;
                    double locationLng = 85.106596;

                    if (dataMap.get("destinationLat") != null && dataMap.get("destinationLng") != null) {
                        String customerID = dataMap.get("CustomerRideID").toString();
                        locationLat = Double.parseDouble(dataMap.get("destinationLat").toString());
                        locationLng = Double.parseDouble(dataMap.get("destinationLng").toString());
                        Toast.makeText(getApplicationContext(),"Destination to Customer Found, Directing you there.", Toast.LENGTH_LONG).show();
                        shareDriverLocation(customerID);
                    }else{
                        Toast.makeText(getApplicationContext(),"Destination to Customer Not Found.", Toast.LENGTH_LONG).show();
                    }
                    LatLng pickUpLocation = new LatLng(locationLat, locationLng);
                    map.addMarker(new MarkerOptions().position(pickUpLocation).title("Pickup Location"));
                    getRoute(Point.fromLngLat(myLocation.getLongitude(), myLocation.getLatitude()),
                            Point.fromLngLat(pickUpLocation.getLongitude(), pickUpLocation.getLatitude()));
                }else{
                    Toast.makeText(getApplicationContext(),"Customer Not Found.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void getRoute(Point origin, Point destination){
        NavigationRoute.builder(this)
                .accessToken(Mapbox.getAccessToken())
                .origin(origin)
                .destination(destination)
                .build()
                .getRoute(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                        if(response.body() == null) {
                            Log.e(TAG, "No Routes found, chek right user and access token");
                            return;
                        }else if(response.body().routes().size() == 0){
                            Log.e(TAG, "No Routes Found");
                            return;
                        }
                        currentRoute = response.body().routes().get(0);
                        if(navigationMapRoute != null){
                            navigationMapRoute.removeRoute();
                        }else {
                            navigationMapRoute = new NavigationMapRoute(null, mapView, map);
                        }
                        navigationMapRoute.addRoute(currentRoute);
                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                        Log.e(TAG, "Error:" + t.getMessage());
                    }
                });
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
