package net.sf.freecol.client;

import java.util.logging.Logger;

import javax.swing.JFrame;

import java.awt.Color;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;


import net.sf.freecol.client.gui.*;
import net.sf.freecol.client.gui.sound.*;
import net.sf.freecol.client.control.*;
import net.sf.freecol.client.networking.*;

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;

import net.sf.freecol.FreeCol;
import net.sf.freecol.server.FreeColServer;

import         javax.swing.*;
import         java.awt.Graphics;


/**
* The main control class for the FreeCol client. This class both
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


    // Gui:
    private JFrame frame;
    private Canvas canvas;
    private GUI gui;
    private ImageLibrary imageLibrary;
    private MusicLibrary musicLibrary;
    private SfxLibrary sfxLibrary;
    private SoundPlayer musicPlayer;
    private SoundPlayer sfxPlayer;


    // Networking:

    /** The network <code>Client</code> that can be used to send messages to the server. */
    private Client client;
    
    
    // Model:

    private Game game;
    
    /** The player "owning" this client. */
    private Player player;

    /** The Server that has been started from the client-GUI. */
    private FreeColServer freeColServer = null;

    private boolean windowed;
    private boolean singleplayer;
    private boolean admin = false;




    /**
    * Creates a new <code>FreeColClient</code>. Creates the control 
    * objects and starts the GUI.
    */
    public FreeColClient(boolean windowed, ImageLibrary imageLibrary, MusicLibrary musicLibrary, SfxLibrary sfxLibrary) {
        this.windowed = windowed;
        this.imageLibrary = imageLibrary;
        this.musicLibrary = musicLibrary;
        this.sfxLibrary = sfxLibrary;

        // Control:
        connectController = new ConnectController(this);
        preGameController = new PreGameController(this);
        preGameInputHandler = new PreGameInputHandler(this);
        inGameController = new InGameController(this);
        inGameInputHandler = new InGameInputHandler(this);

        // Gui:
        startGUI(windowed, imageLibrary, musicLibrary, sfxLibrary);
    }





    /**
    * Starts the GUI by creating and displaying the GUI-objects.
    */
    private void startGUI(boolean windowed, ImageLibrary lib, MusicLibrary musicLibrary, SfxLibrary sfxLibrary) {
        // TODO: Show splash-screen

        if (musicLibrary != null) {
            musicPlayer = new SoundPlayer(false, true, true);
        } else {
            musicPlayer = null;
        }

        // TODO: Start playing some music here.

        if (sfxLibrary != null) {
            sfxPlayer = new SoundPlayer(true, false, false);
        } else {
            sfxPlayer = null;
        }


        if (GraphicsEnvironment.isHeadless()) {
            logger.info("It seems that the GraphicsEnvironment is headless!");
        }

        if (windowed) {
            frame = new WindowedFrame();
        } else {
            GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            if (!gd.isFullScreenSupported()) {
                logger.warning("It seems that full screen mode is not supported for this GraphicsDevice! Using windowed mode instead.");

                windowed = true;
                setWindowed(true);
                frame = new WindowedFrame();
            } else {
                frame = new FullScreenFrame(gd);
            }
        }

        gui = new GUI(this, frame.getBounds(), null, lib);
        canvas = new Canvas(this, frame.getBounds(), gui);

        if (frame instanceof WindowedFrame) {
            ((WindowedFrame) frame).setCanvas(canvas);
        } else if (frame instanceof FullScreenFrame) {
            ((FullScreenFrame) frame).setCanvas(canvas);
        }

        frame.getContentPane().add(canvas);

        if (!windowed) {
            frame.repaint();
        }
        
        frame.setVisible(true);
    }


    /**
    * Gets the <code>Player</code> that uses this client.
    *
    * @return The <code>Player</code> made to represent this clients user.
    * @see #setMyPlayer
    */
    public Player getMyPlayer() {
        return player;
    }


    /**
    * Sets the <code>Player</code> that uses this client.
    *
    * @param player The <code>Player</code> made to represent this clients user.
    * @see #getMyPlayer
    */
    public void setMyPlayer(Player player) {
        this.player = player;
    }


    public void setFreeColServer(FreeColServer freeColServer) {
        this.freeColServer = freeColServer;
    }


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
    * Gets the <code>GUI</code> that is beeing used to draw the map
    * on the {@link Canvas}
    *
    * @return The <code>GUI</code>.
    */
    public GUI getGUI() {
        return gui;
    }


    /**
    * Sets this client to be the game admin or not.
    *
    * @param admin The boolean indicating wether or not this client
    *              is the game admin.
    */
    public void setAdmin(boolean admin) {
        this.admin = admin;
    }


    /**
    * Checks if this client is the game admin.
    * @return <i>true</i> if the client is the game admin and <i>false</i> otherwise.
    */
    public boolean isAdmin() {
        return admin;
    }


    /**
    * Sets the type of main window to display.
    *
    * @param windowed The main window is a full-screen window if
    *                 set to <i>false</i> and a normal window otherwise.
    */
    private void setWindowed(boolean windowed) {
        this.windowed = windowed;
    }


    /**
    * Sets wether or not this game is a singleplayer game.
    * @param singleplayer Indicates wether or not this game is a
    *                     singleplayer game.
    * @see #isSingleplayer
    */
    public void setSingleplayer(boolean singleplayer) {
        this.singleplayer = singleplayer;
    }


    /**
    * Is the user playing in singleplayer mode.
    *
    * @return <i>true</i> if the user is playing in singleplayer mode and <i>false</i> otherwise.
    * @see #setSingleplayer
    */
    public boolean isSingleplayer() {
        return singleplayer;
    }


    /**
    * Gets the controller responsible for starting a server and
    * connecting to it.
    */
    public ConnectController getConnectController() {
        return connectController;
    }


    /**
    * Gets the controller that will be used before the game
    * has been started.
    */
    public PreGameController getPreGameController() {
        return preGameController;
    }


    /**
    * Gets the input handler that will be used before the game
    * has been started.
    */
    public PreGameInputHandler getPreGameInputHandler() {
        return preGameInputHandler;
    }


    /**
    * Gets the controller that will be used when the game
    * has been started.
    */
    public InGameController getInGameController() {
        return inGameController;
    }


    /**
    * Gets the input handler that will be used when the game
    * has been started.
    */
    public InGameInputHandler getInGameInputHandler() {
        return inGameInputHandler;
    }


    /**
    * Sets the <code>Client</code> that shall be used to send
    * messages to the server.
    *
    * @param client the <code>Client</code>
    * @see #getClient
    */
    public void setClient(Client client) {
        this.client = client;
    }


    /**
    * Gets the <code>Client</code> that can be used to send
    * messages to the server.
    *
    * @return the <code>Client</code>
    * @see #setClient
    */
    public Client getClient() {
        return client;
    }
    
    
    public void playSound(int sound) {
        if (sfxPlayer != null) {
            sfxPlayer.play(sfxLibrary.get(sound));
        }
    }
}


