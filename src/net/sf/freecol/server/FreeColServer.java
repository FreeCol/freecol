/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.FreeColSeed;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.io.FreeColSavegameFile;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.NationOptions;
import net.sf.freecol.common.model.NationOptions.NationState;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Stance;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.DOMMessage;
import net.sf.freecol.common.option.BooleanOption;
import net.sf.freecol.common.option.OptionGroup;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.common.util.Utils;
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
import net.sf.freecol.server.model.ServerIndianSettlement;
import net.sf.freecol.server.model.ServerModelObject;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.model.TransactionSession;
import net.sf.freecol.server.networking.DummyConnection;
import net.sf.freecol.server.networking.Server;

import org.w3c.dom.Element;


/**
 * The main control class for the FreeCol server.  This class both
 * starts and keeps references to all of the server objects and the
 * game model objects.
 *
 * If you would like to start a new server you just create a new
 * object of this class.
 */
public final class FreeColServer {

    private static final Logger logger = Logger.getLogger(FreeColServer.class.getName());

    public static final String ACTIVE_UNIT_TAG = "activeUnit";
    public static final String DEBUG_TAG = "debug";
    public static final String RANDOM_STATE_TAG = "randomState";
    public static final String OWNER_TAG = "owner";
    public static final String PUBLIC_SERVER_TAG = "publicServer";
    public static final String SAVED_GAME_TAG = "savedGame";
    public static final String SERVER_OBJECTS_TAG = "serverObjects";
    public static final String SINGLE_PLAYER_TAG = "singleplayer";

    private static final int META_SERVER_UPDATE_INTERVAL = 60000;

    /**
     * The save game format used for saving games.
     *
     * Version 7-10 were used in 0.9.x.
     * Version 11 made a lot of changes and was introduced for the 0.10.0
     *     series.
     * Version 12 was introduced with HighSeas post-0.10.1.
     * Version 13 coincides with the start of the 0.11.x series.
     *
     * Please add to this comment if you increase the version.
     */
    public static final int SAVEGAME_VERSION = 13;

    /**
     * The oldest save game format that can still be loaded.
     * The promise is that FreeCol 0.n.* can load 0.(n-1).* games.
     *
     * Revisit the numbering scheme and save compatibility promise
     * when 1.0 is released?
     */
    public static final int MINIMUM_SAVEGAME_VERSION = 11;

    /**
     * The ruleset to use when loading old format games where a spec
     * may not be readily available.
     */
    public static final String DEFAULT_SPEC = "freecol";

    /** Games are either starting, ending or being played. */
    public static enum GameState { STARTING_GAME, IN_GAME, ENDING_GAME }


    // Instantiation-time parameters.

    /** Is this a single player game? */
    private boolean singlePlayer;

    /** Should this game be listed on the meta-server? */
    private boolean publicServer = false;

    /** The name of this server. */
    private String name;

    /** The current state of the game. */
    private GameState gameState = GameState.STARTING_GAME;

    // Networking:
    private Server server;

    // Controllers
    private final UserConnectionHandler userConnectionHandler;

    private final PreGameController preGameController;

    private final PreGameInputHandler preGameInputHandler;

    private final InGameInputHandler inGameInputHandler;

    private final InGameController inGameController;

    /** The AI controller. */
    private AIMain aiMain;

    /** The game underway. */
    private ServerGame game;

    /** The map generator. */
    private MapGenerator mapGenerator = null;

    /** The internal provider of random numbers. */
    private Random random = null;

    /** The game integrity state. */
    private int integrity = 1;

    /** An active unit specified in a saved game. */
    private Unit activeUnit = null;

    
    /**
     * Starts a new server, with a new game.
     *
     * @param publicServer If true, add to the meta-server.
     * @param singlePlayer True if this is a single player game.
     * @param specification The <code>Specification</code> to use in this game.
     * @param port The TCP port to use for the public socket.
     * @param name An optional name for the server.
     * @exception IOException If the public socket cannot be created.
     */
    public FreeColServer(boolean publicServer, boolean singlePlayer,
                         Specification specification, int port, String name)
        throws IOException {
        this.publicServer = publicServer;
        this.singlePlayer = singlePlayer;
        this.name = name;

        this.server = serverStart(port); // Throws IOException

        this.userConnectionHandler = new UserConnectionHandler(this);
        this.preGameController = new PreGameController(this);
        this.preGameInputHandler = new PreGameInputHandler(this);
        this.inGameInputHandler = new InGameInputHandler(this);

        this.random = new Random(FreeColSeed.getFreeColSeed(true));
        this.game = new ServerGame(specification);
        this.game.setNationOptions(new NationOptions(specification));
        this.game.randomize(random);
        
        this.inGameController = new InGameController(this, random);
        this.mapGenerator = new SimpleMapGenerator(game, random);

        this.publicServer = updateMetaServer(true);
    }

    /**
     * Starts a new networked server, initializing from a saved game.
     *
     * The specification is usually null, which means it will be
     * initialized by extracting it from the saved game.  However
     * MapConverter does call this with an overriding specification.
     *
     * @param savegame The file where the game data is located.
     * @param specification An optional <code>Specification</code> to use.
     * @param port The TCP port to use for the public socket.
     * @param name An optional name for the server.
     * @exception IOException If save game can not be found.
     * @exception FreeColException If the savegame could not be loaded.
     */
    public FreeColServer(final FreeColSavegameFile savegame, 
                         Specification specification, int port, String name)
        throws FreeColException, IOException, XMLStreamException {
        // publicServer will be read from the saved game
        // singlePlayer will be read from the saved game
        this.name = name;

        this.server = serverStart(port); // Throws IOException

        this.userConnectionHandler = new UserConnectionHandler(this);
        this.preGameController = new PreGameController(this);
        this.preGameInputHandler = new PreGameInputHandler(this);
        this.inGameInputHandler = new InGameInputHandler(this);

        this.game = loadGame(savegame, specification, server);
        // NationOptions will be read from the saved game.
        TransactionSession.clearAll();

        // Replace the PRNG in the game if it is missing or a command line
        // option was present.
        long seed = FreeColSeed.getFreeColSeed(this.random == null);
        if (seed != FreeColSeed.DEFAULT_SEED) {
            this.random = new Random(seed);
        }
        this.inGameController = new InGameController(this, random);
        this.mapGenerator = null;

        this.publicServer = updateMetaServer(true);
    }


    /**
     * Is the user playing in single player mode?
     *
     * @return True if this is a single player game.
     */
    public boolean getSinglePlayer() {
        return this.singlePlayer;
    }

    /**
     * Sets the single/multiplayer state of the game.
     *
     * @param singlePlayer The new single/multiplayer status.
     */
    public void setSinglePlayer(boolean singlePlayer) {
        this.singlePlayer = singlePlayer;
    }

    /**
     * Get the public server state.
     *
     * @return The public server state.
     */
    public boolean getPublicServer() {
        return this.publicServer;
    }

    /**
     * Sets the public server state.
     *
     * @param publicServer The new public server state.
     */
    public void setPublicServer(boolean publicServer) {
        this.publicServer = publicServer;
    }

    /**
     * Gets the name of this server.
     *
     * @return The name.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Sets the name of this server.
     *
     * @param name The new name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the host this server was started on.
     *
     * @return The host.
     */
    public String getHost() {
        return (this.server == null) ? null : this.server.getHost();
    }

    /**
     * Gets the port this server was started on.
     *
     * @return The port.
     */
    public int getPort() {
        return (this.server == null) ? -1 : this.server.getPort();
    }


    /**
     * Start a Server at port.
     *
     * If the port is specified, just try once.
     *
     * If the port is unspecified (negative), try multiple times.
     *
     * @param firstPort The port to start trying to connect at.
     * @return A started <code>Server</code>.
     * @exception IOException on failure to open the port.
     */
    private Server serverStart(int firstPort) throws IOException {
        String host = (this.publicServer) ? "0.0.0.0"
            : InetAddress.getLoopbackAddress().getHostAddress();
        int port, tries;
        if (firstPort < 0) {
            port = FreeCol.getServerPort();
            tries = 10;
        } else {
            port = firstPort;
            tries = 1;
        }
        logger.finest("serverStart(" + firstPort + ") => " + port
            + " x " + tries);
        for (int i = tries; i > 0; i--) {
            try {
                server = new Server(this, host, port);
                server.start();
                break;
            } catch (BindException be) {
                if (i == 1) {
                    throw new IOException("Bind exception starting server at: "
                        + host + ":" + port, be);
                }
            } catch (IOException ie) {
                if (i == 1) throw ie;
            }
            port++;
        }
        return server;
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
     * @return One of: {@link GameState#STARTING_GAME},
     *     {@link GameState#IN_GAME} and {@link GameState#ENDING_GAME}.
     */
    public GameState getGameState() {
        return gameState;
    }

    /**
     * Sets the current state of the game.
     *
     * @param state The new state to be set.  One of:
     *     {@link GameState#STARTING_GAME}, {@link GameState#IN_GAME}
     *     and {@link GameState#ENDING_GAME}.
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
    public int getIntegrity() {
        return integrity;
    }

    /**
     * Get the map generator.
     *
     * @return The <code>MapGenerator</code>.
     */
    public MapGenerator getMapGenerator() {
        return this.mapGenerator;
    }

    /**
     * Set the map generator.
     *
     * @param mapGenerator The new <code>MapGenerator</code>.
     */
    public void setMapGenerator(MapGenerator mapGenerator) {
        this.mapGenerator = mapGenerator;
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
     * Sends information about this server to the meta-server.
     *
     * Publically visible version, that is called in game.
     *
     * @return True if the MetaServer was updated.
     */
    public boolean updateMetaServer() {
        return updateMetaServer(false);
    }

    /**
     * Sends information about this server to the meta-server.
     *
     * This is the master routine with private `firstTime' access
     * when called from the constructors.
     *
     * @param firstTime Must be true when called for the first time.
     * @return True if the MetaServer was updated.
     */
    private boolean updateMetaServer(boolean firstTime) {
        if (!this.publicServer) return false;

        Connection mc;
        try {
            mc = new Connection(FreeCol.META_SERVER_ADDRESS,
                                FreeCol.META_SERVER_PORT, null,
                                FreeCol.SERVER_THREAD);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not connect to meta-server: ",
                FreeCol.META_SERVER_ADDRESS + ":" + FreeCol.META_SERVER_PORT);
            this.publicServer = false;
            return false;
        }

        String tag = (firstTime) ? "register" : "update";
        int port = mc.getSocket().getPort();
        String addr = (name != null) ? name
            : (mc.getSocket().getLocalAddress().getHostAddress() + ":" + port);
        int nPlayers = getNumberOfLivingHumanPlayers();
        boolean started = gameState != GameState.STARTING_GAME;
        try {
            Element reply = mc.ask(DOMMessage.createMessage(tag,
                    "name", addr,
                    "port", Integer.toString(port),
                    "slotsAvailable", Integer.toString(getSlotsAvailable()),
                    "currentlyPlaying", Integer.toString(nPlayers),
                    "isGameStarted", Boolean.toString(started),
                    "version", FreeCol.getVersion(),
                    "gameState", Integer.toString(getGameState().ordinal())));
            if (reply != null
                && "noRouteToServer".equals(reply.getTagName())) {
                this.publicServer = false;
                return false;
            }
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Network error with meta-server:" + addr,
                ioe);
            this.publicServer = false;
            return false;
        } finally {
            mc.close();
        }
        if (firstTime) {
            // Starts the metaserver update thread.
            //
            // This update is really a "Hi! I am still here!"-message,
            // since an additional update should be sent when a new
            // player is added to/removed from this server etc.
            Timer t = new Timer(true);
            t.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        if (!updateMetaServer()) cancel();
                    }
                }, META_SERVER_UPDATE_INTERVAL, META_SERVER_UPDATE_INTERVAL);
        }
        return true;
    }

    /**
     * Removes this server from the metaserver's list. The information is only
     * sent if <code>public == true</code>.
     *
     * @return True if the MetaServer was updated.
     */
    public boolean removeFromMetaServer() {
        if (!this.publicServer) return false;

        try (
            Connection mc = new Connection(FreeCol.META_SERVER_ADDRESS,
                FreeCol.META_SERVER_PORT,
                null, FreeCol.SERVER_THREAD);
        ) {
            mc.send(DOMMessage.createMessage("remove",
                    "port", Integer.toString(mc.getSocket().getPort())));
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Network error leaving meta-server: "
                + FreeCol.META_SERVER_ADDRESS + ":" + FreeCol.META_SERVER_PORT,
                ioe);
            this.publicServer = false;
            return false;
        }
        return true;
    }

    /**
     * Gets the number of player that may connect.
     *
     * @return The number of available slots for human players.  This
     *     number also includes european players currently controlled
     *     by the AI.
     */
    public int getSlotsAvailable() {
        int n = 0;
        for (Player player : game.getLiveEuropeanPlayers(null)) {
            ServerPlayer sp = (ServerPlayer)player;
            if (!sp.isREF() && sp.isAI() && !sp.isConnected()) n++;
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
        for (Player player : game.getLivePlayers(null)) {
            ServerPlayer sp = (ServerPlayer)player;
            if (!sp.isAI() && sp.isConnected()) n++;
        }
        return n;
    }

    /**
     * Saves a game.
     *
     * @param file The file where the data will be written.
     * @exception IOException If a problem was encountered while trying
     *     to open, write or close the file.
     */
    public void saveGame(File file, OptionGroup options) throws IOException {
        saveGame(file, options, null);
    }

    /**
     * Save a game from the map editor.
     *
     * @param file The file where the data will be written.
     * @param image A thumbnail image for the map.
     * @exception IOException If a problem was encountered while trying
     *     to open, write or close the file.
     */
    public void saveMapEditorGame(File file, BufferedImage image) 
        throws IOException {
        this.setAIMain(null);
        saveGame(file, null, image);
    }

    /**
     * Saves a game.
     *
     * @param file The file where the data will be written.
     * @param options Optional client options to save in the game.
     * @param image A thumbnail <code>Image</code> value to save in the game.
     * @exception IOException If a problem was encountered while trying
     *     to open, write or close the file.
     */
    public void saveGame(File file, OptionGroup options, BufferedImage image)
        throws IOException {
        final ServerGame game = getGame();
        try (
            JarOutputStream fos = new JarOutputStream(new FileOutputStream(file));
        ) {
            if (image != null) {
                fos.putNextEntry(new JarEntry(FreeColSavegameFile.THUMBNAIL_FILE));
                ImageIO.write(image, "png", fos);
                fos.closeEntry();
            }

            if (options != null) {
                fos.putNextEntry(new JarEntry(FreeColSavegameFile.CLIENT_OPTIONS));
                options.save(fos, FreeColXMLWriter.WriteScope.toSave(), true);
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
            try (
                FreeColXMLWriter xw = new FreeColXMLWriter(fos,
                    FreeColXMLWriter.WriteScope.toSave(), false);
            ) {
                xw.writeStartDocument("UTF-8", "1.0");

                xw.writeComment(FreeCol.getConfiguration().toString());

                xw.writeStartElement(SAVED_GAME_TAG);

                // Add the attributes:
                xw.writeAttribute(OWNER_TAG, FreeCol.getName());

                xw.writeAttribute(PUBLIC_SERVER_TAG, publicServer);

                xw.writeAttribute(SINGLE_PLAYER_TAG, singlePlayer);

                xw.writeAttribute(FreeColSavegameFile.VERSION_TAG,
                                  SAVEGAME_VERSION);

                xw.writeAttribute(RANDOM_STATE_TAG, Utils.getRandomState(random));

                xw.writeAttribute(DEBUG_TAG, FreeColDebugger.getDebugModes());

                if (getActiveUnit() != null) {
                    xw.writeAttribute(ACTIVE_UNIT_TAG, getActiveUnit());
                }
            
                // Add server side model information:
                xw.writeStartElement(SERVER_OBJECTS_TAG);

                for (ServerModelObject smo : game.getServerModelObjects()) {
                    xw.writeStartElement(smo.getServerXMLElementTagName());

                    xw.writeAttribute(FreeColObject.ID_ATTRIBUTE_TAG, smo.getId());

                    xw.writeEndElement();
                }

                xw.writeEndElement();

                game.toXML(xw); // Add the game

                if (aiMain != null) aiMain.toXML(xw); // Add the AIObjects

                xw.writeEndElement();
                xw.writeEndDocument();
                xw.flush();
            }
            fos.closeEntry();
        } catch (XMLStreamException e) {
            throw new IOException("Failed to save (XML)", e);
        } catch (Exception e) {
            throw new IOException("Failed to save", e);
        }
    }

    /**
     * Loads a game.
     *
     * @param fis The file where the game data is located.
     * @return The game found in the stream.
     * @exception FreeColException if the savegame contains incompatible data.
     * @exception IOException if the stream can not be created.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    public ServerGame loadGame(final FreeColSavegameFile fis)
        throws IOException, FreeColException, XMLStreamException {
        return loadGame(fis, null, getServer());
    }

    /**
     * Read just the game part from a file.
     *
     * When the specification is not supplied, the one found in the saved
     * game will be used.
     *
     * @param file The <code>File</code> to read from.
     * @param spec An optional <code>Specification</code> to use.
     * @param server Use this (optional) server to load into.
     * @return The game found in the stream.
     */
    public static ServerGame readGame(File file, Specification spec,
                                      FreeColServer server) {
        ServerGame g = null;
        try {
            g = FreeColServer.readGame(new FreeColSavegameFile(file),
                                       spec, server);
            logger.info("Imported file " + file.getPath());
        } catch (Exception e) {
            logger.log(Level.WARNING, "Import failed for " + file.getPath(), e);
        }

        // If importing as a result of "Start Game" in the map editor,
        // consume the file.
        File startGame = FreeColDirectories.getStartMapFile();
        if (startGame != null
            && startGame.getPath().equals(file.getPath())) {
            file.delete();
        }
        return g;
    }

    /**
     * Reads just the game part from a save game from a stream.
     *
     * When the specification is not supplied, the one found in the saved
     * game will be used.
     *
     * @param fis The stream to read from.
     * @param specification An optional <code>Specification</code> to use.
     * @param server Use this (optional) server to load into.
     * @return The game found in the stream.
     * @exception FreeColException if the format is incompatible.
     * @exception IOException if the stream can not be created.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    public static ServerGame readGame(final FreeColSavegameFile fis,
                                      Specification specification,
                                      FreeColServer server)
        throws IOException, FreeColException, XMLStreamException {
        final int savegameVersion = fis.getSavegameVersion();
        if (savegameVersion < MINIMUM_SAVEGAME_VERSION) {
            throw new FreeColException("server.incompatibleVersions");
        }
        logger.info("Found savegame version " + savegameVersion);

        ServerGame game = null;
        try (
            FreeColXMLReader xr = fis.getFreeColXMLReader();
        ) {
            // Switch to the read scope that creates server objects.
            xr.setReadScope(FreeColXMLReader.ReadScope.SERVER);
            
            String active = null;
            xr.nextTag();

            if (server != null) {
                server.setSinglePlayer(xr.getAttribute(SINGLE_PLAYER_TAG,
                                                       true));

                server.setPublicServer(xr.getAttribute(PUBLIC_SERVER_TAG,
                                                       false));

                String r = xr.getAttribute(RANDOM_STATE_TAG, (String)null);
                server.setServerRandom(Utils.restoreRandomState(r));
                    
                FreeColDebugger.setDebugModes(xr.getAttribute(DEBUG_TAG,
                                                              (String)null));

                active = xr.getAttribute(ACTIVE_UNIT_TAG, (String)null);
            }

            while (xr.nextTag() != XMLStreamConstants.END_ELEMENT) {
                final String tag = xr.getLocalName();
                if (SERVER_OBJECTS_TAG.equals(tag)) { // No longer used
                    while (xr.nextTag() != XMLStreamConstants.END_ELEMENT) {
                        xr.nextTag();
                    }

                } else if (Game.getXMLElementTagName().equals(tag)) {
                    // Read the game
                    game = new ServerGame(null, xr, specification);
                    game.setCurrentPlayer(null);
                    if (server != null) server.setGame(game);

                } else if (AIMain.getXMLElementTagName().equals(tag)) {
                    if (server == null) break;
                    server.setAIMain(new AIMain(server, xr));

                } else {
                    throw new XMLStreamException("Unknown tag"
                        + " reading server game: " + tag);
                }
            }

            if (server != null && active != null && game != null) {
                // Now units are all present, set active unit.
                Unit u = game.getFreeColGameObject(active, Unit.class);
                server.setActiveUnit(u);
            }
        }
        return game;
    }

    /**
     * Loads a game.
     *
     * @param fis The file where the game data is located.
     * @param specification The <code>Specification</code> to refer to.
     * @param server The server to connect the AI players to.
     * @return The new game.
     * @exception FreeColException if the savegame contains incompatible data.
     * @exception IOException if the stream can not be created.
     * @exception XMLStreamException if there a problem reading the stream.
     */
    private ServerGame loadGame(final FreeColSavegameFile fis,
                                Specification specification, Server server)
        throws FreeColException, IOException, XMLStreamException {

        ServerGame game = readGame(fis, specification, this);
        gameState = GameState.IN_GAME;
        integrity = game.checkIntegrity(true);
        if (integrity < 0) {
            logger.warning("Game integrity test failed.");
        } else {
            logger.info("Game integrity test "
                + ((integrity > 0) ? "succeeded" : "failed, but fixed") + ".");
        }

        int savegameVersion = fis.getSavegameVersion();
        // @compat 0.10.x
        if (savegameVersion < 12) {
            for (Player p : game.getPlayers()) {
                // @compat 0.10.5
                if (p.isIndian()) {
                    for (IndianSettlement is : p.getIndianSettlements()) {
                        ((ServerIndianSettlement)is).updateMostHated();
                    }
                }
                // end @compat 0.10.5

                if (!p.isIndian() && p.getEurope() != null) {
                    p.initializeHighSeas();

                    for (Unit u : p.getEurope().getUnitList()) {
                        // Move units to high seas.  Use setLocation()
                        // so that units are removed from Europe, and
                        // appear in correct panes in the EuropePanel
                        // do not set the UnitState, as this clears
                        // workLeft.
                        if (u.getState() == Unit.UnitState.TO_EUROPE) {
                            logger.info("Found unit on way to europe: " + u);
                            u.setLocation(p.getHighSeas());//-vis: safe!map
                            u.setDestination(p.getEurope());
                        } else if (u.getState() == Unit.UnitState.TO_AMERICA) {
                            logger.info("Found unit on way to new world: " + u);
                            u.setLocation(p.getHighSeas());//-vis: safe!map
                            u.setDestination(game.getMap());
                        }
                    }
                }
            }

            for (Tile tile : game.getMap().getAllTiles()) {
                TerrainGenerator.encodeStyle(tile);
            }
        }
        // end @compat 0.10.x

        // @compat 0.10.x
        game.getMap().resetContiguity();
        // end @compat

        // @compat 0.10.x
        Player unknown = game.getUnknownEnemy();
        if (unknown == null) {
            establishUnknownEnemy(game);
        } else {
            unknown.setName(Nation.UNKNOWN_NATION_ID);
        }
        // end @compat

        // Ensure that critical option groups can not be edited.
        specification = getSpecification();
        specification.disableEditing();

        // AI initialization.
        AIMain aiMain = getAIMain();
        int aiIntegrity = (aiMain == null) ? -1 : aiMain.checkIntegrity(true);
        if (aiIntegrity < 0) {
            aiMain = new AIMain(this);
            aiMain.findNewObjects(true);
            logger.warning("AI integrity test failed, replaced AIMain.");
        } else {
            logger.info("AI integrity test "
                + ((aiIntegrity > 0) ? "succeeded" : "failed, but fixed"));
        }
        game.setFreeColGameObjectListener(aiMain);

        Collections.sort(game.getPlayers(), Player.playerComparator);
        for (Player player : game.getLivePlayers(null)) {
            if (player.isAI()) {
                ServerPlayer serverPlayer = (ServerPlayer)player;
                DummyConnection theConnection
                    = new DummyConnection("Server-Server-" + player.getName(),
                        getInGameInputHandler());
                DummyConnection aiConnection
                    = new DummyConnection("Server-AI-" + player.getName(),
                        new AIInGameInputHandler(this, serverPlayer, aiMain));
                aiConnection.setConnection(theConnection);
                theConnection.setConnection(aiConnection);
                server.addDummyConnection(theConnection);
                serverPlayer.setConnection(theConnection);
                serverPlayer.setConnected(true);
            }
            if (player.isEuropean()) {
                // The map will be invalid, so trigger a recalculation of the
                // canSeeTiles, by calling canSee for an arbitrary tile.
                player.canSee(game.getMap().getTile(0, 0));
            }
        }

        return game;
    }

    /**
     * Add option to capture units under repair in a colony.
     * Establish a new unknown enemy player.
     *
     * @param game The <code>Game</code> to establish the enemy within.
     * @return The new unknown enemy <code>Player</code>.
     */
    private ServerPlayer establishUnknownEnemy(Game game) {
        final Specification spec = game.getSpecification();

        ServerPlayer enemy = new ServerPlayer(game, false,
            spec.getNation(Nation.UNKNOWN_NATION_ID), null, null);
        game.setUnknownEnemy(enemy);
        return enemy;
    }

    /**
     * Create an empty map.
     *
     * Public for the map generator.
     *
     * @param game The <code>Game</code> to create the map for.
     * @param width The map width.
     * @param height The map height.
     * @return The new empty <code>Map</code>.
     */
    public Map createEmptyMap(Game game, int width, int height) {
        return getMapGenerator().createEmptyMap(width, height,
                                                new LogBuilder(-1));
    }

    /**
     * Builds a new game using the parameters that exist in the game
     * as it stands.
     *
     * @return The updated game.
     * @exception FreeColException on map generation failure.
     */
    public Game buildGame() throws FreeColException {
        final ServerGame game = getGame();
        final Specification spec = game.getSpecification();
        final AIMain aiMain = new AIMain(this);
        setAIMain(aiMain);

        List<ServerPlayer> newAI = new ArrayList<>();
        game.setFreeColGameObjectListener(aiMain);
        for (Entry<Nation, NationState> entry
                 : game.getNationOptions().getNations().entrySet()) {
            if (entry.getKey().isUnknownEnemy()) continue;
            if (entry.getValue() != NationState.NOT_AVAILABLE
                && game.getPlayerByNation(entry.getKey()) == null) {
                newAI.add(makeAIPlayer(entry.getKey()));
            }
        }
        Collections.sort(game.getPlayers(), Player.playerComparator);
        game.updatePlayers(newAI);

        // We need a fake unknown enemy player
        establishUnknownEnemy(game);

        // Create the map.
        LogBuilder lb = new LogBuilder(256);
        game.setMap(getMapGenerator().createMap(lb));
        lb.log(logger, Level.FINER);
        
        // Initial stances and randomizations for all players.
        spec.generateDynamicOptions();
        Random random = getServerRandom();
        for (Player player : game.getLivePlayers(null)) {
            ((ServerPlayer)player).randomizeGame(random);
            if (player.isIndian()) {
                // Indian players know about each other, but European colonial
                // players do not.
                final int alarm = (Tension.Level.HAPPY.getLimit()
                    + Tension.Level.CONTENT.getLimit()) / 2;
                for (Player other : game.getLiveNativePlayers(player)) {
                    player.setStance(other, Stance.PEACE);
                    for (IndianSettlement is : player.getIndianSettlements()) {
                        is.setAlarm(other, new Tension(alarm));
                    }
                }
            }
        }

        // Ensure that option groups can not be edited any more.
        spec.getMapGeneratorOptions().setEditable(false);
        spec.getGameOptions().setEditable(false);
        spec.getOptionGroup("difficultyLevels").setEditable(false);

        // Let the AIMain scan for objects it should be managing.
        aiMain.findNewObjects(true);

        return game;
    }

    /**
     * Make a new AI player and add it to the game.
     *
     * Public so the controller can add REF players.
     *
     * @param nation The <code>Nation</code> to add.
     * @return The new AI <code>ServerPlayer</code>.
     */
    public ServerPlayer makeAIPlayer(Nation nation) {
        DummyConnection theConnection
            = new DummyConnection("Server connection - " + nation.getId(),
                getInGameInputHandler());
        ServerPlayer aiPlayer = new ServerPlayer(getGame(), false, nation,
                                                 null, theConnection);
        aiPlayer.setAI(true);
        DummyConnection aiConnection
            = new DummyConnection("AI connection - " + nation.getId(),
                new AIInGameInputHandler(this, aiPlayer, getAIMain()));
        aiConnection.setConnection(theConnection);
        theConnection.setConnection(aiConnection);
        getServer().addDummyConnection(theConnection);

        getGame().addPlayer(aiPlayer);
        // Add to the AI, which was previously deferred because the
        // player type was unknown.
        getAIMain().setFreeColGameObject(aiPlayer.getId(), aiPlayer);
        return aiPlayer;
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
        for (Player player : getGame().getLiveEuropeanPlayers(null)) {
            ((ServerPlayer)player).exploreMap(reveal);
        }
     
        // Removes fog of war when revealing the whole map
        // Restores previous setting when hiding it back again
        BooleanOption fogOfWarSetting = game.getSpecification()
            .getBooleanOption(GameOptions.FOG_OF_WAR);
        if (reveal) {
            FreeColDebugger.setNormalGameFogOfWar(fogOfWarSetting.getValue());
            fogOfWarSetting.setValue(Boolean.FALSE); 
        } else {
            fogOfWarSetting.setValue(FreeColDebugger.getNormalGameFogOfWar());
        }

        for (Player player : getGame().getLiveEuropeanPlayers(null)) {
            try {
                ((ServerPlayer)player).getConnection()
                    .send(DOMMessage.createMessage("reconnect"));
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
        return (ServerPlayer)find(game.getPlayers(),
            p -> ((ServerPlayer)p).getConnection() == connection);
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
     * Shut down this FreeColServer.
     */
    public void shutdown() {
        server.shutdown();
    }
}
