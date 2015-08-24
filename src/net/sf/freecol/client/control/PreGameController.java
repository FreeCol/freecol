/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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

import java.awt.Color;

import java.util.logging.Logger;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.NationOptions.NationState;
import net.sf.freecol.common.model.NationType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.option.MapGeneratorOptions;
import net.sf.freecol.common.option.OptionGroup;


/**
 * The controller that will be used before the game starts.
 */
public final class PreGameController {

    private static final Logger logger = Logger.getLogger(PreGameController.class.getName());

    /** The main client. */
    private final FreeColClient freeColClient;

    /** The GUI to display on. */
    private final GUI gui;


    /**
     * The constructor to use.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public PreGameController(FreeColClient freeColClient) {
        this.freeColClient = freeColClient;
        this.gui = freeColClient.getGUI();
    }


    /**
     * Sends a chat message.
     *
     * @param message The text of the message.
     */
    public void chat(String message) {
        freeColClient.askServer().chat(freeColClient.getMyPlayer(), message);
    }

    /**
     * Sets this client to be (or not be) ready to start the game.
     *
     * @param ready Indicates whether or not this client is ready to
     *     start the game.
     */
    public void setReady(boolean ready) {
        freeColClient.getMyPlayer().setReady(ready);
        
        freeColClient.askServer().setReady(ready);
    }

    /**
     * Requests the game to be started.  This will only be successful
     * if all players are ready to start the game.
     */
    public void requestLaunch() {
        if (freeColClient.getGame().allPlayersReadyToLaunch()) {
            gui.showStatusPanel(Messages.message("status.startingGame"));
            freeColClient.askServer().requestLaunch();

        } else {
            gui.showErrorMessage("server.notAllReady");
        }
    }

    /**
     * Sets a nation's state.
     *
     * @param nation The <code>Nation</code> to set.
     * @param state The <code>NationState</code> value to set.
     */
    public void setAvailable(Nation nation, NationState state) {
        freeColClient.getGame().getNationOptions()
            .getNations().put(nation, state);

        freeColClient.askServer().setAvailable(nation, state);
    }

    /**
     * Sets a nation's colour.
     *
     * @param nation The <code>Nation</code> to set the color for.
     * @param color The <code>Color</code> to set.
     */
    public void setColor(Nation nation, Color color) {
        nation.setColor(color);

        freeColClient.askServer().setColor(nation, color);
    }

    /**
     * Sets this client's player's nation.
     *
     * @param nation Which <code>Nation</code> this player wishes to set.
     */
    public void setNation(Nation nation) {
        freeColClient.getMyPlayer().setNation(nation);
        
        freeColClient.askServer().setNation(nation);
    }

    /**
     * Sets this client's player's nation type.
     *
     * @param nationType Which nation type this player wishes to set.
     */
    public void setNationType(NationType nationType) {
        freeColClient.getMyPlayer().changeNationType(nationType);

        freeColClient.askServer().setNationType(nationType);
    }

    /**
     * Starts the game.
     *
     * @return True if the player should continue, false if we are in
     *     a debug run and should be skipping turns.
     */
    public boolean startGame() {
        final Player player = freeColClient.getMyPlayer();
        if (!freeColClient.isHeadless()) {
            gui.closeMainPanel();
            gui.closeMenus();
            gui.closeStatusPanel();
            // Stop the long introduction sound
            freeColClient.getSoundController().playSound(null);
            freeColClient.getSoundController().playSound(
                "sound.intro." + player.getNationId());
        }
        freeColClient.askServer()
            .registerMessageHandler(freeColClient.getInGameInputHandler());

        freeColClient.setInGame(true);
        gui.initializeInGame((Tile)player.getEntryLocation());

        InGameController igc = freeColClient.getInGameController();
        if (freeColClient.currentPlayerIsMyPlayer()) igc.nextActiveUnit();

        gui.setupMouseListeners();

        if (FreeColDebugger.isInDebugMode(FreeColDebugger.DebugMode.MENUS)
            && FreeColDebugger.getDebugRunTurns() > 0) {
            freeColClient.skipTurns(FreeColDebugger.getDebugRunTurns());
            return false;
        }

        if (freeColClient.getGame().getTurn().getNumber() == 1) {
            // force view of tutorial message
            player.addStartGameMessage();
            igc.nextModelMessage();
        }
        return true;
    }

    /**
     * Update the {@link GameOptions} at the server.
     * This method should be called after updating that object.
     */
    public void updateGameOptions() {
        OptionGroup gameOptions = freeColClient.getGame().getGameOptions();
        freeColClient.getGame().getSpecification()
            .clean("update game options (client initiated)");

        freeColClient.askServer().updateGameOptions(gameOptions);
    }

    /**
     * Update the {@link MapGeneratorOptions} at the server.
     * This method should be called after updating that object.
     */
    public void updateMapGeneratorOptions() {
        OptionGroup mgo = freeColClient.getGame().getMapGeneratorOptions();

        freeColClient.askServer().updateMapGeneratorOptions(mgo);
    }
}
