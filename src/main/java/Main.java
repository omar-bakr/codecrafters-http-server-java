import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Main {
    public static final String HTTP_VERSION = "HTTP/1.1 ";
    public static final String ECHO = "/echo/";
    private static final String CRLF = "\r\n";
    private static final String DOUBLE_CRLF = CRLF + CRLF;


    public static void main(String[] args) {
        // You can use print statements as follows for debugging, they'll be visible when running tests.
        System.out.println("Logs from your program will appear here!");


        try (ServerSocket serverSocket = new ServerSocket(4221)) {
            // Since the tester restarts your program quite often, setting SO_REUSEADDR
            // ensures that we don't run into 'Address already in use' errors
            serverSocket.setReuseAddress(true);

            Socket socket = serverSocket.accept();
            //Parsing
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String requestLine = reader.readLine();
            String[] requestLinesSeparated = requestLine.split(" ");
            String path = requestLinesSeparated[1];

            //Check and send response
            if (path.startsWith(ECHO)) {
                String body = path.substring(ECHO.length());
                socket.getOutputStream().write(
                        buildResponse(200, "OK", "text/plain", body).getBytes());
            } else {
                socket.getOutputStream().write(buildResponse(404, "Not Found").getBytes());
            }

            System.out.println("accepted new connection");
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    public static String buildResponse(int statusCode, String statusMessage, String contentType, String body) {
        String statusLine = HTTP_VERSION + statusCode + " " + statusMessage + CRLF;
        String contentTypeHeader = "Content-Type: " + contentType + CRLF;
        String contentLengthHeader = "Content-Length: " + body.getBytes(StandardCharsets.UTF_8).length + CRLF;

        return statusLine + contentTypeHeader + contentLengthHeader + CRLF + body;

    }

    public static String buildResponse(int statusCode, String statusMessage) {
        String statusLine = HTTP_VERSION + statusCode + " " + statusMessage + CRLF;
        return statusLine + DOUBLE_CRLF;
    }
}
