/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.BindException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.logging.Level;
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
import net.sf.freecol.common.FreeColSeed;
import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.io.FreeColSavegameFile;
import net.sf.freecol.common.io.FreeColTcFile;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.HighScore;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.NationOptions;
import net.sf.freecol.common.model.NationOptions.Advantages;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.DOMMessage;
import net.sf.freecol.common.networking.NoRouteToServerException;
import net.sf.freecol.common.option.AbstractOption;
import net.sf.freecol.common.option.BooleanOption;
import net.sf.freecol.common.option.IntegerOption;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.common.option.StringOption;
import net.sf.freecol.common.util.Utils;
import net.sf.freecol.common.util.XMLStream;
import net.sf.freecol.server.ai.AIInGameInputHandler;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.control.Controller;
import net.sf.freecol.server.control.InGameController;
import net.sf.freecol.server.control.InGameInputHandler;
import net.sf.freecol.server.control.PreGameController;
import net.sf.freecol.server.control.PreGameInputHandler;
import net.sf.freecol.server.control.UserConnectionHandler;
import net.sf.freecol.server.generator.MapGenerator;
import net.sf.freecol.server.generator.SimpleMapGenerator;
import net.sf.freecol.server.generator.TerrainGenerator;
import net.sf.freecol.server.model.ServerGame;
import net.sf.freecol.server.model.ServerModelObject;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.model.TransactionSession;
import net.sf.freecol.server.networking.DummyConnection;
import net.sf.freecol.server.networking.Server;

import org.w3c.dom.Element;


/**
 * The main control class for the FreeCol server. This class both starts and
 * keeps references to all of the server objects and the game model objects.
 *
 * If you would like to start a new server you just create a new
 * object of this class.
 */
public final class FreeColServer {

    private static final Logger logger = Logger.getLogger(FreeColServer.class.getName());

    private static final int META_SERVER_UPDATE_INTERVAL = 60000;

    private static final int NUMBER_OF_HIGH_SCORES = 10;
    /**
     * The save game format used for saving games.
     *
     * Version 7-10 were used in 0.9.x.
     * Version 11 made a lot of changes and was introduced for the 0.10.0
     *     series.
     * Version 12 was introduced with HighSeas post-0.10.1.
     *
     * Please add to this comment if you increase the version.
     */
    public static final int SAVEGAME_VERSION = 12;

    /**
     * The oldest save game format that can still be loaded.
     * The promise is that FreeCol 0.n.* can load 0.(n-1).* games.
     *
     * TODO: revisit the numbering scheme and save compatibility promise
     * when 1.0 is released.
     */
    public static final int MINIMUM_SAVEGAME_VERSION = 7;

    /**
     * The specification to use when loading old format games where
     * a spec may not be readily available.
     */
    public static final String defaultSpec = "freecol";

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

    private final InGameController inGameController;

    private ServerGame game;

    private AIMain aiMain;

    private MapGenerator mapGenerator;

    private boolean singlePlayer;

    // The username of the player owning this server.
    private String owner;

    private boolean publicServer = false;

    /** The port the server is available at. */
    private int port;

    /** The name of this server. */
    private String name;

    /** The private provider for random numbers. */
    private Random random = null;

    /** Did the integrity check succeed */
    private boolean integrity = false;

    /** An active unit specified in a saved game. */
    private Unit activeUnit = null;

    /**
     * The high scores on this server.
     */
    private List<HighScore> highScores = null;

    public static final Comparator<HighScore> highScoreComparator
        = new Comparator<HighScore>() {
        public int compare(HighScore score1, HighScore score2) {
            return score2.getScore() - score1.getScore();
        }
    };

    /**
     * Start a Server at port.
     * If the port is specified, just try once.
     * If the port is unspecified (negative), try multiple times.
     *
     * @param firstPort The port to start trying to connect at.
     * @return A started <code>Server</code>.
     * @throws IOException on failure to open the port.
     */
    private Server serverStart(int firstPort) throws IOException {
        int port, tries;
        if (firstPort < 0) {
            port = FreeCol.getDefaultPort();
            tries = 10;
        } else {
            port = firstPort;
            tries = 1;
        }
        logger.finest("serverStart(" + firstPort + ") => " + port
            + " x " + tries);
        for (int i = tries; i > 0; i--) {
            try {
                server = new Server(this, port);
                this.port = port;
                server.start();
                break;
            } catch (BindException be) {
                if (i == 1) {
                    logger.log(Level.WARNING, "Bind exception starting server.",
                        be);
                    throw new IOException(be.getMessage());
                }
            } catch (IOException ie) {
                if (i == 1) {
                    logger.log(Level.WARNING, "IO exception starting server.",
                        ie);
                    throw ie;
                }
            }
            port++;
        }
        return server;
    }

    /**
     * Starts a new server in a specified mode and with a specified port.
     *
     * @param publicServer This value should be set to <code>true</code> in
     *            order to appear on the meta server's listing.
     *
     * @param singlePlayer Sets the game as single player (if <i>true</i>) or
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
    public FreeColServer(Specification specification, boolean publicServer,
                         boolean singlePlayer, int port, String name)
        throws IOException, NoRouteToServerException {
        this(specification, publicServer, singlePlayer, port, name,
             Advantages.SELECTABLE);
    }

    public FreeColServer(Specification specification, boolean publicServer,
                         boolean singlePlayer, int port, String name,
                         Advantages advantages)
        throws IOException, NoRouteToServerException {

        this.publicServer = publicServer;
        this.singlePlayer = singlePlayer;
        this.port = port;
        this.name = name;
        this.random = new Random(FreeColSeed.getFreeColSeed());

        userConnectionHandler = new UserConnectionHandler(this);
        preGameController = new PreGameController(this);
        preGameInputHandler = new PreGameInputHandler(this);
        inGameInputHandler = new InGameInputHandler(this);
        inGameController = new InGameController(this, random);

        game = new ServerGame(specification);
        game.setNationOptions(new NationOptions(specification, advantages));
        // @compat 0.9.x, 0.10.x
        fixGameOptions();
        // end compatibility code

        mapGenerator = new SimpleMapGenerator(random, specification);

        server = serverStart(port);

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
        this(savegame, port, name, null);
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
     * @param specification a <code>Specification</code> value
     * @exception IOException if the public socket cannot be created (the exception
     *             will be logged by this class).
     *
     * @exception FreeColException if the savegame could not be loaded.
     * @exception NoRouteToServerException if an error occurs
     */
    public FreeColServer(final FreeColSavegameFile savegame, int port,
                         String name, Specification specification)
        throws IOException, FreeColException, NoRouteToServerException {
        this.port = port;
        this.name = name;
        //this.nationOptions = nationOptions;
        mapGenerator = null;
        userConnectionHandler = new UserConnectionHandler(this);
        preGameController = new PreGameController(this);
        preGameInputHandler = new PreGameInputHandler(this);
        inGameInputHandler = new InGameInputHandler(this);

        server = serverStart(port);

        try {
            this.game = loadGame(savegame, specification);
        } catch (FreeColException e) {
            server.shutdown();
            throw e;
        } catch (Exception e) {
            server.shutdown();
            FreeColException fe = new FreeColException("couldNotLoadGame");
            fe.initCause(e);
            throw fe;
        }
        if (random == null) {
            this.random = new Random(FreeColSeed.getFreeColSeed());
        }
        inGameController = new InGameController(this, random);
        mapGenerator = new SimpleMapGenerator(random, getSpecification());

        updateMetaServer(true);
        startMetaServerUpdateThread();
        TransactionSession.clearAll();
    }

    /**
     * Gets the port this server was started on.
     *
     * @return The port.
     */
    public int getPort() {
        return this.port;
    }

    /**
     * Sets the mode of the game: single/multiplayer.
     *
     * @param singlePlayer Sets the game as single player (if <i>true</i>) or
     *            multiplayer (if <i>false</i>).
     */
    public void setSinglePlayer(boolean singlePlayer) {
        this.singlePlayer = singlePlayer;
    }

    /**
     * Checks if the user is playing in single player mode.
     *
     * @return <i>true</i> if the user is playing in single player mode,
     *         <i>false</i> otherwise.
     */
    public boolean isSinglePlayer() {
        return singlePlayer;
    }

    /**
     * Gets the specification from the game run by this server.
     *
     * @return The specification from the game.
     */
    public Specification getSpecification() {
        return game.getSpecification();
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
     * Gets the <code>Game</code> that is being played.
     *
     * @return The <code>Game</code> which is the main class of the game-model
     *         being used in this game.
     */
    public ServerGame getGame() {
        return game;
    }

    /**
     * Sets the <code>Game</code> that is being played.
     *
     * @param game The new <code>Game</code>.
     */
    public void setGame(ServerGame game) {
        this.game = game;
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
     * Gets the server random number generator.
     *
     * @return The server random number generator.
     */
    public Random getServerRandom() {
        return random;
    }

    /**
     * Sets the server random number generator.
     *
     * @param random The new random number generator.
     */
    public void setServerRandom(Random random) {
        this.random = random;
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
     * Gets the owner of the <code>Game</code>.
     *
     * @return The owner of the game.
     */
    public String getOwner() {
        return owner;
    }

    /**
     * Sets the owner of the game.
     *
     * @param owner The new owner of the game.
     */
    public void setOwner(String owner) {
        this.owner = owner;
    }

    /**
     * Gets the active unit specified in a saved game, if any.
     *
     * @return The active unit.
     */
    public Unit getActiveUnit() {
        return activeUnit;
    }

    /**
     * Gets the active unit specified in a saved game, if any.
     *
     * @param unit The active unit to save.
     */
    public void setActiveUnit(Unit unit) {
        activeUnit = unit;
    }

    /**
     * Sets the public server.
     *
     * @param publicServer The new public server flag.
     */
    public void setPublicServer(boolean publicServer) {
        this.publicServer = publicServer;
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
     * Sends information about this server to the meta-server. The information
     * is only sent if <code>public == true</code>.
     *
     * @param firstTime Should be set to <i>true></i> when calling this method
     *      for the first time.
     * @throws NoRouteToServerException if the meta-server cannot connect to
     *      this server.
     */
    public void updateMetaServer(boolean firstTime)
        throws NoRouteToServerException {
        if (!publicServer) return;

        Connection mc;
        try {
            mc = new Connection(FreeCol.META_SERVER_ADDRESS,
                                FreeCol.META_SERVER_PORT, null,
                                FreeCol.SERVER_THREAD);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not connect to meta-server.", e);
            return;
        }

        String tag = (firstTime) ? "register" : "update";
        String addr = (name != null) ? name
            : (mc.getSocket().getLocalAddress().getHostAddress() + ":"
                + Integer.toString(port));
        int nPlayers = getNumberOfLivingHumanPlayers();
        boolean started = gameState != GameState.STARTING_GAME;
        try {
            Element reply = mc.askDumping(DOMMessage.createMessage(tag,
                    "name", addr,
                    "port", Integer.toString(port),
                    "slotsAvailable", Integer.toString(getSlotsAvailable()),
                    "currentlyPlaying", Integer.toString(nPlayers),
                    "isGameStarted", Boolean.toString(started),
                    "version", FreeCol.getVersion(),
                    "gameState", Integer.toString(getGameState().ordinal())));
            if (reply != null
                && reply.getTagName().equals("noRouteToServer")) {
                throw new NoRouteToServerException();
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Network error with meta-server.", e);
            return;
        } finally {
            mc.close();
        }
    }

    /**
     * Removes this server from the metaserver's list. The information is only
     * sent if <code>public == true</code>.
     */
    public void removeFromMetaServer() {
        if (!publicServer) return;

        Connection mc;
        try {
            mc = new Connection(FreeCol.META_SERVER_ADDRESS,
                                FreeCol.META_SERVER_PORT,
                                null, FreeCol.SERVER_THREAD);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not connect to meta-server.", e);
            return;
        }

        try {
            mc.sendDumping(DOMMessage.createMessage("remove",
                    "port", Integer.toString(port)));
        } catch (IOException e) {
            logger.log(Level.WARNING, "Network error with meta-server.", e);
            return;
        } finally {
            mc.close();
        }
    }

    /**
     * Gets the number of player that may connect.
     *
     * @return The number of available slots for human players.  This
     *     number also includes european players currently controlled
     *     by the AI.
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
     * @return The number of living human players.
     */
    public int getNumberOfLivingHumanPlayers() {
        int n = 0;
        for (Player p : game.getPlayers()) {
            ServerPlayer serverPlayer = (ServerPlayer)p;
            if (!serverPlayer.isAI()
                && !serverPlayer.isDead()
                && !serverPlayer.isConnected()) n++;
        }
        return n;
    }

    /**
     * Saves a game.
     *
     * @param file The file where the data will be written.
     * @param username The username of the player saving the game.
     * @throws IOException If a problem was encountered while trying to open,
     *             write or close the file.
     */
    public void saveGame(File file, String username, OptionGroup options)
        throws IOException {
        saveGame(file, username, options, null);
    }

    /**
     * Saves a game.
     *
     * @param file The file where the data will be written.
     * @param username The username of the player saving the game.
     * @param image an <code>Image</code> value
     * @throws IOException If a problem was encountered while trying
     *     to open, write or close the file.
     */
    public void saveGame(File file, String username, OptionGroup options,
                         BufferedImage image)
        throws IOException {
        final ServerGame game = getGame();
        XMLOutputFactory xof = XMLOutputFactory.newInstance();
        JarOutputStream fos = null;
        try {
            XMLStreamWriter xsw;
            fos = new JarOutputStream(new FileOutputStream(file));

            if (image != null) {
                fos.putNextEntry(new JarEntry(FreeColSavegameFile.THUMBNAIL_FILE));
                ImageIO.write(image, "png", fos);
                fos.closeEntry();
            }

            if (options != null) {
                fos.putNextEntry(new JarEntry(FreeColSavegameFile.CLIENT_OPTIONS));
                options.save(fos);
                fos.closeEntry();
            }

            Properties properties = new Properties();
            properties.put("map.width", Integer.toString(game.getMap().getWidth()));
            properties.put("map.height", Integer.toString(game.getMap().getHeight()));
            fos.putNextEntry(new JarEntry(FreeColSavegameFile.SAVEGAME_PROPERTIES));
            properties.store(fos, null);
            fos.closeEntry();

            // save the actual game data
            fos.putNextEntry(new JarEntry(FreeColSavegameFile.SAVEGAME_FILE));
            xsw = xof.createXMLStreamWriter(fos, "UTF-8");

            xsw.writeStartDocument("UTF-8", "1.0");
            xsw.writeComment("Game version: "+FreeCol.getRevision());
            xsw.writeStartElement("savedGame");

            // Add the attributes:
            xsw.writeAttribute("owner", username);
            xsw.writeAttribute("publicServer", Boolean.toString(publicServer));
            xsw.writeAttribute("singleplayer", Boolean.toString(singlePlayer));
            xsw.writeAttribute("version", Integer.toString(SAVEGAME_VERSION));
            xsw.writeAttribute("randomState", Utils.getRandomState(random));
            if (getActiveUnit() != null) {
                xsw.writeAttribute("activeUnit", getActiveUnit().getId());
            }
            // Add server side model information:
            xsw.writeStartElement("serverObjects");
            for (ServerModelObject smo : game.getServerModelObjects()) {
                xsw.writeStartElement(smo.getServerXMLElementTagName());
                xsw.writeAttribute(FreeColObject.ID_ATTRIBUTE,
                    ((FreeColGameObject) smo).getId());
                xsw.writeEndElement();
            }
            xsw.writeEndElement();
            // Add the game:
            game.toXML(xsw, null, true, true);
            // Add the AIObjects:
            if (aiMain != null) {
                aiMain.toXML(xsw);
            }
            xsw.writeEndElement();
            xsw.writeEndDocument();
            xsw.flush();
            xsw.close();
        } catch (XMLStreamException e) {
            throw new IOException("XMLStreamException: " + e.getMessage());
        } catch (Exception e) {
            throw new IOException("Exception: " + e.getMessage());
        } finally {
            try {
                if (fos != null) fos.close();
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
    public static XMLStream createXMLStreamReader(FreeColSavegameFile fis)
        throws IOException {
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
    public ServerGame loadGame(final FreeColSavegameFile fis)
        throws IOException, FreeColException {
        return loadGame(fis, null);
    }

    /**
     * Reads just the game part from a save game.
     * This routine exists apart from loadGame so that the map generator
     * can load the predefined maps.
     *
     * @param fis The stream to read from.
     * @param specification The <code>Specification</code> to use in the game.
     * @param server Use this (optional) server to load into.
     * @return The game found in the stream.
     * @exception FreeColException if the format is incompatible.
     * @exception IOException if there is a problem reading the stream.
     */
    public static ServerGame readGame(final FreeColSavegameFile fis,
                                      Specification specification,
                                      FreeColServer server)
        throws IOException, FreeColException {
        final int savegameVersion = getSavegameVersion(fis);
        ArrayList<String> serverStrings = null;
        XMLStream xs = null;
        ServerGame game = null;
        try {
            String active = null;
            xs = createXMLStreamReader(fis);
            final XMLStreamReader xsr = xs.getXMLStreamReader();
            xsr.nextTag();

            logger.info("Found savegame version " + savegameVersion);

            if (server != null) {
                server.setSinglePlayer(FreeColObject.getAttribute(xsr,
                        "singleplayer", true));
                server.setPublicServer(FreeColObject.getAttribute(xsr,
                        "publicServer", false));
                String randomState = xsr.getAttributeValue(null, "randomState");
                server.setServerRandom(Utils.restoreRandomState(randomState));
                server.setOwner(xsr.getAttributeValue(null, "owner"));
                active = xsr.getAttributeValue(null, "activeUnit");
            }

            while (xsr.nextTag() != XMLStreamConstants.END_ELEMENT) {
                if (xsr.getLocalName().equals("serverObjects")) {
                    serverStrings = new ArrayList<String>();
                    while (xsr.nextTag() != XMLStreamConstants.END_ELEMENT) {
                        serverStrings.add(xsr.getLocalName());
                        serverStrings.add(xsr.getAttributeValue(null,
                                FreeColObject.ID_ATTRIBUTE));
                        xsr.nextTag();
                    }
                    // @compat 0.9.x
                    if (savegameVersion < 11) {
                        v11FixServerObjects(serverStrings, fis);
                    }
                    // end compatibility code
                } else if (xsr.getLocalName().equals(Game.getXMLElementTagName())) {
                    // @compat 0.9.x
                    if (savegameVersion < 9 && specification == null) {
                        specification = new FreeColTcFile(defaultSpec)
                            .getSpecification();
                        logger.info("Reading old format game"
                            + " (version: " + savegameVersion
                            + "), using " + defaultSpec + " specification.");
                    }
                    // end compatibility code
                    // Read the game
                    game = new ServerGame(null, xsr, serverStrings,
                        specification);
                    game.setCurrentPlayer(null);
                    if (server != null) server.setGame(game);
                    // @compat 0.9.x
                    if (savegameVersion < 9
                        && game.getDifficultyLevel() == null) {
                        game.getSpecification().applyDifficultyLevel("model.difficulty.medium");
                        logger.info("Applying default difficulty of medium.");
                    }
                    // end compatibility code
                } else if (xsr.getLocalName().equals(AIMain.getXMLElementTagName())) {
                    if (server == null) break;
                    server.setAIMain(new AIMain(server, xsr));
                } else {
                    throw new XMLStreamException("Unknown tag: "
                        + xsr.getLocalName());
                }
            }

            if (server != null && active != null && game != null) {
                // Now units are all present, set active unit.
                Unit u = game.getFreeColGameObject(active, Unit.class);
                server.setActiveUnit(u);
            }
        } catch (Exception e) {
            throw new IOException("Exception: " + e.getMessage());
        } finally {
            if (xs != null) xs.close();
        }
        return game;
    }

    /**
     * Loads a game.
     *
     * @param fis The file where the game data is located.
     * @param specification a <code>Specification</code> value
     * @return The new game.
     * @exception FreeColException if the savegame contains incompatible data.
     * @exception IOException if there is problem reading a stream.
     */
    public ServerGame loadGame(final FreeColSavegameFile fis,
                               Specification specification)
        throws FreeColException, IOException {

        ServerGame game = readGame(fis, specification, this);
        gameState = GameState.IN_GAME;
        integrity = game.checkIntegrity();

        int savegameVersion = getSavegameVersion(fis);
        // @compat 0.10.x
        if (savegameVersion < 12) {
            for (Player p : game.getPlayers()) {
                if(!p.isIndian() && p.getEurope() != null) {
                    p.initializeHighSeas();

                    for (Unit u : p.getEurope().getUnitList()) {
                        // move units to high seas use setLocation()
                        // so that units are removed from Europe, and
                        // appear in correct panes in the EuropePanel
                        // do not set the UnitState, as this clears
                        // workLeft
                        if (u.getState() == Unit.UnitState.TO_EUROPE) {
                            logger.info("Found unit on way to europe: "
                                + u.toString());
                            u.setLocation(p.getHighSeas());
                            u.setDestination(p.getEurope());
                        } else if (u.getState() == Unit.UnitState.TO_AMERICA) {
                            logger.info("Found unit on way to new world: "
                                + u.toString());
                            u.setLocation(p.getHighSeas());
                            u.setDestination(game.getMap());
                        }
                    }
                }
            }

            for (Tile tile : game.getMap().getAllTiles()) {
                TerrainGenerator.encodeStyle(tile);
            }
        }
        // end compatibility code

        // @compat 0.9.x, 0.10.x
        fixGameOptions();
        // end compatibility code

        // @compat 0.9.x
        if (savegameVersion < 11) {
            for (Tile t : game.getMap().getAllTiles()) t.fixup09x();
        }
        // end compatibility code

        // @compat 0.10.x
        game.getMap().resetContiguity();
        // end compatibility code

        // ensure that option groups can not be edited
        game.getMapGeneratorOptions().setEditable(false);
        try {
            specification.getOptionGroup("gameOptions").setEditable(false);
            specification.getOptionGroup("difficultyLevels").setEditable(false);
        } catch(Exception e) {
            logger.warning("Failed to make option groups read-only");
        }

        // AI initialization.
        AIMain aiMain = getAIMain();
        if (aiMain.checkIntegrity()) {
            logger.info("AI integrity test succeeded.");
        } else if (aiMain.fixIntegrity()) {
            logger.info("AI integrity test failed, but fixed.");
        } else {
            aiMain = new AIMain(this);
            logger.warning("AI integrity test failed, replaced AIMain.");
        }
        game.setFreeColGameObjectListener(aiMain);

        Collections.sort(game.getPlayers(), Player.playerComparator);
        for (Player player : game.getPlayers()) {
            if (player.isAI()) {
                ServerPlayer serverPlayer = (ServerPlayer) player;
                DummyConnection theConnection
                    = new DummyConnection("Server-Server-" + player.getName(),
                        getInGameInputHandler());
                DummyConnection aiConnection
                    = new DummyConnection("Server-AI-" + player.getName(),
                        new AIInGameInputHandler(this, serverPlayer, aiMain));
                aiConnection.setOutgoingMessageHandler(theConnection);
                theConnection.setOutgoingMessageHandler(aiConnection);
                getServer().addDummyConnection(theConnection);
                serverPlayer.setConnection(theConnection);
                serverPlayer.setConnected(true);
            }
        }

        return game;
    }

    /**
     * Gets the save game version from a saved game.
     *
     * @param fis The saved game.
     * @return The saved game version.
     * @exception FreeColException if the version is incompatible.
     */
    private static int getSavegameVersion(final FreeColSavegameFile fis)
        throws FreeColException {
        XMLStream xs = null;
        try {
            xs = createXMLStreamReader(fis);
            final XMLStreamReader xsr = xs.getXMLStreamReader();
            xsr.nextTag();

            final String version = xsr.getAttributeValue(null, "version");
            int savegameVersion = 0;
            try {
                savegameVersion = Integer.parseInt(version);
            } catch (Exception e) {
                throw new FreeColException("incompatibleVersions");
            }
            if (savegameVersion < MINIMUM_SAVEGAME_VERSION) {
                throw new FreeColException("incompatibleVersions");
            }
            return savegameVersion;
        } catch (Exception e) {
            throw new FreeColException(e.toString());
        } finally {
            if (xs != null) xs.close();
        }
    }

    private void fixGameOptions() {
        // @compat 0.9.x, 0.10.x
        // Add a default value for options new to each version
        // that are not part of the difficulty settings.
        // Annotate with save format version where introduced
        Specification spec = game.getSpecification();

        // @compat 0.9.x
        // Introduced: SAVEGAME_VERSION == 11
        addIntegerOption("model.option.monarchSupport",
            "model.difficulty.monarch", 2, true);
        // Introduced: SAVEGAME_VERSION == 11
        addStringOption("model.option.buildOnNativeLand",
            "model.difficulty.natives",
            "model.option.buildOnNativeLand.never", true);
        // Introduced: SAVEGAME_VERSION == 11
        if (!spec.hasOption("model.option.amphibiousMoves")) {
            addBooleanOption("model.option.amphibiousMoves",
                "gameOptions.map", false, false);
            spec.addModifier(new Modifier("model.modifier.amphibiousAttack",
                    Specification.AMPHIBIOUS_ATTACK_PENALTY_SOURCE,
                    -75.0f,
                    Modifier.Type.PERCENTAGE));
        }
        // Introduced: SAVEGAME_VERSION == 11
        addBooleanOption("model.option.settlementActionsContactChief",
            "gameOptions.map", false, false);

        // @compat 0.10.x
        // Introduced: SAVEGAME_VERSION == 12
        addBooleanOption("model.option.enhancedMissionaries",
            "gameOptions.map", false, false);
        // Introduced: SAVEGAME_VERSION == 12
        addBooleanOption("model.option.continueFoundingFatherRecruitment",
            "gameOptions.map", false, false);
        // Introduced: SAVEGAME_VERSION == 12
        addIntegerOption("model.option.settlementLimitModifier",
            "gameOptions.map", 0, false);
        // Introduced: SAVEGAME_VERSION == 12
        addIntegerOption("model.option.startingPositions",
            "gameOptions.map", 0, false);
        // Introduced: SAVEGAME_VERSION == 12
        addBooleanOption(GameOptions.TELEPORT_REF,
            "gameOptions.map", false, false);
        // Introduced: SAVEGAME_VERSION == 12
        addIntegerOption(GameOptions.SHIP_TRADE_PENALTY,
            "gameOptions.map", -30, false);
        if (spec.getModifiers("model.modifier.shipTradePenalty") == null) {
            spec.addModifier(new Modifier("model.modifier.shipTradePenalty",
                    Specification.SHIP_TRADE_PENALTY_SOURCE,
                    -30.0f, Modifier.Type.PERCENTAGE));
        }
        // Introduced: SAVEGAME_VERSION == 12
        addBooleanOption(GameOptions.ENABLE_UPKEEP,
            "gameOptions.colony", false, false);
        addIntegerOption(GameOptions.NATURAL_DISASTERS,
            "gameOptions.colony", 0, false);
        // Introduced: SAVEGAME_VERSION == 12
        addOptionGroup("model.difficulty.cheat", true);
        addIntegerOption("model.option.liftBoycottCheat",
            "model.difficulty.cheat", 10, true);
        addIntegerOption("model.option.equipScoutCheat",
            "model.difficulty.cheat", 10, true);
        addIntegerOption("model.option.landUnitCheat",
            "model.difficulty.cheat", 10, true);
        addIntegerOption("model.option.offensiveNavalUnitCheat",
            "model.difficulty.cheat", 10, true);
        addIntegerOption("model.option.transportNavalUnitCheat",
            "model.difficulty.cheat", 10, true);
    }

    private void addOptionGroup(String id, boolean difficulty) {
        Specification spec = game.getSpecification();
        spec.fixOptionGroup(new OptionGroup(id, spec), difficulty);
    }

    private void addBooleanOption(String id, String gr, boolean defaultValue,
                                  boolean difficulty) {
        BooleanOption op = new BooleanOption(id);
        op.setGroup(gr);
        op.setValue(defaultValue);
        addOption(op, difficulty);
    }

    private void addIntegerOption(String id, String gr, int defaultValue,
                                  boolean difficulty) {
        IntegerOption op = new IntegerOption(id);
        op.setGroup(gr);
        op.setValue(defaultValue);
        addOption(op, difficulty);
    }

    private void addStringOption(String id, String gr, String defaultValue,
                                 boolean difficulty) {
        StringOption op = new StringOption(id);
        op.setGroup(gr);
        op.setValue(defaultValue);
        addOption(op, difficulty);
    }

    private void addOption(AbstractOption option, boolean difficulty) {
        Specification spec = game.getSpecification();
        if (!spec.hasOption(option.getId())) {
            spec.addAbstractOption(option);
            if (difficulty) {
                for (OptionGroup level : spec.getDifficultyLevels()) {
                    level.getOptionGroup(option.getGroup()).add(option);
                }
            } else {
                spec.getOptionGroup(option.getGroup()).add(option);
            }
        }
    }
    // end compatibility code


    /**
     * At savegame version 11 (new in 0.10.0) several classes were
     * added to serverObjects.  Evil hack here to scan the game for
     * objects in pre-v11 games that should now be in serverObjects,
     * and put them in.
     *
     * @param serverStrings A list of server object {type, id} pairs to add to.
     * @param fis The savegame to scan.
     */
    private static void v11FixServerObjects(List<String> serverStrings,
                                            final FreeColSavegameFile fis) {
        // @compat 0.10.x
        XMLStream xs = null;
        try {
            xs = createXMLStreamReader(fis);
            final XMLStreamReader xsr = xs.getXMLStreamReader();
            xsr.nextTag();
            final String owner = xsr.getAttributeValue(null, "owner");
            while (xsr.nextTag() != XMLStreamConstants.END_ELEMENT) {
                if (xsr.getLocalName().equals(Game.getXMLElementTagName())) {
                    Game game = new ServerGame(null, xsr,
                            new ArrayList<String>(serverStrings),
                            new FreeColTcFile("freecol").getSpecification());
                    Iterator<FreeColGameObject> objs
                        = game.getFreeColGameObjectIterator();
                    while (objs.hasNext()) {
                        FreeColGameObject fcgo = objs.next();
                        if (serverStrings.contains(fcgo.getId())) {
                            continue;
                        } else if (fcgo instanceof Europe) {
                            serverStrings.add("serverEurope");
                        } else if (fcgo instanceof Colony) {
                            serverStrings.add("serverColony");
                        } else if (fcgo instanceof Building) {
                            serverStrings.add("serverBuilding");
                        } else if (fcgo instanceof ColonyTile) {
                            serverStrings.add("serverColonyTile");
                        } else if (fcgo instanceof IndianSettlement) {
                            serverStrings.add("serverIndianSettlement");
                        } else if (fcgo instanceof Unit) {
                            serverStrings.add("serverUnit");
                        } else {
                            continue;
                        }
                        serverStrings.add(fcgo.getId());
                    }
                    break;
                }
                while (xsr.nextTag() != XMLStreamConstants.END_ELEMENT) {
                    xsr.nextTag();
                }
            }
        } catch (Exception e) {
            ;
        } finally {
            if (xs != null) xs.close();
        }
    }

    /**
     * Removes automatically created save games.
     * Call this function to delete the automatically created save games from
     * a previous game.
     */
    public static void removeAutosaves(final String prefix) {
        for (File autosaveFile : FreeColDirectories.getAutosaveDirectory().listFiles()) {
            if (autosaveFile.getName().startsWith(prefix)) {
                autosaveFile.delete();
            }
        }
    }

    /**
     * Reveals or hides the entire map for all players.
     * Debug menu helper.
     *
     * @param reveal If true, reveal, if false, hide.
     */
    public void exploreMapForAllPlayers(boolean reveal) {
        for (Player player : getGame().getLiveEuropeanPlayers()) {
            ((ServerPlayer)player).exploreMap(reveal);
        }
        getSpecification().getBooleanOption(GameOptions.FOG_OF_WAR)
            .setValue(reveal);

        for (Player player : getGame().getLiveEuropeanPlayers()) {
            try {
                ((ServerPlayer)player).getConnection()
                    .sendDumping(DOMMessage.createMessage("reconnect"));
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error sending reconnect.", e);
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
     * Adds a new AIPlayer to the Game.
     *
     * @param nation a <code>Nation</code> value
     * @return a <code>ServerPlayer</code> value
     */
    public ServerPlayer addAIPlayer(Nation nation) {
        String name = nation.getRulerNameKey();
        DummyConnection theConnection =
            new DummyConnection("Server connection - " + name,
                getInGameInputHandler());
        ServerPlayer aiPlayer
            = new ServerPlayer(getGame(), name, false, nation,
                               null, theConnection);
        aiPlayer.setAI(true);
        DummyConnection aiConnection
            = new DummyConnection("AI connection - " + name,
                new AIInGameInputHandler(this, aiPlayer, getAIMain()));

        aiConnection.setOutgoingMessageHandler(theConnection);
        theConnection.setOutgoingMessageHandler(aiConnection);

        getServer().addDummyConnection(theConnection);

        getGame().addPlayer(aiPlayer);

        // Send message to all players except to the new player:
        // TODO: null-destination-player is unnecessarily generous visibility
        Element player = DOMMessage.createMessage("addPlayer");
        player.appendChild(aiPlayer.toXMLElement(null,
                player.getOwnerDocument()));
        getServer().sendToAll(player, theConnection);
        return aiPlayer;
    }

    /**
     * Gets the AI player corresponding to a given player.
     *
     * @param player The <code>Player</code> to look up.
     * @return The corresponding AI player, or null if not found.
     */
    public AIPlayer getAIPlayer(Player player) {
        return getAIMain().getAIPlayer(player);
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
            fos = new FileOutputStream(FreeColDirectories.getHighScoreFile());
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
            throw new IOException("XMLStreamException: " + e.getMessage());
        } catch (Exception e) {
            throw new IOException("Exception: " + e.getMessage());
        } finally {
            try {
                if (fos != null) fos.close();
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
        File hsf = FreeColDirectories.getHighScoreFile();
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
            throw new IOException("XMLStreamException: " + e.getMessage());
        } catch (Exception e) {
            throw new IOException("Exception: " + e.getMessage());
        } finally {
            if (fis != null) fis.close();
        }
    }

    public void shutdown() {
        server.shutdown();
    }
}
