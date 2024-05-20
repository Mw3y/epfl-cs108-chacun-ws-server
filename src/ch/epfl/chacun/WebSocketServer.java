package ch.epfl.chacun;

import ch.epfl.chacun.logic.GameActionData;
import ch.epfl.chacun.logic.GameLogic;
import ch.epfl.chacun.logic.GamePlayerData;
import ch.epfl.chacun.logic.ServerAction;
import ch.epfl.chacun.server.websocket.AsyncWebSocketServer;
import ch.epfl.chacun.server.websocket.WebSocketChannel;

import java.io.IOException;

/**
 * The WebSocket server for the ChaCuN game.
 * @author Maxence Espagnet (sciper: 372808)
 */
public class WebSocketServer extends AsyncWebSocketServer<GamePlayerData> {

    /**
     * The game logic of the server.
     */
    private final GameLogic gameLogic = new GameLogic();

    /**
     * Create a new WebSocket server with the given hostname and port.
     * @param hostname The hostname of the server.
     * @param port    The port of the server.
     * @throws IOException If an I/O error occurs.
     */
    public WebSocketServer(String hostname, int port) throws IOException {
        super(hostname, port);
    }

    @Override
    protected void onMessage(WebSocketChannel<GamePlayerData> ws, String message) {
        GameActionData action = gameLogic.parseAndApplyWebSocketAction(message, ws.getContext());
        if (action != null) {
            // Keep the context attached to the WebSocket channel for future use
            if (action.ctx() != null) {
                ws.subscribe(action.ctx().gameName());
                ws.attachContext(action.ctx());
            }
            if (action.shouldBeBroadcasted()) // Broadcast the action if needed
                ws.broadcast(ws.getContext().gameName(), action.toGameActionString());
            else // Send the response action to the client
                ws.sendText(action.toGameActionString());
        }
    }

    @Override
    protected void onClose(WebSocketChannel<GamePlayerData> ws) {
        GameActionData action = gameLogic.parseAndApplyWebSocketAction(ServerAction.GAMELEAVE.toString(), ws.getContext());
        if (action != null) {
            // Unsubscribe the WebSocket channel from the game
            ws.unsubscribe(ws.getContext().gameName());
            // Broadcast the action if needed
            if (action.shouldBeBroadcasted())
                ws.broadcast(ws.getContext().gameName(), action.toGameActionString());
        }
        super.onClose(ws);
    }
}

