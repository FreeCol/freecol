
package net.sf.freecol.client.control;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Game;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas;

import org.w3c.dom.Element;
import java.util.logging.Logger;



/**
* Handles the network messages that arrives before the game starts.
*/
public final class PreGameInputHandler extends InputHandler {
    private static final Logger logger = Logger.getLogger(PreGameInputHandler.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2004 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";


    /**
    * The constructor to use.
    * @param freeColClient The main controller.
    */
    public PreGameInputHandler(FreeColClient freeColClient) {
        super(freeColClient);
    }





    /**
    * Deals with incoming messages that have just been received.
    *
    * @param connection The <code>Connection</code> the message was received on.
    * @param element The root element of the message.
    */
    public synchronized Element handle(Connection connection, Element element) {
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
            } else if (type.equals("logout")) {
                reply = logout(element);
            } else if (type.equals("disconnect")) {
                reply = disconnect(element);
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
        Game game = getFreeColClient().getGame();

        Element playerElement = (Element) element.getElementsByTagName(Player.getXMLElementTagName()).item(0);
        Player newPlayer = new Player(game, playerElement);

        getFreeColClient().getGame().addPlayer(newPlayer);
        getFreeColClient().getCanvas().getStartGamePanel().refreshPlayersTable();

        return null;
    }


    /**
    * Handles a "removePlayer"-message.
    *
    * @param element The element (root element in a DOM-parsed XML tree) that
    *                holds all the information.
    */
    private Element removePlayer(Element element) {
        Game game = getFreeColClient().getGame();

        Element playerElement = (Element) element.getElementsByTagName(Player.getXMLElementTagName()).item(0);
        Player player = new Player(game, playerElement);

        getFreeColClient().getGame().removePlayer(player);
        getFreeColClient().getCanvas().getStartGamePanel().refreshPlayersTable();

        return null;
    }


    /**
    * Handles a "chat"-message.
    *
    * @param element The element (root element in a DOM-parsed XML tree) that
    *                holds all the information.
    */
    private Element chat(Element element)  {
        String senderName = element.getAttribute("senderName");
        String message = element.getAttribute("message");
        boolean privateChat = Boolean.valueOf(element.getAttribute("privateChat")).booleanValue();

        getFreeColClient().getCanvas().getStartGamePanel().displayChat(senderName, message, privateChat);

        return null;
    }


    /**
    * Handles a PlayerReady message.
    *
    * @param element The element (root element in a DOM-parsed XML tree) that
    *                holds all the information.
    */
    private Element playerReady(Element element) {
        Game game = getFreeColClient().getGame();

        Player player = (Player) game.getFreeColGameObject(element.getAttribute("player"));
        boolean ready = Boolean.valueOf(element.getAttribute("value")).booleanValue();

        player.setReady(ready);
        getFreeColClient().getCanvas().getStartGamePanel().refreshPlayersTable();

        return null;
    }


    /**
    * Handles an "updateNation"-message.
    *
    * @param element The element (root element in a DOM-parsed XML tree) that
    *                holds all the information.
    */
    private Element updateNation(Element element) {
        Game game = getFreeColClient().getGame();

        Player player = (Player) game.getFreeColGameObject(element.getAttribute("player"));
        String nation = element.getAttribute("value");

        try {
            player.setNation(nation);
        }
        catch (FreeColException e) {
            logger.warning(e.getMessage());
        }

        getFreeColClient().getCanvas().getStartGamePanel().refreshPlayersTable();

        return null;
    }


    /**
    * Handles an "updateColor"-message.
    *
    * @param element The element (root element in a DOM-parsed XML tree) that
    *                holds all the information.
    */
    private Element updateColor(Element element) {
        Game game = getFreeColClient().getGame();

        Player player = (Player) game.getFreeColGameObject(element.getAttribute("player"));
        String color = element.getAttribute("value");

        player.setColor(color);

        getFreeColClient().getCanvas().getStartGamePanel().refreshPlayersTable();

        return null;
    }


    /**
    * Handles an "updateGame"-message.
    *
    * @param element The element (root element in a DOM-parsed XML tree) that
    *                holds all the information.
    */
    private Element updateGame(Element element) {
        getFreeColClient().getGame().readFromXMLElement((Element) element.getElementsByTagName(Game.getXMLElementTagName()).item(0));

        return null;
    }


    /**
    * Handles an "startGame"-message.
    *
    * @param element The element (root element in a DOM-parsed XML tree) that
    *                holds all the information.
    */
    private Element startGame(Element element) {
        getFreeColClient().getPreGameController().startGame();
        return null;
    }


    /**
    * Handles an "logout"-message.
    *
    * @param element The element (root element in a DOM-parsed XML tree) that
    *                holds all the information.
    */
    private Element logout(Element element) {
        Game game = getFreeColClient().getGame();

        String playerID = element.getAttribute("player");
        // For now we ignore the 'reason' attibute, we could display the reason to the user.

        Player player = (Player) game.getFreeColGameObject(playerID);

        game.removePlayer(player);

        getFreeColClient().getCanvas().getStartGamePanel().refreshPlayersTable();

        return null;
    }


    /**
    * Handles an "error"-message.
    *
    * @param element The element (root element in a DOM-parsed XML tree) that
    *                holds all the information.
    */
    private Element error(Element element)  {
        Canvas canvas = getFreeColClient().getCanvas();

        if (element.hasAttribute("messageID")) {
            canvas.errorMessage(element.getAttribute("messageID"), element.getAttribute("message"));
        } else {
            canvas.errorMessage(null, element.getAttribute("message"));
        }

        return null;
    }
}
