package de.sprax2013.hems.webserver_new.routes;

import de.sprax2013.hems.webserver.HttpStatusCode;
import de.sprax2013.hems.webserver_new.http.HttpUtils;
import de.sprax2013.hems.webserver_new.http.WebRequest;
import de.sprax2013.hems.webserver_new.http.WebResponse;
import de.sprax2013.hems.webserver_new.http.router.RouteCallback;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;

public class ServingRoute implements RouteCallback {
    private final Path path;

    public ServingRoute(@NotNull File file) {
        this.path = file.toPath().normalize();
    }

    @Override
    public boolean call(@NotNull WebRequest req, @NotNull WebResponse res) {
        Path resolvedPath = Path.of(this.path.toString(), req.getPath()).normalize();

        if (resolvedPath.startsWith(this.path)) {
            File file = resolvedPath.toFile();

            if (file.isDirectory() && new File(file, "index.html").exists()) {
                file = new File(file, "index.html");
            }

            if (file.exists() && file.isFile()) {
                try (FileInputStream fIn = new FileInputStream(file)) {
                    String contentType = URLConnection.guessContentTypeFromName(file.getName());
                    if (contentType == null) {
                        contentType = URLConnection.guessContentTypeFromStream(fIn);
                    }

                    res.setContentType(contentType);

                    String rangeStr = req.getHeader("Range");
                    if (rangeStr != null &&
                            rangeStr.indexOf(',') == -1 &&
                            rangeStr.startsWith("bytes=")) {
                        String[] s = rangeStr.substring(6).split("-");

                        if (s.length > 2) {
                            res.send(HttpStatusCode.BAD_REQUEST);
                        } else {
                            int fileSize = Math.toIntExact(Files.size(file.toPath()));

                            int fileStart = Integer.parseInt(s[0], 10);
                            int fileEnd = s.length == 2 ?
                                    Integer.parseInt(s[1], 10) :
                                    fileSize;

                            if (fileEnd > fileSize) {
                                res.send(HttpStatusCode.REQUESTED_RANGE_NOT_SATISFIABLE);
                            } else {
                                res.setStatus(HttpStatusCode.PARTIAL_CONTENT)
                                        .setHeader("Content-Range", "bytes " + fileStart + "-" + fileEnd + "/" + fileSize)
                                        .setBody(HttpUtils.readByteRange(file, fileStart, fileEnd));
                            }
                        }
                    } else {
                        res.setStatus(HttpStatusCode.OK)
                                .setBody(Files.readAllBytes(file.toPath()));
                    }
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