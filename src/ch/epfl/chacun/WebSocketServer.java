package ch.epfl.chacun;

import ch.epfl.chacun.logic.GameActionData;
import ch.epfl.chacun.logic.GameLogic;
import ch.epfl.chacun.logic.GamePlayerData;
import ch.epfl.chacun.logic.ServerAction;
import ch.epfl.chacun.server.rfc6455.CloseStatusCode;
import ch.epfl.chacun.server.websocket.AsyncWebSocketServer;
import ch.epfl.chacun.server.websocket.WebSocketChannel;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class WebSocketServer extends AsyncWebSocketServer<GamePlayerData> {

    GameLogic gameLogic = new GameLogic();

    public WebSocketServer(String hostname, int port) throws IOException {
        super(hostname, port);
    }

    @Override
    protected void onOpen(WebSocketChannel<GamePlayerData> ws) {
        super.onOpen(ws);
    }

    @Override
    protected void onMessage(WebSocketChannel<GamePlayerData> ws, String message) {
        GameActionData action = gameLogic.parseAndApplyWebSocketAction(message, ws.getContext());
        if (action != null) {
            if (action.ctx() != null) {
                subscribeTo(action.ctx().gameName(), ws);
                ws.attachContext(action.ctx());
            }
            if (action.shouldBeBroadcasted()) {
                broadcastTo(ws.getContext().gameName(), action.toGameActionString());
            } else {
                ws.sendText(action.toGameActionString());
            }
        }
    }

    @Override
    protected void onPing(WebSocketChannel<GamePlayerData> ws) {
        ws.sendPong();
    }

    @Override
    protected void onPong(WebSocketChannel<GamePlayerData> ws) {
        super.onPong(ws);
    }

    @Override
    protected void onClose(WebSocketChannel<GamePlayerData> ws) {
        GameActionData action = gameLogic
                .parseAndApplyWebSocketAction(ServerAction.GAMELEAVE.toString(), ws.getContext());
        if (action != null) {
            unsubscribeFrom(ws.getContext().gameName(), ws);
            if (action.shouldBeBroadcasted()) {
                broadcastTo(ws.getContext().gameName(), action.toGameActionString());
            }
        }
        super.onClose(ws);
    }
}

