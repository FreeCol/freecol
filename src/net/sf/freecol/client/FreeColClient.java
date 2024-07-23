/**
 *  Copyright (C) 2002-2024   The FreeCol Team
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

package net.sf.freecol.client;

import java.awt.Dimension;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;
import javax.xml.stream.XMLStreamException;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.control.ClientInputHandler;
import net.sf.freecol.client.control.ConnectController;
import net.sf.freecol.client.control.InGameController;
import net.sf.freecol.client.control.MapEditorController;
import net.sf.freecol.client.control.PreGameController;
import net.sf.freecol.client.control.SoundController;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.SplashScreen;
import net.sf.freecol.client.gui.SwingGUI;
import net.sf.freecol.client.gui.action.ActionManager;
import net.sf.freecol.client.networking.UserServerAPI;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColDataFile;
import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.io.FreeColSavegameFile;
import net.sf.freecol.common.io.FreeColTcFile;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Game.LogoutReason;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.MessageHandler;
import net.sf.freecol.common.networking.ServerAPI;
import net.sf.freecol.common.resources.ImageResource;
import net.sf.freecol.common.resources.ResourceManager;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.FreeColServer.ServerState;


/**
 * The main control class for the FreeCol client.  This class both
 * starts and keeps references to the GUI and the control objects.
 */
public final class FreeColClient {

    private static final Logger logger = Logger.getLogger(FreeColClient.class.getName());

    @SuppressWarnings("unused")
    private byte[] memoryToBeFreedOnOutOfMemory = new byte[10 * 1024 * 1024];
    
    private final ConnectController connectController;

    private final PreGameController preGameController;

    private final InGameController inGameController;

    private final ClientInputHandler inputHandler;

    private final MapEditorController mapEditorController;

    private SoundController soundController;

    /** The server that has been started from the client-GUI. */
    private FreeColServer freeColServer = null;

    /** Encapsulation of the server API. */
    private final ServerAPI serverAPI;

    /** The GUI encapsulation. */
    private GUI gui;

    /** The encapsulation of the actions. */
    private final ActionManager actionManager;

    /** The game itself. */
    private Game game;

    /** The player that `owns' this client. */
    private Player player;

    /** The client options specific to this player. */
    private ClientOptions clientOptions;

    /**
     * Indicates if the game has started, has nothing to do with
     * whether or not the client is logged in.
     */
    private boolean inGame = false;

    /** Are we using the map editor? */
    private boolean mapEditor;

    /** Is this a single player game? */
    private boolean singlePlayer;

    /**
     * Indicated whether or not there is an open connection to the
     * server. This is not an indication of the existence of a
     * Connection Object, but instead it is an indication of an
     * approved login to a server.
     */
    private boolean loggedIn = false;

    /** Cached value of server state. */
    private ServerState cachedServerState = null;

    /** Cached list of vacant players. */
    private List<String> cachedVacantPlayerNames = new ArrayList<>();


    /**
     * Creates a new {@code FreeColClient}.  Creates the control
     * objects.
     *
     * @param splashScreen An optional splash screen to display.
     * @param fontName An optional override of the main font.
     * @param windowSize An optional window size.
     * @param userMsg An optional message key to be displayed early.
     * @param sound True if sounds should be played
     * @param showOpeningVideo Display the opening video.
     * @param savedGame An optional saved game.
     * @param spec If non-null, a {@code Specification} to use to start
     *     a new game immediately.
     */
    public FreeColClient(final SplashScreen splashScreen,
                         final String fontName,
                         final Dimension windowSize,
                         final String userMsg,
                         final boolean sound,
                         final boolean showOpeningVideo,
                         final File savedGame,
                         final Specification spec) {
        String quitName = FreeCol.CLIENT_THREAD + "Quit Game";
        Runtime.getRuntime().addShutdownHook(new Thread(quitName) {
                /**
                 * {@inheritDoc}
                 */
                @Override
                public void run() {
                    stopServer();
                }
            });
        
        if (FreeCol.getHeadless() && savedGame == null && spec == null) {
            FreeCol.fatal(logger, Messages.message("client.headlessRequires"));
        }
        
        mapEditor = false;

        // Look for base data directory and get base resources loaded.
        // Failure is fatal.
        File baseDirectory = FreeColDirectories.getBaseDirectory();
        FreeColDataFile baseData = null;
        String ioeMessage = null;
        if (baseDirectory.exists() && baseDirectory.isDirectory()) {
            try {
                baseData = new FreeColDataFile(baseDirectory);
            } catch (IOException ioe) {
                ioeMessage = ioe.getMessage();
            }
        }
        if (baseData == null) {
            FreeCol.fatal(logger,
                Messages.message(StringTemplate.template("client.baseData")
                    .addName("%dir%", baseDirectory.getName()))
                + ((ioeMessage == null) ? "" : "\n" + ioeMessage));
        }

        this.serverAPI = new UserServerAPI();

        // Control.  Controllers expect GUI to be available.
        connectController = new ConnectController(this);
        preGameController = new PreGameController(this);
        inGameController = new InGameController(this);
        mapEditorController = new MapEditorController(this);
        inputHandler = new ClientInputHandler(this);
        setMessageHandler(this.inputHandler);

        // Load resources.
        //   - base resources
        //   - resources in the default "classic" ruleset,
        //   - resources in the default actions
        //
        // FIXME: probably should not need to load "classic", but there
        // are a bunch of things in there that need to move to base because
        // the action manager requires them.
        //
        // Not so easy, since the ActionManager also creates tile
        // improvement actions, which depend on the specification.
        // However, this step could probably be delayed.
        ResourceManager.setBaseData(baseData);
        
        final FreeColTcFile tcData = FreeColTcFile.getFreeColTcFile(FreeCol.getTc());
        ResourceManager.setTcData(tcData);

        actionManager = new ActionManager(this);
        actionManager.initializeActions(inGameController, connectController);

        // Load the client options, which handle reloading the
        // resources specified in the active mods.
        this.clientOptions = loadClientOptions(savedGame);
        this.clientOptions.fixClientOptions();

        // Reset the mod resources as a result of the client option update.
        ResourceManager.setMods(this.clientOptions.getActiveMods());
        
        if (this.clientOptions.getRange(ClientOptions.GRAPHICS_QUALITY) == ClientOptions.GRAPHICS_QUALITY_LOWEST) {
            ImageResource.forceLowestQuality(true);
        }
        
        /*
         * All mods are loaded, so the GUI can safely be created.
         */

        gui = (FreeCol.getHeadless()) ? new GUI(this)
                : new SwingGUI(this);
        
        // Swing system and look-and-feel initialization.
        if (!FreeCol.getHeadless()) {
            try {
                gui.installLookAndFeel(fontName);
            } catch (Exception e) {
                FreeCol.fatal(logger,
                        Messages.message("client.laf") + "\n" + e.getMessage());
            }
        }
        
        // Initialize Sound (depends on client options)
        this.soundController = new SoundController(this, sound);
        
        overrideDefaultUncaughtExceptionHandler();
        
        /*
         * Please do NOT move preloading before mods are loaded -- as that
         * might cause some images to be loaded from base and other images
         * to be loaded from mods.
         */
        ResourceManager.startPreloading(() -> {
            /*
             * We can allow the GUI to be displayed while the preloading is running.
             * 
             * In that case we need to add that preloading gets aborted before
             * ResourceManager.addMapping is called (which is now performed when
             * loading a game). 
             */
    
            /*
             * Run later on the EDT so that we ensure pending GUI actions have
             * completed.
             */
            SwingUtilities.invokeLater(() -> {
                if (splashScreen != null) {
                    splashScreen.setVisible(false);
                    splashScreen.dispose();
                }
                // Start the GUI (headless-safe)
                gui.startGUI(windowSize);
        
                // Update the actions with the running GUI, resources may have changed.
                if (this.actionManager != null) updateActions();
                
                startFirstTaskInGui(userMsg, showOpeningVideo, savedGame, spec);
            });
        });
    }

    private void startFirstTaskInGui(String userMsg, boolean showOpeningVideo, File savedGame,
            Specification spec) {
        // Now the GUI is going, either:
        //   - load the saved game if one was supplied
        //   - use the debug shortcut to immediately start a new game with
        //     supplied specification
        //   - display the opening video (which goes on to display the
        //     main panel when it completes)
        //   - display the main panel and let the user choose what to
        //     do (which will often be to progress through the
        //     NewPanel to a call to the connect controller to start a game)
        //
                
        SwingUtilities.invokeLater(() -> {
            /*
             * About "SwingUtilities.invokeLater"
             * 
             * Yes, we are on the EDT -- but this should still be executed
             * AFTER the loading of Canvas etc.
             */
            
            if (savedGame != null) { // Restore from saved
                gui.showStatusPanel(Messages.message("status.loadingGame"));
                SwingUtilities.invokeLater(() -> {
                    if (connectController.startSavedGame(savedGame)) {
                        gui.closeStatusPanel();
                        if (userMsg != null) {
                            gui.showInformationPanel(userMsg);
                        }
                    } else {
                        gui.playSound("sound.intro.general");
                        gui.closeStatusPanel();
                        gui.showMainPanel(userMsg);
                    }
                });
    
            } else if (spec != null) { // Debug or fast start
                gui.playSound("sound.intro.general");
                if (!connectController.startSinglePlayerGame(spec)) {
                    gui.showMainPanel(userMsg);
                }
            } else if (showOpeningVideo) { // Video first
                gui.showOpeningVideo(userMsg, () -> {
                        gui.playSound("sound.intro.general");
                        gui.showMainPanel(userMsg);
                    });
            } else { // Start main panel
                gui.playSound("sound.intro.general");
                gui.showMainPanel(userMsg);
            }
        });
    }

    /**
     * Loads the client options.
     * There are several sources:
     *   1) Base options (data/base/client-options.xml)
     *   2) Standard action manager actions
     *   3) Saved game
     *   4) User options
     *
     * The base and action manager options are definitive, so they can
     * just be added/loaded.  The others are from sources that may be
     * out of date (i.e. options can be in the wrong group, or no longer
     * exist), so they must be merged cautiously.
     *
     * @param savedGameFile An optional saved game {@code File}
     *     to load options from.
     * @return The loaded {@code ClientOptions}.
     */
    private ClientOptions loadClientOptions(File savedGameFile) {
        ClientOptions clop = new ClientOptions();
        logger.info("Load default client options.");
        clop.load(FreeColDirectories.getBaseClientOptionsFile());

        if (actionManager != null) {
            logger.info("Load client options from the action manager.");
            clop.add(actionManager);
        }

        if (savedGameFile != null) {
            try {
                FreeColSavegameFile fcsf
                    = new FreeColSavegameFile(savedGameFile);
                logger.info("Merge client options from saved game: "
                    + savedGameFile.getPath());
                clop.merge(fcsf);
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "Could not open saved game "
                    + savedGameFile.getPath(), ioe);
            }
        }

        final File userOptions = FreeColDirectories.getClientOptionsFile();
        if (userOptions != null && userOptions.exists()) {
            logger.info("Merge client options from user options file: "
                + userOptions.getPath());
            clop.load(userOptions);
        }

        //logger.info("Final client options: " + clop.toString());
        return clop;
    }


    // Accessors

    /**
     * Gets the controller responsible for starting a server and
     * connecting to it.
     *
     * @return The {@code ConnectController}.
     */
    public ConnectController getConnectController() {
        return connectController;
    }

    /**
     * Gets the controller that will be used before the game has been started.
     *
     * @return The {@code PreGameController}.
     */
    public PreGameController getPreGameController() {
        return preGameController;
    }

    /**
     * Gets the controller that will be used when the game has been started.
     *
     * @return The {@code InGameController}.
     */
    public InGameController getInGameController() {
        return inGameController;
    }

    /**
     * Gets the controller for the map editor, if we are in the map editor.
     *
     * @return The map editor controller, if any.
     */
    public MapEditorController getMapEditorController() {
        return mapEditorController;
    }

    /**
     * Gets the controller for the sound.
     *
     * @return The sound controller, if any.
     */
    public SoundController getSoundController() {
        return soundController;
    }

    /**
     * Gets the {@code FreeColServer} started by the client.
     *
     * @return The {@code FreeColServer} or {@code null} if no
     *     server has been started.
     */
    public FreeColServer getFreeColServer() {
        return freeColServer;
    }

    /**
     * Sets the {@code FreeColServer} which has been started by the
     * client gui.
     *
     * @param freeColServer The {@code FreeColServer}.
     * @see #getFreeColServer()
     */
    public void setFreeColServer(FreeColServer freeColServer) {
        this.freeColServer = freeColServer;
    }

    /**
     * Meaningfully named access to the ServerAPI.
     *
     * @return The user wrapper for the {@code ServerAPI}.
     */
    public ServerAPI askServer() {
        return this.serverAPI;
    }

    /**
     * Gets the GUI attached to this client.
     *
     * @return The current {@code GUI}.
     */
    public GUI getGUI() {
        return gui;
    }

    /**
     * Gets the action manager.
     *
     * @return The action manager.
     */
    public ActionManager getActionManager() {
        return actionManager;
    }

    /**
     * Gets the {@code Game} that we are currently playing.
     *
     * @return The current {@code Game}.
     * @see #setGame
     */
    public Game getGame() {
        return this.game;
    }

    /**
     * Sets the {@code Game} that we are currently playing.
     *
     * @param game The new {@code Game}.
     * @see #getGame
     */
    public void setGame(Game game) {
        this.game = game;
    }

    /**
     * Gets the {@code Player} that uses this client.
     *
     * @return The {@code Player} made to represent this clients user.
     * @see #setMyPlayer(Player)
     */
    public Player getMyPlayer() {
        return this.player;
    }

    /**
     * Sets the {@code Player} that uses this client.
     *
     * @param player The {@code Player} made to represent this clients
     *            user.
     * @see #getMyPlayer()
     */
    public void setMyPlayer(Player player) {
        this.player = player;
    }

    /**
     * Gets the object keeping the current client options.
     *
     * @return The {@code ClientOptions} attached to this
     *     {@code FreeColClient}.
     */
    public ClientOptions getClientOptions() {
        return this.clientOptions;
    }

    /**
     * Toggle the value of a boolean client option.
     *
     * @param op The name of the option to toggle.
     */
    public void toggleClientOption(String op) {
        boolean value = this.clientOptions.getBoolean(op);
        this.clientOptions.setBoolean(op, !value);
    }

    /**
     * Convenience accessor for checking whether to display tutorial messages.
     *
     * @return True if tutorial messages should be displayed.
     */
    public boolean tutorialMode() {
        return getClientOptions().getBoolean("model.option.guiShowTutorial");
    }

    /**
     * Has the game started?
     *
     * @return True if the game has started.
     */
    public synchronized boolean isInGame() {
        return this.inGame;
    }

    /**
     * Change the client in-game state (either in or pre-game).
     *
     * @param inGame If true, change to in-game state.
     */
    public synchronized void changeClientState(boolean inGame) {
        this.inGame = inGame;
    }

    /**
     * Is the client ready to switch to in-game mode?
     *
     * @return True if in pre-game mode, the game is present, and it has a map.
     */
    public boolean isReadyToStart() {
        if (isInGame()) return false;
        final Game game = getGame();
        return (game == null) ? false : game.getMap() != null;
    }

    /**
     * Are we using the map editor?
     *
     * @return True if the map editor is enabled.
     */
    public boolean isMapEditor() {
        return this.mapEditor;
    }

    /**
     * Sets the map editor state.
     *
     * @param mapEditor True if the map editor is enabled.
     */
    public void setMapEditor(boolean mapEditor) {
        this.mapEditor = mapEditor;
    }

    /**
     * Is the user playing in single player mode?
     *
     * @return True if the user is playing in single player mode.
     * @see #setSinglePlayer
     */
    public boolean getSinglePlayer() {
        return this.singlePlayer;
    }

    /**
     * Sets whether or not this game is a single player game.
     *
     * @param singlePlayer Whether or not this game is a single player game.
     */
    public void setSinglePlayer(boolean singlePlayer) {
        this.singlePlayer = singlePlayer;
    }

    /**
     * Is this client logged in to a server?
     *
     * @return True if this client is logged in to a server.
     */
    public synchronized boolean isLoggedIn() {
        return this.loggedIn;
    }

    /**
     * Get the server state, or at least our most recently cached value.
     *
     * @return A server state.
     */
    public ServerState getServerState() {
        return (this.freeColServer == null) ? this.cachedServerState
            : this.freeColServer.getServerState();
    }

    /**
     * Set the cached server state.
     *
     * @param state The new {@code ServerState}.
     */
    public void setServerState(ServerState state) {
        this.cachedServerState = state;
    }

    /**
     * Get the cached list of vacant player names.
     *
     * @return A list of available player names.
     */
    public List<String> getVacantPlayerNames() {
        return this.cachedVacantPlayerNames;
    }

    /**
     * Set the cached list of vacant player names.
     *
     * @param names The new vacant player names.
     */
    public void setVacantPlayerNames(List<String> names) {
        this.cachedVacantPlayerNames.clear();
        this.cachedVacantPlayerNames.addAll(names);
    }
    
    
    // Utilities

    /**
     * Set a message handler to handle messages from the server.
     * Used when switching from pre-game to in-game.
     *
     * @param messageHandler The new {@code MessageHandler}.
     */
    public void setMessageHandler(MessageHandler messageHandler) {
        askServer().setMessageHandler(messageHandler);
    }

    /**
     * Updates the game actions.  Generally useful when menu actions
     * should change due to the current game context.
     */
    public void updateActions() {
        actionManager.update();
    }

    /**
     * Sets the actions derived from the specification.
     *
     * @param specification The {@code Specification} to use.
     */
    public void addSpecificationActions(Specification specification) {
        SwingUtilities.invokeLater(() -> {
            actionManager.addSpecificationActions(specification);
            // XXX: The actions are loaded asynchronously without a callback.
            SwingUtilities.invokeLater(() -> {
                getGUI().resetMenuBar();
                getGUI().resetMapControls();
            });
        });
    }


    /**
     * Checks if this client is the game admin.
     *
     * @return True if the client is the game admin and the game has begun.
     */
    public boolean isAdmin() {
        return this.player != null && this.player.isAdmin();
    }

    /**
     * Verifies if this client can save the current game
     *
     * Clients that do not have the server running, or that have not
     * the required permissions cannot save and should have the menu
     * entry disabled
     *
     * @return True if this client can save the game in progress.
     */
    public boolean canSaveCurrentGame() {
        return freeColServer != null
            && (isAdmin() || freeColServer.getServerState() != ServerState.IN_GAME);
    }

    /**
     * Is the current player the client owner player?
     *
     * @return True if the current player is owned by this client.
     */
    public boolean currentPlayerIsMyPlayer() {
        return this.game != null && isInGame()
            && this.player != null
            && this.player.equals(this.game.getCurrentPlayer());
    }

    /**
     * Common utility routine to retrieve animation speed.
     *
     * @param player The {@code Player} to be animated.
     * @return The animation speed.
     */
    public int getAnimationSpeed(Player player) {
        String key = (getMyPlayer() == player)
            ? ClientOptions.MOVE_ANIMATION_SPEED
            : (getMyPlayer().isPotentialFriend(player)) // i.e. currently hostile
            ? ClientOptions.ENEMY_MOVE_ANIMATION_SPEED
            : ClientOptions.FRIENDLY_MOVE_ANIMATION_SPEED;
        return getClientOptions().getInteger(key);
    }

    /**
     * Set up the GUI after the game starts or a player reconnects.
     *
     * @param player The client {@code Player}.
     */
    public void restoreGUI(Player player) {
        Unit u = player.restoreActiveUnit();
        getGUI().reconnectGUI((u != null && player.owns(u)) ? u : null,
                              player.getFallbackTile());
    }


    // Server handling

    /**
     * Fail to start a server due to an exception.  Complain and fail back
     * to the main panel.
     *
     * @param ex The {@code Exception} that causes the trouble.
     * @param template A {@code StringTemplate} with the error message.
     * @return Null.
     */
    private FreeColServer failToMain(Exception ex, StringTemplate template) {
        if (FreeCol.getHeadless() // If this is a debug run, fail hard.
            || FreeColDebugger.getDebugRunTurns() >= 0) {
            final StringTemplate t = FreeCol.errorFromException(ex, template);
            final String msg = Messages.message(t);
            FreeCol.fatal(null, msg);
        } else {
            getGUI().showErrorPanel(ex, template);
        }
        return null;
    }

    /**
     * Fail to start a server due to an exception.  Complain and fail back
     * to the main panel.
     *
     * @param ex The {@code Exception} that causes the trouble.
     * @param key A message key with the error.
     * @return Null.
     */
    private FreeColServer failToMain(Exception ex, String key) {
        return failToMain(ex, StringTemplate.template(key));
    }
    
    /**
     * Shut down an existing server on a given port.
     *
     * @param port The port to unblock.
     * @return True if there should be no blocking server remaining.
     */
    public boolean unblockServer(int port) {
        final FreeColServer freeColServer = getFreeColServer();
        if (freeColServer != null ) {
            if (!getGUI().modalConfirmDialog("stopServer.text", "stopServer.yes",
                                  "stopServer.no", true)) return false;
            stopServer();
        }
        return true;
    }

    /**
     * Stop a server if present.
     *
     * Public for FreeColClient.quit and showMain.
     */
    public void stopServer() {
        final FreeColServer freeColServer = getFreeColServer();
        if (freeColServer != null) {
            freeColServer.getController().shutdown();
            setFreeColServer(null);
        }
    }

    /**
     * Start a server.
     *
     * @param publicServer If true, add to the meta-server.
     * @param singlePlayer True if this is a single player game.
     * @param spec The {@code Specification} to use in this game.
     * @param address The address to use for the public socket.
     * @param port The TCP port to use for the public socket. If null, try
     *      ports until 
     * @return A new {@code FreeColServer} or null on error.
     */
    public FreeColServer startServer(boolean publicServer,
                                     boolean singlePlayer, Specification spec,
                                     InetAddress address,
                                     int port) {
        final FreeColServer fcs;
        try {
            fcs = new FreeColServer(publicServer, singlePlayer, spec, address, port, null);
            if (!fcs.registerWithMetaServer()) {
                fcs.shutdown();
                return failToMain(null, "server.noRouteToServer");
            }
        } catch (IOException ioe) {
            return failToMain(ioe, "server.initialize");
        }

        setFreeColServer(fcs);
        setSinglePlayer(singlePlayer);
        return fcs;
    }

    /**
     * Start a server with a saved game.
     *
     * @param publicServer If true, add to the meta-server.
     * @param singlePlayer True if this is a single player game.
     * @param saveFile The saved game {@code File}.
     * @param address The TCP address to use for the public socket.
     * @param port The TCP port to use for the public socket.
     * @param name An optional name for the server.
     * @return A new {@code FreeColServer}, or null on error.
     */
    public FreeColServer startServer(boolean publicServer,
                                     boolean singlePlayer,
                                     File saveFile, InetAddress address,
                                     int port, String name) {
        final FreeColSavegameFile fsg;
        try {
            fsg = new FreeColSavegameFile(saveFile);
        } catch (FileNotFoundException fnfe) {
            return failToMain(fnfe, FreeCol.badFile("error.couldNotFind", saveFile));
        } catch (IOException ioe) {
            return failToMain(ioe, "server.initialize");
        }
        final FreeColServer fcs;
        try {
            fcs = new FreeColServer(fsg, (Specification)null, address, port, name);
            fcs.setPublicServer(publicServer);
            if (!fcs.registerWithMetaServer()) {
                return failToMain(null, "server.noRouteToServer");
            }
        } catch (XMLStreamException xse) {
            return failToMain(xse, FreeCol.badFile("error.couldNotLoad", saveFile));
        } catch (Exception ex) {
            return failToMain(ex, "server.initialize");
        }

        setFreeColServer(fcs);
        setSinglePlayer(singlePlayer);
        this.inGameController.setGameConnected();
        ResourceManager.setSavegameFile(fsg);
        return fcs;
    }
    

    // Fundamental game start/stop/continue actions

    /**
     * Log in to a new game.
     *
     * Called when the ConnectController processes a login.
     *
     * @param inGame Whether the server is in-game.
     * @param game The new {@code Game}.
     * @param player The new client {@code Player}.
     * @param single True if this is a single player game.
     */
    public synchronized void login(boolean inGame, Game game, Player player,
                                   boolean single) {
        this.loggedIn = true;
        changeClientState(inGame);
        setGame(game);
        setMyPlayer(player);
        setSinglePlayer(single);
        addSpecificationActions(game.getSpecification());
        if (player != null) {
            final ClientOptions co = getClientOptions();
            player.setColonyComparator(co.getColonyComparator());
        }        
    }

    /**
     * Log this client out.
     *
     * Called when the ConnectController processes a logout.
     *
     * @param inGame Whether the server is in-game.
     */
    public synchronized void logout(@SuppressWarnings("unused") boolean inGame) {
        this.loggedIn = false;
        changeClientState(inGame);
        setGame(null);
        setMyPlayer(null);
        // Ignore single-player state
        // FIXME: should remove specification actions
    }

    /**
     * Continue playing after winning the game.
     */
    public void continuePlaying() {
        askServer().continuePlaying();
    }

    /**
     * Start the game skipping turns.
     *
     * @param turns The number of turns to skip.
     */
    public void skipTurns(int turns) {
        if (freeColServer == null) return;
        if (turns <= 0) {
            freeColServer.getInGameController().setSkippedTurns(0);
            return;
        }
        gui.closeMenus();
        freeColServer.getInGameController().setSkippedTurns(turns);
        askServer().startSkipping();
    }

    /**
     * Quits the application.
     */
    public void askToQuit() {
        if (gui.modalConfirmDialog("quitDialog.areYouSure.text", "ok", "cancel", false)) {
            Player player = getMyPlayer();
            if (player == null) { // If no player, must be already logged out
                quit();
            } else {
                getConnectController().requestLogout(LogoutReason.QUIT);
                quit();
            }
        }
    }

    /**
     * Retire from the game.
     */
    public void retire() {
        if (gui.modalConfirmDialog("retireDialog.areYouSure.text", "ok", "cancel", false)) {
            final Player player = getMyPlayer();
            player.changePlayerType(Player.PlayerType.RETIRED);
            final boolean highScore = askServer().retire();
            getInGameController().highScore(highScore);
        }
    }

    /**
     * Quits the application without any questions.
     */
    public void quit() {
        stopServer();

        final ClientOptions co = getClientOptions();
        List<String> excludeSuffixes = new ArrayList<>(2);
        excludeSuffixes.add(co.getText(ClientOptions.LAST_TURN_NAME));
        excludeSuffixes.add(co.getText(ClientOptions.BEFORE_LAST_TURN_NAME));
        String logMe = FreeColDirectories
            .removeOutdatedAutosaves(co.getText(ClientOptions.AUTO_SAVE_PREFIX),
                                     excludeSuffixes,
                                     co.getInteger(ClientOptions.AUTOSAVE_VALIDITY));
        if (logMe != null) logger.info(logMe);

        // Exit
        try {
            gui.quitGUI();
        } catch (Exception e) {
            FreeCol.fatal(logger, "Failed to shutdown gui: " + e);
        }
        FreeCol.quit(0);
    }
    
    private void overrideDefaultUncaughtExceptionHandler() {
        // This overrides the handler in FreeCol:
        Thread.setDefaultUncaughtExceptionHandler((Thread thread, Throwable e) -> {
            // Free enough space to ensure we can perform the next operations.
            if (e instanceof Error) {
                memoryToBeFreedOnOutOfMemory = null;
                gui.emergencyPurge();
            }
            
            final boolean seriousError = (e instanceof Error);
            try {
                logger.log(Level.WARNING, "Uncaught exception from thread: " + thread, e);
                
                if (seriousError) {
                    gui.showErrorPanel(Messages.message("error.seriousError"), () -> {
                        System.exit(1);
                    });
                }
            } catch (Throwable t) {
                if (seriousError) {
                    t.printStackTrace();
                    System.exit(1);
                }
            }
        });
    }
}
