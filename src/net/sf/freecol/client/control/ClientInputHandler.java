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

package net.sf.freecol.client.control;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.FreeColClientHolder;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.GameStateMessage;
import net.sf.freecol.common.networking.MessageHandler;
import net.sf.freecol.common.networking.TrivialMessage;
import net.sf.freecol.common.networking.VacantPlayersMessage;

import org.w3c.dom.Element;


/**
 * Provides common methods for input handlers on the client side.
 */
public abstract class ClientInputHandler extends FreeColClientHolder
    implements MessageHandler {

    private static final Logger logger = Logger.getLogger(ClientInputHandler.class.getName());

    /**
     * Handle a request to a client.
     *
     */
    public interface ClientNetworkRequestHandler {
        void handle(Connection connection, Element element);
    }
        
    /**
     * The handler map provides named handlers for network
     * requests.  Each handler deals with a given request type.
     */
    private final Map<String, ClientNetworkRequestHandler> handlerMap
        = Collections.synchronizedMap(new HashMap<String, ClientNetworkRequestHandler>());


    /**
     * The constructor to use.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    public ClientInputHandler(FreeColClient freeColClient) {
        super(freeColClient);

        register(TrivialMessage.DISCONNECT_TAG,
            (Connection c, Element e) -> disconnect(e));
        register(GameStateMessage.TAG,
            (Connection c, Element e) -> gameState(e));
        register(VacantPlayersMessage.TAG,
            (Connection c, Element e) -> vacantPlayers(e));
    }


   /**
     * Register a network request handler.
     * 
     * @param name The handler name.
     * @param handler The {@code ClientNetworkRequestHandler} to register.
     */
    protected final void register(String name, ClientNetworkRequestHandler handler) {
        this.handlerMap.put(name, handler);
    }

    /**
     * Unregister a network request handler.
     * 
     * @param name The handler name.
     * @param handler The {@code ClienNetworkRequestHandler} to unregister.
     * @return True if the supplied handler was actually removed.
     */
    protected final boolean unregister(String name, ClientNetworkRequestHandler handler) {
        return this.handlerMap.remove(name, handler);
    }


    // Useful handlers

    /**
     * Handle a "disconnect"-message.
     *
     * @param element The element (root element in a DOM-parsed XML
     *     tree) that holds all the information.
     */
    protected void disconnect(Element element) {
        ; // Do nothing
    }

    /**
     * Handle a "gameState"-message.
     *
     * @param element The {@code Element} to process.
     */
    private void gameState(Element element) {
        final FreeColClient fcc = getFreeColClient();
        final GameStateMessage message
            = new GameStateMessage(fcc.getGame(), element);

        fcc.setServerState(message.getState());
    }

    /**
     * Handle a "vacantPlayers"-message.
     *
     * @param element The {@code Element} to process.
     */
    private void vacantPlayers(Element element) {
        final FreeColClient fcc = getFreeColClient();
        final VacantPlayersMessage message
            = new VacantPlayersMessage(fcc.getGame(), element);

        fcc.setVacantPlayerNames(message.getVacantPlayers());
    }


    // Implement MessageHandler

    /**
     * {@inheritDoc}
     */
    public Element handle(Connection connection, Element element) {
        if (element == null) return null;
        final String tag = element.getTagName();
        ClientNetworkRequestHandler handler = handlerMap.get(tag);
        try {
            if (handler == null) {
                logger.warning("Client ignored: " + tag);
            } else {
                handler.handle(connection, element);
                logger.log(Level.FINEST, "Client handled: " + tag);
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Client failed: " + tag, ex);
        }
        return null;
    }
}
