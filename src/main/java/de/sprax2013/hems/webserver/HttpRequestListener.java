package de.sprax2013.hems.webserver;

public interface HttpRequestListener {
    void onRequest(HttpRequest req, HttpResponse res);
}