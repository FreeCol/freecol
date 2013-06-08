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


package net.sf.freecol.metaserver;


import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.DOMMessage;
import net.sf.freecol.common.networking.MessageHandler;

import org.w3c.dom.Element;


/**
 * Handles all network messages beeing sent to the metaserver.
 */
public final class NetworkHandler implements MessageHandler {
    private static Logger logger = Logger.getLogger(NetworkHandler.class.getName());


    private MetaServer metaServer;
    private MetaRegister metaRegister;



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
    public synchronized Element handle(Connection connection, Element element) {
        Element reply = null;

        String type = element.getTagName();

        if (type.equals("register")) {
            reply = register(connection, element);
        } else if (type.equals("update")) {
            reply = update(connection, element);
        } else if (type.equals("getServerList")) {
            reply = getServerList(connection, element);
        } else if (type.equals("remove")) {
            reply = remove(connection, element);
        } else if (type.equals("disconnect")) {
            reply = disconnect(connection, element);
        } else {
            logger.warning("Unkown request: " + type);
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
        boolean isGameStarted = Boolean.valueOf(element.getAttribute("isGameStarted")).booleanValue();
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
        boolean isGameStarted = Boolean.valueOf(element.getAttribute("isGameStarted")).booleanValue();
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
        try {
            connection.reallyClose();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not close the connection.", e);
        }

        metaServer.removeConnection(connection);

        return null;
    }
}
