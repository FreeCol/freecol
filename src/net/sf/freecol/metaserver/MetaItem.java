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

import net.sf.freecol.common.ServerInfo;


/**
 * This object stores information about a single running server.
 */
public class MetaItem extends ServerInfo {

    /** Timestamp for last update. */
    private long lastUpdated;


    /**
     * {@inheritDoc}
     */
    @Override
    public void update(String name, String address, int port,
                       int slotsAvailable, int currentlyPlaying,
                       boolean isGameStarted, String version, int gameState) {
        super.update(name, address, port, slotsAvailable, currentlyPlaying,
                     isGameStarted, version, gameState);
        this.lastUpdated = System.currentTimeMillis();
    }
    
    /**
     * Gets the last time this object was updated.
     *
     * @return The timestamp of the last time this object was updated,
     *     as returned by <code>System.currentTimeMillis()</code>.
     */
    public long getLastUpdated() {
        return this.lastUpdated;
    }
}
