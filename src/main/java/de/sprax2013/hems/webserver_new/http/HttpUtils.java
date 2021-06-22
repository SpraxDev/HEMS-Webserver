package de.sprax2013.hems.webserver_new.http;

import de.sprax2013.hems.webserver.HttpStatusCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class HttpUtils {
    private HttpUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static String normalizeToRootPath(@NotNull String path) {
        return Path.of("/", Objects.requireNonNull(path)).normalize().toString();
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

    public static byte[] readHttpHeaderLine(@NotNull InputStream in) throws IOException, HttpProtocolException {
        byte[] buffer = new byte[128];
        int i = 0;

        while (true) {
            byte b;
            if ((b = (byte) in.read()) != -1) {
                if (i + 1 >= buffer.length) {
                    int newLength = buffer.length * 2;

                    if (newLength > 4096) throw new HttpProtocolException(HttpStatusCode.REQUEST_ENTITY_TOO_LARGE);

                    buffer = Arrays.copyOf(buffer, newLength);
                }

                buffer[i++] = b;

                if (b == 10 && buffer[i - 2] == 13) {
                    break;
                }
            }
        }

        return Arrays.copyOf(buffer, i - 1);    // Remove "\r\n" and trailing nulls
    }

    public static void writeHttpResponse(@NotNull OutputStream out, int statusCode, @NotNull String statusMsg,
                                         @NotNull Map<String, String> headers, byte[] body, boolean isHeadRequest) throws IOException {
        writeLineCRLF(out, "HTTP/1.1 " + statusCode + " " + statusMsg);

        // Set Content-Length header if not set already
        if (body != null && body.length > 0 && !headers.containsKey("content-length")) {
            headers = new LinkedHashMap<>(headers);
            headers.put("content-length", String.valueOf(body.length));
        }

        // Send header
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            writeLineCRLF(out, formatHeaderField(entry.getKey()) + ": " + entry.getValue());
        }

        // Send body
        writeLineCRLF(out, null);

        if (!isHeadRequest && body != null && body.length > 0) {
            out.write(body);
        }
    }

    private static void writeLineCRLF(@NotNull OutputStream out, @Nullable String str) throws IOException {
        if (str != null) {
            out.write(str.getBytes());
        }

        out.write("\r\n".getBytes());
    }

    public static List<byte[]> split(byte[] input, byte[] pattern) {
        List<byte[]> result = new LinkedList<>();

        int start = -1;
        for (int i = 0; i < input.length; ++i) {
            boolean isMatch = true;

            for (int j = 0; j < pattern.length; ++j) {
                if (pattern[j] != input[i + j]) {
                    isMatch = false;
                    break;
                }
            }

            if (isMatch) {
                if (start != -1) {
                    result.add(Arrays.copyOfRange(input, start, i));
                }

                start = i + pattern.length;
            }
        }

        if (start != -1) {
            result.add(Arrays.copyOfRange(input, start, input.length));
        }

        return result;
    }

    public static byte[] readByteRange(File file, long start, int length) throws IOException {
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r")) {
            if (length == -1) {
                length = Math.toIntExact(randomAccessFile.length());
            }
            byte[] result = new byte[length];

            randomAccessFile.seek(start);
            randomAccessFile.readFully(result);

            return result;
        }
    }
}