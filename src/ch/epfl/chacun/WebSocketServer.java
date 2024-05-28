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
        GameActionData action = gameLogic.parseAndApplyAction(message, ws.getContext());
        if (action != null) {
            // Keep the context attached to the WebSocket channel for future use
            if (action.ctx() != null) {
                ws.subscribe(action.ctx().gameName()); // Subscribe to the game events
                ws.attachContext(action.ctx());
            }
            broadcastIfNeededOrSend(ws, action);
        }
    }

    @Override
    protected void onBinary(WebSocketChannel<GamePlayerData> ws, byte[] message) {
        // Binary messages are not supported
        ws.close(CloseStatusCode.UNSUPPORTED_DATA, "Binary messages are not supported");
    }

    @Override
    protected void onClose(WebSocketChannel<GamePlayerData> ws) {
        GameActionData action = gameLogic.parseAndApplyAction(ServerAction.GAMELEAVE.toString(), ws.getContext());
        if (action != null) {
            ws.unsubscribe(ws.getContext().gameName()); // Unsubscribe from the game events
            broadcastIfNeededOrSend(ws, action);
        }
        super.onClose(ws);
    }

    /**
     * Broadcast the action to the game if needed or send it directly to the player.
     * @param ws    The WebSocket channel to send the action to.
     * @param action The action to send.
     */
    private void broadcastIfNeededOrSend(WebSocketChannel<GamePlayerData> ws, GameActionData action) {
        if (action.shouldBeBroadcasted())
            ws.broadcast(ws.getContext().gameName(), action.toGameActionString());
        else
            ws.sendText(action.toGameActionString());
    }
}

