package ch.epfl.chacun.game;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Represents a placed tile in the game.
 *
 * @param tile     non-null the tile to place
 * @param placer   the player placing the tile
 * @param rotation non-null the rotation of the tile when placed
 * @param pos      non-null the position of the tile when placed on the board
 * @param occupant an occupant on the tile, null if the player decided not to place any
 * @author Valerio De Santis (373247)
 * @author Simon Lefort (371918)
 */

public record PlacedTile(Tile tile, PlayerColor placer, Rotation rotation, Pos pos, Occupant occupant) {

    /**
     * Constructor for PlacedTile, validating the parameters
     *
     * @throws NullPointerException if any of the parameters is null
     */
    public PlacedTile {
        Objects.requireNonNull(tile);
        Objects.requireNonNull(rotation);
        Objects.requireNonNull(pos);
    }

    /**
     * Constructor for PlacedTile, with no occupant
     */
    public PlacedTile(Tile tile, PlayerColor placer, Rotation rotation, Pos pos) {
        this(tile, placer, rotation, pos, null);
    }

    /**
     * Gets the id of the placed tile
     *
     * @return the id of the placed tile
     */
    public int id() {
        return tile.id();
    }

    /**
     * Gets the kind of the placed tile
     *
     * @return the kind of the placed tile
     */
    public Tile.Kind kind() {
        return tile.kind();
    }

    /**
     * Gets the side of the placed tile in the given cardinal direction,
     * knowing that a placed tile is rotated at its instantiation
     *
     * @param direction the wanted direction
     * @return the side of the tile in the given direction
     */
    public TileSide side(Direction direction) {
        return tile.sides().get(direction.rotated(rotation.negated()).ordinal());
    }

    /**
     * Gets the zone of the placed tile with the given id
     *
     * @param id the id of the zone to recuperate
     * @return the zone of the placed tile with the given id if present, throws an exception otherwise
     * @throws IllegalArgumentException if there is no zone with the given id
     */
    public Zone zoneWithId(int id) {
        return tile.zones().stream()
                // we use a drop while rather than a filter because there is only one zone with the given id,
                // and we want to avoid iterating over the whole list
                .dropWhile(zone -> zone.id() != id)
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

    /**
     * Gets the zone of the placed tile having a special power (there may be only one at most),
     * null if there is none
     *
     * @return the zone of the placed tile having a special power if present, null otherwise
     */
    public Zone specialPowerZone() {
        return tile.zones().stream()
                // we use a drop while rather than a filter because there is only one zone with a special power,
                // and we want to avoid iterating over the whole list
                .dropWhile(zone -> zone.specialPower() == null)
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets the set of forest zones of the placed tile, returning an empty set if there are none
     *
     * @return the set of forest zones of the placed tile (empty if there are none)
     */
    public Set<Zone.Forest> forestZones() {
        Set<Zone.Forest> forestZones = new HashSet<>();
        for (Zone zone : tile.zones()) {
            if (zone instanceof Zone.Forest forest) {
                forestZones.add(forest);
            }
        }
        return forestZones;
    }

    /**
     * Gets the set of river zones of the placed tile, returning an empty set if there are none
     *
     * @return the set of river zones of the placed tile (empty if there are none)
     */
    public Set<Zone.River> riverZones() {
        Set<Zone.River> riverZones = new HashSet<>();
        for (Zone zone : tile.zones()) {
            if (zone instanceof Zone.River river) {
                riverZones.add(river);
            }
        }
        return riverZones;
    }

    /**
     * Gets the set of meadow zones of the placed tile, returning an empty set if there are none
     *
     * @return the set of meadow zones of the placed tile (empty if there are none)
     */
    public Set<Zone.Meadow> meadowZones() {
        Set<Zone.Meadow> meadowZones = new HashSet<>();
        for (Zone zone : tile.zones()) {
            if (zone instanceof Zone.Meadow meadow) {
                meadowZones.add(meadow);
            }
        }
        return meadowZones;
    }

    /**
     * Gets the set of the potential occupants that the player can place on the tile
     * depending on the zones of the tile
     *
     * @return the set of potential occupants that the player can place on the tile
     */
    public Set<Occupant> potentialOccupants() {
        if (placer == null) return Set.of();
        return Stream.concat(
                        tile.sideZones().stream()
                                .flatMap(zone -> {
                                    // each side zone can have a pawn
                                    Occupant pawn = new Occupant(Occupant.Kind.PAWN, zone.id());
                                    // and if it's a river, a hut (if there is no lake)
                                    return zone instanceof Zone.River river && !river.hasLake()
                                            ? Stream.of(pawn, new Occupant(Occupant.Kind.HUT, zone.id()))
                                            : Stream.of(pawn);
                                }),
                        // each lake can have a hut
                        tile.zones().stream()
                                .filter(zone -> zone instanceof Zone.Lake)
                                .map(lake -> new Occupant(Occupant.Kind.HUT, lake.id()))
                )
                .collect(Collectors.toSet());
    }

    /**
     * Adds an occupant to the placed tile if there is none yet, throws an exception otherwise
     *
     * @param occupant the occupant to add
     * @return the placed tile with the added occupant
     * @throws IllegalArgumentException if there is already an occupant
     */
    public PlacedTile withOccupant(Occupant occupant) {
        Preconditions.checkArgument(this.occupant == null);
        return new PlacedTile(tile, placer, rotation, pos, occupant);
    }

    /**
     * Removes the occupant from the placed tile if there is one, throws an exception otherwise
     *
     * @return the placed tile with no occupant
     */
    public PlacedTile withNoOccupant() {
        return new PlacedTile(tile, placer, rotation, pos);
    }

    /**
     * Gets the id of the zone occupied by the given kind of occupant if present, -1 otherwise
     *
     * @param occupantKind the kind of occupant to check
     * @return the id of the zone occupied by the given kind of occupant if present, -1 otherwise
     */
    public int idOfZoneOccupiedBy(Occupant.Kind occupantKind) {
        return occupant != null && occupantKind.equals(occupant.kind()) ? occupant.zoneId() : -1;
    }
}
