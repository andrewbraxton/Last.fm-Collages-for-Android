package com.andrewbraxton.lastfmcollagesforandroid;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
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
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // TODO: Javadoc
    // TODO: pull out constants
    // TODO: app icon
    // TODO: persistent collage ImageView

    private static final String LOG_TAG = "MainActivityTag";

    public static final int COVERART_SIZE = 300;
    private static final String COVERART = "coverart";
    private static final String COLLAGE = "collage";
    public static final String PNG = ".png";


    private RequestQueue queue;
    private final Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        queue = Volley.newRequestQueue(this);

        File coverArtDir = new File(getFilesDir(), COVERART);
        File collageDir = new File(getFilesDir(), COLLAGE);
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
        // TODO: implement
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

    private Bitmap createCollageBitmap() {
        clearCollageDir();

        int collageSize = getCollageSize();
        Bitmap collage = getBlackBitmap(COVERART_SIZE*collageSize);
        Canvas canvas = new Canvas(collage);

        // draw all cover art onto the canvas
        for (int i = 0; i < collageSize * collageSize; i++) {
            int x = (i % collageSize) * COVERART_SIZE;
            int y = (i / collageSize) * COVERART_SIZE;
            File coverArtFile = new File(getCoverArtDir(), i + PNG);
            if (coverArtFile.exists()) {
                Bitmap coverArt = BitmapFactory.decodeFile(coverArtFile.getAbsolutePath());
                canvas.drawBitmap(coverArt, x, y, null);
            }
        }

        saveBitmap(new File(getCollageDir(), COLLAGE + PNG), collage);
        return collage;
    }

    private void fetchAllCoverArt() {
        clearCoverArtDir();

        String user = getUsername();
        int numDays = getNumDays();
        int collageSize = getCollageSize();

        StringRequest chartRequest = new StringRequest(
                ApiStringBuilder.buildGetAlbumChartUrl(user, getFromDate(numDays), getToDate()),
                jsonResponse -> {
                    Log.i(LOG_TAG, "Fetching cover art...");

                    List<Album> albums = getChartAlbums(jsonResponse, collageSize);
                    for (int i = 0; i < albums.size(); i++) {
                        fetchCoverArt(albums.get(i), i + PNG);
                    }
                },
                error -> {
                    String errorMessage;
                    if (error instanceof ClientError) {
                        errorMessage = getString(R.string.toast_invalid_username);
                        Log.d(LOG_TAG, "StringRequest error: Invalid username");
                    } else {
                        errorMessage = getString(R.string.toast_network_error);
                        Log.d(LOG_TAG, "StringRequest error: Network error");
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
                }
        );
        queue.add(chartRequest);
    }

    /**
     * Fetches the cover art for this album from the Last.fm API. Downloads it to internal storage.
     *
     * @param album    the album to get the cover art for
     * @param filename the name that the downloaded file will have
     */
    private void fetchCoverArt(Album album, String filename) {
        // TODO: make readable
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                ApiStringBuilder.buildGetAlbumInfoUrl(album),
                null,
                albumInfo -> {
                    File saveLocation = new File(getCoverArtDir(), filename);
                    try {
                        ImageRequest imageRequest = new ImageRequest(
                                getLargestImageUrl(albumInfo),
                                coverArt -> {
                                    Log.d(LOG_TAG, "Fetch success: " + album);
                                    saveBitmap(saveLocation, coverArt);
                                },
                                0, 0, null, null,
                                error -> {
                                    Log.e(LOG_TAG, "Fetch error (ImageRequest):  " + album);
                                });
                        queue.add(imageRequest);
                    } catch (JSONException e) {
                        Log.e(LOG_TAG, "Fetch error (No cover art found): " + album);
                        saveBitmap(saveLocation, getBlackBitmap(COVERART_SIZE));
                        // TODO: better handling of no cover art
                    }
                },
                error -> {
                    Log.e(LOG_TAG, "Fetch error (JsonRequest Error): " + album);
                });
        queue.add(jsonObjectRequest);
    }

    private List<Album> getChartAlbums(String chartJson, int collageSize) {
        chartJson = chartJson.replace("#", "");
        AlbumChart chart = gson.fromJson(chartJson, AlbumChart.class);
        int numAlbums = Math.min(collageSize * collageSize, chart.getAlbums().size());
        return chart.getAlbums().subList(0, numAlbums);
    }

    private String getLargestImageUrl(JSONObject albumInfo) throws JSONException {
        JSONArray imageUrls = albumInfo.getJSONObject("album").getJSONArray("image");
        JSONObject largestImageObject = imageUrls.getJSONObject(imageUrls.length() - 1);
        return largestImageObject.getString("#text");
    }

    private void saveBitmap(File saveLocation, Bitmap bmp) {
        try {
            OutputStream fOutStream = new FileOutputStream(saveLocation);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fOutStream);
            fOutStream.close();
            Log.i(LOG_TAG, "Save success: " + saveLocation.getName());
        } catch (IOException e) {
            Log.e(LOG_TAG, "Save error (IOException): " + saveLocation.getName());
            throw new RuntimeException(e);
        }
    }

    private Bitmap getBlackBitmap(int size) {
        Bitmap blackBmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        blackBmp.eraseColor(Color.BLACK);
        return blackBmp;
    }

    private SharedPreferences getPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(this);
    }

    private String getUsername() {
        return getPrefs().getString(getString(R.string.key_pref_username), "");
    }

    private int getNumDays() {
        return Integer.parseInt(getPrefs().getString(getString(R.string.key_pref_num_days), "7"));
    }

    private int getCollageSize() {
        return Integer.parseInt(getPrefs().getString(getString(R.string.key_pref_size), "3"));
    }

    /**
     * Returns the initial Unix timestamp for use in the "from" API parameter.
     *
     * @param numDays number of days the collage spans
     */
    private long getFromDate(int numDays) {
        if (numDays == -1) { // "All-time" was selected in preferences
            return 0;
        }
        return getToDate() - (numDays * 86400); // 86400 seconds in a day
    }

    /**
     * Returns the Unix timestamp of the current moment for use in the "to" API parameter.
     */
    private long getToDate() {
        return System.currentTimeMillis() / 1000L;
    }

    /**
     * Returns the directory in internal storage where album cover arts are stored.
     */
    private File getCoverArtDir() {
        return new File(getFilesDir(), COVERART);
    }

    /**
     * Returns the directory in internal storage where generated collages are stored.
     */
    private File getCollageDir() {
        return new File(getFilesDir(), COLLAGE);
    }

    /**
     * Clears the cover art directory in internal storage.
     */
    private void clearCoverArtDir() {
        for (File coverArt : getCoverArtDir().listFiles()) {
            coverArt.delete();
        }
        Log.i(LOG_TAG, "Cover art directory cleared");
    }

    /**
     * Clears the collage directory in internal storage.
     */
    private void clearCollageDir() {
        for (File collage : getCollageDir().listFiles()) {
            collage.delete();
        }
        Log.i(LOG_TAG, "Collage directory cleared");
    }
}
