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
    public static final int PORT = 4221;


    public static void main(String[] args) {
        // You can use print statements as follows for debugging, they'll be visible when running tests.
        System.out.println("Logs from your program will appear here!");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                // Handle each client in a new thread
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
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
            String[] headerParts = headerLine.split(": ");
            String key = headerParts[0];
            String value = headerParts[1];
            headers.put(key, value);
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

    private static class ClientHandler implements Runnable {
        private final Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                //Parsing the request
                HttpRequest request = parseHttpRequest(socket.getInputStream());
                
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

            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    socket.close();
                    System.out.println("Disconnected: " + socket.getRemoteSocketAddress());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


        }
    }
}
