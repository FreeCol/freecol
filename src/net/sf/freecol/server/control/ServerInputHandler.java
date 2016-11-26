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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.ChatMessage;
import net.sf.freecol.common.networking.DOMMessage;
import net.sf.freecol.common.networking.DisconnectMessage;
import net.sf.freecol.common.networking.LogoutMessage;
import net.sf.freecol.common.networking.MessageHandler;
import net.sf.freecol.common.networking.NetworkRequestHandler;
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
    implements MessageHandler {

    private static final Logger logger = Logger.getLogger(ServerInputHandler.class.getName());

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

        register(ChatMessage.TAG,
            (Connection conn, Element e) -> handler(false, conn,
                new ChatMessage(getGame(), e)));

        register(DisconnectMessage.TAG,
            (Connection conn, Element e) -> handler(false, conn,
                new DisconnectMessage(getGame(), e)));

        register(LogoutMessage.TAG,
            (Connection conn, Element e) -> handler(false, conn,
                new LogoutMessage(getGame(), e)));
    }


    /**
     * Register a network request handler.
     * 
     * @param name The handler name.
     * @param handler The {@code NetworkRequestHandler} to register.
     */
    protected final void register(String name, NetworkRequestHandler handler) {
        this.handlerMap.put(name, handler);
    }

    /**
     * Unregister a network request handler.
     * 
     * @param name The handler name.
     * @param handler The {@code NetworkRequestHandler} to unregister.
     * @return True if the supplied handler was actually removed.
     */
    protected final boolean unregister(String name, NetworkRequestHandler handler) {
        return this.handlerMap.remove(name, handler);
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
        final FreeColServer freeColServer = getFreeColServer();
        final ServerPlayer serverPlayer = freeColServer.getPlayer(connection);
        final Game game = freeColServer.getGame();
        ChangeSet cs = (current && (game == null || serverPlayer == null
                || serverPlayer != game.getCurrentPlayer()))
            ? serverPlayer.clientError("Received: " + message.getType()
                + " out of turn from player: " + serverPlayer.getNation())
            : message.serverHandler(freeColServer, serverPlayer);
        return (cs == null) ? null : cs.build(serverPlayer);
    }


    // Implement MessageHandler

    /**
     * {@inheritDoc}
     */
    public final Element handle(Connection connection, Element element) {
        if (element == null) return null;
        final FreeColServer freeColServer = getFreeColServer();
        final String tag = element.getTagName();
        final NetworkRequestHandler handler = handlerMap.get(tag);
        Element ret = null;

        if (handler == null) {
            // Should we return an error here? The old handler returned null.
            logger.warning("No "
                + freeColServer.getServerState().toString().toLowerCase()
                + " handler for " + tag);
        } else {
            try {
                ret = handler.handle(connection, element);
                logger.log(Level.FINEST, "Handling " + tag + " ok");
            } catch (Exception e) {
                // FIXME: should we really catch Exception? The old code did.
                logger.log(Level.WARNING, "Handling " + tag + " failed", e);
                connection.reconnect();
            }
        }
        return ret;
    }
}
