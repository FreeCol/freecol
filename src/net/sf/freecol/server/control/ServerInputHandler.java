/**
 *  Copyright (C) 2002-2017   The FreeCol Team
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.networking.ChangeSet;
import net.sf.freecol.common.networking.ChangeSet.See;
import net.sf.freecol.common.networking.ChatMessage;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.DOMMessage;
import net.sf.freecol.common.networking.DOMMessageHandler;
import net.sf.freecol.common.networking.DisconnectMessage;
import net.sf.freecol.common.networking.LogoutMessage;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.MessageHandler;
import net.sf.freecol.common.networking.TrivialMessage;
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
    implements MessageHandler, DOMMessageHandler {

    private static final Logger logger = Logger.getLogger(ServerInputHandler.class.getName());

    /**
     * A network request handler knows how to handle in a given request type.
     */
    public interface DOMNetworkRequestHandler {

        /**
         * Handle a request represented by an {@link Element} and
         * return another {@link Element} or null as the answer.
         * 
         * @param connection The message's {@code Connection}.
         * @param element The root {@code Element} of the message.
         * @return The reply {@code Element}, which may be null.
         */
        Element handle(Connection connection, Element element);
    };

    /**
     * The handler map provides named handlers for network
     * requests.  Each handler deals with a given request type.
     */
    private final Map<String, DOMNetworkRequestHandler> handlerMap
        = Collections.synchronizedMap(new HashMap<String, DOMNetworkRequestHandler>());


    /**
     * The constructor to use.
     * 
     * @param freeColServer The main server object.
     */
    public ServerInputHandler(final FreeColServer freeColServer) {
        super(freeColServer);

        register(ChatMessage.TAG,
            (Connection conn, Element e) -> handler(false, conn,
                new ChatMessage(getGame(), e)));

        register(DisconnectMessage.TAG,
            (Connection conn, Element e) -> handler(false, conn,
                TrivialMessage.disconnectMessage));

        register(LogoutMessage.TAG,
            (Connection conn, Element e) -> handler(false, conn,
                new LogoutMessage(getGame(), e)));
    }


    /**
     * Register a network request handler.
     * 
     * @param name The handler name.
     * @param handler The {@code DOMNetworkRequestHandler} to register.
     */
    protected final void register(String name, DOMNetworkRequestHandler handler) {
        this.handlerMap.put(name, handler);
    }

    /**
     * Unregister a network request handler.
     * 
     * @param name The handler name.
     * @param handler The {@code DOMNetworkRequestHandler} to unregister.
     * @return True if the supplied handler was actually removed.
     */
    protected final boolean unregister(String name, DOMNetworkRequestHandler handler) {
        return this.handlerMap.remove(name, handler);
    }

    /**
     * Get the server player associated with a connection.
     *
     * @param connection The {@code Connection} to look up.
     * @return The {@code ServerPlayer} found.
     */
    protected ServerPlayer getServerPlayer(Connection connection) {
        return getFreeColServer().getPlayer(connection);
    }
    
    /**
     * Internal wrapper for new message handling.
     *
     * @param current If true, insist the message is from the current player
     *     in the game.
     * @param serverPlayer The {@code ServerPlayer} that sent the message.
     * @param message The {@code Message} to handle.
     * @return The resulting reply {@code Message}.
     */
    private Message internalHandler(boolean current, ServerPlayer serverPlayer,
                                    Message message) {
        final FreeColServer freeColServer = getFreeColServer();
        final Game game = freeColServer.getGame();
        ChangeSet cs = (current && (game == null || serverPlayer == null
                || serverPlayer != game.getCurrentPlayer()))
            ? serverPlayer.clientError("Received: " + message.getType()
                + " out of turn from player: " + serverPlayer.getNation())
            : message.serverHandler(freeColServer, serverPlayer);
        return (cs == null) ? null : cs.build(serverPlayer);
    }

    /**
     * Wrapper for new message handling.
     *
     * @param current If true, insist the message is from the current player
     *     in the game.
     * @param connection The {@code Connection} the message arrived on.
     * @param message The {@code DOMMessage} to handle.
     * @return The resulting reply {@code Element}.
     */
    protected Element handler(boolean current, Connection connection,
                              DOMMessage message) {
        Message m = internalHandler(current, getServerPlayer(connection),
                                    message);
        return (m == null) ? null : ((DOMMessage)m).toXMLElement();
    }

    // Implement DOMMessageHandler

    /**
     * {@inheritDoc}
     */
    public final Element handle(Connection connection, Element element) {
        if (element == null) return null;
        final FreeColServer freeColServer = getFreeColServer();
        final String tag = element.getTagName();
        final DOMNetworkRequestHandler handler = handlerMap.get(tag);
        Element ret = null;

        if (handler == null) {
            // Should we return an error here? The old handler returned null.
            logger.warning("No "
                + freeColServer.getServerState().toString().toLowerCase()
                + " handler for " + tag);
        } else {
            try {
                ret = handler.handle(connection, element);
                logger.log(Level.FINEST, "Handling " + tag + " ok = "
                    + ((ret == null) ? "null" : ret.getTagName()));
            } catch (Exception e) {
                // FIXME: should we really catch Exception? The old code did.
                logger.log(Level.WARNING, "Handling " + tag + " failed", e);
                connection.sendReconnect();
            }
        }
        return ret;
    }


    // Implement MessageHandler

    /**
     * {@inheritDoc}
     */
    public Message handle(Message message) throws FreeColException {
        return internalHandler(message.currentPlayerMessage(),
                               message.getSourcePlayer(),
                               message);
    }

    /**
     * {@inheritDoc}
     */
    public Message read(Connection connection)
        throws FreeColException, XMLStreamException {
        Message ret = Message.read(getGame(), connection.getFreeColXMLReader());
        if (ret != null) ret.setSourcePlayer(getServerPlayer(connection));
        return ret;
    }
}
