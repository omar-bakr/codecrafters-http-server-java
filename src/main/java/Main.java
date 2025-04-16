import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {
        // You can use print statements as follows for debugging, they'll be visible when running tests.
        System.out.println("Logs from your program will appear here!");


        try (ServerSocket serverSocket = new ServerSocket(4221)) {
            // Since the tester restarts your program quite often, setting SO_REUSEADDR
            // ensures that we don't run into 'Address already in use' errors
            serverSocket.setReuseAddress(true);

            Socket socket = serverSocket.accept();
            InputStream inputStream = socket.getInputStream();
            String request = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            String[] requestSeparated = request.split("\r\n");
            String[] requestLines = requestSeparated[0].split(" ");
            String path = requestLines[1];
            if (path.equals("/")) {
                socket.getOutputStream().write("HTTP/1.1 200 OK\r\n\r\n".getBytes());
            } else {
                socket.getOutputStream().write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
            }

            System.out.println("accepted new connection");
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }
}
