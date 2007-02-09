package net.sf.freecol.server.control;

import java.util.logging.Logger;
import net.sf.freecol.common.PseudoRandom;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.networking.Server;

/**
 * The control object that is responsible for making changes to the internal
 * model and for communicating with the clients.
 */
public abstract class Controller extends FreeColServerHolder {
    private static final Logger logger = Logger.getLogger(Controller.class.getName());

    public static final String COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";


    /**
     * The constructor to use.
     * 
     * @param freeColServer The main server object.
     */
    public Controller(FreeColServer freeColServer) {
        super(freeColServer);
    }

    /**
     * Shut down the server (which sends a message to each client).
     */
    public void shutdown() {
        Server server = getFreeColServer().getServer();
        if (server != null) {
            server.shutdown();
        } else {
            logger.warning("Server object is null while trying to shut down server.");
        }
    }
}
