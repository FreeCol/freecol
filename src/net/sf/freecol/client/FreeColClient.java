/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.control.ConnectController;
import net.sf.freecol.client.control.InGameController;
import net.sf.freecol.client.control.InGameInputHandler;
import net.sf.freecol.client.control.MapEditorController;
import net.sf.freecol.client.control.PreGameController;
import net.sf.freecol.client.control.PreGameInputHandler;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.MapViewer;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.action.ActionManager;
import net.sf.freecol.client.gui.plaf.FreeColLookAndFeel;
import net.sf.freecol.client.gui.sound.SoundPlayer;
import net.sf.freecol.client.networking.Client;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.io.FreeColDataFile;
import net.sf.freecol.common.io.FreeColModFile;
import net.sf.freecol.common.io.FreeColSavegameFile;
import net.sf.freecol.common.io.FreeColTcFile;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.DOMMessage;
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

    private static FreeColClient instance;


    // Control:
    private ConnectController connectController;

    private PreGameController preGameController;

    private PreGameInputHandler preGameInputHandler;

    private InGameController inGameController;

    private InGameInputHandler inGameInputHandler;

    private MapEditorController mapEditorController;

    private ServerAPI serverAPI;

    
    private GUI gui;



    // Networking:
    /**
     * The network <code>Client</code> that can be used to send messages to
     * the server.
     */
    private Client client;

    // Model:
    private Game game;

    /** The player "owning" this client. */
    private Player player;

    private boolean isRetired = false;

    /**
     * Indicates if the game has started, has nothing to do with
     * whether or not the client is logged in.
     */
    private boolean inGame = false;


    /** The server that has been started from the client-GUI. */
    private FreeColServer freeColServer = null;


    private boolean mapEditor;

    private boolean singleplayer;

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
     * Describe headless here.
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
    public FreeColClient(final File savedGame, Dimension size,
                         final boolean sound,
                         final String splashFilename,
                         final boolean showOpeningVideo, String fontName) {
        
        
        gui = new GUI(this);

        // Look for base data directory.  Failure is fatal.
        File baseDirectory = new File(FreeCol.getDataDirectory(), "base");
        if (!baseDirectory.exists() || !baseDirectory.isDirectory()) {
            System.err.println("Could not find base data directory: "
                               + baseDirectory.getName());
            System.err.println("  The data files could not be found by FreeCol. Please make sure");
            System.err.println("  they are present. If FreeCol is looking in the wrong directory");
            System.err.println("  then run the game with a command-line parameter:\n");
            System.err.println("    --freecol-data <data-directory>\n");
            System.exit(1);
        }

        headless = "true".equals(System.getProperty("java.awt.headless",
                "false"));
        // TODO: make headless operation work
        if (headless) {
            System.err.println("Headless operation disabled.\n");
            System.exit(1);
        }

        mapEditor = false;

        gui.displaySpashScreen(splashFilename);

        // Determine the window size.
        setWindowed(size != null);
        if (size != null && size.width < 0) {
            size = gui.determineWindowSize();
        }

        // Control
        connectController = new ConnectController(this);
        preGameController = new PreGameController(this);
        preGameInputHandler = new PreGameInputHandler(this);
        inGameController = new InGameController(this);
        inGameInputHandler = new InGameInputHandler(this);
        mapEditorController = new MapEditorController(this);
        actionManager = new ActionManager(this);
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
        FreeColTcFile tcData = new FreeColTcFile("classic");
        ResourceManager.setTcMapping(tcData.getResourceMapping());
        actionManager.initializeActions();

        // Load the client options, which handle reloading the
        // resources specified in the active mods.
        loadClientOptions(savedGame);

        // Once resources are in place, get preloading started.
        ResourceManager.preload(size);

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
                = new FreeColLookAndFeel(FreeCol.getDataDirectory(), size);
            FreeColLookAndFeel.install(fclaf, font);
        } catch (FreeColException e) {
            System.err.println("Unable to install FreeCol look-and-feel.");
            e.printStackTrace();
            System.exit(1);
        }

        // Start the GUI.
        
        gui.hideSplashScreen();

        final Dimension windowSize = size;
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

        // Remember the first instance as a quasi-singleton.
        if (instance == null) instance = this;
    }


    /**
     * Gets the quasi-singleton instance.
     */
    public static FreeColClient get() {
        return instance;
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
     * Set the <code>Headless</code> value.
     *
     * @param newHeadless The new Headless value.
     */
    public void setHeadless(final boolean newHeadless) {
        this.headless = newHeadless;
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
     * Describe <code>getFrame</code> method here.
     *
     * @return a <code>JFrame</code> value
     */
    public JFrame getFrame() {
        return gui.getFrame();
    }

    /**
     * Describe <code>updateMenuBar</code> method here.
     *
     */
    public void updateMenuBar() {
        gui.updateMenuBar();
    }


    public void setMapEditor(boolean mapEditor) {
        this.mapEditor = mapEditor;
    }

    public boolean isMapEditor() {
        return mapEditor;
    }

    /**
     * Gets the <code>ImageLibrary</code>.
     *
     * @return The <code>ImageLibrary</code>.
     */
    public ImageLibrary getImageLibrary() {
        return gui.getImageLibrary();
    }

    /**
     * Gets the object responsible for keeping and updating the actions.
     *
     * @return The <code>ActionManager</code>.
     */
    public ActionManager getActionManager() {
        return actionManager;
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

        ActionManager actionManager = getActionManager();
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

        File userOptions = FreeCol.getClientOptionsFile();
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
        if (actionManager != null) actionManager.update();
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
     * Gets the <code>FreeColServer</code> started by the client.
     *
     * @return The <code>FreeColServer</code> or <code>null</code> if no
     *         server has been started.
     */
    public FreeColServer getFreeColServer() {
        return freeColServer;
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
     * Gets the <code>Game</code> that we are currently playing.
     *
     * @return The <code>Game</code>.
     * @see #setGame
     */
    public Game getGame() {
        return game;
    }

    /**
     * Gets the <code>Canvas</code> this client uses to display the
     * GUI-components.
     *
     * @return The <code>Canvas</code>.
     */
    public Canvas getCanvas() {
        return gui.getCanvas();
    }

    /**
     * Gets the <code>GUI</code> that is being used to draw the map on the
     * {@link Canvas}.
     *
     * @return The <code>GUI</code>.
     */
    public MapViewer getMapViewer() {
        return gui.getMapViewer();
    }

    private void exitActions () {
       try {
          // action: delete outdated autosave files
          int validDays = getClientOptions().getInteger(ClientOptions.AUTOSAVE_VALIDITY);
          long validPeriod = (long)validDays * 86400 * 1000;  // millisecond equivalent of valid days
          long timeNow = System.currentTimeMillis();
          File autosaveDir = FreeCol.getAutosaveDirectory();

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
     * Quits the application without any questions.
     */
    public void quit() {
        getConnectController().quitGame(isSingleplayer());
        exitActions();
        gui.quit();
        System.exit(0);
    }


    /**
     * Continue playing after winning the game.
     */
    public void continuePlaying() {
        client.send(DOMMessage.createNewRootElement("continuePlaying"));
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
     * Sets whether or not this game is a singleplayer game.
     *
     * @param singleplayer Indicates whether or not this game is a singleplayer
     *            game.
     * @see #isSingleplayer
     */
    public void setSingleplayer(boolean singleplayer) {
        this.singleplayer = singleplayer;
    }

    /**
     * Is the user playing in singleplayer mode.
     *
     * @return <i>true</i> if the user is playing in singleplayer mode and
     *         <i>false</i> otherwise.
     * @see #setSingleplayer
     */
    public boolean isSingleplayer() {
        return singleplayer;
    }

    /**
     * Sets whether or not the user has retired the game.
     *
     * @param isRetired Indicates whether or not the user has retired the game.
     */
    public void setIsRetired(boolean isRetired) {
        this.isRetired = isRetired;
    }

    /**
     * Has the user retired the game.
     *
     * @return <i>true</i> if the user has retired the game and
     *         <i>false</i> otherwise.
     */
    public boolean isRetired() {
        return isRetired;
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
     * Sets the <code>Client</code> that shall be used to send messages to the
     * server.
     *
     * @param client the <code>Client</code>
     * @see #getClient
     */
    public void setClient(Client client) {
        this.client = client;
    }

    /**
     * Gets the <code>Client</code> that can be used to send messages to the
     * server.
     *
     * @return the <code>Client</code>
     * @see #setClient
     */
    public Client getClient() {
        return client;
    }

    public SoundPlayer getSoundPlayer()
    {
        return gui.getSoundPlayer();
    }

    /**
     * Plays some sound. Parameter == null stops playing a sound.
     *
     * @param sound The sound resource to play or <b>null</b>
     */
    public void playSound(String sound) {
        gui.playSound(sound);
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

    /**
     * Sets whether or not this client is logged in to a server.
     *
     * @param loggedIn An indication of whether or not this client is logged in
     *            to a server.
     */
    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }

    /**
     * Meaningfully named access to the ServerAPI.
     *
     * @return A ServerAPI.
     */
    public ServerAPI askServer() {
        if (serverAPI == null) serverAPI = new ServerAPI(this);
        return serverAPI;
    }

    /**
     * Set the game-wide next active unit if one can be found.
     *
     * @param unitId A unit id for the unit to make active.
     */
    public void setActiveUnit(String unitId) {
        if (unitId != null && getGame() != null) {
            Unit active = (Unit) getGame().getFreeColGameObject(unitId);
            if (active != null) {
                active.getOwner().resetIterators();
                active.getOwner().setNextActiveUnit(active);
                getMapViewer().setActiveUnit(active);
            }
        }
    }



    /**
     * Notifies this GUI that the game has started or ended.
     * @param inGame Indicates whether or not the game has started.
     */
    public void setInGame(boolean inGame) {
        this.inGame = inGame;
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
     * Start the game skipping turns.
     *
     * @param turns The number of turns to skip.
     */
    public void skipTurns(int turns) {
        if (freeColServer == null) return;
        freeColServer.getInGameController().setSkippedTurns(turns);
        getCanvas().closeMenus();
        askServer().startSkipping();
    }


    public void setWindowed(boolean windowed) {
        gui.setWindowed(windowed);
    }

    public GUI getGUI() {
        return gui;
    }
}
