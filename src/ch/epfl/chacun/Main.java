package ch.epfl.chacun;

public class Main {
    public static void main(String[] args) {
        System.out.println("Server started on port 3000");
        WebSocketServer server = new WebSocketServer(3000);
        server.start();
    }
}
