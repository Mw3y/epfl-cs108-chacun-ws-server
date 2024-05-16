package ch.epfl.chacun.game;

/**
 * Util class to calculate points.
 *
 * @author Valerio De Santis (373247)
 * @author Simon Lefort (371918)
 */
public final class Points {

    /**
     * Number of gained points per tile when a forest is closed.
     */
    private static final int CLOSED_FOREST_TILE_POINTS = 2;
    /**
     * Number of gained points per mushroom group when a forest is closed.
     */
    private static final int CLOSED_FOREST_MUSHROOM_POINTS = 3;
    /**
     * Number of gained points per tile when a river is closed.
     */
    private static final int CLOSED_RIVER_TILE_POINTS = 1;
    /**
     * Number of gained points per fish when a river is closed.
     */
    private static final int CLOSED_RIVER_FISH_POINTS = 1;
    /**
     * Number of gained points per mammoth in a meadow at the end of the game.
     */
    private static final int MAMMOTH_POINTS = 3;
    /**
     * Number of gained points per aurochs in a meadow at the end of the game.
     */
    private static final int AUROCHS_POINTS = 2;
    /**
     * Number of gained points per deer in a meadow at the end of the game.
     */
    private static final int DEER_POINTS = 1;
    /**
     * Number of gained points per logboat when placed.
     */
    private static final int LOGBOAT_POINTS = 2;
    /**
     * Number of gained points per raft at the end of the game.
     */
    private static final int RAFT_POINTS = 1;
    /**
     * Number of gained points per fisher hut in a river system at the end of the game.
     */
    private static final int FISHER_HUT_POINTS = 1;

    private Points() {
    }

    /**
     * Returns the number of points gained by a player when a forest is closed
     *
     * @param tileCount          the number of tiles in the forest, at least 2
     * @param mushroomGroupCount the number of mushroom groups in the forest (non-negative)
     * @return the number of points gained by a player when a forest is closed
     * @throws IllegalArgumentException if the number of tiles is less than 2
     *                                  or the number of mushroom groups is negative
     */
    public static int forClosedForest(int tileCount, int mushroomGroupCount) {
        Preconditions.checkArgument(tileCount > 1);
        Preconditions.checkArgument(mushroomGroupCount >= 0);
        return tileCount * CLOSED_FOREST_TILE_POINTS + mushroomGroupCount * CLOSED_FOREST_MUSHROOM_POINTS;
    }

    /**
     * Returns the number of points gained by a player when a river is closed
     *
     * @param tileCount the number of tiles in the river (at least 2)
     * @param fishCount the number of fish in the river (non-negative)
     * @return the number of points gained by a player when a river is closed
     * @throws IllegalArgumentException if the number of tiles is less than 2
     *                                  or the number of fish is negative
     */
    public static int forClosedRiver(int tileCount, int fishCount) {
        Preconditions.checkArgument(tileCount > 1);
        Preconditions.checkArgument(fishCount >= 0);
        return tileCount * CLOSED_RIVER_TILE_POINTS + fishCount * CLOSED_RIVER_FISH_POINTS;
    }

    /**
     * Returns the number of points gained by a player per meadow
     *
     * @param mammothCount the number of mammoths in the meadow (non-negative)
     * @param aurochsCount the number of aurochs in the meadow (non-negative)
     * @param deerCount    the number of deers in the meadow (after the tiger's buffet!) (non-negative)
     * @return the number of points gained by a player per meadow at the end of the game
     * @throws IllegalArgumentException if the number of mammoths, aurochs or deers is negative
     */
    public static int forMeadow(int mammothCount, int aurochsCount, int deerCount) {
        Preconditions.checkArgument(mammothCount >= 0);
        Preconditions.checkArgument(aurochsCount >= 0);
        Preconditions.checkArgument(deerCount >= 0);

        return mammothCount * MAMMOTH_POINTS + aurochsCount * AUROCHS_POINTS + deerCount * DEER_POINTS;
    }

    /**
     * Returns the number of points gained by a player per river system at the end of the game
     *
     * @param fishCount the number of fish in the lake (non-negative)
     * @return the number of points gained by a player per river system at the end of the game
     * @throws IllegalArgumentException if the number of fish is negative
     */
    public static int forRiverSystem(int fishCount) {
        Preconditions.checkArgument(fishCount >= 0);
        return fishCount * FISHER_HUT_POINTS;
    }

    /**
     * Returns the number of points gained by a player per logboat when he places it
     *
     * @param lakeCount the number of lakes a positive number
     * @return the number of points gained by a player per logboat when he places it
     * @throws IllegalArgumentException if the number of lakes is less than 1
     */
    public static int forLogboat(int lakeCount) {
        Preconditions.checkArgument(lakeCount > 0);
        return lakeCount * LOGBOAT_POINTS;
    }

    /**
     * Returns the number of points gained by a player per raft at the end of the game
     *
     * @param lakeCount the number of lakes at least 1
     * @return the number of points gained by a player per raft at the end of the game
     * @throws IllegalArgumentException if the number of lakes is less than 1
     */
    public static int forRaft(int lakeCount) {
        Preconditions.checkArgument(lakeCount > 0);
        return lakeCount * RAFT_POINTS;
    }


}
