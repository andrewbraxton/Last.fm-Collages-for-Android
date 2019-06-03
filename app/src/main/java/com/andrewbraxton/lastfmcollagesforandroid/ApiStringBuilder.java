package com.andrewbraxton.lastfmcollagesforandroid;

/**
 * This class provides methods to build URLs for making requests to the Last.fm API.
 * <p>
 * Enter your unique API key and secret into their respective fields before use.
 */
public final class ApiStringBuilder {
    // TODO: Javadoc

    public static final String BASE_URL = "https://ws.audioscrobbler.com/2.0/";
    public static final String METHOD = "?method=";
    public static final String USER_GET_CHART = "user.getweeklyalbumchart";
    public static final String ALBUM_GET_INFO = "album.getinfo";
    public static final String PARAM_APIKEY = "&api_key=";
    public static final String PARAM_USER = "&user=";
    public static final String PARAM_FROMDATE = "&from=";
    public static final String PARAM_TODATE = "&to=";
    public static final String PARAM_ARTIST = "&artist=";
    public static final String PARAM_ALBUM = "&album=";
    public static final String PARAM_MBID = "&mbid=";
    public static final String FORMAT_JSON = "&format=json";

    public static final String API_KEY = "";
    public static final String API_SECRET = "";

    public static final String buildGetAlbumChartUrl(String user, long fromDate, long toDate) {
        String url = BASE_URL + METHOD + USER_GET_CHART;
        url += PARAM_APIKEY + API_KEY;
        url += PARAM_USER + user;
        url += PARAM_FROMDATE + fromDate + PARAM_TODATE + toDate;
        url += FORMAT_JSON;
        return url;
    }

    public static final String buildGetAlbumInfoUrl(Album album) {
        // TODO: look into MBID weirdness
        String url = BASE_URL + METHOD + ALBUM_GET_INFO;
        url += PARAM_APIKEY + API_KEY;
        url += PARAM_ARTIST + album.getArtistName() + PARAM_ALBUM + album.getName();
        //url += PARAM_MBID + album.getMbid();
        url += FORMAT_JSON;
        return url;
    }

    private ApiStringBuilder() {} // to make the class non-instantiable
}