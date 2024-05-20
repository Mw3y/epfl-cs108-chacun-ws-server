package ch.epfl.chacun.server.rfc6455;

import java.nio.ByteBuffer;

/**
 * Represents the data of a WebSocket payload as defined by RFC 6455.
 * @param payload The raw payload data.
 * @param isFinal Whether this is the final fragment of the message.
 * @param rsv The reserved bits.
 * @param opCode The operation code.
 * @param isMasked Whether the payload is masked.
 * @param length The length of the payload.
 * @param mask The mask used to unmask the payload.
 * @param data The unmasked payload data.
 */
public record PayloadData(
        ByteBuffer payload,
        boolean isFinal,
        int[] rsv,
        OpCode opCode,
        boolean isMasked,
        int length,
        byte[] mask,
        ByteBuffer data
) {
    /**
     * Defensively copies the payload and data buffers.
     */
    public PayloadData {
        payload = payload.duplicate().asReadOnlyBuffer();
        data = data.duplicate().asReadOnlyBuffer();
    }
}