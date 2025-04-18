import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class HttpRequestParser {
    public static HttpRequest parseHttpRequest(BufferedReader reader) throws IOException {
        String requestLine = reader.readLine();

        if (requestLine == null || requestLine.isEmpty()) {
            throw new RuntimeException("Empty request line");
        }

        String[] requestLinesSeparated = requestLine.split(" ");
        if (requestLinesSeparated.length < 3) {
            throw new RuntimeException("Malformed request line: " + requestLine);
        }

        //Request line
        String method = requestLinesSeparated[0];
        String path = requestLinesSeparated[1];
        String httpVersion = requestLinesSeparated[2];

        //Headers
        Map<String, String> headers = getHttpRequestHeaders(reader);

        //Body
        String body = parseHttpBody(reader, headers);

        return new HttpRequest(method, path, httpVersion, headers, body);

    }

    public static String parseHttpBody(BufferedReader reader, Map<String, String> headers) throws IOException {
        int contentLength = getContentLength(headers);
        if (contentLength == 0) {
            return "";
        }

        char[] bodyChars = new char[contentLength];
        int charsRead = reader.read(bodyChars, 0, contentLength);

        if (charsRead != contentLength) {
            throw new RuntimeException("Body length doesn't match Content-Length header");
        }

        return new String(bodyChars);
    }

    public static int getContentLength(Map<String, String> headers) {
        String contentLengthStr = headers.getOrDefault("Content-Length", "0");
        try {
            return Integer.parseInt(contentLengthStr);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid Content-Length header: " + contentLengthStr);
        }
    }


    public static Map<String, String> getHttpRequestHeaders(BufferedReader reader) throws IOException {
        Map<String, String> headers = new HashMap<>();
        String headerLine;
        while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
            String[] headerParts = headerLine.split(": ");
            String key = headerParts[0];
            String value = headerParts[1];
            headers.put(key, value);
        }
        return headers;
    }
}
