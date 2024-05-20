package ch.epfl.chacun.logic;

/**
 * Represents an action to be sent to a player or broadcasted to the game.
 * @author Maxence Espagnet (sciper: 372808)
 * @author Simon Lefort (sciper: 371918)
 */
public record GameActionData(
        ServerAction action,
        String data,
        GamePlayerData ctx,
        boolean shouldBeBroadcasted
) {

    /**
     * Create a new GameActionData with the provided action, data and context to save.
     * @param action The action to send.
     * @param data The data to send.
     * @param ctx The data that need to be attached to the WebSocket connection further use.
     */
    public GameActionData(ServerAction action, String data, GamePlayerData ctx) {
        this(action, data, ctx, false);
    }

    /**
     * Create a new GameActionData with the provided action, data and broadcast flag.
     * @param action The action to send.
     * @param data The data to send.
     * @param shouldBeBroadcasted Whether the action should be broadcasted to the game.
     */
    public GameActionData(ServerAction action, String data, boolean shouldBeBroadcasted) {
        this(action, data, null, shouldBeBroadcasted);
    }

    /**
     * Create a new GameActionData which will not be broadcasted and without context to attach to the WebSocket.
     * @param action The action to send.
     * @param data The data to send.
     */
    public GameActionData(ServerAction action, String data) {
        this(action, data, null, false);
    }

    /**
     * Generate the string representation of the action to send to the player or broadcast to the game.
     * <pre>i.e:{@literal GAMEJOIN.<gameName>,<username>}</pre>
     * @return The string representation of the action.
     */
    public String toGameActionString() {
        return STR."\{action}\{data == null ? "" : STR.".\{data}"}";
    }

}
