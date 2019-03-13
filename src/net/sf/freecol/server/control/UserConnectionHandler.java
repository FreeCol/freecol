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

package net.sf.freecol.server.control;

import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.networking.ChangeSet;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.DisconnectMessage;
import net.sf.freecol.common.networking.LoginMessage;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.MessageHandler;
import net.sf.freecol.server.FreeColServer;


/**
 * Message handler for new client connections.
 */
public final class UserConnectionHandler extends FreeColServerHolder
    implements MessageHandler {

    private static final Logger logger = Logger.getLogger(UserConnectionHandler.class.getName());


    /**
     * Build a new user connection handler.
     *
     * @param freeColServer The main server object.
     */
    public UserConnectionHandler(final FreeColServer freeColServer) {
        super(freeColServer);
    }


    // Implement MessageHandler

    /**
     * {@inheritDoc}
     */
    public Message handle(Connection connection, Message message)
        throws FreeColException {
        final FreeColServer freeColServer = getFreeColServer();
        ChangeSet cs = null;
        switch (message.getType()) {
        case DisconnectMessage.TAG:
            break;
        case LoginMessage.TAG:
            cs = ((LoginMessage)message).loginHandler(freeColServer, connection);
            break;
        default:
            cs = ChangeSet.clientError((Player)null,
                StringTemplate.template("server.couldNotLogin"));
            break;
        }
        return (cs == null) ? null
            : cs.build(freeColServer.getPlayer(connection));
    }

    /**
     * {@inheritDoc}
     */
    public Message read(Connection connection)
        throws FreeColException, XMLStreamException {
        return Message.read(getGame(), connection.getFreeColXMLReader());
    }
}
