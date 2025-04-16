import java.util.HashMap;
import java.util.Map;

class HttpRequest {
    String method;
    String path;
    String version;
    Map<String, String> headers = new HashMap<>();
    String body;

    public HttpRequest(String method, String path, String version, Map<String, String> headers) {
        this.method = method;
        this.path = path;
        this.version = version;
        this.headers = headers;
    }
}