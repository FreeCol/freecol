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

package net.sf.freecol.server.control;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.NationOptions.NationState;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.DOMMessage;
import net.sf.freecol.common.networking.NoRouteToServerException;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.generator.MapGenerator;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The control object that is responsible for setting parameters
 * and starting a new game. {@link PreGameInputHandler} is used
 * to receive and handle network messages from the clients.
 *
 * <br><br>
 *
 * The game enters the state {@link net.sf.freecol.server.FreeColServer.GameState#IN_GAME}, when
 * the {@link #startGame} has successfully been invoked.
 *
 * @see InGameInputHandler
 */
public final class PreGameController extends Controller {

    private static final Logger logger = Logger.getLogger(PreGameController.class.getName());

    /**
     * The constructor to use.
     * @param freeColServer The main server object.
     */
    public PreGameController(FreeColServer freeColServer) {
        super(freeColServer);
    }

    /**
     * Updates and starts the game.
     *
     * <br><br>
     *
     * This method performs these tasks in the given order:
     *
     * <br>
     *
     * <ol>
     *   <li>Generates the map.
     *   <li>Sends updated game information to the clients.
     *   <li>Changes the game state to {@link net.sf.freecol.server.FreeColServer.GameState#IN_GAME}.
     *   <li>Sends the "startGame"-message to the clients.
     * </ol>
     */
    public void startGame() throws FreeColException {
        FreeColServer freeColServer = getFreeColServer();

        Game game = freeColServer.getGame();
        Specification spec = game.getSpecification();
        // Apply the difficulty level

        MapGenerator mapGenerator = freeColServer.getMapGenerator();
        AIMain aiMain = new AIMain(freeColServer);
        freeColServer.setAIMain(aiMain);
        game.setFreeColGameObjectListener(aiMain);

        // Add AI players
        game.setUnknownEnemy(new ServerPlayer(game, Player.UNKNOWN_ENEMY,
                                              false, null, null, null));
        Set<Entry<Nation, NationState>> entries =
            new HashSet<Entry<Nation, NationState>>(game.getNationOptions().getNations().entrySet());
        for (Entry<Nation, NationState> entry : entries) {
            if (entry.getValue() != NationState.NOT_AVAILABLE &&
                game.getPlayer(entry.getKey().getId()) == null) {
                freeColServer.addAIPlayer(entry.getKey());
            }
        }
        Collections.sort(game.getPlayers(), Player.playerComparator);

        // Save the old GameOptions as possibly set by clients..
        // TODO: This might not be the best way to do it, the
        // createMap should not really use the entire loadGame method
        OptionGroup gameOptions = spec.getOptionGroup("gameOptions");
        Element oldGameOptions = gameOptions.toXMLElement(DOMMessage.createNewRootElement("oldGameOptions")
                                                          .getOwnerDocument());

        // Make the map.
        mapGenerator.createMap(game);

        // Restore the GameOptions that may have been overwritten by
        // loadGame in createMap
        gameOptions.readFromXMLElement(oldGameOptions);

        // Initial stances and randomizations for all players.
        Random random = getFreeColServer().getServerRandom();
        for (Player player : game.getPlayers()) {
            ((ServerPlayer) player).startGame(random);
            if (player.isIndian()) {
                // Indian players know about each other, but European colonial
                // players do not.
                for (Player other : game.getPlayers()) {
                    if (other != player && other.isIndian()) {
                        player.setStance(other, Stance.PEACE);
                    }
                }
            }
        }

        // Inform the clients.
        for (Player player : game.getPlayers()) {
            if (!player.isAI()) {
                Connection conn = ((ServerPlayer) player).getConnection();
                try {
                    XMLStreamWriter out = conn.send();
                    out.writeStartElement("updateGame");
                    game.toXML(out, player, false, false);
                    out.writeEndElement();
                    conn.endTransmission(null);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "EXCEPTION: ", e);
                }
            }
        }

        // Start the game:
        freeColServer.setGameState(FreeColServer.GameState.IN_GAME);
        try {
            freeColServer.updateMetaServer();
        } catch (NoRouteToServerException e) {}

        Element startGameElement = DOMMessage.createNewRootElement("startGame");
        freeColServer.getServer().sendToAll(startGameElement);
        freeColServer.getServer().setMessageHandlerToAllConnections(freeColServer.getInGameInputHandler());
    }
}
