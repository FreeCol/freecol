
package net.sf.freecol.server.control;

import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Logger;

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.networking.Message;
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

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";


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
    public void startGame() {
        FreeColServer freeColServer = getFreeColServer();

        Game game = freeColServer.getGame();
        AIMain aiMain = new AIMain(freeColServer);
        freeColServer.setAIMain(aiMain);
        game.setFreeColGameObjectListener(aiMain);

        // Add AI players:
        for (int i = 0; i < Player.NUMBER_OF_NATIONS; i++) {
            if (game.getPlayer(i) != null) {
                continue;
            }

            DummyConnection theConnection = new DummyConnection(freeColServer.getInGameInputHandler());

            String name;
            if (!Player.isEuropean(i)) {
                name = "Indian_" + Integer.toString(i - 3);
            } else if (!Player.isREF(i)) {
                name = "European_" + Integer.toString(i);
            } else {
                name = "REF_" + Integer.toString(i - 12);
            }

            ServerPlayer aiPlayer = new ServerPlayer(game,
                                                    name,
                                                    false,
                                                    true,
                                                    null,
                                                    theConnection,
                                                    i);

            DummyConnection aiConnection = new DummyConnection(new AIInGameInputHandler(freeColServer, aiPlayer, aiMain));
            aiConnection.setOutgoingMessageHandler(theConnection);
            theConnection.setOutgoingMessageHandler(aiConnection);

            freeColServer.getServer().addConnection(theConnection, 3 - i);

            freeColServer.getGame().addPlayer(aiPlayer);

            // Send message to all players except to the new player:
            Element addNewPlayer = Message.createNewRootElement("addPlayer");
            addNewPlayer.appendChild(aiPlayer.toXMLElement(null, addNewPlayer.getOwnerDocument()));
            freeColServer.getServer().sendToAll(addNewPlayer, theConnection);
        }
        
        // Make the map:
        MapGenerator mapGenerator = new MapGenerator(game);
        mapGenerator.createMap(game.getPlayers(), 30, 64);
        Map map = game.getMap();
        
        // Inform the clients:
        setMap(map);
        
        // Initialise the crosses required values.
        Iterator playerIterator = game.getPlayerIterator();
        while (playerIterator.hasNext()) {
            Player p = (Player) playerIterator.next();
            p.updateCrossesRequired();
        }
        
        // Start the game:
        freeColServer.setGameState(FreeColServer.IN_GAME);
        freeColServer.updateMetaServer();
        
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
        
        // Already done my the map generator:
        //game.setMap(map);

        game.reinitialiseMarket(); // Do this here because game is restarting. -sjm

        Iterator playerIterator = game.getPlayerIterator();
        while (playerIterator.hasNext()) {
            ServerPlayer player = (ServerPlayer) playerIterator.next();

            player.resetExploredTiles(map);
            
            if (player.isEuropean()) {
                player.setGold(game.getGameOptions().getInteger(GameOptions.STARTING_MONEY));
            }

            Element updateGameElement = Message.createNewRootElement("updateGame");
            updateGameElement.appendChild(game.toXMLElement(player, updateGameElement.getOwnerDocument()));

            try {
                player.getConnection().send(updateGameElement);
            } catch (IOException e) {
                logger.warning("EXCEPTION: " + e);
            }
        }
    }

}
