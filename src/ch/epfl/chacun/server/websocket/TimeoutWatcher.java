package ch.epfl.chacun.server.websocket;

import ch.epfl.chacun.server.rfc6455.CloseStatusCode;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TimeoutWatcher<T> {

    private static final int PING_INTERVAL = 60 * 1000; // 1 minute

    ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

    private final Map<WebSocketChannel<T>, Date> wsDelays = new HashMap<>();

    public TimeoutWatcher() {
        executor.scheduleAtFixedRate(() -> {
            Date lastPing = new Date(new Date().getTime() - PING_INTERVAL);
            wsDelays.forEach((ws, lastPong) -> {
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
        }, 0, PING_INTERVAL, TimeUnit.MILLISECONDS);
    }

    public void watch(WebSocketChannel<T> ws) {
        wsDelays.put(ws, new Date());
    }

    public void unwatch(WebSocketChannel<T> ws) {
        wsDelays.remove(ws);
    }

    public void registerPong(WebSocketChannel<T> ws) {
        wsDelays.put(ws, new Date());
    }
}