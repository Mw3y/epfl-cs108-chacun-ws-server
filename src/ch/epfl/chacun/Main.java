package ch.epfl.chacun;

public class Main {
    public static void main(String[] args) {
        WebSocketServer server = new WebSocketServer(3000);
        server.start();
    }
}
