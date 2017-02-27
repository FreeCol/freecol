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

package net.sf.freecol.common.metaserver;


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
    public void update(String name, String address, int port,
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
        this.lastUpdated = System.currentTimeMillis();
    }

    /**
     * Update this server.
     *
     * @param si The new {@code ServerInfo} to update with.
     */
    public void update(ServerInfo si) {
        update(si.getName(), si.getAddress(), si.getPort(),
               si.getSlotsAvailable(), si.getCurrentlyPlaying(),
               si.getIsGameStarted(), si.getVersion(), si.getGameState());
    }


    // Override Object

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
