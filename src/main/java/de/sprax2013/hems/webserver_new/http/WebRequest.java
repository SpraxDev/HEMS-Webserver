package de.sprax2013.hems.webserver_new.http;

import de.sprax2013.hems.webserver.HttpStatusCode;
import de.sprax2013.hems.webserver_new.http.router.RequestEventCallback;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings("unused")
public class WebRequest {
    private final @NotNull String remoteHost;

    private final @NotNull String method;
    private final @NotNull String fullPath;
    private final @NotNull String rawPath;

    private final @NotNull Map<String, String> queryArgs;
    private final @NotNull Map<String, String> headers;
    private final byte[] body;

    private @NotNull String path;

    private Map<RequestEvent, List<RequestEventCallback>> eventHandlers;

    // TODO: Only parse get and post params when accessing them
    // TODO: add #getMountPath() e.g. srv.get("/home", obj) "/home/foo.txt" returns "/home"
    // TODO: add #getProtocol() returning http or https
    // TODO: add #isHttps() returning true if #getProtocol() == "https"
    // TODO: parse cookies

    private WebRequest(@NotNull String remoteHost, @NotNull String method, @NotNull String rawPath, @NotNull String path,
                       @NotNull LinkedHashMap<String, String> queryArgs, @NotNull LinkedHashMap<String, String> headers, byte[] body) {
        this.remoteHost = Objects.requireNonNull(remoteHost);

        this.method = Objects.requireNonNull(method);
        this.fullPath = Objects.requireNonNull(path);
        this.rawPath = Objects.requireNonNull(rawPath);

        this.queryArgs = Collections.unmodifiableMap(Objects.requireNonNull(queryArgs));
        this.headers = Collections.unmodifiableMap(Objects.requireNonNull(headers));
        this.body = body != null ? body : new byte[0];

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey() == null ||
                    !entry.getKey().equals(entry.getKey().toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("Header fields need to be lower case strings");
            }
        }

        this.path = this.fullPath;
    }

    @NotNull
    public String getRemoteHost() {
        return this.remoteHost;
    }

    @NotNull
    public String getMethod() {
        return this.method;
    }

    @NotNull
    public String getFullPath() {
        return this.fullPath;
    }

    @NotNull
    public String getRawPath() {
        return this.rawPath;
    }

    // TODO: Do not expose this method to public
    public void setPath(@NotNull String path) {
        this.path = HttpUtils.normalizeToRootPath(Objects.requireNonNull(path));
    }

    /**
     * <b>When called from a middleware, the mount point is not included!</b>
     *
     * @return The path part of the request URL
     *
     * @see #getFullPath()
     */
    @NotNull
    public String getPath() {
        return this.path;
    }

    public String getHeader(String header) {
        return this.headers.get(header.toLowerCase(Locale.ROOT));
    }

    public @NotNull Map<String, String> getHeaders() {
        return this.headers;
    }

    @Nullable
    public String getQueryArg(String key) {
        return this.queryArgs.get(key.toLowerCase(Locale.ROOT));
    }

    public @NotNull Map<String, String> getQueryArgs() {
        return this.queryArgs;
    }

    public byte[] getBody() {
        return Arrays.copyOf(this.body, this.body.length);
    }

    public String getParam(@NotNull String paramName) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void onEvent(@NotNull RequestEvent event, @NotNull RequestEventCallback callback) {
        if (this.eventHandlers == null) {
            this.eventHandlers = new HashMap<>(1);
        }

        this.eventHandlers.computeIfAbsent(event, e -> new LinkedList<>());
        this.eventHandlers.get(event).add(callback);
    }

    void callEvent(@NotNull RequestEvent event, @NotNull WebResponse res) {
        if (this.eventHandlers != null) {
            List<RequestEventCallback> callbacks = this.eventHandlers.get(event);

            if (callbacks != null) {
                for (RequestEventCallback c : callbacks) {
                    c.call(this, res);
                }
            }
        }
    }

    public static WebRequest parse(@NotNull Socket client, @NotNull InputStream in) throws IOException, HttpProtocolException {
        String reqMethod = null;
        String reqPath = null;
        String reqRawPath = null;

        LinkedHashMap<String, String> queryArgs = new LinkedHashMap<>();
        LinkedHashMap<String, String> headers = new LinkedHashMap<>();

        byte[] reqBody = null;
        String multiPartBoundary = null;

        String lastHeaderKey = null;
        while (true) {
            // Parse Method, Path and HTTP-Version
            if (reqMethod == null) {
                byte firstByte = in.readNBytes(1)[0];

                if (!Character.isAlphabetic(firstByte)) {
                    throw new UnsupportedOperationException("Received invalid first byte from client - Wrong protocol?");
                }

                String line = new String(new byte[] {firstByte}, StandardCharsets.UTF_8) + new String(HttpUtils.readHttpHeaderLine(in), StandardCharsets.UTF_8);

                reqMethod = line.substring(0, line.indexOf(' '));

                if (!reqMethod.equals(reqMethod.toUpperCase(Locale.ROOT))) {
                    throw new HttpProtocolException(HttpStatusCode.BAD_REQUEST); // Methods need to be uppercase
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
                            // According to RFC2616: handle all or tell client he should not use the same key more than once
                            throw new HttpProtocolException(HttpStatusCode.BAD_REQUEST);
                        }

                        // FIXME: A URIDecode should be used. URLDecoder may produce wrong output for this kind of input,
                        //        but Java does not have one - Using URLDecoder for now
                        queryArgs.put(qKey,
                                URLDecoder.decode(qPair.substring(dividerIndex + 1), StandardCharsets.UTF_8));
                    }
                }

                String protocol = line.substring(line.lastIndexOf(' ') + 1).trim();
                if (!protocol.equalsIgnoreCase("HTTP/1.1")) {
                    throw new HttpProtocolException(HttpStatusCode.HTTP_VERSION_NOT_SUPPORTED);
                }
            } else {    // Parse Header fields
                String line = new String(HttpUtils.readHttpHeaderLine(in), StandardCharsets.UTF_8);

                if (line.isBlank()) break;  // End of headers (begin of body?)

                // Multiline header?
                if (Character.isWhitespace(line.charAt(0))) {
                    headers.put(lastHeaderKey, headers.get(lastHeaderKey) + line.trim());
                } else {
                    lastHeaderKey = line.substring(0, line.indexOf(':')).trim().toLowerCase(Locale.ROOT);
                    String headerValue = line.substring(line.indexOf(':') + 1).trim();

                    headers.put(lastHeaderKey, headerValue);
                }
            }
        }

        if (!reqMethod.equals(HttpMethod.GET.name())) {
            String contentLength = headers.get("content-length");
            if (contentLength != null) {
                try {
                    reqBody = in.readNBytes(Integer.parseInt(contentLength));
                } catch (NumberFormatException ignore) {
                    throw new HttpProtocolException(HttpStatusCode.BAD_REQUEST); // Invalid value for Content-Length
                }
            }
        }

        WebRequest result = new WebRequest(client.getInetAddress().getHostAddress(), reqMethod, reqRawPath, reqPath, queryArgs, headers, reqBody);

        if (result.getHeader("Host") == null || result.getHeader("Host").isBlank()) {
            throw new HttpProtocolException(HttpStatusCode.BAD_REQUEST);
        }

        return result;
    }
}