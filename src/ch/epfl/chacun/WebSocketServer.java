package ch.epfl.chacun;

import ch.epfl.chacun.logic.GameActionData;
import ch.epfl.chacun.logic.GameLogic;
import ch.epfl.chacun.logic.GamePlayerData;
import ch.epfl.chacun.logic.ServerAction;
import ch.epfl.chacun.server.rfc6455.CloseStatusCode;
import ch.epfl.chacun.server.websocket.AbstractAsyncWebSocketServer;
import ch.epfl.chacun.server.websocket.WebSocketChannel;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class WebSocketServer extends AbstractAsyncWebSocketServer<GamePlayerData> {

    private static final int PING_INTERVAL = 60 * 1000;
    GameLogic gameLogic = new GameLogic();
    TimeoutWatcher<GamePlayerData> timeoutWatcher = new TimeoutWatcher<>();
    ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

    public WebSocketServer(String hostname, int port) throws IOException {
        super(hostname, port);
        System.out.println(STR."Server started on \{hostname}:\{port}");
        executor.scheduleAtFixedRate(timeoutWatcher, 0, PING_INTERVAL, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void onOpen(WebSocketChannel<GamePlayerData> ws) {
        timeoutWatcher.addClient(ws);
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
        timeoutWatcher.registerPong(ws);
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
        timeoutWatcher.removeClient(ws);
    }

    private static class TimeoutWatcher<T> implements Runnable {

        private final Map<WebSocketChannel<T>, Date> clientDelays = new HashMap<>();

        public void addClient(WebSocketChannel<T> ws) {
            clientDelays.put(ws, new Date());
        }

        public void removeClient(WebSocketChannel<T> ws) {
            clientDelays.remove(ws);
        }

        public void registerPong(WebSocketChannel<T> ws) {
            clientDelays.put(ws, new Date());
        }

        @Override
        public void run() {
            Date lastPing = new Date(new Date().getTime() - PING_INTERVAL);
            clientDelays.forEach((ws, lastPong) -> {
                // If the last pong was received more than 2 * PING_INTERVAL ms after the last ping,
                // terminate the connection since the close handshake was not completed
                if (lastPing.getTime() - lastPong.getTime() > 2 * PING_INTERVAL) {
                    ws.terminate();
                }
                // If the last pong was received more than PING_INTERVAL ms after the last ping, close the connection
                else if (lastPing.getTime() - lastPong.getTime() > PING_INTERVAL) {
                    ws.close(CloseStatusCode.PROTOCOL_ERROR, "PLAYER_TIMEOUT");
                }
                // Otherwise, send a ping
                else ws.sendPing();
            });
        }
    }
}

