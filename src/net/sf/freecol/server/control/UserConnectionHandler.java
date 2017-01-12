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

import java.util.logging.Logger;

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.networking.ChangeSet;
import net.sf.freecol.common.networking.DOMMessage;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.GameStateMessage;
import net.sf.freecol.common.networking.LoginMessage;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.VacantPlayersMessage;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * Handles a new client connection.  {@link PreGameInputHandler} is
 * set as the message handler when the client has successfully logged
 * on.
 */
public final class UserConnectionHandler extends ServerInputHandler {

    private static final Logger logger = Logger.getLogger(UserConnectionHandler.class.getName());


    /**
     * Build a new user connection handler.
     *
     * @param freeColServer The main control object.
     */
    public UserConnectionHandler(final FreeColServer freeColServer) {
        super(freeColServer);

        register(GameStateMessage.TAG,
            (Connection conn, Element e) -> handler(conn,
                new GameStateMessage(getGame(), e)));
        register(LoginMessage.TAG,
            (Connection conn, Element e) -> loginHandler(conn,
                new LoginMessage(getGame(), e)));
        register(VacantPlayersMessage.TAG,
            (Connection conn, Element e) -> handler(conn,
                new VacantPlayersMessage(getGame(), e)));
    }

    /**
     * Special handler for login.
     * 
     * Logging in is a very special case as it is the point where a
     * player is created.
     *
     * @param connection The {@code Connection} the login message arrived on.
     * @param message The incoming {@code LoginMessage}.
     * @return An {@code Element} encapsulating the login.
     */
    private Element loginHandler(Connection connection, LoginMessage message) {
        final FreeColServer freeColServer = getFreeColServer();
        final ServerPlayer serverPlayer = new ServerPlayer(connection);
        ChangeSet cs = message.serverHandler(freeColServer, serverPlayer);
        // The player may have already been present, in which case the
        // connection is transferred to the existing player and the stub
        // player ignored.  Find the real player associated with the name
        // in the login message.  That is the one that needs to see the
        // response.
        ServerPlayer real = message.getPlayer(freeColServer.getGame());
        Message m = cs.build(real);
        return (m == null) ? null : ((DOMMessage)m).toXMLElement();
    }
}
