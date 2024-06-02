package com.example.aboveyoudrone;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
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

import cz.msebera.android.httpclient.Header;

public class DroneInteractionActivity extends AppCompatActivity {

    private SharedPreferences sharedPrefs;
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
    }

    public void panicButton(View v){

    }

    public void follow(View v){

    }

    public void stopFollow(View v){

    }

    public void stopRental(View v){
        RequestParams params = new RequestParams();
        params.put("user_id", sharedPrefs.getString("current_user_id", ""));
        params.put("drone_id", sharedPrefs.getString("current_drone_id", ""));
        ConstraintLayout image_loading = findViewById(R.id.stopping_rental);
        image_loading.setVisibility(View.VISIBLE);
        ServerRequestClient.post("/stop_rental", params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    String status = response.getString("status");
                    if (status.equals("success")) {  // open next Activity on success
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
}