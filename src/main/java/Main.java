import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class Main {
    public static final String HTTP_VERSION = "HTTP/1.1 ";
    public static final String ECHO_ROUTE = "/echo/";
    private static final String CRLF = "\r\n";
    private static final String USER_AGENT_KEY = "User-Agent";
    public static final int PORT = 4221;
    public static final String FILES_ROUTE = "/files/";
    public static final String USER_AGENT_ROUTE = "/user-agent";
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
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void parseFilePathIfExists(String[] args) {
        if (args.length == 2 && args[0].equals("--directory"))
            filesPath = args[1];
    }

    private static HttpRequest parseHttpRequest(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String requestLine = reader.readLine();

        if (requestLine == null || requestLine.isEmpty()) {
            throw new IOException("Empty request line");
        }

        String[] requestLinesSeparated = requestLine.split(" ");
        if (requestLinesSeparated.length < 3) {
            throw new IOException("Malformed request line: " + requestLine);
        }

        //Request line
        String method = requestLinesSeparated[0];
        String path = requestLinesSeparated[1];
        String httpVersion = requestLinesSeparated[2];

        //Headers
        Map<String, String> headers = getHttpRequestHeaders(reader);

        //Body
        int requestBodySize = Integer.parseInt(headers.getOrDefault("Content-Length", "0"));
        String body = getHttpRequestBody(reader, requestBodySize);

        return new HttpRequest(method, path, httpVersion, headers, body);

    }

    private static String getHttpRequestBody(BufferedReader reader, int requestBodySize) throws IOException {
        int ch;
        int index = 0;
        StringBuilder sb = new StringBuilder();
        while (index < requestBodySize && (ch = reader.read()) != -1) {
            sb.append((char) ch);
            index++;
        }
        return sb.toString();
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
                OutputStream out = socket.getOutputStream();

                //Check and send response
                if (request.method.equals("GET")) {
                    if (request.path.equals("/")) {
                        out.write(buildResponse(200, "OK").getBytes());
                    } else if (request.path.startsWith(ECHO_ROUTE)) {
                        String body = request.path.substring(ECHO_ROUTE.length());
                        out.write(buildResponse(200, "OK", "text/plain", body).getBytes());
                    } else if (request.path.startsWith(USER_AGENT_ROUTE)) {
                        out.write(buildResponse(200, "OK", "text/plain", request.headers.get(USER_AGENT_KEY)).getBytes());
                    } else if (request.path.startsWith(FILES_ROUTE) && filesPath != null) {
                        String fileName = request.path.substring(FILES_ROUTE.length());
                        Path path = Path.of(filesPath, fileName);
                        boolean exists = Files.exists(path);
                        boolean isFile = Files.isRegularFile(path);

                        if (exists && isFile) {
                            String fileContent = Files.readString(path);
                            out.write(buildResponse(200, "OK", "application/octet-stream", fileContent).getBytes());
                        } else {
                            out.write(buildResponse(404, "Not Found").getBytes());
                        }


                    } else {
                        out.write(buildResponse(404, "Not Found").getBytes());
                    }
                } else if (request.method.equals("POST")) {
                    if (request.path.startsWith(FILES_ROUTE) && !request.body.isEmpty()) {
                        String fileName = request.path.substring(FILES_ROUTE.length());
                        Path path = Path.of(filesPath, fileName);
                        Files.writeString(path, request.body);
                        out.write(buildResponse(201, "Created").getBytes());
                    }
                }


            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }


        }
    }
}
