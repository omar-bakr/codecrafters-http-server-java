import java.nio.charset.StandardCharsets;

public class HttpResponse {
    private static final String HTTP_VERSION = "HTTP/1.1";
    private static final String CRLF = "\r\n";

    private final int statusCode;
    private final String statusText;
    private final String contentType;
    private final String body;

    private HttpResponse(int statusCode, String statusText, String contentType, String body) {
        this.statusCode = statusCode;
        this.statusText = statusText;
        this.contentType = contentType;
        this.body = body;
    }

    public static HttpResponse statusOnly(int code, String text) {
        return new HttpResponse(code, text, null, null);
    }

    public static HttpResponse withBody(int code, String text, String contentType, String body) {
        return new HttpResponse(code, text, contentType, body);
    }

    public byte[] getBytes() {
        StringBuilder sb = new StringBuilder();
        sb.append(HTTP_VERSION).append(" ").append(statusCode).append(" ").append(statusText).append(CRLF);

        if (body != null) {
            byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
            sb.append("Content-Type: ").append(contentType).append(CRLF);
            sb.append("Content-Length: ").append(bodyBytes.length).append(CRLF);
            sb.append(CRLF).append(body);
        } else {
            sb.append(CRLF);
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
