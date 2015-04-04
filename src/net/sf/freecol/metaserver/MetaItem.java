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

package net.sf.freecol.metaserver;

import java.util.logging.Logger;

import net.sf.freecol.common.ServerInfo;


/**
 * This object stores information about a single running server.
 */
public class MetaItem extends ServerInfo {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(MetaItem.class.getName());

    private long lastUpdated;


    /**
     * Creates a new object with the given information.
     *
     * @param name The name of the server.
     * @param address The IP-address of the server.
     * @param port The port number in which clients may connect.
     * @param slotsAvailable Number of players that may connect.
     * @param currentlyPlaying Number of players that are currently connected.
     * @param isGameStarted <i>true</i> if the game has started.
     * @param version The version the server is running.
     * @param gameState The current state of the game.
     */
    public MetaItem(String name, String address, int port, int slotsAvailable,
                    int currentlyPlaying, boolean isGameStarted, String version,
                    int gameState) {
        super();
        update(name, address, port, slotsAvailable, currentlyPlaying, isGameStarted, version, gameState);
    }


    /**
     * Updates this object with the given information.
     *
     * @param name The name of the server.
     * @param address The IP-address of the server.
     * @param port The port number in which clients may connect.
     * @param slotsAvailable Number of players that may connect.
     * @param currentlyPlaying Number of players that are currently connected.
     * @param isGameStarted <i>true</i> if the game has started.
     */
    @Override
    public void update(String name, String address, int port, int slotsAvailable,
                       int currentlyPlaying, boolean isGameStarted, String version,
                       int gameState) {
        super.update(name, address, port, slotsAvailable, currentlyPlaying, isGameStarted, version, gameState);
        lastUpdated = System.currentTimeMillis();
    }
    
    
    /**
     * Gets the last time this object was updated.
     *
     * @return The timestamp of the last time this object was updated,
     *     as returned by <code>System.currentTimeMillis()</code>.
     */
    public long getLastUpdated() {
        return lastUpdated;
    }
}
