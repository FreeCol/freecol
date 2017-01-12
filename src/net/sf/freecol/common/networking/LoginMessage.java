/**
 *  Copyright (C) 2002-2017   The FreeCol Team
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

import java.util.Collections;
import java.util.stream.Collectors;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.networking.ChangeSet.See;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.FreeColServer.ServerState;
import net.sf.freecol.server.model.ServerGame;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when logging in.
 */
public class LoginMessage extends ObjectMessage {

    public static final String TAG = "login";
    private static final String CURRENT_PLAYER_TAG = "currentPlayer";
    private static final String SINGLE_PLAYER_TAG = "singlePlayer";
    private static final String STATE_TAG = "state";
    private static final String USER_NAME_TAG = "userName";
    private static final String VERSION_TAG = "version";
    
    /** The user name. */
    private final String userName;

    /** The client FreeCol version. */
    private final String version;

    /** The server state. */
    private final ServerState state;
    
    /** Is this a single player game. */
    private final boolean singlePlayer;

    /** Is the client the current player. */
    private final boolean currentPlayer;

    /** The game. */
    private final Game game;

        
    /**
     * Create a new {@code LoginMessage} with the supplied parameters.
     *
     * @param userName The name of the user logging in.
     * @param version The version of FreeCol at the client.
     * @param state The server state.
     * @param singlePlayer True in single player games.
     * @param currentPlayer True if this player is the current player.
     * @param game The entire game.
     */
    public LoginMessage(String userName, String version, ServerState state,
                        boolean singlePlayer, boolean currentPlayer, Game game) {
        super(TAG);

        this.userName = userName;
        this.version = version;
        this.state = state;
        this.singlePlayer = singlePlayer;
        this.currentPlayer = currentPlayer;
        this.game = game;
    }

    /**
     * Create a new {@code LoginMessage} from a supplied element.
     *
     * @param game A {@code Game} (not used).
     * @param e The {@code Element} to use to create the message.
     */
    public LoginMessage(Game game, Element e) {
        this(getStringAttribute(e, USER_NAME_TAG),
             getStringAttribute(e, VERSION_TAG),
             getEnumAttribute(e, STATE_TAG,
                              ServerState.class, (ServerState)null),
             getBooleanAttribute(e, SINGLE_PLAYER_TAG, true),
             getBooleanAttribute(e, CURRENT_PLAYER_TAG, false),
             getChild(game, e, 0, Game.class));
    }

    /**
     * Create a new {@code LoginMessage} from a stream.
     *
     * @param game A {@code Game} (not actually used, the actual game is read
     *     from the stream).
     * @param xr The {@code FreeColXMLReader} to read the message from.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    public LoginMessage(@SuppressWarnings("unused")Game game,
                        FreeColXMLReader xr)
        throws XMLStreamException {
        super(TAG);
        
        this.userName = xr.getAttribute(USER_NAME_TAG, (String)null);
        this.version = xr.getAttribute(VERSION_TAG, (String)null);
        this.state = xr.getAttribute(STATE_TAG, ServerState.class, (ServerState)null);
        this.singlePlayer = xr.getAttribute(SINGLE_PLAYER_TAG, true);
        this.currentPlayer = xr.getAttribute(CURRENT_PLAYER_TAG, false);

        xr.nextTag();
        final String tag = xr.getLocalName();
        this.game = (Game.TAG.equals(tag)) ? new Game(null, xr) : null;
        xr.closeTag(TAG);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public MessagePriority getPriority() {
        return MessagePriority.EARLY;
    }
        
    /**
     * {@inheritDoc}
     */
    @Override
    public void clientHandler(FreeColClient freeColClient) {
        freeColClient.getConnectController()
            .login(this.state, this.game, this.userName,
                   this.singlePlayer, this.currentPlayer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        // Note: At this point serverPlayer is just a stub, with only
        // the connection infomation being valid.
        
        // FIXME: Do not allow more than one (human) player to connect
        // to a single player game. This would be easy if we used a
        // dummy connection for single player games.

        if (this.userName == null || this.userName.isEmpty()) {
            return ChangeSet.clientError((ServerPlayer)null, StringTemplate
                .template("server.missingUserName"));
        } else if (this.version == null || this.version.isEmpty()) {
            return ChangeSet.clientError((ServerPlayer)null, StringTemplate
                .template("server.missingVersion"));
        } else if (!this.version.equals(FreeCol.getVersion())) {
            return ChangeSet.clientError((ServerPlayer)null, StringTemplate
                .template("server.wrongFreeColVersion")
                .addName("%clientVersion%", this.version)
                .addName("%serverVersion%", FreeCol.getVersion()));
        }

        Connection conn = serverPlayer.getConnection();
        ServerGame serverGame;
        ServerPlayer present;
        boolean isCurrentPlayer = false;

        switch (freeColServer.getServerState()) {
        case PRE_GAME:
            if ((serverGame = freeColServer.waitForGame()) == null) {
                return ChangeSet.clientError((ServerPlayer)null,
                    StringTemplate.template("server.timeOut"));
            }

            Nation nation = serverGame.getVacantNation();
            if (nation == null) {
                return ChangeSet.clientError((ServerPlayer)null, StringTemplate
                    .template("server.maximumPlayers"));
            }
            present = getPlayer(serverGame);
            if (present != null) {
                return ChangeSet.clientError((ServerPlayer)null, StringTemplate
                    .template("server.userNameInUse")
                    .addName("%name%", this.userName));
            }

            // Complete initialization...
            serverPlayer.initialize(serverGame,
                                    serverGame.getLivePlayerList().isEmpty(),
                                    nation);

            // ... but override player name.
            serverPlayer.setName(this.userName);

            // Add the new player and inform all other players
            ChangeSet cs = new ChangeSet();
            serverGame.addPlayer(serverPlayer);
            cs.addNewPlayer(serverPlayer);

            // Ensure there is a current player.
            if (serverGame.getCurrentPlayer() == null) {
                serverGame.setCurrentPlayer(serverPlayer);
            }

            // Add the connection, send back the game
            freeColServer.addPlayerConnection(conn);
            cs.add(See.only(serverPlayer),
                   new LoginMessage(this.userName, this.version,
                                    freeColServer.getServerState(),
                                    freeColServer.getSinglePlayer(),
                                    serverGame.getCurrentPlayer() == serverPlayer,
                                    serverGame));
            return cs;

        case LOAD_GAME:
            if (FreeColServer.MAP_EDITOR_NAME.equals(this.userName)) {
                // Trying to start a map, see BR#2976 -> IR#217
                return ChangeSet.clientError((ServerPlayer)null, StringTemplate
                    .template("error.mapEditorGame"));
            }
            serverGame = freeColServer.getGame(); // Restoring from existing.
            present = getPlayer(serverGame);
            if (present == null) {
                return ChangeSet.clientError((ServerPlayer)null, StringTemplate
                    .template("server.userNameNotPresent")
                    .addName("%name%", userName)
                    .addName("%names%",
                        transform(serverGame.getLiveEuropeanPlayers(),
                                  alwaysTrue(), Player::getName,
                                  Collectors.joining(", "))));
            } else if (present.isConnected()) {
                // Another player already connected on the name
                return ChangeSet.clientError((ServerPlayer)null, StringTemplate
                    .template("server.userNameInUse")
                    .addName("%name%", this.userName));
            } else if (present.isAI()) {
                // Should not connect to AI
                return ChangeSet.clientError((ServerPlayer)null, StringTemplate
                    .template("server.userNameInUse")
                    .addName("%name%", this.userName));
            }

            present.setConnection(conn);

            // Ensure there is a current player.
            if (serverGame.getCurrentPlayer() == null) {
                serverGame.setCurrentPlayer(present);
            }

            // Add the connection, send back the game
            freeColServer.addPlayerConnection(conn);
            return ChangeSet.simpleChange(present,
                new LoginMessage(this.userName, this.version,
                                 freeColServer.getServerState(),
                                 freeColServer.getSinglePlayer(),
                                 serverGame.getCurrentPlayer() == present,
                                 serverGame));

        case IN_GAME:
            serverGame = freeColServer.getGame(); // Restoring existing game.
            present = getPlayer(serverGame);
            if (present == null) {
                return ChangeSet.clientError((ServerPlayer)null, StringTemplate
                    .template("server.userNameNotPresent")
                    .addName("%name%", userName)
                    .addName("%names%",
                        transform(serverGame.getLiveEuropeanPlayers(),
                                  alwaysTrue(), Player::getName,
                            Collectors.joining(", "))));
            } else if (present.isAI()) { // Allow to join over AI player
                serverGame.changeAI(present, false);
            } else if (present.isConnected()) {
                // Another player already connected on the name
                return ChangeSet.clientError((ServerPlayer)null, StringTemplate
                    .template("server.userNameInUse")
                    .addName("%name%", this.userName));
            }

            present.setConnection(conn);

            // Add the connection, send back the game
            freeColServer.addPlayerConnection(conn);
            return ChangeSet.simpleChange(present,
                new LoginMessage(this.userName, this.version,
                                 freeColServer.getServerState(),
                                 freeColServer.getSinglePlayer(),
                                 serverGame.getCurrentPlayer() == present,
                                 serverGame));
            
        case END_GAME: default:
            break;
        }
        return ChangeSet.clientError((ServerPlayer)null, StringTemplate
            .template("server.couldNotLogin"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Element toXMLElement() {
        Player player = (this.game == null || this.userName == null) ? null
            : getPlayer(this.game);
        String state = (this.state == null) ? "" : this.state.toString();
        return new DOMMessage(TAG,
            USER_NAME_TAG, this.userName,
            VERSION_TAG, this.version,
            STATE_TAG, state,
            SINGLE_PLAYER_TAG, Boolean.toString(this.singlePlayer),
            CURRENT_PLAYER_TAG, Boolean.toString(this.currentPlayer))
            .add(this.game, player)
            .toXMLElement();
    }


    // Public interface

    public String getUserName() {
        return this.userName;
    }

    public String getVersion() {
        return this.version;
    }

    public ServerState getState() {
        return this.state;
    }

    public boolean getSinglePlayer() {
        return this.singlePlayer;
    }

    public boolean getCurrentPlayer() {
        return this.currentPlayer;
    }

    public Game getGame() {
        return this.game;
    }

    /**
     * Get the player (if any) with the current name in a given game.
     *
     * @param game The {@code Game} to look up.
     * @return The {@code ServerPlayer} found.
     */
    public ServerPlayer getPlayer(Game game) {
        return (ServerPlayer)game.getPlayerByName(this.userName);
    }
}
