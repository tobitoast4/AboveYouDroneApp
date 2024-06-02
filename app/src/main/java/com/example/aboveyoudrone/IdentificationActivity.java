package com.example.aboveyoudrone;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

public class IdentificationActivity extends AppCompatActivity {

    private SharedPreferences sharedPrefs;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_identification);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        sharedPrefs = getSharedPreferences("settings", Context.MODE_PRIVATE);
    }

    public void takeSnapshot(View v){
        RequestParams params = new RequestParams();
        params.put("user_id", sharedPrefs.getString("current_user_id", ""));
        params.put("drone_id", sharedPrefs.getString("current_drone_id", ""));

        ConstraintLayout image_loading = findViewById(R.id.image_loading);
        image_loading.setVisibility(View.VISIBLE);
        ServerRequestClient.post("/take_snapshot", params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Bitmap bmp = BitmapFactory.decodeByteArray(responseBody, 0, responseBody.length);
                ImageView identification_image = findViewById(R.id.identification_image);
                identification_image.setImageBitmap(bmp);
                ConstraintLayout image_loading = findViewById(R.id.image_loading);
                image_loading.setVisibility(View.INVISIBLE);

                ImageView button_confirm_snapshot = findViewById(R.id.button_confirm_snapshot);
                button_confirm_snapshot.setVisibility(View.VISIBLE);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                ConstraintLayout image_loading = findViewById(R.id.image_loading);
                image_loading.setVisibility(View.INVISIBLE);
            }
        });
    }

    public void confirmSnapshot(View v){
        RequestParams params = new RequestParams();
        params.put("user_id", sharedPrefs.getString("current_user_id", ""));
        params.put("image_hash", "");
        ServerRequestClient.post("/confirm_snapshot", params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    String status = response.getString("status");
                    if (status.equals("success")) {  // open next Activity on success
                        openDroneInteractionActivity();
                    } else {
                        String message = response.getString("message");
                        Toast.makeText(IdentificationActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void openDroneInteractionActivity() {
        Intent intent = new Intent(this, DroneInteractionActivity.class);
        startActivity(intent);
    }
}