
package net.sf.freecol.server;

import java.io.*;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.Iterator;

// XML:
import org.w3c.dom.*;


// Networking:
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.server.networking.DummyConnection;

// Control:
import net.sf.freecol.server.control.Controller;
import net.sf.freecol.server.control.UserConnectionHandler;
import net.sf.freecol.server.control.PreGameController;
import net.sf.freecol.server.control.PreGameInputHandler;
import net.sf.freecol.server.control.InGameInputHandler;
import net.sf.freecol.server.control.InGameController;
import net.sf.freecol.server.control.ServerModelController;
import net.sf.freecol.server.control.AIInGameInputHandler;

// Model:
import net.sf.freecol.server.networking.Server;
import net.sf.freecol.server.model.*;
import net.sf.freecol.common.model.*;

// Zip:
import java.util.zip.InflaterInputStream;
import java.util.zip.DeflaterOutputStream;


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
    private ServerModelController modelController;
    private InGameController inGameController;

    private Game game;
    private boolean singleplayer;
    
    // The username of the player owning this server.
    private String owner;





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

        modelController = new ServerModelController(this);

        game = new Game(modelController);

        userConnectionHandler = new UserConnectionHandler(this);
        preGameController = new PreGameController(this);
        preGameInputHandler = new PreGameInputHandler(this);
        inGameInputHandler = new InGameInputHandler(this);
        inGameController = new InGameController(this);

        try {
            server = new Server(this, port);
            server.start();
        } catch (IOException e) {
            logger.warning("Exception while starting server: " + e);
            throw e;
        }
    }

    
    /**
    * Starts a new server in a specified mode and with a specified port
    * and loads the game from the given file.
    *
    * @param file         The file where the game data is located.
    *
    * @param port         The TCP port to use for the public socket.
    *                     That is the port the clients will connect to.
    *
    * @throws IOException if the public socket cannot be created (the exception
    *                     will be logged by this class).
    *
    */
    public FreeColServer(File file, int port) throws IOException {
        this.singleplayer = singleplayer;

        modelController = new ServerModelController(this);

        userConnectionHandler = new UserConnectionHandler(this);
        preGameController = new PreGameController(this);
        preGameInputHandler = new PreGameInputHandler(this);
        inGameInputHandler = new InGameInputHandler(this);
        inGameController = new InGameController(this);

        try {
            server = new Server(this, port);
            server.start();
        } catch (IOException e) {
            logger.warning("Exception while starting server: " + e);
            throw e;
        }
        
        owner = loadGame(file);
    }    


    
    /**
    * The owner of the game is the player that have loaded the
    * game.
    * @see #loadGame
    */
    public String getOwner() {
        return owner;
    }
    
    
    /**
    * Saves a game.
    * @param file The file where the data will be written.
    * @param username The username of the player saving the game.
    */
    public void saveGame(File file, String username) throws IOException {
        Game game = getGame();

        Element savedGameElement = Message.createNewRootElement("savedGame");
        Document document = savedGameElement.getOwnerDocument();

        savedGameElement.setAttribute("owner", username);
        savedGameElement.setAttribute("singleplayer", Boolean.toString(singleplayer));
        savedGameElement.setAttribute("version", Message.getFreeColProtocolVersion());

        // Add server side model information:
        Element serverObjectsElement = document.createElement("serverObjects");
        Iterator fcgoIterator = game.getFreeColGameObjectIterator();
        while (fcgoIterator.hasNext()) {
            FreeColGameObject fcgo = (FreeColGameObject) fcgoIterator.next();
            if (fcgo instanceof ServerModelObject) {
                serverObjectsElement.appendChild(((ServerModelObject) fcgo).toServerAdditionElement(document));
            }
        }
        savedGameElement.appendChild(serverObjectsElement);

        // Add the rest:
        savedGameElement.appendChild(game.toSavedXMLElement(document));

        PrintWriter out = new PrintWriter(new DeflaterOutputStream(new FileOutputStream(file)));
        out.print(savedGameElement.toString());
        out.close();
    }
    
    
    /**
    * Loads a game.
    * @param file The file where the game data is located.
    * @return The username of the player saving the game.
    */
    public String loadGame(File file) throws IOException {
        InflaterInputStream in = new InflaterInputStream(new FileInputStream(file));
        StringBuffer sb = new StringBuffer();
           
        Message message = new Message(in);
        
        Element savedGameElement = message.getDocument().getDocumentElement();
        Element serverObjectsElement = (Element) savedGameElement.getElementsByTagName("serverObjects").item(0);
        
        singleplayer = Boolean.valueOf(savedGameElement.getAttribute("singleplayer")).booleanValue();

        ArrayList serverObjects = new ArrayList();
        
        NodeList serverObjectsNodeList = serverObjectsElement.getChildNodes();
        for (int i=0; i<serverObjectsNodeList.getLength(); i++) {
            Element element = (Element) serverObjectsNodeList.item(i);
            if (element.getTagName().equals(ServerPlayer.getServerAdditionXMLElementTagName())) {
                serverObjects.add(new ServerPlayer(element));
            } else if (element.getTagName().equals(ServerUnit.getServerAdditionXMLElementTagName())) {
                serverObjects.add(new ServerUnit(element));
            }
        }
        
        Element gameElement = (Element) savedGameElement.getElementsByTagName(Game.getXMLElementTagName()).item(0);
        game = new Game(getModelController(), gameElement, (FreeColGameObject[]) serverObjects.toArray(new FreeColGameObject[0]));
        
        gameState = IN_GAME;
        
        Iterator playerIterator = game.getPlayerIterator();
        while (playerIterator.hasNext()) {
            ServerPlayer player = (ServerPlayer) playerIterator.next();
            if (player.isAI()) {
                DummyConnection theConnection = new DummyConnection(getInGameInputHandler(), null);
                theConnection.setOutgoingMessageHandler(new AIInGameInputHandler(this, player));
                getServer().addConnection(theConnection, -1);
                player.setConnection(theConnection);
                player.setConnected(true);
            }
        }

        return savedGameElement.getAttribute("owner");
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
    * Gets the <code>Controller</code>.
    * @return The <code>Controller</code>.
    */
    public Controller getController() {
        if (getGameState() == IN_GAME) {
            return inGameController;
        } else {
            return preGameController;
        }
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


    public InGameController getInGameController() {
        return inGameController;
    }
    

    public ServerModelController getModelController() {
        return modelController;
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
