
package net.sf.freecol.common.networking;

import net.sf.freecol.common.FreeColException;
import org.w3c.dom.Element;



/**
* Handles complete incoming messages.
*/
public interface MessageHandler {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2004 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
    /**
    * Handles the main element of an XML message.
    *
    * @param connection The connection the message came from.
    * @param element The element to handle.
    * @return The reply (if any) or <i>null</i>.
    * @throws FreeColException
    */
    public Element handle(Connection connection, Element element) throws FreeColException;

}
