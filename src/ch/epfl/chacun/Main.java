package ch.epfl.chacun;

import ch.epfl.chacun.server.websocket.WebSocketServer;

public class Main {
    public static void main(String[] args) {
        WebSocketServer server = new GameWebSocketServer(3000);
        server.start();
    }
}
