/**
 *  Copyright (C) 2002-2016   The FreeCol Team
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
import net.sf.freecol.common.networking.AddPlayerMessage;
import net.sf.freecol.common.networking.ChatMessage;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.DOMMessage;
import net.sf.freecol.common.networking.ErrorMessage;
import net.sf.freecol.common.networking.UpdateMessage;
import net.sf.freecol.common.networking.UpdateGameOptionsMessage;
import net.sf.freecol.common.networking.UpdateMapGeneratorOptionsMessage;
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
        final String tag = (element == null) ? "(null)" : element.getTagName();
        switch (tag) {
        case AddPlayerMessage.TAG:
            return addPlayer(element);
        case ChatMessage.TAG:
            return chat(element);
        case Connection.DISCONNECT_TAG:
            return disconnect(element);
        case ErrorMessage.TAG:
            return error(element);
        case "logout":
            return logout(element);
        case "multiple":
            return multiple(connection, element);
        case "playerReady":
            return playerReady(element);
        case "removePlayer":
            return removePlayer(element);
        case "setAvailable":
            return setAvailable(element);
        case "startGame":
            return startGame(element);
        case "updateColor":
            return updateColor(element);
        case UpdateMessage.TAG:
            return update(element);
        case UpdateGameOptionsMessage.TAG:
            return updateGameOptions(element);
        case UpdateMapGeneratorOptionsMessage.TAG:
            return updateMapGeneratorOptions(element);
        case "updateNation":
            return updateNation(element);
        case "updateNationType":
            return updateNationType(element);
        default:
            break;
        }
        return unknown(element);
    }

    /**
     * Handles an "addPlayer"-message.
     *
     * @param element The element (root element in a DOM-parsed XML tree) that
     *                holds all the information.
     * @return Null.
     */
    private Element addPlayer(Element element) {
        // The constructor interns the new players directly.
        new AddPlayerMessage(getFreeColClient().getGame(), element);
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
        final ErrorMessage errorMessage = new ErrorMessage(getGame(), element);
        getGUI().showErrorMessage(errorMessage.getMessageId(),
                                  errorMessage.getMessage());
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
            .getElementsByTagName(Player.getTagName()).item(0);
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
                    for (;;) {
                        FreeColClient fcc = getFreeColClient();
                        if (fcc != null) {
                            Game game = fcc.getGame();
                            if (game != null && game.getMap() != null) break;
                        }
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
     * Handles an "update"-message.
     *
     * @param element The element (root element in a DOM-parsed XML
     *     tree) that holds all the information.
     * @return Null.
     */
    private Element update(Element element) {
        final FreeColClient fcc = getFreeColClient();
        final Game game = fcc.getGame();
        final UpdateMessage message = new UpdateMessage(game, element);
        for (FreeColGameObject fcgo : message.getObjects()) {
            if (fcgo instanceof Game) {
                fcgo.insert();
                fcc.addSpecificationActions(((Game)fcgo).getSpecification());
            } else {
                logger.warning("Game node expected: " + fcgo.getId());
            }
        }
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
     * Handles an "updateGameOptions"-message.
     *
     * @param element The element (root element in a DOM-parsed XML tree) that
     *                holds all the information.
     * @return Null.
     */
    private Element updateGameOptions(Element element) {
        final Game game = getFreeColClient().getGame();
        final Specification spec = game.getSpecification();
        UpdateGameOptionsMessage message
            = new UpdateGameOptionsMessage(game, element);
        if (message.mergeOptions(game)) {
            spec.clean("update game options (server initiated)");
            getGUI().updateGameOptions();
        } else {
            logger.warning("Game option update failed");
        }
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
        final Game game = getFreeColClient().getGame();
        UpdateMapGeneratorOptionsMessage message
            = new UpdateMapGeneratorOptionsMessage(game, element);
        if (message.mergeOptions(game)) {
            getGUI().updateMapGeneratorOptions();
        } else {
            logger.warning("Map generator option update failed");
        }
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
