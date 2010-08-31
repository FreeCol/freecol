/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.NationOptions.NationState;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.NoRouteToServerException;
import net.sf.freecol.common.option.StringOption;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.generator.IMapGenerator;
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
    public void startGame() throws FreeColException{
        FreeColServer freeColServer = getFreeColServer();

        Game game = freeColServer.getGame();
        // Apply the difficulty level
        
        IMapGenerator mapGenerator = freeColServer.getMapGenerator();
        AIMain aiMain = new AIMain(freeColServer);
        freeColServer.setAIMain(aiMain);
        game.setFreeColGameObjectListener(aiMain);

        // Add AI players
        game.setUnknownEnemy(new Player(game, Player.UNKNOWN_ENEMY, false, null));

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
        // TODO: This might not be the best way to do it, the createMap should not really use the entire loadGame method
        Element oldGameOptions = game.getGameOptions().toXMLElement(Message.createNewRootElement("oldGameOptions").getOwnerDocument());
        
        // Make the map:
        mapGenerator.createMap(game);
        // Restore the GameOptions that may have been overwritten by loadGame in createMap
        game.getGameOptions().readFromXMLElement(oldGameOptions);
        
        // Inform the clients:
        sendUpdatedGame();        
        
        // Start the game:
        freeColServer.setGameState(FreeColServer.GameState.IN_GAME);
        try {
            freeColServer.updateMetaServer();
        } catch (NoRouteToServerException e) {}
        
        Element startGameElement = Message.createNewRootElement("startGame");
        freeColServer.getServer().sendToAll(startGameElement);
        freeColServer.getServer().setMessageHandlerToAllConnections(freeColServer.getInGameInputHandler());
    }
    
    /**
     * Sets the map and sends an updated <code>Game</code>-object
     * (that includes the map) to the clients.
     */
    public void sendUpdatedGame() {
        Game game = getFreeColServer().getGame();
        Random random = getFreeColServer().getServerRandom();

        Iterator<Player> playerIterator = game.getPlayerIterator();
        while (playerIterator.hasNext()) {
            ServerPlayer player = (ServerPlayer) playerIterator.next();
            
            if (player.isEuropean() && !player.isREF()) {
                player.modifyGold(getGame().getSpecification()
                                  .getIntegerOption(GameOptions.STARTING_MONEY).getValue());

                // Generates the initial recruits for this player.
                // Recruits may be determined by the difficulty level,
                // or generated randomly.
                Europe europe = player.getEurope();
                for (int index = 0; index < Europe.RECRUIT_COUNT; index++) {
                    String optionId = "model.option.recruitable.slot" + index;
                    if (getGame().getSpecification().hasOption(optionId)) {
                        String unitTypeId = getGame().getSpecification()
                            .getStringOption(optionId).getValue();
                        if (!StringOption.NONE.equals(unitTypeId)) {
                            europe.setRecruitable(index, getGame().getSpecification().getUnitType(unitTypeId));
                            continue;
                        }
                    }
                    europe.setRecruitable(index, player.generateRecruitable());
                }

                Market market = player.getMarket();
                for (GoodsType goodsType : getGame().getSpecification().getGoodsTypeList()) {
                    if (goodsType.isNewWorldGoodsType() || goodsType.isNewWorldLuxuryType()) {
                        int increase = random.nextInt(3);
                        if (increase > 0) {
                            int newPrice = goodsType.getInitialSellPrice() + increase;
                            market.setInitialPrice(goodsType, newPrice);
                        }
                    }
                }
            }
            if (player.isAI()) {
                continue;
            }

            try {
                XMLStreamWriter out = player.getConnection().send();
                out.writeStartElement("updateGame");
                game.toXML(out, player);
                out.writeEndElement();
                player.getConnection().endTransmission(null);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "EXCEPTION: ", e);
            }
        }
    }

}
