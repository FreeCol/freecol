
package net.sf.freecol.client.control;

import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.StreamedMessageHandler;

import org.w3c.dom.Element;



/**
* Handles the network messages that arrives before the game starts.
*/
public final class PreGameInputHandler extends InputHandler implements StreamedMessageHandler {
    private static final Logger logger = Logger.getLogger(PreGameInputHandler.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
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
    * @return The reply.
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
     * Handles the main element of an XML message.
     *
     * @param connection The connection the message came from.
     * @param in The stream containing the message.
     * @param out The output stream for the reply.
     */
    public void handle(Connection connection, XMLStreamReader in, XMLStreamWriter out) {
        if (in.getLocalName().equals("updateGame")) {
            updateGame(connection, in, out);
        } else {
            logger.warning("Unkown (streamed) request: " + in.getLocalName());
        }
    }
    
    /**
     * Checks if the message handler support the given message.
     * @param tagName The tag name of the message to check.
     * @return The result.
     */
    public boolean accepts(String tagName) {
        return tagName.equals("updateGame");
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
        if (game.getFreeColGameObject(playerElement.getAttribute("ID")) == null) {
           Player newPlayer = new Player(game, playerElement);
           getFreeColClient().getGame().addPlayer(newPlayer);
        } else {
           game.getFreeColGameObject(playerElement.getAttribute("ID")).readFromXMLElement(playerElement);
        }
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
        int nation = Integer.parseInt(element.getAttribute("value"));

        player.setNation(nation);
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
     * @param connection The <code>Connection</code> the message
     *       will be read from.
     * @param in The stream to read the message from.
     * @param out The stream for the reply.   
     */
    private void updateGame(Connection connection, XMLStreamReader in, XMLStreamWriter out) {
        try {
            in.nextTag();
            getFreeColClient().getGame().readFromXML(in);
        } catch (XMLStreamException e) {
            logger.warning(e.toString());
        }
    }


    /**
    * Handles an "startGame"-message.
    *
    * @param element The element (root element in a DOM-parsed XML tree) that
    *                holds all the information.
    */
    private Element startGame(Element element) {
        /* Wait until map is received from server, sometimes this message arrives
         * when map is still null. Wait in other thread in order not to block and
         * it can receive the map.
         */
        new Thread() {
            public void run() {
                while (getFreeColClient().getGame().getMap() == null) {
                    try {
                        Thread.sleep(200);
                    } catch (Exception ex) {}
                }

                getFreeColClient().getPreGameController().startGame();
            }
        }.start();
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
