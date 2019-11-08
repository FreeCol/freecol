/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.metaserver.ServerInfo;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.DisconnectMessage;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.MessageHandler;
import net.sf.freecol.common.networking.RegisterServerMessage;
import net.sf.freecol.common.networking.RemoveServerMessage;
import net.sf.freecol.common.networking.ServerListMessage;
import net.sf.freecol.common.networking.TrivialMessage;
import net.sf.freecol.common.networking.UpdateServerMessage;


/**
 * Handle network messages sent to the meta-server.
 */
public final class MetaServerHandler implements MessageHandler {
    private static final Logger logger = Logger.getLogger(MetaServerHandler.class.getName());

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
    public MetaServerHandler(MetaServer metaServer, MetaRegister metaRegister) {
        this.metaServer = metaServer;
        this.metaRegister = metaRegister;
    }


    // Implement MessageHandler

    /**
     * {@inheritDoc}
     */
    @Override
    public Message handle(Connection connection, Message message)
        throws FreeColException {
        if (message == null) return null;
        Message reply = null;
        final String tag = message.getType();
        switch (tag) {
        case DisconnectMessage.TAG:
            disconnect(connection);
            break;
        case RegisterServerMessage.TAG:
            RegisterServerMessage rsm = (RegisterServerMessage)message;
            rsm.setAddress(connection.getHostAddress()); // Trust the connection
            register(rsm);
            break;
        case RemoveServerMessage.TAG:
            remove((RemoveServerMessage)message);
            break;
        case ServerListMessage.TAG:
            reply = serverList();
            break;
        case UpdateServerMessage.TAG:
            UpdateServerMessage usm = (UpdateServerMessage)message;
            usm.setAddress(connection.getHostAddress()); // Trust the connection
            update(usm);
            break;
        default:
            logger.warning("Unknown request: " + tag);
            break;
        }
        return reply;
    }

    /**
     * {@inheritDoc}
     */
    public Message read(Connection connection)
        throws FreeColException, XMLStreamException {
        return Message.read(null, connection.getFreeColXMLReader());
    }

    // Individual message handlers

    /**
     * Handle a "disconnect"-request.
     * 
     * @param connection The {@code Connection} the message came from.
     */
    private void disconnect(Connection connection) {
        metaServer.removeConnection(connection);
        connection.close();
    }

    /**
     * Handle a "register"-request.
     * 
     * @param message The {@code RegisterServerMessage} to process.
     */
    private void register(RegisterServerMessage message) {
        final ServerInfo si = message.getServerInfo(); 

        metaRegister.addServer(si);
    }

    /**
     * Handle a "remove"-request.
     * 
     * @param message The {@code RemoveServerMessage} to process.
     */
    private void remove(RemoveServerMessage message) {
        final String address = message.getAddress();
        final int port = message.getPort();

        metaRegister.removeServer(address, port);
    }

    /**
     * Handle a "serverList"-request.
     *
     * @return A {@code ServerListMessage} with attached {@code ServerInfo}
     *     for each current server known to the meta-register.
     */
    private Message serverList() {
        return new ServerListMessage().addServers(metaRegister.getServers());
    }

    /**
     * Handle an "update"-request.
     * 
     * @param message The {@code UpdateServerMessage} to process.
     */
    private void update(UpdateServerMessage message) {
        final ServerInfo si = message.getServerInfo();

        metaRegister.updateServer(si);
    }
}
