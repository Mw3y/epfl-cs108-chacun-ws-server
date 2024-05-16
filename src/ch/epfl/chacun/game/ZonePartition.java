package ch.epfl.chacun.game;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a zone partition
 *
 * @param areas the areas in the zone partition
 * @param <Z>   the type of the areas
 * @author Valerio De Santis (373247)
 * @author Simon Lefort (371918)
 */
public record ZonePartition<Z extends Zone>(Set<Area<Z>> areas) {

    /**
     * Creates a new zone partition and does a defensive copy
     * of the set of areas (to make it unmodifiable)
     *
     * @param areas the areas in the zone partition
     */
    public ZonePartition {
        areas = Set.copyOf(areas);
    }

    /**
     * Creates a zone partition without any area
     */
    public ZonePartition() {
        this(Set.of());
    }

    /**
     * Returns the area that contains the specified zone, or throws an exception
     * if the zone is not in any area
     *
     * @param zone  the zone we are looking for
     * @param areas the set of areas to search in
     * @return the area that contains the specified zone
     * @throws IllegalArgumentException if the zone is not in any area
     */
    private static <Z extends Zone> Area<Z> areaContaining(Z zone, Set<Area<Z>> areas) {
        return areas.stream()
                // we use a drop while rather than a filter because there is only one area containing the given zone,
                // and we want to avoid iterating over the whole list
                .dropWhile(area -> !area.zones().contains(zone))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

    /**
     * Returns the area that contains the specified zone
     *
     * @param zone the zone we are looking for
     * @return the area that contains the specified zone
     * @throws IllegalArgumentException if the zone is not in any area
     */
    public Area<Z> areaContaining(Z zone) {
        return ZonePartition.areaContaining(zone, areas);
    }

    /**
     * A builder for ZonePartition
     *
     * @param <Z> the type of zones forming the areas
     */
    public static final class Builder<Z extends Zone> {

        private final Set<Area<Z>> areas;

        /**
         * Creates a new builder for a zone partition with the same areas as the given one
         *
         * @param partition the zone partition to copy
         */
        public Builder(ZonePartition<Z> partition) {
            // ZonePartition is immutable, so we do not need to
            // call Set.copyOf() here as the areas of partitions will never change
            // we don't use the areas = partition.areas() syntax because we
            // want 'areas' to refer to a modifiable object
            areas = new HashSet<>(partition.areas());
        }

        /**
         * Adds a new area (with no initial occupant) to the zone partition builder
         *
         * @param zone            the zone forming the area to add
         * @param openConnections the number of open connections of the area to add
         */
        public void addSingleton(Z zone, int openConnections) {
            areas.add(new Area<>(Set.of(zone), List.of(), openConnections));
        }

        /**
         * Replaces the area containing the given zone with a new area identical to the former,
         * but having the given color as initial occupant
         *
         * @param zone  the zone contained by the area where we want to add the occupant
         * @param color the color of the initial occupant of the area
         */
        public void addInitialOccupant(Z zone, PlayerColor color) {
            Area<Z> area = ZonePartition.areaContaining(zone, areas);
            areas.add(area.withInitialOccupant(color));
            areas.remove(area);
        }

        /**
         * Replaces the area containing the given zone with a new area identical to the former,
         * but without an occupant of the given color
         *
         * @param zone  the zone contained by the area where we want to remove the occupant
         * @param color the color of the occupant to remove
         * @throws IllegalArgumentException if the zone is not in any area
         *                                  or if the area does not contain an occupant of the given color
         */
        public void removeOccupant(Z zone, PlayerColor color) {
            // throws an exception if the zone does not belong to the partition
            Area<Z> area = ZonePartition.areaContaining(zone, areas);
            // throws an exception if the area does not contain an occupant of the given colour
            areas.add(area.withoutOccupant(color));
            areas.remove(area);
        }

        /**
         * Replaces the given area with a new area identical to the former,
         * but without any occupant, throws an exception if the area is not in the partition
         *
         * @param area the area to remove all the occupants from
         * @throws IllegalArgumentException if the area is not in the partition
         */
        public void removeAllOccupantsOf(Area<Z> area) {
            boolean areaIsFound = areas.remove(area);
            Preconditions.checkArgument(areaIsFound);
            areas.add(area.withoutOccupants());
        }

        /**
         * Unites the areas containing the given zones, throwing an exception
         * if one of the zones is not in any area (areaContaining() will throw an exception)
         * Finally, removes the two areas from the partition builder and adds a
         * new bigger area resulting from their union
         *
         * @param zone1 the first zone whose area we want to unite
         * @param zone2 the second zone whose area we want to unite
         * @throws IllegalArgumentException if one of the zones is not in any area
         */
        public void union(Z zone1, Z zone2) {
            // throws an exception if one of the zones does not belong to the partition
            Area<Z> area1 = ZonePartition.areaContaining(zone1, areas);
            Area<Z> area2 = ZonePartition.areaContaining(zone2, areas);
            Area<Z> newBiggerArea = area1.connectTo(area2);
            areas.remove(area1);
            // here we do not need to check whether
            // area1 == area2, because .remove will just return false
            // if that's the case
            areas.remove(area2);
            areas.add(newBiggerArea);
        }

        /**
         * Builds the zone partition, which will be immutable
         *
         * @return the zone partition built
         */
        public ZonePartition<Z> build() {
            return new ZonePartition<>(areas);
        }

    }


}
