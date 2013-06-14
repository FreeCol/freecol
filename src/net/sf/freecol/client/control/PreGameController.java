/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.NationOptions.NationState;
import net.sf.freecol.common.model.NationType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
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
            gui.errorMessage("server.notAllReady");
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
        freeColClient.getMyPlayer().getNation().setColor(color);
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
     * Sends the {@link GameOptions} to the server.
     * This method should be called after updating that object.
     */
    public void sendGameOptions() {
        Specification spec = freeColClient.getGame().getSpecification();
        OptionGroup gameOptions = spec.getGameOptions();
        spec.clean("update game options (client initiated)");

        freeColClient.askServer().updateGameOptions(gameOptions);

    }

    /**
     * Sends the {@link MapGeneratorOptions} to the server.
     * This method should be called after updating that object.
     */
    public void sendMapGeneratorOptions() {
        Specification spec = freeColClient.getGame().getSpecification();
        OptionGroup mapOptions = spec.getMapGeneratorOptions();

        freeColClient.askServer().updateMapGeneratorOption(mapOptions);
    }

    /**
     * Starts the game.
     */
    public void startGame() {
        Player myPlayer = freeColClient.getMyPlayer();
        if (!freeColClient.isHeadless()) {
            gui.closeMainPanel();
            gui.closeMenus();
            gui.closeStatusPanel();
            gui.playSound(null); // Stop the long introduction sound
            gui.playSound("sound.intro." + myPlayer.getNationId());
        }
        freeColClient.askServer()
            .registerMessageHandler(freeColClient.getInGameInputHandler());

        if (!freeColClient.isHeadless()) {
            freeColClient.setInGame(true);
            gui.setupInGameMenuBar();
        }

        InGameController igc = freeColClient.getInGameController();
        gui.setSelectedTile((Tile) myPlayer.getEntryLocation(), false);
        if (freeColClient.currentPlayerIsMyPlayer()) {
            igc.nextActiveUnit();
        }

        gui.setUpMouseListenersForCanvas();

        if (FreeColDebugger.isInDebugMode(FreeColDebugger.DebugMode.MENUS)
            && FreeColDebugger.getDebugRunTurns() > 0) {
            freeColClient.skipTurns(FreeColDebugger.getDebugRunTurns());
        } else if (freeColClient.getGame().getTurn().getNumber() == 1) {
            ModelMessage message
                = new ModelMessage(ModelMessage.MessageType.TUTORIAL,
                                   "tutorial.startGame", myPlayer);
            String direction = myPlayer.getNation().startsOnEastCoast()
                ? "west" : "east";
            message.add("%direction%", direction);
            myPlayer.addModelMessage(message);
            // force view of tutorial message
            igc.nextModelMessage();
        }
    }
}
