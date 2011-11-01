package net.sf.freecol.client.gui;

import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.menu.FreeColMenuBar;
import net.sf.freecol.client.gui.menu.InGameMenuBar;
import net.sf.freecol.client.gui.menu.MapEditorMenuBar;
import net.sf.freecol.client.gui.sound.SoundPlayer;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.option.AudioMixerOption;
import net.sf.freecol.common.option.BooleanOption;
import net.sf.freecol.common.option.LanguageOption;
import net.sf.freecol.common.option.PercentageOption;
import net.sf.freecol.common.option.LanguageOption.Language;
import net.sf.freecol.common.resources.ResourceManager;

public class GUI {
    

    private static final Logger logger = Logger.getLogger(GUI.class.getName());

    public static final String COPYRIGHT = "Copyright (C) 2003-2011 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";
    

    /**
     * The space not being used in windowed mode.
     */
    private static final int DEFAULT_WINDOW_SPACE = 100;
    

    private FreeColClient freeColClient;

    
    // GUI:
    private GraphicsDevice gd;
    
    private JFrame frame;

    private Canvas canvas;

    private MapViewer mapViewer;

    private ImageLibrary imageLibrary;

    private SoundPlayer soundPlayer;

    private boolean windowed;

    private Rectangle windowBounds;

    private JWindow splash;

    public GUI(FreeColClient freeColClient) {
        this.freeColClient = freeColClient;
        this.imageLibrary = new ImageLibrary();
    }

    /**
     * Verifies if the client can play sounds.
     * @return boolean <b>true</b> if and only if client sound player has an instance
     */
    public boolean canPlaySound() {
        return soundPlayer != null;
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
                this.windowBounds = frame.getBounds();
            }
            frame.setVisible(false);
            frame.dispose();
        }
        setWindowed(windowed);
        if (windowed) {
            this.frame = new WindowedFrame();
        } else {
            this.frame = new FullScreenFrame(gd);
        }
        frame.setJMenuBar(menuBar);
        if (frame instanceof WindowedFrame) {
            ((WindowedFrame) frame).setCanvas(getCanvas());
            frame.getContentPane().add(getCanvas());
            if (getWindowBounds() != null) {
                frame.setBounds(getWindowBounds());
            } else {
                frame.pack();
            }
        } else if (frame instanceof FullScreenFrame) {
            ((FullScreenFrame) frame).setCanvas(getCanvas());
            frame.getContentPane().add(getCanvas());
        }
        mapViewer.forceReposition();
        canvas.updateSizes();
        frame.setVisible(true);
    }

    public Dimension determineWindowSize() {
        
        Rectangle bounds = GraphicsEnvironment
            .getLocalGraphicsEnvironment().getMaximumWindowBounds();
        Dimension size = new Dimension(bounds.width - DEFAULT_WINDOW_SPACE,
                             bounds.height - DEFAULT_WINDOW_SPACE);
        logger.info("Window size is " + size.getWidth()
            + " x " + size.getHeight());
        return size;
    }

    public void displayChat(String senderNme, String message, boolean privateChat) {
        canvas.displayChat(senderNme, message, privateChat);
    
    }

    public void displaySpashScreen(final String splashFilename) {
        splash = null;
        if (splashFilename != null) {
            try {
                Image im = Toolkit.getDefaultToolkit()
                    .getImage(splashFilename);
                splash = new JWindow();
                splash.getContentPane().add(new JLabel(new ImageIcon(im)));
                splash.pack();
                Point center = GraphicsEnvironment
                    .getLocalGraphicsEnvironment().getCenterPoint();
                splash.setLocation(center.x - splash.getWidth() / 2,
                                   center.y - splash.getHeight() / 2);
                splash.setVisible(true);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Splash fail", e);
                splash = null;
            }
        }
    }

    public void errorMessage(String messageId) {
        canvas.errorMessage(messageId);
    }

    public void errorMessage(String messageID, String message) {
        canvas.errorMessage(messageID, message);
        
    }

    public Canvas getCanvas() {
        return canvas;
    }

    public ImageLibrary getImageLibrary() {
        return imageLibrary;
    }

    public MapViewer getMapViewer() {
        return mapViewer;
    }
    
    
    public SoundPlayer getSoundPlayer() {
        return soundPlayer;
    }

    public Rectangle getWindowBounds() {
        return windowBounds;
    }
    
    public void hideSplashScreen() {
        if (splash != null) {
            splash.setVisible(false);
            splash.dispose();
        }
    }
    
    public boolean isWindowed() {
        return windowed;
    }

    /**
     * Plays some sound. Parameter == null stops playing a sound.
     *
     * @param sound The sound resource to play or <b>null</b>
     */
    public void playSound(String sound) {
        if (canPlaySound()) {
            if (sound == null) {
               soundPlayer.stop();
            } else {
               File file = ResourceManager.getAudio(sound);
               if (file != null) {
                   soundPlayer.playOnce(file);
               }
               logger.finest(((file == null) ? "Could not load" : "Playing")
                             + " sound: " + sound);
            }
        }
    }
    

    public void quit() {
        if (!isWindowed()) {
            try {
                gd.setFullScreenWindow(null);
            } catch(Exception e) {
                // this can fail, but who cares?
                // we are quitting anyway
                System.exit(1);
            }
        }
    }
    
    public void refreshPlayersTable() {
        canvas.refreshPlayersTable();
    }
    
    public void resetMenuBar() {
        JMenuBar menuBar = frame.getJMenuBar();
        if (menuBar != null) {
            ((FreeColMenuBar) menuBar).reset();
        }
    }

    
    public void setupInGameMenuBar() {
        frame.setJMenuBar(new InGameMenuBar(freeColClient, this));        
    }

    public void setupMapEditorMenuBar() {
        frame.setJMenuBar(new MapEditorMenuBar(freeColClient, this));
    }

    public void setupMenuBarToNull() {
        frame.setJMenuBar(null);
    }
    
    public void setUpMouseListenersForCanvas(){
        canvas.addMouseListener(new CanvasMouseListener(freeColClient, canvas, mapViewer));
        canvas.addMouseMotionListener(new CanvasMouseMotionListener(freeColClient, mapViewer,
                 freeColClient.getGame().getMap()));
    }
    
    public void setWindowed(boolean windowed) {
        this.windowed = windowed;
        
    }

    /**
     * Starts the GUI by creating and displaying the GUI-objects.
     */
    public void startGUI(Dimension innerWindowSize,
                          final boolean sound,
                          final boolean showOpeningVideo,
                          final boolean loadGame) {
        final ClientOptions opts = freeColClient.getClientOptions();
        // Prepare the sound system.
        if (sound) {
            final AudioMixerOption amo
                = (AudioMixerOption) opts.getOption(ClientOptions.AUDIO_MIXER);
            final PercentageOption volume
                = (PercentageOption) opts.getOption(ClientOptions.AUDIO_VOLUME);
            try {
                this.soundPlayer = new SoundPlayer(amo, volume);
            } catch (Exception e) {
                // #3168279 reports an undocumented NPE thrown by
                // AudioSystem.getMixer(null).  Workaround this and other
                // such failures by just disabling sound.
                this.soundPlayer = null;
                logger.log(Level.WARNING, "Sound disabled", e);
            }
        } else {
            this.soundPlayer = null;
        }

        if (GraphicsEnvironment.isHeadless()) {
            logger.info("It seems that the GraphicsEnvironment is headless!");
        }
        this.gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        if (!isWindowed()) {
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
                 * WindowedFrame(size);
                 */
            }
            Rectangle bounds = gd.getDefaultConfiguration().getBounds();
            innerWindowSize = new Dimension(bounds.width - bounds.x, bounds.height - bounds.y);
        }

        // Work around a Java 2D bug that seems to be X11 specific.
        // According to:
        //   http://www.oracle.com/technetwork/java/javase/index-142560.html
        //
        //   ``The use of pixmaps typically results in better
        //     performance. However, in certain cases, the opposite is true.''
        //
        // The standard workaround is to use -Dsun.java2d.pmoffscreen=false,
        // but this is too hard for some users, so provide an option to
        // do it easily.  However respect the initial value if present.
        //
        // Remove this if Java 2D is ever fixed.  DHYB.
        //
        final String pmoffscreen = "sun.java2d.pmoffscreen";
        BooleanOption usePixmaps
            = (BooleanOption) opts.getOption(ClientOptions.USE_PIXMAPS);
        String pmoffscreenValue = System.getProperty(pmoffscreen);
        if (pmoffscreenValue == null) {
            System.setProperty(pmoffscreen, usePixmaps.getValue().toString());
            logger.info(pmoffscreen + " using client option: "
                + usePixmaps.getValue().toString());
        } else {
            usePixmaps.setValue(new Boolean(pmoffscreenValue));
            logger.info(pmoffscreen + " overrides client option: "
                + pmoffscreenValue);
        }
        usePixmaps.addPropertyChangeListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent e) {
                    String newValue = e.getNewValue().toString();
                    System.setProperty(pmoffscreen, newValue);
                    logger.info("Set " + pmoffscreen + " to: " + newValue);
                }
            });

        this.mapViewer = new MapViewer(freeColClient, this, innerWindowSize, getImageLibrary());
        this.canvas = new Canvas(freeColClient, this, innerWindowSize, mapViewer);
        changeWindowedMode(isWindowed());
        frame.setIconImage(ResourceManager.getImage("FrameIcon.image"));

        // Now that there is a canvas, prepare for language changes.
        LanguageOption o = (LanguageOption) freeColClient.getClientOptions().getOption(ClientOptions.LANGUAGE);
        if (o != null) {
            o.addPropertyChangeListener(new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent e) {
                        if (((Language) e.getNewValue()).getKey().equals(LanguageOption.AUTO)) {
                            canvas.showInformationMessage("autodetectLanguageSelected");
                        } else {
                            Locale l = ((Language) e.getNewValue()).getLocale();
                            Messages.setMessageBundle(l);
                            canvas.showInformationMessage(StringTemplate.template("newLanguageSelected")
                                .addName("%language%", l.getDisplayName()));
                        }
                    }
                });
        }

        // run opening video or main panel
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
        mapViewer.startCursorBlinking();
    }
    
    public void updateGameOptions() {
        canvas.updateGameOptions();
    }
    
    
    /**
     * Updates the label displaying the current amount of gold.
     */
    public void updateGoldLabel() {
        frame.getJMenuBar().repaint();
    }

    public void updateMapGeneratorOptions() {
        canvas.updateMapGeneratorOptions();
    }

    public void updateMenuBar() {
        if (frame != null && frame.getJMenuBar() != null) {
            ((FreeColMenuBar) frame.getJMenuBar()).update();
        }
    }
    
    
}
