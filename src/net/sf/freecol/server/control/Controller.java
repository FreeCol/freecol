
package net.sf.freecol.server.control;

import java.util.logging.Logger;

import net.sf.freecol.common.networking.Message;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.networking.Server;

import org.w3c.dom.Element;


/**
* The control object that is responsible for making changes to the
* internal model and for communicating with the clients.
*/
public abstract class Controller {
    private static final Logger logger = Logger.getLogger(Controller.class.getName());


    private FreeColServer freeColServer;


    /**
    * The constructor to use.
    * @param freeColServer The main server object.
    */
    public Controller(FreeColServer freeColServer) {
        this.freeColServer = freeColServer;
    }


    /**
    * Returns the main server object.
    * @return The main server object.
    */
    protected FreeColServer getFreeColServer() {
        return freeColServer;
    }


    /**
    * Sends a logout message to each client and shuts the server down.
    */
    public void shutdown() {
        Server server = freeColServer.getServer();
        if (server != null) {
            Element disconnectMessage = Message.createNewRootElement("disconnect");
            disconnectMessage.setAttribute("reason", "The server is going down.");
            server.sendToAll(disconnectMessage);

            server.shutdown();
            freeColServer = null;
        }
        else {
            logger.warning("Server object is null while trying to shut down server.");

            freeColServer = null;
        }
    }
}
