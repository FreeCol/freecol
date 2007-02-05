
package net.sf.freecol.server.control;

import java.awt.Color;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Logger;

import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;



/**
* Handles the network messages that arrives before the game starts.
* @see PreGameController
*/
public final class PreGameInputHandler extends InputHandler {
    private static Logger logger = Logger.getLogger(PreGameInputHandler.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    

    /**
    * The constructor to use.
    * @param freeColServer The main server object.
    */
    public PreGameInputHandler(FreeColServer freeColServer) {
        super(freeColServer);
    }





    /**
    * Handles a network message.
    *
    * @param connection The <code>Connection</code> the message came from.
    * @param element The message to be processed.
    * @return The reply.
    */
    public synchronized Element handle(Connection connection, Element element) {
        Element reply = null;

        String type = element.getTagName();

        if (element != null) {
            if (type.equals("updateGameOptions")) {
                reply = updateGameOptions(connection, element);
            } else if (type.equals("updateMapGeneratorOptions")) {
                reply = updateMapGeneratorOptions(connection, element);
            } else if (type.equals("ready")) {
                reply = ready(connection, element);
            } else if (type.equals("setNation")) {
                reply = nation(connection, element);
            } else if (type.equals("setColor")) {
                reply = color(connection, element);
            } else if (type.equals("requestLaunch")) {
                reply = requestLaunch(connection, element);
            } else if (type.equals("logout")) {
                reply = logout(connection, element);
            } else if (type.equals("chat")) {
                reply = chat(connection, element);
            } else if (type.equals("disconnect")) {
                reply = disconnect(connection, element);
            } else if(type.equals("getRandomNumbers")) {
                reply = getRandomNumbers(connection, element);
            }
        }

        return reply;
    }



    /**
    * Handles a "updateGameOptions"-message from a client.
    *
    * @param connection The connection the message came from.
    * @param element The element containing the request.
    * @return The reply.
    */
    public Element updateGameOptions(Connection connection, Element element) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);

        if (!player.isAdmin()) {
            throw new IllegalStateException();
        }

        getFreeColServer().getGame().getGameOptions().readFromXMLElement((Element) element.getChildNodes().item(0));

        Element updateGameOptionsElement = Message.createNewRootElement("updateGameOptions");
        updateGameOptionsElement.appendChild(getFreeColServer().getGame().getGameOptions().toXMLElement(updateGameOptionsElement.getOwnerDocument()));
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
    public Element updateMapGeneratorOptions(Connection connection, Element element) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);

        if (!player.isAdmin()) {
            throw new IllegalStateException();
        }

        getFreeColServer().getMapGenerator().getMapGeneratorOptions().readFromXMLElement((Element) element.getChildNodes().item(0));

        Element umge = Message.createNewRootElement("updateMapGeneratorOptions");
        umge.appendChild(getFreeColServer().getMapGenerator().getMapGeneratorOptions().toXMLElement(umge.getOwnerDocument()));
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
            playerReady.setAttribute("player", player.getID());
            playerReady.setAttribute("value", Boolean.toString(ready));

            getFreeColServer().getServer().sendToAll(playerReady, player.getConnection());
        } else {
            logger.warning("Ready from unknown connection.");
        }

        return null;
    }

    /**
    * Handles a "nation"-message from a client.
    *
    * @param connection The connection the message came from.
    * @param element The element containing the request.
    */
    private Element nation(Connection connection, Element element) {

        ServerPlayer player = getFreeColServer().getPlayer(connection);

        if (player != null) {
            int nation = Integer.parseInt(element.getAttribute("value"));
            player.setNation(nation);

            Element updateNation = Message.createNewRootElement("updateNation");
            updateNation.setAttribute("player", player.getID());
            updateNation.setAttribute("value", Integer.toString(nation));

            getFreeColServer().getServer().sendToAll(updateNation, player.getConnection());
        } else {
            logger.warning("Nation from unknown connection.");
        }

        return null;
    }

    /**
    * Handles a "color"-message from a client.
    *
    * @param connection The connection the message came from.
    * @param element The element containing the request.
    */
    private Element color(Connection connection, Element element) {

        ServerPlayer player = getFreeColServer().getPlayer(connection);

        if (player != null) {
            String color = element.getAttribute("value");
            player.setColor(color);

            Element updateColor = Message.createNewRootElement("updateColor");
            updateColor.setAttribute("player", player.getID());
            updateColor.setAttribute("value", color);

            getFreeColServer().getServer().sendToAll(updateColor, player.getConnection());
        } else {
            logger.warning("Color from unknown connection.");
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
        Iterator playerIterator = freeColServer.getGame().getPlayerIterator();

        LinkedList nations = new LinkedList();
        LinkedList colors = new LinkedList();

        while (playerIterator.hasNext()) {
            ServerPlayer player = (ServerPlayer) playerIterator.next();

            final int nation = player.getNation();
            final Color color = player.getColor();

            // Check the nation.
            for (int i = 0; i < nations.size(); i++) {
                if (((Integer)nations.get(i)).intValue() == nation) {
                    Element reply = Message.createNewRootElement("error");
                    reply.setAttribute("message",
                        "All players need to pick a unique nation before the game can start.");
                    reply.setAttribute("messageID", "server.invalidPlayerNations");

                    return reply;
                }
            }
            nations.add(new Integer(nation));

            // Check the color.
            for (int i = 0; i < colors.size(); i++) {
                if (((Color)colors.get(i)).equals(color)) {
                    Element reply = Message.createNewRootElement("error");
                    reply.setAttribute("message",
                        "All players need to pick a unique color before the game can start.");
                    reply.setAttribute("messageID", "server.invalidPlayerColors");

                    return reply;
                }
            }
            colors.add(color);
        }

        // Check if all players are ready.
        if (!freeColServer.getGame().isAllPlayersReadyToLaunch()) {
            Element reply = Message.createNewRootElement("error");
            reply.setAttribute("message", "Not all players are ready to begin the game!");
            reply.setAttribute("messageID", "server.notAllReady");

            return reply;
        }

        ((PreGameController) freeColServer.getController()).startGame();

        return null;
    }


    /**
    * Handles a "chat"-message from a client.
    *
    * @param connection The connection the message came from.
    * @param element The element containing the request.
    */
    private Element chat(Connection connection, Element element) {
        // TODO: Add support for private chat.

        getFreeColServer().getServer().sendToAll(element, connection);

        return null;
    }
    
    
    /**
    * Handles a "logout"-message.
    *
    * @param connection The <code>Connection</code> the message was received on.
    * @param logoutElement The element (root element in a DOM-parsed XML tree) that
    *                holds all the information.
    * @return The reply.
    */
    protected Element logout(Connection connection, Element logoutElement) {
        ServerPlayer player = getFreeColServer().getPlayer(connection);

        logger.info("Logout by: " + connection + ((player != null) ? " (" + player.getName() + ") " : ""));        

        Element logoutMessage = Message.createNewRootElement("logout");
        logoutMessage.setAttribute("reason", "User has logged out.");
        logoutMessage.setAttribute("player", player.getID());

        player.setConnected(false);
        getFreeColServer().getGame().removePlayer(player);

        getFreeColServer().getServer().sendToAll(logoutMessage, connection);

        getFreeColServer().updateMetaServer();

        return null;
    }
}
