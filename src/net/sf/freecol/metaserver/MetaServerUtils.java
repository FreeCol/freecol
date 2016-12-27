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

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.DOMMessage;
import net.sf.freecol.common.networking.ServerListMessage;
import net.sf.freecol.server.FreeColServer;


/**
 * Wrapper class to contain utility functions used be the FreeColServer to
 * talk to the meta-server.
 */
public class MetaServerUtils {

    private static final Logger logger = Logger.getLogger(MetaServerUtils.class.getName());

    /** Error message type. */
    public static final String NO_ROUTE_TO_SERVER = "noRouteToServer";
    
    /** Client timer update interval. */
    private static final int UPDATE_INTERVAL = 60000;

    /**
     * Utility to get a connection to the meta-server.
     *
     * @return A {@code Connection}, or null on failure.
     */
    public static Connection getMetaServerConnection() {
        Connection mc = null;
        try {
            return new Connection(FreeCol.META_SERVER_ADDRESS,
                                  FreeCol.META_SERVER_PORT, null,
                                  FreeCol.SERVER_THREAD);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not connect to meta-server: ",
                FreeCol.META_SERVER_ADDRESS + ":" + FreeCol.META_SERVER_PORT);
        }
        return null;
    }

    /**
     * Gets a list of servers from the meta server.
     *
     * Note: synchronous comms.
     *
     * @return A list of {@link ServerInfo} objects, or null on error.
     */
    public static List<ServerInfo> getServerList() {
        Connection mc = getMetaServerConnection();
        if (mc == null) return null;

        try {
            DOMMessage message = mc.ask(null, new ServerListMessage());
            if (message instanceof ServerListMessage) {
                return ((ServerListMessage)message).getServers();
            }
        } finally {
            mc.close();
        }
        return null;
    }

    /**
     * Utility to start an update timer for a given server.
     *
     * @param freeColServer The {@code FreeColServer} to send update messages
     *     to the meta-server.
     * @return The {@code Timer} that was started, so that it can be cancelled
     *     if no longer needed.
     */
    public static Timer startUpdateTimer(FreeColServer freeColServer) {
        // This update is really a "Hi! I am still here!"-message,
        // since an additional update should be sent when a new
        // player is added to/removed from this server etc.
        Timer t = new Timer(true);
        t.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if (!freeColServer.updateMetaServer(false)) cancel();
                }
            }, UPDATE_INTERVAL, UPDATE_INTERVAL);
        return t;
    }
}
