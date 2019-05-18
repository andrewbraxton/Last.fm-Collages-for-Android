package com.andrewbraxton.lastfmcollagesforandroid;

import android.os.Bundle;
import android.util.Log;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    private static final String CLICK_TAG = "ButtonClicked";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    public void generateImage(View v) {
        Log.d(CLICK_TAG, "Generate");
    }

    public void openSettings(MenuItem v) {
        Log.d(CLICK_TAG, "Settings");

        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    public void shareImage(MenuItem v) {
        Log.d(CLICK_TAG, "Share");
    }

    public void downloadImage(MenuItem v) {
        Log.d(CLICK_TAG, "Download");
    }

}
