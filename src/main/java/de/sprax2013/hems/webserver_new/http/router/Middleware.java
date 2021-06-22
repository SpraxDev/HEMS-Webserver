package de.sprax2013.hems.webserver_new.http.router;

import de.sprax2013.hems.webserver_new.http.HttpUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public class Middleware {
    private final @Nullable String method;
    private final @NotNull Object path;

    private final @NotNull IRouteCallback callback;

    Middleware(@Nullable String method, @NotNull Object path, @NotNull IRouteCallback callback) {
        if (path instanceof String) {
            this.path = HttpUtils.normalizeToRootPath((String) path);
        } else if (path instanceof Pattern) {
            this.path = path;
        } else if (path instanceof Collection) {
            List<String> pathList = new LinkedList<>();

            for (Object obj : (Collection<?>) path) {
                if (obj == null) continue;

                if (obj instanceof String) {
                    String normalizedPath = HttpUtils.normalizeToRootPath((String) obj);

                    if (!pathList.contains(normalizedPath)) {
                        pathList.add(normalizedPath);
                    }
                } else {
                    throw new UnsupportedOperationException();
                }
            }

            this.path = Collections.unmodifiableList(pathList);
        } else {
            throw new IllegalArgumentException("Invalid type for path");
        }

        this.method = method;
        this.callback = Objects.requireNonNull(callback);
    }

    public @Nullable String getMethod() {
        return this.method;
    }

    boolean matchesMethod(@NotNull String method) {
        return this.method == null || this.method.equals(method);
    }

    public Object getPath() {
        return this.path;
    }

    boolean matchesPath(@NotNull String path, boolean caseSensitive) {
        if (this.path instanceof String) {
            return caseSensitive ?
                    path.startsWith((String) this.path) :
                    path.toLowerCase(Locale.ROOT).startsWith(((String) this.path).toLowerCase(Locale.ROOT));
        }

        if (this.path instanceof Pattern) {
            return ((Pattern) this.path).matcher(path).matches();
        }

        if (this.path instanceof Collection) {
            for (Object l : (Collection<?>) this.path) {
                if (l instanceof String) {
                    if (caseSensitive ? l.equals(path) : ((String) l).equalsIgnoreCase(path)) {
                        return true;
                    }
                } else {
                    throw new UnsupportedOperationException();
                }
            }

            return false;
        }

        throw new UnsupportedOperationException();
    }

    @NotNull
    RouteCallback getCallback() {
        return (RouteCallback) this.callback;
    }

    @NotNull
    RouteExceptionCallback getErrorCallback() {
        return (RouteExceptionCallback) this.callback;
    }
}