
package net.sf.freecol.server;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.Iterator;

// XML:
import org.w3c.dom.Element;


// Networking:
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.Message;

// Control:
import net.sf.freecol.server.control.UserConnectionHandler;
import net.sf.freecol.server.control.PreGameController;
import net.sf.freecol.server.control.PreGameInputHandler;
import net.sf.freecol.server.control.InGameInputHandler;

// Model:
import net.sf.freecol.server.networking.Server;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.common.model.Game;


/**
* The main control class for the FreeCol server. This class both
* starts and keeps references to all of the server objects and the
* game model objects.
*
* <br><br>
*
* If you would like to start a new server you just create a new object of
* {@link #FreeColServer}.
*
*/
public final class FreeColServer {
    public static final String  COPYRIGHT = "Copyright (C) 2004 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private static Logger logger = Logger.getLogger(FreeColServer.class.getName());

    /** Constant for storing the state of the game. */
    public static final int STARTING_GAME = 0,
                            IN_GAME = 1,
                            ENDING_GAME = 2;

    /** Stores the current state of the game. */
    private int gameState = STARTING_GAME;

    // Networking:
    private Server server;

    // Control:
    private UserConnectionHandler userConnectionHandler;
    private PreGameController preGameController;
    private PreGameInputHandler preGameInputHandler;
    private InGameInputHandler inGameInputHandler;

    private Game game;
    private boolean singleplayer;





    /**
    * Starts a new server in a specified mode and with a specified port.
    *
    * @param singleplayer Sets the game as singleplayer (if <i>true</i>)
    *                     or multiplayer (if <i>false</i>).
    *
    * @param port         The TCP port to use for the public socket.
    *                     That is the port the clients will connect to.
    *
    * @throws IOException if the public socket cannot be created (the exception
    *                     will be logged by this class).
    *
    */
    public FreeColServer(boolean singleplayer, int port) throws IOException {
        this.singleplayer = singleplayer;

        game = new Game();

        userConnectionHandler = new UserConnectionHandler(this);
        preGameController = new PreGameController(this);
        preGameInputHandler = new PreGameInputHandler(this);
        inGameInputHandler = new InGameInputHandler(this);

        try {
            server = new Server(this, port);
            server.start();
        } catch (IOException e) {
            logger.warning("Exception while starting server: " + e);
            throw e;
        }
    }




    
    /**
    * Shuts down this server.
    */
    public void shutdown() {
        getServer().shutdown();
    }


    /**
    * Sets the mode of the game: singleplayer/multiplayer.
    *
    * @param singleplayer Sets the game as singleplayer (if <i>true</i>)
    *                     or multiplayer (if <i>false</i>).
    */
    public void setSingleplayer(boolean singleplayer) {
        this.singleplayer = singleplayer;
    }


    /**
    * Checks if the user is playing in singleplayer mode.
    * @return <i>true</i> if the user is playing in singleplayer mode, <i>false</i> otherwise.
    */
    public boolean isSingleplayer() {
        return singleplayer;
    }


    /**
    * Makes the entire map visible for all players. Used only when debugging (will be removed).
    */
    public void revealMapForAllPlayers() {
        Iterator playerIterator = getGame().getPlayerIterator();
        while (playerIterator.hasNext()) {
            ServerPlayer player = (ServerPlayer) playerIterator.next();
            player.revealMap();

            Element updateElement = Message.createNewRootElement("update");
            updateElement.appendChild(getGame().getMap().toXMLElement(player, updateElement.getOwnerDocument()));
            try {
                player.getConnection().send(updateElement);
            } catch (IOException e) {}
        }
    }


    /**
    * Gets a <code>Player</code> specified by a connection.
    *
    * @param connection The connection to use while searching for a <code>ServerPlayer</code>.
    * @return The player.
    */
    public ServerPlayer getPlayer(Connection connection) {
        Iterator playerIterator = getGame().getPlayerIterator();

        while (playerIterator.hasNext()) {
            ServerPlayer player = (ServerPlayer) playerIterator.next();
            if (player.getConnection() == connection) {
                return player;
            }
        }

        return null;
    }


    /**
    * Gets the <code>UserConnectionHandler</code>.
    * @return The <code>UserConnectionHandler</code> that is beeing used when new client connect.
    */
    public UserConnectionHandler getUserConnectionHandler() {
        return userConnectionHandler;
    }


    /**
    * Gets the <code>PreGameController</code>.
    * @return The <code>PreGameController</code>.
    */
    public PreGameController getPreGameController() {
        return preGameController;
    }


    /**
    * Gets the <code>PreGameInputHandler</code>.
    * @return The <code>PreGameInputHandler</code>.
    */
    public PreGameInputHandler getPreGameInputHandler() {
        return preGameInputHandler;
    }


    /**
    * Gets the <code>InGameInputHandler</code>.
    * @return The <code>InGameInputHandler</code>.
    */
    public InGameInputHandler getInGameInputHandler() {
        return inGameInputHandler;
    }


    /**
    * Gets the <code>Game</code> that is beeing played.
    * @return The <code>Game</code> which is the main class of the game-model
    *         beeing used in this game.
    */
    public Game getGame() {
        return game;
    }


    /**
    * Gets the current state of the game.
    * @return One of: {@link #STARTING_GAME}, {@link #IN_GAME} and {@link #ENDING_GAME}.
    */
    public int getGameState() {
        return gameState;
    }

    
    /** 
    * Sets the current state of the game.
    * @param state The new state to be set. One of: {@link #STARTING_GAME},
    *              {@link #IN_GAME} and {@link #ENDING_GAME}.
    */
    public void setGameState(int state) {
        gameState = state;
    }


    /**
    * Gets the network server responsible of handling the connections.
    * @return The network server.
    */
    public Server getServer() {
        return server;
    }
}
