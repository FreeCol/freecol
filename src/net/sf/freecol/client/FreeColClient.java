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
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.control.ConnectController;
import net.sf.freecol.client.control.InGameController;
import net.sf.freecol.client.control.InGameInputHandler;
import net.sf.freecol.client.control.MapEditorController;
import net.sf.freecol.client.control.PreGameController;
import net.sf.freecol.client.control.PreGameInputHandler;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.FreeColMenuBar;
import net.sf.freecol.client.gui.FullScreenFrame;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.WindowedFrame;
import net.sf.freecol.client.gui.action.ActionManager;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.plaf.FreeColLookAndFeel;
import net.sf.freecol.client.gui.sound.SoundPlayer;
import net.sf.freecol.client.networking.Client;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.io.FreeColDataFile;
import net.sf.freecol.common.io.FreeColModFile;
import net.sf.freecol.common.io.FreeColTcFile;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.option.AudioMixerOption;
import net.sf.freecol.common.option.LanguageOption;
import net.sf.freecol.common.option.LanguageOption.Language;
import net.sf.freecol.common.option.Option;
import net.sf.freecol.common.option.PercentageOption;
import net.sf.freecol.common.resources.ResourceManager;
import net.sf.freecol.common.resources.ResourceMapping;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.FreeColServer.GameState;

import org.w3c.dom.Element;

/**
 * The main control class for the FreeCol client. This class both starts and
 * keeps references to the GUI and the control objects.
 */
public final class FreeColClient {

    private static final Logger logger = Logger.getLogger(FreeColClient.class.getName());
    private static FreeColClient instance;

    public static FreeColClient get () {return instance;}

    /**
     * The space not being used in windowed mode.
     */
    private static final int DEFAULT_WINDOW_SPACE = 100;

    // Control:
    private ConnectController connectController;

    private PreGameController preGameController;

    private PreGameInputHandler preGameInputHandler;

    private InGameController inGameController;

    private InGameInputHandler inGameInputHandler;

    private MapEditorController mapEditorController;


    // Gui:
    private GraphicsDevice gd;

    private JFrame frame;

    private Canvas canvas;

    private GUI gui;

    private ImageLibrary imageLibrary;

    private SoundPlayer soundPlayer;

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

    /** The Server that has been started from the client-GUI. */
    private FreeColServer freeColServer = null;

    private boolean windowed;

    private boolean mapEditor;

    private boolean singleplayer;

    private final ActionManager actionManager;

    private ClientOptions clientOptions;

    public final Worker worker;

    /**
     * Indicated whether or not there is an open connection to the server. This
     * is not an indication of the existence of a Connection Object, but instead
     * it is an indication of an approved login to a server.
     */
    private boolean loggedIn = false;

    private Rectangle windowBounds;

    /**
     * Describe headless here.
     */
    private boolean headless;


    /**
     * Creates a new <code>FreeColClient</code>. Creates the control objects
     * and starts the GUI.
     *
     * @param savegameFile a <code>File</code> value
     * @param size a <code>Dimension</code> value
     * @param sound True if sounds should be played
     * @param splashFilename a <code>String</code> value
     * @param showOpeningVideo Display the opening video.
     * @param fontName a <code>String</code> value
     */
    public FreeColClient(final File savegameFile, Dimension size,
                         final boolean sound,
                         final String splashFilename,
                         final boolean showOpeningVideo, String fontName) {

        headless = "true".equals(System.getProperty("java.awt.headless", "false"));

        // look for data base directory
        File baseDirectory = new File(FreeCol.getDataDirectory(), "base");
        if (!baseDirectory.exists() || !baseDirectory.isDirectory()) {
            System.err.println("Could not find base data directory: "
                               + baseDirectory.getName());
            System.exit(1);
        }

        // display the splash screen
        JWindow splash = null;
        if (splashFilename != null) {
            splash = displaySplash(splashFilename);
        }

        // load the resource mappings (whatever that is!)
        FreeColDataFile baseData = new FreeColDataFile(baseDirectory);
        ResourceManager.setBaseMapping(baseData.getResourceMapping());

        FreeColTcFile tcData = new FreeColTcFile("classic");
        ResourceManager.setTcMapping(tcData.getResourceMapping());

        imageLibrary = new ImageLibrary();

        // calculate a window size (if "windowed" mode is opted)
        windowed = (size != null);
        if (size != null && size.width < 0) {
            final Rectangle bounds = GraphicsEnvironment.getLocalGraphicsEnvironment()
                                     .getMaximumWindowBounds();
            size = new Dimension(bounds.width - DEFAULT_WINDOW_SPACE,
                                 bounds.height - DEFAULT_WINDOW_SPACE);
        }
        final Dimension windowSize = size;
        logger.info("Window size is " + windowSize);

        // Swing system + LAF init
        Font font = null;
        if (fontName != null) {
            font = Font.decode(fontName);
            if (font == null) {
                System.err.println("Font not found: " + fontName);
            }
        }
        if (font == null) font = ResourceManager.getFont("NormalFont");
        try {
            FreeColLookAndFeel fclaf
                = new FreeColLookAndFeel(FreeCol.getDataDirectory(), windowSize);
            try {
                FreeColLookAndFeel.install(fclaf, font);
            } catch (FreeColException e) {
                e.printStackTrace();
                System.err.println("Unable to install FreeCol look-and-feel.");
                System.exit(1);
            }
        } catch (FreeColException e) {
            removeSplash(splash);
            e.printStackTrace();
            System.err.println("\nThe data files could not be found by FreeCol. Please make sure");
            System.err.println("they are present. If FreeCol is looking in the wrong directory");
            System.err.println("then run the game with a command-line parameter:\n");
            System.exit(1);
        }

        // TODO: don't use same datafolder for both images and
        // music because the images are best kept inside the .JAR
        // file.
        logger.info("Now starting to load images.");

        mapEditor = false;

        // load options
        clientOptions = new ClientOptions();
        actionManager = new ActionManager(this);
        if (!headless) {
            actionManager.initializeActions();
        }
        if (FreeCol.getClientOptionsFile() != null
                && FreeCol.getClientOptionsFile().exists()) {
            clientOptions.load(FreeCol.getClientOptionsFile());
        }

        List<ResourceMapping> modMappings = new ArrayList<ResourceMapping>();
        for (FreeColModFile f : clientOptions.getActiveMods()) {
            modMappings.add(f.getResourceMapping());
        }
        ResourceManager.setModMappings(modMappings);
        ResourceManager.preload(size);

        //If modMappings change any UI resources related to actions,
        //these would not become visible without a 2nd call to initializeActions()
        //potential TODO: find a better way to untangle this.
        if (!headless) {
            actionManager.initializeActions();
        }

        // Control:
        connectController = new ConnectController(this);
        preGameController = new PreGameController(this);
        preGameInputHandler = new PreGameInputHandler(this);
        inGameController = new InGameController(this);
        inGameInputHandler = new InGameInputHandler(this);
        mapEditorController = new MapEditorController(this);

        removeSplash(splash);

        // Gui:
        if (!headless) {
            SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        startGUI(windowSize, sound, showOpeningVideo, savegameFile != null);
                    }
                });
        }
        worker = new Worker();
        worker.start();

        if (savegameFile != null) {
            SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        connectController.loadGame(savegameFile);
                    }
                });
        }

        if (FreeCol.getClientOptionsFile() != null
                && FreeCol.getClientOptionsFile().exists()) {
            if (!headless) {
                Option o = clientOptions.getOption(ClientOptions.LANGUAGE);
                o.addPropertyChangeListener(new PropertyChangeListener() {
                        public void propertyChange(PropertyChangeEvent e) {
                            if (((Language) e.getNewValue()).getKey().equals(LanguageOption.AUTO)) {
                                canvas.showInformationMessage("autodetectLanguageSelected");
                            } else {
                                Locale l = ((Language) e.getNewValue()).getLocale();
                                Messages.setMessageBundle(l);
                                canvas.showInformationMessage("newLanguageSelected",
                                        "%language%", l.getDisplayName());
                            }
                        }
                    });
            }
        }

        // remember the first instance as (quasi) singleton
        if ( instance == null )
                instance = this;
    }

    /**
     * Displays a splash screen.
     * @return The splash screen. It should be removed by the caller
     *      when no longer needed by a call to removeSplash().
     */
    private static JWindow displaySplash(String filename) {
        try {
            Image im = Toolkit.getDefaultToolkit().getImage(filename);
            JWindow f = new JWindow();
            f.getContentPane().add(new JLabel(new ImageIcon(im)));
            f.pack();
            Point center = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
            f.setLocation(center.x - f.getWidth() / 2, center.y - f.getHeight() / 2);
            f.setVisible(true);
            return f;
        } catch (Exception e) {
            logger.warning(e.toString());
            return null;
        }
    }

    /**
     * Removes splash screen.
     */
    private static void removeSplash(JWindow splash) {
        if (splash != null) {
            splash.setVisible(false);
            splash.dispose();
        }
    }

    /**
     * Starts the GUI by creating and displaying the GUI-objects.
     */
    private void startGUI(Dimension innerWindowSize,
                          final boolean sound,
                          final boolean showOpeningVideo,
                          final boolean loadGame) {
        if (sound) {
            final ClientOptions opts = getClientOptions();
            final AudioMixerOption amo
                = (AudioMixerOption) opts.getOption(ClientOptions.AUDIO_MIXER);
            final PercentageOption volume
                = (PercentageOption) opts.getOption(ClientOptions.AUDIO_VOLUME);
            try {
                soundPlayer = new SoundPlayer(amo, volume);
            } catch (Exception e) {
                // #3168279 reports an undocumented NPE thrown by
                // AudioSystem.getMixer(null).  Workaround this and other
                // such failures by just disabling sound.
                soundPlayer = null;
                logger.log(Level.WARNING, "Unexpected sound failure", e);
            }
        } else {
            soundPlayer = null;
        }

        if (GraphicsEnvironment.isHeadless()) {
            logger.info("It seems that the GraphicsEnvironment is headless!");
        }
        gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        if (!windowed) {
            if (!gd.isFullScreenSupported()) {
                String fullscreenNotSupported =
                   "\nIt seems that full screen mode is not fully supported for this" +
                   "\nGraphicsDevice. Please try the \"--windowed\" option if you\nexperience" +
                   "any graphical problems while running FreeCol.";
                logger.info(fullscreenNotSupported);
                System.out.println(fullscreenNotSupported);
                /*
                 * We might want this behavior later: logger.warning("It seems
                 * that full screen mode is not supported for this
                 * GraphicsDevice! Using windowed mode instead."); windowed =
                 * true; setWindowed(true); frame = new
                 * WindowedFrame(windowSize);
                 */
            }
            Rectangle bounds = gd.getDefaultConfiguration().getBounds();
            innerWindowSize = new Dimension(bounds.width - bounds.x, bounds.height - bounds.y);
        }
        gui = new GUI(this, innerWindowSize, imageLibrary);
        canvas = new Canvas(this, innerWindowSize, gui);
        changeWindowedMode(windowed);

        frame.setIconImage(ResourceManager.getImage("FrameIcon.image"));
        if (showOpeningVideo && !loadGame) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    canvas.showOpeningVideoPanel();
                }
            });
        } else {
            if (!loadGame) {
                canvas.showMainPanel();
            }
            playSound("sound.intro.general");
        }
        gui.startCursorBlinking();
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
        return frame;
    }

    /**
     * Describe <code>updateMenuBar</code> method here.
     *
     */
    public void updateMenuBar() {
        if (frame != null && frame.getJMenuBar() != null) {
            ((FreeColMenuBar) frame.getJMenuBar()).update();
        }
    }

    /**
     * Change the windowed mode.
     * @param windowed Use <code>true</code> for windowed mode
     *      and <code>false</code> for fullscreen mode.
     */
    public void changeWindowedMode(boolean windowed) {
        JMenuBar menuBar = null;
        if (frame != null) {
            menuBar = frame.getJMenuBar();
            if (frame instanceof WindowedFrame) {
                windowBounds = frame.getBounds();
            }
            frame.setVisible(false);
            frame.dispose();
        }
        this.windowed = windowed;
        if (windowed) {
            frame = new WindowedFrame();
        } else {
            frame = new FullScreenFrame(gd);
        }
        frame.setJMenuBar(menuBar);
        if (frame instanceof WindowedFrame) {
            ((WindowedFrame) frame).setCanvas(canvas);
            frame.getContentPane().add(canvas);
            if (windowBounds != null) {
                frame.setBounds(windowBounds);
            } else {
                frame.pack();
            }
        } else if (frame instanceof FullScreenFrame) {
            ((FullScreenFrame) frame).setCanvas(canvas);
            frame.getContentPane().add(canvas);
        }
        gui.forceReposition();
        canvas.updateSizes();
        frame.setVisible(true);
    }

    /**
     * Checks if the application is displayed in a window.
     * @return <code>true</code> if the application is currently
     *      displayed in a frame, and <code>false</code> if
     *      currently in fullscreen mode.
     * @see #changeWindowedMode
     */
    public boolean isWindowed() {
        return windowed;
    }

    /**
     * Writes the client options to the default location.
     *
     * @see ClientOptions
     */
    public void saveClientOptions() {
        saveClientOptions(FreeCol.getClientOptionsFile());
    }

    public void setMapEditor(boolean mapEditor) {
        this.mapEditor = mapEditor;
    }

    public boolean isMapEditor() {
        return mapEditor;
    }

    /**
     * Writes the client options to the given file.
     *
     * @param saveFile The file where the client options should be written.
     * @see ClientOptions
     */
    public void saveClientOptions(File saveFile) {
        getClientOptions().save(saveFile);
    }

    /**
     * Gets the <code>ImageLibrary</code>.
     *
     * @return The <code>ImageLibrary</code>.
     */
    public ImageLibrary getImageLibrary() {
        return imageLibrary;
    }

    /**
     * Reads the {@link ClientOptions} from the given file.
     */
    public void loadClientOptions() {
        loadClientOptions(FreeCol.getClientOptionsFile());
    }

    /**
     * Reads the {@link ClientOptions} from the given file.
     *
     * @param loadFile The <code>File</code> to read the
     *            <code>ClientOptions</code> from.
     */
    public void loadClientOptions(File loadFile) {
        getClientOptions().load(loadFile);
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
        return canvas;
    }

    /**
     * Gets the <code>GUI</code> that is being used to draw the map on the
     * {@link Canvas}.
     *
     * @return The <code>GUI</code>.
     */
    public GUI getGUI() {
        return gui;
    }

    /**
     * Quits the application without any questions.
     */
    public void quit() {
        getConnectController().quitGame(isSingleplayer());
        if (!windowed) {
            try {
                gd.setFullScreenWindow(null);
            } catch(Exception e) {
                // this can fail, but who cares?
                // we are quitting anyway
                System.exit(1);
            }
        }
        System.exit(0);
    }

    /**
     * Retires the player from the game.
     *
     * @return True if the player achieved a new high score.
     */
    public boolean retire() {
        Element retireElement = Message.createNewRootElement("retire");
        Element reply = client.ask(retireElement);
        boolean result = reply != null
            && "true".equals(reply.getAttribute("highScore"));
        return result;
    }


    /**
     * Continue playing after winning the game.
     */
    public void continuePlaying() {
        client.send(Message.createNewRootElement("continuePlaying"));
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

    public SoundPlayer getSoundPlayer ()
    {
        return soundPlayer;
    }

    /**
     * Plays some sound. Parameter == null stops playing a sound.
     *
     * @param sound The sound resource to play or <b>null</b>
     */
    public void playSound(String sound) {
        if (canPlaySound()) {
            if (sound == null)
               soundPlayer.fade();
            else {
               File file = ResourceManager.getAudio(sound);
               if (file != null) {
                   soundPlayer.playOnce(file);
               }
               logger.finest(((file == null) ? "Could not load" : "Playing")
                             + " sound: " + sound);
            }
        }
    }

    /**
     * Verifies if the client can play sounds.
     */
    public boolean canPlaySound() {
        return soundPlayer != null;
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
     * Set the game-wide next active unit if one can be found.
     *
     */
    public void setActiveUnit() {
        if (getFreeColServer() != null) {
            Unit active = getFreeColServer().getActiveUnit();
            if (active != null && getGame() != null) {
                // Convert to client side
                active = (Unit) getGame().getFreeColGameObject(active.getId());
                active.getOwner().setNextActiveUnit(active);
            }
        }
    }
}
