package tools.vitruv.remote.secserver.handler;

public final class ApiPaths {
    public static final String API_CONTEXT = "/api";

    public static final String API_AUTH_PATH = API_CONTEXT + "/auth";

    public static final String OPENID_BASE_PATH = API_AUTH_PATH + "/oidc";

    public static final String OPENID_LOGIN_PATH = OPENID_BASE_PATH + "/login";

    public static final String OPENID_LOGOUT_PATH = OPENID_BASE_PATH + "/logout";

    public static final String OPENID_REDIRECT_PATH = OPENID_BASE_PATH + "/back";

    public static final String OPENID_ERROR_PATH = OPENID_BASE_PATH + "/error";

    public static final String OPENID_FULL_LOGOUT_REDIRECT_PATH = OPENID_BASE_PATH + "/out";

    // Note: Context path is removed from the path since it is automatically re-added by Jetty on logout.
    public static final String OPENID_LOGOUT_REDIRECT_PATH = OPENID_FULL_LOGOUT_REDIRECT_PATH.substring(API_CONTEXT.length());

    private ApiPaths() {}
}
