
package net.sf.freecol.client.control;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.MessageHandler;

import java.util.logging.Logger;

import org.w3c.dom.Element;

public abstract class InputHandler implements MessageHandler {
    private static final Logger logger = Logger.getLogger(InputHandler.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2004 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";


    private FreeColClient freeColClient;


    /**
    * The constructor to use.
    * @param freeColClient The main freecol client object.
    */
    public InputHandler(FreeColClient freeColClient) {
        this.freeColClient = freeColClient;
    }


    /**
    * Returns the main freecol client object.
    * @return The main freecol client object.
    */
    protected FreeColClient getFreeColClient() {
        return freeColClient;
    }


    /**
    * Deals with incoming messages that have just been received.
    *
    * @param connection The <code>Connection</code> the message was received on.
    * @param element The root element of the message.
    */
    public abstract Element handle(Connection connection, Element element);


    /**
    * Handles a "disconnect"-message.
    *
    * @param disconnectElement The element (root element in a DOM-parsed XML tree) that
    *                holds all the information.
    */
    protected Element disconnect(Element disconnectElement) {
        getFreeColClient().getConnectController().quitGame(false, false);
        
        if (freeColClient.getFreeColServer() == null) {
            getFreeColClient().getCanvas().returnToTitle();
        } else {
            getFreeColClient().getCanvas().removeInGameComponents();
        }

        return null;
    }
}
