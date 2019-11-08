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

package net.sf.freecol.metaserver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.metaserver.ServerInfo;
import net.sf.freecol.common.networking.Connection;
import static net.sf.freecol.common.util.Utils.*;


/**
 * The {@code MetaRegister} stores information about running servers.
 * Each server has it's own {@link ServerInfo} object.
 */
public final class MetaRegister {

    private static final Logger logger = Logger.getLogger(MetaRegister.class.getName());

    /** Cleanup interval. */
    private static final int REMOVE_DEAD_SERVERS_INTERVAL = 120000;

    /** Removal interval. @see MetaRegister#removeServer */
    private static final int REMOVE_OLDER_THAN = 90000;

    /** The current list of servers. */
    private final List<ServerInfo> items = new ArrayList<>();
    

    /**
     * Create a new MetaRegister.
     */
    public MetaRegister() {
        startCleanupTimer();
    }

    /**
     * Gets the server entry with the diven address and port.
     *
     * @param address The IP-address of the server.
     * @param port The port number of the server.
     * @return The server entry or {@code null} if the given
     *     entry could not be found.
     */
    private ServerInfo getServer(String address, int port) {
        int index = indexOf(address, port);
        if (index >= 0) {
            return items.get(index);
        } else {
            return null;
        }
    }

    /**
     * Gets the index of the server entry with the diven address and port.
     *
     * @param address The IP-address of the server.
     * @param port The port number of the server.
     * @return The index or {@code -1} if the given entry could
     *     not be found.
     */
    private int indexOf(String address, int port) {
        for (int i = 0; i < items.size(); i++) {
            if (address.equals(items.get(i).getAddress())
                && port == items.get(i).getPort()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Updates a given server.
     *
     * @param si The {@code ServerInfo} that should be updated.
     * @param name The name of the server.
     * @param address The IP-address of the server.
     * @param port The port number in which clients may connect.
     * @param slotsAvailable Number of players that may conncet.
     * @param currentlyPlaying Number of players that are currently connected.
     * @param isGameStarted <i>true</i> if the game has started.
     * @param version The version of the server.
     * @param gameState The current state of the game:
     */
    private void updateServer(ServerInfo si, String name, String address,
                              int port, int slotsAvailable,
                              int currentlyPlaying, boolean isGameStarted,
                              String version, int gameState) {
        si.update(name, address, port, slotsAvailable, currentlyPlaying,
                  isGameStarted, version, gameState);
        logger.info("Server updated:" + si.getName());
    }

    /**
     * Start a timer to periodically clean up dead servers.
     */
    private void startCleanupTimer() {
        Timer t = new Timer(true);
        t.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    removeDeadServers();
                } catch (Exception ex) {
                    logger.log(Level.WARNING, "Could not remove servers.", ex);
                }
            }
        }, REMOVE_DEAD_SERVERS_INTERVAL, REMOVE_DEAD_SERVERS_INTERVAL);
    }


    // Public interface

    /**
     * Adds a new server with the given attributes.
     *
     * @param newSi The new {@code ServerInfo} to add.
     */
    public synchronized void addServer(ServerInfo newSi) {
        ServerInfo si = getServer(newSi.getAddress(), newSi.getPort());
        if (si == null) {
            final String identity = newSi.getName()
                + " (" + newSi.getAddress() + ":" + newSi.getPort() + ")";
            // Check connection before adding the server:
            Connection mc = null;
            try {
                mc = new Connection(newSi.getAddress(), newSi.getPort(),
                                    FreeCol.METASERVER_THREAD);
                if (mc == null) {
                    logger.info("Server not found: " + identity);
                } else {
                    items.add(newSi);
                    logger.info("Server added:" + identity);
                    mc.startReceiving();
                }
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "Server fail: " + identity, ioe);
            }
        } else {
            si.update(newSi);
        }
    }

    /**
     * Get the list of servers.
     *
     * @return The list of servers.
     */
    public synchronized List<ServerInfo> getServers() {
        return new ArrayList<ServerInfo>(items);
    }

    /**
     * Removes servers that have not sent an update for some time.
     */
    public synchronized void removeDeadServers() {
        logger.info("Removing dead servers.");

        long time = now() - REMOVE_OLDER_THAN;
        for (int i=0; i<items.size(); i++) {
            if (items.get(i).getLastUpdated() < time) {
                logger.info("Removing: " + items.get(i));
                items.remove(i);
            }
        }
    }

    /**
     * Removes a server from the register.
     *
     * @param address The IP-address of the server to remove.
     * @param port The port number of the server to remove.
     */
    public synchronized void removeServer(String address, int port) {
        int index = indexOf(address, port);
        if (index >= 0) {
            items.remove(index);
            logger.info("Removing server:" + address + ":" + port);
        } else {
            logger.warning("Trying to remove non-existing server:"
                + address + ":" + port);
        }
    }

    /**
     * Updates a server with the given attributes.
     *
     * @param newSi The new {@code ServerInfo}.
     */
    public synchronized void updateServer(ServerInfo newSi) {
        ServerInfo si = getServer(newSi.getAddress(), newSi.getPort());
        if (si == null) {
            addServer(newSi);
        } else {
            si.update(newSi);
        }
    }
}
