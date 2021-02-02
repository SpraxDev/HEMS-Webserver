package de.sprax2013.hems.webserver.routes;

import de.sprax2013.hems.webserver.HttpRequest;
import de.sprax2013.hems.webserver.HttpRequestHandler;
import de.sprax2013.hems.webserver.HttpResponse;
import de.sprax2013.hems.webserver.HttpStatusCode;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;

public class ServingRoute implements HttpRequestHandler {
    private final Path path;

    public ServingRoute(@NotNull File file) {
        this.path = file.toPath().normalize();
    }

    @Override
    public boolean onRequest(HttpRequest req, HttpResponse res) {
        Path resolvedPath = Path.of(this.path.toString(), req.getPath()).normalize();

        if (resolvedPath.startsWith(this.path)) {
            File file = resolvedPath.toFile();

            if (file.isDirectory() && new File(file, "index.html").exists()) {
                file = new File(file, "index.html");
            }

            if (file.exists() && file.isFile()) {
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
        } else {
            res.send(HttpStatusCode.FORBIDDEN);
        }

        return true;
    }
}