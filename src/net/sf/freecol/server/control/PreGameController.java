
package net.sf.freecol.server.control;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.i18n.Messages;
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
        MapGenerator mapGenerator = freeColServer.getMapGenerator();
        AIMain aiMain = new AIMain(freeColServer);        
        freeColServer.setAIMain(aiMain);
        game.setFreeColGameObjectListener(aiMain);        

        // Add AI players
        // TODO: do not depend on nation types
        int i = 0;
        for (Nation nation : FreeCol.getSpecification().getNations()) {
            if (game.getPlayer(nation) != null) {
                continue;
            }

            String name = nation.getType().getRulerName();
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

            freeColServer.getServer().addConnection(theConnection, 3 - i);

            freeColServer.getGame().addPlayer(aiPlayer);

            // Send message to all players except to the new player:
            Element addNewPlayer = Message.createNewRootElement("addPlayer");
            addNewPlayer.appendChild(aiPlayer.toXMLElement(null, addNewPlayer.getOwnerDocument()));
            freeColServer.getServer().sendToAll(addNewPlayer, theConnection);
            i++;
        }
        
        // Make the map:        
        mapGenerator.createMap(game);
        Map map = game.getMap();
        
        // Inform the clients:
        setMap(map);
        
        // Initialise the crosses required values.
        /* not necessary for simple scheme
        Iterator<Player> playerIterator = game.getPlayerIterator();
        while (playerIterator.hasNext()) {
            Player p = playerIterator.next();
            p.updateCrossesRequired();
        }
        */
        Iterator<Player> playerIterator = game.getPlayerIterator();
        while (playerIterator.hasNext()) {
            ServerPlayer player = (ServerPlayer) playerIterator.next();
            if (player.isEuropean()) {
                player.getMarket().randomizeInitialPrices();
                logger.fine("Randomized market for " + player.getName());
                try {
                    Element updateElement = Message.createNewRootElement("updateMarket");
                    updateElement.appendChild(player.getMarket().toXMLElement(player, updateElement.getOwnerDocument()));
                    player.getConnection().send(updateElement);
                } catch (IOException e) {
                    logger.warning("Could not send message to: " + player.getName() + " with connection "
                                   + player.getConnection());
                }
            }
        }

        // Start the game:
        freeColServer.setGameState(FreeColServer.IN_GAME);
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
