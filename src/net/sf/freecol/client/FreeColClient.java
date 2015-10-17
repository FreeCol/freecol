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

package net.sf.freecol.client;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.control.ConnectController;
import net.sf.freecol.client.control.InGameController;
import net.sf.freecol.client.control.InGameInputHandler;
import net.sf.freecol.client.control.MapEditorController;
import net.sf.freecol.client.control.PreGameController;
import net.sf.freecol.client.control.PreGameInputHandler;
import net.sf.freecol.client.control.SoundController;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.SwingGUI;
import net.sf.freecol.client.gui.action.ActionManager;
import net.sf.freecol.client.networking.UserServerAPI;
import net.sf.freecol.common.FreeColSeed;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColDataFile;
import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.io.FreeColModFile;
import net.sf.freecol.common.io.FreeColSavegameFile;
import net.sf.freecol.common.io.FreeColTcFile;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.networking.ServerAPI;
import net.sf.freecol.common.resources.ResourceManager;
import net.sf.freecol.common.resources.ResourceMapping;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.FreeColServer.GameState;


/**
 * The main control class for the FreeCol client.  This class both
 * starts and keeps references to the GUI and the control objects.
 */
public final class FreeColClient {

    private static final Logger logger = Logger.getLogger(FreeColClient.class.getName());

    private final ConnectController connectController;

    private final PreGameController preGameController;

    private final PreGameInputHandler preGameInputHandler;

    private final InGameController inGameController;

    private final InGameInputHandler inGameInputHandler;

    private final MapEditorController mapEditorController;

    private SoundController soundController;

    /** The server that has been started from the client-GUI. */
    private FreeColServer freeColServer = null;

    /** Encapsulation of the server API. */
    private final ServerAPI serverAPI;


    /** The GUI encapsulation. */
    private final GUI gui;

    /** The encapsulation of the actions. */
    private final ActionManager actionManager;

    /** The game itself. */
    private Game game;

    /** The player that `owns' this client. */
    private Player player;

    /** The client options specific to this player. */
    private ClientOptions clientOptions;

    /** A worker to perform game loading. */
    private final Worker worker;

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

    /** Run in headless mode. */
    private final boolean headless;


    public FreeColClient(final InputStream splashStream,
                         final String fontName) {
        this(splashStream, fontName, FreeCol.GUI_SCALE_DEFAULT, true);
    }

    /**
     * Creates a new <code>FreeColClient</code>.  Creates the control
     * objects.
     *
     * @param splashStream A stream to read the splash image from.
     * @param fontName An optional override of the main font.
     * @param scale The scale factor for gui elements.
     * @param headless Run in headless mode.
     */
    public FreeColClient(final InputStream splashStream, final String fontName,
                         final float scale, boolean headless) {
        mapEditor = false;
        this.headless = headless
            || System.getProperty("java.awt.headless", "false").equals("true");
        if (this.headless) {
            if (!FreeColDebugger.isInDebugMode()
                || FreeColDebugger.getDebugRunTurns() <= 0) {
                fatal(Messages.message("client.headlessDebug"));
            }
        }

        // Get the splash screen up early on to show activity.
        gui = (this.headless) ? new GUI(this, scale)
                              : new SwingGUI(this, scale);
        gui.displaySplashScreen(splashStream);

        // Look for base data directory.  Failure is fatal.
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
            fatal(Messages.message(StringTemplate.template("client.baseData")
                          .addName("%dir%", baseDirectory.getName()))
                + ((ioeMessage == null) ? "" : "\n" + ioeMessage));
        }
        ResourceManager.setBaseMapping(baseData.getResourceMapping());

        // Once the basic resources are in place construct other things.

        serverAPI = new UserServerAPI(gui);

        // Control.  Controllers expect GUI to be available.
        connectController = new ConnectController(this);
        preGameController = new PreGameController(this);
        preGameInputHandler = new PreGameInputHandler(this);
        inGameController = new InGameController(this);
        inGameInputHandler = new InGameInputHandler(this);
        mapEditorController = new MapEditorController(this);

        worker = new Worker();
        worker.start();

        // Load resources.
        //   - base resources
        //   - resources in the default "classic" ruleset,
        //   - resources in the default actions
        //
        // FIXME: probably should not need to load "classic", but there
        // are a bunch of things in there (e.g. order buttons) that first
        // need to move to base because the action manager requires them.
        //
        // Not so easy, since the ActionManager also creates tile
        // improvement actions, which depend on the
        // specification. However, this step could probably be
        // delayed.
        try {
            FreeColTcFile tcData = new FreeColTcFile("classic");
            ResourceManager.setTcMapping(tcData.getResourceMapping());
        } catch (IOException e) {
            fatal(Messages.message("client.classic") + "\n" + e.getMessage());
        }

        if (!this.headless) {
            // Swing system and look-and-feel initialization.
            try {
                gui.installLookAndFeel(fontName);
            } catch (Exception e) {
                fatal(Messages.message("client.laf") + "\n" + e.getMessage());
            }
        }
        actionManager = new ActionManager(this);
        actionManager.initializeActions(inGameController, connectController);
    }

    /**
     * Starts the new <code>FreeColClient</code>, including the GUI.
     *
     * @param size An optional window size.
     * @param userMsg An optional message key to be displayed early.
     * @param sound True if sounds should be played
     * @param showOpeningVideo Display the opening video.
     * @param savedGame An optional saved game.
     * @param spec If non-null, a <code>Specification</code> to use to start
     *     a new game immediately.
     */
    public void startClient(final Dimension size,
                            final String userMsg,
                            final boolean sound,
                            final boolean showOpeningVideo,
                            final File savedGame,
                            final Specification spec) {
        if (headless && savedGame == null && spec == null) {
            fatal(Messages.message("client.headlessRequires"));
        }

        // Load the client options, which handle reloading the
        // resources specified in the active mods.
        this.clientOptions = loadClientOptions(savedGame);
        this.clientOptions.fixClientOptions();

        // Reset the mod resources as a result of the client option update.
        ResourceMapping modMappings = new ResourceMapping();
        for (FreeColModFile f : this.clientOptions.getActiveMods()) {
            modMappings.addAll(f.getResourceMapping());
        }
        ResourceManager.setModMapping(modMappings);
        // Update the actions, resources may have changed.
        if (this.actionManager != null) updateActions();

        // Initialize Sound (depends on client options)
        this.soundController = new SoundController(this, sound);

        // Start the GUI (headless-safe)
        gui.hideSplashScreen();
        gui.startGUI(size);

        // Now the GUI is going, either:
        //   - load the saved game if one was supplied
        //   - use the debug shortcut to immediately start a new game with
        //     supplied specification
        //   - display the opening video (which goes on to display the
        //     main panel when it completes)
        //   - display the main panel and let the user choose what to
        //     do (which will often be to progress through the
        //     NewPanel to a call to the connect controller to start a game)
        if (savedGame != null) {
            soundController.playSound("sound.intro.general");
            SwingUtilities.invokeLater(() -> {
                    if (!connectController.startSavedGame(savedGame, userMsg)) {
                        gui.showMainPanel(userMsg);
                    }
                });
        } else if (spec != null) { // Debug or fast start
            soundController.playSound("sound.intro.general");
            SwingUtilities.invokeLater(() -> {
                    if (!connectController.startSinglePlayerGame(spec, true)) {
                        gui.showMainPanel(userMsg);
                    }
                });
        } else if (showOpeningVideo) {
            SwingUtilities.invokeLater(() -> {
                    gui.showOpeningVideo(userMsg);
                });
        } else {
            soundController.playSound("sound.intro.general");
            SwingUtilities.invokeLater(() -> {
                    gui.showMainPanel(userMsg);
                });
        }

        String quit = FreeCol.CLIENT_THREAD + "Quit Game";
        Runtime.getRuntime().addShutdownHook(new Thread(quit) {
                @Override
                public void run() {
                    getConnectController().quitGame(true);
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
     * @param savedGameFile An optional saved game <code>File</code>
     *     to load options from.
     * @return The loaded <code>ClientOptions</code>.
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
            clop.merge(userOptions);
        }

        //logger.info("Final client options: " + clop.toString());
        return clop;
    }

    /**
     * Quit and exit with an error.
     *
     * @param err The error message.
     */
    public static void fatal(String err) {
        logger.log(Level.SEVERE, err);
        FreeCol.fatal(err);
    }


    // Accessors

    /**
     * Gets the controller responsible for starting a server and
     * connecting to it.
     *
     * @return The <code>ConnectController</code>.
     */
    public ConnectController getConnectController() {
        return connectController;
    }

    /**
     * Gets the controller that will be used before the game has been started.
     *
     * @return The <code>PreGameController</code>.
     */
    public PreGameController getPreGameController() {
        return preGameController;
    }

    /**
     * Gets the input handler that will be used before the game has been
     * started.
     *
     * @return The <code>PreGameInputHandler</code>.
     */
    public PreGameInputHandler getPreGameInputHandler() {
        return preGameInputHandler;
    }

    /**
     * Gets the controller that will be used when the game has been started.
     *
     * @return The <code>InGameController</code>.
     */
    public InGameController getInGameController() {
        return inGameController;
    }

    /**
     * Gets the input handler that will be used when the game has been started.
     *
     * @return The <code>InGameInputHandler</code>.
     */
    public InGameInputHandler getInGameInputHandler() {
        return inGameInputHandler;
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
     * Gets the <code>FreeColServer</code> started by the client.
     *
     * @return The <code>FreeColServer</code> or <code>null</code> if no
     *     server has been started.
     */
    public FreeColServer getFreeColServer() {
        return freeColServer;
    }

    /**
     * Sets the <code>FreeColServer</code> which has been started by the
     * client gui.
     *
     * @param freeColServer The <code>FreeColServer</code>.
     * @see #getFreeColServer()
     */
    public void setFreeColServer(FreeColServer freeColServer) {
        this.freeColServer = freeColServer;
    }

    /**
     * Meaningfully named access to the ServerAPI.
     *
     * @return A ServerAPI.
     */
    public ServerAPI askServer() {
        return serverAPI;
    }


    /**
     * Gets the GUI attached to this client.
     *
     * @return The current <code>GUI</code>.
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
     * Gets the <code>Game</code> that we are currently playing.
     *
     * @return The current <code>Game</code>.
     * @see #setGame
     */
    public Game getGame() {
        return game;
    }

    /**
     * Sets the <code>Game</code> that we are currently playing.
     *
     * @param game The new <code>Game</code>.
     * @see #getGame
     */
    public void setGame(Game game) {
        this.game = game;
    }

    /**
     * Gets the <code>Player</code> that uses this client.
     *
     * @return The <code>Player</code> made to represent this clients user.
     * @see #setMyPlayer(Player)
     */
    public Player getMyPlayer() {
        return player;
    }

    /**
     * Sets the <code>Player</code> that uses this client.
     *
     * @param player The <code>Player</code> made to represent this clients
     *            user.
     * @see #getMyPlayer()
     */
    public void setMyPlayer(Player player) {
        this.player = player;
    }

    /**
     * Gets the object keeping the current client options.
     *
     * @return The <code>ClientOptions</code> attached to this
     *     <code>FreeColClient</code>.
     */
    public ClientOptions getClientOptions() {
        return clientOptions;
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
     * @return <i>true</i> if the game has started.
     * @see #setInGame
     */
    public boolean isInGame() {
        return inGame;
    }

    /**
     * Set the game start state.
     *
     * @param inGame Whether or not the game has started.
     */
    public void setInGame(boolean inGame) {
        this.inGame = inGame;
    }

    /**
     * Are we using the map editor?
     *
     * @return True if the map editor is enabled.
     */
    public boolean isMapEditor() {
        return mapEditor;
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
    public boolean isSinglePlayer() {
        return singlePlayer;
    }

    /**
     * Sets whether or not this game is a single player game.
     *
     * @param singlePlayer Whether or not this game is a single player game.
     * @see #isSinglePlayer
     */
    public void setSinglePlayer(boolean singlePlayer) {
        this.singlePlayer = singlePlayer;
    }

    /**
     * Is this client logged in to a server?
     *
     * @return True if this client is logged in to a server.
     */
    public boolean isLoggedIn() {
        return loggedIn;
    }

    /**
     * Sets whether or not this client is logged in to a server.
     *
     * @param loggedIn Whether or not this client is logged in to a server.
     */
    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }

    /**
     * Is the game in headless mode?
     *
     * @return a <code>boolean</code> value
     */
    public boolean isHeadless() {
        return headless;
    }


    // Utilities

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
     * @param specification The <code>Specification</code> to use.
     */
    public void addSpecificationActions(Specification specification) {
        actionManager.addSpecificationActions(specification);
    }


    /**
     * Checks if this client is the game admin.
     *
     * @return True if the client is the game admin and the game has begun.
     */
    public boolean isAdmin() {
        return player != null && player.isAdmin();
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
            && (isAdmin() || freeColServer.getGameState() != GameState.IN_GAME);
    }

    /**
     * Is the current player the client owner player?
     *
     * @return True if the current player is owned by this client.
     */
    public boolean currentPlayerIsMyPlayer() {
        return inGame
            && game != null
            && player != null
            && player.equals(game.getCurrentPlayer());
    }

    /**
     * Common utility routine to retrieve animation speed.
     *
     * @param player The <code>Player</code> to be animated.
     * @return The animation speed.
     */
    public int getAnimationSpeed(Player player) {
        String key = (getMyPlayer() == player)
            ? ClientOptions.MOVE_ANIMATION_SPEED
            : ClientOptions.ENEMY_MOVE_ANIMATION_SPEED;
        return getClientOptions().getInteger(key);
    }

    /**
     * Get a list of the player colonies.
     *
     * @return The players colonies sorted according to the chosen comparator.
     */
    public List<Colony> getMySortedColonies() {
        return (clientOptions == null || player == null)
            ? Collections.<Colony>emptyList()
            : clientOptions.getSortedColonies(player);
    }

    /**
     * Give the worker some work.
     *
     * @param runnable The <code>Runnable</code> to do.
     */
    public void setWork(Runnable runnable) {
        worker.schedule(runnable);
    }

    // Fundamental game start/stop/continue actions

    /**
     * If currently in a game, displays a quit dialog and if desired,
     * logs out of the current game.
     *
     * When the game is clear, show the new game panel.
     *
     * Called from the New action, often from the button on the MainPanel,
     * and IGC.victory()
     *
     * @param prompt If true, prompt to confirm stopping the game.
     */
    public void newGame(boolean prompt) {
        Specification specification = null;
        if (getGame() != null) {
            if (isMapEditor()) {
                specification = getGame().getSpecification();
            } else if (!prompt || gui.confirmStopGame()) {
                getConnectController().quitGame(true);
                FreeColSeed.incrementFreeColSeed();
            } else {
                return;
            }
        }
        gui.removeInGameComponents();
        gui.showNewPanel(specification);
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
        if (gui.confirm("quitDialog.areYouSure.text", "ok", "cancel")) quit();
    }

    /**
     * Retire from the game.
     */
    public void retire() {
        if (gui.confirm("retireDialog.areYouSure.text", "ok", "cancel")) {
            Player player = getMyPlayer();
            player.changePlayerType(Player.PlayerType.RETIRED);
            askServer().retire();
            gui.showHighScoresPanel(null, askServer().getHighScores());
        }
    }

    /**
     * Quits the application without any questions.
     */
    public void quit() {
        getConnectController().quitGame(isSinglePlayer());
        try { // delete outdated autosave files
            long validPeriod = 1000L * 24L * 60L * 60L // days to ms
                * clientOptions.getInteger(ClientOptions.AUTOSAVE_VALIDITY);
            long timeNow = System.currentTimeMillis();
            File autoSave = FreeColDirectories.getAutosaveDirectory();
            String[] flist;
            if (validPeriod != 0L && autoSave != null
                && (flist = autoSave.list()) != null) {
                for (String f : flist) {
                    if (!f.endsWith("." + FreeCol.FREECOL_SAVE_EXTENSION)) continue;
                    // delete files which are older than user option allows
                    File saveGameFile = new File(autoSave, f);
                    if (saveGameFile.lastModified() + validPeriod < timeNow) {
                        saveGameFile.delete();
                    }
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to delete autosave", e);
        }
        try {
            gui.quit();
        } catch (Exception e) {
            System.exit(1);
        }
        System.exit(0);
    }
}
