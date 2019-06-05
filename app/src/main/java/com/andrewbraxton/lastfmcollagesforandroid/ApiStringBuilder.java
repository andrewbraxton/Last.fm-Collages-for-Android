package com.andrewbraxton.lastfmcollagesforandroid;

/**
 * This class provides methods to build URLs for making requests to the Last.fm API.
 * <p>
 * Enter your unique API key and secret into their respective fields before use.
 */
public final class ApiStringBuilder {

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
    public static final String FORMAT_JSON = "&format=json";

    public static final String API_KEY = "";
    public static final String API_SECRET = "";

    /**
     * Returns the URL needed to make a call to user.getWeeklyAlbumChart.
     *
     * @param user     the Last.fm username to fetch the chart of
     * @param fromDate the Unix timestamp of the date at which the chart should start from
     * @param toDate   the Unix timestamp of the date at which the chart should end on
     */
    public static final String buildGetAlbumChartUrl(String user, long fromDate, long toDate) {
        String url = BASE_URL + METHOD + USER_GET_CHART;
        url += PARAM_APIKEY + API_KEY;
        url += PARAM_USER + user;
        url += PARAM_FROMDATE + fromDate + PARAM_TODATE + toDate;
        url += FORMAT_JSON;
        return url;
    }

    /**
     * Returns the URL needed to make a call to album.getAlbumInfo.
     *
     * @param album the album to fetch the info for
     */
    public static final String buildGetAlbumInfoUrl(Album album) {
        String url = BASE_URL + METHOD + ALBUM_GET_INFO;
        url += PARAM_APIKEY + API_KEY;
        url += PARAM_ARTIST + percentEncodeString(album.getArtistName());
        url += PARAM_ALBUM + percentEncodeString(album.getName());
        url += FORMAT_JSON;
        return url;
    }

    /**
     * Returns a string with its special characters like ' ' and '&' replaced with their respective percent-encodings.
     * Needed for artists/albums with certain special characters in their titles. Character replacements are hard-coded
     * so character list is not exhaustive.
     *
     * @param str the string to percent-encode
     */
    private static final String percentEncodeString(String str) {
        // TODO: add more special characters to replace
        str = str.replace(" ", "%20");
        str = str.replace("&", "%26");
        return str;
    }

    private ApiStringBuilder() {} // to make the class non-instantiable
}