package ch.epfl.chacun.server;

import ch.epfl.chacun.server.rfc6455.PayloadData;

import java.nio.channels.SocketChannel;

public interface WebSocketEventListener {
    void onOpen(SocketChannel channel);
    void onMessage(SocketChannel channel, String message);
    void onPing(SocketChannel channel);
    void onPong(SocketChannel channel);
    void onClose(SocketChannel channel);
    void dispatch(PayloadData payload, SocketChannel channel);
}
