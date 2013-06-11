/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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

import java.io.IOException;
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
import net.sf.freecol.common.networking.NoRouteToServerException;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.networking.Server;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * Handles a new client connection. {@link PreGameInputHandler} is set
 * as the message handler when the client has successfully logged on.
 */
public final class UserConnectionHandler implements MessageHandler {

    private static Logger logger = Logger.getLogger(UserConnectionHandler.class.getName());


    /** The main server object. */
    private final FreeColServer freeColServer;


    /**
     * The constructor to use.
     *
     * @param freeColServer The main control object.
     */
    public UserConnectionHandler(FreeColServer freeColServer) {
        this.freeColServer = freeColServer;
    }


    /**
     * Handles a network message.
     *
     * @param conn The <code>Connection</code> the message came from.
     * @param element The message to be processed.
     * @return The reply.
     */
    public synchronized Element handle(Connection conn, Element element) {
        String type = (element == null) ? "(null)" : element.getTagName();
        return ("disconnect".equals(type)) 
            ? disconnect(conn, element)
            : ("getVacantPlayers".equals(type))
            ? getVacantPlayers(conn, element)
            : ("login".equals(type))
            ? login(conn, element)
            : unknown(type);
    }

    private Element unknown(String type) {
        logger.warning("Unknown user connection request: " + type);
        return null;
    }

    /**
     * Handles a "disconnect"-message.
     *
     * @param connection The <code>Connection</code> the message was
     *     received on.
     * @param element The <code>Element</code> (root element in a
     *     DOM-parsed XML tree) that holds all the information.
     * @return The reply.
     */
    private Element disconnect(Connection connection, Element element) {
        try {
            connection.reallyClose();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not close the connection.", e);
        }
        return null;
    }

    /**
     * Handles a "login"-request.
     *
     * TODO: Do not allow more than one (human) player to connect
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

        if (userName == null || "".equals(userName)) {
            return DOMMessage.createError("server.missingUserName", null);
        } else if (version == null || "".equals(version)) {
            return DOMMessage.createError("server.missingVersion", null);
        } else if (!version.equals(FreeCol.getVersion())) {
            return DOMMessage.createError("server.wrongFreeColVersion",
                version + " != " + FreeCol.getVersion());
        }

        Game game;
        ServerPlayer player;
        Server server = freeColServer.getServer();
        Unit active = null;
        boolean isCurrentPlayer = false;
        MessageHandler mh;
        boolean starting = freeColServer.getGameState()
            == FreeColServer.GameState.STARTING_GAME;
        if (starting) {
            // Wait until the game has been created.
            // TODO: is this still needed?
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
            boolean admin = game.getPlayers().size() == 0;
            player = new ServerPlayer(game, userName, admin,
                                      game.getVacantNation(),
                                      connection.getSocket(), connection);
            game.addPlayer(player);

            // Send message to all players except to the new player:
            Element add = DOMMessage.createMessage("addPlayer");
            add.appendChild(player.toXMLElement(null, add.getOwnerDocument()));
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
                for (Player p : game.getPlayers()) {
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
        try {
            freeColServer.updateMetaServer();
        } catch (NoRouteToServerException e) {
            logger.log(Level.WARNING, "Unable to update meta-server.", e);
        }
        return new LoginMessage(player, userName, version, !starting,
                                freeColServer.isSinglePlayer(),
                                isCurrentPlayer, active,
                                game).toXMLElement();
    }

    /**
     * Handles a "getVacantPlayers"-request.
     *
     * @param connection The connection the message came from.
     * @param element The element containing the request.
     * @return The reply: An XML element containing a list of the
     *       vacant players.
     */
    private Element getVacantPlayers(Connection connection, Element element) {
        Game game = freeColServer.getGame();
        if (freeColServer.getGameState()
            == FreeColServer.GameState.STARTING_GAME) {
            return null;
        }

        Element reply = DOMMessage.createMessage("vacantPlayers");
        Document doc = reply.getOwnerDocument();
        for (Player player : game.getPlayers()) {
            if (!player.isDead()
                && player.isEuropean()
                && !player.isREF()
                && (!((ServerPlayer)player).isConnected() || player.isAI())) {
                Element playerElement = doc.createElement("player");
                playerElement.setAttribute("username", player.getName());
                reply.appendChild(playerElement);
            }
        }
        return reply;
    }
}
