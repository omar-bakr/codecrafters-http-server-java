import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

public class HttpResponse {
    private static final String HTTP_VERSION = "HTTP/1.1";
    private static final String CRLF = "\r\n";

    private final int statusCode;
    private final String statusText;
    private final Map<String, String> headers;
    private final byte[] body;

    private HttpResponse(int statusCode, String statusText, Map<String, String> headers, byte[] body) {
        this.statusCode = statusCode;
        this.statusText = statusText;
        this.headers = headers;
        this.body = body;
    }

    public byte[] getBytes() {
        StringBuilder headerBuilder = new StringBuilder();
        headerBuilder.append(HTTP_VERSION)
                .append(" ")
                .append(statusCode)
                .append(" ")
                .append(statusText)
                .append(CRLF);

        headers.forEach((key, value) -> headerBuilder.append(key).append(": ").append(value).append(CRLF));
        headerBuilder.append(CRLF);

        byte[] headerBytes = headerBuilder.toString().getBytes(StandardCharsets.UTF_8);
        return body != null ? mergeByteArrays(headerBytes, body) : headerBytes;
    }

    private static byte[] mergeByteArrays(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    public static class Builder {
        private int statusCode;
        private String statusText;
        private final Map<String, String> headers = new LinkedHashMap<>();
        private byte[] body;

        public Builder status(int code, String text) {
            this.statusCode = code;
            this.statusText = text;
            return this;
        }

        public Builder header(String key, String value) {
            this.headers.put(key, value);
            return this;
        }

        public Builder body(String content, String contentType, String encoding) {
            try {
                byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);

                if ("gzip".equalsIgnoreCase(encoding)) {
                    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                    try (GZIPOutputStream gzip = new GZIPOutputStream(byteStream)) {
                        gzip.write(contentBytes);
                    }
                    contentBytes = byteStream.toByteArray();
                    headers.put("Content-Encoding", "gzip");
                }

                this.body = contentBytes;
                headers.put("Content-Type", contentType);
                headers.put("Content-Length", String.valueOf(contentBytes.length));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return this;
        }

        public HttpResponse build() {
            return new HttpResponse(statusCode, statusText, headers, body);
        }
    }
}
