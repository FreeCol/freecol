
package net.sf.freecol.metaserver;

import java.util.logging.Logger;
import net.sf.freecol.common.ServerInfo;


/**
* This object stores information about a single running server.
*/
public class MetaItem extends ServerInfo {
    private static Logger logger = Logger.getLogger(MetaItem.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    

    private long lastUpdated;


    /**
    * Creates a new object with the given information.
    *
    * @param name The name of the server.
    * @param address The IP-address of the server.
    * @param port The port number in which clients may connect.
    * @param slotsAvailable Number of players that may conncet.
    * @param currentlyPlaying Number of players that are currently connected.
    * @param isGameStarted <i>true</i> if the game has started.
    */
    public MetaItem(String name, String address, int port, int slotsAvailable, int currentlyPlaying, boolean isGameStarted) {
        super();
        update(name, address, port, slotsAvailable, currentlyPlaying, isGameStarted);
    }


    /**
    * Updates this object with the given information.
    *
    * @param name The name of the server.
    * @param address The IP-address of the server.
    * @param port The port number in which clients may connect.
    * @param slotsAvailable Number of players that may conncet.
    * @param currentlyPlaying Number of players that are currently connected.
    * @param isGameStarted <i>true</i> if the game has started.
    */
    public void update(String name, String address, int port, int slotsAvailable, int currentlyPlaying, boolean isGameStarted) {
        super.update(name, address, port, slotsAvailable, currentlyPlaying, isGameStarted);
        lastUpdated = System.currentTimeMillis();
    }
    
    
    /**
    * Returns the last time this object was updated.
    * @return The timestamp of the last time this object was updated,
    *         as returned by <code>System.currentTimeMillis()</code>.
    */
    public long getLastUpdated() {
        return lastUpdated;
    }
}
