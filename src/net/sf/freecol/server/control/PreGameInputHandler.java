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

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.NationOptions.Advantages;
import net.sf.freecol.common.model.NationOptions.NationState;
import net.sf.freecol.common.model.NationType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.networking.AttributeMessage;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.CurrentPlayerNetworkRequestHandler;
import net.sf.freecol.common.networking.ErrorMessage;
import net.sf.freecol.common.networking.LogoutMessage;
import net.sf.freecol.common.networking.ReadyMessage;
import net.sf.freecol.common.networking.SetAvailableMessage;
import net.sf.freecol.common.networking.SetColorMessage;
import net.sf.freecol.common.networking.SetNationMessage;
import net.sf.freecol.common.networking.SetNationTypeMessage;
import net.sf.freecol.common.networking.TrivialMessage;
import net.sf.freecol.common.networking.UpdateGameOptionsMessage;
import net.sf.freecol.common.networking.UpdateMapGeneratorOptionsMessage;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * Handles the network messages that arrives before the game starts.
 * 
 * @see PreGameController
 */
public final class PreGameInputHandler extends ServerInputHandler {

    private static final Logger logger = Logger.getLogger(PreGameInputHandler.class.getName());

    /** Is the game launching yet. */
    private boolean launching = false;


    /**
     * The constructor to use.
     * 
     * @param freeColServer The main server object.
     */
    public PreGameInputHandler(FreeColServer freeColServer) {
        super(freeColServer);

        final Game game = getGame();

        register(ReadyMessage.TAG,
            (Connection connection, Element element) ->
                new ReadyMessage(game, element)
                    .handle(freeColServer, connection));
        register(TrivialMessage.REQUEST_LAUNCH_TAG,
            (Connection connection, Element element) -> {
                Element reply = requestLaunch(connection, element);
                if (reply != null) launching = false;
                return reply;
            });
        register(SetAvailableMessage.TAG,
            (Connection connection, Element element) ->
                new SetAvailableMessage(game, element)
                    .handle(freeColServer, connection));
        register(SetColorMessage.TAG,
            (Connection connection, Element element) ->
                new SetColorMessage(game, element)
                    .handle(freeColServer, connection));
        register(SetNationMessage.TAG,
            (Connection connection, Element element) ->
                new SetNationMessage(game, element)
                    .handle(freeColServer, connection));
        register(SetNationTypeMessage.TAG,
            (Connection connection, Element element) ->
                new SetNationTypeMessage(game, element)
                    .handle(freeColServer, connection));
        register(UpdateGameOptionsMessage.TAG,
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new UpdateGameOptionsMessage(game, element)
                    .handle(freeColServer, player, connection);
            }});
        register(UpdateMapGeneratorOptionsMessage.TAG,
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new UpdateMapGeneratorOptionsMessage(game, element)
                    .handle(freeColServer, player, connection);
            }});
    }
            
    /**
     * Handles a "logout"-message.
     * 
     * @param connection The {@code Connection} the message came from.
     * @param element The {@code Element} containing the request.
     * @return A logout reply message.
     */
    @Override
    protected Element logout(Connection connection,
                             @SuppressWarnings("unused") Element element) {
        logger.info("Logout from: " + connection);
        final FreeColServer freeColServer = getFreeColServer();
        final ServerPlayer player = freeColServer.getPlayer(connection);

        player.setConnected(false);
        getGame().removePlayer(player);
        LogoutMessage message = new LogoutMessage(player, "User has logged out");
        freeColServer.sendToAll(message, connection);
        freeColServer.updateMetaServer(false);
        return null;
    }

    /**
     * Handles a "requestLaunch"-message from a client.
     * 
     * @param connection The {@code Connection} the message came from.
     * @param element The {@code Element} containing the request.
     * @return Null, or an error message on failure.
     */
    private Element requestLaunch(Connection connection,
                                  @SuppressWarnings("unused") Element element) {
        final FreeColServer freeColServer = getFreeColServer();
        final ServerPlayer player = freeColServer.getPlayer(connection);
        final Specification spec = getGame().getSpecification();

        // Check if launching player is an admin.
        if (!player.isAdmin()) {
            return new ErrorMessage(StringTemplate
                .template("server.onlyAdminCanLaunch"))
                .toXMLElement();
        }
        if (launching) return null;
        launching = true;

        // Check that no two players have the same nation
        final Game game = getGame();
        List<Nation> nations = new ArrayList<>();
        for (Player p : game.getLivePlayerList()) {
            final Nation nation = spec.getNation(p.getNationId());
            if (nations.contains(nation)) {
                return new ErrorMessage(StringTemplate
                    .template("server.invalidPlayerNations"))
                    .toXMLElement();
            }
            nations.add(nation);
        }

        // Check if all players are ready.
        if (!game.allPlayersReadyToLaunch()) {
            return new ErrorMessage(StringTemplate
                .template("server.notAllReady"))
                .toXMLElement();
        }
        try {
            ((PreGameController)freeColServer.getController()).startGame();
        } catch (FreeColException e) {
            return new ErrorMessage(e).toXMLElement();
        }

        return null;
    }
}
