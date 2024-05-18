package ch.epfl.chacun.server.rfc6455;

import java.nio.channels.AsynchronousSocketChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpgradeHandshake {

    /**
     * Globally Unique Identifier (GUID, [RFC4122]) in string form.
     */
    private static final String GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    public static boolean isUpgradeRequest(String request) {
        return request.startsWith("GET /") &&
                request.contains("HTTP/1.1") &&
                request.contains("Upgrade: websocket") &&
                request.contains("Connection: Upgrade") &&
                request.contains("Sec-WebSocket-Key: ");
    }

    /**
     * Create the key for the "Sec-WebSocket-Accept" header of the handshake upgrade response.
     * <p>
     * For this header field, the server has to take the value (as present
     * in the header field, e.g., the base64-encoded [RFC4648] version minus
     * any leading and trailing whitespace) and concatenate this with the
     * Globally Unique Identifier (GUID, [RFC4122]) "258EAFA5-E914-47DA-
     * 95CA-C5AB0DC85B11" in string form, which is unlikely to be used by
     * network endpoints that do not understand the WebSocket Protocol.  A
     * SHA-1 hash (160 bits) [FIPS.180-3], base64-encoded (see Section 4 of
     * [RFC4648]), of this concatenation is then returned in the server's
     * handshake.
     *
     * @param secWebSocketKey The value of the "Sec-WebSocket-Key" header field in the handshake request.
     * @return the value of the "Sec-WebSocket-Accept" header field in the handshake response.
     */
    private static String encodeSha1AndBase64(String secWebSocketKey) {
        String string = secWebSocketKey.concat(GUID);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] sha1Result = digest.digest(string.getBytes());
            return Base64.getEncoder().encodeToString(sha1Result);
        } catch (NoSuchAlgorithmException ignored) {
            return null;
        }
    }

    /**
     * Creates the response to upgrade an HTTP connection to a WebSocket connection.
     * <p>
     * The opening handshake is intended to be compatible with HTTP-based
     * server-side software and intermediaries, so that a single port can be
     * used by both HTTP clients talking to that server and WebSocket
     * clients talking to that server. To this end, the WebSocket client's
     * handshake is an HTTP Upgrade request.
     *
     * @param content The content of the HTTP request.
     * @throws IllegalArgumentException If the Sec-WebSocket-Key header is not present in the request.
     */
    public static String generateResponse(String content) {
        Pattern secWSKeyPattern = Pattern.compile("Sec-WebSocket-Key:\\s*(.*?)\r\n");
        Matcher secWSKeyMatcher = secWSKeyPattern.matcher(content);

        // If the Sec-WebSocket-Key header is not present, close the connection
        if (!secWSKeyMatcher.find()) {
            throw new IllegalArgumentException("Sec-WebSocket-Key header not found");
        }

        // Generate the Sec-WebSocket-Accept header value
        String secWebSocketKey = secWSKeyMatcher.group(1);
        String secWSAcceptHeader = encodeSha1AndBase64(secWebSocketKey);

        // Create the handshake response
        StringJoiner responseBuilder = new StringJoiner("\r\n");
        responseBuilder.add("HTTP/1.1 101 Switching Protocols");
        responseBuilder.add("Upgrade: websocket");
        responseBuilder.add("Connection: Upgrade");
        responseBuilder.add("Sec-WebSocket-Accept: " + secWSAcceptHeader);
        responseBuilder.add("\r\n");
        return responseBuilder.toString();
    }

}
