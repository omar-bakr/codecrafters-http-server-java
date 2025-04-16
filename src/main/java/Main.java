import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Main {
    public static final String HTTP_VERSION = "HTTP/1.1 ";
    public static final String ECHO = "/echo/";
    private static final String CRLF = "\r\n";
    private static final String USER_AGENT_KEY = "User-Agent";


    public static void main(String[] args) {
        // You can use print statements as follows for debugging, they'll be visible when running tests.
        System.out.println("Logs from your program will appear here!");


        try (ServerSocket serverSocket = new ServerSocket(4221)) {
            // Since the tester restarts your program quite often, setting SO_REUSEADDR
            // ensures that we don't run into 'Address already in use' errors
            serverSocket.setReuseAddress(true);
            Socket socket = serverSocket.accept();

            HttpRequest request = parseHttpRequest(socket.getInputStream());//Parsing the data

            //Check and send response
            if (request.path.equals("/")) {
                socket.getOutputStream().write(buildResponse(200, "OK").getBytes());
            } else if (request.path.startsWith(ECHO)) {
                String body = request.path.substring(ECHO.length());
                socket.getOutputStream().write(buildResponse(200, "OK", "text/plain", body).getBytes());
            } else if (request.path.startsWith("/user-agent")) {
                socket.getOutputStream().write(buildResponse(200, "OK", "text/plain", request.headers.get(USER_AGENT_KEY)).getBytes());

            } else {
                socket.getOutputStream().write(buildResponse(404, "Not Found").getBytes());
            }

            System.out.println("accepted new connection");
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    private static HttpRequest parseHttpRequest(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String requestLine = reader.readLine();

            if (requestLine == null || requestLine.isEmpty()) {
                throw new IOException("Empty request line");
            }

            String[] requestLinesSeparated = requestLine.split(" ");
            if (requestLinesSeparated.length < 3) {
                throw new IOException("Malformed request line: " + requestLine);
            }

            String method = requestLinesSeparated[0];
            String path = requestLinesSeparated[1];
            String httpVersion = requestLinesSeparated[2];

            Map<String, String> headers = getHttpRequestHeaders(reader);
            return new HttpRequest(method, path, httpVersion, headers);
        }
    }

    private static Map<String, String> getHttpRequestHeaders(BufferedReader reader) throws IOException {
        Map<String, String> headers = new HashMap<>();
        String headerLine;
        while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
            int separatorIndex = headerLine.indexOf(": ");
            if (separatorIndex != -1) {
                String key = headerLine.substring(0, separatorIndex);
                String value = headerLine.substring(separatorIndex + 2);
                headers.put(key, value);
            } else {
                // Optionally, handle malformed headers
                throw new IOException("Malformed header line: " + headerLine);
            }
        }
        return headers;
    }


    public static String buildResponse(int statusCode, String statusMessage, String contentType, String body) {
        String statusLine = HTTP_VERSION + statusCode + " " + statusMessage + CRLF;
        String contentTypeHeader = "Content-Type: " + contentType + CRLF;
        String contentLengthHeader = "Content-Length: " + body.getBytes(StandardCharsets.UTF_8).length + CRLF;

        return statusLine + contentTypeHeader + contentLengthHeader + CRLF + body;

    }

    public static String buildResponse(int statusCode, String statusMessage) {
        String statusLine = HTTP_VERSION + statusCode + " " + statusMessage + CRLF;

        return statusLine + CRLF;
    }
}
