package net.sf.freecol.server.control;

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.PseudoRandom;
import net.sf.freecol.server.FreeColServer;

/**
 * This base class provides thread-safe access to a
 * {@link net.sf.freecol.server.FreeColServer} for several subclasses. 
 */
public class FreeColServerHolder {
    public static final String COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String REVISION = "$Revision$";
    private final FreeColServer freeColServer;

    /**
     * Constructor.
     * 
     * @param server The initial value for the server.
     */
    protected FreeColServerHolder(FreeColServer server) {
        this.freeColServer = server;
    }

    /**
     * Returns the main server object.
     * 
     * @return The main server object.
     */
    protected FreeColServer getFreeColServer() {
        return freeColServer;
    }

    /**
     * Returns the Game.
     *
     * @return a <code>Game</code> value
     */
    protected Game getGame() {
        return freeColServer.getGame();
    }

    /**
     * Get a random number generator.
     * 
     * @return shared random number generator.
     * @throws NullPointerException if the server reference is null.
     */
    protected PseudoRandom getPseudoRandom() {        
        return getFreeColServer().getPseudoRandom();
    }
}
