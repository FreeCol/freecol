package net.sf.freecol.common.networking;

/**
 * Thrown if there is no route to a server.
 */
public class NoRouteToServerException extends Exception {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
    /**
     * The constructor to use.
     * @param message The message.
     */
    public NoRouteToServerException() {
        super();
    }
}
