import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HttpServer {
    public int port;
    public String filePath;
    public boolean running;
    private static final Logger logger = Logger.getLogger(HttpServer.class.getName());

    public HttpServer(int port, String filePath) {
        this.port = port;
        this.filePath = filePath;
        this.running = false;
    }

    public void start() {
        running = true;
        logger.info("Server starting on port " + port);
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (running) {
                Socket clientSocket = serverSocket.accept();
                logger.info("Accepted connection from " + clientSocket.getRemoteSocketAddress());
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Server encountered an I/O error", e);
        }
    }

    public void stop() {
        running = false;
        logger.info("Server is stopping.");
    }

    private void handleClient(Socket socket) {
        try (socket;
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             OutputStream output = socket.getOutputStream()) {

            while (!socket.isClosed()) {
                HttpRequest request = HttpRequestParser.parseHttpRequest(reader);
                if (request == null) return;

                HttpRequestHandler handler = new HttpRequestHandler(filePath, output);
                handler.handleRequest(request);

                String connectionHeader = request.headers.getOrDefault("Connection", "");
                boolean shouldClose = "close".equalsIgnoreCase(connectionHeader);
                if (shouldClose) {
                    break;
                }
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error while handling client request", e);
        }
    }
}
