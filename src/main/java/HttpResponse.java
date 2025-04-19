import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

public class HttpResponse {
    private static final String HTTP_VERSION = "HTTP/1.1";
    private static final String CRLF = "\r\n";

    private final int statusCode;
    private final String statusText;
    private final String contentType;
    private final String body;
    private final String contentEncoding;

    private HttpResponse(int statusCode, String statusText, String contentType, String body, String contentEncoding) {
        this.statusCode = statusCode;
        this.statusText = statusText;
        this.contentType = contentType;
        this.body = body;
        this.contentEncoding = contentEncoding;
    }

    public static HttpResponse statusOnly(int code, String text) {
        return new HttpResponse(code, text, null, null, null);
    }

    public static HttpResponse withBody(int code, String text, String contentType, String contentEncoding, String body) {
        return new HttpResponse(code, text, contentType, body, contentEncoding);
    }

    public byte[] getBytes() {
        StringBuilder headers = new StringBuilder();
        headers.append(HTTP_VERSION)
                .append(" ")
                .append(statusCode)
                .append(" ")
                .append(statusText)
                .append(CRLF);

        // No body exists, return headers only
        if (body == null) {
            headers.append(CRLF);
            return headers.toString().getBytes(StandardCharsets.UTF_8);
        }

        // Compress body if needed
        byte[] bodyBytes = (contentEncoding != null && contentEncoding.equalsIgnoreCase("gzip"))
                ? compressToGzipBytes(body)
                : body.getBytes(StandardCharsets.UTF_8);


        headers.append("Content-Type: ").append(contentType).append(CRLF);
        headers.append("Content-Length: ").append(bodyBytes.length).append(CRLF);

        if (contentEncoding != null) {
            headers.append("Content-Encoding: ").append(contentEncoding).append(CRLF);
        }

        headers.append(CRLF); // End of headers

        byte[] headerBytes = headers.toString().getBytes(StandardCharsets.UTF_8);
        return mergeByteArrays(headerBytes, bodyBytes);
    }

    private static byte[] compressToGzipBytes(String input) {
        if (input == null || input.isEmpty()) {
            return new byte[0];
        }

        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipStream = new GZIPOutputStream(byteStream)) {
            gzipStream.write(input.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return byteStream.toByteArray();
    }

    private static byte[] mergeByteArrays(byte[] array1, byte[] array2) {
        byte[] merged = new byte[array1.length + array2.length];
        System.arraycopy(array1, 0, merged, 0, array1.length);
        System.arraycopy(array2, 0, merged, array1.length, array2.length);
        return merged;
    }
}
