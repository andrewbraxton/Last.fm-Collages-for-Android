package com.andrewbraxton.lastfmcollagesforandroid;

/**
 * This class provides methods to build URLs for making requests to the Last.fm API.
 * <p>
 * Enter your unique API key before use.
 */
final class ApiStringBuilder {

    private static final String BASE_URL = "https://ws.audioscrobbler.com/2.0/";
    private static final String METHOD = "?method=";
    private static final String USER_GET_CHART = "user.getweeklyalbumchart";
    private static final String ALBUM_GET_INFO = "album.getinfo";
    private static final String PARAM_APIKEY = "&api_key=";
    private static final String PARAM_USER = "&user=";
    private static final String PARAM_FROMDATE = "&from=";
    private static final String PARAM_TODATE = "&to=";
    private static final String PARAM_ARTIST = "&artist=";
    private static final String PARAM_ALBUM = "&album=";
    private static final String FORMAT_JSON = "&format=json";

    private static final String API_KEY = "";

    /**
     * Returns the URL needed to make a call to user.getWeeklyAlbumChart.
     *
     * @param user     the Last.fm username to fetch the chart of
     * @param fromDate the Unix timestamp of the date at which the chart should start from
     * @param toDate   the Unix timestamp of the date at which the chart should end on
     */
    static String buildGetAlbumChartUrl(String user, long fromDate, long toDate) {
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
    static String buildGetAlbumInfoUrl(Album album) {
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
     * so character list is possibly not exhaustive.
     *
     * @param str the string to percent-encode
     */
    private static String percentEncodeString(String str) {
        str = str.replace("%", "%25"); // percent symbol must go first!
        str = str.replace(" ", "%20");
        str = str.replace("#", "%23");
        str = str.replace("$", "%24");
        str = str.replace("&", "%26");
        str = str.replace("+", "%2B");
        str = str.replace(",", "%2C");
        str = str.replace("/", "%2F");
        str = str.replace(":", "%3A");
        str = str.replace(";", "%3B");
        str = str.replace("<", "%3C");
        str = str.replace("=", "%3D");
        str = str.replace(">", "%3E");
        str = str.replace("?", "%3F");
        str = str.replace("@", "%40");
        str = str.replace("[", "%5B");
        str = str.replace("\\", "%5C");
        str = str.replace("^", "%5E");
        str = str.replace("]", "%5D");
        str = str.replace("{", "%7B");
        str = str.replace("|", "%7C");
        str = str.replace("}", "%7D");
        return str;
    }

    private ApiStringBuilder() {} // to make the class non-instantiable
}
