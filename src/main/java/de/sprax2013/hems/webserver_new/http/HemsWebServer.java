package de.sprax2013.hems.webserver_new.http;

import de.sprax2013.hems.webserver.HttpStatusCode;
import de.sprax2013.hems.webserver_new.NewMain;
import de.sprax2013.hems.webserver_new.http.router.Router;
import org.jetbrains.annotations.NotNull;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HemsWebServer extends Router {
    private final Map<String, String> defaultHeaders = new LinkedHashMap<>();

    private ServerSocket srv;
    private ExecutorService pool;

    private SSLServerSocket secureSrv;
    private ExecutorService securePool;

    public HemsWebServer(boolean logRequests) {
        this.defaultHeaders.put("connection", "close");
        this.defaultHeaders.put("referrer-policy", "strict-origin-when-cross-origin");
        this.defaultHeaders.put("server", "HEMS-Webserver (implemented by SpraxDev)");
        this.defaultHeaders.put("x-git", "https://github.com/SpraxDev/HEMS-Webserver");

        if (logRequests) {
            use((req, res) -> {
                req.onEvent(RequestEvent.CLIENT_DISCONNECTED, (eReq, eRes) -> {
                    Object reqStart = eRes.getLocal(WebResponse.LOCALS_KEY_REQ_START);

                    String reqTime = "?";
                    if (reqStart instanceof Long) {
                        long reqNanos = System.nanoTime() - (Long) reqStart;

                        reqTime = String.valueOf(BigDecimal.valueOf(reqNanos / 1_000_000.0).setScale(2, RoundingMode.HALF_UP).doubleValue());
                    }

                    System.out.println("[" + WebResponse.DATE_FORMATTER.format(LocalDateTime.now()) + "] " +
                            eReq.getRemoteHost() + " | " +
                            eReq.getMethod() + " " +
                            eReq.getRawPath() + " " +
                            eRes.getStatus().code + " with " +
                            eRes.getEffectiveBody(eReq).length + " bytes | " +
                            "\"" + eReq.getHeader("User-Agent") + "\" | " + reqTime + " ms");
                });

                return false;
            });
        }
    }

    public int getPort() {
        return this.srv != null ? this.srv.getLocalPort() : -1;
    }

    public int getSecurePort() {
        return this.secureSrv != null ? this.secureSrv.getLocalPort() : -1;
    }

    public Router route(@NotNull String path) {
        // TODO: Return new or existing route that equals (not just matches) the given path

        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void listen(int port) throws IOException {
        if (this.srv != null && !this.srv.isClosed()) {
            throw new IllegalStateException("Server is already running and listening on port " + this.srv.getLocalPort());
        }

        if (this.pool != null) {
            this.pool.shutdownNow();
        }

        this.srv = new ServerSocket(port);

        int cpuCount = Runtime.getRuntime().availableProcessors();
        this.pool = Executors.newFixedThreadPool(cpuCount);

        for (int i = 0; i < cpuCount; ++i) {
            this.pool.execute(() -> {
                try {
                    while (!this.srv.isClosed()) {
                        WebResponse res = null;
                        WebRequest req = null;

                        boolean invalidProtocol = false;

                        try (Socket client = this.srv.accept();
                             InputStream in = client.getInputStream();
                             OutputStream out = client.getOutputStream()) {
                            res = new WebResponse(this.defaultHeaders);

                            try {
                                req = WebRequest.parse(client, in);

                                if (Objects.equals(req.getHeader("Upgrade-Insecure-Requests"), "1") &&
                                        getSecurePort() != -1) {
                                    String host = req.getHeader("Host");

                                    if (host.lastIndexOf(':') != -1) {
                                        host = host.substring(0, host.lastIndexOf(':'));
                                    }

                                    if (getSecurePort() != 443) {
                                        host += ":" + getSecurePort();
                                    }

                                    res.setStatus(HttpStatusCode.TEMPORARY_REDIRECT)
                                            .setHeader("Location", "https://" + host + req.getRawPath())
                                            .setHeader("Vary", "Upgrade-Insecure-Requests");
                                } else {
                                    call(req, res);
                                }
                            } catch (HttpProtocolException httpEx) {
                                res.setHeaders(this.defaultHeaders);
                                res.send(httpEx.getHttpCode());
                            } catch (Exception ex) {
                                invalidProtocol = ex instanceof UnsupportedOperationException;

                                if (!invalidProtocol) {
                                    ex.printStackTrace();
                                }

                                StringWriter strW = new StringWriter();
                                ex.printStackTrace(new PrintWriter(strW));

                                res.setHeaders(this.defaultHeaders);
                                res.send(HttpStatusCode.INTERNAL_SERVER_ERROR);
                                res.setBody(strW.toString());
                            }

                            // TODO: call preSendEvent
                            HttpUtils.writeHttpResponse(out, res.getStatus().code, res.getStatus().name,
                                    res.getHeaders(), req != null ? res.getEffectiveBody(req) : res.getBody(), false);
                        } catch (Exception ex) {
                            if (!invalidProtocol) {
                                ex.printStackTrace();
                            }
                        } finally {
                            if (req != null) {
                                req.callEvent(RequestEvent.CLIENT_DISCONNECTED, res);
                            }
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
        }
    }

    /**
     * Starts listening for encrypted connection on a given port, using <em>TLSv1.2</em> and <em>TLSv1.3</em>.
     * <br><br>
     * A self-signed certificate is used by default. You can replace it with a your own (e.g.
     * <code>keytool -genkeypair -v -alias SpraxDev/HEMS-WebServer -keyalg EC -keysize 256 -sigalg SHA256withECDSA
     * -keystore srvCert.p12 -storepass hemsWeb_SpraxDev -storetype pkcs12 -validity 5475 -ext san=ip:127.0.0.1,dns:localhost</code>)
     *
     * @param port The port that should be used
     *
     * @throws IllegalStateException When the server is already running
     */
    public void listenSecure(int port) throws IOException, KeyStoreException, CertificateException,
            NoSuchAlgorithmException, UnrecoverableKeyException, KeyManagementException {
        if (this.secureSrv != null && !this.secureSrv.isClosed()) {
            throw new IllegalStateException("Server is already running and listening on port " + this.secureSrv.getLocalPort());
        }

        if (this.securePool != null) {
            this.securePool.shutdownNow();
        }

        try (InputStream certIn = NewMain.class.getResourceAsStream("/srvCert.p12")) {
            final char[] CERT_PASSWORD = "hemsWeb_SpraxDev".toCharArray();

            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(certIn, CERT_PASSWORD);

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, CERT_PASSWORD);

            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(kmf.getKeyManagers(), null, SecureRandom.getInstanceStrong());

            this.secureSrv = (SSLServerSocket) ctx.getServerSocketFactory().createServerSocket(port);
            this.secureSrv.setEnabledProtocols(new String[] {"TLSv1.2", "TLSv1.3"});
        }

        int cpuCount = Runtime.getRuntime().availableProcessors();
        this.securePool = Executors.newFixedThreadPool(cpuCount);

        for (int i = 0; i < cpuCount; ++i) {
            this.securePool.execute(() -> {
                try {
                    while (!this.secureSrv.isClosed()) {
                        WebResponse res = null;
                        WebRequest req = null;

                        boolean invalidProtocol = false;

                        try (SSLSocket client = (SSLSocket) this.secureSrv.accept();
                             InputStream in = client.getInputStream();
                             OutputStream out = client.getOutputStream()) {
                            res = new WebResponse(this.defaultHeaders);

                            try {
                                req = WebRequest.parse(client, in);

                                call(req, res);
                            } catch (HttpProtocolException httpEx) {
                                res.setHeaders(this.defaultHeaders);
                                res.send(httpEx.getHttpCode());
                            } catch (SSLException ex) {
                                throw ex; // Don't send any data to the client, just disconnect
                            } catch (Exception ex) {
                                invalidProtocol = ex instanceof UnsupportedOperationException;

                                if (!invalidProtocol) {
                                    ex.printStackTrace();
                                }

                                StringWriter strW = new StringWriter();
                                ex.printStackTrace(new PrintWriter(strW));

                                res.setHeaders(this.defaultHeaders);
                                res.send(HttpStatusCode.INTERNAL_SERVER_ERROR);
                                res.setBody(strW.toString());
                            }

                            // TODO: call preSendEvent
                            HttpUtils.writeHttpResponse(out, res.getStatus().code, res.getStatus().name,
                                    res.getHeaders(), req != null ? res.getEffectiveBody(req) : res.getBody(), false);
                        } catch (SSLException ex) {
                            System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
                        } catch (Exception ex) {
                            if (!invalidProtocol) {
                                ex.printStackTrace();
                            }
                        } finally {
                            if (req != null) {
                                req.callEvent(RequestEvent.CLIENT_DISCONNECTED, res);
                            }
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
        }
    }

    @Override
    public boolean call(@NotNull WebRequest req, @NotNull WebResponse res) {
        try {
            return super.call(req, res);
        } catch (Exception ex) {
            ex.printStackTrace();   // TODO: Send error to client if headers not sent, else close connection
        }

        return true;
    }
}