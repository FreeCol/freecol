
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
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.NationType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.server.generator.MapGeneratorOptions;

import org.w3c.dom.Element;



/**
* The controller that will be used before the game starts.
*/
public final class PreGameController {
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(PreGameController.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

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
    public void setNation(NationType nation) {
        // Make the change:
        freeColClient.getMyPlayer().setNation(nation);

        // Inform the server:
        Element nationElement = Message.createNewRootElement("setNation");
        nationElement.setAttribute("value", nation.getID());

        freeColClient.getClient().sendAndWait(nationElement);
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
        colorElement.setAttribute("value", Player.convertColorToString(color));

        freeColClient.getClient().sendAndWait(colorElement);
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
    * @param message The message as plain text.
    */
    public void chat(String message) {
        Element chatElement = Message.createNewRootElement("chat");
        chatElement.setAttribute("senderName", freeColClient.getMyPlayer().getName());
        chatElement.setAttribute("message", message);
        chatElement.setAttribute("privateChat", "false");

        freeColClient.getClient().send(chatElement);
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
             updateMapGeneratorOptionsElement.appendChild(mapGeneratorOptions.toXMLElement(updateMapGeneratorOptionsElement.getOwnerDocument()));
             freeColClient.getClient().send(updateMapGeneratorOptionsElement);
         }
     }    

    /**
     * Starts the game.
     */
    public void startGame() {
        Canvas canvas = freeColClient.getCanvas();
        GUI gui = freeColClient.getGUI();

        canvas.closeMainPanel();
        canvas.closeMenus();

        InGameController inGameController = freeColClient.getInGameController();
        InGameInputHandler inGameInputHandler = freeColClient.getInGameInputHandler();

        freeColClient.getClient().setMessageHandler(inGameInputHandler);
        gui.setInGame(true);

        freeColClient.getCanvas().setJMenuBar(new InGameMenuBar(freeColClient));
        if (freeColClient.getGame().getTurn().getNumber() == 1) {
            Player player = freeColClient.getMyPlayer();
            player.addModelMessage(new ModelMessage(player, "tutorial.startGame", null, ModelMessage.TUTORIAL, player));
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

        if (freeColClient.getMyPlayer().equals(freeColClient.getGame().getCurrentPlayer())) {
            canvas.requestFocus();
            freeColClient.getInGameController().nextModelMessage();
        } else {
            //canvas.setEnabled(false);
        }
    }
}
