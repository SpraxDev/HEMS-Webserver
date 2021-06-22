package de.sprax2013.hems.webserver_new.http.router;

import de.sprax2013.hems.webserver_new.http.WebRequest;
import de.sprax2013.hems.webserver_new.http.WebResponse;
import org.jetbrains.annotations.NotNull;

public interface RouteExceptionCallback extends IRouteCallback {
    /**
     * @param req The current request being served
     * @param res The current response being sent
     * @param err A exception that has been thrown while processing a request
     *
     * @return true, if the request has been fulfilled, false if the Router should try calling the next matching Callback
     */
    boolean call(@NotNull WebRequest req, @NotNull WebResponse res, @NotNull Exception err);
}