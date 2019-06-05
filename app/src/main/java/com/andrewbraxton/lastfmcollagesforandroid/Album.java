package com.andrewbraxton.lastfmcollagesforandroid;

import com.google.gson.annotations.SerializedName;

/**
 * Represents an album JSON object returned as part of a call to user.getWeeklyAlbumChart. For use with Gson's
 * fromJson().
 * <p>
 * Note: NOT intended to represent the album JSON object returned by call to album.getInfo (that object is structured
 * differently).
 */
public class Album {

    private Artist artist;
    private String mbid;
    private String name;
    private String url;

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

    public String getMbid() {
        return mbid;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

}
