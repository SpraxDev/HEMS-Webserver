package de.sprax2013.hems.webserver_new.http.router;

import de.sprax2013.hems.webserver_new.http.HemsWebServer;
import de.sprax2013.hems.webserver_new.http.HttpMethod;
import de.sprax2013.hems.webserver_new.http.WebRequest;
import de.sprax2013.hems.webserver_new.http.WebResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

@SuppressWarnings({"UnusedReturnValue", "unused"})
public class Router implements RouteCallback {
    private final List<Middleware> middlewares = new LinkedList<>();
    private final List<Middleware> errorMiddlewares = new LinkedList<>();
//    private final List<Middleware> paramMiddlewares = new LinkedList<>();

    protected List<Middleware> getMiddlewares() {
        return middlewares;
    }

    protected List<Middleware> getErrorMiddlewares() {
        return errorMiddlewares;
    }

    public Router param(@NotNull String param, @NotNull RouteCallback middleware) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /* use */

    /**
     * @param middleware A middleware
     *
     * @return The same {@link Router} instance for chaining
     */
    public Router use(@NotNull RouteCallback middleware) {
        return use("/", middleware);
    }

    /**
     * @param path       The path for which the middleware is invoked
     * @param middleware A middleware
     *
     * @return The same {@link Router} instance for chaining
     */
    public Router use(@NotNull String path, @NotNull RouteCallback middleware) {
        registerMiddleware((String) null, path, middleware);

        return this;
    }

    /**
     * @param paths      A list of paths for which the middleware is invoked
     * @param middleware A middleware
     *
     * @return The same {@link Router} instance for chaining
     */
    public Router use(Collection<? extends String> paths, @NotNull RouteCallback middleware) {
        registerMiddleware((String) null, paths, middleware);

        return this;
    }

    /**
     * @param pathPattern The pattern for which the middleware is invoked
     * @param middleware  A middleware
     *
     * @return The same {@link Router} instance for chaining
     */
    public Router use(@NotNull Pattern pathPattern, @NotNull RouteCallback middleware) {
        registerMiddleware((String) null, pathPattern, middleware);

        return this;
    }

    /* use (with error handling) */

    /**
     * @param middleware A middleware
     *
     * @return The same {@link Router} instance for chaining
     */
    public Router use(@NotNull RouteExceptionCallback middleware) {
        return use("/", middleware);
    }

    /**
     * @param path       The path for which the middleware is invoked
     * @param middleware A middleware
     *
     * @return The same {@link Router} instance for chaining
     */
    public Router use(@NotNull String path, @NotNull RouteExceptionCallback middleware) {
        registerErrorMiddleware(path, middleware);

        return this;
    }

    /**
     * @param paths      A list of paths for which the middleware is invoked
     * @param middleware A middleware
     *
     * @return The same {@link Router} instance for chaining
     */
    public Router use(@NotNull Collection<? extends String> paths, @NotNull RouteExceptionCallback middleware) {
        registerErrorMiddleware(paths, middleware);

        return this;
    }

    /**
     * @param pathPattern The pattern for which the middleware is invoked
     * @param middleware  A middleware
     *
     * @return The same {@link Router} instance for chaining
     */
    public Router use(@NotNull Pattern pathPattern, @NotNull RouteExceptionCallback middleware) {
        registerErrorMiddleware(pathPattern, middleware);

        return this;
    }

    /* method */

    /**
     * @param method     The method for wich the middleware is invoked
     * @param path       The path for which the middleware is invoked
     * @param middleware A middleware
     *
     * @return The same {@link Router} instance for chaining
     */
    public Router method(@NotNull String method, @NotNull String path, @NotNull RouteCallback middleware) {
        registerMiddleware(method, path, middleware);

        return this;
    }

    /**
     * @param method     The method for wich the middleware is invoked
     * @param paths      A list of paths for which the middleware is invoked
     * @param middleware A middleware
     *
     * @return The same {@link Router} instance for chaining
     */
    public Router method(@NotNull String method, @NotNull Collection<? extends String> paths, @NotNull RouteCallback middleware) {
        registerMiddleware(method, paths, middleware);

        return this;
    }

    /**
     * @param method      The method for wich the middleware is invoked
     * @param pathPattern The pattern for which the middleware is invoked
     * @param middleware  A middleware
     *
     * @return The same {@link Router} instance for chaining
     */
    public Router method(@NotNull String method, @NotNull Pattern pathPattern, @NotNull RouteCallback middleware) {
        registerMiddleware(method, pathPattern, middleware);

        return this;
    }

    /* HEAD */

    /**
     * @param path       The path for which the middleware is invoked
     * @param middleware A middleware
     *
     * @return The same {@link Router} instance for chaining
     */
    public Router head(@NotNull String path, @NotNull RouteCallback middleware) {
        registerMiddleware(HttpMethod.HEAD, path, middleware);

        return this;
    }

    /**
     * @param paths      A list of paths for which the function is invoked
     * @param middleware A middleware
     *
     * @return The same {@link Router} instance for chaining
     */
    public Router head(@NotNull Collection<? extends String> paths, @NotNull RouteCallback middleware) {
        registerMiddleware(HttpMethod.HEAD, paths, middleware);

        return this;
    }

    /**
     * @param pathPattern The pattern for which the function is invoked
     * @param middleware  A middleware
     *
     * @return The same {@link Router} instance for chaining
     */
    public Router head(@NotNull Pattern pathPattern, @NotNull RouteCallback middleware) {
        registerMiddleware(HttpMethod.HEAD, pathPattern, middleware);

        return this;
    }

    /* GET */

    /**
     * @param path       The path for which the middleware is invoked
     * @param middleware A middleware
     *
     * @return The same {@link Router} instance for chaining
     */
    public Router get(@NotNull String path, @NotNull RouteCallback middleware) {
        registerMiddleware(HttpMethod.GET, path, middleware);

        return this;
    }

    /**
     * @param paths      A list of paths for which the middleware is invoked
     * @param middleware A middleware
     *
     * @return The same {@link Router} instance for chaining
     */
    public Router get(@NotNull Collection<? extends String> paths, @NotNull RouteCallback middleware) {
        registerMiddleware(HttpMethod.GET, paths, middleware);

        return this;
    }

    /**
     * @param pathPattern The pattern for which the middleware is invoked
     * @param middleware  A middleware
     *
     * @return The same {@link Router} instance for chaining
     */
    public Router get(@NotNull Pattern pathPattern, @NotNull RouteCallback middleware) {
        registerMiddleware(HttpMethod.GET, pathPattern, middleware);

        return this;
    }

    /* POST */

    /**
     * @param path       The path for which the middleware is invoked
     * @param middleware A middleware
     *
     * @return The same {@link Router} instance for chaining
     */
    public Router post(@NotNull String path, @NotNull RouteCallback middleware) {
        registerMiddleware(HttpMethod.POST, path, middleware);

        return this;
    }

    /**
     * @param paths      A list of paths for which the middleware is invoked
     * @param middleware A middleware
     *
     * @return The same {@link Router} instance for chaining
     */
    public Router post(@NotNull Collection<? extends String> paths, @NotNull RouteCallback middleware) {
        registerMiddleware(HttpMethod.POST, paths, middleware);

        return this;
    }

    /**
     * @param pathPattern The pattern for which the middleware is invoked
     * @param middleware  A middleware
     *
     * @return The same {@link Router} instance for chaining
     */
    public Router post(@NotNull Pattern pathPattern, @NotNull RouteCallback middleware) {
        registerMiddleware(HttpMethod.POST, pathPattern, middleware);

        return this;
    }

    /* RouteCallback */

    @Override
    public boolean call(@NotNull WebRequest req, @NotNull WebResponse res) {
        final Path startPath = Path.of(req.getPath());

        boolean middlewareSuccess = false;

        try {
            for (Middleware m : getMiddlewares()) {
                if (m.matchesMethod(req.getMethod()) &&
                        m.matchesPath(req.getPath(), false /* TODO */)) {
                    if (!(m.getPath() instanceof String && m.getPath().equals("/"))) {
                        if (startPath.getNameCount() > 1) {
                            req.setPath(startPath.subpath(1, startPath.getNameCount()).toString());
                        } else if (startPath.getNameCount() == 1) {
                            req.setPath("/");
                        }
                    }

                    if (m.getCallback().call(req, res)) {
                        middlewareSuccess = true;

                        break;
                    }

                    req.setPath(startPath.toString());
                }
            }

            // Automatically handle HEAD request
            if (!middlewareSuccess && req.getMethod().equals(HttpMethod.HEAD.name())) {
                for (Middleware m : getMiddlewares()) {
                    if ("GET".equals(m.getMethod()) &&
                            m.matchesPath(req.getPath(), false /* TODO */)) {
                        if (startPath.getNameCount() > 1) {
                            req.setPath(startPath.subpath(1, startPath.getNameCount()).toString());
                        } else if (startPath.getNameCount() == 1) {
                            req.setPath("/");
                        }

                        if (m.getCallback().call(req, res)) {
                            middlewareSuccess = true;

                            break;
                        }

                        req.setPath(startPath.toString());
                    }
                }
            }
        } catch (Exception ex) {
            for (Middleware m : getErrorMiddlewares()) {
                if (m.matchesMethod(req.getMethod())) {
                    if (m.matchesPath(req.getPath(), false /* TODO */)) {
                        if (m.getErrorCallback().call(req, res, ex)) {
                            middlewareSuccess = true;

                            break;
                        }
                    }
                }
            }

            if (!middlewareSuccess) {
                throw ex;
            }
        } finally {
            req.setPath(startPath.toString());
        }

        return true;
    }

    /* Utility */

    private void registerErrorMiddleware(@NotNull Object path, @NotNull RouteExceptionCallback middleware) {
        this.errorMiddlewares.add(new Middleware(null, path, middleware));
    }

    private void registerMiddleware(@Nullable String method, @NotNull Object path, @NotNull RouteCallback middleware) {
        if (middleware instanceof HemsWebServer) {
            throw new IllegalArgumentException("Please use a normal Router instead of an '" + HemsWebServer.class.getName() + "' instance");
        }

        this.middlewares.add(new Middleware(method, path, middleware));
    }

    private void registerMiddleware(@NotNull HttpMethod method, @NotNull Object path, @NotNull RouteCallback middleware) {
        registerMiddleware(method.name(), path, middleware);
    }
}