
package net.sf.freecol.server.control;

import java.util.logging.Logger;
import org.w3c.dom.Element;

import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.MessageHandler;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


/**
* Handles the network messages.
* @see Controller
*/
public abstract class InputHandler implements MessageHandler {
    private static Logger logger = Logger.getLogger(InputHandler.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2004 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
    private FreeColServer freeColServer;


    /**
    * The constructor to use.
    * @param freeColServer The main server object.
    */
    public InputHandler(FreeColServer freeColServer) {
        this.freeColServer = freeColServer;
    }


    /**
    * Returns the main freecol server object.
    * @return The main freecol server object.
    */
    protected FreeColServer getFreeColServer() {
        return freeColServer;
    }


    /**
    * Deals with incoming messages that have just been received.
    *
    * @param connection The <code>Connection</code> the message was received on.
    * @param element The root element of the message.
    */
    public abstract Element handle(Connection connection, Element element);


    /**
    * Handles a "logout"-message.
    *
    * @param connection The <code>Connection</code> the message was received on.
    * @param logoutElement The element (root element in a DOM-parsed XML tree) that
    *                holds all the information.
    */
    abstract protected Element logout(Connection connection, Element logoutElement);

    
    /**
    * Handles a "disconnect"-message.
    *
    * @param connection The <code>Connection</code> the message was received on.
    * @param disconnectElement The element (root element in a DOM-parsed XML tree) that
    *                holds all the information.
    */
    protected Element disconnect(Connection connection, Element disconnectElement) {
        // The player should be logged out by now, but just in case:

        ServerPlayer player = freeColServer.getPlayer(connection);

        logger.info("Disconnection by: " + connection + ((player != null) ? " (" + player.getName() + ") " : ""));

        if (player == null || player.isConnected()) {
            logout(connection, null);
        }

        return null;
    }
}
