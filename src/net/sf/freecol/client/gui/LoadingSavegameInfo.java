/**
 *  Copyright (C) 2002-2022   The FreeCol Team
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

package net.sf.freecol.client.gui;

import java.net.InetAddress;

/**
 * Used for transferring data for the savegame to be loaded.
 */
public class LoadingSavegameInfo {

    private final boolean singlePlayer;
    private final InetAddress address;
    private final int port;
    private final String serverName;
    private final boolean publicServer;

    public LoadingSavegameInfo(boolean singlePlayer, InetAddress address, int port, String serverName, boolean publicServer) {
        this.singlePlayer = singlePlayer;
        this.address = address;
        this.port = port;
        this.serverName = serverName;
        this.publicServer = publicServer;
    }

    /**
     * Is a single player game selected?
     *
     * @return True if single player is selected.
     */
    public boolean isSinglePlayer() {
        return singlePlayer;
    }
    
    /**
     * Get the selected address;
     *
     * @return The {@code InetAddress}.
     */
    public InetAddress getAddress() {
        return address;
    }

    /**
     * Get the selected port number.
     *
     * @return The port number.
     */
    public int getPort() {
        return port;
    }

    /**
     * Get the specified server name.
     *
     * @return The server name.
     */
    public String getServerName() {
        return serverName;
    }

    /**
     * Is this a public server?
     *
     * @return True if this is a public server.
     */
    public boolean isPublicServer() {
        return publicServer;
    }
}
