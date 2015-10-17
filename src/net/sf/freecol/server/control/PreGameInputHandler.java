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

import java.awt.Color;
import java.util.ArrayList;
import java.util.logging.Logger;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.NationOptions.Advantages;
import net.sf.freecol.common.model.NationOptions.NationState;
import net.sf.freecol.common.model.NationType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.DOMMessage;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * Handles the network messages that arrives before the game starts.
 * 
 * @see PreGameController
 */
public final class PreGameInputHandler extends InputHandler {

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
        register("updateGameOptions",
            (Connection connection, Element element) ->
            updateGameOptions(connection, element));
        register("updateMapGeneratorOptions",
            (Connection connection, Element element) ->
            updateMapGeneratorOptions(connection, element));
    }

    /**
     * Handles a "logout"-message.
     * 
     * @param connection The <code>Connection</code> the message came from.
     * @param element The <code>Element</code> containing the request.
     * @return A logout reply message.
     */
    @Override
    protected Element logout(Connection connection, Element element) {
        logger.info("Logout from: " + connection);
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        player.setConnected(false);
        getGame().removePlayer(player);
        getFreeColServer().getServer()
            .sendToAll(DOMMessage.createMessage("logout",
                    "reason", "User has logged out.",
                    "player", player.getId()),
                connection);
        getFreeColServer().updateMetaServer();
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
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        if (player != null) {
            boolean ready = Boolean.parseBoolean(element.getAttribute("value"));
            player.setReady(ready);
            getFreeColServer().getServer()
                .sendToAll(DOMMessage.createMessage("playerReady",
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
        FreeColServer freeColServer = getFreeColServer();

        // Check if launching player is an admin.
        ServerPlayer launchingPlayer = freeColServer.getPlayer(connection);
        if (!launchingPlayer.isAdmin()) {
            return DOMMessage.createError("server.onlyAdminCanLaunch",
                "Only the server admin can launch the game.");
        }

        if (launching) return null;
        launching = true;

        // Check that no two players have the same nation
        ArrayList<Nation> nations = new ArrayList<>();
        for (Player player : getGame().getLivePlayers(null)) {
            final Nation nation = getGame().getSpecification()
                .getNation(player.getNationId());
            if (nations.contains(nation)) {
                return DOMMessage.createError("server.invalidPlayerNations",
                    "All players need to pick a unique nation before the game can start.");
            }
            nations.add(nation);
        }

        // Check if all players are ready.
        if (!getGame().allPlayersReadyToLaunch()) {
            return DOMMessage.createError("server.notAllReady",
                "Not all players are ready to begin the game!");
        }
        try {
            ((PreGameController)freeColServer.getController()).startGame();
        } catch (FreeColException e) {
            return DOMMessage.createError("server.errorStartingGame",
                                          e.getMessage());
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
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        if (player != null) {
            Nation nation = getGame().getSpecification()
                .getNation(element.getAttribute("nation"));
            NationState state = Enum.valueOf(NationState.class,
                                             element.getAttribute("state"));
            getGame().getNationOptions().setNationState(nation, state);
            getFreeColServer().getServer().sendToAll(element, 
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
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        if (player != null) {
            Nation nation = getGame().getSpecification()
                .getNation(element.getAttribute("nation"));
            String str = element.getAttribute("color");
            Color color;
            try {
                int rgb = Integer.decode(str);
                color = new Color(rgb);
            } catch (NumberFormatException nfe) {
                return DOMMessage.createError("server.badColor",
                                              "Invalid color: " + str);
            }
            nation.setColor(color);
            getFreeColServer().getServer()
                .sendToAll(DOMMessage.createMessage("updateColor",
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
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        if (player != null) {
            Nation nation = getGame().getSpecification()
                .getNation(element.getAttribute("value"));
            if (getGame().getNationOptions().getNations().get(nation)
                == NationState.AVAILABLE) {
                player.setNation(nation);
                getFreeColServer().getServer()
                    .sendToAll(DOMMessage.createMessage("updateNation",
                            "player", player.getId(),
                            "value", nation.getId()),
                        player.getConnection());
            } else {
                return DOMMessage.createError("server.badNation",
                    "Selected non-selectable nation: " + nation);
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
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        if (player != null) {
            final Specification spec = getGame().getSpecification();
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
                getFreeColServer().getServer()
                    .sendToAll(DOMMessage.createMessage("updateNationType",
                            "player", player.getId(),
                            "value", nationType.getId()),
                        player.getConnection());
            } else {
                return DOMMessage.createError("server.badNationType",
                    "Selected non-selectable nation type: " + nationType);
            }
        } else {
            logger.warning("setNationType from unknown connection.");
        }
        return null;
    }

    /**
     * Handles a "updateGameOptions"-message from a client.
     * 
     * @param connection The <code>Connection</code> the message came from.
     * @param element The <code>Element</code> containing the request.
     * @return Null.
     */
    private Element updateGameOptions(Connection connection, Element element) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        if (!player.isAdmin()) {
            throw new IllegalStateException("Not an admin");
        }
        final Specification spec = getGame().getSpecification();
        OptionGroup gameOptions = spec.getGameOptions();
        Element child = (Element)element.getChildNodes().item(0);
        gameOptions.readFromXMLElement(child);
        spec.clean("update game options (server)");

        Element up = DOMMessage.createMessage("updateGameOptions");
        up.appendChild(gameOptions.toXMLElement(up.getOwnerDocument()));
        getFreeColServer().getServer().sendToAll(up, connection);
        return null;
    }

    /**
     * Handles a "updateMapGeneratorOptions"-message from a client.
     * 
     * @param connection The <code>Connection</code> the message came from.
     * @param element The <code>Element</code> containing the request.
     * @return Null.
     */
    private Element updateMapGeneratorOptions(Connection connection,
                                              Element element) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        if (!player.isAdmin()) {
            throw new IllegalStateException("Not an admin");
        }
        final Specification spec = getGame().getSpecification();
        OptionGroup mgo = spec.getMapGeneratorOptions();
        Element child = (Element)element.getChildNodes().item(0);
        mgo.readFromXMLElement(child);
        Element umge = DOMMessage.createMessage("updateMapGeneratorOptions");
        umge.appendChild(mgo.toXMLElement(umge.getOwnerDocument()));
        getFreeColServer().getServer().sendToAll(umge, connection);
        return null;
    }
}
