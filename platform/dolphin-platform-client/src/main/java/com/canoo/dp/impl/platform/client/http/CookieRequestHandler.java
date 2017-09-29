package com.canoo.dp.impl.platform.client.http;

import com.canoo.dp.impl.platform.core.Assert;
import com.canoo.platform.client.http.HttpURLConnectionHandler;
import com.canoo.platform.core.DolphinRuntimeException;

import java.net.HttpURLConnection;
import java.net.URISyntaxException;

/**
 * Created by hendrikebbers on 29.09.17.
 */
public class CookieRequestHandler implements HttpURLConnectionHandler {

    private final HttpClientCookieHandler clientCookieHandler;

    public CookieRequestHandler(final HttpClientCookieHandler clientCookieHandler) {
        this.clientCookieHandler = Assert.requireNonNull(clientCookieHandler, "clientCookieHandler");
    }

    @Override
    public void handle(final HttpURLConnection connection) {
        Assert.requireNonNull(connection, "connection");
        try {
            clientCookieHandler.setRequestCookies(connection);
        } catch (URISyntaxException e) {
            throw new DolphinRuntimeException("Can not set cookies", e);
        }
    }
}
