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

package net.sf.freecol.client;

import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.SwingUtilities;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.control.ClientModelController;
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
import net.sf.freecol.client.gui.sound.MusicLibrary;
import net.sf.freecol.client.gui.sound.SfxLibrary;
import net.sf.freecol.client.gui.sound.SoundLibrary;
import net.sf.freecol.client.gui.sound.SoundPlayer;
import net.sf.freecol.client.networking.Client;
import net.sf.freecol.common.io.FreeColModFile;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.option.AudioMixerOption;
import net.sf.freecol.common.option.LanguageOption;
import net.sf.freecol.common.option.Option;
import net.sf.freecol.common.option.PercentageOption;
import net.sf.freecol.common.option.LanguageOption.Language;
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

    // Control:
    private ConnectController connectController;

    private PreGameController preGameController;

    private PreGameInputHandler preGameInputHandler;

    private InGameController inGameController;

    private InGameInputHandler inGameInputHandler;

    private ClientModelController modelController;
    
    private MapEditorController mapEditorController;
    

    // Gui:
    private GraphicsDevice gd;

    private JFrame frame;

    private Canvas canvas;

    private GUI gui;

    private ImageLibrary imageLibrary;

    private MusicLibrary musicLibrary;

    private SfxLibrary sfxLibrary;

    private SoundPlayer musicPlayer;

    private SoundPlayer sfxPlayer;

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
     * @param windowed Determines if the <code>Canvas</code> should be
     *            displayed within a <code>JFrame</code> (when
     *            <code>true</code>) or in fullscreen mode (when
     *            <code>false</code>).
     * @param innerWindowSize The inner size of the window (borders not included).
     * @param imageLibrary The object holding the images.
     * @param musicLibrary The object holding the music.
     * @param sfxLibrary The object holding the sound effects.
     * @param showOpeningVideo Display the opening video.
     */
    public FreeColClient(boolean windowed, final Dimension innerWindowSize, 
                         ImageLibrary imageLibrary, MusicLibrary musicLibrary,
                         SfxLibrary sfxLibrary, final boolean showOpeningVideo) {
        headless = "true".equals(System.getProperty("java.awt.headless", "false"));
        this.windowed = windowed;
        this.imageLibrary = imageLibrary;
        this.musicLibrary = musicLibrary;
        this.sfxLibrary = sfxLibrary;
        
        mapEditor = false;
        
        clientOptions = new ClientOptions(Specification.getSpecification());
        actionManager = new ActionManager(this, Specification.getSpecification());
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
        ResourceManager.preload(innerWindowSize);
        
        // Control:
        connectController = new ConnectController(this);
        preGameController = new PreGameController(this);
        preGameInputHandler = new PreGameInputHandler(this);
        inGameController = new InGameController(this);
        inGameInputHandler = new InGameInputHandler(this);
        modelController = new ClientModelController(this);
        mapEditorController = new MapEditorController(this);
        
        // Gui:
        if (!headless) {
            SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        startGUI(innerWindowSize, showOpeningVideo);
                    }
                });
        }
        worker = new Worker();
        worker.start();
        
        if (FreeCol.getClientOptionsFile() != null
                && FreeCol.getClientOptionsFile().exists()) {
            if (!headless) {
                Option o = clientOptions.getObject(ClientOptions.LANGUAGE);
                o.addPropertyChangeListener(new PropertyChangeListener() {
                        public void propertyChange(PropertyChangeEvent e) {
                            if (((Language) e.getNewValue()).getKey().equals(LanguageOption.AUTO)) {
                                canvas.showInformationMessage("autodetectLanguageSelected");
                            } else {
                                Locale l = ((Language) e.getNewValue()).getLocale();
                                Messages.setMessageBundle(l);
                                canvas.showInformationMessage("newLanguageSelected", "%language%", l.getDisplayName());
                            }
                        }
                    });
            }
        }
    }

    /**
     * Starts the GUI by creating and displaying the GUI-objects.
     */
    private void startGUI(Dimension innerWindowSize, final boolean showOpeningVideo) {
        final AudioMixerOption amo = (AudioMixerOption) getClientOptions().getObject(ClientOptions.AUDIO_MIXER);
        if (musicLibrary != null) {
            musicPlayer = new SoundPlayer(amo,
                    (PercentageOption) getClientOptions().getObject(ClientOptions.MUSIC_VOLUME),
                    false,
                    true);
            //playMusic("intro");
        } else {
            musicPlayer = null;
        }
        if (sfxLibrary != null) {
            sfxPlayer = new SoundPlayer(amo,
                    (PercentageOption) getClientOptions().getObject(ClientOptions.SFX_VOLUME),
                    true,
                    false);
        } else {
            sfxPlayer = null;
        }
        
        if (GraphicsEnvironment.isHeadless()) {
            logger.info("It seems that the GraphicsEnvironment is headless!");
        }
        gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        if (!windowed) {
            if (!gd.isFullScreenSupported()) {
                String fullscreenNotSupported = "\nIt seems that full screen mode is not fully supported for this\nGraphicsDevice. Please try the \"--windowed\" option if you\nexperience any graphical problems while running FreeCol.";
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
        if (showOpeningVideo) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    canvas.showOpeningVideoPanel();
                }
            });
        } else {
            canvas.showMainPanel();
            playMusic("intro");
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
        boolean result = reply != null && "true".equals(reply.getAttribute("highScore"));
        Element endTurnElement = Message.createNewRootElement("endTurn");
        client.send(endTurnElement);
        return result;
    }


    /**
     * Continue playing after win the game
     */
    public void continuePlaying() {
        Element continueElement = Message.createNewRootElement("continuePlaying");
        client.send(continueElement);
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
     * @param singleplayer Indicates whether or not the user has retired the game.
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
     * Gets the <code>ClientModelController</code>.
     * 
     * @return The <code>ClientModelController</code>.
     */
    public ClientModelController getModelController() {
        return modelController;
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

    /**
     * Plays the music.
     */
    public void playMusic(String music) {
        if (musicPlayer != null) {
            musicPlayer.play(musicLibrary.get(music));
        }
    }
    
    /**
     * Plays a random music from the given playlist.
     */
    public void playMusicOnce(String music) {
        if (musicPlayer != null) {
            musicPlayer.playOnce(musicLibrary.get(music));
        }
    }
    
    /**
     * Plays a random music from the given playlist.
     * @param delay A delay before playing the sound (ms).
     */
    public void playMusicOnce(String music, int delay) {
        if (musicPlayer != null) {
            musicPlayer.playOnce(musicLibrary.get(music), delay);
        }
    }
    
    /**
     * Plays the given sound effect.
     * 
     * @param sound The key sound effect given by {@link SfxLibrary}.
     */
    public void playSound(String sound) {
        if (sfxPlayer != null) {
            sfxPlayer.play(sfxLibrary.get(sound));
        }
    }

    /**
     * Plays the given sound effect.
     * 
     * @param sound The key sound effect given by {@link SfxLibrary}.
     */
    public void playSound(SoundLibrary.SoundEffect sound) {
        if (sfxPlayer != null) {
            sfxPlayer.play(sfxLibrary.get(sound));
        }
    }
    
    /**
     * Verifies if the client can play music
     */
    public boolean canPlayMusic(){
        return musicPlayer != null;
    }

    /**
     * Returns <i>true</i> if this client is logged in to a server or <i>false</i>
     * otherwise.
     * 
     * @return <i>true</i> if this client is logged in to a server or <i>false</i>
     *         otherwise.
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
}

