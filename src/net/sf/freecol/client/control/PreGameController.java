
package net.sf.freecol.client.control;


import java.awt.Color;
import java.util.logging.Logger;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.FreeColMenuBar;
import net.sf.freecol.client.gui.CanvasMouseMotionListener;
import net.sf.freecol.client.gui.CanvasMouseListener;
import net.sf.freecol.client.gui.CanvasKeyListener;
import net.sf.freecol.client.gui.panel.MapControls;


import net.sf.freecol.common.model.*;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.networking.Message;

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
    public void setNation(String nation) {
        // Make the change:
        try {
            freeColClient.getMyPlayer().setNation(nation);
        }
        catch (FreeColException e) {
            logger.warning(e.getMessage());
        }

        // Inform the server:
        Element nationElement = Message.createNewRootElement("setNation");
        nationElement.setAttribute("value", nation);

        freeColClient.getClient().send(nationElement);
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

        freeColClient.getClient().send(colorElement);
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

        canvas.setEnabled(false);
        canvas.showStatusPanel("Please wait: Starting game");
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
    * Starts the game.
    */
    public void startGame() {
        Canvas canvas = freeColClient.getCanvas();
        GUI gui = freeColClient.getGUI();

        canvas.closeMainPanel();
        canvas.closeMenus();

        FreeColMenuBar freeColMenuBar = new FreeColMenuBar(freeColClient, canvas, gui);
        canvas.setJMenuBar(freeColMenuBar);

        InGameController inGameController = freeColClient.getInGameController();
        InGameInputHandler inGameInputHandler = freeColClient.getInGameInputHandler();

        freeColClient.getClient().setMessageHandler(inGameInputHandler);
        gui.setInGame(true);

        MapControls mapControls = new MapControls(freeColClient, gui);
        canvas.setMapControls(mapControls);

        Unit activeUnit = freeColClient.getMyPlayer().getNextActiveUnit();
        gui.setActiveUnit(activeUnit);
        if (activeUnit != null) {
            gui.setFocus(activeUnit.getTile().getPosition());
        } else {
            gui.setFocus(((Tile) freeColClient.getMyPlayer().getEntryLocation()).getPosition());
        }

        canvas.addKeyListener(new CanvasKeyListener(canvas, inGameController, mapControls));
        canvas.addMouseListener(new CanvasMouseListener(canvas, gui));
        canvas.addMouseMotionListener(new CanvasMouseMotionListener(gui,  freeColClient.getGame().getMap()));

        canvas.showMapControls();

        if (freeColClient.getMyPlayer().equals(freeColClient.getGame().getCurrentPlayer())) {
            //canvas.takeFocus();
        } else {
            canvas.setEnabled(false);
            canvas.showStatusPanel("Waiting for the other players to complete their turn...");
        }
    }
}
