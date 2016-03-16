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

package net.sf.freecol.client.control;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.FreeColClientHolder;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.MessageHandler;
import net.sf.freecol.common.networking.NetworkRequestHandler;

import org.w3c.dom.Element;


/**
 * Provides common methods for input handlers on the client side.
 */
public abstract class ClientInputHandler extends FreeColClientHolder
    implements MessageHandler {

    private static final Logger logger = Logger.getLogger(ClientInputHandler.class.getName());

    /**
     * The handler map provides named handlers for network
     * requests.  Each handler deals with a given request type.
     */
    private final Map<String, NetworkRequestHandler> handlerMap
        = Collections.synchronizedMap(new HashMap<String, NetworkRequestHandler>());


    /**
     * The constructor to use.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public ClientInputHandler(FreeColClient freeColClient) {
        super(freeColClient);

        register(Connection.DISCONNECT_TAG,
            (Connection c, Element e) -> disconnect(e));
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


    // Useful handlers

    /**
     * Handle a "disconnect"-message.
     *
     * @param element The element (root element in a DOM-parsed XML
     *     tree) that holds all the information.
     * @return Null.
     */
    protected Element disconnect(Element element) {
        // Updating the GUI should always be done in the EDT:
        javax.swing.SwingUtilities.invokeLater(() -> {
                if (getGUI().containsInGameComponents()) {
                    if (getFreeColServer() == null) {
                        getGUI().returnToTitle();
                    } else {
                        getGUI().removeInGameComponents();
                    }
                }
            });

        return null;
    }


    // Implement MessageHandler

    /**
     * {@inheritDoc}
     */
    public Element handle(Connection connection, Element element) {
        if (element == null) return null;
        final String tag = element.getTagName();
        NetworkRequestHandler handler = handlerMap.get(tag);
        Element reply = null;
        if (handler == null) {
            logger.warning("Client ignore: " + tag);
        } else {
            try {
                reply = handler.handle(connection, element);
                logger.log(Level.FINEST, "Client ok: " + tag
                    + " to " + ((reply == null) ? "null" : reply.getTagName()));
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Client fail: " + tag, ex);
                connection.reconnect();
            }
        }
        return reply;
    }
}
