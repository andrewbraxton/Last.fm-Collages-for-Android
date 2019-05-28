package com.andrewbraxton.lastfmcollagesforandroid;

import android.graphics.drawable.Drawable;

import java.util.List;

public class AlbumChart {
    private WeeklyAlbumChart weeklyalbumchart;

    private class WeeklyAlbumChart {
        private List<Album> album;
    }

    public List<Album> getAlbums() {
        return weeklyalbumchart.album;
    }

}
