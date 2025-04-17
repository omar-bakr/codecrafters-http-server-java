import java.io.IOException;

@FunctionalInterface
public interface RouteHandler {
    void handle(HttpRequest request) throws IOException;
}
