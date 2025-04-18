import java.util.Map;

public class HttpRequest {
    String method;
    String path;
    String version;
    Map<String, String> headers;
    String body;

    public HttpRequest(String method, String path, String version, Map<String, String> headers, String body) {
        this.method = method;
        this.path = path;
        this.version = version;
        this.headers = headers;
        this.body = body;
    }
}