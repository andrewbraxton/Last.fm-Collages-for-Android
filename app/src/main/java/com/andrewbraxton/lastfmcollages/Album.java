package com.andrewbraxton.lastfmcollages;

/**
 * Represents an album JSON object returned as part of a call to user.getTopAlbums. For use with Gson's
 * fromJson().
 */
class Album extends MusicItem {

    private Artist artist;
    private class Artist {
        private String name;
    }

    /**
     * @return this album's info in the form of "Artist - Title"
     */
    @Override
    public String toString() {
        return artist.name + " - " + getName();
    }

    /**
     * @return the name of the artist for this album
     */
    public String getArtistName() {
        return artist.name;
    }

}
