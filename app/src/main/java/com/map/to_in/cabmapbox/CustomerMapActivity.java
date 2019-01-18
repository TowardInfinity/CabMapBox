package com.map.to_in.cabmapbox;

import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.firebase.geofire.LocationCallback;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.tapadoo.alerter.Alerter;

import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CustomerMapActivity extends AppCompatActivity implements OnMapReadyCallback, PermissionsListener,
        GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener, GoogleApiClient.ConnectionCallbacks {

    private Button logout, requestBtn, startbtn;
    private MapView mapView;
    private MapboxMap mapboxMap;
    private PermissionsManager permissionsManager;
    private Location myLocation;
    private static final String TAG = "CustomerMapActivity";
    private FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

    private FusedLocationProviderClient mFusedLocationClient;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private String customerID = "";

    private Marker marker;
    private int radius = 1;
    private Boolean driverFound = false;
    private String driverFoundID;

    private NavigationMapRoute navigationMapRoute;
    private DirectionsRoute currentRoute;
    private boolean state = true;

    private LatLng origincord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.activity_customer_map);
        mapView = (MapView) findViewById(R.id.mapViewCustomer);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        CustomerMapActivity.this.mapboxMap = mapboxMap;
        enableLocation();
        buildGoogleApiClient();
        startbtn = findViewById(R.id.startCus);
        startbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                originCoord = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
                CustomerMapActivity.this.mapboxMap.addMarker(new MarkerOptions().
                        position(new LatLng(myLocation.getLatitude(), myLocation.getLongitude()))
                        .title("Pickup Here").setIcon(IconFactory.getInstance(CustomerMapActivity.this)
                                .fromResource(R.drawable.icons8_street_view_32)));
                requestBtn.setText(getString(R.string.getting_your_driver));

                getClosestDriver();
            }
        });
        logout = findViewById(R.id.logout);
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                state = false;
                clearDatabase();
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

            }
        });
    }

    private void clearDatabase(){
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("CustomerRequest");
        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(customerID);
        customerID = "";
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

    @Override // When User Denies the Permissions
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if(granted){
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
    private void getClosestDriver(){
        DatabaseReference driverLocation = FirebaseDatabase.getInstance().getReference().child("DriverAvailable");
        GeoFire geoFire = new GeoFire(driverLocation);

        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(myLocation.getLatitude(), myLocation.getLongitude()), radius);
        geoQuery.removeAllListeners();  //Removes all data stored

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if(!driverFound){
                    driverFound = true;
                    driverFoundID = key;
                    Log.d("Key : ", driverFoundID);

                    DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference()
                            .child("Users").child("Driver").child(driverFoundID).child("Request");
                    customerID = user.getUid();
                    HashMap<String, Object> dataMap = new HashMap<String, Object>( );
                    dataMap.put("CustomerRideID", customerID);
                    dataMap.put("destinationLat", myLocation.getLatitude());
                    dataMap.put("destinationLng", myLocation.getLongitude());
                    driverRef.updateChildren(dataMap);
//                    Toast.makeText(getApplicationContext(),"Driver Found, Wait.", Toast.LENGTH_LONG).show();
                    Alerter.create(CustomerMapActivity.this)
                            .setTitle("Driver Found ")
                            .setText(" Driver Reaching here. ")
                            .enableProgress(false)
                            .setBackgroundColorInt(R.color.mapbox_blue)
                            .enableSwipeToDismiss()
                            .setIcon(R.drawable.alert_progress_drawable)
                            .setIconColorFilter(0)
                            .show();
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
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("DriverAvailable");
        GeoFire geoFire = new GeoFire(ref);

        geoFire.getLocation(driverFoundID, new LocationCallback() {
            @Override
            public void onLocationResult(String key, GeoLocation location) {
                if (location != null) {
                    double lat = location.latitude;
                    double lng = location.longitude;
                    LatLng driverLatLng = new LatLng(lat, lng);
                    if (marker != null) {
                        mapboxMap.removeMarker(marker);
                    }
                    marker = mapboxMap.addMarker(new MarkerOptions().position(driverLatLng)
                            .title("Your Driver").setIcon(IconFactory.getInstance(CustomerMapActivity.this)
                                    .fromResource(R.drawable.icons8_car_top_view_32)));
                    getRoute(Point.fromLngLat(myLocation.getLongitude(), myLocation.getLatitude()),
                            Point.fromLngLat(driverLatLng.getLongitude(), driverLatLng.getLatitude()));
                    animate(driverLatLng);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(getApplicationContext(),"There was an error getting the GeoFire location: "
                        + databaseError, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void getRoute(Point origin, Point destination) {
        NavigationRoute.builder(this)
                .accessToken(Mapbox.getAccessToken())
                .origin(origin)
                .destination(destination)
//                .overview(DirectionsCriteria.OVERVIEW_FULL)
                .profile(DirectionsCriteria.PROFILE_DRIVING)
                .build()
                .getRoute(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                        // You can get the generic HTTP info about the response
                        Log.d(TAG, "Response code: " + response.code());
                        if (response.body() == null) {
                            Log.e(TAG, "No routes found, make sure you set the right user and access token.");
                            return;
                        } else if (response.body().routes().size() < 1) {
                            Log.e(TAG, "No routes found");
                            return;
                        }

                        currentRoute = response.body().routes().get(0);

                        String text = "Distance: " + Math.round(((currentRoute.distance())/1000.0) * 100.0)/100.0
                                + " Kilometre Duration: " + Math.round(((currentRoute.duration())/60.0)*100.0)/100.0 + " Minutes";
                        Alerter.create(CustomerMapActivity.this)
                                .setTitle("Driver Approaching")
                                .setText(text)
                                .enableProgress(true)
                                .setBackgroundColorInt(R.color.mapbox_blue)
                                .setDuration(8000)
                                .enableSwipeToDismiss()
                                .setIcon(R.drawable.alert_progress_drawable)
                                .setIconColorFilter(0)
                                .show();

                        // Draw the route on the map
                        if (navigationMapRoute != null) {
                            navigationMapRoute.removeRoute();
                        } else {
                            navigationMapRoute = new NavigationMapRoute(null, mapView, mapboxMap, R.style.NavigationMapRoute);
                        }
                        navigationMapRoute.addRoute(currentRoute);
                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                        Log.e(TAG, "Error: " + t.getMessage());
                    }
                });
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY); //high Accuracy decrease if needed
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
        if(state) {
            if (driverFoundID != null) {
                getDriverLocation();
            }
            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                myLocation = location;
                                origincord = new LatLng(location.getLatitude(), location.getLongitude());
                                setCameraPosition(location);
                                try {
                                    String userID = user.getUid();
                                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("CustomerRequest");
                                    GeoFire geoFire = new GeoFire(ref);
                                    geoFire.setLocation(userID, new GeoLocation(location.getLatitude(), location.getLongitude()));
                                } catch (Exception te) {
                                    finish();
                                }
                            }
                        }
                    });
        }
    }

    private void animate(LatLng destLatLng){
        double[] startValues = new double[]{marker.getPosition().getLatitude(), marker.getPosition().getLongitude()};
        double[] endValues = new double[]{destLatLng.getLatitude(), destLatLng.getLongitude()};
        ValueAnimator latLngAnimator = ValueAnimator.ofObject(new DoubleArrayEvaluator(), startValues, endValues);
        latLngAnimator.setDuration(600);
        latLngAnimator.setInterpolator(new DecelerateInterpolator());
        latLngAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                double[] animatedValue = (double[]) animation.getAnimatedValue();
                marker.setPosition(new LatLng(animatedValue[0], animatedValue[1]));
            }
        });
        latLngAnimator.start();
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
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        clearDatabase();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    public class DoubleArrayEvaluator implements TypeEvaluator<double[]> {

        private double[] mArray;

        /**
         * Create a DoubleArrayEvaluator that does not reuse the animated value. Care must be taken
         * when using this option because on every evaluation a new <code>double[]</code> will be
         * allocated.
         *
         * @see #DoubleArrayEvaluator(double[])
         */
        public DoubleArrayEvaluator() {
        }

        /**
         * Create a DoubleArrayEvaluator that reuses <code>reuseArray</code> for every evaluate() call.
         * Caution must be taken to ensure that the value returned from
         * {@link android.animation.ValueAnimator#getAnimatedValue()} is not cached, modified, or
         * used across threads. The value will be modified on each <code>evaluate()</code> call.
         *
         * @param reuseArray The array to modify and return from <code>evaluate</code>.
         */
        public DoubleArrayEvaluator(double[] reuseArray) {
            mArray = reuseArray;
        }

        /**
         * Interpolates the value at each index by the fraction. If
         * {@link #DoubleArrayEvaluator(double[])} was used to construct this object,
         * <code>reuseArray</code> will be returned, otherwise a new <code>double[]</code>
         * will be returned.
         *
         * @param fraction   The fraction from the starting to the ending values
         * @param startValue The start value.
         * @param endValue   The end value.
         * @return A <code>double[]</code> where each element is an interpolation between
         * the same index in startValue and endValue.
         */
        @Override
        public double[] evaluate(float fraction, double[] startValue, double[] endValue) {
            double[] array = mArray;
            if (array == null) {
                array = new double[startValue.length];
            }

            for (int i = 0; i < array.length; i++) {
                double start = startValue[i];
                double end = endValue[i];
                array[i] = start + (fraction * (end - start));
            }
            return array;
        }
    }
}