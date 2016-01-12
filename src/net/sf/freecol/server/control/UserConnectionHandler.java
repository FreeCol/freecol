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

package net.sf.freecol.server.control;

import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.DOMMessage;
import net.sf.freecol.common.networking.ErrorMessage;
import net.sf.freecol.common.networking.GameStateMessage;
import net.sf.freecol.common.networking.LoginMessage;
import net.sf.freecol.common.networking.MessageHandler;
import net.sf.freecol.common.networking.VacantPlayersMessage;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.networking.Server;

import org.w3c.dom.Element;


/**
 * Handles a new client connection.  {@link PreGameInputHandler} is
 * set as the message handler when the client has successfully logged
 * on.
 */
public final class UserConnectionHandler extends InputHandler {

    private static final Logger logger = Logger.getLogger(UserConnectionHandler.class.getName());


    /**
     * Build a new user connection handler.
     *
     * @param freeColServer The main control object.
     */
    public UserConnectionHandler(final FreeColServer freeColServer) {
        super(freeColServer);

        final Game game = freeColServer.getGame();
        register(GameStateMessage.GAME_STATE_TAG,
            (Connection c, Element e) ->
            new GameStateMessage(game, e).handle(freeColServer, c));
        register(LoginMessage.LOGIN_TAG,
            (Connection c, Element e) ->
            login(c, e));
        register(VacantPlayersMessage.VACANT_PLAYERS_TAG,
            (Connection c, Element e) ->
            new VacantPlayersMessage(game, e).handle(freeColServer, c));
    }


    /**
     * {@inheritDoc}
     */
    protected Element logout(
        @SuppressWarnings("unused") Connection connection,
        @SuppressWarnings("unused") Element element) {
        return null; // Logout is inoperative at this point
    }


    // Individual message handlers

    /**
     * Handle a "login"-request.
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
            return new ErrorMessage("server.missingUserName", null)
                .toXMLElement();
        } else if (version == null || version.isEmpty()) {
            return new ErrorMessage("server.missingVersion", null)
                .toXMLElement();
        } else if (!version.equals(FreeCol.getVersion())) {
            return new ErrorMessage("server.wrongFreeColVersion",
                version + " != " + FreeCol.getVersion()).toXMLElement();
        }

        final FreeColServer freeColServer = getFreeColServer();
        Game game;
        ServerPlayer player;
        boolean isCurrentPlayer = false;
        MessageHandler mh;
        boolean starting = freeColServer.getGameState()
            == FreeColServer.GameState.STARTING_GAME;
        if (starting) {
            // Wait until the game has been created.
            // FIXME: is this still needed?
            int timeOut = 20000;
            while ((game = freeColServer.getGame()) == null) {
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
            } else if (game.playerNameInUse(userName)) {
                return new ErrorMessage("server.userNameInUse",
                    userName + " is already in use.").toXMLElement();
            }

            // Create and add the new player:
            boolean admin = game.getLivePlayers(null).isEmpty();
            player = new ServerPlayer(game, admin, game.getVacantNation(),
                                      connection.getSocket(), connection);
            player.setName(userName);
            game.addPlayer(player);

            // Send message to all players except to the new player.
            // FIXME: check visibility.
            DOMMessage adp = new DOMMessage("addPlayer");
            adp.add(player);
            freeColServer.sendToAll(adp, connection);

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
                return new ErrorMessage("server.userNameNotPresent",
                    sb.toString()).toXMLElement();
            } else if (player.isConnected() && !player.isAI()) {
                return new ErrorMessage("server.userNameInUse",
                    userName + " is already in use.").toXMLElement();
            }
            player.setConnection(connection);
            player.setConnected(true);

            if (player.isAI()) {
                player.setAI(false);
                freeColServer.sendToAll(new DOMMessage("setAI",
                        "player", player.getId(),
                        "ai", Boolean.toString(false)),
                    null);
            }

            // If this player is the first to reconnect, it is the
            // current player.
            isCurrentPlayer = game.getCurrentPlayer() == null;
            if (isCurrentPlayer) game.setCurrentPlayer(player);

            // Go straight into the game.
            mh = freeColServer.getInGameInputHandler();
        }

        connection.setMessageHandler(mh);
        freeColServer.getServer().addConnection(connection);
        freeColServer.updateMetaServer();
        return new LoginMessage(userName, version, player.isAdmin(), !starting,
                                freeColServer.getSinglePlayer(),
                                isCurrentPlayer, game).toXMLElement();
    }
}
