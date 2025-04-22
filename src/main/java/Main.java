public class Main {
    public static void main(String[] args) throws InterruptedException {
        String filesPath = parseFilePathIfExists(args);
        HttpServer server = new HttpServer(4221, filesPath);
        server.start();
    }

    private static String parseFilePathIfExists(String[] args) {
        if (args.length == 2 && args[0].equals("--directory"))
            return args[1];
        return "";
    }
}
