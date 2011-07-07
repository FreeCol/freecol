/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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

import java.util.logging.Logger;

import javax.swing.SwingUtilities;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.NationOptions.NationState;
import net.sf.freecol.common.model.NationType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.networking.ChatMessage;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.StreamedMessageHandler;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.server.generator.MapGeneratorOptions;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
* Handles the network messages that arrives before the game starts.
*/
public final class PreGameInputHandler extends InputHandler implements StreamedMessageHandler {

    private static final Logger logger = Logger.getLogger(PreGameInputHandler.class.getName());


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
            } else if (type.equals("updateGameOptions")) {
                reply = updateGameOptions(element);
            } else if (type.equals("updateMapGeneratorOptions")) {
                reply = updateMapGeneratorOptions(element);
            } else if (type.equals("chat")) {
                reply = chat(element);
            } else if (type.equals("playerReady")) {
                reply = playerReady(element);
            } else if (type.equals("updateNation")) {
                reply = updateNation(element);
            } else if (type.equals("updateNationType")) {
                reply = updateNationType(element);
            } else if (type.equals("setAvailable")) {
                reply = setAvailable(element);
            } else if (type.equals("startGame")) {
                reply = startGame(element);
            } else if (type.equals("logout")) {
                reply = logout(element);
            } else if (type.equals("disconnect")) {
                reply = disconnect(element);
            } else if (type.equals("error")) {
                reply = error(element);
            } else if (type.equals("multiple")) {
                reply = multiple(connection, element);
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
        if (game.getFreeColGameObject(playerElement.getAttribute(FreeColObject.ID_ATTRIBUTE)) == null) {
           Player newPlayer = new Player(game, playerElement);
           getFreeColClient().getGame().addPlayer(newPlayer);
        } else {
           game.getFreeColGameObject(playerElement.getAttribute(FreeColObject.ID_ATTRIBUTE)).readFromXMLElement(playerElement);
        }
        getFreeColClient().getCanvas().refreshPlayersTable();

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
        getFreeColClient().getCanvas().refreshPlayersTable();

        return null;
    }

    /**
     * Handles an "updateGameOptions"-message.
     *
     * @param element The element (root element in a DOM-parsed XML tree) that
     *                holds all the information.
     */
    private Element updateGameOptions(Element element) {
        Game game = getFreeColClient().getGame();

        Element mgoElement = (Element) element.getElementsByTagName("gameOptions").item(0);
        OptionGroup gameOptions = game.getSpecification().getOptionGroup("gameOptions");
        gameOptions.readFromXMLElement(mgoElement);

        getFreeColClient().getCanvas().updateGameOptions();

        return null;
    }
    
    /**
     * Handles an "updateMapGeneratorOptions"-message.
     *
     * @param element The element (root element in a DOM-parsed XML tree) that
     *                holds all the information.
     */
    private Element updateMapGeneratorOptions(Element element) {
        Element mgoElement = (Element) element.getElementsByTagName(MapGeneratorOptions.getXMLElementTagName()).item(0);
        getFreeColClient().getGame().getMapGeneratorOptions().readFromXMLElement(mgoElement);

        getFreeColClient().getCanvas().updateMapGeneratorOptions();

        return null;
    }
    
    /**
     * Handles a "chat"-message.
     *
     * @param element The element (root element in a DOM-parsed XML tree) that
     *                holds all the information.
     * @return Null.
     */
    private Element chat(Element element)  {
        ChatMessage chatMessage = new ChatMessage(getGame(), element);
        Canvas canvas = getFreeColClient().getCanvas();
        canvas.displayChat(chatMessage.getPlayer().getName(),
                                               chatMessage.getMessage(),
                                               chatMessage.isPrivate());
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
        getFreeColClient().getCanvas().refreshPlayersTable();

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
        Nation nation = getGame().getSpecification().getNation(element.getAttribute("value"));

        player.setNation(nation);
        getFreeColClient().getCanvas().refreshPlayersTable();

        return null;
    }


    /**
    * Handles an "updateNationType"-message.
    *
    * @param element The element (root element in a DOM-parsed XML tree) that
    *                holds all the information.
    */
    private Element updateNationType(Element element) {
        Game game = getFreeColClient().getGame();

        Player player = (Player) game.getFreeColGameObject(element.getAttribute("player"));
        NationType nationType = getGame().getSpecification().getNationType(element.getAttribute("value"));

        player.setNationType(nationType);
        getFreeColClient().getCanvas().refreshPlayersTable();

        return null;
    }


    /**
    * Handles a "setAvailable"-message.
    *
    * @param element The element (root element in a DOM-parsed XML tree) that
    *                holds all the information.
    */
    private Element setAvailable(Element element) {
        Nation nation = getGame().getSpecification().getNation(element.getAttribute("nation"));
        NationState state = Enum.valueOf(NationState.class, element.getAttribute("state"));
        getFreeColClient().getGame().getNationOptions().setNationState(nation, state);
        getFreeColClient().getCanvas().refreshPlayersTable();

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
        new Thread(FreeCol.CLIENT_THREAD+"Starting game") {
            @Override
            public void run() {
                while (getFreeColClient().getGame().getMap() == null) {
                    try {
                        Thread.sleep(200);
                    } catch (Exception ex) {}
                }

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        getFreeColClient().getPreGameController().startGame();
                    }
                });
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

        getFreeColClient().getCanvas().refreshPlayersTable();

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

    /**
     * Handle all the children of this element.
     *
     * @param connection <code>Connection</code>
     * @param element The element (root element in a DOM-parsed XML tree) that
     *                holds all the information.
     * @return <code>Element</code>
     */
    public Element multiple(Connection connection, Element element) {
        NodeList nodes = element.getChildNodes();
        Element reply = null;

        for (int i = 0; i < nodes.getLength(); i++) {
            reply = handle(connection, (Element) nodes.item(i));
        }
        return reply;
    }
}
