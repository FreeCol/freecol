
package net.sf.freecol.server.control;


import java.io.IOException;
import java.net.ConnectException;
import java.util.logging.Logger;
import java.util.Iterator;

import net.sf.freecol.FreeCol;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.generator.MapGenerator;

import net.sf.freecol.common.model.*;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


import net.sf.freecol.server.FreeColServer;


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
public final class PreGameController {
    private static final Logger logger = Logger.getLogger(PreGameController.class.getName());


    private FreeColServer freeColServer;



    /**
    * The constructor to use.
    * @param freeColServer The main control object.
    */
    public PreGameController(FreeColServer freeColServer) {
        this.freeColServer = freeColServer;
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
        try {
            // Make the map:
            Game game = freeColServer.getGame();
            MapGenerator mapGenerator = new MapGenerator(game);
            Map map = mapGenerator.createMap(game.getPlayers(), 30, 64);

            // Set the map and inform the clients:
            setMap(map);

            // Start the game:
            freeColServer.setGameState(FreeColServer.IN_GAME);
            Element startGameElement = Message.createNewRootElement("startGame");
            freeColServer.getServer().sendToAll(startGameElement);
            freeColServer.getServer().setMessageHandlerToAllConnections(freeColServer.getInGameInputHandler());
        } catch (FreeColException e) {
            logger.warning("Exception: " + e);
        }

    }


    /**
    * Sets the map and sends an updated <code>Game</code>-object
    * (that includes the map) to the clients.
    *
    * @param map The new <code>Map</code> to be set.
    */
    public void setMap(Map map) {
        Game game = freeColServer.getGame();

        game.setMap(map);

        Iterator playerIterator = game.getPlayerIterator();
        while (playerIterator.hasNext()) {
            ServerPlayer player = (ServerPlayer) playerIterator.next();

            player.resetExploredTiles(map);

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
