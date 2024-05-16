package ch.epfl.chacun.server.rfc6455;

import java.nio.ByteBuffer;

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
    public PayloadData {
        payload = payload.duplicate().asReadOnlyBuffer();
        data = data.duplicate().asReadOnlyBuffer();
    }
}