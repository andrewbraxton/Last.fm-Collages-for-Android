package com.andrewbraxton.lastfmcollages;

import com.google.gson.annotations.SerializedName;

/**
 * Represents an album JSON object returned as part of a call to user.getWeeklyAlbumChart. For use with Gson's
 * fromJson().
 * <p>
 * Note: NOT intended to represent the album JSON object returned by call to album.getInfo (that object is structured
 * differently).
 */
class Album {

    private Artist artist;
    private String name;

    private class Artist {
        @SerializedName("#text")
        private String text;
    }

    @Override
    public String toString() {
        return artist.text + " - " + name;
    }

    public String getArtistName() {
        return artist.text;
    }

    public String getName() {
        return name;
    }

}
