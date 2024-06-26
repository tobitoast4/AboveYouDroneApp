package com.example.aboveyoudrone;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Calendar;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

public class DroneInteractionActivity extends AppCompatActivity {

    private SharedPreferences sharedPrefs;
    private TextView textView_time_passed;
    private long timestamp_rental_started;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_drone_interaction);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        sharedPrefs = getSharedPreferences("settings", Context.MODE_PRIVATE);
        timestamp_rental_started = sharedPrefs.getLong("timestamp_rental_started", -1);
        textView_time_passed = findViewById(R.id.textView_time_passed);

        BackgroundThread thread = new BackgroundThread();
        thread.start();
    }

    public void panicButton(View v){

    }

    public void follow(View v){
        sendToggleFollow("/drone_follow");
    }

    public void stopFollow(View v){
        sendToggleFollow("/stop_drone_follow");
    }

    private void sendToggleFollow(String endpoint) {
        JSONObject jsonParams = new JSONObject();
        StringEntity jsonParamsAsString;
        try {
            jsonParams.put("user_id", sharedPrefs.getString("current_user_id", ""));
            jsonParams.put("drone_id", sharedPrefs.getString("current_drone_id", ""));
            jsonParamsAsString = new StringEntity(jsonParams.toString());
        } catch (UnsupportedEncodingException | JSONException e) {
            throw new RuntimeException(e);
        }
        ServerRequestClient.post(getApplicationContext(), endpoint, jsonParamsAsString, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    String status = response.getString("status");
                    String message = response.getString("message");
                    if (status.equals("success")) {  // open next Activity on success
                        Toast.makeText(DroneInteractionActivity.this, message, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(DroneInteractionActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void stopRental(View v){
        JSONObject jsonParams = new JSONObject();
        StringEntity jsonParamsAsString;
        try {
            jsonParams.put("user_id", sharedPrefs.getString("current_user_id", ""));
            jsonParams.put("drone_id", sharedPrefs.getString("current_drone_id", ""));
            jsonParamsAsString = new StringEntity(jsonParams.toString());
        } catch (UnsupportedEncodingException | JSONException e) {
            throw new RuntimeException(e);
        }

        ConstraintLayout image_loading = findViewById(R.id.stopping_rental);
        image_loading.setVisibility(View.VISIBLE);
        ServerRequestClient.post(getApplicationContext(), "/stop_rental", jsonParamsAsString, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    String status = response.getString("status");
                    if (status.equals("success")) {  // open next Activity on success
                        sharedPrefs.edit().putString("current_drone_id", null).apply();
                        sharedPrefs.edit().putLong("timestamp_rental_started", -1).apply();

                        double timestamp_rental_started = response.getDouble("timestamp_rental_started");
                        double timestamp_rental_ended = response.getDouble("timestamp_rental_ended");
                        double price_to_pay = response.getDouble("price_to_pay");

                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        Bundle bundle = new Bundle();
                        bundle.putDouble("timestamp_rental_started", timestamp_rental_started);
                        bundle.putDouble("timestamp_rental_ended", timestamp_rental_ended);
                        bundle.putDouble("price_to_pay", price_to_pay);
                        intent.putExtras(bundle);
                        startActivity(intent);
                    } else {
                        String message = response.getString("message");
                        Toast.makeText(DroneInteractionActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                } finally {
                    ConstraintLayout image_loading = findViewById(R.id.stopping_rental);
                    image_loading.setVisibility(View.GONE);
                }
            }
        });
    }

    class BackgroundThread extends Thread{

        @Override
        public void run() {
            refresh();
        }

        public void refresh(){
            while (true) {
                long unixTime = System.currentTimeMillis() / 1000L;
                long time_difference = unixTime - timestamp_rental_started;
                int seconds = (int) (time_difference % 60);
                time_difference = time_difference - seconds;
                int minutes = (int) ((time_difference % 3600) / 60);
                time_difference = time_difference - minutes;
                int hours = (int) (time_difference / 3600);

                String timestamp;
                if (hours > 0) {
                    timestamp = "" + String.format("%02d", hours) + ":" + String.format("%02d", minutes) + ":" + String.format("%02d", seconds);
                } else {
                    timestamp = "" + String.format("%02d", minutes) + ":" + String.format("%02d", seconds);
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textView_time_passed.setText(timestamp);
                    }
                });

                // Refresh the TextView every 1 second
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}