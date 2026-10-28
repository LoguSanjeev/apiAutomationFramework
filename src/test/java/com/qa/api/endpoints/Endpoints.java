package com.qa.api.endpoints;

/** Keep API paths here. The base URL stays in the environment property files. */
public final class Endpoints {
    public static final String POSTS = "/posts";
    public static final String POST_BY_ID = POSTS + "/{id}";
    public static final String INVALID_ROUTE = "/route-that-does-not-exist";

    public static final String AUTH_LOGIN = "/auth/login";
    public static final String AUTH_REGISTER = "/auth/register";
    public static final String AUTH_REFRESH = "/auth/refresh";
    public static final String AUTH_ME = "/auth/me";
    public static final String AUTH_LOGOUT = "/auth/logout";

    private Endpoints() { }
}
