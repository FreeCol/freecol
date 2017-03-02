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
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.FreeColClientHolder;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.DOMMessageHandler;
import net.sf.freecol.common.networking.GameStateMessage;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.MessageHandler;
import net.sf.freecol.common.networking.MultipleMessage;
import net.sf.freecol.common.networking.TrivialMessage;
import net.sf.freecol.common.networking.VacantPlayersMessage;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.FreeColServer.ServerState;
import net.sf.freecol.common.util.Introspector;

import org.w3c.dom.Element;


/**
 * Provides common methods for input handlers on the client side.
 */
public abstract class ClientInputHandler extends FreeColClientHolder
    implements MessageHandler, DOMMessageHandler {

    private static final Logger logger = Logger.getLogger(ClientInputHandler.class.getName());

    /**
     * Handle a DOM request to a client.
     */
    public interface DOMClientNetworkRequestHandler {
        void handle(Connection connection, Element element) throws FreeColException;
    }

    private final Map<String, DOMClientNetworkRequestHandler> domHandlerMap
        = Collections.synchronizedMap(new HashMap<String, DOMClientNetworkRequestHandler>());


    /**
     * The constructor to use.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    public ClientInputHandler(FreeColClient freeColClient) {
        super(freeColClient);

        register(TrivialMessage.DISCONNECT_TAG, (Connection c, Element e) ->
            disconnect());
        register(GameStateMessage.TAG, (Connection c, Element e) ->
            new GameStateMessage(getGame(), e).clientHandler(freeColClient));
        register(MultipleMessage.TAG, (Connection c, Element e) ->
            multiple(new MultipleMessage(getGame(), e)));
        register(VacantPlayersMessage.TAG, (Connection c, Element e) ->
            vacantPlayers(new VacantPlayersMessage(getGame(), e)));
    }


   /**
     * Register a network request handler.
     * 
     * @param name The handler name.
     * @param handler The {@code DOMClientNetworkRequestHandler} to register.
     */
    protected final void register(String name, DOMClientNetworkRequestHandler handler) {
        this.domHandlerMap.put(name, handler);
    }

    /**
     * Unregister a network request handler.
     * 
     * @param name The handler name.
     * @param handler The {@code ClienNetworkRequestHandler} to unregister.
     * @return True if the supplied handler was actually removed.
     */
    protected final boolean unregister(String name, DOMClientNetworkRequestHandler handler) {
        return this.domHandlerMap.remove(name, handler);
    }


    // Useful handlers

    /**
     * Handle a "disconnect"-message.
     */
    protected void disconnect() {
        ; // Do nothing
    }

    /**
     * Handle a "multiple"-message.
     *
     * @param message The {@code MultipleMessage} to process.
     */
    private void multiple(MultipleMessage message) {
        // Using null Connection, it should go away completely soon
        Message result = message.applyHandler(this, null);
        if (result != null) {
            logger.warning("Multiple message -> " + result.getType());
        }
    }

    /**
     * Handle a "vacantPlayers"-message.
     *
     * @param message The {@code VacantPlayersMessage} to process.
     */
    private void vacantPlayers(VacantPlayersMessage message) {
        final FreeColClient fcc = getFreeColClient();
        final List<String> vacant = message.getVacantPlayers();

        fcc.setVacantPlayerNames(vacant);
    }


    // Implement DOMMessageHandler

    /**
     * {@inheritDoc}
     */
    public Element handle(Connection connection, Element element)
        throws FreeColException {
        if (element == null) return null;
        final String tag = element.getTagName();
        DOMClientNetworkRequestHandler handler = domHandlerMap.get(tag);
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


    // Implement MessageHandler

    /**
     * {@inheritDoc}
     */
    public Message handle(Message message) throws FreeColException {
        message.clientHandler(getFreeColClient());
        return null; 
    }

    /**
     * {@inheritDoc}
     */
    public Message read(FreeColXMLReader xr)
        throws FreeColException, XMLStreamException {
        return Message.read(getGame(), xr);
    }
}
