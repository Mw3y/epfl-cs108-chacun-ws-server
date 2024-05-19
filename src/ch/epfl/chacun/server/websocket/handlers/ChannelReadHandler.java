package ch.epfl.chacun.server.websocket.handlers;

import ch.epfl.chacun.server.rfc6455.PayloadData;
import ch.epfl.chacun.server.rfc6455.RFC6455;
import ch.epfl.chacun.server.websocket.AbstractAsyncWebSocketServer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public class ChannelReadHandler<T> implements CompletionHandler<Integer, AsynchronousSocketChannel> {

    ByteBuffer payload;

    private final AbstractAsyncWebSocketServer<T> server;

    public ChannelReadHandler(AbstractAsyncWebSocketServer<T> server, ByteBuffer payload) {
        this.server = server;
        this.payload = payload;
    }

    @Override
    public void completed(Integer result, AsynchronousSocketChannel channel) {
        if (result == -1) {
            try {
                System.out.println("Client disconnected");
                channel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        String content = new String(payload.array());
        if (RFC6455.isUpgradeRequest(content)) {
            // send the upgrade response
            try {
                String upgradeResponse = RFC6455.upgradeToWebsocket(content);
                server.startWrite(channel, ByteBuffer.wrap(upgradeResponse.getBytes()));
            } catch (IllegalArgumentException e) {
                failed(e, channel);
            }
            // start to read next message again
            server.startRead(channel);
            return;
        }


        // echo the message
        PayloadData payloadData = RFC6455.parsePayload(payload);
        if (payloadData != null) {
            // server.startWrite(channel, RFC6455.encodeTextFrame(RFC6455.decodeTextFrame(payloadData)));
            server.dispatch(payloadData, channel);
        }

        // start to read next message again
        server.startRead(channel);
    }

    @Override
    public void failed(Throwable exc, AsynchronousSocketChannel attachment) {
        System.out.println("fail to read message from client");
    }
}
