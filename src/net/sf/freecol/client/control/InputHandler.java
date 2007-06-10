
package net.sf.freecol.client.control;

import java.util.logging.Logger;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.MessageHandler;

import org.w3c.dom.Element;

/**
 * Provides common methods for input handlers.
 */
public abstract class InputHandler implements MessageHandler {
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(InputHandler.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";


    private final FreeColClient freeColClient;

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
     * Returns the Game.
     *
     * @return a <code>Game</code> value
     */
    protected Game getGame() {
        return freeColClient.getGame();
    }


    /**
    * Deals with incoming messages that have just been received.
    *
    * @param connection The <code>Connection</code> the message was received on.
    * @param element The root element of the message.
    * @return The reply.
    */
    public abstract Element handle(Connection connection, Element element);


    /**
    * Handles a "disconnect"-message.
    *
    * @param disconnectElement The element (root element in a DOM-parsed XML tree) that
    *                holds all the information.
    * @return <code>null</code>.
    */
    protected Element disconnect(Element disconnectElement) {
        // Updating the GUI should always be done in the EDT:
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (freeColClient.getCanvas().containsInGameComponents()) {
                    if (freeColClient.getFreeColServer() == null) {
                        freeColClient.getCanvas().returnToTitle();
                    } else {
                        freeColClient.getCanvas().removeInGameComponents();
                    }
                }
            }
        });

        return null;
    }
}
