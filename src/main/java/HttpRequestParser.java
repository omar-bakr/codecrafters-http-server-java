import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HttpRequestParser {
    private static final Logger logger = Logger.getLogger(HttpRequestParser.class.getName());

    public static HttpRequest parseHttpRequest(BufferedReader reader) throws IOException {
        String requestLine = reader.readLine();

        if (requestLine == null || requestLine.isEmpty()) {
            logger.warning("Empty request line received");
            return null;
        }

        logger.info("Request line: " + requestLine);
        String[] requestLinesSeparated = requestLine.split(" ");
        if (requestLinesSeparated.length < 3) {
            logger.severe("Malformed request line: " + requestLine);
            throw new RuntimeException("Malformed request line: " + requestLine);
        }

        String method = requestLinesSeparated[0];
        String path = requestLinesSeparated[1];
        String httpVersion = requestLinesSeparated[2];

        logger.fine("Method: " + method + ", Path: " + path + ", Version: " + httpVersion);

        Map<String, String> headers = getHttpRequestHeaders(reader);
        logger.fine("Headers parsed: " + headers);

        String body = parseHttpBody(reader, headers);
        logger.fine("Body parsed: " + (body.length() > 100 ? body.substring(0, 100) + "..." : body));

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
            logger.severe("Expected " + contentLength + " chars, but read " + charsRead);
            throw new RuntimeException("Body length doesn't match Content-Length header");
        }

        return new String(bodyChars);
    }

    public static int getContentLength(Map<String, String> headers) {
        String contentLengthStr = headers.getOrDefault("Content-Length", "0");
        try {
            return Integer.parseInt(contentLengthStr);
        } catch (NumberFormatException e) {
            logger.log(Level.SEVERE, "Invalid Content-Length header: " + contentLengthStr, e);
            throw new RuntimeException("Invalid Content-Length header: " + contentLengthStr);
        }
    }

    public static Map<String, String> getHttpRequestHeaders(BufferedReader reader) throws IOException {
        Map<String, String> headers = new HashMap<>();
        String headerLine;
        while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
            logger.fine("Header line: " + headerLine);
            String[] headerParts = headerLine.split(": ", 2);
            if (headerParts.length == 2) {
                headers.put(headerParts[0], headerParts[1]);
            } else {
                logger.warning("Malformed header: " + headerLine);
            }
        }
        return headers;
    }
}
