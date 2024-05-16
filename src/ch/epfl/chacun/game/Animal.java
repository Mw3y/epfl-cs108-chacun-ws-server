package ch.epfl.chacun.game;

/**
 * Represents an animal in the game
 *
 * @param id   non-negative, the id of the animal
 * @param kind non-null, the kind of animal
 * @author Valerio De Santis (373247)
 * @author Simon Lefort (371918)
 */
public record Animal(int id, Kind kind) {

    /**
     * Returns the id of the tile where the animal is
     *
     * @return the id of the tile where the animal is
     */
    public int tileId() {
        return Zone.tileId(zoneId());
    }

    /**
     * we do a right shift to get the zone id,
     * as the id of the animal is the zone id * 10 + the local id
     *
     * @return the zone id of the animal
     */
    private int zoneId() {
        return id / 10;
    }

    /**
     * Represents the different kinds of animal
     *
     * @author Valerio De Santis (373247)
     * @author Simon Lefort (371918)
     */
    public enum Kind {
        MAMMOTH, AUROCHS, DEER, TIGER
    }
}