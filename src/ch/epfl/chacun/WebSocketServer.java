package ch.epfl.chacun;

import ch.epfl.chacun.logic.GameActionData;
import ch.epfl.chacun.logic.GameLogic;
import ch.epfl.chacun.logic.GamePlayerData;
import ch.epfl.chacun.logic.ServerAction;
import ch.epfl.chacun.server.rfc6455.CloseStatusCode;
import ch.epfl.chacun.server.websocket.AsyncWebSocketServer;
import ch.epfl.chacun.server.websocket.WebSocketChannel;

import java.io.IOException;

/**
 * The WebSocket server for the ChaCuN game.
 * @author Maxence Espagnet (sciper: 372808)
 */
public class WebSocketServer extends AsyncWebSocketServer<GamePlayerData> {

    /**
     * The maximum size of a message that can be sent or received.
     * <p>
     * Should be a multiple of 2.
     */
    public static final int MAX_MESSAGE_SIZE = 512;

    /**
     * The interval in milliseconds at which the server sends ping messages to clients.
     */
    private static final int PING_INTERVAL = 60 * 1000; // 1 minute

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
        super(hostname, port, MAX_MESSAGE_SIZE, PING_INTERVAL);
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
    protected void onBinary(WebSocketChannel<GamePlayerData> ws, byte[] message) {
        // Binary messages are not supported
        ws.close(CloseStatusCode.UNSUPPORTED_DATA, "Binary messages are not supported");
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

