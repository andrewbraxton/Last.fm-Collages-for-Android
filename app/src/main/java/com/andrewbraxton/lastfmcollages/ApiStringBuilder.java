package com.andrewbraxton.lastfmcollages;

/**
 * This class provides methods to build URLs for making requests to the Last.fm API.
 * <p>
 * Enter your unique API key before use.
 */
final class ApiStringBuilder {

    private static final String BASE_URL = "https://ws.audioscrobbler.com/2.0/";
    private static final String METHOD = "?method=";
    private static final String GET_TOP_ALBUMS = "user.getTopAlbums";
    private static final String GET_TOP_ARTISTS = "user.getTopArtists";
    private static final String PARAM_APIKEY = "&api_key=";
    private static final String PARAM_USER = "&user=";
    private static final String PARAM_PERIOD = "&period=";
    private static final String PARAM_LIMIT = "&limit=";
    private static final String FORMAT_JSON = "&format=json";

    private static final String API_KEY = "0593ed6567d58548028e7fda4bb55e70";

    /**
     * Builds and returns the URL needed to make a call to user.getTopAlbums.
     *
     * @param user   the Last.fm username to fetch top albums for
     * @param period the time period over which to retrieve top albums for
     * @param limit  the max number of albums to fetch
     */
    public static String buildGetTopAlbumsUrl(String user, String period, int limit) {
        String url = BASE_URL + METHOD + GET_TOP_ALBUMS;
        url += PARAM_APIKEY + API_KEY;
        url += PARAM_USER + user;
        url += PARAM_PERIOD + period;
        url += PARAM_LIMIT + limit;
        url += FORMAT_JSON;
        return url;
    }

    /**
     * Builds and returns the URL needed to make a call to user.getTopArtists.
     *
     * @param user   the Last.fm username to fetch top albums for
     * @param period the time period over which to retrieve top albums for
     * @param limit  the max number of albums to fetch
     */
    public static String buildGetTopArtistsUrl(String user, String period, int limit) {
        String url = BASE_URL + METHOD + GET_TOP_ARTISTS;
        url += PARAM_APIKEY + API_KEY;
        url += PARAM_USER + user;
        url += PARAM_PERIOD + period;
        url += PARAM_LIMIT + limit;
        url += FORMAT_JSON;
        return url;
    }

    private ApiStringBuilder() {} // to make the class non-instantiable
}
