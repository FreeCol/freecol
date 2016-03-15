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
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.CurrentPlayerNetworkRequestHandler;
import net.sf.freecol.common.networking.DOMMessage;
import net.sf.freecol.common.networking.ErrorMessage;
import net.sf.freecol.common.networking.UpdateGameOptionsMessage;
import net.sf.freecol.common.networking.UpdateMapGeneratorOptionsMessage;
import net.sf.freecol.common.option.OptionGroup;
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
        // FIXME: move and simplify methods later, for now just delegate
        register("ready",
            (Connection connection, Element element) ->
            ready(connection, element));
        register("requestLaunch",
            (Connection connection, Element element) -> {
                Element reply = requestLaunch(connection, element);
                if (reply != null) {
                    launching = false;
                }
                return reply;
            });
        register("setColor",
            (Connection connection, Element element) ->
            setColor(connection, element));
        register("setNation",
            (Connection connection, Element element) ->
            setNation(connection, element));
        register("setNationType",
            (Connection connection, Element element) ->
            setNationType(connection, element));
        register("setAvailable",
            (Connection connection, Element element) ->
            setAvailable(connection, element));
        register(UpdateGameOptionsMessage.TAG,
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new UpdateGameOptionsMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
        register(UpdateMapGeneratorOptionsMessage.TAG,
                 new CurrentPlayerNetworkRequestHandler(freeColServer) {
            @Override
            public Element handle(Player player, Connection connection,
                                  Element element) {
                return new UpdateMapGeneratorOptionsMessage(getGame(), element)
                    .handle(freeColServer, player, connection);
            }});
    }
            
    /**
     * Handles a "logout"-message.
     * 
     * @param connection The <code>Connection</code> the message came from.
     * @param element The <code>Element</code> containing the request.
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
        freeColServer.sendToAll(new DOMMessage("logout",
                "reason", "User has logged out.",
                "player", player.getId()),
            connection);
        freeColServer.updateMetaServer();
        return null;
    }

    /**
     * Handles a "ready"-message from a client.
     * 
     * @param connection The <code>Connection</code> the message came from.
     * @param element The <code>Element</code> containing the request.
     * @return Null.
     */
    private Element ready(Connection connection, Element element) {
        final FreeColServer freeColServer = getFreeColServer();
        final ServerPlayer player = freeColServer.getPlayer(connection);

        if (player != null) {
            boolean ready = Boolean.parseBoolean(element.getAttribute("value"));
            player.setReady(ready);
            freeColServer.sendToAll(new DOMMessage("playerReady",
                    "player", player.getId(),
                    "value", Boolean.toString(ready)),
                player.getConnection());
        } else {
            logger.warning("Ready from unknown connection.");
        }
        return null;
    }

    /**
     * Handles a "requestLaunch"-message from a client.
     * 
     * @param connection The <code>Connection</code> the message came from.
     * @param element The <code>Element</code> containing the request.
     * @return Null, or an error message on failure.
     */
    private Element requestLaunch(Connection connection,
                                  @SuppressWarnings("unused") Element element) {
        final FreeColServer freeColServer = getFreeColServer();
        final ServerPlayer player = freeColServer.getPlayer(connection);
        final Specification spec = getGame().getSpecification();

        // Check if launching player is an admin.
        if (!player.isAdmin()) {
            return new ErrorMessage("server.onlyAdminCanLaunch",
                "Only the server admin can launch the game.").toXMLElement();
        }
        if (launching) return null;
        launching = true;

        // Check that no two players have the same nation
        final Game game = getGame();
        List<Nation> nations = new ArrayList<>();
        for (Player p : game.getLivePlayers(null)) {
            final Nation nation = spec.getNation(p.getNationId());
            if (nations.contains(nation)) {
                return new ErrorMessage("server.invalidPlayerNations",
                    "All players need to pick a unique nation before the game can start.")
                    .toXMLElement();
            }
            nations.add(nation);
        }

        // Check if all players are ready.
        if (!game.allPlayersReadyToLaunch()) {
            return new ErrorMessage("server.notAllReady",
                "Not all players are ready to begin the game!").toXMLElement();
        }
        try {
            ((PreGameController)freeColServer.getController()).startGame();
        } catch (FreeColException e) {
            return new ErrorMessage("server.errorStartingGame",
                                    e.getMessage()).toXMLElement();
        }

        return null;
    }

    /**
     * Handles a "setAvailable"-message from a client.
     * 
     * @param connection The <code>Connection</code> the message came from.
     * @param element The <code>Element</code> containing the request.
     * @return Null, or an error message on failure.
     */
    private Element setAvailable(Connection connection, Element element) {
        final FreeColServer freeColServer = getFreeColServer();
        final ServerPlayer player = freeColServer.getPlayer(connection);
        final Specification spec = getGame().getSpecification();

        if (player != null) {
            Nation nation = spec.getNation(element.getAttribute("nation"));
            NationState state = Enum.valueOf(NationState.class,
                                             element.getAttribute("state"));
            getGame().getNationOptions().setNationState(nation, state);
            freeColServer.sendToAll(new DOMMessage("setAvailable",
                    "nation", nation.getId(),
                    "state", state.toString()),
                player.getConnection());
        } else {
            logger.warning("Available from unknown connection.");
        }
        return null;
    }

    /**
     * Handles a "setColor"-message from a client.
     * 
     * @param connection The <code>Connection</code> the message came from.
     * @param element The <code>Element</code> containing the request.
     * @return Null, or an error message on failure.
     */
    private Element setColor(Connection connection, Element element) {
        final FreeColServer freeColServer = getFreeColServer();
        final ServerPlayer player = freeColServer.getPlayer(connection);
        final Specification spec = getGame().getSpecification();

        if (player != null) {
            Nation nation = spec.getNation(element.getAttribute("nation"));
            String str = element.getAttribute("color");
            Color color;
            try {
                int rgb = Integer.decode(str);
                color = new Color(rgb);
            } catch (NumberFormatException nfe) {
                return new ErrorMessage("server.badColor",
                    "Invalid color: " + str).toXMLElement();
            }
            nation.setColor(color);
            freeColServer.sendToAll(new DOMMessage("updateColor",
                    "nation", nation.getId(),
                    "color", Integer.toString(color.getRGB())),
                player.getConnection());
        } else {
            logger.warning("setColor from unknown connection.");
        }
        return null;
    }

    /**
     * Handles a "setNation"-message from a client.
     * 
     * @param connection The <code>Connection</code> the message came from.
     * @param element The <code>Element</code> containing the request.
     * @return Null.
     */
    private Element setNation(Connection connection, Element element) {
        final FreeColServer freeColServer = getFreeColServer();
        final ServerPlayer player = freeColServer.getPlayer(connection);
        final Specification spec = getGame().getSpecification();

        if (player != null) {
            Nation nation = spec.getNation(element.getAttribute("value"));
            if (getGame().getNationOptions().getNations().get(nation)
                == NationState.AVAILABLE) {
                player.setNation(nation);
                freeColServer.sendToAll(new DOMMessage("updateNation",
                        "player", player.getId(),
                        "value", nation.getId()),
                    player.getConnection());
            } else {
                return new ErrorMessage("server.badNation",
                    "Selected non-selectable nation: " + nation)
                    .toXMLElement();
            }
        } else {
            logger.warning("setNation from unknown connection.");
        }
        return null;
    }

    /**
     * Handles a "setNationType"-message from a client.
     * 
     * @param connection The <code>Connection</code> the message came from.
     * @param element The <code>Element</code> containing the request.
     * @return Null, or an error message on failure.
     */
    private Element setNationType(Connection connection, Element element) {
        final FreeColServer freeColServer = getFreeColServer();
        final ServerPlayer player = freeColServer.getPlayer(connection);
        final Specification spec = getGame().getSpecification();

        if (player != null) {
            NationType nationType = spec.getNationType(element.getAttribute("value"));
            NationType fixedNationType = spec.getNation(player.getNationId())
                .getType();
            Advantages advantages = getGame().getNationOptions()
                .getNationalAdvantages();
            boolean ok;
            switch (advantages) {
            case SELECTABLE:
                ok = true;
                break;
            case FIXED:
                ok = nationType.equals(fixedNationType);
                break;
            case NONE:
                ok = nationType == spec.getDefaultNationType();
                break;
            default:
                ok = false;
                break;
            }
            if (ok) {
                player.changeNationType(nationType);
                freeColServer.sendToAll(new DOMMessage("updateNationType",
                        "player", player.getId(),
                        "value", nationType.getId()),
                    player.getConnection());
            } else {
                return new ErrorMessage("server.badNationType",
                    "Selected non-selectable nation type: " + nationType)
                    .toXMLElement();
            }
        } else {
            logger.warning("setNationType from unknown connection.");
        }
        return null;
    }
}
