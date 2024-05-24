package ch.epfl.chacun.logic;

import ch.epfl.chacun.game.Preconditions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents the logic of the game server.
 *
 * @author Maxence Espagnet (sciper: 372808)
 * @author Simon Lefort (sciper: 371918)
 */
public class GameLogic {

    /**
     * The minimum number of players required to start a game.
     */
    private static final int MINIMUM_PLAYERS = 2;

    /**
     * All the game lobbies currently open.
     * A game lobby is a group of players waiting for a game to start.
     */
    private final Map<String, GameLobby> lobbies = new HashMap<>();

    /**
     * All the games currently ongoing.
     */
    private final Map<String, OnGoingGame> games = new HashMap<>();

    /**
     * Parse and apply an action received by the WebSocket.
     *
     * @param action  The action to parse and apply.
     * @param context The context of the player sending the action.
     * @return The response action to send back to the player or broadcast to the game.
     */
    public GameActionData parseAndApplyWebSocketAction(String action, GamePlayerData context) {
        String[] payload = action.split("\\.");
        System.out.println(Arrays.toString(payload));
        ServerAction serverAction = ServerAction.fromString(payload[0]);
        if (serverAction != ServerAction.UNKNOWN) {
            String gameName = context != null ? context.gameName() : null;
            String username = context != null ? context.username() : null;
            String[] data = payload.length > 1 ? payload[1].split(",") : new String[0];
            return applyAction(serverAction, data, gameName, username);
        }
        return null;
    }

    /**
     * Try to apply the action to a lobby or an ongoing game and determines the response to send.
     *
     * @param action   The action to apply.
     * @param data     The data of the action.
     * @param gameName The name of the game the player is in.
     * @param username The username of the player sending the action.
     * @return The response action to send back to the player or broadcast to the game.
     */
    private GameActionData applyAction(ServerAction action, String[] data, String gameName, String username) {
        GameLobby lobby = lobbies.get(gameName);
        OnGoingGame game = games.get(gameName);

        switch (action) {
            /*
             * The player tries to join a game lobby.
             */
            case GAMEJOIN -> {
                // Check if the provided data is valid
                if (data.length != 2)
                    return new GameActionData(ServerAction.GAMEJOIN_DENY, "INVALID_DATA");

                // Extract the game name and the username from the data
                String providedGameName = data[0];
                String providedUsername = data[1];

                // Check if the player is already in a game
                if (gameName != null)
                    return new GameActionData(ServerAction.GAMEJOIN_DENY, "ALREADY_IN_GAME");
                // Check if the lobby already exists
                GameLobby alreadyExistingLobby = lobbies.get(providedGameName);
                if (alreadyExistingLobby != null) {
                    return alreadyExistingLobby.addPlayer(providedUsername);
                }
                // Check if the game has already started
                if (games.get(providedGameName) != null) {
                    return new GameActionData(ServerAction.GAMEJOIN_DENY, "GAME_ALREADY_STARTED");
                }

                // Create a new game lobby
                lobbies.put(providedGameName, new GameLobby(providedGameName, providedUsername));
                return new GameActionData(ServerAction.GAMEJOIN_ACCEPT, providedUsername,
                        new GamePlayerData(providedGameName, providedUsername));
            }
            /*
             * The player tries to perform an action in the game.
             */
            case GAMEACTION -> {
                // Check if the lobby has enough players to start the game.
                // The game is started if the first player to join plays.
                if (lobby != null && lobby.getPlayers().size() >= MINIMUM_PLAYERS
                        && lobby.getPlayers().getFirst().equals(username)) {
                    game = startGameWithLobby(lobby);
                }

                // Check if the game has started
                if (game == null)
                    return new GameActionData(ServerAction.GAMEACTION_DENY, "GAME_NOT_STARTED");

                // Try to apply the action to the game
                GameActionData nextServerAction = game.applyAction(data[0], username);
                // If the game has ended, put everyone back in the lobby
                if (game.hasEnded())
                    downgradeGameToLobby(game);
                return nextServerAction;
            }
            /*
             * The player tries to leave a lobby or its current game.
             */
            case GAMELEAVE -> {
                // Check if the player is in a lobby
                if (lobby != null)
                    return lobby.removePlayer(username);
                // Check if the game exists and has not ended
                if (game != null && !game.hasEnded()) {
                    // Cancel the game and put everyone back in the lobby
                    GameLobby newLobby = downgradeGameToLobby(game);
                    return newLobby.removePlayer(username);
                }
                return null;
            }
            /*
             * The player tries to send a message to the game.
             */
            case GAMEMSG -> {
                // Check if the game exists and the player is in it.
                if (game != null || lobby != null) {
                    String message = STR."\{username}=\{data[0]}";
                    return new GameActionData(ServerAction.GAMEMSG, message, true);
                }
                return new GameActionData(ServerAction.GAMEMSG_DENY, "GAME_HAS_ENDED");
            }
        }
        return null;
    }

    /**
     * Cancel a game and return the players to the lobby.
     *
     * @param game The game to cancel.
     * @return The lobby that has been created.
     */
    private GameLobby downgradeGameToLobby(OnGoingGame game) {
        String gameName = game.getName();
        Preconditions.checkArgument(games.containsKey(gameName));
        GameLobby newLobby = new GameLobby(gameName, game.getPlayers().values());
        lobbies.put(gameName, newLobby);
        games.remove(gameName);
        return newLobby;
    }

    /**
     * Start a game with the provided lobby content.
     *
     * @param lobby The lobby to start the game with.
     * @return The game that has been started.
     */
    private OnGoingGame startGameWithLobby(GameLobby lobby) {
        OnGoingGame newGame = lobby.startGame();
        games.put(lobby.getGameName(), newGame);
        lobbies.remove(lobby.getGameName());
        return newGame;
    }
}
