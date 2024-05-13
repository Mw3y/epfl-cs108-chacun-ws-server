package ch.epfl.chacun.server;

import ch.epfl.chacun.server.rfc6455.TextEncoder;
import ch.epfl.chacun.server.websocket.WebSocketServer;

public class Main {
    public static void main(String[] args) {
        WebSocketServer server = new WebSocketServer(3000);
        server.start();
    }
}
