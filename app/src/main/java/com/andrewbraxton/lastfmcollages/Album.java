package com.andrewbraxton.lastfmcollages;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Represents an album JSON object returned as part of a call to user.getTopAlbums. For use with Gson's
 * fromJson().
 */
class Album {

    private Artist artist;
    private String name;
    private List<CoverArt> image;

    private class Artist {
        private String name;
    }

    private class CoverArt {
        @SerializedName("#text")
        private String url;
    }

    /**
     * @return this album's info in the form of "Artist - Title"
     */
    @Override
    public String toString() {
        return artist.name + " - " + name;
    }

    /**
     * @return the name of the artist for this album
     */
    public String getArtistName() {
        return artist.name;
    }

    /**
     * @return the name of this album
     */
    public String getName() {
        return name;
    }

    /**
     * @return the URL for the largest cover art for this album
     */
    public String getLargestCoverArtUrl() {
        return image.get(image.size() - 1).url; // largest cover art is located at the end of the JSON array
    }

}
