/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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

package net.sf.freecol.server.control;

import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.DOMMessage;
import net.sf.freecol.common.networking.LoginMessage;
import net.sf.freecol.common.networking.MessageHandler;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.networking.Server;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * Handles a new client connection.  {@link PreGameInputHandler} is
 * set as the message handler when the client has successfully logged
 * on.
 */
public final class UserConnectionHandler extends FreeColServerHolder
    implements MessageHandler {

    private static final Logger logger = Logger.getLogger(UserConnectionHandler.class.getName());


    /**
     * The constructor to use.
     *
     * @param freeColServer The main control object.
     */
    public UserConnectionHandler(FreeColServer freeColServer) {
        super(freeColServer);
    }


    // Implement MessageHandler

    /**
     * Handles a network message.
     *
     * @param conn The <code>Connection</code> the message came from.
     * @param element The message to be processed.
     * @return The reply.
     */
    @Override
    public synchronized Element handle(Connection conn, Element element) {
        final String tag = element.getTagName();
        return ("disconnect".equals(tag)) 
            ? disconnect(conn, element)
            : ("gameState".equals(tag))
            ? gameState(conn, element)
            : ("getVacantPlayers".equals(tag))
            ? getVacantPlayers(conn, element)
            : ("login".equals(tag))
            ? login(conn, element)
            : unknown(tag);
    }

    // Individual message handlers

    /**
     * Handles a "disconnect"-message.
     *
     * @param connection The <code>Connection</code> the message was
     *     received on.
     * @param element The <code>Element</code> (root element in a
     *     DOM-parsed XML tree) that holds all the information.
     * @return The reply.
     */
    private Element disconnect(Connection connection,
        @SuppressWarnings("unused") Element element) {
        connection.reallyClose();
        return null;
    }

    /**
     * Handles a "gameState"-request.
     *
     * @param connection The connection the message came from.
     * @param element The element containing the request.
     * @return An element with a "gameState" attribute set.
     */
    private Element gameState(
        @SuppressWarnings("unused") Connection connection,
        @SuppressWarnings("unused") Element element) {
        final FreeColServer freeColServer = getFreeColServer();

        Element reply = DOMMessage.createMessage("gameState");
        reply.setAttribute("state",
                           freeColServer.getGameState().toString());
        return reply;
    }

    /**
     * Handles a "getVacantPlayers"-request.
     *
     * @param connection The connection the message came from.
     * @param element The element containing the request.
     * @return Null on error (such as requesting during end game), an empty
     *     list if the game is starting, or a list of all the inactive
     *     European players.
     */
    private Element getVacantPlayers(
        @SuppressWarnings("unused") Connection connection,
        @SuppressWarnings("unused") Element element) {
        final FreeColServer freeColServer = getFreeColServer();
        final Game game = getGame();

        Element reply = DOMMessage.createMessage("vacantPlayers");
        Document doc = reply.getOwnerDocument();
        for (Player p : game.getLiveEuropeanPlayers(null)) {
            if (!p.isREF()
                && (p.isAI() || !((ServerPlayer)p).isConnected())) {
                Element playerElement = doc.createElement("player");
                playerElement.setAttribute("username", p.getNationId());
                reply.appendChild(playerElement);
            }
        }
        return reply;
    }

    /**
     * Handles a "login"-request.
     *
     * FIXME: Do not allow more than one (human) player to connect
     * to a single player game. This would be easy if we used a
     * dummy connection for single player games.
     *
     * @param connection The <code>Connection</code> the message was
     *     received on.
     * @param element The <code>Element</code> (root element in a
     *     DOM-parsed XML tree) that holds all the information.
     * @return The reply.
     */
    private Element login(Connection connection, Element element) {
        final String userName = element.getAttribute("userName");
        final String version = element.getAttribute("version");

        if (userName == null || userName.isEmpty()) {
            return DOMMessage.createError("server.missingUserName", null);
        } else if (version == null || version.isEmpty()) {
            return DOMMessage.createError("server.missingVersion", null);
        } else if (!version.equals(FreeCol.getVersion())) {
            return DOMMessage.createError("server.wrongFreeColVersion",
                version + " != " + FreeCol.getVersion());
        }

        final FreeColServer freeColServer = getFreeColServer();
        final Server server = freeColServer.getServer();
        Game game;
        ServerPlayer player;
        Unit active = null;
        boolean isCurrentPlayer = false;
        MessageHandler mh;
        boolean starting = freeColServer.getGameState()
            == FreeColServer.GameState.STARTING_GAME;
        if (starting) {
            // Wait until the game has been created.
            // FIXME: is this still needed?
            int timeOut = 20000;
            while (freeColServer.getGame() == null) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {}
                if ((timeOut -= 1000) <= 0) {
                    return DOMMessage.createError("server.timeOut", null);
                }
            }

            game = freeColServer.getGame();
            if (!game.canAddNewPlayer()) {
                return DOMMessage.createError("server.maximumPlayers", null);
            } else if (game.playerNameInUse(userName)) {
                return DOMMessage.createError("server.userNameInUse",
                    userName + " is already in use.");
            }

            // Create and add the new player:
            boolean admin = game.getLivePlayers(null).isEmpty();
            player = new ServerPlayer(game, admin, game.getVacantNation(),
                                      connection.getSocket(), connection);
            player.setName(userName);
            game.addPlayer(player);

            // Send message to all players except to the new player.
            // FIXME: check visibility.
            Element add = DOMMessage.createMessage("addPlayer");
            add.appendChild(player.toXMLElement(add.getOwnerDocument()));
            server.sendToAll(add, connection);

            // Ready now to handle pre-game messages.
            mh = freeColServer.getPreGameInputHandler();

        } else { // Restoring from existing game.
            game = freeColServer.getGame();
            player = (ServerPlayer)game.getPlayerByName(userName);
            if (player == null) {
                StringBuilder sb = new StringBuilder("Player \"");
                sb.append(userName).append("\" is not present in the game.")
                    .append("\n  Known players = ( ");
                for (Player p : game.getLiveEuropeanPlayers(null)) {
                    sb.append(p.getName()).append(" ");
                }
                sb.append(")");
                return DOMMessage.createError("server.userNameNotPresent",
                                              sb.toString());
            } else if (player.isConnected() && !player.isAI()) {
                return DOMMessage.createError("server.userNameInUse",
                    userName + " is already in use.");
            }
            player.setConnection(connection);
            player.setConnected(true);

            if (player.isAI()) {
                player.setAI(false);
                server.sendToAll(DOMMessage.createMessage("setAI",
                        "player", player.getId(),
                        "ai", Boolean.toString(false)));
            }

            // If this player is the first to reconnect, it is the
            // current player.
            isCurrentPlayer = game.getCurrentPlayer() == null;
            if (isCurrentPlayer) {
                game.setCurrentPlayer(player);
                active = freeColServer.getActiveUnit();
            }

            // Go straight into the game.
            mh = freeColServer.getInGameInputHandler();
        }

        connection.setMessageHandler(mh);
        server.addConnection(connection);
        freeColServer.updateMetaServer();
        return new LoginMessage(player, userName, version, !starting,
                                freeColServer.getSinglePlayer(),
                                isCurrentPlayer, active,
                                game).toXMLElement();
    }

    /**
     * Gripe about an unknown tag.
     *
     * @param tag The unknown tag.
     * @return Null.
     */
    private Element unknown(String tag) {
        logger.warning("Unknown user connection request: " + tag);
        return null;
    }
}
