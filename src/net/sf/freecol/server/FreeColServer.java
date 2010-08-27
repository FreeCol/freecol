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

import java.awt.image.BufferedImage;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.io.FreeColSavegameFile;
import net.sf.freecol.common.io.FreeColTcFile;
import net.sf.freecol.common.model.DifficultyLevel;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.HighScore;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.NationOptions;
import net.sf.freecol.common.model.NationOptions.Advantages;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.Player.PlayerType;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.NoRouteToServerException;
import net.sf.freecol.common.util.XMLStream;
import net.sf.freecol.server.ai.AIInGameInputHandler;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.control.Controller;
import net.sf.freecol.server.control.InGameController;
import net.sf.freecol.server.control.InGameInputHandler;
import net.sf.freecol.server.control.PreGameController;
import net.sf.freecol.server.control.PreGameInputHandler;
import net.sf.freecol.server.control.ServerModelController;
import net.sf.freecol.server.control.UserConnectionHandler;
import net.sf.freecol.server.generator.IMapGenerator;
import net.sf.freecol.server.generator.MapGenerator;
import net.sf.freecol.server.generator.TerrainGenerator;
import net.sf.freecol.server.model.ServerGame;
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

    private static final Logger logger = Logger.getLogger(FreeColServer.class.getName());

    private static final int META_SERVER_UPDATE_INTERVAL = 60000;

    private static final int NUMBER_OF_HIGH_SCORES = 10;
    private static final String HIGH_SCORE_FILE = "HighScores.xml";

    /**
     * The save game format used for saving games.
     */
    public static final int SAVEGAME_VERSION = 9;

    /**
     * The oldest save game format that can still be loaded.
     */
    public static final int MINIMUM_SAVEGAME_VERSION = 1;

    /** Constant for storing the state of the game. */
    public static enum GameState {STARTING_GAME, IN_GAME, ENDING_GAME}

    /** Stores the current state of the game. */
    private GameState gameState = GameState.STARTING_GAME;

    // Networking:
    private Server server;

    // Control:
    private final UserConnectionHandler userConnectionHandler;

    private final PreGameController preGameController;

    private final PreGameInputHandler preGameInputHandler;

    private final InGameInputHandler inGameInputHandler;

    private final ServerModelController modelController;

    private final InGameController inGameController;

    private ServerGame game;

    private AIMain aiMain;

    private IMapGenerator mapGenerator;

    private boolean singleplayer;

    // The username of the player owning this server.
    private String owner;

    private boolean publicServer = false;

    private final int port;

    /** The name of this server. */
    private String name;

    /** The private provider for random numbers. */
    private Random random = new Random(new SecureRandom().nextLong());

    /** Did the integrity check succeed */
    private boolean integrity = false;

    /**
     * The high scores on this server.
     */
    private List<HighScore> highScores = null;


    public static final Comparator<HighScore> highScoreComparator = new Comparator<HighScore>() {
        public int compare(HighScore score1, HighScore score2) {
            return score2.getScore() - score1.getScore();
        }
    };


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
    public FreeColServer(Specification specification, boolean publicServer, boolean singleplayer, int port, String name)
        throws IOException, NoRouteToServerException {
        this(specification, publicServer, singleplayer, port, name, Advantages.SELECTABLE);
    }

    public FreeColServer(Specification specification, boolean publicServer, boolean singleplayer,
                         int port, String name, Advantages advantages)
        throws IOException, NoRouteToServerException {

        this.publicServer = publicServer;
        this.singleplayer = singleplayer;
        this.port = port;
        this.name = name;

        modelController = new ServerModelController(this);
        userConnectionHandler = new UserConnectionHandler(this);
        preGameController = new PreGameController(this);
        preGameInputHandler = new PreGameInputHandler(this);
        inGameInputHandler = new InGameInputHandler(this);
        inGameController = new InGameController(this);

        game = new ServerGame(modelController, specification);
        game.setNationOptions(new NationOptions(specification, advantages));
        mapGenerator = new MapGenerator(random, specification);

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
     * @param savegame The file where the game data is located.
     * 
     * @param port The TCP port to use for the public socket. That is the port
     *            the clients will connect to.
     * 
     * @param name The name of the server, or <code>null</code> if the default
     *            name should be used.
     * 
     * @exception IOException if the public socket cannot be created (the exception
     *             will be logged by this class).
     * 
     * @exception FreeColException if the savegame could not be loaded.
     * @exception NoRouteToServerException if an error occurs
     */
    public FreeColServer(final FreeColSavegameFile savegame, int port, String name)
        throws IOException, FreeColException, NoRouteToServerException {
        this.port = port;
        this.name = name;
        //this.nationOptions = nationOptions;
        mapGenerator = null;
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
            owner = loadGame(savegame);
        } catch (FreeColException e) {
            server.shutdown();
            throw e;
        } catch (Exception e) {
            server.shutdown();
            FreeColException fe = new FreeColException("couldNotLoadGame");
            fe.initCause(e);
            throw fe;
        }
        mapGenerator = new MapGenerator(random, getSpecification());

        updateMetaServer(true);
        startMetaServerUpdateThread();
    }

    public Specification getSpecification() {
        return game.getSpecification();
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
            List<UnitType> undeads = getSpecification().getUnitTypesWithAbility("model.ability.undead");
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
                UnitType navalType = navalUnits.get(random.nextInt(navalUnits.size()));
                Unit theFlyingDutchman = new Unit(game, p.getEntryLocation(), p, navalType, UnitState.ACTIVE);
                if (landUnits.size() > 0) {
                    UnitType landType = landUnits.get(random.nextInt(landUnits.size()));
                    new Unit(game, theFlyingDutchman, p, landType, UnitState.SENTRY);
                }
                p.setDead(false);
                p.setPlayerType(PlayerType.UNDEAD);
                Element updateElement = Message.createNewRootElement("update");
                updateElement.appendChild(((FreeColGameObject) p.getEntryLocation()).toXMLElement(p, updateElement
                                                                                                  .getOwnerDocument()));
                updateElement.appendChild(p.toXMLElement(p, updateElement.getOwnerDocument()));
                try {
                    p.getConnection().sendAndWait(updateElement);
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
    public IMapGenerator getMapGenerator() {
        return mapGenerator;
    }
    
    /**
     * Sets the <code>MapGenerator</code> this <code>FreeColServer</code> is
     * using when creating random maps.
     * 
     * @param mapGenerator The <code>MapGenerator</code>.
     */
    public void setMapGenerator(IMapGenerator mapGenerator) {
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
            mc = new Connection(FreeCol.META_SERVER_ADDRESS, FreeCol.META_SERVER_PORT, null, FreeCol.SERVER_THREAD);
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
            // TODO: Add possibility of choosing a name:
            if (name != null) {
                element.setAttribute("name", name);
            } else {
                element.setAttribute("name", mc.getSocket().getLocalAddress().getHostAddress() + ":"
                                     + Integer.toString(port));
            }
            element.setAttribute("port", Integer.toString(port));
            element.setAttribute("slotsAvailable", Integer.toString(getSlotsAvailable()));
            element.setAttribute("currentlyPlaying", Integer.toString(getNumberOfLivingHumanPlayers()));
            element.setAttribute("isGameStarted", Boolean.toString(gameState != GameState.STARTING_GAME));
            element.setAttribute("version", FreeCol.getVersion());
            element.setAttribute("gameState", Integer.toString(getGameState().ordinal()));
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
            mc = new Connection(FreeCol.META_SERVER_ADDRESS, FreeCol.META_SERVER_PORT, null, FreeCol.SERVER_THREAD);
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
        int n = 0;
        for (int i = 0; i < players.size(); i++) {
            ServerPlayer p = (ServerPlayer) players.get(i);
            if (!p.isEuropean() || p.isREF()) {
                continue;
            }
            if (!(p.isDead() || p.isConnected() && !p.isAI())) {
                n++;
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
        saveGame(file, username, null);
    }

    /**
     * Saves a game.
     * 
     * @param file The file where the data will be written.
     * @param username The username of the player saving the game.
     * @param image an <code>Image</code> value
     * @exception IOException If a problem was encountered while trying to open,
     *             write or close the file.
     */
    public void saveGame(File file, String username, BufferedImage image) throws IOException {
        final Game game = getGame();
        XMLOutputFactory xof = XMLOutputFactory.newInstance();
        JarOutputStream fos = null;
        try {
            XMLStreamWriter xsw;
            fos = new JarOutputStream(new FileOutputStream(file));
            fos.putNextEntry(new JarEntry("specification.xml"));
            InputStream in = FreeCol.getSpecificationInputStream();
            int len;
            byte[] buf = new byte[1024];
            while ((len = in.read(buf)) > 0) {
                fos.write(buf, 0, len);
            }
            fos.closeEntry();
            in.close();
            if (image != null) {
                fos.putNextEntry(new JarEntry("thumbnail.png"));
                ImageIO.write(image, "png", fos);
                fos.closeEntry();
            }

            fos.putNextEntry(new JarEntry(FreeColSavegameFile.SAVEGAME_FILE));
            xsw = xof.createXMLStreamWriter(fos, "UTF-8");

            xsw.writeStartDocument("UTF-8", "1.0");
            xsw.writeComment("Game version: "+FreeCol.getRevision());
            xsw.writeStartElement("savedGame");
            
            // Add the attributes:
            xsw.writeAttribute("owner", username);
            xsw.writeAttribute("publicServer", Boolean.toString(publicServer));
            xsw.writeAttribute("singleplayer", Boolean.toString(singleplayer));
            xsw.writeAttribute("version", Integer.toString(SAVEGAME_VERSION));
            xsw.writeAttribute("randomState", getRandomState(random));
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
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                // do nothing
            }
        }
    }

    /**
     * Creates a <code>XMLStream</code> for reading the given file.
     * Compression is automatically detected.
     * 
     * @param fis The file to be read.
     * @return The <code>XMLStreamr</code>.
     * @exception IOException if thrown while loading the game or if a
     *                <code>XMLStreamException</code> have been thrown by the
     *                parser.
     */
    public static XMLStream createXMLStreamReader(FreeColSavegameFile fis) throws IOException {
        return new XMLStream(fis.getSavegameInputStream());
    }

    /**
     * Loads a game.
     * 
     * @param fis The file where the game data is located.
     * @return The username of the player saving the game.
     * @throws IOException If a problem was encountered while trying to open,
     *             read or close the file.
     * @exception IOException if thrown while loading the game or if a
     *                <code>XMLStreamException</code> have been thrown by the
     *                parser.
     * @exception FreeColException if the savegame contains incompatible data.
     */
    public String loadGame(final FreeColSavegameFile fis) throws IOException, FreeColException {
        boolean doNotLoadAI = false;
        XMLStream xs = null;
        try {
            xs = createXMLStreamReader(fis);
            final XMLStreamReader xsr = xs.getXMLStreamReader();
            xsr.nextTag();
            
            int savegameVersion = getSavegameVersion(xsr);
            logger.info("Found savegame version " + savegameVersion);
            singleplayer = FreeColObject.getAttribute(xsr, "singleplayer", true);
            publicServer =  FreeColObject.getAttribute(xsr, "publicServer", false);
            
            String randomState = xsr.getAttributeValue(null, "randomState");
            if (randomState != null && randomState.length() > 0) {
                try {
                    random = restoreRandomState(randomState);
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
                    Specification specification = null;
                    if (savegameVersion < 9) {
                        logger.info("Compatibility code: providing fresh specification.");
                        specification = new FreeColTcFile("freecol").getSpecification();
                    }
                    game = new ServerGame(null, getModelController(), xsr, serverObjects
                                          .toArray(new FreeColGameObject[0]), specification);
                    if (savegameVersion < 9) {
                        logger.info("Compatibility code: applying difficulty level.");
                        // Apply the difficulty level
                        DifficultyLevel level = game.getDifficultyLevel();
                        if (level == null) {
                            try {
                                int levelIndex = game.getGameOptions().getInteger("model.option.difficulty");
                                level = game.getSpecification().getDifficultyLevels().get(levelIndex);
                            } catch(Exception e) {
                                // no such setting
                                level = game.getSpecification().getDifficultyLevel("model.difficulty.medium");
                            }
                        }
                        logger.fine("Difficulty level is " + level.getId());
                        game.getSpecification().applyDifficultyLevel(level);
                        game.setDifficultyLevel(level);
                    }
                    game.setCurrentPlayer(null);
                    gameState = GameState.IN_GAME;
                    integrity = game.checkIntegrity();
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
            // TODO: remove compatibility code
            for (Tile tile : game.getMap().getAllTiles()) {
                TerrainGenerator.encodeStyle(tile);
            }
            // end compatibility code
            Collections.sort(game.getPlayers(), Player.playerComparator);
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
                    getServer().addDummyConnection(theConnection);
                    player.setConnection(theConnection);
                    player.setConnected(true);
                }
            }
            xs.close();
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
        } finally {
            xs.close();
        }
    }

    public static int getSavegameVersion(final XMLStreamReader xsr) throws FreeColException {
        final String version = xsr.getAttributeValue(null, "version");
        int savegameVersion = 0;
        try {
            savegameVersion = Integer.parseInt(version);
        } catch(Exception e) {
            throw new FreeColException("incompatibleVersions");
        }
        if (savegameVersion < MINIMUM_SAVEGAME_VERSION) {
            throw new FreeColException("incompatibleVersions");
        }
        return savegameVersion;
    }
	
    /**
     * Removes automatically created save games.
     * Call this function to delete the automatically created save games from
     * a previous game.
     */
    public static void removeAutosaves(final String prefix) {
        for (File autosaveFile : FreeCol.getAutosaveDirectory().listFiles()) {
            if (autosaveFile.getName().startsWith(prefix)) {
                autosaveFile.delete();
            }
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
        if (getGameState() == GameState.IN_GAME) {
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
     * Gets the <code>Game</code> that is being played.
     * 
     * @return The <code>Game</code> which is the main class of the game-model
     *         being used in this game.
     */
    public ServerGame getGame() {
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
     * @return One of: {@link GameState#STARTING_GAME}, {@link GameState#IN_GAME} and
     *         {@link GameState#ENDING_GAME}.
     */
    public GameState getGameState() {
        return gameState;
    }

    /**
     * Sets the current state of the game.
     * 
     * @param state The new state to be set. One of: {@link GameState#STARTING_GAME},
     *            {@link GameState#IN_GAME} and {@link GameState#ENDING_GAME}.
     */
    public void setGameState(GameState state) {
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
     * Gets the integrity check result.
     *
     * @return The integrity check result.
     */
    public boolean getIntegrity() {
        return integrity;
    }

    /**
     * Get the server-private random number generator.
     *
     * @return The server-private random number generator.
     */
    public Random getServerRandom() {
        return random;
    }


    /**
     * Get a unit by ID, validating the ID as much as possible.  Designed for
     * message unpacking where the ID should not be trusted.
     *
     * @param unitId The ID of the unit to be found.
     * @param serverPlayer The <code>ServerPlayer</code> to whom the unit must belong.
     *
     * @return The unit corresponding to the unitId argument.
     * @throws IllegalStateException on failure to validate the unitId
     *         in any way.
     *         In the worst case this may be indicative of a malign client.
     */
    public Unit getUnitSafely(String unitId, ServerPlayer serverPlayer)
        throws IllegalStateException {
        Game game = serverPlayer.getGame();
        FreeColGameObject obj;
        Unit unit;

        if (unitId == null || unitId.length() == 0) {
            throw new IllegalStateException("ID must not be empty.");
        }
        obj = game.getFreeColGameObjectSafely(unitId);
        if (obj == null) {
            throw new IllegalStateException("Not an object: " + unitId);
        } else if (!(obj instanceof Unit)) {
            throw new IllegalStateException("Unit expected, "
                                            + " got " + obj.getClass()
                                            + ": " + unitId);
        }
        unit = (Unit) obj;
        if (unit.getOwner() != serverPlayer) {
            throw new IllegalStateException("Not the owner of unit: " + unitId);
        }
        return unit;
    }

    /**
     * Get a settlement by ID, validating the ID as much as possible.
     * Designed for message unpacking where the ID should not be trusted.
     *
     * @param settlementId The ID of the <code>Settlement</code> to be found.
     * @param unit A <code>Unit</code> which must be adjacent
     *             to the <code>Settlement</code>.
     *
     * @return The settlement corresponding to the settlementId argument.
     * @throws IllegalStateException on failure to validate the settlementId
     *         in any way.
     *         In the worst case this may be indicative of a malign client.
     */
    public Settlement getAdjacentSettlementSafely(String settlementId, Unit unit)
        throws IllegalStateException {
        Game game = unit.getOwner().getGame();
        Settlement settlement;

        if (settlementId == null || settlementId.length() == 0) {
            throw new IllegalStateException("ID must not be empty.");
        } else if (!(game.getFreeColGameObject(settlementId) instanceof Settlement)) {
            throw new IllegalStateException("Not a settlement ID: " + settlementId);
        }
        settlement = (Settlement) game.getFreeColGameObject(settlementId);
        if (settlement.getTile() == null) {
            throw new IllegalStateException("Settlement is not on the map: "
                                            + settlementId);
        }
        if (unit.getTile() == null) {
            throw new IllegalStateException("Unit is not on the map: "
                                            + unit.getId());
        }
        if (unit.getTile().getDistanceTo(settlement.getTile()) > 1) {
            throw new IllegalStateException("Unit " + unit.getId()
                                            + " is not adjacent to settlement: " + settlementId);
        }
        if (unit.getOwner() == settlement.getOwner()) {
            throw new IllegalStateException("Unit: " + unit.getId()
                                            + " and settlement: " + settlementId
                                            + " are both owned by player: "
                                            + unit.getOwner().getId());
        }
        return settlement;
    }

    /**
     * Get an adjacent Indian settlement by ID, validating as much as possible,
     * including checking whether the nation involved has been contacted.
     * Designed for message unpacking where the ID should not be trusted.
     *
     * @param settlementId The ID of the <code>Settlement</code> to be found.
     * @param unit A <code>Unit</code> which must be adjacent
     *             to the <code>Settlement</code>.
     *
     * @return The settlement corresponding to the settlementId argument.
     * @throws IllegalStateException on failure to validate the settlementId
     *         in any way.
     *         In the worst case this may be indicative of a malign client.
     */
    public IndianSettlement getAdjacentIndianSettlementSafely(String settlementId, Unit unit)
        throws IllegalStateException {
        Settlement settlement = getAdjacentSettlementSafely(settlementId, unit);
        if (!(settlement instanceof IndianSettlement)) {
            throw new IllegalStateException("Not an indianSettlement: " + settlementId);
        }
        if (!unit.getOwner().hasContacted(settlement.getOwner())) {
            throw new IllegalStateException("Player has not established contact with the "
                                            + settlement.getOwner().getNation());
        }
        return (IndianSettlement) settlement;
    }


    /**
     * Adds a new AIPlayer to the Game.
     *
     * @param nation a <code>Nation</code> value
     * @return a <code>ServerPlayer</code> value
     */
    public ServerPlayer addAIPlayer(Nation nation) {
        String name = nation.getRulerNameKey();
        DummyConnection theConnection = 
            new DummyConnection("Server connection - " + name, getInGameInputHandler());
        ServerPlayer aiPlayer = 
            new ServerPlayer(getGame(), name, false, true, null, theConnection, nation);
        DummyConnection aiConnection = 
            new DummyConnection("AI connection - " + name,
                                new AIInGameInputHandler(this, aiPlayer, getAIMain()));
            
        aiConnection.setOutgoingMessageHandler(theConnection);
        theConnection.setOutgoingMessageHandler(aiConnection);

        getServer().addDummyConnection(theConnection);

        getGame().addPlayer(aiPlayer);

        // Send message to all players except to the new player:
        Element addNewPlayer = Message.createNewRootElement("addPlayer");
        addNewPlayer.appendChild(aiPlayer.toXMLElement(null, addNewPlayer.getOwnerDocument()));
        getServer().sendToAll(addNewPlayer, theConnection);
        return aiPlayer;
    }

    /**
     * Get the <code>HighScores</code> value.
     *
     * @return a <code>List<HighScore></code> value
     */
    public List<HighScore> getHighScores() {
        if (highScores == null) {
            try {
                loadHighScores();
            } catch (Exception e) {
                logger.warning(e.toString());
                highScores = new ArrayList<HighScore>();
            }
        }
        return highScores;
    }

    /**
     * Adds a new high score for player and returns <code>true</code>
     * if possible.
     *
     * @param player a <code>Player</code> value
     * @return a <code>boolean</code> value
     */
    public boolean newHighScore(Player player) {
        getHighScores();
        if (!highScores.isEmpty() && player.getScore() <= highScores.get(highScores.size() - 1).getScore()) {
            return false;
        } else {
            highScores.add(new HighScore(player, new Date()));
            Collections.sort(highScores, highScoreComparator);
            if (highScores.size() == NUMBER_OF_HIGH_SCORES) {
                highScores.remove(NUMBER_OF_HIGH_SCORES - 1);
            }
            return true;
        }
    }

    /**
     * Saves high scores.
     * 
     * @throws IOException If a problem was encountered while trying to open,
     *             write or close the file.
     */
    public void saveHighScores() throws IOException {
        if (highScores == null || highScores.isEmpty()) {
            return;
        }
        Collections.sort(highScores, highScoreComparator);
        XMLOutputFactory xof = XMLOutputFactory.newInstance();
        FileOutputStream fos = null;
        try {
            XMLStreamWriter xsw;
            fos = new FileOutputStream(new File(FreeCol.getDataDirectory(), HIGH_SCORE_FILE));

            xsw = xof.createXMLStreamWriter(fos, "UTF-8");
            xsw.writeStartDocument("UTF-8", "1.0");
            xsw.writeStartElement("highScores");
            int count = 0;
            for (HighScore score : highScores) {
                score.toXML(xsw);
                count++;
                if (count == NUMBER_OF_HIGH_SCORES) {
                    break;
                }
            }
            xsw.writeEndElement();
            xsw.writeEndDocument();
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
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                // do nothing
            }
        }
    }

    /**
     * Loads high scores.
     * 
     * @throws IOException If a problem was encountered while trying to open,
     *             read or close the file.
     * @exception IOException if thrown while loading the game or if a
     *                <code>XMLStreamException</code> have been thrown by the
     *                parser.
     * @exception FreeColException if the savegame contains incompatible data.
     */
    public void loadHighScores() throws IOException, FreeColException {
        highScores = new ArrayList<HighScore>();
        File hsf = new File(FreeCol.getDataDirectory(), HIGH_SCORE_FILE);
        if (!hsf.exists()) return;
        XMLInputFactory xif = XMLInputFactory.newInstance();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(hsf);
            XMLStreamReader xsr = xif.createXMLStreamReader(fis, "UTF-8");
            xsr.nextTag();
            while (xsr.nextTag() != XMLStreamConstants.END_ELEMENT) {
                if (xsr.getLocalName().equals("highScore")) {
                    highScores.add(new HighScore(xsr));
                }
            }
            xsr.close();
            Collections.sort(highScores, highScoreComparator);
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
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
    }


    /** Hex constant digits for get/restoreRandomState. */
    private static final String HEX_DIGITS = "0123456789ABCDEF";

    /**
     * Get the internal state of a random number generator as a
     * string.  It would have been more convenient to simply return
     * the current seed, but unfortunately it is private.
     *
     * @param random The <code>Random</code> to use.
     * @return A <code>String</code> encapsulating the object state.
     */
    public synchronized String getRandomState(Random random) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(random);
            oos.flush();
        } catch (IOException e) {
            throw new IllegalStateException("IO exception in memory!?", e);
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
     * @param state The saved state (@see #getRandomState()).
     * @return The restored <code>Random</code>.
     * @throws IOException if unable to restore state.
     */
    public synchronized Random restoreRandomState(String state)
        throws IOException {
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
            return (Random) ois.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException("Failed to restore ServerRandom!");
        }
    }

}
