package ch.epfl.chacun.server.websocket;

import ch.epfl.chacun.server.rfc6455.PayloadData;
import ch.epfl.chacun.server.rfc6455.RFC6455;
import ch.epfl.chacun.server.rfc6455.UpgradeHandshake;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Steven Ou
 */
public class Server {

    public Server(String bindAddr, int bindPort) throws IOException {
        InetSocketAddress sockAddr = new InetSocketAddress(bindAddr, bindPort);

        // create a socket channel and bind to local bind address
        AsynchronousServerSocketChannel serverSock = AsynchronousServerSocketChannel.open().bind(sockAddr);

        // start to accept the connection from client
        serverSock.accept(serverSock, new CompletionHandler<>() {
            @Override
            public void completed(AsynchronousSocketChannel sockChannel, AsynchronousServerSocketChannel serverSock) {
                // a connection is accepted, start to accept next connection
                serverSock.accept(serverSock, this);
                // start to read message from the client
                startRead(sockChannel);
            }

            @Override
            public void failed(Throwable exc, AsynchronousServerSocketChannel serverSock) {
                System.out.println("fail to accept a connection");
            }

        });

    }

    public static void main(String[] args) {
        try {
            new Server("127.0.0.1", 3000);
            for (; ; ) {
                Thread.sleep(10 * 1000);
            }
        } catch (Exception ex) {
            Logger.getLogger(String.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void startRead(AsynchronousSocketChannel sockChannel) {
        final ByteBuffer buf = ByteBuffer.allocate(256);

        // read message from client
        sockChannel.read(buf, sockChannel, new CompletionHandler<>() {

            /**
             * some message is read from client, this callback will be called
             */
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

                String content = new String(buf.array());
                if (UpgradeHandshake.isUpgradeRequest(content)) {
                    // send the upgrade response
                    try {
                        String upgradeResponse = UpgradeHandshake.generateResponse(content, channel);
                        startWrite(channel, ByteBuffer.wrap(upgradeResponse.getBytes()));
                    } catch (IllegalArgumentException e) {
                        failed(e, channel);
                    }
                    // start to read next message again
                    startRead(channel);
                    return;
                }


                // echo the message
                PayloadData payloadData = RFC6455.parsePayload(buf);
                if (payloadData != null)
                    startWrite(channel, RFC6455.encodeTextFrame(RFC6455.decodeTextFrame(payloadData)));

                // start to read next message again
                startRead(channel);
            }

            @Override
            public void failed(Throwable exc, AsynchronousSocketChannel channel) {
                System.out.println("fail to read message from client");
            }
        });
    }

    private void startWrite(AsynchronousSocketChannel sockChannel, final ByteBuffer buf) {
        sockChannel.write(buf, sockChannel, new CompletionHandler<>() {

            @Override
            public void completed(Integer result, AsynchronousSocketChannel channel) {
                // finish to write message to client, nothing to do
                System.out.println("Message written to client");
            }

            @Override
            public void failed(Throwable exc, AsynchronousSocketChannel channel) {
                //fail to write message to client
                System.out.println("Fail to write message to client");
            }

        });
    }
}
