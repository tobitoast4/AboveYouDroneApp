package com.example.aboveyoudrone;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity implements
        GoogleMap.OnMapClickListener,
        GoogleMap.OnMapLongClickListener,
        GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnMarkerClickListener,
        OnMapReadyCallback {

    private GoogleMap map;

    private SupportMapFragment mapFragment;
    private boolean cameraHasBeenMoved = false;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int REQUEST_GPS_LOCATION = 100;
    private boolean permissionDenied = false;
    private FusedLocationProviderClient fusedLocationClient;

    private SharedPreferences sharedPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        sharedPrefs = getSharedPreferences("settings", Context.MODE_PRIVATE);
        // In the sharedPrefs we want to store:
        //  current_user_id (String)
        //  current_drone_id (String)
        //  timestamp_rental_started (long)

        if (sharedPrefs.getString("current_user_id", "").equals("")) {
            sharedPrefs.edit().putString("current_user_id", "myUserId").apply();
        }
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        enableMyLocation();

        RequestParams params = new RequestParams();
        params.put("user_id", sharedPrefs.getString("current_user_id", ""));
        ServerRequestClient.post("/get_rental", params, new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    String active_rental = response.getString("active_rental");
                    if (active_rental.equals("yes")) {
                        String drone_id = response.getString("drone_id");
                        sharedPrefs.edit().putString("current_drone_id", drone_id).apply();
                        long timestamp_rental_started = response.getLong("timestamp_rental_started");
                        sharedPrefs.edit().putLong("timestamp_rental_started", timestamp_rental_started).apply();

                        ImageView button_current_rental = findViewById(R.id.button_current_rental);
                        button_current_rental.setVisibility(View.VISIBLE);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void rentDrone(View v) throws JSONException {
        ConstraintLayout connect_to_drone_loading = findViewById(R.id.stopping_rental);
        connect_to_drone_loading.setVisibility(View.VISIBLE);

        RequestParams params = new RequestParams();
        params.put("user_id", sharedPrefs.getString("current_user_id", ""));
        params.put("drone_id", sharedPrefs.getString("current_drone_id", ""));
        ServerRequestClient.post("/rent_drone", params, new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    String status = response.getString("status");
                    if (status.equals("success")) {  // open next Activity on success
                        long timestamp_rental_started = response.getLong("timestamp_rental_started");
                        sharedPrefs.edit().putLong("timestamp_rental_started", timestamp_rental_started).apply();
                        openIdentificationActivity();
                    } else {
                        String message = response.getString("message");
                        Toast.makeText(MainActivity.this, "Rental failed: " + message, Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                connect_to_drone_loading.setVisibility(View.GONE);
            }
        });
    }

    public void openDroneInteractionActivity(View v) {
        Intent intent = new Intent(this, DroneInteractionActivity.class);
        startActivity(intent);
    }

    private void openIdentificationActivity() {
        Intent intent = new Intent(this, IdentificationActivity.class);
        startActivity(intent);
    }

    public void reportIssue(View v){
        Toast.makeText(this, "Not implemented", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.setOnMyLocationButtonClickListener(this);
        map.setOnMapClickListener(this);
        map.setOnMapLongClickListener(this);
        map.setOnMarkerClickListener(this);
        map.getUiSettings().setZoomControlsEnabled(false);

        if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        map.setMyLocationEnabled(true);

        int height = 100;
        int width = 100;
        BitmapDrawable bitmapdraw = (BitmapDrawable) ContextCompat.getDrawable(getApplicationContext(), R.drawable.drone);   ;
        Bitmap b = bitmapdraw.getBitmap();
        Bitmap smallMarker = Bitmap.createScaledBitmap(b, width, height, false);

        ServerRequestClient.get("/get_drones", null, new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    JSONArray drones = response.getJSONArray("drones");
                    for (int i = 0; i < drones.length(); i++){
                        JSONObject droneObject = drones.getJSONObject(i);
                        String drone_id = droneObject.getString("id");
                        int battery = droneObject.getInt("battery");
                        double price = droneObject.getDouble("price");
                        Drone drone = new Drone(drone_id, battery, price);

                        JSONObject positionObject = droneObject.getJSONObject("position");
                        double lat = positionObject.getDouble("lat");
                        double lng = positionObject.getDouble("lng");
                        LatLng position = new LatLng(lat, lng);

                        Marker myMarker = map.addMarker(new MarkerOptions()
                                .position(position)
                                .anchor(0.5f, 0.5f)
                                .icon(BitmapDescriptorFactory.fromBitmap(smallMarker)));
                        myMarker.setTag(drone);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                super.onSuccess(statusCode, headers, response);
            }
        });

        map.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
            @Override
            public void onMyLocationChange(@NonNull Location location) {
                if(!cameraHasBeenMoved){
                    LatLng my_pos = new LatLng(location.getLatitude(), location.getLongitude());
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(my_pos, 16.0f));
                    cameraHasBeenMoved = true;
                }
            }
        });
    }

    @Override
    public void onMapClick(@NonNull LatLng latLng) {
        moveDroneMenu(700);
    }

    @Override
    public void onMapLongClick(@NonNull LatLng latLng) {

    }

    @Override
    public boolean onMyLocationButtonClick() {
        return false;
    }

    /** The following code is for retrieving user's location
     * ########################################################
     */
    @SuppressLint("MissingPermission")
    private void enableMyLocation() {
        // [START maps_check_location_permission]
        // 1. Check if permissions are granted, if so, enable the my location layer
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {
                                LatLng my_pos = new LatLng(location.getLatitude(), location.getLongitude());
                                map.moveCamera(CameraUpdateFactory.newLatLngZoom(my_pos, 16.0f));
                            }
                        }
                    });
        } else {
            // 2. Otherwise, request location permissions from the user.
            com.tozil.meetupapp.utils.PermissionUtils.requestLocationPermissions(this, LOCATION_PERMISSION_REQUEST_CODE, true);
            // [END maps_check_location_permission]
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (permissionDenied) {
            // Permission was not granted, display error dialog.
            showMissingPermissionError();
            permissionDenied = false;
        }
    }

    /**
     * Displays a dialog with error message explaining that the location permission is missing.
     */
    private void showMissingPermissionError() {
        com.tozil.meetupapp.utils.PermissionUtils.PermissionDeniedDialog.newInstance(true).show(getSupportFragmentManager(), "dialog");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_GPS_LOCATION && resultCode == 0) {
            finish();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        Drone drone = (Drone) marker.getTag();
        sharedPrefs.edit().putString("current_drone_id", drone.getId()).apply();
        TextView drone_id_text = findViewById(R.id.drone_id_text);
        drone_id_text.setText("Drone " + drone.getId());
        TextView drone_price_text = findViewById(R.id.drone_price_text);
        drone_price_text.setText("" + drone.getPrice() + "€ /\nMINUTE");
        TextView drone_battery_level_text = findViewById(R.id.drone_battery_level_text);
        int minutes = (int) (drone.getBattery() * 1.64);
        drone_battery_level_text.setText("" + drone.getBattery() + " %\n(" + minutes + " MIN)");
        moveDroneMenu(-700);
        return false;
    }

    private void moveDroneMenu(int distance) {
        ConstraintLayout rent_drone_menu = findViewById(R.id.rent_drone_menu);
        rent_drone_menu.animate()
                .translationY(distance)
                .setDuration(300)
                .start();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Bundle bundle = intent.getExtras();
        if(bundle != null){
            int timestamp_rental_started = (int) bundle.getDouble("timestamp_rental_started");
            int timestamp_rental_ended = (int) bundle.getDouble("timestamp_rental_ended");
            double price_to_pay = bundle.getDouble("price_to_pay");

            int duration = (timestamp_rental_ended - timestamp_rental_started) / 60;
            TextView after_rent_overlay_duration = findViewById(R.id.after_rent_overlay_duration);
            after_rent_overlay_duration.setText("" + duration + " min");
            TextView after_rent_overlay_price = findViewById(R.id.after_rent_overlay_price);
            after_rent_overlay_price.setText("" + price_to_pay + " €");
            
            ImageView button_current_rental = findViewById(R.id.button_current_rental);
            button_current_rental.setVisibility(View.GONE);
            ConstraintLayout after_rent_overlay = findViewById(R.id.after_rent_overlay);
            after_rent_overlay.setVisibility(View.VISIBLE);
        }
        super.onNewIntent(intent);
    }

    public void closeAfterRentOverlay(View v){
        ConstraintLayout after_rent_overlay = findViewById(R.id.after_rent_overlay);
        after_rent_overlay.setVisibility(View.GONE);
    }
}