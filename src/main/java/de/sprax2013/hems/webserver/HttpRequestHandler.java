package de.sprax2013.hems.webserver;

public interface HttpRequestHandler {
    boolean onRequest(HttpRequest req, HttpResponse res);
}