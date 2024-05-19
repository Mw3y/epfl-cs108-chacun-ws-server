package ch.epfl.chacun;

import ch.epfl.chacun.server.websocket.AbstractAsyncWebSocketServer;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    public static void main(String[] args) {
        System.out.println("Server started on port 3000");
        try {
            WebSocketServer server = new WebSocketServer("127.0.0.1", 3000);
            // server.start();
            for (; ; ) {
                Thread.sleep(1000);
            }
        } catch (Exception ex) {
            Logger.getLogger(String.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
