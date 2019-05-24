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
}
