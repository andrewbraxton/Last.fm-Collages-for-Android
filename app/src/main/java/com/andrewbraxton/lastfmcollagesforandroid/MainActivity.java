package com.andrewbraxton.lastfmcollagesforandroid;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.ClientError;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;

public class MainActivity extends AppCompatActivity {

    private static final String CLICK_TAG = "ButtonClicked";

    private RequestQueue queue;
    private final Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        queue = Volley.newRequestQueue(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    public void generateImage(View v) {
        Log.d(CLICK_TAG, "Generate");

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String username = prefs.getString(getString(R.string.key_pref_username), "");
        int numDays = Integer.parseInt(prefs.getString(getString(R.string.key_pref_date_range), "7"));

        if (username.isEmpty()) {
            Toast.makeText(this, getString(R.string.toast_no_username), Toast.LENGTH_SHORT).show();
            return;
        }

        String url = buildUrlString(username, getFromDate(numDays), getToDate());
        StringRequest stringRequest = new StringRequest(
                url,
                response -> {
                    response = response.replace("#", ""); // removing the pound signs in Last.fm's JSON keys
                    AlbumChart chartObject = gson.fromJson(response, AlbumChart.class);
                    ImageView chartImage = findViewById(R.id.chartImage);
                    chartImage.setImageDrawable(chartObject.generateDrawable());
                    Toast.makeText(this, getString(R.string.toast_generate_successful), Toast.LENGTH_SHORT).show();
                },
                error -> {
                    String errorMessage = getString(R.string.toast_network_error);
                    if (error instanceof ClientError) {
                        errorMessage = getString(R.string.toast_invalid_username);
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
                }
        );
        queue.add(stringRequest);
    }

    public void openSettings(MenuItem v) {
        Log.d(CLICK_TAG, "Settings");

        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    public void shareImage(MenuItem v) {
        Log.d(CLICK_TAG, "Share");
        // TODO: implement shareImage()
    }

    public void downloadImage(MenuItem v) {
        Log.d(CLICK_TAG, "Download");
        // TODO: implement downloadImage()
    }

    // Helper functions.

    private long getFromDate(int numDays) {
        if (numDays == -1) { // "All-time" was selected in preferences
            return 0;
        }
        return getToDate() - (numDays * 86400); // 86400 seconds in a day
    }

    private long getToDate() {
        return System.currentTimeMillis() / 1000L; // conversion to Unix timestamp
    }

    private String buildUrlString(String username, long fromDate, long toDate) {
        // TODO: restructure ApiInfo to hold string literals
        String url = ApiInfo.BASE_URL + ApiInfo.GET_CHART;
        url += "&user=" + username;
        url += "&from=" + fromDate + "&to=" + toDate;
        url += "&api_key=" + ApiInfo.API_KEY;
        url += "&format=json";
        return url;
    }
}
