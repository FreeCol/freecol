
package net.sf.freecol.client.control;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.ConnectException;
import java.util.logging.Logger;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.networking.Client;
import net.sf.freecol.client.gui.Canvas;

import net.sf.freecol.common.model.*;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.networking.Message;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


import net.sf.freecol.server.FreeColServer;


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
}
