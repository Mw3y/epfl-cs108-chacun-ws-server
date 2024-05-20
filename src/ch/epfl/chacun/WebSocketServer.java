package ch.epfl.chacun;

import ch.epfl.chacun.logic.GameActionData;
import ch.epfl.chacun.logic.GameLogic;
import ch.epfl.chacun.logic.GamePlayerData;
import ch.epfl.chacun.logic.ServerAction;
import ch.epfl.chacun.server.websocket.AsyncWebSocketServer;
import ch.epfl.chacun.server.websocket.WebSocketChannel;

import java.io.IOException;

public class WebSocketServer extends AsyncWebSocketServer<GamePlayerData> {

    GameLogic gameLogic = new GameLogic();

    public WebSocketServer(String hostname, int port) throws IOException {
        super(hostname, port);
    }

    @Override
    protected void onMessage(WebSocketChannel<GamePlayerData> ws, String message) {
        GameActionData action = gameLogic.parseAndApplyWebSocketAction(message, ws.getContext());
        if (action != null) {
            if (action.ctx() != null) {
                ws.subscribe(action.ctx().gameName());
                ws.attachContext(action.ctx());
            }
            if (action.shouldBeBroadcasted()) {
                ws.broadcast(ws.getContext().gameName(), action.toGameActionString());
            } else {
                ws.sendText(action.toGameActionString());
            }
        }
    }

    @Override
    protected void onClose(WebSocketChannel<GamePlayerData> ws) {
        GameActionData action = gameLogic
                .parseAndApplyWebSocketAction(ServerAction.GAMELEAVE.toString(), ws.getContext());
        if (action != null) {
            ws.unsubscribe(ws.getContext().gameName());
            if (action.shouldBeBroadcasted()) {
                ws.broadcast(ws.getContext().gameName(), action.toGameActionString());
            }
        }
        super.onClose(ws);
    }
}

