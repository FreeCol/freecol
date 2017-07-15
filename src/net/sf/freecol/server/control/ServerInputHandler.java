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

package net.sf.freecol.server.control;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.networking.ChangeSet;
import net.sf.freecol.common.networking.ChangeSet.See;
import net.sf.freecol.common.networking.ChatMessage;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.DisconnectMessage;
import net.sf.freecol.common.networking.LoginMessage;
import net.sf.freecol.common.networking.LogoutMessage;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.MessageHandler;
import net.sf.freecol.common.networking.QuestionMessage;
import net.sf.freecol.common.networking.TrivialMessage;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.networking.Server;


/**
 * Handles the network messages on the server side.
 * 
 * @see Controller
 */
public abstract class ServerInputHandler extends FreeColServerHolder
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


    /**
     * Get the server player associated with a connection.
     *
     * @param connection The {@code Connection} to look up.
     * @return The {@code ServerPlayer} found.
     */
    protected ServerPlayer getServerPlayer(Connection connection) {
        return getFreeColServer().getPlayer(connection);
    }
    
    /**
     * Internal wrapper for new message handling.
     *
     * @param current If true, insist the message is from the current player
     *     in the game.
     * @param serverPlayer The {@code ServerPlayer} that sent the message.
     * @param message The {@code Message} to handle.
     * @return The resulting reply {@code Message}.
     */
    private Message internalHandler(boolean current, ServerPlayer serverPlayer,
                                    Message message) {
        final FreeColServer freeColServer = getFreeColServer();
        final Game game = freeColServer.getGame();
        ChangeSet cs = (current && (game == null || serverPlayer == null
                || serverPlayer != game.getCurrentPlayer()))
            ? serverPlayer.clientError("Received: " + message.getType()
                + " out of turn from player: " + serverPlayer.getNation())
            : message.serverHandler(freeColServer, serverPlayer);
        return (cs == null) ? null : cs.build(serverPlayer);
    }


    // Implement MessageHandler

    /**
     * {@inheritDoc}
     */
    public Message handle(Connection connection, Message message)
        throws FreeColException {
        ServerPlayer serverPlayer = getServerPlayer(connection);
        if (serverPlayer == null) {
            if (message instanceof LoginMessage) {
                // Very special case of the login question, where
                // there is no player connection set up yet.  Make a
                // dummy one with the current connection.
                serverPlayer = new ServerPlayer(connection);
            } else {
                throw new RuntimeException("No server player available: " + message);
            }
        }
        return internalHandler(message.currentPlayerMessage(), serverPlayer,
                               message);
    }

    /**
     * {@inheritDoc}
     */
    public Message read(Connection connection)
        throws FreeColException, XMLStreamException {
        return Message.read(getGame(), connection.getFreeColXMLReader());
    }
}
