package de.sprax2013.hems.webserver;

import org.jetbrains.annotations.NotNull;

public class HttpRequestException extends Exception {
    private final HttpStatusCode httpStatus;

    public HttpRequestException(@NotNull HttpStatusCode httpStatus) {
        super("An error occurred while processing a HTTP request: " + httpStatus.code + " " + httpStatus.name);

        this.httpStatus = httpStatus;
    }

    public HttpStatusCode getHttpCode() {
        return this.httpStatus;
    }
}