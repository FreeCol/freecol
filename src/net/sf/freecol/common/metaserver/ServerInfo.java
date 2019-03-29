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

package net.sf.freecol.common.metaserver;

import net.sf.freecol.common.util.Utils;


/**
 * Container class for information about a single server as retrieved
 * from the meta-server.
 */
public class ServerInfo {

    public static final String TAG = "serverInfo";

    private String name;
    private String address;
    private int port;
    private int currentlyPlaying;
    private int slotsAvailable;
    private boolean isGameStarted;
    private String version;
    private int gameState;

    /** Timestamp used by the meta-server. */
    private long lastUpdated = -1L;
    

    /**
     * Create a new server infomation holder.
     *
     * @param name The name of the server.
     * @param address The IP-address of the server.
     * @param port The port number in which clients may connect.
     * @param slotsAvailable Number of players that may conncet.
     * @param currentlyPlaying Number of players that are currently connected.
     * @param isGameStarted True if the game has started.
     * @param version The version of the server.
     * @param gameState The current state of the game.
     */
    public ServerInfo(String name, String address, int port,
                      int slotsAvailable, int currentlyPlaying,
                      boolean isGameStarted, String version, int gameState) {
        this.update(name, address, port, slotsAvailable, currentlyPlaying,
                    isGameStarted, version, gameState);
    }


    /**
     * Get the name of the server that is beeing represented 
     * by this object.
     * 
     * @return The server name.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Get the IP address.
     *
     * @return The IP address of the server.
     */
    public String getAddress() {
        return this.address;
    }

    /**
     * Get the TCP port in which clients may connect.
     *
     * @return The server port.
     */
    public int getPort() {
        return this.port;
    }

    /**
     * Get the number of currently active (connected and not dead)
     * players.
     *
     * @return The number of live players.
     */
    public int getCurrentlyPlaying() {
        return this.currentlyPlaying;
    }

    /**
     * Get the number of players that may connect.
     *
     * @return The number of slots available on the server.
     */
    public int getSlotsAvailable() {
        return this.slotsAvailable;
    }

    /**
     * Is the game started?
     *
     * @return True if the game is started.
     */
    public boolean getIsGameStarted() {
        return this.isGameStarted;
    }
    
    /**
     * Get the FreeCol version of the server.
     *
     * @return The version.
     * @see net.sf.freecol.FreeCol#getVersion
     */
    public String getVersion() {
        return this.version;
    }

    /**
     * Gets the current state of the game.
     * 
     * @return The current state of the game.
     */
    public int getGameState() {
        return this.gameState;
    }

    /**
     * Get the last update timestamp (may be negative if unset).
     *
     * @return The update timestamp.
     */
    public long getLastUpdated() {
        return this.lastUpdated;
    }

    /**
     * Set the connection information.
     *
     * @param name The name of the connection.
     * @param address The IP-address of the server.
     * @param port The port number to connect to.
     */
    public void setConnection(String name, String address, int port) {
        if (this.name == null) this.name = name;
        this.address = address;
        this.port = port;
    }

    /**
     * Update this server.
     *
     * @param name The name of the server.
     * @param address The IP-address of the server.
     * @param port The port number in which clients may connect.
     * @param slotsAvailable Number of players that may conncet.
     * @param currentlyPlaying Number of players that are currently connected.
     * @param isGameStarted True if the game has started.
     * @param version The version of the server.
     * @param gameState The current state of the game.
     */
    public final void update(String name, String address, int port,
                             int slotsAvailable, int currentlyPlaying,
                             boolean isGameStarted, String version, int gameState) {
        this.name = name;
        this.address = address;
        this.port = port;
        this.slotsAvailable = slotsAvailable;
        this.currentlyPlaying = currentlyPlaying;
        this.isGameStarted = isGameStarted;
        this.version = version;
        this.gameState = gameState;
        this.lastUpdated = Utils.now();
    }

    /**
     * Update this server.
     *
     * @param si The new {@code ServerInfo} to update with.
     */
    public final void update(ServerInfo si) {
        update(si.getName(), si.getAddress(), si.getPort(),
               si.getSlotsAvailable(), si.getCurrentlyPlaying(),
               si.getIsGameStarted(), si.getVersion(), si.getGameState());
    }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof ServerInfo) {
            ServerInfo other = (ServerInfo)o;
            return Utils.equals(this.name, other.name)
                && Utils.equals(this.address, other.address)
                && this.port == other.port
                && this.currentlyPlaying == other.currentlyPlaying
                && this.slotsAvailable == other.slotsAvailable
                && this.isGameStarted == other.isGameStarted
                && Utils.equals(this.version, other.version)
                && this.gameState == other.gameState;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 31 * hash + Utils.hashCode(this.name);
        hash = 31 * hash + Utils.hashCode(this.address);
        hash = 31 * hash + this.port;
        hash = 31 * hash + this.currentlyPlaying;
        hash = 31 * hash + this.slotsAvailable;
        hash = 31 * hash + ((this.isGameStarted) ? 1 : 0);
        hash = 31 * hash + Utils.hashCode(this.version);
        hash = 31 * hash + this.gameState;
        return hash;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return name + " (" + this.address + ":" + this.port + ") "
            + this.currentlyPlaying + ", " + this.slotsAvailable
            + ", " + this.isGameStarted + ", " + this.version
            + ", " + this.gameState + ", " + this.lastUpdated;
    }
}
