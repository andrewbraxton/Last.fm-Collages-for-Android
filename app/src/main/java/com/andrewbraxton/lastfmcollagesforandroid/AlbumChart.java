package com.andrewbraxton.lastfmcollagesforandroid;

import java.util.List;

/**
 * Represents the JSON object returned by a call to user.getWeeklyAlbumChart. For use with Gson's fromJson().
 */
class AlbumChart {

    private WeeklyAlbumChart weeklyalbumchart;

    private class WeeklyAlbumChart {
        private List<Album> album;
    }

    public List<Album> getAlbums() {
        return weeklyalbumchart.album;
    }

}
