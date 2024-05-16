package ch.epfl.chacun;

import ch.epfl.chacun.logic.GameLogic;
import ch.epfl.chacun.server.websocket.WebSocketChannel;
import ch.epfl.chacun.server.websocket.WebSocketServer;

import java.util.ArrayList;
import java.util.List;

public class GameWebSocketServer extends WebSocketServer {

    private List<GameLogic> games = new ArrayList<>();

    public GameWebSocketServer(int port) {
        super(port);
    }

    @Override
    protected void onClose(WebSocketChannel ws) {
        super.onClose(ws);
    }

    @Override
    protected void onOpen(WebSocketChannel ws) {
        System.out.println("opened!");
        super.onOpen(ws);
    }
}
