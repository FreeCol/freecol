
package net.sf.freecol.server.control;

import java.awt.Color;
import java.net.Socket;
import java.util.Vector;
import java.util.logging.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.MessageHandler;
import net.sf.freecol.common.networking.Connection;

import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;



/**
* Handles the network messages that arrives before the game starts. 
* @see PreGameController
*/
public final class PreGameInputHandler implements MessageHandler {
    private static Logger logger = Logger.getLogger(UserConnectionHandler.class.getName());

    private FreeColServer freeColServer;



    /**
    * The constructor to use.
    * @param freeColServer The main control object.
    */
    public PreGameInputHandler(FreeColServer freeColServer) {
        this.freeColServer = freeColServer;
    }





    /**
    * Handles a network message.
    *
    * @param connection The <code>Connection</code> the message came from.
    * @param element The message to be processed.
    */
    public synchronized Element handle(Connection connection, Element element) {
        Element reply = null;

        String type = element.getTagName();

        if (element != null) {
            if (type.equals("ready")) {
                reply = ready(connection, element);
            } else if (type.equals("setNation")) {
                reply = nation(connection, element);
            } else if (type.equals("setColor")) {
                reply = color(connection, element);
            } else if (type.equals("requestLaunch")) {
                reply = requestLaunch(connection, element);
            } else if (type.equals("chat")) {
                reply = chat(connection, element);
            }
        }

        return reply;
    }


    /**
    * Handles a "ready"-message from a client.
    *
    * @param connection The connection the message came from.
    * @param element The element containing the request.
    */
    private Element ready(Connection connection, Element element) {

        ServerPlayer player = freeColServer.getPlayer(connection);

        if (player != null) {
            boolean ready = (new Boolean(element.getAttribute("value"))).booleanValue();
            player.setReady(ready);

            Element playerReady = Message.createNewRootElement("playerReady");
            playerReady.setAttribute("player", player.getID());
            playerReady.setAttribute("value", Boolean.toString(ready));

            freeColServer.getServer().sendToAll(playerReady, player.getConnection());
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

        ServerPlayer player = freeColServer.getPlayer(connection);

        if (player != null) {
            String nation = element.getAttribute("value");
            player.setNation(nation);

            Element updateNation = Message.createNewRootElement("updateNation");
            updateNation.setAttribute("player", player.getID());
            updateNation.setAttribute("value", nation);

            freeColServer.getServer().sendToAll(updateNation, player.getConnection());
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

        ServerPlayer player = freeColServer.getPlayer(connection);

        if (player != null) {
            String color = element.getAttribute("value");
            player.setColor(color);

            Element updateColor = Message.createNewRootElement("updateColor");
            updateColor.setAttribute("player", player.getID());
            updateColor.setAttribute("value", color);

            freeColServer.getServer().sendToAll(updateColor, player.getConnection());
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
        // TODO: Check if launching player is an admin
        // TODO: Check that no two players have the same color or nation

        // Check if all players are ready.
        if (!freeColServer.getGame().isAllPlayersReadyToLaunch()) {
            Element reply = Message.createNewRootElement("error");
            reply.setAttribute("message", "Not all players are ready to begin the game!");
            reply.setAttribute("messageID", "server.notAllReady");

            return reply;
        }

        freeColServer.getPreGameController().startGame();

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

        freeColServer.getServer().sendToAll(element, connection);

        return null;
    }
}
