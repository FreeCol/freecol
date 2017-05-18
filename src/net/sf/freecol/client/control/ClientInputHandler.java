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
import net.sf.freecol.common.networking.DisconnectMessage;
import net.sf.freecol.common.networking.DOMMessageHandler;
import net.sf.freecol.common.networking.ErrorMessage;
import net.sf.freecol.common.networking.GameStateMessage;
import net.sf.freecol.common.networking.LoginMessage;
import net.sf.freecol.common.networking.LogoutMessage;
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

    protected final Runnable displayModelMessagesRunnable = () -> {
        igc().displayModelMessages(false);
    };

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

        register(DisconnectMessage.TAG, (Connection c, Element e) ->
            TrivialMessage.disconnectMessage.clientHandler(freeColClient));

        register(ErrorMessage.TAG,
            (Connection c, Element e) ->
                new ErrorMessage(getGame(), e).clientHandler(freeColClient));

        register(GameStateMessage.TAG, (Connection c, Element e) ->
            new GameStateMessage(getGame(), e).clientHandler(freeColClient));

        register(LoginMessage.TAG,
            (Connection c, Element e) ->
                new LoginMessage(null, e).clientHandler(freeColClient));

        register(LogoutMessage.TAG,
            (Connection c, Element e) ->
                new LogoutMessage(getGame(), e).clientHandler(freeColClient));

        register(MultipleMessage.TAG, (Connection c, Element e) ->
            new MultipleMessage(getGame(), e).clientHandler(freeColClient));

        register(VacantPlayersMessage.TAG, (Connection c, Element e) ->
            new VacantPlayersMessage(getGame(), e).clientHandler(freeColClient));
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

    /**
     * Shorthand to run in the EDT and wait.
     *
     * @param runnable The {@code Runnable} to run.
     */
    protected void invokeAndWait(Runnable runnable) {
        getGUI().invokeNowOrWait(runnable);
    }
    
    /**
     * Shorthand to run in the EDT eventually.
     *
     * @param runnable The {@code Runnable} to run.
     */
    protected void invokeLater(Runnable runnable) {
        getGUI().invokeNowOrLater(runnable);
    }


    // Implement DOMMessageHandler

    /**
     * {@inheritDoc}
     */
    public Element handle(Connection connection, Element element) {
        if (element == null) return null;

        final String logMe = "Client handler ";
        final String tag = element.getTagName();
        DOMClientNetworkRequestHandler handler = domHandlerMap.get(tag);
        if (handler == null) {
            throw new RuntimeException(logMe + "missing for " + tag);
        }

        try {
            handler.handle(connection, element);
            logger.log(Level.FINEST, logMe + tag + " to null");
        } catch (Exception ex) {
            logger.log(Level.WARNING, logMe + "failed " + tag, ex);
        }
        return null;
    }


    // Implement MessageHandler

    /**
     * {@inheritDoc}
     */
    public Message handle(Message message) throws FreeColException {
        message.clientHandler(getFreeColClient());

        if (currentPlayerIsMyPlayer()) {
            // Play a sound if specified
            String sound = message.getStringAttribute("sound");
            if (sound != null && !sound.isEmpty()) {
                getGUI().playSound(sound);
            }
            // If there is a "flush" attribute present, encourage the
            // client to display any new messages.
            if (message.getBooleanAttribute("flush", false)) {
                invokeLater(displayModelMessagesRunnable);
            }
        }
        return null; 
    }

    /**
     * {@inheritDoc}
     */
    public Message read(Connection connection)
        throws FreeColException, XMLStreamException {
        return Message.read(getGame(), connection.getFreeColXMLReader());
    }
}
