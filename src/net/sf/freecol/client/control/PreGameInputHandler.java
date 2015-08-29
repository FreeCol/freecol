/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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

import javax.swing.SwingUtilities;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.NationOptions.NationState;
import net.sf.freecol.common.model.NationType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.networking.ChatMessage;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.option.MapGeneratorOptions;
import net.sf.freecol.common.option.OptionGroup;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
 * Handles the network messages that arrives before the game starts.
 */
public final class PreGameInputHandler extends InputHandler {

    private static final Logger logger = Logger.getLogger(PreGameInputHandler.class.getName());


    /**
     * The constructor to use.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public PreGameInputHandler(FreeColClient freeColClient) {
        super(freeColClient);
    }


    /**
     * Deals with incoming messages that have just been received.
     *
     * @param connection The <code>Connection</code> the message was
     *     received on.
     * @param element The root <code>Element</code> of the message.
     * @return The reply.
     */
    @Override
    public synchronized Element handle(Connection connection,
                                       Element element) {
        String type = (element == null) ? "(null)" : element.getTagName();
        return ("addPlayer".equals(type))
            ? addPlayer(element)
            : ("chat".equals(type))
            ? chat(element)
            : ("disconnect".equals(type))
            ? disconnect(element)
            : ("error".equals(type))
            ? error(element)
            : ("logout".equals(type))
            ? logout(element)
            : ("multiple".equals(type))
            ? multiple(connection, element)
            : ("playerReady".equals(type))
            ? playerReady(element)
            : ("removePlayer".equals(type))
            ? removePlayer(element)
            : ("setAvailable".equals(type))
            ? setAvailable(element)
            : ("startGame".equals(type))
            ? startGame(element)
            : ("updateColor".equals(type))
            ? updateColor(element)
            : ("updateGame".equals(type))
            ? updateGame(element)
            : ("updateGameOptions".equals(type))
            ? updateGameOptions(element)
            : ("updateMapGeneratorOptions".equals(type))
            ? updateMapGeneratorOptions(element)
            : ("updateNation".equals(type))
            ? updateNation(element)
            : ("updateNationType".equals(type))
            ? updateNationType(element)
            : unknown(element);
    }

    /**
     * Handles an "addPlayer"-message.
     *
     * @param element The element (root element in a DOM-parsed XML tree) that
     *                holds all the information.
     * @return Null.
     */
    private Element addPlayer(Element element) {
        Game game = getFreeColClient().getGame();

        Element playerElement = (Element)element
            .getElementsByTagName(Player.getXMLElementTagName()).item(0);
        String id = FreeColObject.readId(playerElement);
        FreeColGameObject fcgo = game.getFreeColGameObject(id);
        if (fcgo == null) {
            game.addPlayer(new Player(game, playerElement));
        } else {
            fcgo.readFromXMLElement(playerElement);
        }
        getGUI().refreshPlayersTable();

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
        Game game = getGame();
        ChatMessage chatMessage = new ChatMessage(game, element);
        getGUI().displayChatMessage(chatMessage.getPlayer(game),
                                    chatMessage.getMessage(),
                                    chatMessage.isPrivate());
        return null;
    }

    /**
     * Handles an "error"-message.
     *
     * @param element The element (root element in a DOM-parsed XML
     *     tree) that holds all the information.
     * @return Null.
     */
    private Element error(Element element)  {
        getGUI().showErrorMessage((element.hasAttribute("messageID"))
            ? element.getAttribute("messageID")
            : null,
            element.getAttribute("message"));
        return null;
    }

    /**
     * Handles an "logout"-message.
     *
     * @param element The element (root element in a DOM-parsed XML tree) that
     *                holds all the information.
     * @return Null.
     */
    private Element logout(Element element) {
        Game game = getFreeColClient().getGame();

        String playerId = element.getAttribute("player");
        String reason = element.getAttribute("reason");
        if (reason != null && !reason.isEmpty()) {
            logger.info("Client logging out: " + reason);
        }

        Player player = game.getFreeColGameObject(playerId, Player.class);
        game.removePlayer(player);
        getGUI().refreshPlayersTable();

        return null;
    }

    /**
     * Handle all the children of this element.
     *
     * @param connection The <code>Connection</code> the element arrived on.
     * @param element The element (root element in a DOM-parsed XML tree) that
     *                holds all the information.
     * @return The result of handling the last <code>Element</code>.
     */
    public Element multiple(Connection connection, Element element) {
        NodeList nodes = element.getChildNodes();
        Element reply = null;

        for (int i = 0; i < nodes.getLength(); i++) {
            reply = handle(connection, (Element)nodes.item(i));
        }
        return reply;
    }

    /**
     * Handles a "playerReady"-message.
     *
     * @param element The element (root element in a DOM-parsed XML tree) that
     *                holds all the information.
     * @return Null.
     */
    private Element playerReady(Element element) {
        Game game = getFreeColClient().getGame();

        Player player = game
            .getFreeColGameObject(element.getAttribute("player"), Player.class);
        boolean ready = Boolean.parseBoolean(element.getAttribute("value"));
        player.setReady(ready);
        getGUI().refreshPlayersTable();

        return null;
    }

    /**
     * Handles a "removePlayer"-message.
     *
     * @param element The element (root element in a DOM-parsed XML tree) that
     *                holds all the information.
     * @return Null.
     */
    private Element removePlayer(Element element) {
        Game game = getFreeColClient().getGame();

        Element playerElement = (Element)element
            .getElementsByTagName(Player.getXMLElementTagName()).item(0);
        Player player = new Player(game, playerElement);

        getFreeColClient().getGame().removePlayer(player);
        getGUI().refreshPlayersTable();

        return null;
    }

    /**
     * Handles a "setAvailable"-message.
     *
     * @param element The element (root element in a DOM-parsed XML tree) that
     *                holds all the information.
     * @return Null.
     */
    private Element setAvailable(Element element) {
        Nation nation = getGame().getSpecification()
            .getNation(element.getAttribute("nation"));
        NationState state = Enum.valueOf(NationState.class,
                                         element.getAttribute("state"));
        getFreeColClient().getGame().getNationOptions()
            .setNationState(nation, state);
        getGUI().refreshPlayersTable();
        return null;
    }

    /**
     * Handles an "startGame"-message.
     *
     * Wait until map is received from server, sometimes this
     * message arrives when map is still null. Wait in other
     * thread in order not to block and it can receive the map.
     *
     * @param element The element (root element in a DOM-parsed XML
     *     tree) that holds all the information.
     * @return Null.
     */
    private Element startGame(@SuppressWarnings("unused") Element element) {
        new Thread(FreeCol.CLIENT_THREAD + "Starting game") {
                @Override
                public void run() {
                    while (getFreeColClient().getGame().getMap() == null) {
                        try {
                            Thread.sleep(200);
                        } catch (Exception ex) {}
                    }
                    
                    SwingUtilities.invokeLater(() -> {
                            getFreeColClient().getPreGameController()
                                .startGame();
                        });
                }
            }.start();
        return null;
    }

    /**
     * Handles an "updateColor"-message.
     *
     * @param element The element (root element in a DOM-parsed XML tree) that
     *                holds all the information.
     * @return Null.
     */
    private Element updateColor(Element element) {
        Game game = getFreeColClient().getGame();
        Specification spec = game.getSpecification();
        String str = element.getAttribute("nation");
        Nation nation = spec.getNation(str);
        if (nation == null) {
            logger.warning("Invalid nation: " + str);
            return null;
        }
        Color color;
        try {
            str = element.getAttribute("color");
            int rgb = Integer.parseInt(str);
            color = new Color(rgb);
        } catch (NumberFormatException nfe) {
            logger.warning("Invalid color: " + str);
            return null;
        }
        nation.setColor(color);
        getFreeColClient().getGUI().refreshPlayersTable();
        return null;
    }

    /**
     * Handles an "updateGame"-message.
     *
     * @param element The element (root element in a DOM-parsed XML tree) that
     *                holds all the information.
     * @return Null.
     */
    private Element updateGame(Element element) {
        NodeList children = element.getChildNodes();
        if (children.getLength() == 1) {
            FreeColClient fcc = getFreeColClient();
            Game game = fcc.getGame();
            game.readFromXMLElement((Element)children.item(0));
            fcc.addSpecificationActions(game.getSpecification());
        } else {
            logger.warning("Child node expected: " + element.getTagName());
        }
        return null;
    }

    /**
     * Handles an "updateGameOptions"-message.
     *
     * @param element The element (root element in a DOM-parsed XML tree) that
     *                holds all the information.
     * @return Null.
     */
    private Element updateGameOptions(Element element) {
        Game game = getFreeColClient().getGame();

        Element mgoElement = (Element)element
            .getElementsByTagName(GameOptions.getXMLElementTagName()).item(0);
        Specification spec = game.getSpecification();
        OptionGroup gameOptions = spec.getGameOptions();
        gameOptions.readFromXMLElement(mgoElement);
        spec.clean("update game options (server initiated)");

        getGUI().updateGameOptions();

        return null;
    }
    
    /**
     * Handles an "updateMapGeneratorOptions"-message.
     *
     * @param element The element (root element in a DOM-parsed XML tree) that
     *                holds all the information.
     * @return Null.
     */
    private Element updateMapGeneratorOptions(Element element) {
        Element mgoElement = (Element)element
            .getElementsByTagName(MapGeneratorOptions.getXMLElementTagName())
            .item(0);
        getFreeColClient().getGame().getMapGeneratorOptions()
            .readFromXMLElement(mgoElement);

        getGUI().updateMapGeneratorOptions();

        return null;
    }
    
    /**
     * Handles an "updateNation"-message.
     *
     * @param element The element (root element in a DOM-parsed XML tree) that
     *                holds all the information.
     * @return Null.
     */
    private Element updateNation(Element element) {
        Game game = getFreeColClient().getGame();

        Player player = game
            .getFreeColGameObject(element.getAttribute("player"), Player.class);
        Nation nation = getGame().getSpecification()
            .getNation(element.getAttribute("value"));

        player.setNation(nation);
        getGUI().refreshPlayersTable();

        return null;
    }

    /**
     * Handles an "updateNationType"-message.
     *
     * @param element The element (root element in a DOM-parsed XML tree) that
     *                holds all the information.
     * @return Null.
     */
    private Element updateNationType(Element element) {
        Game game = getFreeColClient().getGame();

        Player player = game
            .getFreeColGameObject(element.getAttribute("player"), Player.class);
        NationType nationType = getGame().getSpecification()
            .getNationType(element.getAttribute("value"));

        player.changeNationType(nationType);
        getGUI().refreshPlayersTable();

        return null;
    }
}
