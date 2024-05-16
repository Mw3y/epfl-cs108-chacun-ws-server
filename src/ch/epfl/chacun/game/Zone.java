package ch.epfl.chacun.game;

import java.util.List;

/**
 * Represents a zone in the game.
 *
 * @author Valerio De Santis (373247)
 * @author Simon Lefort (371918)
 */
public sealed interface Zone {

    /**
     * Gets the tile id of the zone.
     *
     * @param zoneId the zone id
     * @return the tile id
     */
    static int tileId(int zoneId) {
        return zoneId / 10;
    }

    /**
     * Gets the local id of the zone.
     *
     * @param zoneId the zone id
     * @return the local id
     */
    static int localId(int zoneId) {
        return zoneId % 10;
    }

    /**
     * Gets the id of the zone.
     *
     * @return the id of the zone
     */
    int id();

    /**
     * Gets the tile id of the zone.
     *
     * @return the tile id of the zone
     */
    default int tileId() {
        return tileId(this.id());
    }

    /**
     * Gets the local id of the zone.
     *
     * @return the local id of the zone
     */
    default int localId() {
        return localId(this.id());
    }

    /**
     * Gets the special power of the zone.
     *
     * @return the special power of the zone
     */
    default SpecialPower specialPower() {
        return null;
    }

    /**
     * Represents a special power a zone can have
     *
     * @author Valerio De Santis (373247)
     * @author Simon Lefort (371918)
     */
    enum SpecialPower {
        SHAMAN, LOGBOAT, HUNTING_TRAP, PIT_TRAP, WILD_FIRE, RAFT
    }

    /**
     * Represents a forest.
     *
     * @param id   the id of the forest
     * @param kind the kind of the forest
     * @author Valerio De Santis (373247)
     * @author Simon Lefort (371918)
     */
    record Forest(int id, Kind kind) implements Zone {
        /**
         * Represents the different kinds of forest
         *
         * @author Valerio De Santis (373247)
         * @author Simon Lefort (371918)
         */
        public enum Kind {
            PLAIN, WITH_MENHIR, WITH_MUSHROOMS
        }
    }

    /**
     * Represents a meadow.
     *
     * @param id           the id of the meadow
     * @param animals      the animals in the meadow
     * @param specialPower the special power of the meadow
     * @author Valerio De Santis (373247)
     * @author Simon Lefort (371918)
     */
    record Meadow(int id, List<Animal> animals, SpecialPower specialPower) implements Zone {
        /**
         * Defensive copy of the animals list
         */
        public Meadow {
            animals = List.copyOf(animals);
        }
    }

    /**
     * Represents any zone that can contain water.
     *
     * @author Valerio De Santis (373247)
     * @author Simon Lefort (371918)
     */
    sealed interface Water extends Zone {
        /**
         * Gets the fish count of the water.
         *
         * @return the fish count of the water
         */
        int fishCount();
    }

    /**
     * Represents a lake.
     *
     * @param id           the id of the lake
     * @param fishCount    the fish count of the lake
     * @param specialPower the special power of the lake
     * @author Valerio De Santis (373247)
     * @author Simon Lefort (371918)
     */
    record Lake(int id, int fishCount, SpecialPower specialPower) implements Water {
    }

    /**
     * Represents a river.
     *
     * @param id        the id of the river
     * @param fishCount the fish count of the river
     * @param lake      the lake of the river
     * @author Valerio De Santis (373247)
     * @author Simon Lefort (371918)
     */
    record River(int id, int fishCount, Lake lake) implements Water {
        /**
         * Checks if the river has a lake.
         *
         * @return true if the river has a lake, false otherwise
         */
        public boolean hasLake() {
            return lake != null;
        }
    }

}
