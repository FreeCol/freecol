/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.DOMMessage;
import net.sf.freecol.common.networking.ChatMessage;
import net.sf.freecol.common.networking.MessageHandler;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.networking.Server;

import org.w3c.dom.Element;

/**
 * Handles the network messages.
 * 
 * @see Controller
 */
public abstract class InputHandler extends FreeColServerHolder implements MessageHandler {

    private static Logger logger = Logger.getLogger(InputHandler.class.getName());

    /**
     * The handler map provides named handlers for network requests. Each
     * handler deals with a given request type.
     */
    private final Map<String, NetworkRequestHandler> _handlerMap = Collections
            .synchronizedMap(new HashMap<String, NetworkRequestHandler>());


    /**
     * The constructor to use.
     * 
     * @param freeColServer The main server object.
     */
    public InputHandler(final FreeColServer freeColServer) {
        super(freeColServer);
        // All sub-classes are forced to implement this one
        register("logout", new NetworkRequestHandler() {
                public Element handle(Connection connection, Element element) {
                    return logout(connection, element);
                }
            });
        register("disconnect", new DisconnectHandler());
        register("chat", new NetworkRequestHandler() {
                public Element handle(Connection connection, Element element) {
                    return new ChatMessage(freeColServer.getGame(),
                                           element).handle(freeColServer, connection);
                }
            });
    }

    /**
     * Register a network request handler.
     * 
     * @param name The name.
     * @param handler The handler.
     */
    protected void register(String name, NetworkRequestHandler handler) {
        _handlerMap.put(name, handler);
    }

    /**
     * Deals with incoming messages that have just been received.
     * 
     * @param connection The <code>Connection</code> the message was received
     *            on.
     * @param element The root element of the message.
     * @return The reply.
     */
    public final Element handle(Connection connection, Element element) {
        String tagName = element.getTagName();
        NetworkRequestHandler handler = _handlerMap.get(tagName);
        if (handler != null) {
            try {
                return handler.handle(connection, element);
            } catch (Exception e) {
                // TODO: should we really catch Exception? The old code did.
                logger.log(Level.WARNING, "Handler failed", e);
                sendReconnectSafely(connection);
            }
        } else {
            // Should we return an error here? The old handler returned null.
            logger.warning("No handler installed for " + tagName);
        }
        return null;
    }

    /**
     * Send a reconnect message ignoring (but logging) IO errors.
     * 
     * @param connection The connection.
     */
    private void sendReconnectSafely(Connection connection) {
        try {
            connection.sendDumping(DOMMessage.createNewRootElement("reconnect"));
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not send reconnect message!", e);
        }
    }

    /**
     * Create a reply message with an error.
     * 
     * @param message The error message.
     * @return reply element with message.
     */
    protected Element createErrorReply(String message) {
        Element reply = DOMMessage.createNewRootElement("error");
        // TODO: should this be localized (return message name)?
        reply.setAttribute("message", message);
        return reply;
    }

    /**
     * Handles a "logout"-message.
     * 
     * @param connection The <code>Connection</code> the message was received
     *            on.
     * @param logoutElement The element (root element in a DOM-parsed XML tree)
     *            that holds all the information.
     * @return The reply.
     */
    abstract protected Element logout(Connection connection, Element logoutElement);


    /**
     * A network request handler knows how to handle in a given request type.
     */
    interface NetworkRequestHandler {
        /**
         * Handle a request represented by an {@link Element} and return another
         * {@link Element} or null as the answer.
         * 
         * @param connection The message's <code>Connection</code>.
         * @param element The root element of the message.
         * @return reply element, may be null.
         */
        Element handle(Connection connection, Element element);
    }

    /**
     * A network request handler for the current player will automatically
     * return an error (&quot;not your turn&quot;) if called by a connection
     * other than that of the currently active player. If no game is active or
     * if the player is unknown the same error is returned.
     */
    abstract class CurrentPlayerNetworkRequestHandler implements NetworkRequestHandler {
        public final Element handle(Connection conn, Element element) {
            ServerPlayer player = getFreeColServer().getPlayer(conn);
            if (isCurrentPlayer(player)) {
                try {
                    return handle(player, conn, element);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Handler failure.", e);
                    sendReconnectSafely(conn);
                    return null;
                }
            } else {
                logger.warning("Received message out of turn from " 
                        + player.getNation()
                        + " player:"
                        + element.getTagName());
                return createErrorReply("Not your turn.");
            }
        }

        /**
         * Check if a player is the current player.
         * 
         * @param player The player.
         * @return true if a game is active and the player is the current one.
         */
        private boolean isCurrentPlayer(Player player) {
            Game game = getFreeColServer().getGame();
            if (player != null && game != null) {
                return player.equals(game.getCurrentPlayer());
            }
            return false;
        }

        /**
         * Handle a request for the current player.
         * 
         * @param player The player.
         * @param conn The connection.
         * @param element The element with the request.
         * @return answer element, may be null.
         */
        protected abstract Element handle(Player player, Connection conn, Element element);
    }

    private class DisconnectHandler implements NetworkRequestHandler {
        public Element handle(Connection connection, Element disconnectElement) {
            // The player should be logged out by now, but just in case:
            ServerPlayer player = getFreeColServer().getPlayer(connection);
            logDisconnect(connection, player);
            if (player != null && player.isConnected()) {
                logout(connection, null);
            }
            try {
                connection.reallyClose();
            } catch (IOException e) {
                logger.warning("Could not close the connection.");
            }
            Server server = getFreeColServer().getServer();
            if (server != null) {
                server.removeConnection(connection);
            }
            return null;
        }

        private void logDisconnect(Connection connection, ServerPlayer player) {
            logger.info("Disconnection by: " + connection + ((player != null) ? " (" + player.getName() + ") " : ""));
        }
    }
}
