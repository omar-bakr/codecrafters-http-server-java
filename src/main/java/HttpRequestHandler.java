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
                sendResponse(buildStatusOnlyResponse(405, "Method Not Allowed", request));
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
                sendResponse(buildStatusOnlyResponse(201, "Created", request));
            } catch (IOException e) {
                handleServerError(e, request);
            }
        } else {
            sendResponse(buildStatusOnlyResponse(400, "Bad Request", request));
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
        sendResponse(buildStatusOnlyResponse(200, "OK", request));
    }

    private void handleEchoRoute(HttpRequest request) {
        String body = request.path.substring(ECHO_ROUTE.length());
        String contentEncoding = getValidEncoding(request.headers.getOrDefault("Accept-Encoding", ""));

        HttpResponse.Builder builder = new HttpResponse.Builder()
                .status(200, "OK")
                .header("Connection", getConnectionHeader(request))
                .body(body, "text/plain", contentEncoding);

        sendResponse(builder.build());
    }

    private void handleUserAgentRoute(HttpRequest request) {
        String userAgent = request.headers.getOrDefault(USER_AGENT_KEY, "");
        String contentEncoding = getValidEncoding(request.headers.getOrDefault("Accept-Encoding", ""));

        HttpResponse.Builder builder = new HttpResponse.Builder()
                .status(200, "OK")
                .header("Connection", getConnectionHeader(request))
                .body(userAgent, "text/plain", contentEncoding);

        sendResponse(builder.build());
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
            String contentEncoding = getValidEncoding(request.headers.getOrDefault("Accept-Encoding", ""));

            HttpResponse.Builder builder = new HttpResponse.Builder()
                    .status(200, "OK")
                    .header("Connection", getConnectionHeader(request))
                    .body(content, "application/octet-stream", contentEncoding);

            sendResponse(builder.build());
        } catch (IOException e) {
            handleServerError(e, request);
        }
    }

    private void handleNotFound(HttpRequest request) {
        sendResponse(buildStatusOnlyResponse(404, "Not Found", request));
    }

    private void handleServerError(Exception e, HttpRequest request) {
        e.printStackTrace();
        sendResponse(buildStatusOnlyResponse(500, "Internal Server Error", request));
    }

    private String getValidEncoding(String requestEncoding) {
        Set<String> requestEncodings = Arrays.stream(requestEncoding.split(",")).map(String::strip).collect(Collectors.toSet());
        return requestEncodings.stream()
                .filter(SUPPORTED_ENCODINGS::contains)
                .findFirst()
                .orElse(null);
    }

    private String getConnectionHeader(HttpRequest request) {
        return "close".equalsIgnoreCase(request.headers.getOrDefault("Connection", "close")) ? "close" : "keep-alive";
    }

    private HttpResponse buildStatusOnlyResponse(int code, String text, HttpRequest request) {
        return new HttpResponse.Builder()
                .status(code, text)
                .header("Connection", getConnectionHeader(request))
                .build();
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
