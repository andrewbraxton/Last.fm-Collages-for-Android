package com.andrewbraxton.lastfmcollages;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.ClientError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

/**
 * The main screen of the app where the user can generate and view collages.
 */
public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = "MainActivityTag";
    private static final String FILEPROVIDER_AUTHORITY = "com.andrewbraxton.lastfmcollages.fileprovider";

    private static final int COVERART_SIZE = 300; // length/width in pixels of the largest cover art returned by API
    private static final String COVERART = "coverart";
    private static final String COLLAGE = "collage";
    private static final String PNG = ".png";

    private ImageView collageView;
    private Button generateButton;
    private ProgressBar progressBar;
    private RequestQueue queue;
    private DefaultRetryPolicy retryPolicy;
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
        generateButton = findViewById(R.id.button_generate);
        progressBar = findViewById(R.id.progressBar);
        queue = Volley.newRequestQueue(this);
        retryPolicy = new DefaultRetryPolicy(5000, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);

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

        File collageFile = getCollageFile();
        if (collageFile.exists()) {
            displayCollage(BitmapFactory.decodeFile(collageFile.getAbsolutePath()));
        }
    }

    /**
     * Calls saveCollageToExternalStorage() if the permission was granted.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.i(LOG_TAG, "Write permission granted");
            saveCollageToExternalStorage();
        } else {
            Log.i(LOG_TAG, "Write permission denied");
            showToast(R.string.toast_save_error_permission);
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
     * Click listener for the Share menu item. If a collage file exists in internal storage, an app chooser is launched
     * to allow the user to share the collage.
     */
    public void shareButtonClicked(MenuItem v) {
        Log.i(LOG_TAG, "Share button clicked");

        File collageFile = getCollageFile();
        if (collageFile.exists()) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            Uri collageUri = FileProvider.getUriForFile(this, FILEPROVIDER_AUTHORITY, collageFile);
            shareIntent.putExtra(Intent.EXTRA_STREAM, collageUri);
            shareIntent.setType("image/png");
            startActivity(Intent.createChooser(shareIntent, getString(R.string.menu_share_title)));
        } else {
            showToast(R.string.toast_share_invalid);
        }
    }

    /**
     * Click listener for the Save menu item. Requests the permission to write to external storage if needed or calls
     * saveCollageToExternalStorage() if the permission has already been granted.
     */
    public void saveButtonClicked(MenuItem v) {
        Log.i(LOG_TAG, "Download button clicked");

        if (havePermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            saveCollageToExternalStorage();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }
    }

    /**
     * Click listener for the Settings menu item. Simply launches the settings activity.
     */
    public void settingsButtonClicked(MenuItem v) {
        Log.i(LOG_TAG, "Settings button clicked");

        startActivity(new Intent(this, SettingsActivity.class));
    }

    // Private functions.

    /**
     * Makes the API call to user.getTopAlbums and then calls fetchCoverArt() for each album in the
     * collage. Much of the actual "generation" work is placed in fetchCoverArt() due to Volley's asynchronous task
     * execution. Also handles various problems at the initial generation phase (invalid username, API down, etc.).
     */
    private void generateCollage() {
        clearCoverArtDir();
        generateButton.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        JsonObjectRequest topAlbumsRequest = new JsonObjectRequest(
                ApiStringBuilder.buildGetTopAlbumsUrl(getUsername(), getPeriod(), getNumAlbums()),
                null,
                response -> {
                    Log.i(LOG_TAG, "Fetching top albums...");

                    List<Album> albums = parseAlbumList(response);
                    if (albums.size() < getNumAlbums()) {
                        generateButton.setEnabled(true);
                        progressBar.setVisibility(View.INVISIBLE);
                        showToast(R.string.toast_generate_invalid_numalbums);
                    } else {
                        for (int i = 0; i < albums.size(); i++) {
                            Album album = albums.get(i);
                            fetchCoverArt(album, i + PNG);
                        }
                    }
                },
                error -> {
                    int errorMessageId;
                    if (error instanceof ClientError) {
                        Log.e(LOG_TAG, "Chart request error: Invalid username");
                        errorMessageId = R.string.toast_generate_invalid_username;
                    } else if (error.networkResponse == null) {
                        Log.e(LOG_TAG, "Chart request error: Device couldn't connect to network");
                        errorMessageId = R.string.toast_generate_error_network;
                    } else {
                        Log.e(LOG_TAG, "Chart request error: Last.fm API down");
                        errorMessageId = R.string.toast_generate_error_api;
                    }

                    generateButton.setEnabled(true);
                    progressBar.setVisibility(View.INVISIBLE);
                    showToast(errorMessageId);
                }
        );
        topAlbumsRequest.setRetryPolicy(retryPolicy);
        queue.add(topAlbumsRequest);
    }

    /**
     * Fetches the cover art for an album from the URL provided by Last.fm and downloads it to internal storage.
     * Also responsible for displaying the collage when all covers have been downloaded (placed here due to Volley's
     * asynchronous task execution).
     *
     * @param album    the album to get the cover art for
     * @param filename the name that the downloaded file will have
     */
    private void fetchCoverArt(Album album, String filename) {
        File saveLocation = new File(getCoverArtDir(), filename);
        ImageRequest coverArtRequest = new ImageRequest(
                album.getLargestCoverArtUrl(),
                coverArt -> handleCoverArtFetched(album, saveLocation, coverArt),
                0, 0, null, null,
                error -> handleCoverArtFetched(album, saveLocation, null)
        );
        coverArtRequest.setRetryPolicy(retryPolicy);
        queue.add(coverArtRequest);
    }

    /**
     * Saves the cover art to internal storage and calls handleAllCoverFetched() if necessary.
     *
     * @param album        the album that the cover art is for
     * @param saveLocation the location to save the cover art to
     * @param coverArt     the album's cover art, null if the cover art couldn't be fetched
     */
    private void handleCoverArtFetched(Album album, File saveLocation, Bitmap coverArt) {
        if (coverArt != null) {
            Log.i(LOG_TAG, "Fetch success: " + album);
            saveBitmap(saveLocation, coverArt);
        } else {
            Log.e(LOG_TAG, "Fetch error: " + album);
            saveBitmap(saveLocation, drawGenericCoverArt(album));
        }

        boolean allCoverArtFetched = getCoverArtDir().listFiles().length == getNumAlbums();
        if (allCoverArtFetched) {
            handleAllCoverArtFetched();
        }
    }

    /**
     * Should be called after checking that doneFetchingCoverArt() returns true. Creates and displays the collage from
     * the downloaded cover art, re-enables the generate button, and shows a "successful generation" toast.
     */
    private void handleAllCoverArtFetched() {
        displayCollage(createCollageBitmap());
        generateButton.setEnabled(true);
        progressBar.setVisibility(View.INVISIBLE);
        showToast(R.string.toast_generate_successful);
    }

    /**
     * Takes the raw response from the result of the Volley request to user.getTopAlbums and returns the list of albums
     * to be included in the collage.
     *
     * @param response the org.json.JSONObject returned by the request to user.getTopAlbums
     */
    private List<Album> parseAlbumList(JSONObject response) {
        JsonObject convertedResponse = gson.fromJson(response.toString(), JsonObject.class);
        JsonArray albumsArray = convertedResponse.getAsJsonObject("topalbums").getAsJsonArray("album");
        return Arrays.asList(gson.fromJson(albumsArray, Album[].class));
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

        saveBitmap(getCollageFile(), collage);
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
            Log.i(LOG_TAG, "Save success: " + saveLocation.getPath());
        } catch (IOException e) {
            Log.e(LOG_TAG, "Save error (IOException): " + saveLocation.getPath());
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
     * Creates and returns a generic cover art for when an album's cover art couldn't be fetched. The generic art is
     * simply the artist name on top of the album name.
     *
     * @param album the album to create generic cover art for
     */
    private Bitmap drawGenericCoverArt(Album album) {
        Bitmap coverArt = getBlackBitmap(COVERART_SIZE);
        Canvas canvas = new Canvas(coverArt);
        TextPaint paint = new TextPaint();
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(40);

        String text = album.getArtistName() + "\n" + album.getName();
        StaticLayout layout = StaticLayout.Builder.obtain(text, 0, text.length(), paint, COVERART_SIZE).build();
        canvas.translate(canvas.getWidth() / 2, canvas.getHeight() / 2 - layout.getHeight() / 2);
        layout.draw(canvas);

        return coverArt;
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
     * @return the number of albums per row/column of the collage as chosen in settings, e.g. 3 for a 3x3 collage
     */
    private int getCollageSize() {
        return Integer.parseInt(getPrefs().getString(getString(R.string.key_pref_size), "3"));
    }

    /**
     * @return the total number of albums in the collage as chosen in settings; equal to getCollageSize() squared
     */
    private int getNumAlbums() {
        return getCollageSize() * getCollageSize();
    }

    /**
     * @return the date range for this collage as chosen in settings
     */
    private String getPeriod() {
        return getPrefs().getString(getString(R.string.key_pref_period), "7day");
    }

    /**
     * @param permission the name of the permission being checked
     * @return true if the permission has been granted, false if not
     */
    private boolean havePermission(String permission) {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Copies the collage bitmap saved in internal storage (if it exists) to the app's external storage directory.
     */
    private void saveCollageToExternalStorage() {
        File collageFile = getCollageFile();
        if (collageFile.exists()) {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                Bitmap collage = BitmapFactory.decodeFile(collageFile.getAbsolutePath());
                String filename = COLLAGE + (System.currentTimeMillis() / 1000L) + PNG;
                saveBitmap(new File(getPublicCollageDir(), filename), collage);
                showToast(R.string.toast_save_successful);
            } else {
                Log.e(LOG_TAG, "Device not allowing file saving");
                showToast(R.string.toast_save_error_device);
            }
        } else {
            Log.d(LOG_TAG, "No collage to save");
            showToast(R.string.toast_save_invalid);
        }
    }

    /**
     * @return the file in internal storage where the most recently generated collage is stored
     */
    private File getCollageFile() {
        return new File(getCollageDir(), COLLAGE + PNG);
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
     * @return the directory for saving collages in external storage
     */
    private File getPublicCollageDir() {
        File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File collageDir = new File(picturesDir, getString(R.string.app_name));
        collageDir.mkdirs();
        return collageDir;
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
