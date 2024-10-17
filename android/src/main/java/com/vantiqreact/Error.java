package com.vantiqreact;

public class Error
{

    /* error codes returned by rejected promises */

    /*
     * veNotAuthorized
     *
     * Returned when a REST-related call fails because of an expired token that cannot be refreshed.
     * This is to be expected and should be followed by a call to either authWithOAuth or authWithInternal.
     *
     */
    public static final String veNotAuthorized = "com.vantiq.notAuthorized";

    /*
     * veRESTError
     *
     * Returned when a REST-related call fails because of a Vantiq API error. This is a developer error of some sort
     * because of, for example, a malformed request or a missing referenced Type or Procedure.
     *
     */
    public static final String veRESTError = "com.vantiq.RESTError";

    /*
     * veOSError
     *
     * Returned when a REST-related call fails because of a network infrastructure error. This should be an
     * uncommon error.
     *
     */
    public static final String veOSError = "com.vantiq.OSError";

    /*
     * veServerType
     *
     * Returned when the Vantiq server type (Internal or OAuth) cannot be determined. This indicates a network error
     * or a bad server URL passed to the init method.
     *
     */
    public static final String veServerType = "com.vantiq.serverTypeUnknown";

    /*
     * veInvalidAuthToken
     *
     * Returned by the init method when a saved access token has expired and cannot be refreshed. This is to be expected
     * and should be followed by a call to either authWithOAuth or authWithInternal.
     *
     */
    public static final String veInvalidAuthToken = "com.vantiq.invalidAuthToken";

    /*
     * veJsonParseError
     *
     * An error was found while parsing a JSON string - this is an internal error that should never happen
     *
     */
    public static final String veJsonParseError = "com.vantiq.JsonParseError";
}
