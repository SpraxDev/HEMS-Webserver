package de.sprax2013.hems.webserver.routes;

import de.sprax2013.hems.webserver.HttpRequest;
import de.sprax2013.hems.webserver.HttpRequestHandler;
import de.sprax2013.hems.webserver.HttpResponse;
import de.sprax2013.hems.webserver.HttpServer;
import de.sprax2013.hems.webserver.HttpStatusCode;
import org.apache.commons.text.StringEscapeUtils;

import java.time.LocalDateTime;
import java.util.Map;

public class DebugRoute implements HttpRequestHandler {
    @Override
    public boolean onRequest(HttpRequest req, HttpResponse res) {
        res.setDate(LocalDateTime.now());
        res.setStatus(HttpStatusCode.OK);

        // Try up to 14 times to get the right Content-Length header printed into the HTML (takes 3 iterations on average)
        for (int i = 0; i < 14; i++) {
            int contentLength = i > 1 ? res.getEffectiveBody(req).length : -1;

            Map<String, String> reqHeaders = req.getHeaders();
            Map<String, String> reqQueryArgs = req.getQueryArgs();

            Map<String, String> resHeaders = res.getHeaders();

            res.setHTML("<!DOCTYPE html>" +
                    "<html lang=\"en\">" +
                    "<head>" +
                    "<meta charset=\"utf-8\">" +
                    "<title>Debug-Page</title>" +
                    "</head>" +
                    "<body>" +
                    "<h2>Request</h2>" +
                    "<strong>Method:</strong> " + StringEscapeUtils.escapeHtml4(req.getMethod()) + "<br>" +
                    "<strong>Path:</strong> " + StringEscapeUtils.escapeHtml4(req.getPath()) + "<br>" +
                    "<strong>Headers:</strong> " + (reqHeaders.isEmpty() ? "<em>None</em>" : toHtmlUl(reqHeaders, true)) + "<br>" +
                    "<strong>Query-Arguments:</strong> " + (reqQueryArgs.isEmpty() ? "<em>None</em>" : toHtmlUl(reqQueryArgs, false)) + "<br>" +
                    "<strong>Body-Size:</strong> " + req.getBody().length +

                    "<hr>" +

                    "<h2>Response <small>(<em>Content-Length</em> may be inaccurate for obvious reasons)</small></h2>" +
                    "<strong>Status-Code:</strong> " + res.getStatus().code + "<br>" +
                    "<strong>Status-Name:</strong> " + StringEscapeUtils.escapeHtml4(res.getStatus().name) + "<br>" +
                    "<strong>Headers:</strong> " + (resHeaders.isEmpty() ? "<em>None</em>" : toHtmlUl(resHeaders, true)) + "<br>" +
                    "</body>" +
                    "</html>");

            if (i > 1 && contentLength == res.getEffectiveBody(req).length) break;
        }

        return true;
    }

    private static String toHtmlUl(Map<String, String> map, boolean applyHeaderCasing) {
        StringBuilder result = new StringBuilder("<ul>");

        for (Map.Entry<String, String> entry : map.entrySet()) {
            result.append("<li>")
                    .append("<strong>")
                    .append(StringEscapeUtils.escapeHtml4(applyHeaderCasing ? HttpServer.formatHeaderField(entry.getKey()) : entry.getKey()))
                    .append("</strong>: ")
                    .append(StringEscapeUtils.escapeHtml4(entry.getValue()))
                    .append("</li>");
        }

        result.append("</ul>");
        return result.toString();
    }
}