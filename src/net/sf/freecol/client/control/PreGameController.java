/**
 *  Copyright (C) 2002-2017   The FreeCol Team
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

import javax.swing.SwingUtilities;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.FreeColClientHolder;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.NationOptions.NationState;
import net.sf.freecol.common.model.NationType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.option.GameOptions;
import net.sf.freecol.common.option.MapGeneratorOptions;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.common.util.Utils;


/**
 * The controller that will be used before the game starts.
 */
public final class PreGameController extends FreeColClientHolder {

    private static final Logger logger = Logger.getLogger(PreGameController.class.getName());


    /**
     * The constructor to use.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    public PreGameController(FreeColClient freeColClient) {
        super(freeColClient);
    }


    /**
     * Sends a chat message.
     *
     * @param message The text of the message.
     */
    public void chat(String message) {
        askServer().chat(getMyPlayer(), message);
    }

    /**
     * Requests the game to be started.  This will only be successful
     * if all players are ready to start the game.
     */
    public void requestLaunch() {
        if (getGame().allPlayersReadyToLaunch()) {
            getGUI().showStatusPanel(Messages.message("status.startingGame"));
            askServer().requestLaunch();

        } else {
            getGUI().showErrorMessage(StringTemplate
                .template("server.notAllReady"));
        }
    }

    /**
     * Sets a nation's state.
     *
     * @param nation The {@code Nation} to set.
     * @param state The {@code NationState} value to set.
     */
    public void setAvailable(Nation nation, NationState state) {
        getGame().getNationOptions().getNations().put(nation, state);

        askServer().setAvailable(nation, state);
    }

    /**
     * Sets a nation's colour.
     *
     * @param nation The {@code Nation} to set the color for.
     * @param color The {@code Color} to set.
     */
    public void setColor(Nation nation, Color color) {
        nation.setColor(color);

        askServer().setColor(nation, color);
    }

    /**
     * Sets this client's player's nation.
     *
     * @param nation Which {@code Nation} this player wishes to set.
     */
    public void setNation(Nation nation) {
        getMyPlayer().setNation(nation);
        
        askServer().setNation(nation);
    }

    /**
     * Sets this client's player's nation type.
     *
     * @param nationType Which nation type this player wishes to set.
     */
    public void setNationType(NationType nationType) {
        getMyPlayer().changeNationType(nationType);

        askServer().setNationType(nationType);
    }

    /**
     * Sets this client to be (or not be) ready to start the game.
     *
     * @param ready Indicates whether or not this client is ready to
     *     start the game.
     */
    public void setReady(boolean ready) {
        getMyPlayer().setReady(ready);
        
        askServer().setReady(ready);
    }

    /**
     * Start the game.
     */
    public void startGame() {
        new Thread(FreeCol.CLIENT_THREAD + "Starting game") {
                @Override
                public void run() {
                    // Wait until map is received from server
                    // (sometimes a startGame message arrives arrives
                    // when map is still null).  Make sure we do this
                    // in a new thread so as not to block the input handler
                    // from receiving the map!
                    Game game;
                    for (;;) {
                        game = getGame();
                        if (game != null && game.getMap() != null) break;
                        Utils.delay(200, "StartGame has been interupted.");
                    }

                    SwingUtilities.invokeLater(() -> {
                            startGameInternal();
                        });
                }
        }.start();
    }
    
    /**
     * Internal start of the game.
     *
     * @return True if the player should continue, false if we are in
     *     a debug run and should be skipping turns.
     */
    private boolean startGameInternal() {
        final FreeColClient fcc = getFreeColClient();
        final Player player = getMyPlayer();
        final GUI gui = getGUI();
        // Clear the main display
        gui.closeMainPanel();
        gui.closeMenus();
        gui.closeStatusPanel();
        
        // Stop the long introduction sound and play the player intro
        getSoundController().playSound(null);
        getSoundController().playSound("sound.intro." + player.getNationId());
        
        // Switch to InGame mode
        fcc.changeClientState(true);
        gui.initializeInGame();
        
        // Clean up autosaves
        final ClientOptions co = getClientOptions();
        if (player.isAdmin() && co.getBoolean(ClientOptions.AUTOSAVE_DELETE)) {
            String logMe = FreeColDirectories
                .removeAutosaves(co.getText(ClientOptions.AUTO_SAVE_PREFIX));
            if (logMe != null) logger.info(logMe);
        }
        
        // Sort out the unit initialization
        final Game game = getGame();
        fcc.restoreGUI(player);
        game.setInitialActiveUnitId(null);
        
        // Check for debug skipping
        if (FreeColDebugger.isInDebugMode(FreeColDebugger.DebugMode.MENUS)
            && FreeColDebugger.getDebugRunTurns() > 0) {
            fcc.skipTurns(FreeColDebugger.getDebugRunTurns());
            return false;
        }
        
        // Tutorial message if needed
        if (game.getTurn().getNumber() == 1) {
            player.addStartGameMessage();
        }
        igc().nextModelMessage();
        return true;
    }

    /**
     * Update the {@link GameOptions} at the server.
     * This method should be called after updating that object.
     */
    public void updateGameOptions() {
        OptionGroup gameOptions = getGame().getGameOptions();
        getSpecification().clean("update game options (client initiated)");

        askServer().updateGameOptions(gameOptions);
    }

    /**
     * Update the {@link MapGeneratorOptions} at the server.
     * This method should be called after updating that object.
     */
    public void updateMapGeneratorOptions() {
        OptionGroup mgo = getGame().getMapGeneratorOptions();

        askServer().updateMapGeneratorOptions(mgo);
    }
}
