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

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.DOMMessage;
import net.sf.freecol.common.networking.ChatMessage;
import net.sf.freecol.common.networking.MessageHandler;
import net.sf.freecol.common.networking.NetworkRequestHandler;
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

    private static final Logger logger = Logger.getLogger(InputHandler.class.getName());

    /**
     * The handler map provides named handlers for network requests. Each
     * handler deals with a given request type.
     */
    private final Map<String, NetworkRequestHandler> _handlerMap
        = Collections.synchronizedMap(new HashMap<String, NetworkRequestHandler>());


    /**
     * The constructor to use.
     * 
     * @param freeColServer The main server object.
     */
    public InputHandler(final FreeColServer freeColServer) {
        super(freeColServer);
        // All sub-classes are forced to implement this one
        register("logout", (Connection connection, Element element) ->
            logout(connection, element));
        register("disconnect", new DisconnectHandler());
        register("chat", (Connection connection, Element element) ->
            new ChatMessage(getGame(), element)
                .handle(freeColServer, connection));
    }

    /**
     * Register a network request handler.
     * 
     * @param name The name.
     * @param handler The handler.
     */
    protected final void register(String name, NetworkRequestHandler handler) {
        _handlerMap.put(name, handler);
    }

    /**
     * Unregister a network request handler.
     * 
     * @param name The name.
     * @param handler The handler.
     * @return True if the supplied handler was actually removed.
     */
    protected final boolean unregister(String name, NetworkRequestHandler handler) {
        // _handlerMap.remove(name, handler) would be better?
        return _handlerMap.remove(name) == handler;
    }

    /**
     * Deals with incoming messages that have just been received.
     * 
     * @param connection The <code>Connection</code> the message was received
     *            on.
     * @param element The root element of the message.
     * @return The reply.
     */
    @Override
    public final Element handle(Connection connection, Element element) {
        if (element == null) return null;
        String tagName = element.getTagName();
        NetworkRequestHandler handler = _handlerMap.get(tagName);
        if (handler != null) {
            try {
                logger.log(Level.FINEST, "Handling " + tagName);
                return handler.handle(connection, element);
            } catch (Exception e) {
                // FIXME: should we really catch Exception? The old code did.
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
            connection.send(DOMMessage.createMessage("reconnect"));
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not send reconnect message!", e);
        }
    }

    /**
     * Create a reply message with an error.
     *
     * FIXME: should this be localized (return message name)?
     * 
     * @param message The error message.
     * @return An error message.
     */
    protected Element createErrorReply(String message) {
        return DOMMessage.createMessage("error",
            "message", message);
    }

    /**
     * Handles a "logout"-message.
     * 
     * @param connection The <code>Connection</code> the message was received
     *     on.
     * @param element The <code>Element</code> (root element in a
     *     DOM-parsed XML tree) that holds all the information.
     * @return The reply.
     */
    protected abstract Element logout(Connection connection, Element element);


    private class DisconnectHandler implements NetworkRequestHandler {

        @Override
        public Element handle(Connection connection, Element disconnectElement) {
            // The player should be logged out by now, but just in case:
            ServerPlayer player = getFreeColServer().getPlayer(connection);
            logDisconnect(connection, player);
            if (player != null && player.isConnected()) {
                logout(connection, null);
            }
            connection.reallyClose();
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
