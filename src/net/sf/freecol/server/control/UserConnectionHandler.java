
package net.sf.freecol.server.control;

import java.awt.Color;
import java.net.Socket;
import java.util.Vector;
import java.util.Iterator;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.sf.freecol.FreeCol;

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
* Handles a new client connection. {@link PreGameInputHandler} is set
* as the message handler when the client has successfully logged on.
*/
public final class UserConnectionHandler implements MessageHandler {
    private static Logger logger = Logger.getLogger(UserConnectionHandler.class.getName());

    private FreeColServer freeColServer;



    /**
    * The constructor to use.
    * @param freeColServer The main control object.
    */
    public UserConnectionHandler(FreeColServer freeColServer) {
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
            if (type.equals("login")) {
                reply = login(connection, element);
            }
        }

        return reply;
    }


    /**
    * Handles a "login"-request.
    * @param element The element containing the request.
    */
    private Element login(Connection connection, Element element) {
        // TODO: Do not allow more than one (human) player to connect to a singleplayer game.

        if (!element.hasAttribute("username")) {
            throw new IllegalArgumentException("The attribute 'username' is missing.");
        }

        if (!element.hasAttribute("freeColVersion")) {
            throw new IllegalArgumentException("The attribute 'freeColVersion' is missing.");
        }


        if (!element.getAttribute("freeColVersion").equals(FreeCol.getVersion())) {
            return Message.createError("server.wrongFreeColVersion", "The game versions do not match.");
        }


        if (freeColServer.getGameState() != FreeColServer.STARTING_GAME) {
            return Message.createError("server.alreadyStarted", "The game has already been started!");
        } // TODO: allow a client to take up a lost connection.

        if (!freeColServer.getGame().canAddNewPlayer()) {
            return Message.createError("server.maximumPlayers", "Sorry, the maximum number of players reached.");
        }

        String username = element.getAttribute("username");

        if (freeColServer.getGame().playerNameInUse(username)) {
            return Message.createError("server.usernameInUse", "The specified username is already in use.");
        }


        // Create and add the new player:
        boolean admin = (freeColServer.getGame().getPlayers().size() == 0);
        ServerPlayer newPlayer = new ServerPlayer(freeColServer.getGame(), username, admin, connection.getSocket(), connection);
        freeColServer.getGame().addPlayer(newPlayer);

        // Send message to all players except to the new player:
        Element addNewPlayer = Message.createNewRootElement("addPlayer");
        addNewPlayer.appendChild(newPlayer.toXMLElement(null, addNewPlayer.getOwnerDocument()));
        freeColServer.getServer().sendToAll(addNewPlayer, connection);

        connection.setMessageHandler(freeColServer.getPreGameInputHandler());

        // Make the reply:
        Element reply = Message.createNewRootElement("loginConfirmed");
        reply.setAttribute("admin", (admin ? "true" : "false"));
        reply.appendChild(freeColServer.getGame().toXMLElement(newPlayer, reply.getOwnerDocument()));
        
        return reply;
    }
}
