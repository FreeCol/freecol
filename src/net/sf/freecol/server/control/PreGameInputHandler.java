/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Logger;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.NationOptions.Advantages;
import net.sf.freecol.common.model.NationOptions.NationState;
import net.sf.freecol.common.model.NationType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.NoRouteToServerException;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;

/**
 * Handles the network messages that arrives before the game starts.
 * 
 * @see PreGameController
 */
public final class PreGameInputHandler extends InputHandler {
    private static Logger logger = Logger.getLogger(PreGameInputHandler.class.getName());





    /**
     * The constructor to use.
     * 
     * @param freeColServer The main server object.
     */
    public PreGameInputHandler(FreeColServer freeColServer) {
        super(freeColServer);
        // TODO: move and simplify methods later, for now just delegate
        register("updateGameOptions", new NetworkRequestHandler() {
            public Element handle(Connection connection, Element element) {
                return updateGameOptions(connection, element);
            }
        });
        register("updateMapGeneratorOptions", new NetworkRequestHandler() {
            public Element handle(Connection connection, Element element) {
                return updateMapGeneratorOptions(connection, element);
            }
        });
        register("ready", new NetworkRequestHandler() {
            public Element handle(Connection connection, Element element) {
                return ready(connection, element);
            }
        });
        register("setNation", new NetworkRequestHandler() {
            public Element handle(Connection connection, Element element) {
                return nation(connection, element);
            }
        });
        register("setNationType", new NetworkRequestHandler() {
            public Element handle(Connection connection, Element element) {
                return nationType(connection, element);
            }
        });
        register("setAvailable", new NetworkRequestHandler() {
            public Element handle(Connection connection, Element element) {
                return available(connection, element);
            }
        });
        register("requestLaunch", new NetworkRequestHandler() {
            public Element handle(Connection connection, Element element) {
                return requestLaunch(connection, element);
            }
        });
    }

    /**
     * Handles a &quot;updateGameOptions&quot;-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param element The element containing the request.
     * @return The reply.
     */
    private Element updateGameOptions(Connection connection, Element element) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        if (!player.isAdmin()) {
            throw new IllegalStateException();
        }
        getFreeColServer().getGame().getGameOptions().readFromXMLElement((Element) element.getChildNodes().item(0));
        Element updateGameOptionsElement = Message.createNewRootElement("updateGameOptions");
        updateGameOptionsElement.appendChild(getFreeColServer().getGame().getGameOptions().toXMLElement(
                updateGameOptionsElement.getOwnerDocument()));
        getFreeColServer().getServer().sendToAll(updateGameOptionsElement, connection);
        return null;
    }

    /**
     * Handles a "updateMapGeneratorOptions"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param element The element containing the request.
     * @return The reply.
     */
    private Element updateMapGeneratorOptions(Connection connection, Element element) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        if (!player.isAdmin()) {
            throw new IllegalStateException();
        }
        getFreeColServer().getMapGenerator().getMapGeneratorOptions().readFromXMLElement(
                (Element) element.getChildNodes().item(0));
        Element umge = Message.createNewRootElement("updateMapGeneratorOptions");
        umge.appendChild(getFreeColServer().getMapGenerator().getMapGeneratorOptions().toXMLElement(
                umge.getOwnerDocument()));
        getFreeColServer().getServer().sendToAll(umge, connection);
        return null;
    }

    /**
     * Handles a "ready"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param element The element containing the request.
     */
    private Element ready(Connection connection, Element element) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        if (player != null) {
            boolean ready = (new Boolean(element.getAttribute("value"))).booleanValue();
            player.setReady(ready);
            Element playerReady = Message.createNewRootElement("playerReady");
            playerReady.setAttribute("player", player.getId());
            playerReady.setAttribute("value", Boolean.toString(ready));
            getFreeColServer().getServer().sendToAll(playerReady, player.getConnection());
        } else {
            logger.warning("Ready from unknown connection.");
        }
        return null;
    }

    /**
     * Handles a "setNation"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param element The element containing the request.
     */
    private Element nation(Connection connection, Element element) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        if (player != null) {
            Nation nation = FreeCol.getSpecification().getNation(element.getAttribute("value"));
            if (getFreeColServer().getGame().getNationOptions().getNations().get(nation) ==
                NationState.AVAILABLE) {
                player.setNation(nation);
                Element updateNation = Message.createNewRootElement("updateNation");
                updateNation.setAttribute("player", player.getId());
                updateNation.setAttribute("value", nation.getId());
                getFreeColServer().getServer().sendToAll(updateNation, player.getConnection());
            } else {
                logger.warning("Selected non-selectable nation.");
            }
        } else {
            logger.warning("Nation from unknown connection.");
        }
        return null;
    }

    /**
     * Handles a "setNationType"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param element The element containing the request.
     */
    private Element nationType(Connection connection, Element element) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        if (player != null) {
            NationType nationType = FreeCol.getSpecification().getNationType(element.getAttribute("value"));
            NationType fixedNationType = FreeCol.getSpecification().getNation(player.getNationID()).getType();
            Advantages advantages = getFreeColServer().getGame().getNationOptions().getNationalAdvantages();
            if (advantages == Advantages.SELECTABLE
                || (advantages == Advantages.FIXED && nationType.equals(fixedNationType))) {
                player.setNationType(nationType);
                Element updateNationType = Message.createNewRootElement("updateNationType");
                updateNationType.setAttribute("player", player.getId());
                updateNationType.setAttribute("value", nationType.getId());
                getFreeColServer().getServer().sendToAll(updateNationType, player.getConnection());
            } else {
                logger.warning("NationType is not selectable");
            }
        } else {
            logger.warning("NationType from unknown connection.");
        }
        return null;
    }

    /**
     * Handles a "setAvailable"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param element The element containing the request.
     */
    private Element available(Connection connection, Element element) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        if (player != null) {
            Nation nation = Specification.getSpecification().getNation(element.getAttribute("nation"));
            NationState state = Enum.valueOf(NationState.class, element.getAttribute("state"));
            getFreeColServer().getGame().getNationOptions().setNationState(nation, state);
            getFreeColServer().getServer().sendToAll(element, player.getConnection());
        } else {
            logger.warning("Available from unknown connection.");
        }
        return null;
    }

    /**
     * Handles a "requestLaunch"-message from a client.
     * 
     * @param connection The connection the message came from.
     * @param element The element containing the request.
     */
    private Element requestLaunch(Connection connection, Element element) {
        FreeColServer freeColServer = getFreeColServer();
        ServerPlayer launchingPlayer = freeColServer.getPlayer(connection);
        // Check if launching player is an admin.
        if (!launchingPlayer.isAdmin()) {
            Element reply = Message.createNewRootElement("error");
            reply.setAttribute("message", "Sorry, only the server admin can launch the game.");
            reply.setAttribute("messageID", "server.onlyAdminCanLaunch");
            return reply;
        }
        // Check that no two players have the same color or nation
        Iterator<Player> playerIterator = freeColServer.getGame().getPlayerIterator();
        LinkedList<Nation> nations = new LinkedList<Nation>();
        while (playerIterator.hasNext()) {
            ServerPlayer player = (ServerPlayer) playerIterator.next();
            final Nation nation = FreeCol.getSpecification().getNation(player.getNationID());
            // Check the nation.
            for (int i = 0; i < nations.size(); i++) {
                if (nations.get(i) == nation) {
                    Element reply = Message.createNewRootElement("error");
                    reply
                            .setAttribute("message",
                                    "All players need to pick a unique nation before the game can start.");
                    reply.setAttribute("messageID", "server.invalidPlayerNations");
                    return reply;
                }
            }
            nations.add(nation);
        }
        // Check if all players are ready.
        if (!freeColServer.getGame().isAllPlayersReadyToLaunch()) {
            Element reply = Message.createNewRootElement("error");
            reply.setAttribute("message", "Not all players are ready to begin the game!");
            reply.setAttribute("messageID", "server.notAllReady");
            return reply;
        }
        try {
            ((PreGameController) freeColServer.getController()).startGame();
        } catch (FreeColException e) {
            // send an error message to the client(s)
            Element reply = Message.createNewRootElement("error");
            reply.setAttribute("message", "An error occurred while starting the game!");
            reply.setAttribute("messageID", "server.errorStartingGame");
            return reply;
        }
        return null;
    }

    /**
     * Handles a &quot;logout&quot;-message.
     * 
     * @param connection The <code>Connection</code> the message was received
     *            on.
     * @param logoutElement The element (root element in a DOM-parsed XML tree)
     *            that holds all the information.
     * @return The reply.
     */
    protected Element logout(Connection connection, Element logoutElement) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        logger.info("Logout by: " + connection + ((player != null) ? " (" + player.getName() + ") " : ""));
        Element logoutMessage = Message.createNewRootElement("logout");
        logoutMessage.setAttribute("reason", "User has logged out.");
        logoutMessage.setAttribute("player", player.getId());
        player.setConnected(false);
        getFreeColServer().getGame().removePlayer(player);
        getFreeColServer().getServer().sendToAll(logoutMessage, connection);
        try {
            getFreeColServer().updateMetaServer();
        } catch (NoRouteToServerException e) {}
        
        return null;
    }
}
