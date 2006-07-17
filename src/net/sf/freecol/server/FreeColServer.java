
package net.sf.freecol.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Monarch;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.server.ai.AIInGameInputHandler;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.control.Controller;
import net.sf.freecol.server.control.InGameController;
import net.sf.freecol.server.control.InGameInputHandler;
import net.sf.freecol.server.control.PreGameController;
import net.sf.freecol.server.control.PreGameInputHandler;
import net.sf.freecol.server.control.ServerModelController;
import net.sf.freecol.server.control.UserConnectionHandler;
import net.sf.freecol.server.model.ServerModelObject;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.networking.DummyConnection;
import net.sf.freecol.server.networking.Server;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


/**
* The main control class for the FreeCol server. This class both
* starts and keeps references to all of the server objects and the
* game model objects.
*
* <br><br>
*
* If you would like to start a new server you just create a new object of
* this class.
*
*/
public final class FreeColServer {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private static Logger logger = Logger.getLogger(FreeColServer.class.getName());

    private static final boolean DISABLE_SAVEGAME_COMPRESSION = false;

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

    /** The name of this server. */
    private String name;




    /**
    * Starts a new server in a specified mode and with a specified port.
    *
    * @param publicServer This value should be set to <code>true</code>
    *                     in order to appear on the meta server's listing.
    *
    * @param singleplayer Sets the game as singleplayer (if <i>true</i>)
    *                     or multiplayer (if <i>false</i>).

    * @param port         The TCP port to use for the public socket.
    *                     That is the port the clients will connect to.
    *
    * @param name         The name of the server, or <code>null</code> if the
    *                     default name should be used.
    *
    * @throws IOException if the public socket cannot be created (the exception
    *                     will be logged by this class).
    *
    */
    public FreeColServer(boolean publicServer, boolean singleplayer, int port, String name) throws IOException {
        this.publicServer = publicServer;
        this.singleplayer = singleplayer;
        this.port = port;
        this.name = name;

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
    * @throws FreeColException if the savegame could not be loaded.
    */
    public FreeColServer(File file, int port) throws IOException, FreeColException {
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
            server.shutdown();
            throw e;
        }

        try {
            owner = loadGame(file);
        } catch (FreeColException e) {
            server.shutdown();
            throw e;
        } catch (Exception e) {
            server.shutdown();
            FreeColException fe = new FreeColException("couldNotLoadGame");
            fe.initCause(e);
            throw fe;
        }

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

        Timer t = new Timer(true);
        t.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                updateMetaServer();
            }
        }, META_SERVER_UPDATE_INTERVAL, META_SERVER_UPDATE_INTERVAL);        
    }



    /**
    * Sends information about this server to the meta-server.
    * The information is only sent if <code>public == true</code>.
    */
    public void updateMetaServer() {
        updateMetaServer(false);
    }


    /**
    * Returns the name of this server.
    * @return The name.
    */
    public String getName() {
        return name;
    }


    /**
    * Sets the name of this server.
    * @param name The name.
    */
    public void setName(String name) {
        this.name = name;
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
            if (name != null) {
                element.setAttribute("name", name);
            } else {
                element.setAttribute("name", mc.getSocket().getLocalAddress().getHostAddress() + ":" + Integer.toString(port));
            }
            element.setAttribute("port", Integer.toString(port));
            element.setAttribute("slotsAvailable", Integer.toString(getSlotsAvailable()));
            element.setAttribute("currentlyPlaying", Integer.toString(getNumberOfLivingHumanPlayers()));
            element.setAttribute("isGameStarted", Boolean.toString(gameState != STARTING_GAME));
            element.setAttribute("version", FreeCol.getVersion());
            element.setAttribute("gameState", Integer.toString(getGameState()));

            mc.send(element);
        } catch (IOException e) {
            logger.warning("Network error while communicating with the meta-server.");
            return;
        } finally {
            try {
                //mc.reallyClose();
                mc.close();
            } catch (IOException e) {
                logger.warning("Could not close connection to meta-server.");
                return;
            }
        }
    }


    /**
    * Removes this server from the metaserver's list.
    * The information is only sent if <code>public == true</code>.
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
                //mc.reallyClose();
                mc.close();
            } catch (IOException e) {
                logger.warning("Could not close connection to meta-server.");
                return;
            }
        }
    }


    /**
    * Gets the number of player that may connect.
    * @return The number of available slots for human
    *       players. This number also includes
    *       european players currently controlled by the AI.
    */
    public int getSlotsAvailable() {
        Vector players = game.getPlayers();

        int n = game.getMaximumPlayers();
        for (int i=0; i<players.size(); i++) {
            ServerPlayer p = (ServerPlayer) players.get(i);
            if (!p.isEuropean() || p.isREF()) {
                continue;
            }
            if (p.isDead() || p.isConnected() && !p.isAI()) {
                n--;
            }
        }

        return n;
    }


    /**
    * Gets the number of human players in this game that is still playing.
    * @return The number.
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
     * Gets the owner of the <code>Game</code>.
     * @return The owner of the game. THis is the player that
     *      has loaded the game (if any).
     * @see #loadGame
     */
    public String getOwner() {
        return owner;
    }


    /**
    * Saves a game.
    *
    * @param file The file where the data will be written.
    * @param username The username of the player saving the game.
    * @throws IOException If a problem was encountered while trying
    *        to open, write or close the file.
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
            xmlTransformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            xmlTransformer.setOutputProperty(OutputKeys.INDENT, "yes");

            if (DISABLE_SAVEGAME_COMPRESSION) {
                // No compression
                OutputStream out = new FileOutputStream(file);
                xmlTransformer.transform(new DOMSource(savedGameElement), new StreamResult(out));
                out.close();
            } else {
                // Compression
                DeflaterOutputStream out = new DeflaterOutputStream(new FileOutputStream(file));
                xmlTransformer.transform(new DOMSource(savedGameElement), new StreamResult(out));
                out.close();
            }

        } catch (TransformerException e) {
            e.printStackTrace();
            return;
        }
    }


    /**
    * Loads a game.
    * 
    * @param file The file where the game data is located.
    * @return The username of the player saving the game.
    * @throws IOException If a problem was encountered while trying
    *        to open, read or close the file.
    * @exception IOException if thrown while loading the game, or if
    *        a <code>SAXException</code> is thrown while parsing the
    *        text.
    * @exception FreeColException if the savegame contains incompatible
    *        data.
    */
    public String loadGame(File file) throws IOException, FreeColException {
        InputStream in;
        if (DISABLE_SAVEGAME_COMPRESSION) {
            // No compression
            in = new FileInputStream(file);
        } else {
            // Compression
            in = new InflaterInputStream(new FileInputStream(file));
        }

        Message message;
        try {
            message = new Message(in);
        } catch (SAXException sxe) {
            // Error generated during parsing
            Exception  x = sxe;
            if (sxe.getException() != null) {
                x = sxe.getException();
            }
            StringWriter sw = new StringWriter();
            x.printStackTrace(new PrintWriter(sw));
            logger.warning(sw.toString());
            throw new IOException("SAXException while creating Message.");
        }

        Element savedGameElement = message.getDocument().getDocumentElement();
        String version = savedGameElement.getAttribute("version");

        if (Message.getFreeColProtocolVersion().equals(version)) {
            Element serverObjectsElement = (Element) savedGameElement.getElementsByTagName("serverObjects").item(0);

            singleplayer = Boolean.valueOf(savedGameElement.getAttribute("singleplayer")).booleanValue();

            if (savedGameElement.hasAttribute("publicServer")) {
                publicServer = Boolean.valueOf(savedGameElement.getAttribute("publicServer")).booleanValue();
            }

            try {
                // Read the ServerAdditionObjects:
                ArrayList serverObjects = new ArrayList();
                ArrayList serverPlayerElements = new ArrayList();
                NodeList serverObjectsNodeList = serverObjectsElement.getChildNodes();
                for (int i=0; i<serverObjectsNodeList.getLength(); i++) {
                    Node node = serverObjectsNodeList.item(i);
                    if (!(node instanceof Element)) {
                        continue;
                    }
                    Element element = (Element) node;
                    if (element.getTagName().equals(ServerPlayer.getServerAdditionXMLElementTagName())) {
                        serverObjects.add(new ServerPlayer(element));
                        serverPlayerElements.add(element);
                    }
                }

                // Read the game model:
                Element gameElement = (Element) savedGameElement.getElementsByTagName(Game.getXMLElementTagName()).item(0);
                game = new Game(aiMain, getModelController(), gameElement, (FreeColGameObject[]) serverObjects.toArray(new FreeColGameObject[0]));
                game.setCurrentPlayer(null);

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

                // Support for pre-0.0.3 protocols:
                Iterator pi = game.getMap().getWholeMapIterator();
                while (pi.hasNext()) {
                    Tile t = game.getMap().getTile((Map.Position) pi.next());
                    if (t.getSettlement() != null && t.getSettlement() instanceof IndianSettlement) {
                        ((IndianSettlement) t.getSettlement()).createGoodsContainer();
                    }
                }

                // Support for pre-0.1.3 protocols:
                Iterator monarchPlayerIterator = game.getPlayerIterator();
                while (monarchPlayerIterator.hasNext()) {
                    Player p = (Player) monarchPlayerIterator.next();
                    if (p.getMonarch() == null) {
                        p.setMonarch(new Monarch(game, p, ""));
                    }
                }

                return savedGameElement.getAttribute("owner");
            } catch (Exception e) {
                throw new IOException(e.getMessage());
            }
        } else {
            throw new FreeColException("incompatibleVersions");
        }
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
                player.getConnection().sendAndWait(updateElement);
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


    /**
     * Gets the controller being used while the game is running.
     * @return The controller from making a new turn etc.
     */
    public InGameController getInGameController() {
        return inGameController;
    }


    /**
     * Gets the <code>ModelController</code>.
     * @return The controller used for generating random numbers
     *      and creating new {@link FreeColGameObject}s.
     */
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
     * Sets the main AI-object.
     * @param aiMain The main AI-object which is responsible for
     *      controlling, updating and saving the AI objects.
     */
    public void setAIMain(AIMain aiMain) {
        this.aiMain = aiMain;
    }


    /**
     * Gets the main AI-object.
     * @return The main AI-object which is responsible for
     *      controlling, updating and saving the AI objects.
     */
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
