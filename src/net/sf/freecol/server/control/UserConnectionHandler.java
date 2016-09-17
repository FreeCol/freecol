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

package net.sf.freecol.server.control;

import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.DOMMessage;
import net.sf.freecol.common.networking.GameStateMessage;
import net.sf.freecol.common.networking.LoginMessage;
import net.sf.freecol.common.networking.MessageHandler;
import net.sf.freecol.common.networking.VacantPlayersMessage;
import net.sf.freecol.server.FreeColServer;

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

        final Game game = freeColServer.getGame();
        register(GameStateMessage.TAG, (Connection c, Element e) ->
            new GameStateMessage(game, e).handle(freeColServer, c));
        register(LoginMessage.TAG, (Connection c, Element e) ->
            new LoginMessage(game, e).handle(freeColServer, c));
        register(VacantPlayersMessage.TAG, (Connection c, Element e) ->
            new VacantPlayersMessage(game, e).handle(freeColServer, c));
    }


    // Implement InputHandler

    /**
     * {@inheritDoc}
     */
    protected Element logout(
        @SuppressWarnings("unused") Connection connection,
        @SuppressWarnings("unused") Element element) {
        return null; // Logout is inoperative at this point
    }
}
