package com.map.to_in.cabmapbox;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;


public class DriverMapActivity extends AppCompatActivity implements OnMapReadyCallback, PermissionsListener,
        GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener, GoogleApiClient.ConnectionCallbacks {

    private Button logout, startNavigation, startBtn;
    private MapView mapView;
    private MapboxMap mapboxMap;
    private PermissionsManager permissionsManager;
    private NavigationMapRoute navigationMapRoute;
    private Location myLocation;
    private DirectionsRoute currentRoute;
    private String customerID = "";
    private static final String TAG = "DriverMapActivity";
    private FusedLocationProviderClient mFusedLocationClient;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private String driverId = "";
    private FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.activity_driver_map);
        mapView = (MapView) findViewById(R.id.mapViewDriver);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;
        enableLocation();
        buildGoogleApiClient();
        logout = findViewById(R.id.logout);
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearDatabase();
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
                NavigationLauncherOptions options = NavigationLauncherOptions.builder()
                        .directionsRoute(currentRoute)
                        .shouldSimulateRoute(true)
                        .build();
                NavigationLauncher.startNavigation(DriverMapActivity.this, options);
            }
        });
        startBtn = findViewById(R.id.startDrv);
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getAssignedCustomer();
            }
        });
    }

    private void clearDatabase(){
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("DriverAvailable");
        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(driverId);

        customerID="";
    }


    @SuppressLint("MissingPermission")
    private void enableLocation() {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            LocationComponent locationComponent = mapboxMap.getLocationComponent();
            locationComponent.activateLocationComponent(this);
            locationComponent.setLocationComponentEnabled(true);
            locationComponent.setCameraMode(CameraMode.TRACKING);
            myLocation = locationComponent.getLastKnownLocation();
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    private void getAssignedCustomer(){
//        String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Driver").child(driverId).child("Request").getRef();
        assignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Toast.makeText(getApplicationContext(),"Customer Found.", Toast.LENGTH_LONG).show();
//                    customerID = dataSnapshot.getKey();
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
                    mapboxMap.addMarker(new MarkerOptions().position(pickUpLocation).title("Pickup Location").setIcon(IconFactory.getInstance(DriverMapActivity.this).fromResource(R.drawable.icons8_street_view_32)));
                    getRoute(Point.fromLngLat(myLocation.getLongitude(), myLocation.getLatitude()),
                            Point.fromLngLat(pickUpLocation.getLongitude(), pickUpLocation.getLatitude()));
                    Toast.makeText(getApplicationContext(), "Direction Found, Starting Navigation", Toast.LENGTH_LONG).show();
                    startNavigation.setEnabled(true);
                    startBtn.setText(R.string.start_navigation);
                }else{
                    Toast.makeText(getApplicationContext(),"Customer Not Found.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(getApplicationContext(),"Database Error.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void shareDriverLocation(String ID){
        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Customer").child(ID).child("Request");
        String driverID = user.getUid();
        HashMap<String, Object> dataMap = new HashMap<String, Object>();
        dataMap.put("driverRideID", driverID);
        dataMap.put("destinationLat", myLocation.getLatitude());
        dataMap.put("destinationLng", myLocation.getLongitude());
        driverRef.updateChildren(dataMap);
    }

    private void getRoute(Point origin, Point destination){
        NavigationRoute.builder(this)
            .accessToken(Mapbox.getAccessToken())
            .origin(origin)
            .destination(destination)
            .build()
            .getRoute(new Callback<DirectionsResponse>() {
                @Override
                public void onResponse(Call<DirectionsResponse> call, retrofit2.Response<DirectionsResponse> response) {
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
                        navigationMapRoute = new NavigationMapRoute(null, mapView, mapboxMap);
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
        Toast.makeText(this, R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            enableLocation();
        } else {
            Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @SuppressLint("MissingPermission")
    @Override
    public void onLocationChanged(Location location) {
//        shareDriverLocation(customerID);
        mFusedLocationClient.getLastLocation()
            .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    // Got last known location. In some rare situations this can be null.
                    if (location != null) {
                        // Logic to handle location object
                        myLocation = location;
                        setCameraPosition(location);
                        String userId;
                        try {
                            userId = user.getUid();
                            DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference("DriverAvailable");
                            DatabaseReference refWorking = FirebaseDatabase.getInstance().getReference("DriverWorking");
                            GeoFire geoFireAvailable = new GeoFire(refAvailable);
                            GeoFire geoFireWorking = new GeoFire(refWorking);

                            switch (customerID) {
                                case "":
                                    geoFireWorking.removeLocation(userId);
                                    geoFireAvailable.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));
                                    break;
                                default:
                                    geoFireAvailable.removeLocation(userId);
                                    geoFireWorking.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));
                                    break;
                            }
                            driverId = userId;
                        } catch (Exception t){
//                            Toast.makeText(getApplicationContext(), "User Id Not Found", Toast.LENGTH_LONG).show();
                            finish();
                        }

                    }
                    else{
                        Toast.makeText(getApplicationContext(), "Location Not Found.", Toast.LENGTH_LONG).show();
                    }
                }
            });
    }
    private void setCameraPosition(Location location) {
        mapboxMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(),
                location.getLongitude()), 13.0));
    }

    protected synchronized void buildGoogleApiClient(){
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onStart() {
        super.onStart();
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
        mapView.onStop();
        clearDatabase();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}
