package ch.epfl.chacun.logic;

import ch.epfl.chacun.game.PlayerColor;
import ch.epfl.chacun.game.Preconditions;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A builder for creating game lobbies.
 */
public class GameLobby {

    /**
     * The name of the game.
     */
    private final String gameName;

    /**
     * The list of players in the game lobby.
     */
    private final List<String> players = new ArrayList<>(PlayerColor.ALL.size());

    /**
     * Create a new game lobby builder with the provided game name.
     * @param gameName The name of the game.
     */
    public GameLobby(String gameName) {
        this.gameName = gameName;
    }

    /**
     * Create a new game lobby builder with the provided game name and player.
     * @param gameName The name of the game.
     * @param username  The username of the player.
     */
    public GameLobby(String gameName, String username) {
        this.gameName = gameName;
        this.players.add(username);
    }

    /**
     * Returns the list of player usernames in the game lobby.
     * @return The list of player usernames in the game lobby.
     */
    public List<String> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    /**
     * Returns the name of the game.
     * @return The name of the game.
     */
    public String getGameName() {
        return gameName;
    }

    /**
     * Add a player to the game lobby.
     * @param username The username of the player to add.
     * @return {@code GameLogic.ServerAction.GAMEJOIN_ACCEPT} if the player was added,
     * {@code GameLogic.ServerAction.GAMEJOIN_DENY} otherwise.
     */
    public ServerActionWithData addPlayer(String username) {
        if (players.contains(username)) {
            return new ServerActionWithData(ServerAction.GAMEJOIN_DENY, "USERNAME_TAKEN");
        }
        if (players.size() == PlayerColor.ALL.size()) {
            return new ServerActionWithData(ServerAction.GAMEJOIN_DENY, "GAME_FULL");
        }
        players.add(username);
        return new ServerActionWithData(ServerAction.GAMEJOIN_ACCEPT, String.join(",", players));
    }

    /**
     * Remove a player from the game lobby.
     * @param username The username of the player to remove.
     * @throws IllegalArgumentException If the player is not in the game lobby.
     */
    public ServerActionWithData removePlayer(String username) {
        players.remove(username);
        return new ServerActionWithData(ServerAction.GAMELEAVE, String.join(",", players));
    }

    /**
     * Maps the provided usernames to player colors.
     * @param usernames The usernames to map.
     * @return The mapping of usernames to player colors.
     */
    private Map<PlayerColor, String> mapUsernamesToColors(List<String> usernames) {
        Preconditions.checkArgument(usernames.size() <= PlayerColor.ALL.size());
        return IntStream.range(0, usernames.size()).boxed()
                .collect(Collectors.toMap(PlayerColor.ALL::get, usernames::get));
    }

    /**
     * Start the game with the current players.
     * @return The game that has been started.
     */
    public OnGoingGame startGame() {
        return new OnGoingGame(gameName, mapUsernamesToColors(players));
    }

}
