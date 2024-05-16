package ch.epfl.chacun.logic;

import ch.epfl.chacun.game.Preconditions;

import java.util.HashMap;
import java.util.Map;

public class GameLogic {

    private final Map<String, GameLobby> lobbies = new HashMap<>();

    /**
     * The list of games currently running.
     */
    private final Map<String, OnGoingGame> games = new HashMap<>();

    public ServerActionWithData parseAndApplyWebSocketAction(String action, String gameName, String username) {
        String[] payload = action.split("\\.");
        Preconditions.checkArgument(payload.length == 2);
        return applyAction(ServerAction.valueOf(payload[0]), payload[1].split(","), gameName, username);
    }

    private ServerActionWithData applyAction(ServerAction action, String[] data, String gameName, String username) {
        OnGoingGame game = games.get(gameName);
        return switch (action) {
            case GAMEJOIN -> {
                Preconditions.checkArgument(data.length == 2);
                Preconditions.checkArgument(!data[0].isBlank() && !data[1].isBlank());
                String providedGameName = data[0];
                String providedUsername = data[1];

                GameLobby lobby = lobbies.get(providedGameName);
                if (lobby != null) {
                    yield lobby.addPlayer(providedUsername);
                }

                if (game != null) {
                    // The game has already started
                    yield new ServerActionWithData(ServerAction.GAMEJOIN_DENY, "GAME_ALREADY_STARTED");
                }

                // Check if the game name is already taken
                if (lobbies.containsKey(providedGameName) || games.containsKey(providedGameName)) {
                    yield new ServerActionWithData(ServerAction.GAMEJOIN_DENY, "GAME_NAME_TAKEN");
                }
                // Create a new game lobby
                lobbies.put(providedGameName, new GameLobby(providedGameName, providedUsername));
                yield new ServerActionWithData(ServerAction.GAMEJOIN_ACCEPT);
            }
            case GAMEACTION -> {
                Preconditions.checkArgument(data.length == 1);

                GameLobby lobby = lobbies.get(gameName);
                if (lobby != null && lobby.getPlayers().getFirst().equals(username)) {
                    game = startGameWithLobby(lobby);
                }
                // Check if the game exists
                else if (game == null) {
                    // The game has not started yet
                    yield new ServerActionWithData(ServerAction.GAMEACTION_DENY, "GAME_NOT_STARTED");
                }

                yield game.applyAction(data[0], username);
            }
            case GAMELEAVE -> {
                GameLobby lobby = lobbies.get(gameName);
                if (lobby != null) {
                    yield lobby.removePlayer(username);
                }
                // Check if the game exists and has not ended
                if (game != null && !game.hasEnded()) {
                        yield new ServerActionWithData(ServerAction.GAMEEND, "PLAYER_LEFT_MIDGAME");
                }
                yield null;
            }
            case GAMEMSG -> {
                Preconditions.checkArgument(data.length == 1);
                yield new ServerActionWithData(ServerAction.GAMEMSG, data[0]);
            }
            default -> new ServerActionWithData(ServerAction.UNKNOWN);
        };
    }

    private OnGoingGame startGameWithLobby(GameLobby lobby) {
        OnGoingGame newGame = lobby.startGame();
        games.put(lobby.getGameName(), newGame);
        lobbies.remove(lobby.getGameName());
        return newGame;
    }
}
