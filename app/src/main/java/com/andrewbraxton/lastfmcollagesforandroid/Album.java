package com.andrewbraxton.lastfmcollagesforandroid;

public class Album {

    private Artist artist;
    private String mbid;
    private String name;
    private String url;

    private class Artist {
        private String mbid;
        private String text;
    }

    public String getArtistMbid() {
        return artist.mbid;
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
