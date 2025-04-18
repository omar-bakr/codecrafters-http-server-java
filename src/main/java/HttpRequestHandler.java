import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class HttpRequestHandler {
    private static final String ECHO_ROUTE = "/echo/";
    private static final String USER_AGENT_ROUTE = "/user-agent";
    private static final String FILES_ROUTE = "/files/";
    private static final String USER_AGENT_KEY = "User-Agent";
    private static final List<String> SUPPORTED_ENCODINGS = List.of("gzip");

    private final String filesPath;
    private final OutputStream out;

    public HttpRequestHandler(String filesPath, OutputStream out) {
        this.filesPath = filesPath;
        this.out = out;
    }

    public void handleRequest(HttpRequest request) throws IOException {
        switch (request.method) {
            case "GET":
                handleGetRequest(request);
                break;
            case "POST":
                handlePostRequest(request);
                break;
            default:
                sendResponse(HttpResponse.statusOnly(405, "Method Not Allowed"));
        }
    }

    private void handleGetRequest(HttpRequest request) throws IOException {
        RouteHandler handler = getRouteHandler(request.path);
        handler.handle(request);
    }

    private void handlePostRequest(HttpRequest request) {
        if (filesPath != null && request.path.startsWith(FILES_ROUTE) && !request.body.isEmpty()) {
            String fileName = request.path.substring(FILES_ROUTE.length());
            Path filePath = Path.of(filesPath, fileName);
            try {
                Files.writeString(filePath, request.body);
                sendResponse(HttpResponse.statusOnly(201, "Created"));
            } catch (IOException e) {
                handleServerError(e);
            }
        } else {
            sendResponse(HttpResponse.statusOnly(400, "Bad Request"));
        }
    }

    private RouteHandler getRouteHandler(String path) {
        if (path.equals("/")) return this::handleRootRoute;
        if (path.startsWith(ECHO_ROUTE)) return this::handleEchoRoute;
        if (path.startsWith(USER_AGENT_ROUTE)) return this::handleUserAgentRoute;
        if (path.startsWith(FILES_ROUTE)) return this::handleFilesRoute;
        return this::handleNotFound;
    }

    private void handleRootRoute(HttpRequest request) {
        sendResponse(HttpResponse.statusOnly(200, "OK"));
    }

    private void handleEchoRoute(HttpRequest request) {
        String body = request.path.substring(ECHO_ROUTE.length());
        String requestEncoding = request.headers.getOrDefault("Accept-Encoding", "");
        String contentEncoding = getValidEncoding(requestEncoding);
        sendResponse(HttpResponse.withBody(200, "OK", "text/plain", contentEncoding, body));
    }

    private String getValidEncoding(String requestEncoding) {
        Set<String> requestEncodings = Arrays.stream(requestEncoding.split(",")).collect(Collectors.toSet());
        return SUPPORTED_ENCODINGS.stream()
                .filter(requestEncodings::contains)
                .findFirst()
                .orElse(null);
    }

    private void handleUserAgentRoute(HttpRequest request) {
        String userAgent = request.headers.getOrDefault(USER_AGENT_KEY, "");
        String requestEncoding = request.headers.getOrDefault("Accept-Encoding", "");
        String contentEncoding = getValidEncoding(requestEncoding);
        sendResponse(HttpResponse.withBody(200, "OK", "text/plain", contentEncoding, userAgent));
    }

    private void handleFilesRoute(HttpRequest request) {
        if (filesPath == null) {
            handleNotFound(request);
            return;
        }

        String fileName = request.path.substring(FILES_ROUTE.length());
        Path filePath = Path.of(filesPath, fileName);

        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            handleNotFound(request);
            return;
        }

        try {
            byte[] fileBytes = Files.readAllBytes(filePath);
            String content = new String(fileBytes);
            String requestEncoding = request.headers.getOrDefault("Accept-Encoding", "");
            String contentEncoding = getValidEncoding(requestEncoding);

            sendResponse(HttpResponse.withBody(200, "OK", "application/octet-stream", contentEncoding, content));
        } catch (IOException e) {
            sendResponse(HttpResponse.statusOnly(500, "Internal Server Error"));
        }
    }

    private void handleNotFound(HttpRequest request) {
        sendResponse(HttpResponse.statusOnly(404, "Not Found"));
    }

    private void handleServerError(Exception e) {
        sendResponse(HttpResponse.statusOnly(500, "Internal Server Error"));
    }

    private void sendResponse(HttpResponse response) {
        try {
            out.write(response.getBytes());
            out.flush();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
