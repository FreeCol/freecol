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

package net.sf.freecol.client;

import java.awt.Dimension;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


import javax.swing.SwingUtilities;

import net.sf.freecol.client.control.ConnectController;
import net.sf.freecol.client.control.InGameController;
import net.sf.freecol.client.control.InGameInputHandler;
import net.sf.freecol.client.control.MapEditorController;
import net.sf.freecol.client.control.PreGameController;
import net.sf.freecol.client.control.PreGameInputHandler;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.action.ActionManager;
import net.sf.freecol.client.gui.plaf.FreeColLookAndFeel;
import net.sf.freecol.client.networking.UserServerAPI;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.FreeColSeed;
import net.sf.freecol.common.io.FreeColDataFile;
import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.io.FreeColModFile;
import net.sf.freecol.common.io.FreeColSavegameFile;
import net.sf.freecol.common.io.FreeColTcFile;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.networking.Client;
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

    // Control:
    private ConnectController connectController;

    private PreGameController preGameController;

    private PreGameInputHandler preGameInputHandler;

    private InGameController inGameController;

    private InGameInputHandler inGameInputHandler;

    private MapEditorController mapEditorController;

    private ServerAPI serverAPI;



    // GUI - this encapsulates the whole gui stuff
    private GUI gui;

    // Networking:
    /**
     * The network <code>Client</code> that can be used to send messages to
     * the server.
     */

    // Model:
    private Game game;

    /** The player "owning" this client. */
    private Player player;


    /**
     * Indicates if the game has started, has nothing to do with
     * whether or not the client is logged in.
     */
    private boolean inGame = false;


    /** The server that has been started from the client-GUI. */
    private FreeColServer freeColServer = null;

    private boolean mapEditor;

    private boolean singlePlayer;

    private final ActionManager actionManager;

    private ClientOptions clientOptions;

    public final Worker worker;

    /**
     * Indicated whether or not there is an open connection to the
     * server. This is not an indication of the existence of a
     * Connection Object, but instead it is an indication of an
     * approved login to a server.
     */
    private boolean loggedIn = false;

    /**
     * Run in headless mode.
     */
    private boolean headless;


    /**
     * Creates a new <code>FreeColClient</code>. Creates the control objects
     * and starts the GUI.
     *
     * @param savedGame An optional saved game.
     * @param size An optional window size.
     * @param sound True if sounds should be played
     * @param splashFilename The name of the splash image.
     * @param showOpeningVideo Display the opening video.
     * @param fontName An optional override of the main font.
     */
    public FreeColClient(final File savedGame,
                         final Dimension size,
                         final boolean sound,
                         final String splashFilename,
                         final boolean showOpeningVideo,
                         final String fontName) {
        gui = new GUI(this);
        serverAPI = new UserServerAPI(gui);

        // Look for base data directory.  Failure is fatal.
        File baseDirectory = FreeColDirectories.getBaseDirectory();
        if (!baseDirectory.exists() || !baseDirectory.isDirectory()) {
            System.err.println("Could not find base data directory: "
                               + baseDirectory.getName());
            System.err.println("  The data files could not be found by FreeCol. Please make sure");
            System.err.println("  they are present. If FreeCol is looking in the wrong directory");
            System.err.println("  then run the game with a command-line parameter:\n");
            System.err.println("    --freecol-data <data-directory>\n");
            System.exit(1);
        }

        // headless mode is enable for the test suite, where it now
        // works again.
        // TODO: It would be nice to have it useful for running full
        // automated debug games without GUI, but that is untested and
        // probably still borked.  Fix.
        headless = "true".equals(System.getProperty("java.awt.headless",
                "false"));

        mapEditor = false;

        gui.displaySpashScreen(splashFilename);

        // Determine the window size.
        gui.setWindowed(size != null);
        final Dimension windowSize = (size == null) ? null
            : (size.width <= 0 || size.height <= 0) ? gui.determineWindowSize()
            : size;
        logger.info("Window size is " + windowSize);

        // Control
        connectController = new ConnectController(this, gui);
        preGameController = new PreGameController(this, gui);
        preGameInputHandler = new PreGameInputHandler(this, gui);
        inGameController = new InGameController(this, gui);
        inGameInputHandler = new InGameInputHandler(this, gui);
        mapEditorController = new MapEditorController(this, gui);
        actionManager = new ActionManager(this, gui);
        worker = new Worker();
        worker.start();

        // Load resources.
        //   - base resources
        //   - resources in the default "classic" ruleset,
        //   - resources in the default actions
        // TODO: probably should not need to load "classic", but there
        // are a bunch of things in there (e.g. orderButton) that first
        // need to move to base because the action manager requires them.
        FreeColDataFile baseData = new FreeColDataFile(baseDirectory);
        ResourceManager.setBaseMapping(baseData.getResourceMapping());
        try {
            FreeColTcFile tcData = new FreeColTcFile("classic");
            ResourceManager.setTcMapping(tcData.getResourceMapping());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to load resource mapping from rule set 'classic'.", e);
            System.err.println("Failed to load resource mapping from rule set 'classic'.");
            System.exit(1);
        }
        actionManager.initializeActions();

        // Load the client options, which handle reloading the
        // resources specified in the active mods.
        loadClientOptions(savedGame);

        // Work out the main font now that resources are loaded.
        Font font = null;
        if (fontName != null) {
            font = Font.decode(fontName);
            if (font == null) {
                System.err.println("Font not found: " + fontName);
            }
        }
        if (font == null) font = ResourceManager.getFont("NormalFont");

        // Swing system and look-and-feel initialization.
        try {
            FreeColLookAndFeel fclaf
                = new FreeColLookAndFeel(FreeColDirectories.getDataDirectory());
            FreeColLookAndFeel.install(fclaf, font);
        } catch (FreeColException e) {
            logger.log(Level.SEVERE, "Unable to install FreeCol look-and-feel.",
                       e);
            System.err.println("Unable to install FreeCol look-and-feel.");
            System.exit(1);
        }

        // Once resources are in place, get preloading started.
        if (!headless) 
            ResourceManager.preload(windowSize);

        // Start the GUI.
        gui.hideSplashScreen();
        SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    gui.startGUI(windowSize, sound, showOpeningVideo,
                                 savedGame != null);
                }
            });

        // Load the optional saved game.
        if (savedGame != null) {
            SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        connectController.loadGame(savedGame);
                    }
                });
        }
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
     * Quits the application.
     * This method uses
     * {@link net.sf.freecol.client.gui.GUI#showConfirmDialog} in
     * order to get a "Are you sure"-confirmation from the user.
     */
    public void askToQuit() {
        if (gui.showConfirmDialog("quitDialog.areYouSure.text",
                                  "ok", "cancel")) {
            quit();
        }
    }

    /**
     *  Verifies if this client can save the current game
     *  Clients that do not have the server running, or that have not the required permissions
     *cannot save and should have the menu entry disabled
     * @return true if this client can save the game in progress, false otherwise
     */
    public boolean canSaveCurrentGame(){
        if (getFreeColServer() == null) {
            return false;
        } else if (getMyPlayer() == null) {
            return false;
        } else if (getFreeColServer().getGameState() == GameState.IN_GAME
                   && !getMyPlayer().isAdmin()) {
            return false;
        }
        return true;
    }


    /**
     * Continue playing after winning the game.
     */
    public void continuePlaying() {
        askServer().continuePlaying();
    }

    public boolean currentPlayerIsMyPlayer() {
        return inGame && game.getCurrentPlayer().equals(player);
    }


    public void updateActions() {
        actionManager.update();
    }
    

    /**
     * Gets the <code>Client</code> that can be used to send messages to the
     * server.
     *
     * @return the <code>Client</code>
     * @see #setClient
     */
    public Client getClient() {
        return serverAPI.getClient();
    }

    /**
     * Returns the object keeping the current client options.
     *
     * @return The <code>ClientOptions</code>.
     */
    public ClientOptions getClientOptions() {
        return clientOptions;
    }

    /**
     * Gets the controller responsible for starting a server and connecting to
     * it.
     *
     * @return The <code>ConnectController</code>.
     */
    public ConnectController getConnectController() {
        return connectController;
    }

    /**
     * Gets the <code>FreeColServer</code> started by the client.
     *
     * @return The <code>FreeColServer</code> or <code>null</code> if no
     *         server has been started.
     */
    public FreeColServer getFreeColServer() {
        return freeColServer;
    }

    /**
     * Gets the <code>Game</code> that we are currently playing.
     *
     * @return The <code>Game</code>.
     * @see #setGame
     */
    public Game getGame() {
        return game;
    }

    public GUI getGUI() {
        return gui;
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

    public MapEditorController getMapEditorController() {
        return mapEditorController;
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
     * Checks if this client is the game admin.
     *
     * @return <i>true</i> if the client is the game admin and <i>false</i>
     *         otherwise. <i>false</i> is also returned if a game have not yet
     *         been started.
     */
    public boolean isAdmin() {
        if (getMyPlayer() == null) {
            return false;
        }
        return getMyPlayer().isAdmin();
    }

    /**
     * Get the <code>Headless</code> value.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isHeadless() {
        return headless;
    }

    /**
     * Checks if the game has started.
     * @return <i>true</i> if the game has started.
     * @see #setInGame
     */
    public boolean isInGame() {
        return inGame;
    }

    /**
     * Returns <i>true</i> if this client is logged in to a server or
     * <i>false</i> otherwise.
     *
     * @return <i>true</i> if this client is logged in to a server or
     *         <i>false</i> otherwise.
     */
    public boolean isLoggedIn() {
        return loggedIn;
    }

    public boolean isMapEditor() {
        return mapEditor;
    }


    /**
     * Is the user playing in single player mode.
     *
     * @return <i>true</i> if the user is playing in single player mode and
     *         <i>false</i> otherwise.
     * @see #setSinglePlayer
     */
    public boolean isSinglePlayer() {
        return singlePlayer;
    }

    /**
     * Displays a quit dialog and, if desired, logs out of the current game and
     * shows the new game panel.
     */
    public void newGame() {
        Specification specification = null;
        if (getGame() != null) {
            if (isMapEditor()) {
                specification = getGame().getSpecification();
            } else if (gui.showConfirmDialog("stopCurrentGame.text",
                                         "stopCurrentGame.yes",
                                         "stopCurrentGame.no")) {
                getConnectController().quitGame(true);
                FreeColSeed.incrementFreeColSeed();
            } else {
                return;
            }
            gui.removeInGameComponents();
        }

        gui.showNewPanel(specification);
    }

    /**
     * Quits the application without any questions.
     */
    public void quit() {
        getConnectController().quitGame(isSinglePlayer());
        exitActions();
        try {
            gui.quit();
        } catch (Exception e) {
            System.exit(1);
        }
        System.exit(0);
    }

    /**
     * Quits the application.
     * This method uses {@link net.sf.freecol.client.gui.GUI#showConfirmDialog}
     * in order to get a "Are you sure"-confirmation from the user.
     */
    public void retire() {
        if (gui.showConfirmDialog("retireDialog.areYouSure.text",
                                  "ok", "cancel")) {
            if (askServer().retire()) {
                // Panel exit calls quit.
                gui.showHighScoresPanel(null);
            }
            quit();
        }
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
     * Sets the <code>Game</code> that we are currently playing.
     *
     * @param game The <code>Game</code>.
     * @see #getGame
     */
    public void setGame(Game game) {
        this.game = game;
    }

    /**
     * Set the <code>Headless</code> value.
     *
     * @param newHeadless The new Headless value.
     */
    public void setHeadless(final boolean newHeadless) {
        this.headless = newHeadless;
    }

    /**
     * Notifies this GUI that the game has started or ended.
     * @param inGame Indicates whether or not the game has started.
     */
    public void setInGame(boolean inGame) {
        this.inGame = inGame;
    }



    /**
     * Sets whether or not this client is logged in to a server.
     *
     * @param loggedIn An indication of whether or not this client is logged in
     *            to a server.
     */
    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }


    public void setMapEditor(boolean mapEditor) {
        this.mapEditor = mapEditor;
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
     * Sets whether or not this game is a single player game.
     *
     * @param singlePlayer Indicates whether or not this game is a
     *     single player game.
     * @see #isSinglePlayer
     */
    public void setSinglePlayer(boolean singlePlayer) {
        this.singlePlayer = singlePlayer;
    }
    
    /**
     * Start the game skipping turns.
     *
     * @param turns The number of turns to skip.
     */
    public void skipTurns(int turns) {
        if (freeColServer == null) 
            return;
        freeColServer.getInGameController().setSkippedTurns(turns);
        gui.closeMenus();
        askServer().startSkipping();
    }
    

    private void exitActions () {
       try {
          // action: delete outdated autosave files
          int validDays = getClientOptions().getInteger(ClientOptions.AUTOSAVE_VALIDITY);
          long validPeriod = (long)validDays * 86400 * 1000;  // millisecond equivalent of valid days
          long timeNow = System.currentTimeMillis();
          File autosaveDir = FreeColDirectories.getAutosaveDirectory();

          if (validPeriod != 0) {
             // analyse all files in autosave directory
             String[] flist = autosaveDir.list();
             for ( int i = 0; flist != null && i < flist.length; i++ ) {
                String filename = flist[i];
                // delete files which are older than valid period set by user option
                if (filename.endsWith(".fsg")) {
                   File saveGameFile = new File(autosaveDir, filename);
                   if (saveGameFile.lastModified() + validPeriod < timeNow) {
                       saveGameFile.delete();
                   }
                }
             }
          }
       } catch (Exception e) {
          e.printStackTrace();
       }
    }
    

    /**
     * Loads the client options.
     * There are several sources:
     *   1) Base options (set in the ClientOptions constructor with
     *        ClientOptions.addDefaultOptions())
     *   2) Standard action manager actions
     *   3) Saved game
     *   4) User options
     *
     * @param savedGame An optional <code>File</code> to load options from.
     */
    private void loadClientOptions(File savedGame) {
        clientOptions = new ClientOptions();
        logger.info("Loaded default client options.");

        if (actionManager != null) {
            clientOptions.add(actionManager);
            logger.info("Loaded client options from the action manager.");
        }

        if (savedGame != null) {
            try {
                FreeColSavegameFile save = new FreeColSavegameFile(savedGame);
                String fileName = FreeColSavegameFile.CLIENT_OPTIONS;
                clientOptions.loadOptions(save.getInputStream(fileName));
                logger.info("Loaded client options from saved game:"
                    + savedGame.getPath() + "(" + fileName + ")");
            } catch (Exception e) {
                logger.warning("Unable to read client options from: "
                    + savedGame.getPath());
            }
        }

        File userOptions = FreeColDirectories.getClientOptionsFile();
        if (userOptions != null && userOptions.exists()) {
            clientOptions.updateOptions(userOptions);
            logger.info("Updated client options from user options file: "
                + userOptions.getPath());
        } else {
            logger.warning("User options file not present.");
        }

        // Reset the mod resources as a result of the client option update.
        List<ResourceMapping> modMappings = new ArrayList<ResourceMapping>();
        for (FreeColModFile f : clientOptions.getActiveMods()) {
            modMappings.add(f.getResourceMapping());
        }
        ResourceManager.setModMappings(modMappings);

        // Update the actions, resources may have changed.
        if (actionManager != null)
            actionManager.update();
    }



    public ActionManager getActionManager() {
        return actionManager;
    }
    
    public void addSpecificationActions(Specification specification) {
        actionManager.addSpecificationActions(specification);
    }


    
    
    
}
