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
import net.sf.freecol.common.networking.ChangeSet;
import net.sf.freecol.common.networking.ChangeSet.See;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.MessageHandler;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * Handles the network messages on the server side.
 */
public final class ServerInputHandler extends FreeColServerHolder
    implements MessageHandler {

    private static final Logger logger = Logger.getLogger(ServerInputHandler.class.getName());


    /**
     * The constructor to use.
     * 
     * @param freeColServer The main server object.
     */
    public ServerInputHandler(final FreeColServer freeColServer) {
        super(freeColServer);
    }


    // Implement MessageHandler

    /**
     * {@inheritDoc}
     */
    public Message handle(Connection connection, Message message)
        throws FreeColException {
        final FreeColServer freeColServer = getFreeColServer();
        ServerPlayer serverPlayer = freeColServer.getPlayer(connection);
        if (serverPlayer == null) {
            throw new RuntimeException("No server player available: "
                + message);
        }
        final Game game = freeColServer.getGame();
        final boolean current = message.currentPlayerMessage();
        ChangeSet cs = (current
            && (game == null || serverPlayer != game.getCurrentPlayer()))
            ? serverPlayer.clientError("Received: " + message.getType()
                + " out of turn from player: " + serverPlayer.getNation())
            : message.serverHandler(freeColServer, serverPlayer);
        return (cs == null) ? null : cs.build(serverPlayer);
    }

    /**
     * {@inheritDoc}
     */
    public Message read(Connection connection)
        throws FreeColException, XMLStreamException {
        return Message.read(getGame(), connection.getFreeColXMLReader());
    }
}
