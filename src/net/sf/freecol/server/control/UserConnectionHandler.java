
package net.sf.freecol.server.control;

import java.util.logging.Logger;

import org.w3c.dom.Element;

import net.sf.freecol.FreeCol;

import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.MessageHandler;
import net.sf.freecol.common.networking.Connection;

import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.common.model.*;




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
            } else {
                logger.warning("Unkown request: " + type);
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
        
        Game game = freeColServer.getGame();

        if (!element.hasAttribute("username")) {
            throw new IllegalArgumentException("The attribute 'username' is missing.");
        }

        if (!element.hasAttribute("freeColVersion")) {
            throw new IllegalArgumentException("The attribute 'freeColVersion' is missing.");
        }


        if (!element.getAttribute("freeColVersion").equals(FreeCol.getVersion())) {
            return Message.createError("server.wrongFreeColVersion", "The game versions do not match.");
        }

        String username = element.getAttribute("username");

        if (freeColServer.getGameState() != FreeColServer.STARTING_GAME) {
            if (game.getPlayerByName(username) == null) {
                return Message.createError("server.alreadyStarted", "The game has already been started!");
            }
            
            ServerPlayer player = (ServerPlayer) game.getPlayerByName(username);
            player.setConnection(connection);
            player.setConnected(true);

            // In case this player is the first to reconnect:
            boolean isCurrentPlayer = (game.getCurrentPlayer() == null);
            if (isCurrentPlayer) {
                game.setCurrentPlayer(player);
            }

            connection.setMessageHandler(freeColServer.getInGameInputHandler());

            // Make the reply:
            Element reply = Message.createNewRootElement("loginConfirmed");
            reply.setAttribute("admin", Boolean.toString(player.isAdmin()));
            reply.setAttribute("startGame", "true");
            reply.setAttribute("isCurrentPlayer", Boolean.toString(isCurrentPlayer));
            reply.appendChild(freeColServer.getGame().toXMLElement(player, reply.getOwnerDocument()));

            return reply;
        }

        if (!freeColServer.getGame().canAddNewPlayer()) {
            return Message.createError("server.maximumPlayers", "Sorry, the maximum number of players reached.");
        }

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
