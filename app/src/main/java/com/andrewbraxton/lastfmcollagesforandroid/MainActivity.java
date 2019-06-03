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
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * The main screen of the app where the user can generate and view collages.
 */
public class MainActivity extends AppCompatActivity {

    // TODO: progress indicator when pressing generate button
    // TODO: look into problem with album names containing ampersands, ñ, etc. (MBID solution?)
    // TODO: app icon
    // TODO: notifications

    private static final String LOG_TAG = "MainActivityTag";

    public static final int COVERART_SIZE = 300; // length/width in pixels of the largest cover art returned by API
    private static final String COVERART = "coverart";
    private static final String COLLAGE = "collage";
    public static final String PNG = ".png";

    private ImageView collageView;
    private RequestQueue queue;
    private final Gson gson = new Gson();

    // Functions called by Android system.

    /**
     * Initializes the Volley request queue and creates empty cover art and collage directories in internal storage.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(LOG_TAG, "onCreate() called");

        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.main_toolbar));

        collageView = findViewById(R.id.collageImageView);
        queue = Volley.newRequestQueue(this);

        getCoverArtDir().mkdir();
        getCollageDir().mkdir();
        clearCoverArtDir();
        clearCollageDir();
    }

    /**
     * Inflates the action bar menu.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    /**
     * Displays the most recently created collage if one exists.
     */
    @Override
    protected void onResume() {
        super.onResume();
        Log.i(LOG_TAG, "onResume() called");

        File collageFile = new File(getCollageDir(), COLLAGE + PNG);
        if (collageFile.exists()) {
            displayCollage(BitmapFactory.decodeFile(collageFile.getAbsolutePath()));
        }
    }

    /**
     * Click listener for Generate button. Simply calls generateCollage().
     */
    public void generateButtonClicked(View v) {
        Log.i(LOG_TAG, "Generate button clicked");

        generateCollage();
    }

    /**
     * Click listener for the Settings button. Simply launches the settings activity.
     */
    public void settingsButtonClicked(MenuItem v) {
        Log.i(LOG_TAG, "Settings button clicked");

        startActivity(new Intent(this, SettingsActivity.class));
    }

    // TODO: Javadoc
    public void shareButtonClicked(MenuItem v) {
        // TODO: implement
        Log.i(LOG_TAG, "Share button clicked");
    }

    // TODO: Javadoc
    public void downloadButtonClicked(MenuItem v) {
        // TODO: implement
        Log.i(LOG_TAG, "Download button clicked");
    }

    // Private functions.

    /**
     * Makes the initial API call to user.getWeeklyAlbumChart and then calls fetchCoverArt() for each album in the
     * collage. Much of the actual "generation" work is placed in fetchCoverArt() due to Volley's asynchronous task
     * execution.
     */
    private void generateCollage() {
        clearCoverArtDir();

        StringRequest chartRequest = new StringRequest(
                ApiStringBuilder.buildGetAlbumChartUrl(getUsername(), getFromDate(), getToDate()),
                rawChartJson -> {
                    Log.i(LOG_TAG, "Fetching cover art...");

                    List<Album> albums = getChartAlbums(rawChartJson);
                    for (int i = 0; i < albums.size(); i++) {
                        fetchCoverArt(albums.get(i), i + PNG);
                    }
                },
                error -> {
                    int errorMessageId;
                    if (error instanceof ClientError) {
                        Log.e(LOG_TAG, "StringRequest error: Invalid username");
                        errorMessageId = R.string.toast_invalid_username;
                    } else if (error.networkResponse == null) {
                        Log.e(LOG_TAG, "StringRequest error: Device couldn't connect to network");
                        errorMessageId = R.string.toast_device_network_error;
                    } else {
                        Log.e(LOG_TAG, "StringRequest error: Last.fm API down");
                        errorMessageId = R.string.toast_api_error;
                    }
                    showToast(errorMessageId);
                }
        );
        queue.add(chartRequest);
    }

    /**
     * Fetches the cover art for an album from the Last.fm API and downloads it to internal storage. Also responsible
     * for displaying the collage when all covers have been downloaded (must be placed here due to Volley's
     * asynchronous task execution).
     *
     * @param album    the album to get the cover art for
     * @param filename the name that the downloaded file will have
     */
    private void fetchCoverArt(Album album, String filename) {
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

                                    saveCoverArt(saveLocation, coverArt);
                                    if (doneFetchingCoverArt()) {
                                        displayCollage(createCollageBitmap());
                                        showToast(R.string.toast_generate_successful);
                                    }
                                },
                                0, 0, null, null,
                                error -> {
                                    // TODO: improve (MBID solution?)
                                    Log.e(LOG_TAG, "Fetch error (ImageRequest):  " + album);

                                    saveCoverArt(saveLocation, null);
                                    if (doneFetchingCoverArt()) {
                                        displayCollage(createCollageBitmap());
                                        showToast(R.string.toast_generate_successful);
                                    }
                                });
                        queue.add(imageRequest);
                    } catch (JSONException e) {
                        Log.e(LOG_TAG, "Fetch error (No cover art found): " + album);

                        saveCoverArt(saveLocation, null);
                        if (doneFetchingCoverArt()) {
                            displayCollage(createCollageBitmap());
                            showToast(R.string.toast_generate_successful);
                        }
                    }
                },
                error -> {
                    Log.e(LOG_TAG, "Fetch error (JsonRequest Error): " + album);
                    // TODO: look into this
                });
        queue.add(jsonObjectRequest);
    }

    /**
     * Draws the collage using the images in the cover art directory and saves the result to the collage directory.
     * Should only be called after all cover art has been fetched.
     *
     * @return the created collage
     */
    private Bitmap createCollageBitmap() {
        clearCollageDir();

        int collageSize = getCollageSize();
        Bitmap collage = getBlackBitmap(COVERART_SIZE * collageSize);
        Canvas canvas = new Canvas(collage);

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

    /**
     * Displays the collage on screen.
     *
     * @param collage the collage to display
     */
    private void displayCollage(Bitmap collage) {
        collageView.setImageBitmap(collage);

        Log.i(LOG_TAG, "Collage displayed");
    }

    // TODO: Javadoc
    private void saveCoverArt(File saveLocation, Bitmap coverArt) {
        // TODO: improve
        if (coverArt == null) {
            saveBitmap(saveLocation, getBlackBitmap(COVERART_SIZE));
        } else {
            saveBitmap(saveLocation, coverArt);
        }
    }

    /**
     * Saves a bitmap to a given location. Crashes application if an IOException is thrown (should never occur).
     *
     * @param saveLocation File object representing where the bitmap should be saved to
     * @param bmp          the bitmap to save
     */
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

    /**
     * Returns a black square bitmap of a given size.
     *
     * @param size the length/width in pixels of the square bitmap to create
     */
    private Bitmap getBlackBitmap(int size) {
        Bitmap blackBmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        blackBmp.eraseColor(Color.BLACK);
        return blackBmp;
    }

    /**
     * Returns the list of albums that are to be included in this collage, e.g. the first 16 albums in the list
     * returned by the call to user.getWeeklyAlbumChart if a 4x4 collage is selected.
     *
     * @param chartJson the raw JSON result of the call to user.getWeeklyAlbumChart
     */
    private List<Album> getChartAlbums(String chartJson) {
        chartJson = chartJson.replace("#", ""); // Removing the #s found in some of Last.fm's JSON keys
        AlbumChart chart = gson.fromJson(chartJson, AlbumChart.class);
        int numAlbums = Math.min(getCollageSize() * getCollageSize(), chart.getAlbums().size());
        return chart.getAlbums().subList(0, numAlbums);
    }

    /**
     * Returns the URL of the largest cover art returned by the call to album.getAlbumInfo (the API returns an array
     * of images in ascending size order).
     *
     * @param albumInfo the JSONObject returned by the call to album.getAlbumInfo
     * @throws JSONException if Last.fm doesn't have any cover art on file for this album
     */
    private String getLargestImageUrl(JSONObject albumInfo) throws JSONException {
        JSONArray imageUrls = albumInfo.getJSONObject("album").getJSONArray("image");
        JSONObject largestImage = imageUrls.getJSONObject(imageUrls.length() - 1);
        return largestImage.getString("#text");
    }

    // TODO: Javadoc
    private boolean doneFetchingCoverArt() {
        // TODO: handle user not having enough albums
        return getCoverArtDir().listFiles().length == getCollageSize() * getCollageSize();
    }

    /**
     * Shortcut for displaying a toast.
     *
     * @param resId the resource id of the message to display
     */
    private void showToast(int resId) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show();
    }

    /**
     * @return the default shared preferences
     */
    private SharedPreferences getPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(this);
    }

    /**
     * @return the Last.fm username entered in settings
     */
    private String getUsername() {
        return getPrefs().getString(getString(R.string.key_pref_username), "");
    }

    /**
     * @return the number of days the collage spans chosen in settings
     */
    private int getNumDays() {
        return Integer.parseInt(getPrefs().getString(getString(R.string.key_pref_num_days), "7"));
    }

    /**
     * @return the number of albums per row/column of the collage chosen in settings, e.g. returns 3 for a 3x3 collage
     */
    private int getCollageSize() {
        return Integer.parseInt(getPrefs().getString(getString(R.string.key_pref_size), "3"));
    }

    /**
     * @return the initial Unix timestamp for use in the "from" API parameter
     */
    private long getFromDate() {
        if (getNumDays() == -1) { // "All-time" was selected in preferences
            return 0;
        }
        return getToDate() - (getNumDays() * 86400); // 86400 seconds in a day
    }

    /**
     * @return the Unix timestamp of the current moment for use in the "to" API parameter
     */
    private long getToDate() {
        return System.currentTimeMillis() / 1000L;
    }

    /**
     * @return the directory in internal storage where album cover arts are stored
     */
    private File getCoverArtDir() {
        return new File(getFilesDir(), COVERART);
    }

    /**
     * @return the directory in internal storage where generated collages are stored
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
