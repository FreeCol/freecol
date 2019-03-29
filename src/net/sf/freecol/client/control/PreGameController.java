/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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

import java.util.List;
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
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Game.LogoutReason;
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
     * Handle an addPlayer message.
     *
     * @param players The {@code Player}s to add.
     */
    public void addPlayerHandler(List<Player> players) {
        getGame().addPlayers(players);
        getFreeColClient().getGUI().refreshPlayersTable();
    }

    /**
     * Sends a chat message.
     *
     * @param message The text of the message.
     */
    public void sendChat(String message) {
        final Player player = getMyPlayer();

        getGUI().displayStartChat(player, message, false);
        askServer().chat(player, message);
    }

    /**
     * Display a chat message.
     *
     * @param player The {@code Player} to chat with.
     * @param message What to say.
     * @param pri If true, the message is private.
     */
    public void chatHandler(Player player, String message, boolean pri) {
        getGUI().displayStartChat(player, message, pri);
    }

    /**
     * Handle an error.
     *
     * @param template A {@code StringTemplate} describing the error.
     * @param message A backup string describing the error.
     */
    public void errorHandler(StringTemplate template, String message) {
        getGUI().showErrorMessage(template, message);
    }            

    /**
     * Handle a player logging out.
     *
     * @param player The {@code Player} that is logging out.
     * @param reason The {@code LogoutReason} why the player left.
     */
    public void logoutHandler(Player player, LogoutReason reason) {
        final FreeColClient freeColClient = getFreeColClient();

        getGame().removePlayer(player);
        getGUI().refreshPlayersTable();
        if (player == getMyPlayer()) {
            freeColClient.getConnectController().logout(reason);
        }
    }

    /**
     * Handle a ready message.
     *
     * @param player The {@code Player} whose readiness changed.
     * @param ready The new readiness state.
     */
    public void readyHandler(Player player, boolean ready) {
        player.setReady(ready);
        getGUI().refreshPlayersTable();
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
     * Handle a setAvailable message.
     *
     * @param nation The {@code Nation} to set.
     * @param nationState The {@code NationState} value to set.
     */
    public void setAvailableHandler(Nation nation, NationState nationState) {
        getGame().getNationOptions().setNationState(nation, nationState);
        getGUI().refreshPlayersTable();
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
     * Handle a setColor message.
     *
     * @param nation The {@code Nation} to set the color for.
     * @param color The {@code Color} to set.
     */
    public void setColorHandler(Nation nation, Color color) {
        nation.setColor(color);
        getGUI().refreshPlayersTable();
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
     * Handle a nation type change.
     *
     * @param nationType Which nation type this player wishes to set.
     */
    public void setNationTypeHandler(NationType nationType) {
        getMyPlayer().changeNationType(nationType);
        getGUI().refreshPlayersTable();
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
     * Handle starting the game.
     *
     * Wait until map is received from server (sometimes a startGame
     * message arrives arrives when map is still null).  Make sure we
     * do this in a new thread so as not to block the input handler
     * from receiving the map!
     */
    public void startGameHandler() {
        final FreeColClient fcc = getFreeColClient();
        new Thread(FreeCol.CLIENT_THREAD + "Starting game") {
                @Override
                public void run() {
                    logger.info("Client starting game");
                    for (int tries = 50; tries >= 0; tries--) {
                        if (fcc.isReadyToStart()) {
                            SwingUtilities.invokeLater(() -> {
                                    startGameInternal();
                                });
                            return;
                        }
                        Utils.delay(200, "StartGame has been interupted.");
                    }
                    final GUI gui = getGUI();
                    String err = (getGame() == null) ? "client.noGame"
                        : "client.noMap";
                    gui.closeMainPanel();
                    gui.closeMenus();
                    gui.closeStatusPanel();
                    gui.showMainPanel(Messages.message(StringTemplate.template(err)));
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
        fcc.restoreGUI(player);
        
        // Clean up autosaves
        final ClientOptions co = getClientOptions();
        if (player.isAdmin() && co.getBoolean(ClientOptions.AUTOSAVE_DELETE)) {
            String logMe = FreeColDirectories
                .removeAutosaves(co.getText(ClientOptions.AUTO_SAVE_PREFIX));
            if (logMe != null) logger.info(logMe);
        }
        
        // Check for debug skipping
        if (FreeColDebugger.isInDebugMode(FreeColDebugger.DebugMode.MENUS)
            && FreeColDebugger.getDebugRunTurns() > 0) {
            fcc.skipTurns(FreeColDebugger.getDebugRunTurns());
            return false;
        }
        
        // Tutorial message if needed
        if (getGame().getTurn().getNumber() == 1) {
            player.addStartGameMessage();
        }
        igc().nextModelMessage();
        return true;
    }

    /**
     * Handles an update.
     *
     * @param objects The {@code FreeColObject}s to update.
     */
    public void updateHandler(List<FreeColObject> objects) {
        final Game game = getGame();
        
        for (FreeColObject fco : objects) {
            if (fco instanceof Game) {
                if (game.preGameUpdate((Game)fco)) {
                    final FreeColClient fcc = getFreeColClient();
                    fcc.addSpecificationActions(((Game)fco).getSpecification());
                } else {
                    logger.warning("Pre-game copy-in failed: " + fco.getId());
                }
            } else {
                logger.warning("Game node expected: " + fco.getId());
            }
        }
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
     * Handle new game options.
     *
     * @param gameOptions The {@code OptionGroup} containing the game options.
     */
    public void updateGameOptionsHandler(OptionGroup gameOptions) {
        if (!getSpecification().mergeGameOptions(gameOptions, "client")) {
            logger.warning("Game option update failed");
        }
    }

    /**
     * Update the {@link MapGeneratorOptions} at the server.
     * This method should be called after updating that object.
     */
    public void updateMapGeneratorOptions() {
        OptionGroup mgo = getGame().getMapGeneratorOptions();

        askServer().updateMapGeneratorOptions(mgo);
    }

    /**
     * Handle new map options.
     *
     * @param mapOptions An {@code OptionGroup} containing the map options.
     */
    public void updateMapGeneratorOptionsHandler(OptionGroup mapOptions) {
        if (!getSpecification().mergeMapGeneratorOptions(mapOptions, "client")) {
            logger.warning("Map generator option update failed");
        }
    }        
}
