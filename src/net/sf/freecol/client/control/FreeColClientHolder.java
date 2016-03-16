/**
 *  Copyright (C) 2002-2016   The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.client.control;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.control.ConnectController;
import net.sf.freecol.client.control.InGameController;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.networking.UserServerAPI;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.networking.ServerAPI;
import net.sf.freecol.server.FreeColServer;


/**
 * This base class provides access to a
 * {@link net.sf.freecol.client.FreeColClient} for several subclasses.
 */
public class FreeColClientHolder {

    /** The main client object. */
    private final FreeColClient freeColClient;


    /**
     * Simple constructor.
     * 
     * @param freeColClient The <code>FreeColClient</code> to hold.
     */
    protected FreeColClientHolder(FreeColClient freeColClient) {
        this.freeColClient = freeColClient;
    }


    /**
     * Meaningfully named access to the server API.
     *
     * @return The <code>ServerAPI</code>.
     */
    public UserServerAPI askServer() {
        return this.freeColClient.askServer();
    }

    /**
     * Check if the current player is the client player.
     *
     * @return True if the client player is current.
     */
    protected boolean currentPlayerIsMyPlayer() {
        return this.freeColClient.currentPlayerIsMyPlayer();
    }

    /**
     * Get the client options.
     *
     * @return The <code>ClientOptions</code> held by the client.
     */
    protected ClientOptions getClientOptions() {
        return this.freeColClient.getClientOptions();
    }

    /**
     * Get the connect controller.
     *
     * @return The <code>ConnectController</code> held by the client.
     */
    protected ConnectController getConnectController() {
        return this.freeColClient.getConnectController();
    }

    /**
     * Get the main client object.
     * 
     * @return The <code>FreeColClient</code> held by this object.
     */
    protected FreeColClient getFreeColClient() {
        return this.freeColClient;
    }

    /**
     * Get the server.
     *
     * @return The <code>FreeColServer</code> held by the client.
     */
    protected FreeColServer getFreeColServer() {
        return this.freeColClient.getFreeColServer();
    }

    /**
     * Get the game.
     *
     * @return The <code>Game</code> held by the client.
     */
    protected Game getGame() {
        return this.freeColClient.getGame();
    }

    /**
     * Get the GUI.
     *
     * @return The <code>GUI</code> held by the client.
     */
    protected GUI getGUI() {
        return this.freeColClient.getGUI();
    }

    /**
     * Get the client player.
     *
     * @return The <code>Player</code> associated with the client.
     */
    protected Player getMyPlayer() {
        return this.freeColClient.getMyPlayer();
    }

    /**
     * Gets the controller for the sound.
     *
     * @return The sound controller, if any.
     */
    public SoundController getSoundController() {
        return this.freeColClient.getSoundController();
    }

    /**
     * Get the specification.
     *
     * @return The <code>Specification</code> held by the game.
     */
    protected Specification getSpecification() {
        Game game = getGame();
        return (game == null) ? null : game.getSpecification();
    }

    /**
     * Get the in game controller.
     *
     * @return The <code>InGameController</code> for the client.
     */
    protected InGameController igc() {
        return this.freeColClient.getInGameController();
    }
}
