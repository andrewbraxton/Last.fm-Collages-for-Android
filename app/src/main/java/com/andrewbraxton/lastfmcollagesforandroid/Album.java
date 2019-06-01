package com.andrewbraxton.lastfmcollagesforandroid;

/**
 * Represents an album JSON object returned as part of a call to user.getWeeklyAlbumChart. For use with Gson's
 * fromJson().
 * <p>
 * Note: NOT for use to represent the album JSON object returned by call to album.getInfo (that object is structured
 * differently).
 */
public class Album {

    private Artist artist;
    private String mbid;
    private String name;
    private String url;

    private class Artist {
        private String mbid;
        private String text;
    }

    @Override
    public String toString() {
        return artist.text + " - " + name;
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
