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
import net.sf.freecol.client.gui.CanvasKeyListener;
import net.sf.freecol.client.gui.CanvasMouseListener;
import net.sf.freecol.client.gui.CanvasMouseMotionListener;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.InGameMenuBar;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.sound.SoundPlayer;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.NationOptions.NationState;
import net.sf.freecol.common.model.NationType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.ChatMessage;
import net.sf.freecol.common.resources.ChipResource;
import net.sf.freecol.common.resources.ColorResource;
import net.sf.freecol.common.resources.ResourceManager;
import net.sf.freecol.common.resources.ResourceMapping;
import net.sf.freecol.server.generator.MapGeneratorOptions;

import org.w3c.dom.Element;



/**
* The controller that will be used before the game starts.
*/
public final class PreGameController {
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(PreGameController.class.getName());


    private FreeColClient freeColClient;

    private MapGeneratorOptions mapGeneratorOptions = null;




    /**
    * The constructor to use.
    * @param freeColClient The main controller.
    */
    public PreGameController(FreeColClient freeColClient) {
        this.freeColClient = freeColClient;
    }



    /**
     * Sets the <code>MapGeneratorOptions</code> used when creating
     * a map.
     * 
     * @param mapGeneratorOptions The <code>MapGeneratorOptions</code>.
     */
    void setMapGeneratorOptions(MapGeneratorOptions mapGeneratorOptions) {
        this.mapGeneratorOptions = mapGeneratorOptions; 
    }
    
    /**
     * Gets the <code>MapGeneratorOptions</code> used when creating
     * a map.
     * 
     * @return The <code>MapGeneratorOptions</code>.
     */
    public MapGeneratorOptions getMapGeneratorOptions() {
        return mapGeneratorOptions; 
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


    /**
    * Sets this client's player's color.
    * @param color Which color this player wishes to set.
    */
    public void setColor(Color color) {
        // Make the change:
        freeColClient.getMyPlayer().setColor(color);

        // Inform the server:
        Element colorElement = Message.createNewRootElement("setColor");
        // the substring hack is necessary to prevent an integer
        // overflow, as the hex string will be unsigned, and
        // Integer.decode() expects a signed int
        colorElement.setAttribute("value", "#" + Integer.toHexString(color.getRGB()).substring(2));

        freeColClient.getClient().sendAndWait(colorElement);
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
        updateGameOptionsElement.appendChild(freeColClient.getGame().getGameOptions().toXMLElement(updateGameOptionsElement.getOwnerDocument()));

        freeColClient.getClient().send(updateGameOptionsElement);        
    }

    /**
     * Sends the {@link MapGeneratorOptions} to the server.
     * This method should be called after updating that object.
     */
     public void sendMapGeneratorOptions() {
         if (mapGeneratorOptions != null) {
             Element updateMapGeneratorOptionsElement = Message.createNewRootElement("updateMapGeneratorOptions");
             updateMapGeneratorOptionsElement
                 .appendChild(mapGeneratorOptions.toXMLElement(updateMapGeneratorOptionsElement.getOwnerDocument()));
             freeColClient.getGame().setMapGeneratorOptions(mapGeneratorOptions);
             freeColClient.getClient().send(updateMapGeneratorOptionsElement);
         }
     }    

    /**
     * Starts the game.
     */
    public void startGame() {
        Canvas canvas = freeColClient.getCanvas();
        GUI gui = freeColClient.getGUI();

        ResourceMapping gameMapping = new ResourceMapping();
        for (Player player : freeColClient.getGame().getPlayers()) {
            gameMapping.add(player.getNationID() + ".color",
                            new ColorResource(player.getColor()));
            gameMapping.add(player.getNationID() + ".chip",
                            ChipResource.colorChip(player.getColor()));
            gameMapping.add(player.getNationID() + ".mission.chip",
                            ChipResource.missionChip(player.getColor(), false));
            gameMapping.add(player.getNationID() + ".mission.expert.chip",
                            ChipResource.missionChip(player.getColor(), true));
            ResourceManager.setGameMapping(gameMapping);
        }

        if (!freeColClient.isHeadless()) {
            canvas.closeMainPanel();
            canvas.closeMenus();
            canvas.closeStatusPanel();
            
            // TODO: Nation specific intro-music:
            freeColClient.playMusicOnce("england", SoundPlayer.STANDARD_DELAY);
        }

        InGameController inGameController = freeColClient.getInGameController();
        InGameInputHandler inGameInputHandler = freeColClient.getInGameInputHandler();

        freeColClient.getClient().setMessageHandler(inGameInputHandler);

        if (!freeColClient.isHeadless()) {
            gui.setInGame(true);
            freeColClient.getFrame().setJMenuBar(new InGameMenuBar(freeColClient));
        }

        Unit activeUnit = freeColClient.getMyPlayer().getNextActiveUnit();
        //freeColClient.getMyPlayer().updateCrossesRequired();
        gui.setActiveUnit(activeUnit);
        if (activeUnit != null) {
            gui.setFocus(activeUnit.getTile().getPosition());
        } else {
            gui.setFocus(((Tile) freeColClient.getMyPlayer().getEntryLocation()).getPosition());
        }

        canvas.addKeyListener(new CanvasKeyListener(canvas, inGameController));
        canvas.addMouseListener(new CanvasMouseListener(canvas, gui));
        canvas.addMouseMotionListener(new CanvasMouseMotionListener(canvas, gui, freeColClient.getGame().getMap()));
        
        if (freeColClient.getGame().getTurn().getNumber() == 1) {
            Player player = freeColClient.getMyPlayer();
            player.addModelMessage(new ModelMessage(ModelMessage.MessageType.TUTORIAL, 
                                                    "tutorial.startGame", player));
            // force view of tutorial message
            inGameController.nextModelMessage();
        }
    }
}
