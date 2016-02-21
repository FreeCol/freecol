/**
 *  Copyright (C) 2002-2016   The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.common.networking;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.networking.AddPlayerMessage;
import net.sf.freecol.common.networking.ErrorMessage;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when logging in.
 */
public class LoginMessage extends DOMMessage {

    public static final String TAG = "login";
    private static final String ADMIN_TAG = "admin";
    private static final String CURRENT_PLAYER_TAG = "currentPlayer";
    private static final String SINGLE_PLAYER_TAG = "singlePlayer";
    private static final String START_GAME_TAG = "startGame";
    private static final String USER_NAME_TAG = "userName";
    private static final String VERSION_TAG = "version";
    
    /** The user name. */
    private final String userName;

    /** The client FreeCol version. */
    private final String version;

    /** Is the player an admin. */
    private final boolean admin;

    /** Whether to start the game. */
    private final boolean startGame;

    /** Is this a single player game. */
    private final boolean singlePlayer;

    /** Is the client the current player. */
    private final boolean currentPlayer;

    /** The game. */
    private final Game game;

        
    /**
     * Create a new <code>LoginMessage</code> with the supplied parameters.
     *
     * @param userName The name of the user logging in.
     * @param version The version of FreeCol at the client.
     * @param admin Is the player an administrator?
     * @param startGame Whether to start the game.
     * @param singlePlayer True in single player games.
     * @param currentPlayer True if this player is the current player.
     * @param game The entire game.
     */
    public LoginMessage(String userName, String version,
                        boolean admin, boolean startGame, boolean singlePlayer,
                        boolean currentPlayer, Game game) {
        super(getTagName());

        this.userName = userName;
        this.version = version;
        this.admin = admin;
        this.startGame = startGame;
        this.singlePlayer = singlePlayer;
        this.currentPlayer = currentPlayer;
        this.game = game;
    }

    /**
     * Create a new <code>LoginMessage</code> request with name and version
     * only.
     *
     * @param userName The name of the user logging in.
     * @param version The version of FreeCol at the client.
     */
    public LoginMessage(String userName, String version) {
        this(userName, version, false, false, false, false, null);
    }

    /**
     * Create a new <code>LoginMessage</code> from a supplied element.
     *
     * @param game A <code>Game</code> (not used).
     * @param e The <code>Element</code> to use to create the message.
     */
    public LoginMessage(Game game, Element e) {
        super(getTagName());

        this.userName = getStringAttribute(e, USER_NAME_TAG);
        this.version = getStringAttribute(e, VERSION_TAG);
        this.admin = getBooleanAttribute(e, ADMIN_TAG, false);
        this.startGame = getBooleanAttribute(e, START_GAME_TAG, false);
        this.singlePlayer = getBooleanAttribute(e, SINGLE_PLAYER_TAG, true);
        this.currentPlayer = getBooleanAttribute(e, CURRENT_PLAYER_TAG, false);
        this.game = getChild(game, e, 0, true, Game.class);
    }


    // Public interface

    public String getUserName() {
        return this.userName;
    }

    public String getVersion() {
        return this.version;
    }

    public boolean isAdmin() {
        return this.admin;
    }

    public boolean getStartGame() {
        return this.startGame;
    }

    public boolean isSinglePlayer() {
        return this.singlePlayer;
    }

    public boolean isCurrentPlayer() {
        return this.currentPlayer;
    }

    public Game getGame() {
        return this.game;
    }


    /**
     * Handle a "login"-message.
     *
     * FIXME: Do not allow more than one (human) player to connect
     * to a single player game. This would be easy if we used a
     * dummy connection for single player games.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param connection The <code>Connection</code> message was received on.
     * @return An <code>Element</code> to update the originating player
     *     with the result of the query.
     */
    public Element handle(FreeColServer server, Connection connection) {
        if (this.userName == null || this.userName.isEmpty()) {
            return new ErrorMessage("server.missingUserName", null)
                .toXMLElement();
        } else if (this.version == null || this.version.isEmpty()) {
            return new ErrorMessage("server.missingVersion", null)
                .toXMLElement();
        } else if (!this.version.equals(FreeCol.getVersion())) {
            return new ErrorMessage("server.wrongFreeColVersion",
                this.version + " != " + FreeCol.getVersion()).toXMLElement();
        }

        Game game;
        ServerPlayer player;
        boolean isCurrentPlayer = false;
        MessageHandler mh;
        boolean starting = server.getGameState()
            == FreeColServer.GameState.STARTING_GAME;
        if (starting) {
            // Wait until the game has been created.
            // FIXME: is this still needed?
            int timeOut = 20000;
            while ((game = server.getGame()) == null) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {}
                if ((timeOut -= 1000) <= 0) {
                    return new ErrorMessage("server.timeOut", null)
                        .toXMLElement();
                }
            }

            if (!game.canAddNewPlayer()) {
                return new ErrorMessage("server.maximumPlayers", null)
                    .toXMLElement();
            } else if (game.playerNameInUse(this.userName)) {
                return new ErrorMessage("server.userNameInUse",
                    this.userName + " is already in use.").toXMLElement();
            }

            // Create and add the new player:
            boolean admin = game.getLivePlayers(null).isEmpty();
            player = new ServerPlayer(game, admin, game.getVacantNation(),
                                      connection.getSocket(), connection);
            player.setName(userName);
            game.addPlayer(player);

            // Send message to all players except to the new player.
            // FIXME: check visibility.
            server.sendToAll(new AddPlayerMessage(player), connection);

            // Ready now to handle pre-game messages.
            mh = server.getPreGameInputHandler();

        } else { // Restoring from existing game.
            game = server.getGame();
            player = (ServerPlayer)game.getPlayerByName(this.userName);
            if (player == null) {
                StringBuilder sb = new StringBuilder("Player \"");
                sb.append(userName).append("\" is not present in the game.")
                    .append("\n  Known players = ( ");
                for (Player p : game.getLiveEuropeanPlayers(null)) {
                    sb.append(p.getName()).append(" ");
                }
                sb.append(")");
                return new ErrorMessage("server.userNameNotPresent",
                    sb.toString()).toXMLElement();
            } else if (player.isConnected() && !player.isAI()) {
                return new ErrorMessage("server.userNameInUse",
                    this.userName + " is already in use.").toXMLElement();
            }
            player.setConnection(connection);
            player.setConnected(true);

            if (player.isAI()) {
                player.setAI(false);
                server.sendToAll(new DOMMessage("setAI",
                        "player", player.getId(),
                        "ai", Boolean.toString(false)),
                    null);
            }

            // If this player is the first to reconnect, it is the
            // current player.
            isCurrentPlayer = game.getCurrentPlayer() == null;
            if (isCurrentPlayer) game.setCurrentPlayer(player);

            // Go straight into the game.
            mh = server.getInGameInputHandler();
        }

        connection.setMessageHandler(mh);
        server.getServer().addConnection(connection);
        server.updateMetaServer();
        return new LoginMessage(this.userName, this.version, player.isAdmin(),
                                !starting, server.getSinglePlayer(),
                                isCurrentPlayer, game).toXMLElement();
    }

    /**
     * Convert this LoginMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        Player player = (this.game == null || this.userName == null) ? null
            : this.game.getPlayerByName(this.userName);
        return new DOMMessage(getTagName(),
            USER_NAME_TAG, this.userName,
            VERSION_TAG, this.version,
            ADMIN_TAG, Boolean.toString(this.admin),
            START_GAME_TAG, Boolean.toString(this.startGame),
            SINGLE_PLAYER_TAG, Boolean.toString(this.singlePlayer),
            CURRENT_PLAYER_TAG, Boolean.toString(this.currentPlayer))
            .add(this.game, player)
            .toXMLElement();
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "login".
     */
    public static String getTagName() {
        return TAG;
    }
}
