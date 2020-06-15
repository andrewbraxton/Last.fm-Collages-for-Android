package com.andrewbraxton.lastfmcollages;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class MusicItem {
    private String name;
    private List<Image> image;

    private class Image {
        @SerializedName("#text")
        private String url;
    }

    public String toString() {
        return "MusicItem: " + name;
    }

    public String getName() {
        return name;
    }

    public String getLargestImageUrl() {
        return image.get(image.size() - 1).url; // largest cover art is located at the end of the JSON array
    }
}
