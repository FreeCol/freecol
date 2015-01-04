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

package net.sf.freecol.server.control;

import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerGame;


/**
 * This base class provides thread-safe access to a
 * {@link net.sf.freecol.server.FreeColServer} for several subclasses. 
 */
public class FreeColServerHolder {

    /** The main server object. */
    private final FreeColServer freeColServer;


    /**
     * Constructor.
     * 
     * @param server The initial value for the server.
     */
    protected FreeColServerHolder(FreeColServer server) {
        this.freeColServer = server;
    }

    /**
     * Returns the main server object.
     * 
     * @return The main server object.
     */
    protected FreeColServer getFreeColServer() {
        return freeColServer;
    }

    /**
     * Returns the Game.
     *
     * @return a <code>Game</code> value
     */
    protected ServerGame getGame() {
        return freeColServer.getGame();
    }
}
