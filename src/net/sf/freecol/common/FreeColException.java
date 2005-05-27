
package net.sf.freecol.common;

/**
* The Exception thrown by the FreeCol application.
*/
public final class FreeColException extends Exception {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
    /**
    * The constructor to use.
    * @param message The message that this FreeColException should hold.
    */
    public FreeColException(String message) {
        super(message);
    }
}
