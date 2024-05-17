package ch.epfl.chacun.logic;

public record GameActionData(
        ServerAction action,
        String data,
        GamePlayerData ctx,
        boolean shouldBeBroadcasted
) {

    public GameActionData(ServerAction action, String data, GamePlayerData ctx) {
        this(action, data, ctx, false);
    }

    public GameActionData(ServerAction action, String data, boolean shouldBeBroadcasted) {
        this(action, data, null, shouldBeBroadcasted);
    }

    /**
     * Create a new GameActionData which will not be broadcasted and without context.
     * @param action The action to send.
     * @param data The data to send.
     */
    public GameActionData(ServerAction action, String data) {
        this(action, data, null, false);
    }

    public GameActionData(ServerAction action) {
        this(action, null, null, false);
    }

    public String toGameActionString() {
        return STR."\{action}\{data == null ? "" : STR.".\{data}"}";
    }

}
