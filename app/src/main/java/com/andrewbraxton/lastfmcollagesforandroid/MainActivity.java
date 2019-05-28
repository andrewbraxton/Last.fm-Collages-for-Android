package com.andrewbraxton.lastfmcollagesforandroid;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
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
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;

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

        File coverArtDir = new File(getFilesDir(), "coverart");
        File collageDir = new File(getFilesDir(), "collage");
        coverArtDir.mkdir();
        collageDir.mkdir();
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

        String url = ApiStringBuilder.buildGetAlbumChartUrl(username, getFromDate(numDays), getToDate());
        StringRequest stringRequest = new StringRequest(
                url,
                response -> {
                    response = response.replace("#", ""); // removing the pound signs in Last.fm's JSON keys
                    AlbumChart chartObject = gson.fromJson(response, AlbumChart.class);
                    ImageView chartImage = findViewById(R.id.chartImage);
                    chartImage.setImageDrawable(generateChartDrawable(chartObject));
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

    /**
     * Fetches the cover art for this album from the Last.fm API and downloads it to internal storage.
     *
     * @param album the album to get the cover art for
     * @param filename the name that the downloaded file will have
     */
    private void fetchCoverArt(Album album, String filename) {
        String albumInfoUrl = ApiStringBuilder.buildGetAlbumInfoUrl(album);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(albumInfoUrl, null, response -> {
            try {
                JSONArray imageUrls = response.getJSONObject("album").getJSONArray("image");
                JSONObject largestImageObject = imageUrls.getJSONObject(imageUrls.length()-1);
                String largestImageUrl = largestImageObject.getString("#text");
                ImageRequest imageRequest = new ImageRequest(largestImageUrl, bitmap -> {
                    try {
                        File imageFile = new File(getCoverArtDir(), filename);
                        OutputStream fOutStream = new FileOutputStream(imageFile);
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOutStream);
                        fOutStream.close();
                    } catch (Exception e) {
                        // TODO: implement
                    }
                }, 0, 0, null, null, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO: implement
                    }
                });
                queue.add(imageRequest);
            } catch (JSONException e) {
                e.printStackTrace();
                // TODO: implement
            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // TODO: implement
            }
        });
        queue.add(jsonObjectRequest);
    }

    /**
     * Returns the album chart as a Drawable.
     *
     * @param chartObject
     * @return
     */
    private Drawable generateChartDrawable(AlbumChart chartObject) {
        // TODO: implement
        return null;
    }

    private File getCoverArtDir() {
        return new File(getFilesDir(), "coverart");
    }

    private File getCollageDir() {
        return new File(getFilesDir(), "collage");
    }

    private void clearCoverArtDir() {
        for (File coverArt: getCoverArtDir().listFiles()) {
            coverArt.delete();
        }
    }

    private void clearCollageDir() {
        for (File collage: getCollageDir().listFiles()) {
            collage.delete();
        }
    }


}
