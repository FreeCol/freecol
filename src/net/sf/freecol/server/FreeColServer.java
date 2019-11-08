/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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
import java.io.IOException;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Random;
import java.util.Timer;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.xml.stream.XMLStreamException;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.FreeColSeed;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.io.FreeColSavegameFile;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.metaserver.MetaServerUtils;
import net.sf.freecol.common.metaserver.ServerInfo;
import static net.sf.freecol.common.model.Constants.*;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Game.LogoutReason;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.NationOptions;
import net.sf.freecol.common.model.NationOptions.NationState;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Stance;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.ChangeSet;
import net.sf.freecol.common.networking.ChangeSet.See;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.GameStateMessage;
import net.sf.freecol.common.networking.LogoutMessage;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.TrivialMessage;
import net.sf.freecol.common.networking.VacantPlayersMessage;
import net.sf.freecol.common.option.BooleanOption;
import net.sf.freecol.common.option.GameOptions;
import net.sf.freecol.common.option.MapGeneratorOptions;
import net.sf.freecol.common.option.OptionGroup;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.common.util.LogBuilder;
import static net.sf.freecol.common.util.Utils.*;
import net.sf.freecol.server.ai.AIInGameInputHandler;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.control.Controller;
import net.sf.freecol.server.control.InGameController;
import net.sf.freecol.server.control.PreGameController;
import net.sf.freecol.server.control.ServerInputHandler;
import net.sf.freecol.server.control.UserConnectionHandler;
import net.sf.freecol.server.generator.MapGenerator;
import net.sf.freecol.server.generator.SimpleMapGenerator;
import net.sf.freecol.server.generator.TerrainGenerator;
import net.sf.freecol.server.model.ServerGame;
import net.sf.freecol.server.model.ServerIndianSettlement;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.model.Session;
import net.sf.freecol.server.networking.DummyConnection;
import net.sf.freecol.server.networking.Server;


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

    /**
     * The name to use as the owner of maps that have been edited by
     * the map editor.
     *
     * This is used so we can tell when a user tries to start a map
     * (which may be lacking vital pieces) as a game.  Make sure all
     * the maps in data/maps have this owner.
     *
     * TODO: Make loading a map as a game work.
     */
    public static final String MAP_EDITOR_NAME = "mapEditor";
    
    // @compat 0.11.6
    public static final String ACTIVE_UNIT_TAG = "activeUnit";
    public static final String OLD_SERVER_OBJECTS_TAG = "serverObjects";
    // end @compat 0.11.6
    public static final String DEBUG_TAG = "debug";
    public static final String RANDOM_STATE_TAG = "randomState";
    public static final String OWNER_TAG = "owner";
    public static final String PUBLIC_SERVER_TAG = "publicServer";
    public static final String SAVED_GAME_TAG = "savedGame";
    public static final String SINGLE_PLAYER_TAG = "singleplayer";

    /**
     * The save game format used for saving games.
     *
     * Version 7-10 were used in 0.9.x.
     * Version 11 made a lot of changes and was introduced for 0.10.0.
     * Version 12 was introduced with HighSeas post-0.10.1.
     * Version 13 coincides with the start of the 0.11.x series.
     * Version 14 coincides with the start of the 0.12.x series.
     *
     * Please add to this comment if you increase the version.
     * These days we *try* to change this only for (semi-)major
     * releases (i.e. when x changes in 0.x.y).
     */
    public static final int SAVEGAME_VERSION = 14;

    /**
     * The oldest save game format that can still be loaded.
     * The promise is that FreeCol 0.n.* can load 0.(n-1).* games.
     *
     * Revisit the numbering scheme and save compatibility promise
     * when 1.0 is released?
     */
    public static final int MINIMUM_SAVEGAME_VERSION = 12;

    /**
     * The ruleset to use when loading old format games where a spec
     * may not be readily available.
     */
    public static final String DEFAULT_SPEC = "freecol";

    /** The server is either starting, loading, being played, or ending. */
    public static enum ServerState { PRE_GAME, LOAD_GAME, IN_GAME, END_GAME }


    // Serializable fundamentals

    /** The name of this server. */
    private String name;

    /** Should this game be listed on the meta-server? */
    private boolean publicServer = false;

    /** Is this a single player game? */
    private boolean singlePlayer;

    /** The internal provider of random numbers. */
    private Random random = null;

    /** The game underway. */
    private ServerGame serverGame;

    // Non-serialized internals follow

    /** The underlying interface to the network. */
    private Server server;

    /** The current state of the game. */
    private ServerState serverState = ServerState.PRE_GAME;

    /** The handler for new user connections. */
    private final UserConnectionHandler userConnectionHandler;

    /** The pre-game controller. */
    private final PreGameController preGameController;

    /** The in-game controller. */
    private final InGameController inGameController;

    /** The input handler. */
    private final ServerInputHandler inputHandler;

    /** The AI controller. */
    private AIMain aiMain;

    /** The map generator. */
    private MapGenerator mapGenerator = null;

    /** The game integrity state. */
    private IntegrityType integrity = IntegrityType.INTEGRITY_GOOD;


    /**
     * Base constructor common to the following new-game and
     * saved-game constructors.
     *
     * @param name An optional name for the server.
     * @param port The TCP port to use for the public socket.
     * @exception IOException on failure to open the port.
     */
    private FreeColServer(String name, int port) throws IOException {
        this.name = name;
        this.server = createServer(port); // Throws IOException
        this.server.start();
        this.userConnectionHandler = new UserConnectionHandler(this);
        this.preGameController = new PreGameController(this);
        this.inGameController = new InGameController(this);
        this.inputHandler = new ServerInputHandler(this);
    }
    
    /**
     * Create a Server at port.
     *
     * If the port is specified, just try once.
     * If the port is unspecified (negative), try multiple times.
     *
     * @param firstPort The port to start trying to connect at.
     * @return A started {@code Server}.
     * @exception IOException on failure to open the port.
     */
    private Server createServer(int firstPort) throws IOException {
        List<InetAddress> addresses = new ArrayList<>();
        try {
            addresses.add(InetAddress.getLocalHost());
        } catch (UnknownHostException uhe) {
            logger.warning("Could not resolve local host name");
        }
        addresses.add(InetAddress.getLoopbackAddress());
        int tryMax = 1;
        if (firstPort < 0) {
            firstPort = FreeCol.getServerPort();
            tryMax = 10;
        }
        IOException ex = null;
        for (InetAddress ia : addresses) {
            String host = ia.getHostAddress();
            int port = firstPort;
            for (int i = tryMax; i > 0; i--) {
                try {
                    Server ret = new Server(this, host, port);
                    logger.finest("Server started: " + ret.getHost()
                        + ", " + ret.getPort());
                    return ret;
                } catch (IOException ioe) {
                    ex = ioe;
                }
                port++;
            }
        }
        throw ex;
    }

    /**
     * Starts a new server, with a new game.
     *
     * @param publicServer If true, add to the meta-server.
     * @param singlePlayer True if this is a single player game.
     * @param specification The {@code Specification} to use in this game.
     * @param port The TCP port to use for the public socket.
     * @param name An optional name for the server.
     * @exception IOException If the public socket cannot be created.
     */
    public FreeColServer(boolean publicServer, boolean singlePlayer,
                         Specification specification, int port, String name)
        throws IOException {
        this(name, port);

        this.setPublicServer(publicServer);
        this.singlePlayer = singlePlayer;
        this.random = new Random(FreeColSeed.getFreeColSeed(true));
        this.serverGame = new ServerGame(specification, random);
        this.inGameController.setRandom(this.random);
        this.mapGenerator = new SimpleMapGenerator(this.random);
        registerWithMetaServer();
    }

    /**
     * Starts a new networked server, initializing from a saved game.
     *
     * The specification is usually null, which means it will be
     * initialized by extracting it from the saved game.  However
     * MapConverter does call this with an overriding specification.
     *
     * @param savegame The file where the game data is located.
     * @param specification An optional {@code Specification} to use.
     * @param port The TCP port to use for the public socket.
     * @param name An optional name for the server.
     * @exception IOException If save game can not be found.
     * @exception FreeColException If the savegame could not be loaded.
     * @exception XMLStreamException If the server comms fail.
     */
    public FreeColServer(final FreeColSavegameFile savegame, 
                         Specification specification, int port, String name)
        throws FreeColException, IOException, XMLStreamException {
        this(name, port);

        this.serverGame = loadGame(savegame, specification);
        // NationOptions will be read from the saved game.
        Session.clearAll();

        // Replace the PRNG in the game if it is missing or a command line
        // option was present.
        long seed = FreeColSeed.getFreeColSeed(this.random == null);
        if (seed != FreeColSeed.DEFAULT_SEED) {
            this.random = new Random(seed);
        }
        this.inGameController.setRandom(random);
        this.mapGenerator = null;
        registerWithMetaServer();
    }


    // Basic accessors

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
     * Get the public server state.
     *
     * @return The public server state.
     */
    public synchronized boolean getPublicServer() {
        return this.publicServer;
    }

    /**
     * Sets the public server state.
     *
     * @param publicServer The new public server state.
     */
    public synchronized void setPublicServer(boolean publicServer) {
        this.publicServer = publicServer;
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
     * Gets the server random number generator.
     *
     * @return The server random number generator.
     */
    public Random getServerRandom() {
        return this.random;
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
     * Gets the {@code Game} that is being played.
     *
     * @return The {@code Game} which is the main class of the game-model
     *     being used in this game.
     */
    public ServerGame getGame() {
        return this.serverGame;
    }

    /**
     * Sets the {@code Game} that is being played.
     *
     * @param serverGame The new {@code Game}.
     */
    public void setGame(ServerGame serverGame) {
        this.serverGame = serverGame;
    }

    /**
     * Gets the specification from the game run by this server.
     *
     * @return The specification from the game.
     */
    public Specification getSpecification() {
        return (this.serverGame == null) ? null
            : this.serverGame.getSpecification();
    }

    /**
     * Gets the network server responsible of handling the connections.
     *
     * @return The network server.
     */
    public Server getServer() {
        return this.server;
    }

    /**
     * Gets the host this FreeColServer was started on.
     *
     * @return The host.
     */
    public String getHost() {
        return (this.server == null) ? null : this.server.getHost();
    }

    /**
     * Gets the port this FreeColServer was started on.
     *
     * @return The port.
     */
    public int getPort() {
        return (this.server == null) ? -1 : this.server.getPort();
    }

    /**
     * Shut down this FreeColServer.
     */
    public void shutdown() {
        this.server.shutdown();
    }

    /**
     * Gets the current state of the server.
     *
     * @return The current {@code ServerState}.
     */
    public ServerState getServerState() {
        return this.serverState;
    }

    /**
     * Change the server state.
     *
     * @param serverState The new {@code ServerState}.
     * @return The old server state.
     */
    private ServerState changeServerState(ServerState serverState) {
        ServerState ret = this.serverState;
        switch (this.serverState = serverState) {
        case PRE_GAME: case LOAD_GAME: case IN_GAME:
            getServer().setMessageHandlerToAllConnections(this.inputHandler);
            break;
        case END_GAME: default:
            getServer().setMessageHandlerToAllConnections(null);
            break;
        }
        return ret;
    }

    /**
     * Gets the {@code UserConnectionHandler}.
     *
     * @return The {@code UserConnectionHandler} that is used when a
     *     new client connects.
     */
    public UserConnectionHandler getUserConnectionHandler() {
        return this.userConnectionHandler;
    }

    /**
     * Gets the {@code Controller}.
     *
     * @return The {@code Controller}.
     */
    public Controller getController() {
        return (getServerState() == ServerState.IN_GAME)
            ? this.inGameController
            : this.preGameController;
    }

    /**
     * Get the current input handler.
     *
     * @return The {@code ServerInputHandler} for the current server state.
     */
    public ServerInputHandler getInputHandler() {
        switch (this.serverState) {
        case PRE_GAME: case LOAD_GAME: case IN_GAME:
            return this.inputHandler;
        default:
            break;
        }
        return null;
    }

    /**
     * Gets the {@code PreGameController}.
     *
     * @return The {@code PreGameController}.
     */
    public PreGameController getPreGameController() {
        return this.preGameController;
    }

    /**
     * Gets the controller being used while the game is running.
     *
     * @return The controller from making a new turn etc.
     */
    public InGameController getInGameController() {
        return this.inGameController;
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
        return this.aiMain;
    }

    /**
     * Get the map generator.
     *
     * @return The {@code MapGenerator}.
     */
    public MapGenerator getMapGenerator() {
        return this.mapGenerator;
    }

    /**
     * Set the map generator.
     *
     * @param mapGenerator The new {@code MapGenerator}.
     */
    public void setMapGenerator(MapGenerator mapGenerator) {
        this.mapGenerator = mapGenerator;
    }

    /**
     * Gets the integrity check result.
     *
     * @return The integrity check result.
     */
    public IntegrityType getIntegrity() {
        return this.integrity;
    }

    // Higher level operations

    /**
     * The game is ending, tell all the non-admin players to quit.
     */
    public void endGame() {
        changeServerState(ServerState.END_GAME);
        for (Player p : getGame().getLiveEuropeanPlayerList()) {
            if (p.isAdmin()) continue;
            p.send(new ChangeSet()
                .add(See.only(p),
                     new LogoutMessage(p, LogoutReason.QUIT)));
        }
    }
        
    /**
     * Add a new user connection.  That is a new connection to the server
     * that has not yet logged in as a player.
     *
     * @param socket The client {@code Socket} the connection arrives on.
     * @exception FreeColException on extreme confusion.
     * @exception IOException if the socket was already broken.
     * @exception XMLStreamException on stream problem.
     */
    public void addNewUserConnection(Socket socket)
        throws FreeColException, IOException, XMLStreamException {
        final String name = socket.getInetAddress() + ":" + socket.getPort();
        Connection c = new Connection(socket, FreeCol.SERVER_THREAD + name)
            .setMessageHandler(this.userConnectionHandler);
        getServer().addConnection(c);
        // Short delay here improves reliability
        c.startReceiving();
        //delay(100, "New connection delay interrupted");
        c.send(new GameStateMessage(this.serverState));
        if (this.serverState == ServerState.IN_GAME) {
            c.send(new VacantPlayersMessage().setVacantPlayers(getGame()));
        }
        logger.info("Client connected from " + name);
    }

    /**
     * Add player connection.  That is, a user connection is now
     * transitioning to a player connection.
     *
     * @param connection The new {@code Connection}.
     */
    public void addPlayerConnection(Connection connection) {
        switch (this.serverState) {
        case PRE_GAME: case LOAD_GAME: case IN_GAME:
            connection.setMessageHandler(this.inputHandler);
            break;
        case END_GAME: default:
            return;
        }            
        getServer().addConnection(connection);
        updateMetaServer();
    }

    /**
     * Remove a player connection.
     *
     * @param player The {@code Player} to disconnect.
     */
    public void removePlayerConnection(Player player) {
        Connection conn = player.getConnection();
        if (conn != null) {
            getServer().removeConnection(conn);
            conn.close();
            player.setConnection(null);
        }
    }

    /**
     * Establish the connections for an AI player.
     *
     * @param aiPlayer The AI {@code Player} to connect.
     */
    private void addAIConnection(Player aiPlayer) {
        DummyConnection theConnection
            = new DummyConnection("Server-to-AI-" + aiPlayer.getSuffix());
        theConnection.setMessageHandler(this.inputHandler);
        theConnection.setWriteScope(FreeColXMLWriter.WriteScope
            .toClient(aiPlayer));
        DummyConnection aiConnection
            = new DummyConnection("AI-" + aiPlayer.getSuffix() + "-to-Server");
        aiConnection.setMessageHandler(new AIInGameInputHandler(this, aiPlayer, getAIMain()));
        aiConnection.setWriteScope(FreeColXMLWriter.WriteScope.toServer());
        aiConnection.setOtherConnection(theConnection);
        theConnection.setOtherConnection(aiConnection);
        aiPlayer.setConnection(theConnection);
        getServer().addDummyConnection(theConnection);
    }
        
    /**
     * Wait until the game has been created.
     *
     * 2017: This should not be needed, but just occasionally the
     * client and server threads conspire to race in a way that
     * causes a hang.  Check this again in due course.
     *
     * @return The {@code ServerGame}, or null if the wait times out.
     */
    public ServerGame waitForGame() {        
        final int timeStep = 1000;
        int timeOut = 20000;
        ServerGame serverGame = null;
        while ((serverGame = getGame()) == null) {
            delay(timeStep, "waitForGame delay interrupt");
            if ((timeOut -= timeStep) <= 0) break;
        }
        return serverGame;
    }

    /**
     * Start the game.
     *
     * Called from PreGameController following a requestLaunch message
     * (or from the test suite).
     *
     * <ol>
     *   <li>Creates the game.
     *   <li>Sends updated game information to the clients.
     *   <li>Changes the game state to
     *         {@link net.sf.freecol.server.FreeColServer.ServerState#IN_GAME}.
     *   <li>Sends the "startGame"-message to the clients.
     *   <li>Switches to using the in-game version of the input handler.
     * </ol>
     *
     * @exception FreeColException if there is a problem creating the game.
     */
    public void startGame() throws FreeColException {
        logger.info("Server starting game: " + this.serverState);

        switch (this.serverState) {
        case PRE_GAME: // Send the updated game to the clients.
            final Game game = buildGame();
            for (Player player : transform(game.getLivePlayers(),
                                           p -> !p.isAI())) {
                player.invalidateCanSeeTiles();
                ChangeSet cs = new ChangeSet();
                cs.add(See.only(player), game);
                player.send(cs);
            }
            break;
        case LOAD_GAME: // Do nothing, game has been sent.
            break;
        default:
            logger.warning("Invalid startGame when server state = "
                + this.serverState);
            return;
        }

        changeServerState(ServerState.IN_GAME);
        sendToAll(TrivialMessage.startGameMessage, (Player)null);
        updateMetaServer();
    }

    /**
     * Send a message to all connections.
     *
     * @param msg The {@code Message} to send.
     * @param conn An optional {@code Connection} to omit.
     */
    private void sendToAll(Message msg, Connection conn) {
        getServer().sendToAll(msg, conn);
    }

    /**
     * Send a message to all connections.
     *
     * @param msg The {@code Message} to send.
     * @param player An optional {@code Player} to omit.
     */
    public void sendToAll(Message msg, Player player) {
        sendToAll(msg, (player == null) ? null : player.getConnection());
    }


    // Game save/load

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
        // Mask out spec while saving map.
        Specification spec = getSpecification();
        getGame().setSpecification(null);
        saveGame(file, MAP_EDITOR_NAME, null, null, image);
        getGame().setSpecification(spec);
    }

    /**
     * Saves a normal (non-map-editor) game.
     *
     * @param file The file where the data will be written.
     * @param options The client options to save.
     * @param active An optional active {@code Unit}.
     * @exception IOException If a problem was encountered while trying
     *     to open, write or close the file.
     */
    public void saveGame(File file, OptionGroup options, Unit active)
        throws IOException {
        saveGame(file, null, options, active, null);
    }

    /**
     * Saves a game.
     *
     * @param file The file where the data will be written.
     * @param owner An optional name to use as the owner of the game.
     * @param options Optional client options to save in the game.
     * @param active An optional active {@code Unit}.
     * @param image A thumbnail {@code Image} value to save in the game.
     * @exception IOException If a problem was encountered while trying
     *     to open, write or close the file.
     */
    private void saveGame(File file, String owner, OptionGroup options,
                          Unit active, BufferedImage image) throws IOException {
        // Try to GC now before launching into the save, as a failure
        // here can lead to a corrupt saved game file (BR#3146).
        // Alas, gc is only advisory, but it is all we have got.
        garbageCollect();
        try (JarOutputStream fos = new JarOutputStream(Files
                .newOutputStream(file.toPath()))) {
            if (image != null) {
                fos.putNextEntry(new JarEntry(FreeColSavegameFile.THUMBNAIL_FILE));
                ImageIO.write(image, "png", fos);
                fos.closeEntry();
            }

            if (options != null) {
                fos.putNextEntry(new JarEntry(FreeColSavegameFile.CLIENT_OPTIONS));
                options.save(fos, null, true);
                fos.closeEntry();
            }

            Properties properties = new Properties();
            properties.setProperty("map.width",
                Integer.toString(this.serverGame.getMap().getWidth()));
            properties.setProperty("map.height",
                Integer.toString(this.serverGame.getMap().getHeight()));
            fos.putNextEntry(new JarEntry(FreeColSavegameFile.SAVEGAME_PROPERTIES));
            properties.store(fos, null);
            fos.closeEntry();

            // save the actual game data
            fos.putNextEntry(new JarEntry(FreeColSavegameFile.SAVEGAME_FILE));
            try (
                // throws IOException                 
                FreeColXMLWriter xw = new FreeColXMLWriter(fos,
                    FreeColXMLWriter.WriteScope.toSave(), false)) {
                xw.writeStartDocument("UTF-8", "1.0");

                xw.writeComment(FreeCol.getConfiguration().toString());
                xw.writeCharacters("\n");

                xw.writeStartElement(SAVED_GAME_TAG);

                // Add the attributes:
                xw.writeAttribute(OWNER_TAG,
                                  (owner != null) ? owner : FreeCol.getName());

                xw.writeAttribute(PUBLIC_SERVER_TAG, this.getPublicServer());

                xw.writeAttribute(SINGLE_PLAYER_TAG, this.singlePlayer);

                xw.writeAttribute(FreeColSavegameFile.VERSION_TAG,
                                  SAVEGAME_VERSION);

                xw.writeAttribute(RANDOM_STATE_TAG,
                                  getRandomState(this.random));

                xw.writeAttribute(DEBUG_TAG, FreeColDebugger.getDebugModes());

                if (active != null) {
                    this.serverGame.setInitialActiveUnitId(active.getId());
                }

                this.serverGame.toXML(xw); // Add the game

                if (this.aiMain != null) { // Add the AIObjects
                    this.aiMain.toXML(xw);
                }

                xw.writeEndElement();
                xw.writeEndDocument();
            }
            fos.closeEntry();
        } catch (XMLStreamException e) {
            throw new IOException("Failed to save (XML): " + file.getName(), e);
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
        return loadGame(fis, null);
    }

    /**
     * Read just the map part from a file.
     *
     * When the specification is not supplied, the one found in the saved
     * game will be used.
     *
     * @param file The {@code File} to read from.
     * @param spec An optional {@code Specification} to use.
     * @return The {@code Map} found in the stream.
     * @exception FreeColException if the format is incompatible.
     * @exception IOException if the stream can not be created.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    public static Map readMap(File file, Specification spec)
        throws FreeColException, IOException, XMLStreamException {
        ServerGame serverGame = readGame(file, spec, null);
        return (serverGame == null) ? null : serverGame.getMap();
    }
    
    /**
     * Read just the game part from a file.
     *
     * When the specification is not supplied, the one found in the saved
     * game will be used.
     *
     * @param file The {@code File} to read from.
     * @param spec An optional {@code Specification} to use.
     * @param freeColServer Use this (optional) server to load into.
     * @return The game found in the stream.
     * @exception FreeColException if the format is incompatible.
     * @exception IOException if the stream can not be created.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    private static ServerGame readGame(File file, Specification spec,
                                       FreeColServer freeColServer)
        throws FreeColException, IOException, XMLStreamException {
        ServerGame serverGame
            = FreeColServer.readGame(new FreeColSavegameFile(file),
                                     spec, freeColServer);
        logger.info("Read file " + file.getPath());

        // If importing as a result of "Start Game" in the map editor,
        // consume the file.
        File startGame = FreeColDirectories.getStartMapFile();
        if (startGame != null
            && startGame.getPath().equals(file.getPath())) deleteFile(file);
        return serverGame;
    }

    /**
     * Reads just the game part from a save game from a stream.
     *
     * When the specification is not supplied, the one found in the saved
     * game will be used.
     *
     * @param fis The stream to read from.
     * @param specification An optional {@code Specification} to use.
     * @param freeColServer Use this (optional) server to load into.
     * @return The game found in the stream.
     * @exception FreeColException if the format is incompatible.
     * @exception IOException if the stream can not be created.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    public static ServerGame readGame(final FreeColSavegameFile fis,
                                      Specification specification,
                                      FreeColServer freeColServer)
        throws FreeColException, IOException, XMLStreamException {
        final int savegameVersion = fis.getSavegameVersion();
        logger.info("Found savegame version " + savegameVersion);
        if (savegameVersion < MINIMUM_SAVEGAME_VERSION) {
            throw new FreeColException("server.incompatibleVersions: "
                + savegameVersion + " < " + MINIMUM_SAVEGAME_VERSION);
        }

        ServerGame serverGame = null;
        try (
            FreeColXMLReader xr = fis.getSavedGameFreeColXMLReader();
        ) {
            // Switch to the read scope that creates server objects.
            xr.setReadScope(FreeColXMLReader.ReadScope.SERVER);
            
            String active = null;
            xr.nextTag();

            if (freeColServer != null) {
                String owner = xr.getAttribute(OWNER_TAG, (String)null);
                if (specification == null && MAP_EDITOR_NAME.equals(owner)) {
                    throw new FreeColException("Can not start a map editor map as a game: " + fis.getPath());
                }
                freeColServer.setSinglePlayer(xr.getAttribute(SINGLE_PLAYER_TAG,
                                                              true));

                freeColServer.setPublicServer(xr.getAttribute(PUBLIC_SERVER_TAG,
                                                              false));

                String r = xr.getAttribute(RANDOM_STATE_TAG, (String)null);
                freeColServer.setServerRandom(restoreRandomState(r));
                    
                FreeColDebugger.setDebugModes(xr.getAttribute(DEBUG_TAG,
                                                              (String)null));

                // @compat 0.11.6
                active = xr.getAttribute(ACTIVE_UNIT_TAG, (String)null);
                // end @compat 0.11.6
            }

            while (xr.moreTags()) {
                final String tag = xr.getLocalName();
                // @compat 0.11.6
                if (OLD_SERVER_OBJECTS_TAG.equals(tag)) { // No longer used
                    xr.swallowTag(OLD_SERVER_OBJECTS_TAG);
                // end @compat 0.11.6

                } else if (Game.TAG.equals(tag)) {
                    // Read the game
                    serverGame = new ServerGame(specification, xr);
                    serverGame.setCurrentPlayer(null);
                    if (freeColServer != null) {
                        freeColServer.setGame(serverGame);
                    }

                } else if (AIMain.TAG.equals(tag)) {
                    if (freeColServer != null) {
                        AIMain aiMain = new AIMain(freeColServer, xr);
                        freeColServer.setAIMain(aiMain);
                    } else { // Reading a map, ignore AI
                        xr.swallowTag(AIMain.TAG);
                    }

                } else {
                    throw new XMLStreamException("Unknown tag"
                        + " reading server game: " + tag);
                }
            }

            // @compat 0.11.6
            if (serverGame != null && active != null) {
                serverGame.setInitialActiveUnitId(active);
            }
            // end @compat 0.11.6
        }
        return serverGame;
    }

    /**
     * Loads a game.
     *
     * @param fis The file where the game data is located.
     * @param specification The {@code Specification} to refer to.
     * @return The new game.
     * @exception FreeColException if the savegame contains incompatible data.
     * @exception IOException if the stream can not be created.
     * @exception XMLStreamException if there a problem reading the stream.
     */
    private ServerGame loadGame(final FreeColSavegameFile fis,
                                Specification specification)
        throws FreeColException, IOException, XMLStreamException {
        changeServerState(ServerState.LOAD_GAME);
        ServerGame serverGame = readGame(fis, specification, this);
        LogBuilder lb = new LogBuilder(512);
        this.integrity = serverGame.checkIntegrity(true, lb);
        switch (integrity) {
        case INTEGRITY_GOOD:
            logger.info("Game integrity test succeeded.");
            break;
        case INTEGRITY_FIXED:
            logger.info("Game integrity test failed, but fixed." + lb);
            break;
        default:
            logger.warning("Game integrity test failed." + lb);
            break;
        }

        serverGame.getMap().resetContiguity();
        serverGame.establishUnknownEnemy();

        // Ensure that critical option groups can not be edited.
        specification = getSpecification();
        specification.disableEditing();

        // AI initialization.
        AIMain aiMain = getAIMain();
        lb.truncate(0);
        IntegrityType aiIntegrity = IntegrityType.INTEGRITY_GOOD;
        if (aiMain == null) {
            aiIntegrity = aiIntegrity.fail();
            lb.add("\n  AIMain missing.");
        } else {
            aiIntegrity = aiMain.checkIntegrity(true, lb);
        }
        switch (aiIntegrity) {
        case INTEGRITY_GOOD:
            logger.info("AI integrity test succeeded.");
            break;
        case INTEGRITY_FIXED:
            logger.info("AI integrity test failed, but fixed." + lb);
            break;
        default:
            aiMain = new AIMain(this);
            aiMain.findNewObjects(true);
            setAIMain(aiMain);
            logger.warning("AI integrity test failed, replaced AIMain." + lb);
            break;
        }
        serverGame.setFreeColGameObjectListener(aiMain);

        serverGame.sortPlayers(Player.playerComparator);

        for (Player player : serverGame.getLivePlayerList()) {
            if (player.isAI()) {
                addAIConnection(player);
            }
            if (player.isEuropean()) {
                // The map will be invalid, so trigger a recalculation of the
                // canSeeTiles, by calling canSee for an arbitrary tile.
                player.canSee(serverGame.getMap().getTile(0, 0));
            }
        }

        return serverGame;
    }

    /**
     * Builds a new game using the parameters that exist in the game
     * as it stands.
     *
     * @return The updated {@code Game}.
     * @exception FreeColException on map generation failure.
     */
    private Game buildGame() throws FreeColException {
        final ServerGame serverGame = getGame();
        final Specification spec = serverGame.getSpecification();

        // Establish AI main if missing
        if (getAIMain() == null) {
            AIMain aiMain = new AIMain(this);
            serverGame.setFreeColGameObjectListener(aiMain);
            setAIMain(aiMain);
        }

        // We need a fake unknown enemy player
        serverGame.establishUnknownEnemy();

        // Fill in missing available players, and the unknown enemy
        final Predicate<Entry<Nation, NationState>> availablePred = e ->
            e.getValue() != NationState.NOT_AVAILABLE
            && serverGame.getPlayerByNationId(e.getKey().getId()) == null;
        List<ServerPlayer> toUpdate
            = transform(serverGame.getNationOptions().getNations().entrySet(),
                        availablePred, e -> makeAIPlayer(e.getKey()),
            Player.playerComparator);
        toUpdate.add((ServerPlayer)serverGame.getUnknownEnemy());
        serverGame.updatePlayers(toUpdate);

        // Create the map.
        if (serverGame.getMap() == null) {
            generateMap();
        
            // Initial stances and randomizations for all players.
            spec.generateDynamicOptions();
            Random random = getServerRandom();
            for (Player player : serverGame.getLivePlayerList()) {
                ((ServerPlayer)player).randomizeGame(random);
                if (player.isIndian()) {
                    // Indian players know about each other, but
                    // European colonial players do not.
                    final int alarm = (Tension.Level.HAPPY.getLimit()
                        + Tension.Level.CONTENT.getLimit()) / 2;
                    for (Player other : serverGame.getLiveNativePlayerList(player)) {
                        player.setStance(other, Stance.PEACE);
                        for (IndianSettlement is : player.getIndianSettlementList()) {
                            is.setAlarm(other, new Tension(alarm));
                        }
                    }
                }
            }
        }

        // Ensure that option groups can not be edited any more.
        spec.getMapGeneratorOptions().setEditable(false);
        spec.getGameOptions().setEditable(false);
        spec.getOptionGroup(Specification.DIFFICULTY_LEVELS).setEditable(false);

        // Let the AIMain scan for objects it should be managing.
        getAIMain().findNewObjects(true);

        return serverGame;
    }

    /**
     * Generate an empty map in this server/game.
     *
     * Public for the map generator.
     *
     * @param width The map width.
     * @param height The map height.
     * @return The new empty {@code Map}.
     */
    public Map generateEmptyMap(int width, int height) {
        return getMapGenerator().generateEmptyMap(getGame(), width, height,
                                                  new LogBuilder(-1));
    }

    /**
     * Generate a new map in this server/game.
     *
     * @return The {@code Map} that was created.
     */
    public Map generateMap() {
        ServerGame serverGame = getGame();
        LogBuilder lb = new LogBuilder(256);
        File importFile = serverGame.getMapGeneratorOptions()
            .getFile(MapGeneratorOptions.IMPORT_FILE);
        Map importMap = null;
        if (importFile != null) {
            try {
                importMap = FreeColServer.readMap(importFile,
                    serverGame.getSpecification());
            } catch (FreeColException|IOException|XMLStreamException ex) {
                logger.log(Level.WARNING, "Failed to import map: "
                    + importFile.getName(), ex);
            }
        }
        Map ret = getMapGenerator().generateMap(serverGame, importMap, lb);
        lb.shrink("\n");
        lb.log(logger, Level.FINER);
        return ret;
    }

    /**
     * Make a new AI player and add it to the game.
     *
     * Public so the controller can add REF players.
     *
     * @param nation The {@code Nation} to add.
     * @return The new AI {@code ServerPlayer}.
     */
    public ServerPlayer makeAIPlayer(Nation nation) {
        ServerPlayer aiPlayer = new ServerPlayer(getGame(), false, nation);
        aiPlayer.setAI(true);
        aiPlayer.setReady(true);
        addAIConnection(aiPlayer);

        getGame().addPlayer(aiPlayer);
        // Add to the AI, which was previously deferred because the
        // player type was unknown.
        getAIMain().setFreeColGameObject(aiPlayer.getId(), aiPlayer);
        return aiPlayer;
    }

    /**
     * Reveals or hides the entire map for all players.
     * Debug menu helper.
     *
     * @param reveal If true, reveal, if false, hide.
     */
    public void exploreMapForAllPlayers(boolean reveal) {
        final Specification spec = getSpecification();

        for (Player p : getGame().getLiveEuropeanPlayerList()) {
            ((ServerPlayer)p).exploreMap(reveal);
        }
     
        // Removes fog of war when revealing the whole map
        // Restores previous setting when hiding it back again
        if (reveal) {
            FreeColDebugger.setNormalGameFogOfWar(spec.getBoolean(GameOptions.FOG_OF_WAR));
            spec.setBoolean(GameOptions.FOG_OF_WAR, false);
        } else {
            spec.setBoolean(GameOptions.FOG_OF_WAR,
                            FreeColDebugger.getNormalGameFogOfWar());
        }

        for (Player p : getGame().getLiveEuropeanPlayerList()) {
            p.getConnection().sendReconnect();
        }
    }

    /**
     * Get a {@code ServerPlayer} by connection.
     *
     * @param conn The {@code Connection} to use while searching for a
     *     {@code ServerPlayer}.
     * @return The {@code ServerPlayer} found, or null if none.
     */
    public ServerPlayer getPlayer(final Connection conn) {
        final Predicate<Player> connPred
            = matchKeyEquals(conn, Player::getConnection);
        return (ServerPlayer)getGame().getPlayer(connPred);
    }

    /**
     * Gets the AI player corresponding to a given player.
     *
     * @param player The {@code Player} to look up.
     * @return The corresponding AI player, or null if not found.
     */
    public AIPlayer getAIPlayer(Player player) {
        return getAIMain().getAIPlayer(player);
    }


    // MetaServer handling

    /**
     * Create a {@code ServerInfo} record for this server and connection.
     *
     * @return A suitable record.
     */
    private ServerInfo getServerInfo() {
        final Predicate<Player> absentAI
            = p -> !p.isREF() && p.isAI() && !p.isConnected();
        final Predicate<Player> liveHuman
            = p -> !p.isAI() && p.isConnected();
        int slots = count(getGame().getLiveEuropeanPlayers(), absentAI);
        int players = count(getGame().getLiveEuropeanPlayers(), liveHuman);
        return new ServerInfo(getName(),
                              null, -1, // Missing these at this point
                              slots, players,
                              this.serverState == ServerState.IN_GAME,
                              FreeCol.getVersion(),
                              getServerState().ordinal());
    }

    /**
     * Register this FreeColServer with the meta-server.
     *
     * @return True if the meta-server was updated.
     */
    public synchronized boolean registerWithMetaServer() {
        if (!this.publicServer) return false;
        boolean ret = MetaServerUtils.registerServer(getServerInfo());
        if (!ret) this.publicServer = false;
        return ret;
    }

    /**
     * Removes this server from the meta-server.
     *
     * @return True if the meta-server was updated.
     */
    public synchronized boolean removeFromMetaServer() {
        if (!this.publicServer) return false;
        boolean ret = MetaServerUtils.removeServer(getServerInfo());
        if (!ret) this.publicServer = false;
        return ret;
    }

    /**
     * Update this FreeColServer with the meta-server.
     *
     * @return True if the meta-server was updated.
     */
    public synchronized boolean updateMetaServer() {
        if (!this.publicServer) return false;
        boolean ret = MetaServerUtils.updateServer(getServerInfo());
        if (!ret) this.publicServer = false;
        return ret;
    }
}
