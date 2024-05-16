package ch.epfl.chacun.game;

import java.util.List;

/**
 * List all possible colors.
 *
 * @author Valerio De Santis (373247)
 * @author Simon Lefort (371918)
 */
public enum PlayerColor {
    RED, BLUE, GREEN, YELLOW, PURPLE;

    /**
     * List all possible colors.
     */
    public static final List<PlayerColor> ALL = List.of(PlayerColor.values());
}
