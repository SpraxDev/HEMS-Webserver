package de.sprax2013.hems.webserver;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpServer implements Closeable {
    private final Map<String, String> defaultHeaders = new LinkedHashMap<>();

    private final List<HttpRequestListener> requestPreListeners = new ArrayList<>();
    private final Map<String, HttpRequestHandler> requestHandlers = new LinkedHashMap<>();
    private final List<HttpRequestListener> requestPostListeners = new ArrayList<>();

    private final ServerSocket srv;

    private final ExecutorService pool;

    public HttpServer(int port) throws IOException {
        this.defaultHeaders.put("connection", "close");
        this.defaultHeaders.put("referrer-policy", "strict-origin-when-cross-origin");
        this.defaultHeaders.put("server", "HEMS-Webserver (implemented by SpraxDev)");
        this.defaultHeaders.put("x-git", "https://github.com/SpraxDev/HEMS-Webserver");

        addRequestPostListener((req, res) -> {
            Object reqStart = res.getLocal(HttpResponse.LOCALS_KEY_REQ_START);

            String reqTime = "?";
            if (reqStart instanceof Long) {
                long reqNanos = System.nanoTime() - (Long) reqStart;

                reqTime = String.valueOf(BigDecimal.valueOf(reqNanos / 1_000_000.0).setScale(2, RoundingMode.HALF_UP).doubleValue());
            }

            System.out.println("[" + HttpResponse.DATE_FORMATTER.format(LocalDateTime.now()) + "] " +
                    req.getRemoteHost() + " | " +
                    req.getMethod() + " " +
                    req.getRawPath() + " " +
                    res.getStatus().code + " with " +
                    res.getEffectiveBody(req).length + " bytes | " +
                    "\"" + req.getHeader("User-Agent") + "\" | " + reqTime + " ms");
        });

        this.srv = new ServerSocket(port);

        int cpuCount = Runtime.getRuntime().availableProcessors();
        this.pool = Executors.newFixedThreadPool(cpuCount);

        for (int i = 0; i < cpuCount; ++i) {
            pool.execute(() -> {
                while (!srv.isClosed()) {
                    try (Socket client = srv.accept();
                         InputStream in = client.getInputStream();
                         OutputStream out = client.getOutputStream()) {
                        HttpResponse res = new HttpResponse(defaultHeaders);

                        try {
                            HttpRequest req = parseRequest(client, in);

                            for (HttpRequestListener handler : requestPreListeners) {
                                handler.onRequest(req, res);
                            }

                            boolean reqSuccess = false;
                            for (Map.Entry<String, HttpRequestHandler> entry : requestHandlers.entrySet()) {
                                if (req.getFullPath().toLowerCase(Locale.ROOT).startsWith(entry.getKey())) {
                                    req.setPath(req.getFullPath().substring(entry.getKey().length()));

                                    reqSuccess = entry.getValue().onRequest(req, res);
                                }

                                if (reqSuccess) break;
                            }

                            // Handle 404 error
                            if (!reqSuccess) {
                                res.send(HttpStatusCode.NOT_FOUND);
                            }

                            // Send response
                            sendResponse(out, res.getStatus().code, res.getStatus().name, res.getHeaders(),
                                    res.getEffectiveBody(req), req.getMethod().equals("HEAD"));

                            // TODO: Move this call into finally block
                            for (HttpRequestListener handler : requestPostListeners) {
                                handler.onRequest(req, res);
                            }
                        } catch (HttpRequestException httpEx) {
                            res.setHeaders(defaultHeaders);
                            res.send(httpEx.getHttpCode());

                            // TODO: Call postListener for logging
                            sendResponse(out, res.getStatus().code, res.getStatus().name, res.getHeaders(),
                                    res.getBody(), false);  // TODO: Use res.getEffectiveBody(req)
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
        }
    }

    public int getPort() {
        return this.srv.getLocalPort();
    }

    public void addRequestPreListener(HttpRequestListener listener) {
        this.requestPreListeners.add(listener);
    }

    public void addRequestHandler(@NotNull String path, @NotNull HttpRequestHandler handler) {
        this.requestHandlers.put(Objects.requireNonNull(path).toLowerCase(Locale.ROOT), Objects.requireNonNull(handler));
    }

    public void addRequestPostListener(HttpRequestListener listener) {
        this.requestPostListeners.add(listener);
    }

    @Override
    public void close() throws IOException {
        this.srv.close();
        this.pool.shutdown();
    }

    // TODO: Put this inside HttpRequest as #parse(Socket, InputStream), so we have a request object for logging
    private HttpRequest parseRequest(@NotNull Socket client, @NotNull InputStream in) throws IOException, HttpRequestException {
        String reqMethod = null;
        String reqPath = null;
        String reqRawPath = null;

        LinkedHashMap<String, String> queryArgs = new LinkedHashMap<>();
        LinkedHashMap<String, String> headers = new LinkedHashMap<>();
        byte[] reqBody = null;

        String lastHeaderKey = null;
        while (true) {
            String line = new String(readHeaderLine(in));

            // Parse Method, Path and HTTP-Version
            if (reqMethod == null) {
                reqMethod = line.substring(0, line.indexOf(' '));

                if (!reqMethod.equals(reqMethod.toUpperCase(Locale.ROOT))) {
                    throw new HttpRequestException(HttpStatusCode.BAD_REQUEST); // Methods need to be uppercase
                }

                // FIXME: Check if URLDecoder is the way to go. I think a URIDecoder should be used for this
                reqPath = URLDecoder.decode(line.substring(line.indexOf(' ', line.indexOf(' ')) + 1, line.lastIndexOf(' ')), StandardCharsets.UTF_8);

                if (reqPath.isEmpty()) {
                    reqPath = "/";
                }

                reqRawPath = reqPath;

                int queryStartIndex = reqPath.indexOf('?');
                if (queryStartIndex != -1) {
                    String queryStr = reqPath.substring(queryStartIndex + 1);   // e.g. "username=Sprax&darkMode=1"
                    reqPath = reqPath.substring(0, queryStartIndex);

                    for (String qPair : queryStr.split("&")) {
                        int dividerIndex = qPair.indexOf('=');

                        String qKey = URLDecoder.decode(qPair.substring(0, dividerIndex), StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);

                        if (queryArgs.containsKey(qKey)) {
                            // Specs say handle all or tell client he should not use the same key more than once
                            throw new HttpRequestException(HttpStatusCode.BAD_REQUEST);
                        }

                        // FIXME: A URIDecode should be used. URLDecoder may produce wrong output for this kind of input,
                        //        but Java does not have one - Using URLDecoder for now
                        queryArgs.put(qKey,
                                URLDecoder.decode(qPair.substring(dividerIndex + 1), StandardCharsets.UTF_8));
                    }
                }

                String protocol = line.substring(line.lastIndexOf(' ') + 1).trim();
                if (!protocol.equalsIgnoreCase("HTTP/1.1")) {
                    throw new HttpRequestException(HttpStatusCode.HTTP_VERSION_NOT_SUPPORTED);
                }
            } else {    // Parse Header fields
                if (line.isBlank()) break;  // End of headers (begin of body?)

                // Multiline header?
                if (Character.isWhitespace(line.charAt(0))) {
                    headers.put(lastHeaderKey, headers.get(lastHeaderKey) + line.trim());
                } else {
                    lastHeaderKey = line.substring(0, line.indexOf(':')).trim().toLowerCase(Locale.ROOT);
                    headers.put(lastHeaderKey, line.substring(line.indexOf(':') + 1).trim());
                }
            }
        }

        String contentLength = headers.get("content-length");
        if (contentLength != null) {
            try {
                reqBody = in.readNBytes(Integer.parseInt(contentLength));
            } catch (NumberFormatException ignore) {
                throw new HttpRequestException(HttpStatusCode.BAD_REQUEST); // Invalid value for Content-Length
            }
        }

        return new HttpRequest(client.getInetAddress().getHostAddress(), reqMethod, reqRawPath, reqPath, queryArgs, headers, reqBody);
    }

    private void sendResponse(@NotNull OutputStream out, int statusCode, @NotNull String statusMsg,
                              @NotNull Map<String, String> headers, byte[] body, boolean isHeadRequest) throws IOException {
        writeLineCRLF(out, "HTTP/1.1 " + statusCode + " " + statusMsg);

        if (body != null && body.length > 0 && !headers.containsKey("content-length")) {
            headers = new LinkedHashMap<>(headers);
            headers.put("content-length", String.valueOf(body.length));
        }

        sendHeaders(out, headers);

        writeBody(out, isHeadRequest ? null : body);
    }

    private void sendHeaders(@NotNull OutputStream out, @NotNull Map<String, String> headers) throws IOException {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            writeHeader(out, entry.getKey(), entry.getValue());
        }
    }

    private void writeHeader(@NotNull OutputStream out, @NotNull String field, @NotNull String value) throws IOException {
        writeLineCRLF(out, formatHeaderField(field) + ": " + value);
    }

    private void writeBody(@NotNull OutputStream out, byte[] bytes) throws IOException {
        writeLineCRLF(out, null);

        if (bytes != null && bytes.length > 0) {
            out.write(bytes);
        }
    }

    private byte[] readHeaderLine(@NotNull InputStream in) throws IOException, HttpRequestException {
        byte[] buffer = new byte[128];
        int i = 0;

        while (true) {
            if (in.available() > 0) {
                if (i + 1 >= buffer.length) {
                    int newLength = buffer.length * 2;

                    if (newLength > 4096) throw new HttpRequestException(HttpStatusCode.REQUEST_ENTITY_TOO_LARGE);

                    buffer = Arrays.copyOf(buffer, newLength);
                }

                byte b = (byte) in.read();
                buffer[i++] = b;

                if (b == 10 && buffer[i - 2] == 13) {
                    break;
                }
            }
        }

        return Arrays.copyOf(buffer, i - 1);    // Remove "\r\n" and trailing nulls
    }

    private void writeLineCRLF(@NotNull OutputStream out, @Nullable String str) throws IOException {
        if (str != null) {
            out.write(str.getBytes());
        }

        out.write("\r\n".getBytes());
    }

    public static @NotNull String formatHeaderField(@NotNull String field) {
        StringBuilder result = new StringBuilder();

        for (String s : field.toLowerCase().split("-")) {
            if (result.length() > 0) {
                result.append('-');
            }

            result.append(Character.toUpperCase(s.charAt(0)))
                    .append(s.substring(1));
        }

        return result.toString();
    }
}