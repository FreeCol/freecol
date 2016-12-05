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
import java.util.logging.Level;

import javax.swing.SwingUtilities;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.NationOptions.NationState;
import net.sf.freecol.common.model.NationType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.networking.AddPlayerMessage;
import net.sf.freecol.common.networking.ChatMessage;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.ErrorMessage;
import net.sf.freecol.common.networking.LoginMessage;
import net.sf.freecol.common.networking.LogoutMessage;
import net.sf.freecol.common.networking.MultipleMessage;
import net.sf.freecol.common.networking.ReadyMessage;
import net.sf.freecol.common.networking.SetAvailableMessage;
import net.sf.freecol.common.networking.SetColorMessage;
import net.sf.freecol.common.networking.SetNationMessage;
import net.sf.freecol.common.networking.SetNationTypeMessage;
import net.sf.freecol.common.networking.TrivialMessage;
import net.sf.freecol.common.networking.UpdateMessage;
import net.sf.freecol.common.networking.UpdateGameOptionsMessage;
import net.sf.freecol.common.networking.UpdateMapGeneratorOptionsMessage;
import net.sf.freecol.server.FreeColServer.ServerState;

import org.w3c.dom.Element;


/**
 * Handles the network messages that arrives before the game starts.
 */
public final class PreGameInputHandler extends ClientInputHandler {

    private static final Logger logger = Logger.getLogger(PreGameInputHandler.class.getName());


    /**
     * The constructor to use.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
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
        register(LogoutMessage.TAG,
            (Connection c, Element e) -> logout(e));
        register(MultipleMessage.TAG,
            (Connection c, Element e) -> multiple(c, e));
        register(ReadyMessage.TAG,
            (Connection c, Element e) -> ready(e));
        register(SetAvailableMessage.TAG,
            (Connection c, Element e) -> setAvailable(e));
        register(SetColorMessage.TAG,
            (Connection c, Element e) -> setColor(e));
        register(SetNationMessage.TAG,
            (Connection c, Element e) -> setNation(e));
        register(TrivialMessage.START_GAME_TAG,
            (Connection c, Element e) -> startGame(e));
        register(UpdateMessage.TAG,
            (Connection c, Element e) -> update(e));
        register(UpdateGameOptionsMessage.TAG,
            (Connection c, Element e) -> updateGameOptions(e));
        register(UpdateMapGeneratorOptionsMessage.TAG,
            (Connection c, Element e) -> updateMapGeneratorOptions(e));
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
        getGUI().showErrorMessage(errorMessage.getTemplate(),
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
        // The {@link LoginMessage} received here should have the game
        // attached.  If we are joining a running game, there is
        // little more needed to be done.  If we are restoring from
        // saved, the game should include a map and a full complement
        // of players, including ours.
        //
        // Otherwise we may still need to select a nation, and
        // optionally change game or map options (using several
        // possible messages).  {@link StartGamePanel} does this.
        //
        // When all the parameters are in place and all players are
        // ready (trivially true in single player, needs checking in
        // multiplayer) the client needs to send a requestLaunch
        // message to ask the server to start the game.  That is
        // either done here or by the start game panel.
        //
        // requestLaunch effectively transfers control to
        // {@link FreeColServer#startGame}.
        //
        final FreeColClient fcc = getFreeColClient();
        final LoginMessage message = new LoginMessage(new Game(), element);
        final String user = message.getUserName();
        final boolean single = message.getSinglePlayer();
        final boolean current = message.getCurrentPlayer();
        final ServerState state = message.getState();
        final Game game = message.getGame(); // This is the real game!

        Player player = game.getPlayerByName(user);
        if (player == null) {
            StringTemplate err = StringTemplate.template("server.noSuchPlayer")
                .addName("%player%", user);
            getGUI().showErrorMessage(err);
            logger.warning(Messages.message(err));
            return null;
        }
        fcc.setGame(game);
        fcc.setSinglePlayer(single);
        fcc.setMyPlayer(player);
        fcc.addSpecificationActions(game.getSpecification());
        fcc.setLoggedIn(true);
        if (current) game.setCurrentPlayer(player);
        if (state == ServerState.IN_GAME) fcc.changeClientState(true);
        logger.info("FreeColClient logged in to "
            + ((fcc.isInGame()) ? "running"
                : (game.allPlayersReadyToLaunch()) ? "ready" : "new")
            + " " + ((single) ? "single" : "multi")
            + "-player game as " + user + "/" + player.getId());

        if (fcc.isInGame()) { // Joining existing game, possibly reconnect
            getGUI().refresh();
            igc().nextModelMessage();
        } else if (game.getMap() != null
            && game.allPlayersReadyToLaunch()) { // Ready to launch!
            pgc().requestLaunch();
        } else { // More parameters need to be set or players to become ready
            getGUI().showStartGamePanel(game, player, single);
        }
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
        final LogoutMessage message = new LogoutMessage(game, element);
        final Player player = message.getPlayer(game);
        final String reason = message.getReason();

        logger.info("Player " + player.getName() + " logged out: " + reason);
        game.removePlayer(player);
        getGUI().refreshPlayersTable();

        return null;
    }

    /**
     * Handle all the children of this element.
     *
     * @param connection The {@code Connection} the element arrived on.
     * @param element The element (root element in a DOM-parsed XML
     *     tree) that holds all the information.
     * @return The result of handling the last {@code Element}.
     */
    public Element multiple(Connection connection, Element element) {
        return new MultipleMessage(element).applyHandler(this, connection);
    }

    /**
     * Handles a "ready"-message.
     *
     * @param element The element (root element in a DOM-parsed XML
     *     tree) that holds all the information.
     * @return Null.
     */
    private Element ready(Element element) {
        final Game game = getGame();
        final ReadyMessage message = new ReadyMessage(game, element);

        Player player = message.getPlayer(game);
        boolean ready = message.getValue();
        if (player != null) {
            player.setReady(ready);
            getGUI().refreshPlayersTable();
        }

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
        final Game game = getGame();
        final Specification spec = getSpecification();
        final SetAvailableMessage message
            = new SetAvailableMessage(game, element);

        game.getNationOptions().setNationState(message.getNation(spec),
                                               message.getNationState());
        getGUI().refreshPlayersTable();
        return null;
    }

    /**
     * Handles an "setColor"-message.
     *
     * @param element The element (root element in a DOM-parsed XML
     *     tree) that holds all the information.
     * @return Null.
     */
    private Element setColor(Element element) {
        final Game game = getGame();
        final Specification spec = game.getSpecification();
        final SetColorMessage message = new SetColorMessage(game, element);
            
        Nation nation = message.getNation(spec);
        if (nation != null) {
            Color color = message.getColor();
            if (color != null) {
                nation.setColor(color);
                getGUI().refreshPlayersTable();
            } else {
                logger.warning("Invalid color: " + message.toString());
            }
        } else {
            logger.warning("Invalid nation: " + message.toString());
        }
        return null;
    }

    /**
     * Handles a "setNation"-message.
     *
     * @param element The element (root element in a DOM-parsed XML
     *     tree) that holds all the information.
     * @return Null.
     */
    private Element setNation(Element element) {
        final Game game = getGame();
        final SetNationMessage message = new SetNationMessage(game, element);

        Player player = message.getPlayer(game);
        Nation nation = message.getValue(getSpecification());
        if (player != null && nation != null) {
            player.setNation(nation);
            getGUI().refreshPlayersTable();
        }

        return null;
    }

    /**
     * Handles a "setNationType"-message.
     *
     * @param element The element (root element in a DOM-parsed XML
     *     tree) that holds all the information.
     * @return Null.
     */
    private Element setNationType(Element element) {
        final Game game = getGame();
        final SetNationTypeMessage message
            = new SetNationTypeMessage(game, element);

        Player player = message.getPlayer(game);
        NationType nationType = message.getValue(getSpecification());
        if (player != null && nationType != null) {
            player.changeNationType(nationType);
            getGUI().refreshPlayersTable();
        }

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
                    Game game;
                    for (;;) {
                        game = getGame();
                        if (game != null && game.getMap() != null) break;
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException ie) {
                            logger.log(Level.SEVERE, "Thread used for starting a game has been interupted.", ie);
                        }
                    }
                    
                    SwingUtilities.invokeLater(() -> {
                            pgc().startGame();
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

        if (!spec.mergeGameOptions(message.getGameOptions(), "client")) {
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
        final Specification spec = getSpecification();
        final UpdateMapGeneratorOptionsMessage message
            = new UpdateMapGeneratorOptionsMessage(game, element);

        if (!spec.mergeMapGeneratorOptions(message.getMapGeneratorOptions(),
                                           "client")) {
            logger.warning("Map generator option update failed");
        }
        return null;
    }
}
