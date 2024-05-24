package ch.epfl.chacun.server.websocket;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A class to allow broadcasting messages to WebSocket channels subscribed to a specific identifier.
 * @param <T> The type of the context attached to the WebSocket channel.
 * @author Maxence Espagnet (sciper: 372808)
 */
public abstract class WebSocketBroadcaster<T> extends WebSocketEventListener<T> {

    /**
     * The map of WebSocket channels subscribed to an identifier.
     */
    private final Map<String, List<WebSocketChannel<T>>> channels = new HashMap<>();

    /**
     * Subscribe a WebSocket channel to a broadcast channel.
     * @param id the identifier of the broadcast channel to subscribe to
     * @param channel the WebSocket channel to subscribe
     */
    void subscribeTo(String id, WebSocketChannel<T> channel) {
        // Use CopyOnWriteArrayList to prevent a broadcast from throwing if a player joins or leaves the game
        channels.computeIfAbsent(id, _ -> new CopyOnWriteArrayList<>()).add(channel);
    }

    /**
     * Unsubscribe a WebSocket channel from a broadcast channel.
     * @param id the identifier of the broadcast channel to unsubscribe from
     * @param channel the WebSocket channel to unsubscribe
     */
    void unsubscribeFrom(String id, WebSocketChannel<T> channel) {
        List<WebSocketChannel<T>> channelList = channels.get(id);
        if (channelList != null) {
            channelList.remove(channel);
            // Erase the broadcast channel if no more WebSocket channels are subscribed
            if (channelList.isEmpty()) {
                channels.remove(id);
            }
        }
    }

    /**
     * Broadcast a byte buffer content to all WebSocket channels subscribed to an identifier.
     * @param id the identifier of the broadcast channel
     * @param buffer the byte buffer to broadcast
     */
    void broadcastTo(String id, ByteBuffer buffer) {
        if (channels.containsKey(id)) {
            List<WebSocketChannel<T>> channelList = channels.get(id);
            for (WebSocketChannel<T> channel : channelList) {
                channel.sendBytes(buffer);
            }
        }
    }
}
