package de.sprax2013.hems.webserver_new.http;

import de.sprax2013.hems.webserver.HttpStatusCode;
import org.jetbrains.annotations.NotNull;

public class HttpProtocolException extends Exception {
    private final HttpStatusCode httpStatus;

    public HttpProtocolException(@NotNull HttpStatusCode httpStatus) {
        super("An error occurred while processing a HTTP request: " + httpStatus.code + " " + httpStatus.name);

        this.httpStatus = httpStatus;
    }

    public HttpStatusCode getHttpCode() {
        return this.httpStatus;
    }
}