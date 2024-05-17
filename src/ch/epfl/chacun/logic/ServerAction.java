package ch.epfl.chacun.logic;

/**
 * Represents the possible actions that can be sent by a game client.
 */
public enum ServerAction {
    UNKNOWN,
    GAMEJOIN,
    GAMEJOIN_ACCEPT,
    GAMEJOIN_DENY,
    GAMELEAVE,
    GAMEACTION,
    GAMEACTION_ACCEPT,
    GAMEACTION_DENY,
    GAMEEND,
    GAMEMSG;

    @Override
    public String toString() {
        return this.name();
    }

    public static ServerAction fromString(String action) {
        try {
            return ServerAction.valueOf(action);
        } catch (IllegalArgumentException e) {
            return ServerAction.UNKNOWN;
        }
    }
}
