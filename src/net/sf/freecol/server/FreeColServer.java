
package net.sf.freecol.server;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.Iterator;
import javax.swing.Timer;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import net.sf.freecol.FreeCol;

// XML:
import org.w3c.dom.*;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;


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

// Model:
import net.sf.freecol.server.networking.Server;
import net.sf.freecol.server.model.*;
import net.sf.freecol.common.model.*;

// AI:
import net.sf.freecol.server.ai.*;

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
    public static final String  COPYRIGHT = "Copyright (C) 2003-2004 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private static Logger logger = Logger.getLogger(FreeColServer.class.getName());

    private static final int META_SERVER_UPDATE_INTERVAL = 60000;

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
    private AIMain aiMain;
    private boolean singleplayer;

    // The username of the player owning this server.
    private String owner;

    private boolean publicServer = false;
    private final int port;




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
    public FreeColServer(boolean publicServer, boolean singleplayer, int port) throws IOException {
        this.publicServer = publicServer;
        this.singleplayer = singleplayer;
        this.port = port;

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
        
        updateMetaServer(true);
        startMetaServerUpdateThread();
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
        this.port = port;

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
        updateMetaServer(true);
        startMetaServerUpdateThread();
    }

    
    
    /**
    * Starts the metaserver update thread if <code>publicServer == true</code>.
    *
    * This update is really a "Hi! I am still here!"-message, since an additional
    * update should be sent when a new player is added to/removed from this server etc.
    */
    public void startMetaServerUpdateThread() {
        if (!publicServer) {
            return;
        }

        ActionListener updater = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateMetaServer();
            }
        };
        new Timer(META_SERVER_UPDATE_INTERVAL, updater).start();
    }



    /**
    * Sends information about this server to the meta-server.
    * The information is only sent if <code>public == true</code>.
    */
    public void updateMetaServer() {
        updateMetaServer(false);
    }


    /**
    * Sends information about this server to the meta-server.
    * The information is only sent if <code>public == true</code>.
    *
    * @param firstTime Should be set to <i>true></i> when calling
    *                  this method for the first time.
    */
    public void updateMetaServer(boolean firstTime) {
        if (!publicServer) {
            return;
        }

        Connection mc;
        try {
            mc = new Connection(FreeCol.META_SERVER_ADDRESS, FreeCol.META_SERVER_PORT, null);
        } catch (IOException e) {
            logger.warning("Could not connect to meta-server.");
            return;
        }

        try {
            Element element;
            if (firstTime) {
                element = Message.createNewRootElement("register");
            } else {
                element = Message.createNewRootElement("update");
            }

            // TODO: Add possibillity of choosing a name:
            element.setAttribute("name", mc.getSocket().getLocalAddress().getHostAddress() + ":" + Integer.toString(port));
            element.setAttribute("port", Integer.toString(port));
            element.setAttribute("slotsAvailable", Integer.toString(getSlotsAvailable()));
            element.setAttribute("currentlyPlaying", Integer.toString(getNumberOfLivingHumanPlayers()));
            element.setAttribute("isGameStarted", Boolean.toString(gameState != STARTING_GAME));

            mc.send(element);
        } catch (IOException e) {
            logger.warning("Network error while communicating with the meta-server.");
            return;
        } finally {
            try {
                mc.reallyClose();
            } catch (IOException e) {
                logger.warning("Could not close connection to meta-server.");
                return;
            }
        }
    }


    /**
    * Removes this server from the metaserver's list.
    * The information is only sent if <code>public == true</code>.
    *
    * @param firstTime Should be set to <i>true></i> when calling
    *                  this method for the first time.
    */
    public void removeFromMetaServer() {
        if (!publicServer) {
            return;
        }
        
        Connection mc;
        try {
            mc = new Connection(FreeCol.META_SERVER_ADDRESS, FreeCol.META_SERVER_PORT, null);
        } catch (IOException e) {
            logger.warning("Could not connect to meta-server.");
            return;
        }

        try {
            Element element = Message.createNewRootElement("remove");
            element.setAttribute("port", Integer.toString(port));

            mc.send(element);
        } catch (IOException e) {
            logger.warning("Network error while communicating with the meta-server.");
            return;
        } finally {
            try {
                mc.reallyClose();
            } catch (IOException e) {
                logger.warning("Could not close connection to meta-server.");
                return;
            }
        }
    }

    
    /**
    * Gets the number of player that may connect.
    */
    public int getSlotsAvailable() {
        Vector players = game.getPlayers();

        int n = game.getMaximumPlayers();
        for (int i=0; i<players.size(); i++) {
            if (!((ServerPlayer) players.get(i)).isEuropean() || ((ServerPlayer) players.get(i)).isREF()) {
                continue;
            }
            if (((ServerPlayer) players.get(i)).isDead() || ((ServerPlayer) players.get(i)).isConnected()) {
                n--;
            }
        }

        return n;
    }


    /**
    * Gets the number of human players in this game that is still playing.
    */
    public int getNumberOfLivingHumanPlayers() {
        Vector players = game.getPlayers();

        int n = 0;
        for (int i=0; i<players.size(); i++) {
            if (!((ServerPlayer) players.get(i)).isAI() && !((ServerPlayer) players.get(i)).isDead() && ((ServerPlayer) players.get(i)).isConnected()) {
                n++;
            }
        }

        return n;
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
        savedGameElement.setAttribute("publicServer", Boolean.toString(publicServer));
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

        // Add the game:
        savedGameElement.appendChild(game.toSavedXMLElement(document));

        // Add the AIObjects:
        savedGameElement.appendChild(aiMain.toXMLElement(document));

        // Write the XML Element to the file:
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer xmlTransformer = factory.newTransformer();
            xmlTransformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

            // For debugging (without compression):
            //PrintWriter out = new PrintWriter(new FileOutputStream(file));
            DeflaterOutputStream out = new DeflaterOutputStream(new FileOutputStream(file));
            xmlTransformer.transform(new DOMSource(savedGameElement), new StreamResult(out));
            out.close();
        } catch (TransformerException e) {
            e.printStackTrace();
            return;
        }
    }


    /**
    * Loads a game.
    * @param file The file where the game data is located.
    * @return The username of the player saving the game.
    */
    public String loadGame(File file) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(new InflaterInputStream(new FileInputStream(file))));
        // For debugging (without compression):
        //BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file)));

        Message message = new Message(in.readLine());

        Element savedGameElement = message.getDocument().getDocumentElement();
        String version = savedGameElement.getAttribute("version");
        Element serverObjectsElement = (Element) savedGameElement.getElementsByTagName("serverObjects").item(0);

        singleplayer = Boolean.valueOf(savedGameElement.getAttribute("singleplayer")).booleanValue();

        if (savedGameElement.hasAttribute("publicServer")) {
            publicServer = Boolean.valueOf(savedGameElement.getAttribute("publicServer")).booleanValue();
        }

        // Read the ServerAdditionObjects:
        ArrayList serverObjects = new ArrayList();
        ArrayList serverPlayerElements = new ArrayList();
        NodeList serverObjectsNodeList = serverObjectsElement.getChildNodes();
        for (int i=0; i<serverObjectsNodeList.getLength(); i++) {
            Element element = (Element) serverObjectsNodeList.item(i);
            if (element.getTagName().equals(ServerPlayer.getServerAdditionXMLElementTagName())) {
                serverObjects.add(new ServerPlayer(element));
                serverPlayerElements.add(element);
            }
        }

        // Read the game model:
        Element gameElement = (Element) savedGameElement.getElementsByTagName(Game.getXMLElementTagName()).item(0);
        game = new Game(aiMain, getModelController(), gameElement, (FreeColGameObject[]) serverObjects.toArray(new FreeColGameObject[0]));
        game.setCurrentPlayer(null);

        // Support for pre-0.0.2 protocols:
        if (version.compareTo(Message.getFreeColProtocolVersion()) < 0) {
            for (int k=0; k<serverPlayerElements.size(); k++) {
                Element spElement = (Element) serverPlayerElements.get(k);
                ServerPlayer sp = (ServerPlayer) game.getFreeColGameObject(spElement.getAttribute("ID"));

                Element exploredTileElement = Message.getChildElement(spElement, "exploredTiles");
                if (exploredTileElement != null) {
                    boolean[][] exploredTiles = Message.readFromArrayElement("exploredTiles", exploredTileElement, new boolean[0][0]);
                    for (int i=0; i<exploredTiles.length; i++) {
                        for (int j=0; j<exploredTiles[0].length; j++) {
                            game.getMap().getTile(i, j).createPlayerExploredTile(sp);
                            game.getMap().getTile(i, j).getPlayerExploredTile(sp).setExplored(exploredTiles[i][j]);
                        }
                    }
                }

            }
        }

        gameState = IN_GAME;

        // Read the AIObjects:
        if (savedGameElement.getElementsByTagName(AIMain.getXMLElementTagName()).getLength() > 0) {
            Element aiMainElement = (Element) savedGameElement.getElementsByTagName(AIMain.getXMLElementTagName()).item(0);
            aiMain = new AIMain(this, aiMainElement);
        } else {
            aiMain = new AIMain(this);
        }

        // Connect the AI-players:
        Iterator playerIterator = game.getPlayerIterator();
        while (playerIterator.hasNext()) {
            ServerPlayer player = (ServerPlayer) playerIterator.next();
            if (player.isAI()) {
                DummyConnection theConnection = new DummyConnection(getInGameInputHandler());
                DummyConnection aiConnection = new DummyConnection(new AIInGameInputHandler(this, player, aiMain));
                aiConnection.setOutgoingMessageHandler(theConnection);
                theConnection.setOutgoingMessageHandler(aiConnection);

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


    public void setAIMain(AIMain aiMain) {
        this.aiMain = aiMain;
    }


    public AIMain getAIMain() {
        return aiMain;
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
