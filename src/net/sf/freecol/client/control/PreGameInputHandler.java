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
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.networking.AddPlayerMessage;
import net.sf.freecol.common.networking.ChatMessage;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.DOMMessage;
import net.sf.freecol.common.networking.ErrorMessage;
import net.sf.freecol.common.networking.LoginMessage;
import net.sf.freecol.common.networking.MultipleMessage;
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
public final class PreGameInputHandler extends ClientInputHandler {

    private static final Logger logger = Logger.getLogger(PreGameInputHandler.class.getName());


    /**
     * The constructor to use.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public PreGameInputHandler(FreeColClient freeColClient) {
        super(freeColClient);

        register(AddPlayerMessage.TAG,
            (Connection c, Element e) -> addPlayer(e));
        register(ChatMessage.TAG,
            (Connection c, Element e) -> chat(e));
        register(ErrorMessage.TAG,
            (Connection c, Element e) -> error(e));
        register(LoginMessage.TAG,
            (Connection c, Element e) -> login(e));
        register("logout",
            (Connection c, Element e) -> logout(e));
        register(MultipleMessage.TAG,
            (Connection c, Element e) -> multiple(c, e));
        register("playerReady",
            (Connection c, Element e) -> playerReady(e));
        register("removePlayer",
            (Connection c, Element e) -> removePlayer(e));
        register("setAvailable",
            (Connection c, Element e) -> setAvailable(e));
        register("startGame",
            (Connection c, Element e) -> startGame(e));
        register("updateColor",
            (Connection c, Element e) -> updateColor(e));
        register(UpdateMessage.TAG,
            (Connection c, Element e) -> update(e));
        register(UpdateGameOptionsMessage.TAG,
            (Connection c, Element e) -> updateGameOptions(e));
        register(UpdateMapGeneratorOptionsMessage.TAG,
            (Connection c, Element e) -> updateMapGeneratorOptions(e));
        register("updateNation",
            (Connection c, Element e) -> updateNation(e));
        register("updateNationType",
            (Connection c, Element e) -> updateNationType(e));
    }


    // Individual handlers

    /**
     * Handles an "addPlayer"-message.
     *
     * @param element The element (root element in a DOM-parsed XML
     *     tree) that holds all the information.
     * @return Null.
     */
    private Element addPlayer(Element element) {
        // The message constructor interns the new players directly.
        new AddPlayerMessage(getFreeColClient().getGame(), element);

        getGUI().refreshPlayersTable();
        return null;
    }

    /**
     * Handles a "chat"-message.
     *
     * @param element The element (root element in a DOM-parsed XML
     *     tree) that holds all the information.
     * @return Null.
     */
    private Element chat(Element element)  {
        final Game game = getGame();
        final ChatMessage chatMessage = new ChatMessage(game, element);

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
     * Handle a "login"-request.
     *
     * @param element The element (root element in a DOM-parsed XML
     *     tree) that holds all the information.
     * @return Null.
     */
    private Element login(Element element) {
        final FreeColClient fcc = getFreeColClient();
        final LoginMessage message = new LoginMessage(new Game(), element);
        Game game = message.getGame();
        fcc.setGame(game);

        fcc.setSinglePlayer(message.isSinglePlayer());

        final String user = message.getUserName();
        Player player = game.getPlayerByName(user);
        if (player == null) {
            logger.warning("New game does not contain player: " + user);
            StringTemplate st = StringTemplate.template("server.noSuchPlayer")
                .addName("%player%", user);
            getGUI().showErrorMessage(st);
            return null;
        }
        fcc.setMyPlayer(player);
        fcc.addSpecificationActions(game.getSpecification());
        logger.info("FreeColClient logged in as " + user
            + "/" + player.getId());

        final boolean currentPlayer = message.isCurrentPlayer();
        if (currentPlayer) game.setCurrentPlayer(player);

        if (message.getStartGame()) fcc.getPreGameController().startGame();
        return null;
    }

    /**
     * Handles an "logout"-message.
     *
     * @param element The element (root element in a DOM-parsed XML
     *     tree) that holds all the information.
     * @return Null.
     */
    private Element logout(Element element) {
        final Game game = getGame();

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
     * @param element The element (root element in a DOM-parsed XML
     *     tree) that holds all the information.
     * @return The result of handling the last <code>Element</code>.
     */
    public Element multiple(Connection connection, Element element) {
        return new MultipleMessage(element).applyHandler(this, connection);
    }

    /**
     * Handles a "playerReady"-message.
     *
     * @param element The element (root element in a DOM-parsed XML
     *     tree) that holds all the information.
     * @return Null.
     */
    private Element playerReady(Element element) {
        final Game game = getGame();

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
     * @param element The element (root element in a DOM-parsed XML
     *     tree) that holds all the information.
     * @return Null.
     */
    private Element removePlayer(Element element) {
        final Game game = getGame();

        Element playerElement = (Element)element
            .getElementsByTagName(Player.getTagName()).item(0);
        Player player = new Player(game, playerElement);

        game.removePlayer(player);
        getGUI().refreshPlayersTable();

        return null;
    }

    /**
     * Handles a "setAvailable"-message.
     *
     * @param element The element (root element in a DOM-parsed XML
     *     tree) that holds all the information.
     * @return Null.
     */
    private Element setAvailable(Element element) {
        Nation nation = getSpecification().getNation(element.getAttribute("nation"));
        NationState state = Enum.valueOf(NationState.class,
                                         element.getAttribute("state"));
        getGame().getNationOptions().setNationState(nation, state);
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
                        Game game = getGame();
                        if (game != null && game.getMap() != null) break;
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
        final Game game = getGame();
        final UpdateMessage message = new UpdateMessage(game, element);

        for (FreeColGameObject fcgo : message.getObjects()) {
            if (fcgo instanceof Game) {
                getFreeColClient()
                    .addSpecificationActions(((Game)fcgo).getSpecification());
            } else {
                logger.warning("Game node expected: " + fcgo.getId());
            }
        }
        return null;
    }

    /**
     * Handles an "updateColor"-message.
     *
     * @param element The element (root element in a DOM-parsed XML
     *     tree) that holds all the information.
     * @return Null.
     */
    private Element updateColor(Element element) {
        final Game game = getGame();
        final Specification spec = game.getSpecification();

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
        getGUI().refreshPlayersTable();
        return null;
    }

    /**
     * Handles an "updateGameOptions"-message.
     *
     * @param element The element (root element in a DOM-parsed XML
     *     tree) that holds all the information.
     * @return Null.
     */
    private Element updateGameOptions(Element element) {
        final Game game = getGame();
        final Specification spec = getSpecification();
        final UpdateGameOptionsMessage message
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
     * @param element The element (root element in a DOM-parsed XML
     *     tree) that holds all the information.
     * @return Null.
     */
    private Element updateMapGeneratorOptions(Element element) {
        final Game game = getGame();
        final UpdateMapGeneratorOptionsMessage message
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
     * @param element The element (root element in a DOM-parsed XML
     *     tree) that holds all the information.
     * @return Null.
     */
    private Element updateNation(Element element) {
        final Game game = getGame();

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
     * @param element The element (root element in a DOM-parsed XML
     *     tree) that holds all the information.
     * @return Null.
     */
    private Element updateNationType(Element element) {
        final Game game = getGame();

        Player player = game
            .getFreeColGameObject(element.getAttribute("player"), Player.class);
        NationType nationType = getSpecification()
            .getNationType(element.getAttribute("value"));

        player.changeNationType(nationType);
        getGUI().refreshPlayersTable();

        return null;
    }
}
