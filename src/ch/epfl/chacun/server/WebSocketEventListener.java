package ch.epfl.chacun.server;

import ch.epfl.chacun.server.rfc6455.PayloadData;

public interface WebSocketEventListener {
    void onOpen();
    void onMessage(String message);
    void onPing();
    void onPong();
    void onClose();
    void dispatch(PayloadData payload);
}
