import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    public static final int PORT = 4221;
    public static String filesPath;


    public static void main(String[] args) {
        // You can use print statements as follows for debugging, they'll be visible when running tests.
        System.out.println("Logs from your program will appear here!");

        //Getting the files directory from cmd args
        parseFilePathIfExists(args);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                // Handle each client in a new thread
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket socket) {
        try (socket;
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             OutputStream output = socket.getOutputStream()) {

            while (!socket.isClosed()) {

                //Parse
                HttpRequest request = HttpRequestParser.parseHttpRequest(reader);
                if (request == null) return;

                //Handle
                HttpRequestHandler handler = new HttpRequestHandler(filesPath, output);
                handler.handleRequest(request);


                //Break to close the connection
                String connectionHeader = request.headers.getOrDefault("Connection", "");
                boolean shouldClose = "close".equalsIgnoreCase(connectionHeader);
                if (shouldClose) {
                    break;
                }


            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void parseFilePathIfExists(String[] args) {
        if (args.length == 2 && args[0].equals("--directory"))
            filesPath = args[1];
    }

}
