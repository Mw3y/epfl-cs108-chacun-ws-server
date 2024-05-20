package ch.epfl.chacun.server.websocket;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A class to allow broadcasting messages to WebSocket channels subscribed to a specific identifier.
 * @param <T> The type of the context attached to the WebSocket channel.
 * @author Maxence Espagnet (sciper: 372808)
 */
public abstract class WebSocketBroadcaster<T> extends WebSocketEventListener<T> {

    /**
     * The map of WebSocket channels subscribed to an identifier.
     */
    private final Map<String, List<WebSocketChannel<T>>> channels = new ConcurrentHashMap<>();

    /**
     * Subscribe a WebSocket channel to a broadcast channel.
     * @param id the identifier of the broadcast channel to subscribe to
     * @param channel the WebSocket channel to subscribe
     */
    void subscribeTo(String id, WebSocketChannel<T> channel) {
        channels.computeIfAbsent(id, _ -> new ArrayList<>()).add(channel);
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
            synchronized (channelList) {
                Iterator<WebSocketChannel<T>> iterator = channelList.iterator();
                while (iterator.hasNext()) {
                    WebSocketChannel<T> channel = iterator.next();
                    channel.sendBytes(buffer);
                }
            }
        }
    }
}
