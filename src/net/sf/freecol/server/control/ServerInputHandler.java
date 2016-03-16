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
 * Handles the network messages on the server side.
 * 
 * @see Controller
 */
public abstract class ServerInputHandler extends FreeColServerHolder
    implements MessageHandler {

    private static final Logger logger = Logger.getLogger(ServerInputHandler.class.getName());

    private static final String LOGOUT_TAG = "logout";

    /**
     * The handler map provides named handlers for network
     * requests.  Each handler deals with a given request type.
     */
    private final Map<String, NetworkRequestHandler> handlerMap
        = Collections.synchronizedMap(new HashMap<String, NetworkRequestHandler>());


    /**
     * The constructor to use.
     * 
     * @param freeColServer The main server object.
     */
    public ServerInputHandler(final FreeColServer freeColServer) {
        super(freeColServer);

        register(ChatMessage.TAG, (Connection conn, Element e) ->
            new ChatMessage(getGame(), e).handle(freeColServer, conn));

        register(Connection.DISCONNECT_TAG, (Connection conn, Element e) ->
            disconnect(conn, e));

        register(LOGOUT_TAG, (Connection conn, Element e) ->
            logout(conn, e));
    }


    /**
     * Register a network request handler.
     * 
     * @param name The handler name.
     * @param handler The <code>NetworkRequestHandler</code> to register.
     */
    protected final void register(String name, NetworkRequestHandler handler) {
        this.handlerMap.put(name, handler);
    }

    /**
     * Unregister a network request handler.
     * 
     * @param name The handler name.
     * @param handler The <code>NetworkRequestHandler</code> to unregister.
     * @return True if the supplied handler was actually removed.
     */
    protected final boolean unregister(String name, NetworkRequestHandler handler) {
        return this.handlerMap.remove(name, handler);
    }

    /**
     * Handle a "disconnect"-message.
     * 
     * @param connection The <code>Connection</code> the message was received
     *     on.
     * @param element The <code>Element</code> (root element in a
     *     DOM-parsed XML tree) that holds all the information.
     * @return Null.
     */
    protected Element disconnect(Connection connection, Element element) {
        // The player should be logged out by now, but just in case:
        ServerPlayer player = getFreeColServer().getPlayer(connection);
        logger.info("Disconnecting player "
            + ((player == null) ? "null" : player.getName()));
        if (player != null && player.isConnected()) {
            logout(connection, null);
        }
        connection.reallyClose();
        Server server = getFreeColServer().getServer();
        if (server != null) server.removeConnection(connection);
        return null;
    }
    
    /**
     * Handle a "logout"-message.
     * 
     * @param connection The <code>Connection</code> the message was received
     *     on.
     * @param element The <code>Element</code> (root element in a
     *     DOM-parsed XML tree) that holds all the information.
     * @return The reply.
     */
    protected abstract Element logout(Connection connection, Element element);


    // Implement MessageHandler

    /**
     * {@inheritDoc}
     */
    public final Element handle(Connection connection, Element element) {
        if (element == null) return null;
        final String tag = element.getTagName();
        NetworkRequestHandler handler = handlerMap.get(tag);
        if (handler == null) {
            // Should we return an error here? The old handler returned null.
            logger.warning("No handler installed for " + tag);
        } else {
            try {
                logger.log(Level.FINEST, "Handling " + tag);
                return handler.handle(connection, element);
            } catch (Exception e) {
                // FIXME: should we really catch Exception? The old code did.
                logger.log(Level.WARNING, "Handler failure for " + tag, e);
                connection.reconnect();
            }
        }
        return null;
    }
}
