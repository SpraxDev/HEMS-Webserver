package de.sprax2013.hems.webserver.routes;

import de.sprax2013.hems.webserver.HttpRequest;
import de.sprax2013.hems.webserver.HttpRequestHandler;
import de.sprax2013.hems.webserver.HttpResponse;
import de.sprax2013.hems.webserver.HttpStatusCode;
import org.apache.commons.text.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileIndexRoute implements HttpRequestHandler {
    private final Path path;

    public FileIndexRoute(@NotNull File file) {
        this.path = file.toPath().normalize();
    }

    @Override
    public boolean onRequest(HttpRequest req, HttpResponse res) {
        Path resolvedPath = Path.of(this.path.toString(), req.getPath()).normalize();

        if (resolvedPath.startsWith(this.path)) {
            StringBuilder fileIndex = new StringBuilder();

            File file = resolvedPath.toFile();

            if (file.exists()) {
                if (file.isDirectory()) {
                    String resolvedPathFriendly = "/" + this.path.relativize(resolvedPath).toString();

                    String body = "<!DOCTYPE html>" +
                            "<html lang=\"en\">" +
                            "<head>" +
                            "<meta charset=\"utf-8\">" +
                            "<title>Index of " + StringEscapeUtils.escapeHtml4(resolvedPathFriendly) + "</title>" +
                            "</head>" +
                            "<body>" +
                            "<h1>Index of " +
                            StringEscapeUtils.escapeHtml4(resolvedPathFriendly) +
                            "</h1>" +
                            "${FileIndex}" +
                            "</body>" +
                            "</html>";

                    File[] files = file.listFiles();    // TODO: Sort alphabetically (directories first)
                    if (files != null) {
                        if (!resolvedPath.equals(this.path)) {
                            fileIndex.append("<a href=\"")
                                    .append(Path.of(req.getFullPath(), "..").normalize().toString())
                                    .append("\">")

                                    .append("..")

                                    .append("</a>")
                                    .append("<br>");
                        }

                        if (files.length > 0) {
                            for (File f : files) {
                                if (fileIndex.length() > 0) {
                                    fileIndex.append("<br>");
                                }

                                fileIndex.append("<a href=\"")
                                        .append(Path.of(req.getFullPath(), URLEncoder.encode(f.getName(), StandardCharsets.UTF_8)).toString())
                                        .append("\">")

                                        .append(f.isDirectory() ? "[D] " : "")
                                        .append(StringEscapeUtils.escapeHtml4(f.getName()))

                                        .append("</a>");
                            }
                        } else {
                            fileIndex.append("<br>No files.");
                        }
                    } else {
                        fileIndex.append("<br>Path does not exist.");
                    }

                    res.setStatus(HttpStatusCode.OK)
                            .setHTML(body.replace("${FileIndex}", fileIndex.toString()));
                } else {
                    try {
                        byte[] fileData = Files.readAllBytes(file.toPath());

                        String contentType = URLConnection.guessContentTypeFromName(file.getName());
                        if (contentType == null) {
                            contentType = URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(fileData));
                        }

                        res.setStatus(HttpStatusCode.OK)
                                .setContentType(contentType)
                                .setBody(fileData);
                    } catch (IOException ex) {
                        ex.printStackTrace();

                        res.send(HttpStatusCode.INTERNAL_SERVER_ERROR);
                    }
                }
            }
        } else {
            res.send(HttpStatusCode.FORBIDDEN);
        }

        return true;
    }
}