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

package net.sf.freecol.metaserver;

import java.io.IOException;
import java.util.logging.Logger;

import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.DOMMessage;
import net.sf.freecol.common.networking.MessageHandler;

import org.w3c.dom.Element;


/**
 * Handles all network messages being sent to the metaserver.
 */
public final class NetworkHandler implements MessageHandler {
    private static final Logger logger = Logger.getLogger(NetworkHandler.class.getName());


    private final MetaServer metaServer;
    private final MetaRegister metaRegister;



    /**
    * The constructor to use.
    * 
    * @param metaServer The <code>MetaServer</code> this
    *       <code>NetworkHandler</code> has been created
    *       for.
    * @param metaRegister An object containing a list
    *       of all the servers.
    */
    public NetworkHandler(MetaServer metaServer, MetaRegister metaRegister) {
        this.metaServer = metaServer;
        this.metaRegister = metaRegister;
    }

    
    /**
    * Handles a network message.
    *
    * @param connection The <code>Connection</code> the message came from.
    * @param element The message to be processed.
    */
    @Override
    public synchronized Element handle(Connection connection, Element element) {
        Element reply = null;

        String type = element.getTagName();

        switch (type) {
            case "register":
                reply = register(connection, element);
                break;
            case "update":
                reply = update(connection, element);
                break;
            case "getServerList":
                reply = getServerList(connection, element);
                break;
            case "remove":
                reply = remove(connection, element);
                break;
            case "disconnect":
                reply = disconnect(connection, element);
                break;
            default:
                logger.warning("Unkown request: " + type);
                break;
        }

        return reply;
    }

    
    /**
     * Handles a "getServerList"-request.
     * @param connection The <code>Connection</code> the message
     *       was received on.
     * @param element The element containing the request.
     * @return The reply: An <code>Element</code> with a list of the
     *      servers in the {@link MetaRegister}.
     */
    private Element getServerList(Connection connection, Element element) {
        return metaRegister.createServerList();
    }


    /**
     * Handles a "register"-request.
     * 
     * @param connection The connection the message was received on.
     * @param element The element containing the request.
     * @return The reply: <code>null</code>.
     */
    private Element register(Connection connection, Element element) {
        String name = element.getAttribute("name");
        String address = connection.getSocket().getInetAddress().getHostAddress();
        int port = Integer.parseInt(element.getAttribute("port"));
        int slotsAvailable = Integer.parseInt(element.getAttribute("slotsAvailable"));
        int currentlyPlaying = Integer.parseInt(element.getAttribute("currentlyPlaying"));
        boolean isGameStarted = Boolean.valueOf(element.getAttribute("isGameStarted"));
        String version = element.getAttribute("version");
        int gameState = Integer.parseInt(element.getAttribute("gameState"));

        try {
            metaRegister.addServer(name, address, port, slotsAvailable, currentlyPlaying, isGameStarted, version, gameState);
        } catch (IOException e) {
            return DOMMessage.createMessage("noRouteToServer");
        }

        return DOMMessage.createMessage("ok");
    }


    /**
     * Handles an "update"-request.
     * 
     * @param connection The connection the message came from.
     * @param element The element containing the request.
     * @return The reply: <code>null</code>.
     */
    private Element update(Connection connection, Element element) {
        String name = element.getAttribute("name");
        String address = connection.getSocket().getInetAddress().getHostAddress();
        int port = Integer.parseInt(element.getAttribute("port"));
        int slotsAvailable = Integer.parseInt(element.getAttribute("slotsAvailable"));
        int currentlyPlaying = Integer.parseInt(element.getAttribute("currentlyPlaying"));
        boolean isGameStarted = Boolean.valueOf(element.getAttribute("isGameStarted"));
        String version = element.getAttribute("version");
        int gameState = Integer.parseInt(element.getAttribute("gameState"));

        try {
            metaRegister.updateServer(name, address, port, slotsAvailable, currentlyPlaying, isGameStarted, version, gameState);
        } catch (IOException e) {}

        return null;
    }


    /**
     * Handles a "remove"-request.
     * 
     * @param connection The connection the message came from.
     * @param element The element containing the request.
     * @return The reply: <code>null</code>.
     */
    private Element remove(Connection connection, Element element) {
        String address = connection.getSocket().getInetAddress().getHostAddress();
        int port = Integer.parseInt(element.getAttribute("port"));

        metaRegister.removeServer(address, port);

        return null;
    }


    /**
     * Handles a "disconnect"-request.
     * 
     * @param connection The connection the message came from.
     * @param element The element containing the request.
     * @return The reply: <code>null</code>.
     */
    private Element disconnect(Connection connection, Element element) {
        connection.reallyClose();
        metaServer.removeConnection(connection);
        return null;
    }
}
