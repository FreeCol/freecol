/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.CanvasMouseListener;
import net.sf.freecol.client.gui.CanvasMouseMotionListener;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.InGameMenuBar;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.NationOptions.NationState;
import net.sf.freecol.common.model.NationType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.ChatMessage;
import net.sf.freecol.common.resources.ChipResource;
import net.sf.freecol.common.resources.ResourceManager;
import net.sf.freecol.common.resources.ResourceMapping;
import net.sf.freecol.server.generator.MapGeneratorOptions;

import org.w3c.dom.Element;



/**
* The controller that will be used before the game starts.
*/
public final class PreGameController {

    private static final Logger logger = Logger.getLogger(PreGameController.class.getName());

    private FreeColClient freeColClient;

    /**
    * The constructor to use.
    * @param freeColClient The main controller.
    */
    public PreGameController(FreeColClient freeColClient) {
        this.freeColClient = freeColClient;
    }

    /**
     * Gets the <code>MapGeneratorOptions</code> used when creating
     * a map.
     * 
     * @return The <code>MapGeneratorOptions</code>.
     */
    public OptionGroup getMapGeneratorOptions() {
        return freeColClient.getGame().getMapGeneratorOptions();
    }

    /**
    * Sets this client to be (or not be) ready to start the game.
    * @param ready Indicates wether or not this client is ready
    *              to start the game.
    */
    public void setReady(boolean ready) {
        // Make the change:
        freeColClient.getMyPlayer().setReady(ready);

        // Inform the server:
        Element readyElement = Message.createNewRootElement("ready");
        readyElement.setAttribute("value", Boolean.toString(ready));

        freeColClient.getClient().send(readyElement);
    }


    /**
     * Sets this client's player's nation.
     * @param nation Which nation this player wishes to set.
     */
    public void setNation(Nation nation) {
        // Make the change:
        freeColClient.getMyPlayer().setNation(nation);

        // Inform the server:
        Element nationElement = Message.createNewRootElement("setNation");
        nationElement.setAttribute("value", nation.getId());

        freeColClient.getClient().sendAndWait(nationElement);
    }


    /**
     * Sets this client's player's nation type.
     * @param nationType Which nation this player wishes to set.
     */
    public void setNationType(NationType nationType) {
        // Make the change:
        freeColClient.getMyPlayer().setNationType(nationType);

        // Inform the server:
        Element nationTypeElement = Message.createNewRootElement("setNationType");
        nationTypeElement.setAttribute("value", nationType.getId());

        freeColClient.getClient().sendAndWait(nationTypeElement);
    }


    public void setAvailable(Nation nation, NationState state) {
        freeColClient.getGame().getNationOptions().getNations().put(nation, state);
        Element availableElement = Message.createNewRootElement("setAvailable");
        availableElement.setAttribute("nation", nation.getId());
        availableElement.setAttribute("state", state.toString());
        freeColClient.getClient().sendAndWait(availableElement);
    }


    /**
    * Requests the game to be started. This will only be successful
    * if all players are ready to start the game.
    */
    public void requestLaunch() {
        Canvas canvas = freeColClient.getCanvas();

        if (!freeColClient.getGame().isAllPlayersReadyToLaunch()) {
            canvas.errorMessage("server.notAllReady");
            return;
        }

        Element requestLaunchElement = Message.createNewRootElement("requestLaunch");
        freeColClient.getClient().send(requestLaunchElement);

        canvas.showStatusPanel( Messages.message("status.startingGame") );
    }


    /**
     * Sends a chat message.
     *
     * @param message The text of the message.
     */
    public void chat(String message) {
        ChatMessage chatMessage = new ChatMessage(freeColClient.getMyPlayer(),
                                                  message,
                                                  Boolean.FALSE);
        freeColClient.getClient().send(chatMessage.toXMLElement());
    }
    

    /**
    * Sends the {@link GameOptions} to the server.
    * This method should be called after updating that object.
    */
    public void sendGameOptions() {
        Element updateGameOptionsElement = Message.createNewRootElement("updateGameOptions");
        OptionGroup gameOptions = freeColClient.getGame().getSpecification().getOptionGroup("gameOptions");
        updateGameOptionsElement.appendChild(gameOptions.toXMLElement(updateGameOptionsElement.getOwnerDocument()));
        freeColClient.getClient().send(updateGameOptionsElement);        
    }

    /**
     * Sends the {@link MapGeneratorOptions} to the server.
     * This method should be called after updating that object.
     */
     public void sendMapGeneratorOptions() {
         OptionGroup mapGeneratorOptions = getMapGeneratorOptions();
         Element updateMapGeneratorOptionsElement = Message.createNewRootElement("updateMapGeneratorOptions");
         updateMapGeneratorOptionsElement
             .appendChild(mapGeneratorOptions.toXMLElement(updateMapGeneratorOptionsElement.getOwnerDocument()));
         //freeColClient.getGame().setMapGeneratorOptions(mapGeneratorOptions);
         freeColClient.getClient().send(updateMapGeneratorOptionsElement);
     }    

    /**
     * Add player-specific resources to the resource manager.
     *
     * @param nationId The player nation identifier.
     */
    private void addPlayerResources(String nationId) {
        Color color = ResourceManager.getColor(nationId + ".color");
        ResourceMapping gameMapping = new ResourceMapping();
        gameMapping.add(nationId + ".chip", ChipResource.colorChip(color));
        gameMapping.add(nationId + ".mission.chip",
                        ChipResource.missionChip(color, false));
        gameMapping.add(nationId + ".mission.expert.chip",
                        ChipResource.missionChip(color, true));
        ResourceManager.addGameMapping(gameMapping);
    }

    /**
     * Starts the game.
     */
    public void startGame() {
        Canvas canvas = freeColClient.getCanvas();
        GUI gui = freeColClient.getGUI();

        for (Player player : freeColClient.getGame().getPlayers()) {
            addPlayerResources(player.getNationID());
        }
        // Unknown nation is not in getPlayers() list.
        addPlayerResources(Nation.UNKNOWN_NATION_ID);

        Player myPlayer = freeColClient.getMyPlayer();
        if (!freeColClient.isHeadless()) {
            canvas.closeMainPanel();
            canvas.closeMenus();
            canvas.closeStatusPanel();
            freeColClient.playSound("sound.intro." + myPlayer.getNationID());
        }
        freeColClient.getClient()
            .setMessageHandler(freeColClient.getInGameInputHandler());

        if (!freeColClient.isHeadless()) {
            gui.setInGame(true);
            freeColClient.getFrame()
                .setJMenuBar(new InGameMenuBar(freeColClient));
        }

        InGameController igc = freeColClient.getInGameController();
        igc.nextActiveUnit((Tile) myPlayer.getEntryLocation());

        canvas.addMouseListener(new CanvasMouseListener(canvas, gui));
        canvas.addMouseMotionListener(new CanvasMouseMotionListener(canvas, gui,
                 freeColClient.getGame().getMap()));
        
        if (freeColClient.getGame().getTurn().getNumber() == 1) {
            myPlayer.addModelMessage(new ModelMessage(ModelMessage.MessageType.TUTORIAL,
                                                      "tutorial.startGame",
                                                      myPlayer));
            // force view of tutorial message
            igc.nextModelMessage();
        }
    }
}
