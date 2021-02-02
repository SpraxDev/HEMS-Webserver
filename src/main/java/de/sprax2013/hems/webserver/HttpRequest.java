package de.sprax2013.hems.webserver;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class HttpRequest {
    private final @NotNull String remoteHost;

    private final @NotNull String method;
    private @NotNull String path;
    private final @NotNull String fullPath;
    private final @NotNull String rawPath;

    private final @NotNull LinkedHashMap<String, String> queryArgs;
    private final @NotNull LinkedHashMap<String, String> headers;
    private final byte[] body;

    public HttpRequest(@NotNull String remoteHost, @NotNull String method, @NotNull String rawPath, @NotNull String path,
                       @NotNull LinkedHashMap<String, String> queryArgs, @NotNull LinkedHashMap<String, String> headers, byte[] body) {
        this.remoteHost = Objects.requireNonNull(remoteHost);

        this.method = Objects.requireNonNull(method);
        this.path = Objects.requireNonNull(path);
        this.fullPath = this.path;
        this.rawPath = Objects.requireNonNull(rawPath);

        this.queryArgs = Objects.requireNonNull(queryArgs);
        this.headers = Objects.requireNonNull(headers);
        this.body = body != null ? body : new byte[0];

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey() == null ||
                    !entry.getKey().equals(entry.getKey().toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("Header fields need to be lower case strings");
            }
        }
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

    public void setPath(@NotNull String path) {
        this.path = Objects.requireNonNull(path);
    }

    @NotNull
    public String getPath() {
        return this.path;
    }

    public String getHeader(String header) {
        return this.headers.get(header.toLowerCase(Locale.ROOT));
    }

    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    @Nullable
    public String getQueryArg(String key) {
        return this.queryArgs.get(key.toLowerCase(Locale.ROOT));
    }

    public Map<String, String> getQueryArgs() {
        return Collections.unmodifiableMap(queryArgs);
    }

    public byte[] getBody() {
        return Arrays.copyOf(this.body, this.body.length);
    }
}