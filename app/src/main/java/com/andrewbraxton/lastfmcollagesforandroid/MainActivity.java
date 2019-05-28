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
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // TODO: Javadoc
    // TODO: better logging

    private static final String LOG_TAG = "MainActivityTag";

    private static final String COVERART_DIR = "coverart";
    private static final String COLLAGE_DIR = "collage";

    private RequestQueue queue;
    private final Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        queue = Volley.newRequestQueue(this);

        File coverArtDir = new File(getFilesDir(), COVERART_DIR);
        File collageDir = new File(getFilesDir(), COLLAGE_DIR);
        coverArtDir.mkdir();
        collageDir.mkdir();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    public void generateButtonClicked(View v) {
        Log.i(LOG_TAG, "Generate button clicked");

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String username = prefs.getString(getString(R.string.key_pref_username), "");
        int numDays = Integer.parseInt(prefs.getString(getString(R.string.key_pref_date_range), "7"));
        int collageSize = Integer.parseInt(prefs.getString(getString(R.string.key_pref_size), "3"));

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
                    chartImage.setImageDrawable(generateChartDrawable(chartObject, collageSize));
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

    public void settingsButtonClicked(MenuItem v) {
        Log.i(LOG_TAG, "Settings button clicked");

        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    public void shareButtonClicked(MenuItem v) {
        Log.i(LOG_TAG, "Share button clicked");
        // TODO: implement
    }

    public void downloadButtonClicked(MenuItem v) {
        Log.i(LOG_TAG, "Download button clicked");
        // TODO: implement
    }

    // Helper functions.

    /**
     * Returns the initial Unix timestamp for use in the "from" API parameter.

     * @param numDays number of days the collage spans
     */
    private long getFromDate(int numDays) {
        if (numDays == -1) { // "All-time" was selected in preferences
            return 0;
        }
        return getToDate() - (numDays * 86400); // 86400 seconds in a day
    }

    /** Returns the Unix timestamp of the current moment for use in the "to" API parameter. */
    private long getToDate() {
        return System.currentTimeMillis() / 1000L;
    }

    /**
     * Fetches the cover art for this album from the Last.fm API and downloads it to internal storage.
     *
     * @param album    the album to get the cover art for
     * @param filename the name that the downloaded file will have
     */
    private void fetchCoverArt(Album album, String filename) {
        // TODO: implement actual error handling
        // TODO: make readable
        String albumInfoUrl = ApiStringBuilder.buildGetAlbumInfoUrl(album);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                albumInfoUrl,
                null,
                response -> {
                    try {
                        JSONArray imageUrls = response.getJSONObject("album").getJSONArray("image");
                        JSONObject largestImageObject = imageUrls.getJSONObject(imageUrls.length()-1);
                        String largestImageUrl = largestImageObject.getString("#text");
                        ImageRequest imageRequest = new ImageRequest(
                                largestImageUrl,
                                bitmap -> {
                                    try {
                                        Log.d(LOG_TAG, "Fetch success: " + album);

                                        File imageFile = new File(getCoverArtDir(), filename);
                                        OutputStream fOutStream = new FileOutputStream(imageFile);
                                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOutStream);
                                        fOutStream.close();
                                    } catch (Exception e) {
                                        Log.e(LOG_TAG, "Fetch error (ImageRequest Error1): " + album);
                                    }
                                }, 0, 0, null, null,
                                error -> {
                                    Log.e(LOG_TAG, "Fetch error (ImageRequest Error2): " + album);
                                });
                        queue.add(imageRequest);
                    } catch (JSONException e) {
                        // album couldn't be found
                        Log.e(LOG_TAG, "Fetch error (JsonRequest Error1): " + album);
                    }
                },
                error -> {
                    Log.e(LOG_TAG, "Fetch error (JsonRequest Error2): " + album);
                });
        queue.add(jsonObjectRequest);
    }

    private Drawable generateChartDrawable(AlbumChart chartObject, int collageSize) {
        clearCoverArtDir();
        List<Album> albums = chartObject.getAlbums();
        int numAlbums = collageSize * collageSize;
        for (int i = 0; i < numAlbums; i++) {
            fetchCoverArt(albums.get(i), i + ".png");
        }

        // TODO: implement collage generation
        return null;
    }

    /** Returns the directory in internal storage where album cover arts are stored. */
    private File getCoverArtDir() {
        return new File(getFilesDir(), COVERART_DIR);
    }

    /** Returns the directory in internal storage where generated collages are stored. */
    private File getCollageDir() {
        return new File(getFilesDir(), COLLAGE_DIR);
    }

    /** Clears the cover art directory in internal storage. */
    private void clearCoverArtDir() {
        for (File coverArt : getCoverArtDir().listFiles()) {
            coverArt.delete();
        }
        Log.i(LOG_TAG, "Cover art directory cleared");
    }

    /** Clears the collage directory in internal storage. */
    private void clearCollageDir() {
        for (File collage : getCollageDir().listFiles()) {
            collage.delete();
        }
        Log.i(LOG_TAG, "Collage directory cleared");
    }

}
