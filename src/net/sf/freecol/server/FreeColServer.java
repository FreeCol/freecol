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

package net.sf.freecol.server;

import java.awt.Color;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.PseudoRandom;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Player.PlayerType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.NoRouteToServerException;
import net.sf.freecol.server.ai.AIInGameInputHandler;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.control.Controller;
import net.sf.freecol.server.control.InGameController;
import net.sf.freecol.server.control.InGameInputHandler;
import net.sf.freecol.server.control.PreGameController;
import net.sf.freecol.server.control.PreGameInputHandler;
import net.sf.freecol.server.control.ServerModelController;
import net.sf.freecol.server.control.UserConnectionHandler;
import net.sf.freecol.server.generator.MapGenerator;
import net.sf.freecol.server.model.ServerModelObject;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.networking.DummyConnection;
import net.sf.freecol.server.networking.Server;

import org.w3c.dom.Element;

/**
 * The main control class for the FreeCol server. This class both starts and
 * keeps references to all of the server objects and the game model objects.
 * <br>
 * <br>
 * If you would like to start a new server you just create a new object of this
 * class.
 */
public final class FreeColServer {

    private static Logger logger = Logger.getLogger(FreeColServer.class.getName());

    private static final boolean DISABLE_SAVEGAME_COMPRESSION = false;

    private static final int META_SERVER_UPDATE_INTERVAL = 60000;

    /** Constant for storing the state of the game. */
    public static final int STARTING_GAME = 0, IN_GAME = 1, ENDING_GAME = 2;

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

    private MapGenerator mapGenerator;

    private boolean singleplayer;

    // The username of the player owning this server.
    private String owner;

    private boolean publicServer = false;

    private final int port;

    /** The name of this server. */
    private String name;

    private int numberOfPlayers;
    private int advantages;
    private boolean additionalNations;

    /** The provider for random numbers */
    private final ServerPseudoRandom _pseudoRandom = new ServerPseudoRandom();


    /**
     * Starts a new server in a specified mode and with a specified port.
     * 
     * @param publicServer This value should be set to <code>true</code> in
     *            order to appear on the meta server's listing.
     * 
     * @param singleplayer Sets the game as singleplayer (if <i>true</i>) or
     *            multiplayer (if <i>false</i>).
     * 
     * @param port The TCP port to use for the public socket. That is the port
     *            the clients will connect to.
     * 
     * @param name The name of the server, or <code>null</code> if the default
     *            name should be used.
     * 
     * @throws IOException if the public socket cannot be created (the exception
     *             will be logged by this class).
     * 
     */
    
    public FreeColServer(boolean publicServer, boolean singleplayer, int port, String name)
        throws IOException, NoRouteToServerException {
        this(publicServer, singleplayer, port, name, 4, 0, false);
    }

    public FreeColServer(boolean publicServer, boolean singleplayer, int port, String name, int players,
                         int advantages, boolean additionalNations)
        throws IOException, NoRouteToServerException {
        this.publicServer = publicServer;
        this.singleplayer = singleplayer;
        this.port = port;
        this.name = name;
        this.numberOfPlayers = players;
        this.additionalNations = additionalNations;
        this.advantages = advantages;

        modelController = new ServerModelController(this);
        game = new Game(modelController);
        if (additionalNations) {
            game.setVacantNations(FreeCol.getSpecification().getEuropeanNations());
        } else {
            game.setVacantNations(FreeCol.getSpecification().getClassicNations());
        }
        game.setMaximumPlayers(players);
        mapGenerator = new MapGenerator();
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
     * Starts a new server in a specified mode and with a specified port and
     * loads the game from the given file.
     * 
     * @param file The file where the game data is located.
     * 
     * @param publicServer This value should be set to <code>true</code> in
     *            order to appear on the meta server's listing.
     * 
     * @param singleplayer Sets the game as singleplayer (if <i>true</i>) or
     *            multiplayer (if <i>false</i>).
     * @param port The TCP port to use for the public socket. That is the port
     *            the clients will connect to.
     * 
     * @param name The name of the server, or <code>null</code> if the default
     *            name should be used.
     * 
     * @throws IOException if the public socket cannot be created (the exception
     *             will be logged by this class).
     * 
     * @throws FreeColException if the savegame could not be loaded.
     */
    public FreeColServer(File file, boolean publicServer, boolean singleplayer, int port, String name)
            throws IOException, FreeColException, NoRouteToServerException {
        this.publicServer = publicServer;
        this.singleplayer = singleplayer;
        this.port = port;
        this.name = name;
        mapGenerator = new MapGenerator();
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
     * This update is really a "Hi! I am still here!"-message, since an
     * additional update should be sent when a new player is added to/removed
     * from this server etc.
     */
    public void startMetaServerUpdateThread() {
        if (!publicServer) {
            return;
        }
        Timer t = new Timer(true);
        t.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                try {
                    updateMetaServer();
                } catch (NoRouteToServerException e) {}
            }
        }, META_SERVER_UPDATE_INTERVAL, META_SERVER_UPDATE_INTERVAL);
    }

    /**
     * Enters revenge mode against those evil AIs.
     * 
     * @param username The player to enter revenge mode.
     */
    public void enterRevengeMode(String username) {
        if (!singleplayer) {
            throw new IllegalStateException("Cannot enter revenge mode when not singleplayer.");
        }
        final ServerPlayer p = (ServerPlayer) getGame().getPlayerByName(username);
        synchronized (p) {
            List<UnitType> undeads = FreeCol.getSpecification().getUnitTypesWithAbility("model.ability.undead");
            ArrayList<UnitType> navalUnits = new ArrayList<UnitType>();
            ArrayList<UnitType> landUnits = new ArrayList<UnitType>();
            for (UnitType undead : undeads) {
                if (undead.hasAbility("model.ability.navalUnit")) {
                    navalUnits.add(undead);
                } else if (undead.getId().equals("model.unit.revenger")) { // TODO: softcode this
                    landUnits.add(undead);
                }
            }
            if (navalUnits.size() > 0) {
                UnitType navalType = navalUnits.get(getPseudoRandom().nextInt(navalUnits.size()));
                Unit theFlyingDutchman = new Unit(game, p.getEntryLocation(), p, navalType, UnitState.ACTIVE);
                if (landUnits.size() > 0) {
                    UnitType landType = landUnits.get(getPseudoRandom().nextInt(landUnits.size()));
                    new Unit(game, theFlyingDutchman, p, landType, UnitState.SENTRY);
                }
                p.setDead(false);
                p.setPlayerType(PlayerType.UNDEAD);
                p.setColor(Color.BLACK);
                Element updateElement = Message.createNewRootElement("update");
                updateElement.appendChild(((FreeColGameObject) p.getEntryLocation()).toXMLElement(p, updateElement
                        .getOwnerDocument()));
                updateElement.appendChild(p.toXMLElement(p, updateElement.getOwnerDocument()));
                try {
                    p.getConnection().send(updateElement);
                } catch (IOException e) {
                    logger.warning("Could not send update");
                }
            }
        }
    }

    /**
     * Gets the <code>MapGenerator</code> this <code>FreeColServer</code> is
     * using when creating random maps.
     * 
     * @return The <code>MapGenerator</code>.
     */
    public MapGenerator getMapGenerator() {
        return mapGenerator;
    }
    
    /**
     * Sets the <code>MapGenerator</code> this <code>FreeColServer</code> is
     * using when creating random maps.
     * 
     * @param mapGenerator The <code>MapGenerator</code>.
     */
    public void setMapGenerator(MapGenerator mapGenerator) {
        this.mapGenerator = mapGenerator;
    }

    /**
     * Sends information about this server to the meta-server. The information
     * is only sent if <code>public == true</code>.
     */
    public void updateMetaServer() throws NoRouteToServerException {
        updateMetaServer(false);
    }

    /**
     * Returns the name of this server.
     * 
     * @return The name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of this server.
     * 
     * @param name The name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Return the type of advantages used.
     *
     * @return a <code>int</code> value
     */
    public int getAdvantages() {
        return advantages;
    }

    /**
     * Describe <code>getAdditionalNations</code> method here.
     *
     * @return a <code>boolean</code> value
     */
    public boolean getAdditionalNations() {
        return additionalNations;
    }

    /**
     * Returns the number of players.
     *
     * @return an <code>int</code> value
     */
    public int getNumberOfPlayers() {
        return numberOfPlayers;
    }

    /**
     * Sends information about this server to the meta-server. The information
     * is only sent if <code>public == true</code>.
     * 
     * @param firstTime Should be set to <i>true></i> when calling this method
     *      for the first time.
     * @throws NoRouteToServerException if the meta-server cannot connect to
     *      this server.
     */
    public void updateMetaServer(boolean firstTime) throws NoRouteToServerException {
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
                element.setAttribute("name", mc.getSocket().getLocalAddress().getHostAddress() + ":"
                        + Integer.toString(port));
            }
            element.setAttribute("port", Integer.toString(port));
            element.setAttribute("slotsAvailable", Integer.toString(getSlotsAvailable()));
            element.setAttribute("currentlyPlaying", Integer.toString(getNumberOfLivingHumanPlayers()));
            element.setAttribute("isGameStarted", Boolean.toString(gameState != STARTING_GAME));
            element.setAttribute("version", FreeCol.getVersion());
            element.setAttribute("gameState", Integer.toString(getGameState()));
            Element reply = mc.ask(element);
            if (reply != null && reply.getTagName().equals("noRouteToServer")) {
                throw new NoRouteToServerException();
            }
        } catch (IOException e) {
            logger.warning("Network error while communicating with the meta-server.");
            return;
        } finally {
            try {
                // mc.reallyClose();
                mc.close();
            } catch (IOException e) {
                logger.warning("Could not close connection to meta-server.");
                return;
            }
        }
    }

    /**
     * Removes this server from the metaserver's list. The information is only
     * sent if <code>public == true</code>.
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
                // mc.reallyClose();
                mc.close();
            } catch (IOException e) {
                logger.warning("Could not close connection to meta-server.");
                return;
            }
        }
    }

    /**
     * Gets the number of player that may connect.
     * 
     * @return The number of available slots for human players. This number also
     *         includes european players currently controlled by the AI.
     */
    public int getSlotsAvailable() {
        List<Player> players = game.getPlayers();
        int n = game.getMaximumPlayers();
        for (int i = 0; i < players.size(); i++) {
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
     * 
     * @return The number.
     */
    public int getNumberOfLivingHumanPlayers() {
        List<Player> players = game.getPlayers();
        int n = 0;
        for (int i = 0; i < players.size(); i++) {
            if (!((ServerPlayer) players.get(i)).isAI() && !((ServerPlayer) players.get(i)).isDead()
                    && ((ServerPlayer) players.get(i)).isConnected()) {
                n++;
            }
        }
        return n;
    }

    /**
     * Gets the owner of the <code>Game</code>.
     * 
     * @return The owner of the game. THis is the player that has loaded the
     *         game (if any).
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
     * @throws IOException If a problem was encountered while trying to open,
     *             write or close the file.
     */
    public void saveGame(File file, String username) throws IOException {
        final Game game = getGame();
        XMLOutputFactory xof = XMLOutputFactory.newInstance();
        try {
            XMLStreamWriter xsw;
            GZIPOutputStream gzip;
            if (DISABLE_SAVEGAME_COMPRESSION) {
                // No compression
                xsw = xof.createXMLStreamWriter(new FileOutputStream(file));
            } else {
                // Compression
                gzip = new GZIPOutputStream(new FileOutputStream(file));
                xsw = xof.createXMLStreamWriter(gzip);
            }
            xsw.writeStartDocument("UTF-8", "1.0");
            xsw.writeStartElement("savedGame");
            // Add the attributes:
            xsw.writeAttribute("owner", username);
            xsw.writeAttribute("publicServer", Boolean.toString(publicServer));
            xsw.writeAttribute("singleplayer", Boolean.toString(singleplayer));
            xsw.writeAttribute("version", Message.getFreeColProtocolVersion());
            xsw.writeAttribute("randomState", _pseudoRandom.getState());
            // Add server side model information:
            xsw.writeStartElement("serverObjects");
            Iterator<FreeColGameObject> fcgoIterator = game.getFreeColGameObjectIterator();
            while (fcgoIterator.hasNext()) {
                FreeColGameObject fcgo = fcgoIterator.next();
                if (fcgo instanceof ServerModelObject) {
                    ((ServerModelObject) fcgo).toServerAdditionElement(xsw);
                }
            }
            xsw.writeEndElement();
            // Add the game:
            game.toSavedXML(xsw);
            // Add the AIObjects:
            if (aiMain != null) {
                aiMain.toXML(xsw);
            }
            xsw.writeEndElement();
            xsw.writeEndDocument();
            if (!DISABLE_SAVEGAME_COMPRESSION) {
                gzip.finish();
            }
            xsw.flush();
            xsw.close();
        } catch (XMLStreamException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.warning(sw.toString());
            throw new IOException("XMLStreamException.");
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.warning(sw.toString());
            throw new IOException(e.toString());
        }
    }

    /**
     * Creates a <code>XMLStreamReader</code> for reading the given file.
     * Compression is automatically detected.
     * 
     * @param file The file to be read.
     * @return The <code>XMLStreamReader</code>.
     * @exception IOException if thrown while loading the game or if a
     *                <code>XMLStreamException</code> have been thrown by the
     *                parser.
     */
    public static XMLStreamReader createXMLStreamReader(File file) throws IOException {
        InputStream in = new BufferedInputStream(new FileInputStream(file));
        // Automatically detect compression:
        in.mark(10);
        byte[] buf = new byte[5];
        in.read(buf, 0, 5);
        in.reset();
        if (!(new String(buf)).equals("<?xml")) {
            in = new BufferedInputStream(new GZIPInputStream(in));
        }
        XMLInputFactory xif = XMLInputFactory.newInstance();
        try {
            return xif.createXMLStreamReader(in);
        } catch (XMLStreamException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.warning(sw.toString());
            throw new IOException("XMLStreamException.");
        }
    }

    /**
     * Loads a game.
     * 
     * @param file The file where the game data is located.
     * @return The username of the player saving the game.
     * @throws IOException If a problem was encountered while trying to open,
     *             read or close the file.
     * @exception IOException if thrown while loading the game or if a
     *                <code>XMLStreamException</code> have been thrown by the
     *                parser.
     * @exception FreeColException if the savegame contains incompatible data.
     */
    public String loadGame(File file) throws IOException, FreeColException {
        boolean doNotLoadAI = false;
        try {
            XMLStreamReader xsr = createXMLStreamReader(file);
            xsr.nextTag();
            final String version = xsr.getAttributeValue(null, "version");
            if (!Message.getFreeColProtocolVersion().equals(version)) {
                throw new FreeColException("incompatibleVersions");
            }
            String randomState = xsr.getAttributeValue(null, "randomState");
            if (randomState != null && randomState.length() > 0) {
                try {
                    _pseudoRandom.restoreState(randomState);
                } catch (IOException e) {
                    logger.warning("Failed to restore random state, ignoring!");
                }
            }
            final String owner = xsr.getAttributeValue(null, "owner");
            ArrayList<Object> serverObjects = null;
            aiMain = null;
            while (xsr.nextTag() != XMLStreamConstants.END_ELEMENT) {
                if (xsr.getLocalName().equals("serverObjects")) {
                    // Reads the ServerAdditionObjects:
                    serverObjects = new ArrayList<Object>();
                    while (xsr.nextTag() != XMLStreamConstants.END_ELEMENT) {
                        if (xsr.getLocalName().equals(ServerPlayer.getServerAdditionXMLElementTagName())) {
                            serverObjects.add(new ServerPlayer(xsr));
                        } else {
                            throw new XMLStreamException("Unknown tag: " + xsr.getLocalName());
                        }
                    }
                } else if (xsr.getLocalName().equals(Game.getXMLElementTagName())) {
                    // Read the game model:
                    game = new Game(null, getModelController(), xsr, serverObjects
                            .toArray(new FreeColGameObject[0]));
                    game.setCurrentPlayer(null);
                    gameState = IN_GAME;
                    game.checkIntegrity();
                } else if (xsr.getLocalName().equals(AIMain.getXMLElementTagName())) {
                    if (doNotLoadAI) {
                        aiMain = new AIMain(this);
                        game.setFreeColGameObjectListener(aiMain);
                        break;
                    }
                    // Read the AIObjects:
                    aiMain = new AIMain(this, xsr);
                    if (!aiMain.checkIntegrity()) {
                        aiMain = new AIMain(this);
                        logger.info("Replacing AIMain.");
                    }
                    game.setFreeColGameObjectListener(aiMain);
                } else if (xsr.getLocalName().equals("marketdata")) {
                    logger.info("Ignoring market data for compatibility.");
                } else {
                    throw new XMLStreamException("Unknown tag: " + xsr.getLocalName());
                }
            }
            if (aiMain == null) {
                aiMain = new AIMain(this);
                game.setFreeColGameObjectListener(aiMain);
            }
            // Connect the AI-players:
            Iterator<Player> playerIterator = game.getPlayerIterator();
            while (playerIterator.hasNext()) {
                ServerPlayer player = (ServerPlayer) playerIterator.next();
                if (player.isAI()) {
                    DummyConnection theConnection = new DummyConnection(
                            "Server-Server-" + player.getName(),
                            getInGameInputHandler());
                    DummyConnection aiConnection = new DummyConnection(
                            "Server-AI-" + player.getName(),                            
                            new AIInGameInputHandler(this, player, aiMain));
                    aiConnection.setOutgoingMessageHandler(theConnection);
                    theConnection.setOutgoingMessageHandler(aiConnection);
                    getServer().addConnection(theConnection, -1);
                    player.setConnection(theConnection);
                    player.setConnected(true);
                }
            }
            xsr.close();
            // Later, we might want to modify loaded savegames:
            return owner;
        } catch (XMLStreamException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.warning(sw.toString());
            throw new IOException("XMLStreamException.");
        } catch (FreeColException fe) {
            StringWriter sw = new StringWriter();
            fe.printStackTrace(new PrintWriter(sw));
            logger.warning(sw.toString());
            throw fe;
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.warning(sw.toString());
            throw new IOException(e.toString());
        }
    }

    /**
     * Sets the mode of the game: singleplayer/multiplayer.
     * 
     * @param singleplayer Sets the game as singleplayer (if <i>true</i>) or
     *            multiplayer (if <i>false</i>).
     */
    public void setSingleplayer(boolean singleplayer) {
        this.singleplayer = singleplayer;
    }

    /**
     * Checks if the user is playing in singleplayer mode.
     * 
     * @return <i>true</i> if the user is playing in singleplayer mode,
     *         <i>false</i> otherwise.
     */
    public boolean isSingleplayer() {
        return singleplayer;
    }

    /**
     * Makes the entire map visible for all players. Used only when debugging
     * (will be removed).
     */
    public void revealMapForAllPlayers() {
        Iterator<Player> playerIterator = getGame().getPlayerIterator();
        while (playerIterator.hasNext()) {
            ServerPlayer player = (ServerPlayer) playerIterator.next();
            player.revealMap();
        }
        playerIterator = getGame().getPlayerIterator();
        while (playerIterator.hasNext()) {
            ServerPlayer player = (ServerPlayer) playerIterator.next();
            Element reconnect = Message.createNewRootElement("reconnect");
            try {
                player.getConnection().send(reconnect);
            } catch (IOException ex) {
                logger.warning("Could not send reconnect message!");
            }
        }
    }

    /**
     * Gets a <code>Player</code> specified by a connection.
     * 
     * @param connection The connection to use while searching for a
     *            <code>ServerPlayer</code>.
     * @return The player.
     */
    public ServerPlayer getPlayer(Connection connection) {
        Iterator<Player> playerIterator = getGame().getPlayerIterator();
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
     * 
     * @return The <code>UserConnectionHandler</code> that is beeing used when
     *         new client connect.
     */
    public UserConnectionHandler getUserConnectionHandler() {
        return userConnectionHandler;
    }

    /**
     * Gets the <code>Controller</code>.
     * 
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
     * 
     * @return The <code>PreGameInputHandler</code>.
     */
    public PreGameInputHandler getPreGameInputHandler() {
        return preGameInputHandler;
    }

    /**
     * Gets the <code>InGameInputHandler</code>.
     * 
     * @return The <code>InGameInputHandler</code>.
     */
    public InGameInputHandler getInGameInputHandler() {
        return inGameInputHandler;
    }

    /**
     * Gets the controller being used while the game is running.
     * 
     * @return The controller from making a new turn etc.
     */
    public InGameController getInGameController() {
        return inGameController;
    }

    /**
     * Gets the <code>ModelController</code>.
     * 
     * @return The controller used for generating random numbers and creating
     *         new {@link FreeColGameObject}s.
     */
    public ServerModelController getModelController() {
        return modelController;
    }

    /**
     * Gets the <code>Game</code> that is beeing played.
     * 
     * @return The <code>Game</code> which is the main class of the game-model
     *         beeing used in this game.
     */
    public Game getGame() {
        return game;
    }

    /**
     * Sets the main AI-object.
     * 
     * @param aiMain The main AI-object which is responsible for controlling,
     *            updating and saving the AI objects.
     */
    public void setAIMain(AIMain aiMain) {
        this.aiMain = aiMain;
    }

    /**
     * Gets the main AI-object.
     * 
     * @return The main AI-object which is responsible for controlling, updating
     *         and saving the AI objects.
     */
    public AIMain getAIMain() {
        return aiMain;
    }

    /**
     * Gets the current state of the game.
     * 
     * @return One of: {@link #STARTING_GAME}, {@link #IN_GAME} and
     *         {@link #ENDING_GAME}.
     */
    public int getGameState() {
        return gameState;
    }

    /**
     * Sets the current state of the game.
     * 
     * @param state The new state to be set. One of: {@link #STARTING_GAME},
     *            {@link #IN_GAME} and {@link #ENDING_GAME}.
     */
    public void setGameState(int state) {
        gameState = state;
    }

    /**
     * Gets the network server responsible of handling the connections.
     * 
     * @return The network server.
     */
    public Server getServer() {
        return server;
    }

    /**
     * Get the common pseudo random number generator for the server.
     * 
     * @return random number generator.
     */
    public PseudoRandom getPseudoRandom() {
        return _pseudoRandom;
    }

    /**
     * Get multiple random numbers.
     * 
     * @param n The size of the returned array.
     * @return array with random numbers.
     */
    public int[] getRandomNumbers(int n) {
        return _pseudoRandom.getRandomNumbers(n);
    }


    /**
     * This class provides pseudo-random numbers. It is used on the server side.
     * 
     * TODO (if others agree): refactor Game into an interface and client-side
     * and server-side implementations, move this to the server-side class.
     */
    private static class ServerPseudoRandom implements PseudoRandom {
        private static final String HEX_DIGITS = "0123456789ABCDEF";

        private Random _random;


        /**
         * Create a new random number generator with a random seed.
         * <p>
         * The initial seed is calculated using {@link SecureRandom}, which is
         * slower but better than the normal {@link Random} class. Note,
         * however, that {@link SecureRandom} cannot be used for all numbers, as
         * it will return different numbers given the same seed! That breaks the
         * contract established by {@link PseudoRandom}.
         */
        public ServerPseudoRandom() {
            _random = new Random(new SecureRandom().nextLong());
        }

        /**
         * Get the next integer between 0 and n.
         * 
         * @param n The upper bound (exclusive).
         * @return random number between 0 and n.
         */
        public synchronized int nextInt(int n) {
            return _random.nextInt(n);
        }

        /**
         * Get multiple random numbers. This can be used on the client side in
         * order to reduce the number of round-trips to the server side.
         * 
         * @param size The size of the returned array.
         * @return array with random numbers.
         */
        public synchronized int[] getRandomNumbers(int size) {
            int[] numbers = new int[size];
            for (int i = 0; i < size; i++) {
                numbers[i] = _random.nextInt();
            }
            return numbers;
        }

        /**
         * Get the internal state of the random provider as a string.
         * <p>
         * It would have been more convenient to simply return the current seed,
         * but unfortunately it is private.
         * 
         * @return state.
         */
        public synchronized String getState() {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try {
                ObjectOutputStream oos = new ObjectOutputStream(bos);
                oos.writeObject(_random);
                oos.flush();
            } catch (IOException e) {
                throw new IllegalStateException("IO exception in memory!?!", e);
            }
            byte[] bytes = bos.toByteArray();
            StringBuffer sb = new StringBuffer(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(HEX_DIGITS.charAt((b >> 4) & 0x0F));
                sb.append(HEX_DIGITS.charAt(b & 0x0F));
            }
            return sb.toString();
        }

        /**
         * Restore a previously saved state.
         * 
         * @param state The saved state (@see #getState()).
         * @throws IOException if unable to restore state.
         */
        public synchronized void restoreState(String state) throws IOException {
            byte[] bytes = new byte[state.length() / 2];
            int pos = 0;
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = (byte) HEX_DIGITS.indexOf(state.charAt(pos++));
                bytes[i] <<= 4;
                bytes[i] |= (byte) HEX_DIGITS.indexOf(state.charAt(pos++));
            }
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(bis);
            try {
                _random = (Random) ois.readObject();
            } catch (ClassNotFoundException e) {
                throw new IOException("Failed to restore random!");
            }
        }
    }
}
