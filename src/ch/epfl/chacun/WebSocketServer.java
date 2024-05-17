package ch.epfl.chacun;

import ch.epfl.chacun.logic.GamePlayerData;
import ch.epfl.chacun.logic.GameActionData;
import ch.epfl.chacun.logic.GameLogic;
import ch.epfl.chacun.server.websocket.AbstractWebSocketServer;
import ch.epfl.chacun.server.websocket.WebSocketChannel;

public class WebSocketServer extends AbstractWebSocketServer<GamePlayerData> {

    GameLogic gameLogic = new GameLogic();

    public WebSocketServer(int port) {
        super(port);
    }

    @Override
    protected void onOpen(WebSocketChannel<GamePlayerData> ws) {
        System.out.println("Connection opened");
    }

    @Override
    protected void onMessage(WebSocketChannel<GamePlayerData> ws, String message) {
        GameActionData action = gameLogic.parseAndApplyWebSocketAction(message, ws.getContext());
        if (action != null) {
            if (action.ctx() != null) {
                System.out.println("action.ctx().gameName() = " + action.ctx().gameName());
                System.out.println("action.ctx().username() = " + action.ctx().username());
                ws.attachContext(action.ctx());
            }
            System.out.println(action);
            ws.sendText(action.toGameActionString());
        }
    }

    @Override
    protected void onPing(WebSocketChannel<GamePlayerData> ws) {
        System.out.println("Ping received");
        ws.sendPong();
    }

    @Override
    protected void onPong(WebSocketChannel<GamePlayerData> ws) {
        System.out.println("Pong received");
    }

    @Override
    protected void onClose(WebSocketChannel<GamePlayerData> ws) {
        System.out.println("Connection closed");
    }
}

