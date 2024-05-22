package ch.epfl.chacun.server.websocket;

import ch.epfl.chacun.server.rfc6455.CloseStatusCode;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A class that watches WebSocket channels for timeouts.
 * @param <T> The type of the context attached to the WebSocket channel.
 * @author Maxence Espagnet (sciper: 372808)
 */
public class TimeoutWatcher<T> {

    /**
     * The map of WebSocket channels to their last pong date.
     */
    private final Map<WebSocketChannel<T>, Date> wsDelays = new HashMap<>();

    /**
     * Create a new timeout watcher that checks for timeouts every {@code timeoutAfterMs} milliseconds.
     * @param timeoutAfterMs the timeout in milliseconds
     */
    public TimeoutWatcher(int timeoutAfterMs) {
        try (ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1)) {
            executor.scheduleAtFixedRate(() -> {
                Date lastPing = new Date(new Date().getTime() - timeoutAfterMs);
                wsDelays.forEach((ws, lastPong) -> {
                    // If the last pong was received more than PING_INTERVAL ms after the last ping,
                    // close the connection from the server side as the client has timed out.
                    if (lastPing.getTime() - lastPong.getTime() > timeoutAfterMs) {
                        System.out.println("A client has timed out.");
                        ws.terminate();
                    }
                    // Otherwise, send a ping
                    else ws.sendPing();
                });
            }, 0, timeoutAfterMs, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Watch a WebSocket channel for timeouts.
     * @param ws the WebSocket channel to watch
     */
    public void watch(WebSocketChannel<T> ws) {
        wsDelays.put(ws, new Date());
    }

    /**
     * Stop watching a WebSocket channel for timeouts.
     * @param ws the WebSocket channel to stop watching
     */
    public void unwatch(WebSocketChannel<T> ws) {
        wsDelays.remove(ws);
    }

    /**
     * Register a pong message from a WebSocket channel.
     * @param ws the WebSocket channel that sent the pong
     */
    public void registerPong(WebSocketChannel<T> ws) {
        wsDelays.put(ws, new Date());
    }
}