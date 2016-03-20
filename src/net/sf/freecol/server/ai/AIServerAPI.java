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

package net.sf.freecol.server.ai;

import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.ServerAPI;

import org.w3c.dom.Element;


/**
 * Implementation of the ServerAPI for an AI without neither attached
 * GUI nor real connection to the server.
 */
public class AIServerAPI extends ServerAPI {

    private static final Logger logger = Logger.getLogger(AIServerAPI.class.getName());

    /** The AI player that owns this wrapper. */
    private AIPlayer owner;


    /**
     * Create the new AI wrapper for the server API.
     *
     * @param owner The <code>AIPlayer</code> attached to this API.
     */
    public AIServerAPI(AIPlayer owner) {
        super();

        this.owner = owner;
    }


    // Implement ServerAPI
    
    /**
     * {@inheritDoc}
     */
    protected void doClientProcessingFor(Element reply) {}

    /**
     * {@inheritDoc}
     */
    public Connection getConnection() {
        return (this.owner == null) ? null : this.owner.getConnection();
    }
}
