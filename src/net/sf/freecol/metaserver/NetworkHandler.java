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

package net.sf.freecol.metaserver;

import java.io.IOException;
import java.util.logging.Logger;

import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.DOMMessage;
import net.sf.freecol.common.networking.MessageHandler;
import net.sf.freecol.common.networking.RegisterServerMessage;
import net.sf.freecol.common.networking.RemoveServerMessage;
import net.sf.freecol.common.networking.ServerListMessage;
import net.sf.freecol.common.networking.TrivialMessage;
import net.sf.freecol.common.networking.UpdateServerMessage;

import org.w3c.dom.Element;


/**
 * Handle network messages sent to the meta-server.
 */
public final class NetworkHandler implements MessageHandler {
    private static final Logger logger = Logger.getLogger(NetworkHandler.class.getName());

    /** The encapsulated meta-server. */
    private final MetaServer metaServer;

    /** The register of connected FreeColServers. */
    private final MetaRegister metaRegister;



    /**
     * The constructor to use.
     * 
     * @param metaServer The {@code MetaServer} this
     *     {@code NetworkHandler} has been created for.
     * @param metaRegister An object containing a list of all the servers.
     */
    public NetworkHandler(MetaServer metaServer, MetaRegister metaRegister) {
        this.metaServer = metaServer;
        this.metaRegister = metaRegister;
    }

    
    /**
     * Handle a network message.
     *
     * @param connection The {@code Connection} the message came from.
     * @param element The message to be processed.
     */
    @Override
    public synchronized Element handle(Connection connection, Element element) {
        DOMMessage reply = null;
        final String tag = element.getTagName();
        switch (tag) {
        case Connection.DISCONNECT_TAG:
            reply = disconnect(connection, element);
            break;
        case RegisterServerMessage.TAG:
            reply = register(connection, element);
            break;
        case RemoveServerMessage.TAG:
            reply = remove(connection, element);
            break;
        case ServerListMessage.TAG:
            reply = serverList();
            break;
        case UpdateServerMessage.TAG:
            reply = update(connection, element);
            break;
        default:
            logger.warning("Unknown request: " + tag);
            break;
        }
        return (reply == null) ? null : reply.toXMLElement();
    }

    
    /**
     * Handles a "register"-request.
     * 
     * @param connection The connection the message was received on.
     * @param element The element containing the request.
     * @return Null.
     */
    private DOMMessage register(Connection connection, Element element) {
        final RegisterServerMessage message
            = new RegisterServerMessage(null, element);
        message.setAddress(connection.getHostAddress()); // Trust the connection

        metaRegister.addServer(message.getServerInfo());

        return null;
    }


    /**
     * Handles a "serverList"-request.
     *
     * @param connection The connection the message came from.
     * @param element The element containing the request.
     * @return A {@code ServerListMessage} with attached {@code ServerInfo}
     *     for each current server known to the meta-register.
     */
    private DOMMessage serverList() {
        final ServerListMessage message = new ServerListMessage();

        for (ServerInfo si : metaRegister.getServers()) {
            message.addServer(si);
        }
        return message;
    }
        

    /**
     * Handles an "update"-request.
     * 
     * @param connection The connection the message came from.
     * @param element The element containing the request.
     * @return Null.
     */
    private DOMMessage update(Connection connection, Element element) {
        final UpdateServerMessage message
            = new UpdateServerMessage(null, element);
        message.setAddress(connection.getHostAddress());

        metaRegister.updateServer(message.getServerInfo());

        return null;
    }


    /**
     * Handles a "remove"-request.
     * 
     * @param connection The connection the message came from.
     * @param element The element containing the request.
     * @return Null.
     */
    private DOMMessage remove(Connection connection, Element element) {
        final RemoveServerMessage message
            = new RemoveServerMessage(null, element);
        message.setAddress(connection.getHostAddress());

        metaRegister.removeServer(message.getAddress(), message.getPort());

        return null;
    }


    /**
     * Handles a "disconnect"-request.
     * 
     * @param connection The connection the message came from.
     * @param element The element containing the request.
     * @return Null.
     */
    private DOMMessage disconnect(Connection connection, Element element) {
        metaServer.removeConnection(connection);
        connection.reallyClose();

        return null;
    }
}
