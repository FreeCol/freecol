
package net.sf.freecol.client.control;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.MessageHandler;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Unit;

import net.sf.freecol.client.networking.*;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.FreeColMenuBar;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.CanvasMouseMotionListener;
import net.sf.freecol.client.gui.CanvasMouseListener;
import net.sf.freecol.client.gui.CanvasKeyListener;
import net.sf.freecol.client.gui.panel.MapControls;

import org.w3c.dom.Node;
import org.w3c.dom.Element;
import java.util.logging.Logger;
import java.awt.Color;



/**
* Handles the network messages that arrives before the game starts.
*/
public final class PreGameInputHandler implements MessageHandler {
    private static final Logger logger = Logger.getLogger(PreGameInputHandler.class.getName());

    private final FreeColClient freeColClient;




    /**
    * The constructor to use.
    * @param freeColClient The main controller.
    */
    public PreGameInputHandler(FreeColClient freeColClient) {
        this.freeColClient = freeColClient;
    }





    /**
    * Deals with incoming messages that have just been received.
    *
    * @param connection The <code>Connection</code> the message was received on.
    * @param element The root element of the message.
    */
    public Element handle(Connection connection, Element element) {
        Element reply = null;

        if (element != null) {

            String type = element.getTagName();

            if (type.equals("addPlayer")) {
                reply = addPlayer(element);
            } else if (type.equals("removePlayer")) {
                reply = removePlayer(element);
            } else if (type.equals("chat")) {
                reply = chat(element);
            } else if (type.equals("playerReady")) {
                reply = playerReady(element);
            } else if (type.equals("updateNation")) {
                reply = updateNation(element);
            } else if (type.equals("updateColor")) {
                reply = updateColor(element);
            } else if (type.equals("updateGame")) {
                reply = updateGame(element);
            } else if (type.equals("startGame")) {
                reply = startGame(element);
            } else if (type.equals("error")) {
                reply = error(element);
            } else {
                logger.warning("Message is of unsupported type \"" + type + "\".");
            }
        }

        return reply;
    }


    /**
    * Handles an "addPlayer"-message.
    *
    * @param element The element (root element in a DOM-parsed XML tree) that
    *                holds all the information.
    */
    private Element addPlayer(Element element) {
        Game game = freeColClient.getGame();

        Element playerElement = (Element) element.getElementsByTagName(Player.getXMLElementTagName()).item(0);
        Player newPlayer = new Player(game, playerElement);

        freeColClient.getGame().addPlayer(newPlayer);

        return null;
    }


    /**
    * Handles a "removePlayer"-message.
    *
    * @param element The element (root element in a DOM-parsed XML tree) that
    *                holds all the information.
    */
    private Element removePlayer(Element element) {
        Game game = freeColClient.getGame();

        Element playerElement = (Element) element.getElementsByTagName(Player.getXMLElementTagName()).item(0);
        Player player = new Player(game, playerElement);

        freeColClient.getGame().removePlayer(player);

        return null;
    }


    /**
    * Handles a "chat"-message.
    *
    * @param element The element (root element in a DOM-parsed XML tree) that
    *                holds all the information.
    */
    private Element chat(Element element)  {
        String senderName = element.getAttribute("sender");
        String message = element.getAttribute("message");
        boolean privateChat = Boolean.valueOf(element.getAttribute("privateChat")).booleanValue();

        freeColClient.getCanvas().getStartGamePanel().displayChat(senderName, message, privateChat);
        
        return null;
    }


    /**
    * Handles a PlayerReady message.
    *
    * @param element The element (root element in a DOM-parsed XML tree) that
    *                holds all the information.
    */
    private Element playerReady(Element element) {
        Game game = freeColClient.getGame();

        Player player = (Player) game.getFreeColGameObject(element.getAttribute("player"));
        boolean ready = Boolean.valueOf(element.getAttribute("value")).booleanValue();
        
        player.setReady(ready);

        return null;
    }
    

    /**
    * Handles an "updateNation"-message.
    *
    * @param element The element (root element in a DOM-parsed XML tree) that
    *                holds all the information.
    */
    private Element updateNation(Element element) {        
        Game game = freeColClient.getGame();

        Player player = (Player) game.getFreeColGameObject(element.getAttribute("player"));
        String nation = element.getAttribute("nation");

        player.setNation(nation);
        return null;
    }
    

    /**
    * Handles an "updateColor"-message.
    *
    * @param element The element (root element in a DOM-parsed XML tree) that
    *                holds all the information.
    */
    private Element updateColor(Element element) {
        Game game = freeColClient.getGame();

        Player player = (Player) game.getFreeColGameObject(element.getAttribute("player"));
        String color = element.getAttribute("color");

        player.setColor(color);
        return null;

    }


    /**
    * Handles an "updateGame"-message.
    *
    * @param element The element (root element in a DOM-parsed XML tree) that
    *                holds all the information.
    */
    private Element updateGame(Element element) {
        freeColClient.getGame().readFromXMLElement((Element) element.getElementsByTagName(Game.getXMLElementTagName()).item(0));
        
        return null;
    }
    

    /*
    * Handles an "startGame"-message.
    *
    * @param element The element (root element in a DOM-parsed XML tree) that
    *                holds all the information.
    */
    private Element startGame(Element element) {
        Canvas canvas = freeColClient.getCanvas();
        GUI gui = freeColClient.getGUI();

        canvas.closeMainPanel();        
        canvas.closeMenus();

        FreeColMenuBar freeColMenuBar = new FreeColMenuBar(freeColClient, canvas, gui);
        canvas.setJMenuBar(freeColMenuBar);

        InGameController inGameController = freeColClient.getInGameController();
        InGameInputHandler inGameInputHandler = freeColClient.getInGameInputHandler();

        freeColClient.getClient().setMessageHandler(inGameInputHandler);
        gui.setGame(freeColClient.getGame());

        MapControls mapControls = new MapControls(freeColClient, gui);
        canvas.setMapControls(mapControls);

        Unit activeUnit = freeColClient.getMyPlayer().getNextActiveUnit();
        gui.setActiveUnit(activeUnit);
        gui.setFocus(activeUnit.getTile().getPosition());

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

        return null;
    }


    /**
    * Handles an "error"-message.
    *
    * @param element The element (root element in a DOM-parsed XML tree) that
    *                holds all the information.
    */
    private Element error(Element element)  {
        Canvas canvas = freeColClient.getCanvas();
        
        if (element.hasAttribute("messageID")) {
            canvas.errorMessage(element.getAttribute("messageID"), element.getAttribute("message"));
        } else {
            canvas.errorMessage(null, element.getAttribute("message"));
        }

        return null;
    }
}
