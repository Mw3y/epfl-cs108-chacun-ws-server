package ch.epfl.chacun.server.websocket;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class WebSocketBroadcaster<T> extends WebSocketEventListener<T> {

    private final Map<String, List<WebSocketChannel<T>>> channels = new ConcurrentHashMap<>();

    void subscribeTo(String id, WebSocketChannel<T> channel) {
        channels.computeIfAbsent(id, _ -> new ArrayList<>()).add(channel);
    }

    void unsubscribeFrom(String id, WebSocketChannel<T> channel) {
        List<WebSocketChannel<T>> channelList = channels.get(id);
        if (channelList != null) {
            channelList.remove(channel);
            if (channelList.isEmpty())
                channels.remove(id);
        }
    }

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
