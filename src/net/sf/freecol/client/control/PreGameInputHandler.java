/**
 *  Copyright (C) 2002-2017   The FreeCol Team
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
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

import javax.swing.SwingUtilities;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Game.LogoutReason;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.NationOptions.NationState;
import net.sf.freecol.common.model.NationType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.AddPlayerMessage;
import net.sf.freecol.common.networking.ChatMessage;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.ErrorMessage;
import net.sf.freecol.common.networking.LoginMessage;
import net.sf.freecol.common.networking.LogoutMessage;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.ReadyMessage;
import net.sf.freecol.common.networking.SetAvailableMessage;
import net.sf.freecol.common.networking.SetColorMessage;
import net.sf.freecol.common.networking.SetNationMessage;
import net.sf.freecol.common.networking.SetNationTypeMessage;
import net.sf.freecol.common.networking.StartGameMessage;
import net.sf.freecol.common.networking.TrivialMessage;
import net.sf.freecol.common.networking.UpdateMessage;
import net.sf.freecol.common.networking.UpdateGameOptionsMessage;
import net.sf.freecol.common.networking.UpdateMapGeneratorOptionsMessage;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.common.util.Utils;
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
            (Connection c, Element e) ->
                addPlayer(new AddPlayerMessage(getGame(), e)));
        register(ChatMessage.TAG,
            (Connection c, Element e) ->
                new ChatMessage(getGame(), e).clientHandler(freeColClient));
        register(ErrorMessage.TAG,
            (Connection c, Element e) ->
                error(new ErrorMessage(getGame(), e)));
        register(LoginMessage.TAG,
            (Connection c, Element e) ->
                new LoginMessage(new Game(), e).clientHandler(freeColClient));
        register(LogoutMessage.TAG,
            (Connection c, Element e) ->
                new LogoutMessage(getGame(), e).clientHandler(freeColClient));
        register(ReadyMessage.TAG,
            (Connection c, Element e) ->
                new ReadyMessage(getGame(), e).clientHandler(freeColClient));
        register(SetAvailableMessage.TAG,
            (Connection c, Element e) ->
                setAvailable(new SetAvailableMessage(getGame(), e)));
        register(SetColorMessage.TAG,
            (Connection c, Element e) ->
                setColor(new SetColorMessage(getGame(), e)));
        register(SetNationMessage.TAG,
            (Connection c, Element e) ->
                setNation(new SetNationMessage(getGame(), e)));
        register(SetNationTypeMessage.TAG,
            (Connection c, Element e) ->
                setNationType(new SetNationTypeMessage(getGame(), e)));
        register(StartGameMessage.TAG,
            (Connection c, Element e) ->
                TrivialMessage.startGameMessage.clientHandler(freeColClient));
        register(UpdateMessage.TAG,
            (Connection c, Element e) ->
                update(new UpdateMessage(getGame(), e)));
        register(UpdateGameOptionsMessage.TAG,
            (Connection c, Element e) ->
                updateGameOptions(new UpdateGameOptionsMessage(getGame(), e)));
        register(UpdateMapGeneratorOptionsMessage.TAG,
            (Connection c, Element e) ->
                updateMapGeneratorOptions(new UpdateMapGeneratorOptionsMessage(getGame(), e)));
    }


    // Individual handlers

    /**
     * Handles an "addPlayer"-message.
     *
     * @param message The {@code AddPlayerMessage} to process.
     */
    private void addPlayer(AddPlayerMessage message) {
        // Reading the player in does most of the work.

        getGUI().refreshPlayersTable();
    }

    /**
     * Handle an "error"-message.
     *
     * @param message The {@code ErrorMessage} to process.
     */
    private void error(ErrorMessage message) {
        final StringTemplate template = message.getTemplate();
        final String text = message.getMessage();

        getGUI().showErrorMessage(template, text);
    }

    /**
     * Handles a "setAvailable"-message.
     *
     * @param message The {@code SetAvailableMessage} to process.
     */
    private void setAvailable(SetAvailableMessage message) {
        final Game game = getGame();
        final Specification spec = game.getSpecification();
        final Nation nation = message.getNation(spec);
        final NationState nationState = message.getNationState();

        game.getNationOptions().setNationState(nation, nationState);
        getGUI().refreshPlayersTable();
    }

    /**
     * Handles an "setColor"-message.
     *
     * @param message The {@code SetColorMessage} to process.
     */
    private void setColor(SetColorMessage message) {
        final Game game = getGame();
        final Specification spec = game.getSpecification();
        final Nation nation = message.getNation(spec);
        final Color color = message.getColor();
      
        if (nation != null) {
            if (color != null) {
                nation.setColor(color);
                getGUI().refreshPlayersTable();
            } else {
                logger.warning("Invalid color: " + message.toString());
            }
        } else {
            logger.warning("Invalid nation: " + message.toString());
        }
    }

    /**
     * Handles a "setNation"-message.
     *
     * @param message The {@code SetNationMessage} to process.
     */
    private void setNation(SetNationMessage message) {
        final Game game = getGame();
        final Player player = message.getPlayer(game);
        final Nation nation = message.getValue(getSpecification());

        if (player != null && nation != null) {
            player.setNation(nation);
            getGUI().refreshPlayersTable();
        }
    }

    /**
     * Handles a "setNationType"-message.
     *
     * @param message The {@code SetNationTypeMessage} to process.
     */
    private void setNationType(SetNationTypeMessage message) {
        final Game game = getGame();
        final Player player = message.getPlayer(game);
        final NationType nationType = message.getValue(getSpecification());
        
        if (player != null && nationType != null) {
            player.changeNationType(nationType);
            getGUI().refreshPlayersTable();
        }
    }

    /**
     * Handles an "update"-message.
     *
     * @param message The {@code UpdateMessage} to process.
     */
    private void update(UpdateMessage message) {
        final FreeColClient fcc = getFreeColClient();
        final Game game = getGame();
        final List<FreeColGameObject> objects = message.getObjects();
        
        for (FreeColGameObject fcgo : objects) {
            if (fcgo instanceof Game) {
                fcc.addSpecificationActions(((Game)fcgo).getSpecification());
            } else {
                logger.warning("Game node expected: " + fcgo.getId());
            }
        }
    }

    /**
     * Handles an "updateGameOptions"-message.
     *
     * @param message The {@code UpdateGameOptionsMessage} to process.
     */
    private void updateGameOptions(UpdateGameOptionsMessage message) {
        final Game game = getGame();
        final Specification spec = game.getSpecification();
        final OptionGroup gameOptions = message.getGameOptions();
        
        if (!spec.mergeGameOptions(gameOptions, "client")) {
            logger.warning("Game option update failed");
        }
    }
    
    /**
     * Handles an "updateMapGeneratorOptions"-message.
     *
     * @param message The {@code UpdateMapOptionsMessage} to process.
     */
    private void updateMapGeneratorOptions(UpdateMapGeneratorOptionsMessage message) {
        final Game game = getGame();
        final Specification spec = game.getSpecification();
        final OptionGroup mapOptions = message.getMapGeneratorOptions();

        if (!spec.mergeMapGeneratorOptions(mapOptions, "client")) {
            logger.warning("Map generator option update failed");
        }
    }
}
