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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.Specification;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.NoRouteToServerException;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ai.AIInGameInputHandler;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.generator.MapGenerator;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.networking.DummyConnection;

import org.w3c.dom.Element;


/**
* The control object that is responsible for setting parameters
* and starting a new game. {@link PreGameInputHandler} is used
* to receive and handle network messages from the clients.
*
* <br><br>
*
* The game enters the state {@link FreeColServer#IN_GAME}, when
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
    *   <li>Changes the game state to {@link FreeColServer#IN_GAME}.
    *   <li>Sends the "startGame"-message to the clients.
    * </ol>
    */
    public void startGame() throws FreeColException{
        FreeColServer freeColServer = getFreeColServer();

        Game game = freeColServer.getGame();
        MapGenerator mapGenerator = freeColServer.getMapGenerator();
        AIMain aiMain = new AIMain(freeColServer);
        freeColServer.setAIMain(aiMain);
        game.setFreeColGameObjectListener(aiMain);

        List<Nation> nations = new ArrayList<Nation>();
        int numberOfPlayers = freeColServer.getNumberOfPlayers() - game.getPlayers().size();
        if (numberOfPlayers > 0) {
            nations.addAll(game.getVacantNations().subList(0, numberOfPlayers));
        }
        nations.addAll(FreeCol.getSpecification().getIndianNations());
        nations.addAll(FreeCol.getSpecification().getREFNations());

        // Apply the difficulty level
        Specification.getSpecification().applyDifficultyLevel(game.getGameOptions().getInteger(GameOptions.DIFFICULTY));
        
        // Add AI players
        game.setUnknownEnemy(new Player(game, Player.UNKNOWN_ENEMY, false, null));
        for (Nation nation : nations) {
            if (game.getPlayer(nation.getId()) != null ||
                nation.getType().isREF() && game.getPlayer(nation.getRefID()) == null) {
                continue;
            }

            String name = nation.getRulerName();
            DummyConnection theConnection = new DummyConnection(
                    "Server connection - " + name,
                    freeColServer.getInGameInputHandler());
            ServerPlayer aiPlayer = new ServerPlayer(game,
                                                    name,
                                                    false,
                                                    true,
                                                    null,
                                                    theConnection,
                                                     nation);
            DummyConnection aiConnection = new DummyConnection(
                    "AI connection - " + name,
                    new AIInGameInputHandler(freeColServer, aiPlayer, aiMain));
            
            aiConnection.setOutgoingMessageHandler(theConnection);
            theConnection.setOutgoingMessageHandler(aiConnection);

            freeColServer.getServer().addDummyConnection(theConnection);

            game.addPlayer(aiPlayer);

            // Send message to all players except to the new player:
            Element addNewPlayer = Message.createNewRootElement("addPlayer");
            addNewPlayer.appendChild(aiPlayer.toXMLElement(null, addNewPlayer.getOwnerDocument()));
            freeColServer.getServer().sendToAll(addNewPlayer, theConnection);
        }
        
        // Make the map:
        mapGenerator.createMap(game);
        Map map = game.getMap();
        
        // Inform the clients:
        setMap(map);
        
        
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
     *
     * @param map The new <code>Map</code> to be set.
     */
    public void setMap(Map map) {
        Game game = getFreeColServer().getGame();

        Iterator<Player> playerIterator = game.getPlayerIterator();
        while (playerIterator.hasNext()) {
            ServerPlayer player = (ServerPlayer) playerIterator.next();
            
            if (player.isEuropean() && !player.isREF()) {
                player.setGold(game.getGameOptions().getInteger(GameOptions.STARTING_MONEY));
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
