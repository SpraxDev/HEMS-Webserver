package de.sprax2013.hems.webserver_new.http;

import de.sprax2013.hems.webserver.HttpResponse;
import de.sprax2013.hems.webserver.HttpStatusCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

@SuppressWarnings({"UnusedReturnValue", "unused"})
public class WebResponse {
    // TODO: add #setCookie
    // TODO: add #redirect(status, path)

    public static final String LOCALS_KEY_REQ_START = "reqStart@" + HttpResponse.class.getName();
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH).withZone(ZoneId.of("GMT"));

    private HttpStatusCode status;
    private byte[] body;
    private byte[] effectiveBody;

    private final Map<String, String> headers = new LinkedHashMap<>();

    private final HashMap<String, Object> locals = new HashMap<>(1);

    public WebResponse(@Nullable Map<String, String> defaultHeaders) {
        setLocal(HttpResponse.LOCALS_KEY_REQ_START, System.nanoTime());

        send(HttpStatusCode.NOT_FOUND);

        if (defaultHeaders != null) {
            this.headers.putAll(defaultHeaders);
        }
    }

    public WebResponse send(HttpStatusCode status) {
        this.status = status;
        setDate(LocalDateTime.now());

        setHTML("<!DOCTYPE html>" +
                "<html lang=\"en\">" +
                "<head>" +
                "<meta charset=\"utf-8\">" +
                "<title>" + status.code + " - " + status.name + "</title>" +
                "</head>" +
                "<body>" +
                "<h1>" + status.code + " - " + status.name + "</h1>" +
                "</body>" +
                "</html>");

        return this;
    }

    public WebResponse setStatus(int status) {
        this.status = HttpStatusCode.getByCode(status);

        return this;
    }

    public WebResponse setStatus(HttpStatusCode status) {
        this.status = status;

        return this;
    }

    public HttpStatusCode getStatus() {
        return this.status;
    }

    public byte[] getBody() {
        return this.body;
    }

    public byte[] getEffectiveBody(WebRequest req) {
        if (this.effectiveBody == null) {
            if (this.body.length > 0 && req.getHeader("Accept-Encoding") != null) {
                String[] acceptedEncodings = req.getHeader("Accept-Encoding")
                        .toLowerCase(Locale.ROOT)
                        .split(",");

                for (String s : acceptedEncodings) {
                    s = s.trim();

                    if (s.equals("gzip")) {
                        try {
                            byte[] compressedBody = gzipData(this.body);

                            if (compressedBody.length < this.body.length) {
                                this.effectiveBody = compressedBody;
                                setHeader("Content-Encoding", "gzip");
                            }
                        } catch (IOException ex) {
                            ex.printStackTrace();

                            this.effectiveBody = this.body;
                        }

                        break;
                    } else if (s.equals("deflate")) {
                        try {
                            byte[] compressedBody = deflateData(this.body);

                            if (compressedBody.length < this.body.length) {
                                this.effectiveBody = compressedBody;
                                setHeader("Content-Encoding", "deflate");
                            }
                        } catch (IOException ex) {
                            ex.printStackTrace();

                            this.effectiveBody = this.body;
                        }

                        break;
                    }
                }
            }

            if (this.effectiveBody == null) {
                this.effectiveBody = this.body;
            } else {
                setHeader("Content-Length", String.valueOf(this.effectiveBody.length));
            }
        }

        return this.effectiveBody;
    }

    public WebResponse setBody(byte[] body) {
        this.body = body == null ? new byte[0] : body;
        this.effectiveBody = null;

        setHeader("Content-Length", String.valueOf(this.body.length));
        setHeader("Content-Encoding", null);

        return this;
    }

    public WebResponse setBody(String body) {
        setBody(body.getBytes(StandardCharsets.UTF_8));
        setContentType("text/plain; charset=utf-8");

        return this;
    }

    public WebResponse setHTML(String body) {
        setBody(body);
        setContentType("text/html; charset=utf-8");

        if (!this.headers.containsKey("cache-control")) {
            this.headers.put("cache-control", "max-age=0, no-cache, no-store, must-revalidate");
        }

        return this;
    }

    public WebResponse setHeader(String field, String value) {
        if (value == null) {
            this.headers.remove(field.toLowerCase(Locale.ROOT));
        } else {
            this.headers.put(field.toLowerCase(Locale.ROOT), value);
        }

        return this;
    }

    public WebResponse setHeaders(Map<String, String> headers) {
        this.headers.clear();
        this.headers.putAll(headers);

        return this;
    }

    public String getHeader(String field) {
        return headers.get(field.toLowerCase(Locale.ROOT));
    }

    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    public WebResponse setContentType(@Nullable String contentType) {
        setHeader("Content-Type", contentType);

        return this;
    }

    public WebResponse setDate(TemporalAccessor time) {
        setHeader("Date", DATE_FORMATTER.format(time));

        return this;
    }

    public WebResponse setLocal(@NotNull String key, Object value) {
        this.locals.put(Objects.requireNonNull(key), value);

        return this;
    }

    public Object getLocal(@NotNull String key) {
        return this.locals.get(key);
    }

    public HashMap<String, Object> getLocals() {
        return this.locals;
    }

    private byte[] deflateData(byte[] data) throws IOException {
        try (ByteArrayOutputStream bOut = new ByteArrayOutputStream();
             DeflaterOutputStream dOut = new DeflaterOutputStream(bOut)) {
            dOut.write(data);
            dOut.flush();
            dOut.finish();

            return bOut.toByteArray();
        }
    }

    private static byte[] gzipData(byte[] data) throws IOException {
        try (ByteArrayOutputStream bOut = new ByteArrayOutputStream();
             GZIPOutputStream gzipOut = new GZIPOutputStream(bOut)) {
            gzipOut.write(data);
            gzipOut.flush();
            gzipOut.finish();

            return bOut.toByteArray();
        }
    }
}