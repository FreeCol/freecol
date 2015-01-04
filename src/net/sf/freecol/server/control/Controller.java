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

import java.util.logging.Logger;

import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.networking.Server;


/**
 * The control object that is responsible for making changes to the internal
 * model and for communicating with the clients.
 */
public abstract class Controller extends FreeColServerHolder {

    private static final Logger logger = Logger.getLogger(Controller.class.getName());


    /**
     * The constructor to use.
     *
     * @param freeColServer The main server object.
     */
    public Controller(FreeColServer freeColServer) {
        super(freeColServer);
    }


    /**
     * Shut down the server (which sends a message to each client).
     */
    public void shutdown() {
        Server server = getFreeColServer().getServer();
        if (server != null) {
            server.shutdown();
        } else {
            logger.warning("Tried to shut down a null server.");
        }
    }
}
