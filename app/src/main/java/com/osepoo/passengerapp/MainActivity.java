package com.osepoo.passengerapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.*;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.*;
import com.osepoo.passengerapp.collection.MarkerCollection;
import com.osepoo.passengerapp.helpers.FirebaseEventListenerHelper;
import com.osepoo.passengerapp.helpers.GoogleMapHelper;
import com.osepoo.passengerapp.helpers.MarkerAnimationHelper;
import com.osepoo.passengerapp.helpers.UiHelper;
import com.osepoo.passengerapp.interfaces.FirebaseDriverListener;
import com.osepoo.passengerapp.interfaces.LatLngInterpolator;
import com.osepoo.passengerapp.model.Driver;

public class MainActivity extends AppCompatActivity implements FirebaseDriverListener, GoogleMap.OnMarkerClickListener {

    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 2161;
    private static final String ONLINE_DRIVERS = "UsersData";

    private final GoogleMapHelper googleMapHelper = new GoogleMapHelper();
    private DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child(ONLINE_DRIVERS);

    private GoogleMap googleMap;
    private LocationRequest locationRequest;
    private UiHelper uiHelper;
    private FirebaseEventListenerHelper firebaseEventListenerHelper;
    private FusedLocationProviderClient locationProviderClient;

    private Marker currentLocationMarker;

    private TextView totalOnlineDrivers;

    private boolean locationFlag = true;

    private LocationCallback locationCallback = new LocationCallback() {

        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
            Location location = locationResult.getLastLocation();
            if (location == null) return;
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            if (locationFlag) {
                locationFlag = false;
                animateCamera(latLng);
            }
            showOrAnimateUserMarker(latLng);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnBack = findViewById(R.id.btnBack);
        Button btnSensorValues = findViewById(R.id.btnSensorValues);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create an intent to navigate back to the EntryActivity
                Intent intent = new Intent(MainActivity.this, EntryActivity.class);
                startActivity(intent);
                finish(); // Optional: Finish the current activity
            }
        });

         btnSensorValues.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create an intent to navigate to the SensorValuesActivity
                Intent intent = new Intent(MainActivity.this, SensorGraphActivity.class);
                startActivity(intent);
            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.supportMap);
        uiHelper = new UiHelper(this);
        assert mapFragment != null;
        mapFragment.getMapAsync(googleMap -> {
            this.googleMap = googleMap;

            // Set a marker click listener on the GoogleMap
            googleMap.setOnMarkerClickListener(this);
        });

        locationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = uiHelper.getLocationRequest();
        if (!uiHelper.isPlayServicesAvailable()) {
            Toast.makeText(this, "Play Services did not install!", Toast.LENGTH_SHORT).show();
            finish();
        } else requestLocationUpdates();
        totalOnlineDrivers = findViewById(R.id.totalOnlineDrivers);
        firebaseEventListenerHelper = new FirebaseEventListenerHelper(this);
        databaseReference.addChildEventListener(firebaseEventListenerHelper);
    }

    private void animateCamera(LatLng latLng) {
        CameraUpdate cameraUpdate = googleMapHelper.buildCameraUpdate(latLng);
        googleMap.animateCamera(cameraUpdate);
    }

    private void showOrAnimateUserMarker(LatLng latLng) {
        if (currentLocationMarker == null)
            currentLocationMarker = googleMap.addMarker(googleMapHelper.getCurrentLocationMarker(latLng));
        else
            MarkerAnimationHelper.animateMarkerToGB(
                    currentLocationMarker,
                    new LatLng(latLng.latitude, latLng.longitude),
                    new LatLngInterpolator.Spherical());
    }

    @SuppressLint("MissingPermission")
    private void requestLocationUpdates() {
        if (!uiHelper.isHaveLocationPermission()) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            return;
        }
        if (uiHelper.isLocationProviderEnabled())
            uiHelper.showPositiveDialogWithListener(this, getResources().getString(R.string.need_location), getResources().getString(R.string.location_content), () -> startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)), "Turn On", false);
        locationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {
            int value = grantResults[0];
            if (value == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, "Location Permission denied", Toast.LENGTH_SHORT).show();
                finish();
            } else if (value == PackageManager.PERMISSION_GRANTED) requestLocationUpdates();
        }
    }

    @Override
    public void onDriverAdded(Driver driver) {
        double latitude = driver.getLat();
        double longitude = driver.getLng();

        if (latitude == 0.0 || longitude == 0.0) {
            // Latitude and/or longitude are not available (GPS not locked)
            Toast.makeText(this, "GPS not locked for Cow " + driver.getDriverId(), Toast.LENGTH_SHORT).show();
        } else {
            // Latitude and longitude are available
            LatLng driverLatLng = new LatLng(latitude, longitude);
            MarkerOptions markerOptions = googleMapHelper.getDriverMarkerOptions(driverLatLng);
            Marker marker = googleMap.addMarker(markerOptions);
            marker.setTag(driver.getDriverId());
            MarkerCollection.insertMarker(marker);
            totalOnlineDrivers.setText(getResources()
                    .getString(R.string.total_online_drivers)
                    .concat(" ")
                    .concat(String.valueOf(MarkerCollection.allMarkers().size())));
            ImageView warningIcon = findViewById(R.id.warningIcon);
            if (MarkerCollection.allMarkers().size() == 0) {
                warningIcon.setVisibility(View.VISIBLE); // Show warning icon
            } else {
                warningIcon.setVisibility(View.GONE);
                Log.d("WarningIcon", "Warning Icon Hidden"); // Hide warning icon
            }

        }
    }

    @Override
    public void onDriverRemoved(Driver driver) {
        MarkerCollection.removeMarker(driver.getDriverId());
        totalOnlineDrivers.setText(getResources()
                .getString(R.string.total_online_drivers)
                .concat(" ")
                .concat(String
                        .valueOf(MarkerCollection
                                .allMarkers()
                                .size())));
        ImageView warningIcon = findViewById(R.id.warningIcon);
        if (MarkerCollection.allMarkers().size() == 0) {
            warningIcon.setVisibility(View.VISIBLE); // Show warning icon
        } else {
            warningIcon.setVisibility(View.GONE);
            Log.d("WarningIcon", "Warning Icon Hidden"); // Hide warning icon
        }
    }

    @Override
    public void onDriverUpdated(Driver driver) {
        Marker marker = MarkerCollection.getMarker(driver.getDriverId());
        assert marker != null;
        MarkerAnimationHelper.animateMarkerToGB(marker, new LatLng(driver.getLat(), driver.getLng()), new LatLngInterpolator.Spherical());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        databaseReference.removeEventListener(firebaseEventListenerHelper);
        locationProviderClient.removeLocationUpdates(locationCallback);
        MarkerCollection.clearMarkers();
        currentLocationMarker = null;
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        // Retrieve the user's data from Firebase based on the marker's tag
        String driverId = (String) marker.getTag();
        if (driverId != null) {
            FirebaseDatabase.getInstance().getReference("/UsersData/")
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                // Retrieve and update UI with real-time data
                                double x = dataSnapshot.child("x").getValue(Double.class);
                                double y = dataSnapshot.child("y").getValue(Double.class);
                                double z = dataSnapshot.child("z").getValue(Double.class);

                                // Update the custom info window with x, y, z values
                                updateCustomInfoWindow(marker, x, y, z);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            // Handle any errors here
                        }
                    });
        }

        return false; // Return false to use the default info window behavior
    }

    private void updateCustomInfoWindow(Marker marker, double x, double y, double z) {
        // Inflate your custom info window layout
        View customInfoView = getLayoutInflater().inflate(R.layout.marker_info_window, null);

        // Find TextViews in the custom info window layout
        TextView xTextView = customInfoView.findViewById(R.id.xTextView);
        TextView yTextView = customInfoView.findViewById(R.id.yTextView);
        TextView zTextView = customInfoView.findViewById(R.id.zTextView);

        // Update the TextViews with real-time data
        xTextView.setText("X: " + x);
        yTextView.setText("Y: " + y);
        zTextView.setText("Z: " + z);

        // Set the custom info window for the marker
        marker.setInfoWindowAnchor(0.5f, 0.5f);
        googleMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                return null; // Use default info window
            }

            @Override
            public View getInfoContents(Marker marker) {
                return customInfoView; // Return the custom info window view
            }
        });

        // Show the info window
        marker.showInfoWindow();
    }
}

