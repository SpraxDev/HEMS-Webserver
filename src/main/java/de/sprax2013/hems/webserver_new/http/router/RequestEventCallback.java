package de.sprax2013.hems.webserver_new.http.router;

import de.sprax2013.hems.webserver_new.http.WebRequest;
import de.sprax2013.hems.webserver_new.http.WebResponse;
import org.jetbrains.annotations.NotNull;

public interface RequestEventCallback extends IRouteCallback {
    /**
     * @param req The current request being served
     * @param res The current response being sent
     */
    void call(@NotNull WebRequest req, @NotNull WebResponse res);
}