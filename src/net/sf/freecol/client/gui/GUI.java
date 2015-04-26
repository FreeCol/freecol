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

package net.sf.freecol.client.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JWindow;
import javax.swing.Timer;
import javax.swing.filechooser.FileFilter;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.InGameController;
import net.sf.freecol.client.gui.animation.Animations;
import net.sf.freecol.client.gui.menu.FreeColMenuBar;
import net.sf.freecol.client.gui.menu.InGameMenuBar;
import net.sf.freecol.client.gui.menu.MapEditorMenuBar;
import net.sf.freecol.client.gui.panel.ChoiceItem;
import net.sf.freecol.client.gui.panel.ColonyPanel;
import net.sf.freecol.client.gui.panel.ColorChooserPanel;
import net.sf.freecol.client.gui.panel.CornerMapControls;
import net.sf.freecol.client.gui.panel.DialogHandler;
import net.sf.freecol.client.gui.panel.LabourData.UnitData;
import net.sf.freecol.client.gui.panel.LoadingSavegameDialog;
import net.sf.freecol.client.gui.panel.MapControls;
import net.sf.freecol.client.gui.panel.MiniMap;
import net.sf.freecol.client.gui.panel.Parameters;
import net.sf.freecol.client.gui.panel.TradeRoutePanel;
import net.sf.freecol.client.gui.panel.Utility;
import net.sf.freecol.client.gui.plaf.FreeColLookAndFeel;
import net.sf.freecol.client.gui.video.VideoComponent;
import net.sf.freecol.client.gui.video.VideoListener;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.ServerInfo;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.HighScore;
import net.sf.freecol.common.model.IndianNationType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Monarch.MonarchAction;
import net.sf.freecol.common.model.NationSummary;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Region;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Stance;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.TypeCountMap;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.WorkLocation;
import net.sf.freecol.common.option.BooleanOption;
import net.sf.freecol.common.option.LanguageOption;
import net.sf.freecol.common.option.LanguageOption.Language;
import net.sf.freecol.common.option.Option;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.common.resources.ResourceManager;
import net.sf.freecol.common.resources.Video;
import net.sf.freecol.common.util.LogBuilder;

import static net.sf.freecol.common.util.StringUtils.*;


/**
 * A wrapper for the overall GUI.
 */
public class GUI {

    private static final Logger logger = Logger.getLogger(GUI.class.getName());

    /** Actions when an armed unit contacts a settlement. */
    public static enum ArmedUnitSettlementAction {
        SETTLEMENT_ATTACK,
        SETTLEMENT_TRIBUTE,
    }

    /** Actions when dealing with a boycott. */
    public static enum BoycottAction {
        PAY_ARREARS,
        DUMP_CARGO
    }

    /** Actions when buying from the natives. */
    public static enum BuyAction {
        BUY,
        HAGGLE
    }

    /** Actions when claiming land. */
    public static enum ClaimAction {
        ACCEPT,
        STEAL
    }

    /** Actions with a missionary at a native settlement. */
    public static enum MissionaryAction {
        ESTABLISH_MISSION,
        DENOUNCE_HERESY,
        INCITE_INDIANS
    }

    /** Actions in scouting a colony. */
    public static enum ScoutColonyAction {
        FOREIGN_COLONY_NEGOTIATE,
        FOREIGN_COLONY_SPY,
        FOREIGN_COLONY_ATTACK
    }

    /** Actions in scouting a native settlement. */
    public static enum ScoutIndianSettlementAction {
        INDIAN_SETTLEMENT_SPEAK,
        INDIAN_SETTLEMENT_TRIBUTE,
        INDIAN_SETTLEMENT_ATTACK
    }

    /** Actions when selling to the natives. */
    public static enum SellAction {
        SELL,
        HAGGLE,
        GIFT
    }

    /** Choice of sales action at a native settlement. */
    public static enum TradeAction {
        BUY,
        SELL,
        GIFT
    }

    /** Warning levels. */
    private static final String levels[] = {
        "low", "normal", "high"
    };

    /** The space not being used in windowed mode. */
    private static final int DEFAULT_SCREEN_INSET_WIDTH  = 0;
    private static final int DEFAULT_SCREEN_INSET_HEIGHT = 40;
    private static final int DEFAULT_WINDOW_INSET_WIDTH  = 16;
    private static final int DEFAULT_WINDOW_INSET_HEIGHT = 39;

    /** View modes. */
    public static final int MOVE_UNITS_MODE = 0;
    public static final int VIEW_TERRAIN_MODE = 1;



    /** The client for the game. */
    private final FreeColClient freeColClient;

    private final GraphicsDevice graphicsDevice;

    /** An image library to use. */
    private final ImageLibrary imageLibrary;

    /**
     * This is the MapViewer instance used to paint the colony tiles
     * in the ColonyPanel and other panels.  It should not be scaled
     * along with the default MapViewer.
     */
    private MapViewer colonyTileMapViewer;

    /**
     * The MapViewer instance used to paint the main map.
     * This does need to be scaled.
     */
    private MapViewer mapViewer;

    /** The canvas that implements much of the functionality. */
    private Canvas canvas;

    /** The parent frame, either a window or the full screen. */
    private FreeColFrame frame;

    private MapControls mapControls;

    private JWindow splash;

    private boolean windowed;

    private Rectangle windowBounds;


    /**
     * Create the GUI.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param headless If the game is running headless.
     */
    public GUI(FreeColClient freeColClient, boolean headless) {
        this.freeColClient = freeColClient;
        this.graphicsDevice = headless ? null : getGoodGraphicsDevice();
        this.imageLibrary = new ImageLibrary();
    }


    // Simple accessors

    private InGameController igc() {
        return freeColClient.getInGameController();
    }

    public Canvas getCanvas() {
        return canvas;
    }

    public FreeColFrame getFrame() {
        return frame;
    }

    public ImageLibrary getImageLibrary() {
        return imageLibrary;
    }

    public MapViewer getMapViewer() {
        return mapViewer;
    }

    public MapViewer getColonyTileMapViewer() {
        return colonyTileMapViewer;
    }

    public boolean isWindowed() {
        return windowed;
    }

    // Initialization related methods

    /** 
     * Swing system and look-and-feel initialization.
     * 
     * @param fontName An optional font name to be used.
     */
    public static void installLookAndFeel(String fontName) throws FreeColException {
        FreeColLookAndFeel fclaf = new FreeColLookAndFeel();
        FreeColLookAndFeel.install(fclaf);
        Font font = FontLibrary.createMainFont(fontName);
        FreeColLookAndFeel.installFont(font);
    }

    /**
     * Quit the GUI.  All that is required is to exit the full screen.
     */
    public void quit() throws Exception {
        if (this.frame != null) {
            GraphicsConfiguration GraphicsConf = this.frame.getGraphicsConfiguration();
            GraphicsDevice gd = GraphicsConf.getDevice();
            if (!isWindowed()) gd.setFullScreenWindow(null);
        }
    }

    /**
     * In game initializations.
     * Called from PreGameController.startGame().
     *
     * @param tile An initial <code>Tile</code> to select.
     */
    public void initializeInGame(Tile tile) {
        if (frame == null || canvas == null) return;

        frame.setJMenuBar(new InGameMenuBar(freeColClient));
        frame.paintAll(canvas.getGraphics());
        enableMapControls(freeColClient.getClientOptions()
            .getBoolean(ClientOptions.DISPLAY_MAP_CONTROLS));
        setSelectedTile(tile, false);
    }

    /**
     * Set up the mouse listeners for the canvas and map viewer.
     */
    public void setupMouseListeners() {
        if (canvas == null) return;
        canvas.setupMouseListeners();
    }

    /**
     * Display the splash screen.
     *
     * @param splashFilename The name of the file to find the image in.
     */
    public void displaySplashScreen(final String splashFilename) {
        splash = null;
        if (splashFilename == null || graphicsDevice == null)
            return;
        try {
            Image im = Toolkit.getDefaultToolkit().getImage(splashFilename);
            splash = new JWindow(graphicsDevice.getDefaultConfiguration());
            splash.getContentPane().add(new JLabel(new ImageIcon(im)));
            splash.pack();
            Point start = splash.getLocation();
            DisplayMode dm = graphicsDevice.getDisplayMode();
            splash.setLocation(start.x + dm.getWidth()/2 - splash.getWidth() / 2,
                start.y + dm.getHeight()/2 - splash.getHeight() / 2);
            splash.setVisible(true);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Splash fail", e);
            splash = null;
        }
    }

    /**
     * Hide the splash screen.
     */
    public void hideSplashScreen() {
        if (splash != null) {
            splash.setVisible(false);
            splash.dispose();
            splash = null;
        }
    }

    /**
     * Shows the <code>VideoPanel</code>.
     *
     * @param userMsg An optional user message.
     */
    public void showOpeningVideo(final String userMsg) {
        if (canvas == null) return;
        canvas.closeMenus();
        final Video video = ResourceManager.getVideo("video.opening");
        boolean muteAudio = !freeColClient.getSoundController().canPlaySound();
        final VideoComponent vp = new VideoComponent(video, muteAudio);

        final class AbortListener implements ActionListener, KeyListener, MouseListener, VideoListener {

            private Timer t = null;

            @Override
            public void keyPressed(KeyEvent e) {
            }

            @Override
            public void keyReleased(KeyEvent e1) {
                execute();
            }

            @Override
            public void keyTyped(KeyEvent e2) {
            }

            @Override
            public void mouseClicked(MouseEvent e3) {
                execute();
            }

            @Override
            public void mouseEntered(MouseEvent e4) {
            }

            @Override
            public void mouseExited(MouseEvent e5) {
            }

            @Override
            public void mousePressed(MouseEvent e6) {
            }

            @Override
            public void mouseReleased(MouseEvent e7) {
            }

            @Override
            public void stopped() {
                execute();
            }

            @Override
            public void actionPerformed(ActionEvent e8) {
                execute();
            }

            private void setTimer(Timer t1) {
                this.t = t1;
            }

            private void execute() {
                canvas.removeKeyListener(this);
                canvas.removeMouseListener(this);
                vp.removeMouseListener(this);
                //vp.removeVideoListener(this);
                vp.stop();
                canvas.remove(vp);
                if (t != null) {
                    t.stop();
                }
                freeColClient.getSoundController().playSound("sound.intro.general");
                showMainPanel(userMsg);
            }
        }
        AbortListener l = new AbortListener();
        vp.addMouseListener(l);
        //vp.addVideoListener(l);
        canvas.showVideoComponent(vp, l, l);
        vp.play();
        // Cortado applet is failing to quit when finished, make sure it
        // eventually gets kicked.  Change the magic number if we
        // change the opening video length.
        Timer t2 = new Timer(80000, l);
        l.setTimer(t2);
        t2.setRepeats(false);
        t2.start();
    }

    /**
     * Get a good screen device for starting FreeCol.
     *
     * @return A screen device, or null if none available
     *     (as in headless mode).
     */
    private static GraphicsDevice getGoodGraphicsDevice() {
        try {
            return MouseInfo.getPointerInfo().getDevice();
        } catch (HeadlessException he) {}

        try {
            final GraphicsEnvironment lge
                = GraphicsEnvironment.getLocalGraphicsEnvironment();
            return lge.getDefaultScreenDevice();
        } catch (HeadlessException he) {}

        return null;
    }

    /**
     * Starts the GUI by creating and displaying the GUI-objects.
     *
     * @param desiredWindowSize The desired size of the GUI window.
     */
    public void startGUI(final Dimension desiredWindowSize) {
        final ClientOptions opts = freeColClient.getClientOptions();

        if (graphicsDevice == null) {
            logger.info("It seems that the GraphicsEnvironment is headless!");
            return;
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
                + usePixmaps.getValue());
        } else {
            usePixmaps.setValue(Boolean.valueOf(pmoffscreenValue));
            logger.info(pmoffscreen + " overrides client option: "
                + pmoffscreenValue);
        }
        usePixmaps.addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent e) {
                    String newValue = e.getNewValue().toString();
                    System.setProperty(pmoffscreen, newValue);
                    logger.info("Set " + pmoffscreen + " to: " + newValue);
                }
            });

        // Determine the window size.
        Dimension windowSize;
        if (desiredWindowSize == null) {
            if(graphicsDevice.isFullScreenSupported()) {
                windowed = false;
                windowSize = determineFullScreenSize(graphicsDevice);
                logger.info("Full screen window size is " + windowSize);
            } else {
                windowed = true;
                windowSize = new Dimension(-1, -1);
                logger.warning("Full screen not supported.");
                System.err.println(Messages.message("client.fullScreen"));
            }
        } else {
            windowed = true;
            windowSize = desiredWindowSize;
            logger.info("Desired window size is " + windowSize);
        }
        if(isWindowed() && (windowSize.width <= 0 || windowSize.height <= 0)) {
            windowSize = determineWindowSize(graphicsDevice);
            logger.info("Inner window size is " + windowSize);
        }
        this.mapViewer = new MapViewer(freeColClient, windowSize);
        this.canvas = new Canvas(freeColClient, windowSize, mapViewer);
        this.colonyTileMapViewer = new MapViewer(freeColClient, windowSize);

        changeWindowedMode(windowed);
        frame.setIconImage(ResourceManager.getImage("image.miscicon.FrameIcon"));

        // Now that there is a canvas, prepare for language changes.
        LanguageOption o = (LanguageOption)freeColClient.getClientOptions()
            .getOption(ClientOptions.LANGUAGE);
        o.addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent e) {
                    Language language = (Language)e.getNewValue();
                    logger.info("Set language to: " + language);
                    if (Messages.AUTOMATIC.equalsIgnoreCase(language.getKey())) {
                        showInformationMessage("info.autodetectLanguageSelected");
                    } else {
                        Locale l = language.getLocale();
                        Messages.setMessageBundle(l);
                        Messages.setModMessageBundle(l);
                        showInformationMessage(StringTemplate
                            .template("info.newLanguageSelected")
                            .addName("%language%", l.getDisplayName()));
                    }
                }
            });

        mapViewer.startCursorBlinking();
        logger.info("GUI created.");
        logger.info("Starting in Move Units View Mode");
    }

    /**
     * Estimate size of client area when using the full screen.
     *
     * @return A suitable window size.
     */
    private static Dimension determineFullScreenSize(GraphicsDevice gd) {
        Rectangle bounds = gd.getDefaultConfiguration().getBounds();
        return new Dimension(bounds.width - bounds.x,
            bounds.height - bounds.y);
    }

    /**
     * Estimate size of client area for a window of maximum size.
     *
     * @return A suitable window size.
     */
    private static Dimension determineWindowSize(GraphicsDevice gd) {
        // This is supposed to maximize a window
        //   setExtendedState(getExtendedState() | JFrame.MAXIMIZED_BOTH);
        // but there have been problems.

        // Get max size of window including border.
        DisplayMode dm = gd.getDisplayMode();
        int width = dm.getWidth();
        int height = dm.getHeight();
        LogBuilder lb = new LogBuilder(256);
        lb.add("determineWindowSize\n",
            "  Display mode size: ", width, "x", height, "\n");

        // Reduce by any screen insets (windowing system menu bar etc)
        try {
            Insets in = Toolkit.getDefaultToolkit()
                .getScreenInsets(gd.getDefaultConfiguration());
            width -= in.left + in.right;
            height -= in.bottom + in.top;
            lb.add("  less screen insets: ", (in.left + in.right),
                "x", (in.bottom + in.top), "\n");
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to get screen insets", e);
            width -= DEFAULT_SCREEN_INSET_WIDTH;
            height -= DEFAULT_SCREEN_INSET_HEIGHT;
            lb.add("  less faked screen insets: ", DEFAULT_SCREEN_INSET_WIDTH,
                "x", DEFAULT_SCREEN_INSET_HEIGHT, "\n");
        }

        // FIXME: find better way to get size of window title and
        // border.  The information is only available from getInsets
        // when a window is already displayable.
        height -= DEFAULT_WINDOW_INSET_HEIGHT;
        width -= DEFAULT_WINDOW_INSET_WIDTH;
        lb.add("  less faked window decoration adjust: ",
            DEFAULT_WINDOW_INSET_WIDTH, "x", DEFAULT_WINDOW_INSET_HEIGHT, "\n");

        Dimension size = new Dimension(width, height);
        lb.add("  = ", size);
        lb.log(logger, Level.INFO);
        return size;
    }

    /**
     * Change the windowed mode.
     *
     * @param windowed Use <code>true</code> for windowed mode
     *     and <code>false</code> for fullscreen mode.
     */
    public void changeWindowedMode(boolean windowed) {
        // Clean up the old frame
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

        // User might have moved window to new screen in a
        // multi-screen setup, so make this.gd point to the current screen.
        frame = windowed
            ? new WindowedFrame(freeColClient, graphicsDevice)
            : new FullScreenFrame(freeColClient, graphicsDevice);
        frame.setJMenuBar(menuBar);
        frame.setCanvas(canvas);
        frame.updateBounds(windowBounds);
        if (windowed) {
            frame.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    logger.info("Window size changes to " + canvas.getSize());
                }
            });
        }

        mapViewer.forceReposition();
        canvas.updateSizes();
        frame.setVisible(true);
    }

    /**
     * Start the GUI for the map editor.
     */
    public void startMapEditorGUI() {
        if (frame == null || canvas == null) return;

        // We may need to reset the zoom value to the default value
        resetMapZoom();

        frame.setJMenuBar(new MapEditorMenuBar(freeColClient));
        canvas.showMapEditorTransformPanel();

        CanvasMapEditorMouseListener listener
            = new CanvasMapEditorMouseListener(freeColClient, canvas);
        canvas.addMouseListener(listener);
        canvas.addMouseMotionListener(listener);
    }


    // Non-trivial public routines.

    /**
     * Start/stop the goto path display.
     */
    public void activateGotoPath() {
        Unit unit = getActiveUnit();

        // Action should be disabled if there is no active unit, but make sure
        if (unit == null || mapViewer == null) return;

        // Enter "goto mode" if not already activated; otherwise cancel it
        if (mapViewer.isGotoStarted()) {
            mapViewer.stopGoto();
        } else {
            mapViewer.startGoto();

            // Draw the path to the current mouse position, if the
            // mouse is over the screen; see also
            // CanvasMouseMotionListener.
            Point pt = canvas.getMousePosition();
            if (pt != null) {
                Tile tile = mapViewer.convertToMapTile(pt.x, pt.y);
                if (tile != null && unit.getTile() != tile) {
                    mapViewer.setGotoPath(unit.findPath(tile));
                }
            }
        }

    }

    /**
     * Stop the goto path display.
     */
    public void clearGotoPath() {
        Unit unit = getActiveUnit();

        // Action should be disabled if there is no active unit, but make sure
        if (unit == null || mapViewer == null) return;
        mapViewer.stopGoto();
        refresh();
    }

    /**
     * Make image icon from an image.
     *
     * @param image The <code>Image</code> to create an icon for.
     * @return The <code>ImageIcon</code>.
     */
    public static ImageIcon createImageIcon(Image image) {
        return (image==null) ? null : new ImageIcon(image);
    }

    public ImageIcon createUnitImageIcon(Unit unit) {
        return GUI.createImageIcon(imageLibrary.getUnitImage(unit));
    }

    public ImageIcon createSettlementImageIcon(Settlement settlement) {
        return GUI.createImageIcon(imageLibrary.getSettlementImage(settlement));
    }

    public ImageIcon createGoodsImageIcon(GoodsType goodsType) {
        return GUI.createImageIcon(imageLibrary.getImage(goodsType));
    }

    /**
     * Make image icon from an object.
     *
     * @param display The FreeColObject to find an icon for.
     * @return The <code>ImageIcon</code> found.
     */
    public ImageIcon createObjectImageIcon(FreeColObject display) {
        return (display == null) ? null
            : createImageIcon(imageLibrary.getObjectImage(display));
    }

    /**
     * Create a thumbnail for the minimap.
     * 
     * @return The created image.
     */
    public BufferedImage createMiniMapThumbNail() {
        MiniMap miniMap = new MiniMap(freeColClient);
        miniMap.setTileSize(MiniMap.MAX_TILE_SIZE);
        Game game = freeColClient.getGame();
        int width = game.getMap().getWidth() * MiniMap.MAX_TILE_SIZE
            + MiniMap.MAX_TILE_SIZE / 2;
        int height = game.getMap().getHeight() * MiniMap.MAX_TILE_SIZE / 4;
        BufferedImage image = new BufferedImage(
            width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g1 = image.createGraphics();
        miniMap.paintMap(g1);
        g1.dispose();

        // FIXME: this can probably done more efficiently by applying
        // a suitable AffineTransform to the Graphics2D
        int scaledWidth = Math.min((int)((64 * width) / (float)height), 128);
        BufferedImage scaledImage = new BufferedImage(scaledWidth, 64,
            BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = scaledImage.createGraphics();
        g2.drawImage(image, 0, 0, scaledWidth, 64, null);
        g2.dispose();
        return scaledImage;
    }

    /**
     * Tells the map controls that a chat message was received.
     *
     * @param player The player who sent the chat message.
     * @param message The chat message.
     * @param privateChat 'true' if the message is a private one, 'false'
     *            otherwise.
     * @see GUIMessage
     */
    public void displayChatMessage(Player player, String message,
                                   boolean privateChat) {
        if (canvas == null) return;
        canvas.displayChatMessage(new GUIMessage(
            player.getName() + ": " + message, player.getNationColor()));
    }

    /**
     * Refresh the GUI.
     */
    public void refresh() {
        if (mapViewer == null || canvas == null) return;
        mapViewer.forceReposition();
        canvas.refresh();
    }

    /**
     * Refreshes the screen at the specified Tile.
     *
     * @param tile The <code>Tile</code> to refresh.
     */
    public void refreshTile(Tile tile) {
        if (mapViewer == null || canvas == null) return;
        if (tile.getX() >= 0 && tile.getY() >= 0) {
            canvas.repaint(mapViewer.calculateTileBounds(tile));
        }
    }

    /**
     * Reset the menu bar.
     */
    public void resetMenuBar() {
        if (frame == null) return;
        JMenuBar menuBar = frame.getJMenuBar();
        if (menuBar != null) {
            ((FreeColMenuBar)menuBar).reset();
        }
    }

    private void resetMapZoom() {
        ResourceManager.clean();
        mapViewer.resetMapScale();
        refresh();
    }

    public boolean canZoomInMap() {
        return mapViewer != null && !mapViewer.isAtMaxMapScale();
    }

    public boolean canZoomOutMap() {
        return mapViewer != null && !mapViewer.isAtMinMapScale();
    }

    public void zoomInMap() {
        ResourceManager.clean();
        mapViewer.increaseMapScale();
        refresh();
    }

    public void zoomOutMap() {
        ResourceManager.clean();
        mapViewer.decreaseMapScale();
        refresh();
    }

    /**
     * Set the active unit.
     *
     * @param unit The <code>Unit</code> to activate.
     */
    public void setActiveUnit(Unit unit) {
        if (mapViewer == null || canvas == null) return;
        mapViewer.setActiveUnit(unit);
        updateMapControls();
        if (unit != null && !freeColClient.getMyPlayer().owns(unit)) {
            canvas.refresh();
        }
        updateMenuBar();
    }

    /**
     * Update the menu bar.
     */
    public void updateMenuBar() {
        if (frame != null && frame.getJMenuBar() != null) {
            ((FreeColMenuBar)frame.getJMenuBar()).update();
        }
    }


    // Animation handling

    /**
     * Common utility routine to retrieve animation speed.
     *
     * @param unit The <code>Unit</code> to be animated.
     * @return The animation speed.
     */
    public int getAnimationSpeed(Unit unit) {
        String key = (freeColClient.getMyPlayer() == unit.getOwner())
            ? ClientOptions.MOVE_ANIMATION_SPEED
            : ClientOptions.ENEMY_MOVE_ANIMATION_SPEED;
        return freeColClient.getClientOptions().getInteger(key);
    }

    /**
     * Animate a unit attack.
     *
     * @param attacker The attacking <code>Unit</code>.
     * @param defender The defending <code>Unit</code>.
     * @param attackerTile The <code>Tile</code> to show the attacker on.
     * @param defenderTile The <code>Tile</code> to show the defender on.
     * @param success Did the attack succeed?
     */
    public void animateUnitAttack(Unit attacker, Unit defender,
                                  Tile attackerTile, Tile defenderTile,
                                  boolean success) {
        if (canvas == null) return;
        Animations.unitAttack(this, attacker, defender,
                              attackerTile, defenderTile, success);
    }

    /**
     * Animate a unit move.
     *
     * @param unit The <code>Unit</code> that is moving.
     * @param srcTile The <code>Tile</code> the unit starts at.
     * @param dstTile The <code>Tile</code> the unit moves to.
     */
    public void animateUnitMove(Unit unit, Tile srcTile, Tile dstTile) {
        if (canvas == null) return;
        Animations.unitMove(this, unit, srcTile, dstTile);
    }


    // MapControls handling

    /**
     * Enable the map controls.
     *
     * Called from the MapControlsAction.
     *
     * @param enable If true then enable.
     */
    public void enableMapControls(boolean enable) {
        // Always instantiate in game.
        if (enable && mapControls == null) {
            String className = freeColClient.getClientOptions()
                .getString(ClientOptions.MAP_CONTROLS);
            try {
                final String panelName = "net.sf.freecol.client.gui.panel."
                    + lastPart(className, ".");
                Class<?> controls = Class.forName(panelName);
                mapControls = (MapControls)controls
                    .getConstructor(FreeColClient.class)
                    .newInstance(freeColClient);
                logger.info("Instantiated " + panelName);
            } catch (Exception e) {
                logger.log(Level.INFO, "Fallback to CornerMapControls from "
                    + className, e);
                mapControls = new CornerMapControls(freeColClient);
            }
            if (mapControls != null) {
                mapControls.addToComponent(canvas);
                mapControls.update();
            }
        } else if (!enable && mapControls != null) {
            mapControls.removeFromComponent(canvas);
            mapControls = null;
        }
    }

    public void updateMapControls() {
        if (canvas == null) return;

        if (mapControls != null) mapControls.update();
    }

    public void updateMapControlsInCanvas() {
        if (canvas == null || mapControls == null) return;

        mapControls.removeFromComponent(canvas);
        mapControls.addToComponent(canvas);
    }

    public void zoomInMapControls() {
        if (mapControls == null) return;
        mapControls.zoomIn();
    }

    public void zoomOutMapControls() {
        if (mapControls == null) return;
        mapControls.zoomOut();
    }

    public boolean canZoomInMapControls() {
        return mapControls != null && mapControls.canZoomInMapControls();
    }

    public boolean canZoomOutMapControls() {
        return mapControls != null && mapControls.canZoomOutMapControls();
    }

    public void miniMapToggleViewControls() {
        if (mapControls == null) return;
        mapControls.toggleView();
    }

    public void miniMapToggleFogOfWarControls() {
        if (mapControls == null) return;
        mapControls.toggleFogOfWar();
    }


    // Dialogs that return values

    /**
     * Simple confirmation dialog.
     *
     * @param textKey A string to use as the message key.
     * @param okKey A key for the "ok" button.
     * @param cancelKey A key for the "cancel" button.
     * @return True if the "ok" button was selected.
     */
    public boolean confirm(String textKey, String okKey, String cancelKey) {
        if (canvas == null) return false;

        return canvas.showConfirmDialog(true, null,
                                        Messages.message(textKey), null,
                                        okKey, cancelKey);
    }

    /**
     * General confirmation dialog.
     *
     * @param modal Is this a modal dialog?
     * @param tile An optional <code>Tile</code> to expose.
     * @param template The <code>StringTemplate</code> explaining the choice.
     * @param okKey A key for the "ok" button.
     * @param cancelKey A key for the "cancel" button.
     * @return True if the "ok" button was selected.
     */
    public boolean confirm(boolean modal, Tile tile,
                           StringTemplate template,
                           String okKey, String cancelKey) {
        if (canvas == null) return false;

        return canvas.showConfirmDialog(modal, tile,
                                        Utility.getDefaultTextArea(template),
                                        null, okKey, cancelKey);
    }

    /**
     * General confirmation dialog.
     *
     * @param modal Is this a modal dialog?
     * @param tile An optional <code>Tile</code> to expose.
     * @param template The <code>StringTemplate</code> explaining the choice.
     * @param obj An optional unit to make an icon for the dialog from.
     * @param okKey A key for the "ok" button.
     * @param cancelKey A key for the "cancel" button.
     * @return True if the "ok" button was selected.
     */
    public boolean confirm(boolean modal, Tile tile,
                           StringTemplate template, Unit obj,
                           String okKey, String cancelKey) {
        if (canvas == null) return false;

        return canvas.showConfirmDialog(modal, tile,
                                        Utility.getDefaultTextArea(template),
                                        createImageIcon(
                                            imageLibrary.getUnitImage(obj)),
                                        okKey, cancelKey);
    }

    /**
     * General confirmation dialog.
     *
     * @param modal Is this a modal dialog?
     * @param tile An optional <code>Tile</code> to expose.
     * @param template The <code>StringTemplate</code> explaining the choice.
     * @param icon An optional icon for the dialog.
     * @param okKey A key for the "ok" button.
     * @param cancelKey A key for the "cancel" button.
     * @return True if the "ok" button was selected.
     */
    public boolean confirm(boolean modal, Tile tile,
                           StringTemplate template, ImageIcon icon,
                           String okKey, String cancelKey) {
        if (canvas == null) return false;

        return canvas.showConfirmDialog(modal, tile,
                                        Utility.getDefaultTextArea(template),
                                        icon, okKey, cancelKey);
    }

    /**
     * Confirm that a unit should abandon its educational activity.
     *
     * @param unit The <code>Unit</code> to check.
     * @param leaveColony True if the unit is about to leave the colony,
     *     not just the education building.
     * @return True if the unit can proceed.
     */
    public boolean confirmAbandonEducation(Unit unit, boolean leaveColony) {
        if (!unit.isInColony()) return true;
        boolean teacher = unit.getStudent() != null;
        // if leaving the colony, the student loses learning spot, so
        // check with player
        boolean student = leaveColony && unit.getTeacher() != null;
        if (!teacher && !student) return true;

        Building school = (Building)((teacher) ? unit.getLocation()
            : unit.getTeacher().getLocation());
        StringTemplate label = unit.getLabel(Unit.UnitLabelType.NATIONAL);
        StringTemplate template = (leaveColony) ? StringTemplate
            .template("abandonEducation.text")
            .addStringTemplate("%unit%", label)
            .addName("%colony%", school.getColony().getName())
            .addNamed("%building%", school)
            .addStringTemplate("%action%", (teacher)
                ? StringTemplate.key("abandonEducation.action.teaching")
                : StringTemplate.key("abandonEducation.action.studying"))
            : (teacher)
            ? StringTemplate.template("abandonTeaching.text")
                .addStringTemplate("%unit%", label)
                .addNamed("%building%", school)
            : null;
        return template == null
            || confirm(true, unit.getTile(), template, unit,
                       "abandonEducation.yes", "abandonEducation.no");
    }

    /**
     * If a unit has a trade route, get confirmation that it is
     * ok to clear it and set a destination.
     *
     * @param unit The <code>Unit</code> to check.
     * @return Whether it is acceptable to set a destination for this unit.
     */
    public boolean confirmClearTradeRoute(Unit unit) {
        TradeRoute tr = unit.getTradeRoute();
        if (tr == null) return true;
        StringTemplate template = StringTemplate
            .template("clearTradeRoute.text")
            .addStringTemplate("%unit%",
                unit.getLabel(Unit.UnitLabelType.NATIONAL))
            .addName("%route%", tr.getName());
        return confirm(true, unit.getTile(), template, unit, "yes", "no");
    }

    /**
     * Confirm declaration of independence.
     *
     * @return A list of new nation and country names.
     */
    public List<String> confirmDeclaration() {
        return (canvas == null) ? Collections.<String>emptyList()
            : canvas.showConfirmDeclarationDialog();
    }

    /**
     * Confirm whether the player wants to demand tribute from a colony.
     *
     * @param attacker The potential attacking <code>Unit</code>.
     * @param colony The target <code>Colony</code>.
     * @param ns A <code>NationSummary</code> of the other nation.
     * @return The amount of tribute to demand, positive if the demand
     *     should proceed.
     */
    public int confirmEuropeanTribute(Unit attacker, Colony colony,
                                      NationSummary ns) {
        Player player = attacker.getOwner();
        Player other = colony.getOwner();
        int strength = player.calculateStrength(false);
        int otherStrength = ns.getMilitaryStrength();
        int mil = (otherStrength <= 1 || otherStrength * 5 < strength) ? 0
            : (strength == 0 || strength * 5 < otherStrength) ? 2
            : 1;

        StringTemplate t;
        int gold = ns.getGold();
        if (gold == 0) {
            t = StringTemplate.template("confirmTribute.broke")
                .addStringTemplate("%nation%", other.getNationName());
            showInformationMessage(t);
            return -1;
        }

        int fin = (gold <= 100) ? 0 : (gold <= 1000) ? 1 : 2;
        t = StringTemplate.template("confirmTribute.european")
            .addStringTemplate("%nation%", other.getNationName())
            .addStringTemplate("%danger%",
                StringTemplate.template("danger." + levels[mil]))
            .addStringTemplate("%finance%",
                StringTemplate.template("finance." + levels[fin]));
        return showSelectTributeAmountDialog(t, gold);
    }

    /**
     * Check if an attack results in a transition from peace or cease fire to
     * war and, if so, warn the player.
     *
     * @param attacker The potential attacking <code>Unit</code>.
     * @param target The target <code>Tile</code>.
     * @return True to attack, false to abort.
     */
    public boolean confirmHostileAction(Unit attacker, Tile target) {
        if (attacker.hasAbility(Ability.PIRACY)) {
            // Privateers can attack and remain at peace
            return true;
        }

        Player enemy;
        if (target.hasSettlement()) {
            enemy = target.getSettlement().getOwner();
        } else if (target == attacker.getTile()) {
            // Fortify on tile owned by another nation
            enemy = target.getOwner();
            if (enemy == null) return true;
        } else {
            Unit defender = target.getDefendingUnit(attacker);
            if (defender == null) {
                logger.warning("Attacking, but no defender - will try!");
                return true;
            }
            if (defender.hasAbility(Ability.PIRACY)) {
                // Privateers can be attacked and remain at peace
                return true;
            }
            enemy = defender.getOwner();
        }

        String messageId;
        switch (attacker.getOwner().getStance(enemy)) {
        case WAR:
            logger.finest("Player at war, no confirmation needed");
            return true;
        case CEASE_FIRE:
            messageId = "confirmHostile.ceaseFire";
            break;
        case ALLIANCE:
            messageId = "confirmHostile.alliance";
            break;
        case UNCONTACTED: case PEACE: default:
            messageId = "confirmHostile.peace";
            break;
        }
        return confirm(true, attacker.getTile(), StringTemplate
            .template(messageId)
            .addStringTemplate("%nation%", enemy.getNationName()),
            attacker, "confirmHostile.yes", "cancel");
    }

    /**
     * Confirm that a unit can leave its colony.
     * - Check for population limit.
     * - Query if education should be abandoned.
     *
     * @param unit The <code>Unit</code> that is leaving the colony.
     * @return True if the unit is allowed to leave.
     */
    public boolean confirmLeaveColony(Unit unit) {
        Colony colony = unit.getColony();
        StringTemplate message = colony.getReducePopulationMessage();
        if (message != null) {
            showInformationMessage(message);
            return false;
        }
        return confirmAbandonEducation(unit, true);
    }

    /**
     * Confirm whether the player wants to demand tribute from a native
     * settlement.
     *
     * @param attacker The potential attacking <code>Unit</code>.
     * @param is The target <code>IndianSettlement</code>.
     * @return The amount of tribute to demand, positive if the demand
     *     should proceed.
     */
    public int confirmNativeTribute(Unit attacker, IndianSettlement is) {
        Player player = attacker.getOwner();
        Player other = is.getOwner();
        int strength = player.calculateStrength(false);
        String messageId = (other.getNumberOfSettlements() >= strength)
            ? "confirmTribute.unwise"
            : (other.getStance(player) == Stance.CEASE_FIRE)
            ? "confirmTribute.warLikely"
            : (is.getAlarm(player).getLevel() == Tension.Level.HAPPY)
            ? "confirmTribute.happy"
            : "confirmTribute.normal";
        return (confirm(true, is.getTile(), StringTemplate.template(messageId)
                .addName("%settlement%", is.getName())
                .addStringTemplate("%nation%", other.getNationName()),
                attacker, "confirmTribute.yes", "confirmTribute.no"))
            ? 1 : -1;
    }

    /**
     * Shows the pre-combat dialog if enabled, allowing the user to
     * view the odds and possibly cancel the attack.
     *
     * @param attacker The attacking <code>Unit</code>.
     * @param tile The target <code>Tile</code>.
     * @return True to attack, false to abort.
     */
    public boolean confirmPreCombat(Unit attacker, Tile tile) {
        if (freeColClient.getClientOptions()
            .getBoolean(ClientOptions.SHOW_PRECOMBAT)) {
            Settlement settlement = tile.getSettlement();
            // Don't tell the player how a settlement is defended!
            FreeColGameObject defender = (settlement != null) ? settlement
                : tile.getDefendingUnit(attacker);
            return showPreCombatDialog(attacker, defender, tile);
        }
        return true;
    }

    /**
     * Confirm whether to stop the current game.
     *
     * @return True if confirmation was given.
     */
    public boolean confirmStopGame() {
        return confirm("stopCurrentGame.text",
                       "stopCurrentGame.yes", "stopCurrentGame.no");
    }

    /**
     * Get the choice of what a user wants to do with an armed unit at
     * a foreign settlement.
     *
     * @param settlement The <code>Settlement</code> to consider.
     * @return The chosen action, tribute, attack or cancel.
     */
    public ArmedUnitSettlementAction getArmedUnitSettlementChoice(Settlement settlement) {
        final Player player = freeColClient.getMyPlayer();

        List<ChoiceItem<ArmedUnitSettlementAction>> choices = new ArrayList<>();
        choices.add(new ChoiceItem<>(Messages.message("armedUnitSettlement.tribute"),
                ArmedUnitSettlementAction.SETTLEMENT_TRIBUTE));
        choices.add(new ChoiceItem<>(Messages.message("armedUnitSettlement.attack"),
                ArmedUnitSettlementAction.SETTLEMENT_ATTACK));

        return getChoice(true, settlement.getTile(),
            Utility.getDefaultTextArea(settlement.getAlarmLevelLabel(player)),
            GUI.createImageIcon(imageLibrary.getSettlementImage(settlement)),
            "cancel", choices);
    }

    /**
     * Get the user choice of whether to pay arrears for boycotted
     * goods or to dump them instead.
     *
     * @param goods The <code>Goods</code> to possibly dump.
     * @param europe The player <code>Europe</code> where the boycott
     *     is in force.
     * @return The chosen <code>BoycottAction</code>.
     */
    public BoycottAction getBoycottChoice(Goods goods, Europe europe) {
        int arrears = europe.getOwner().getArrears(goods.getType());
        StringTemplate template = StringTemplate
            .template("boycottedGoods.text")
            .addNamed("%goods%", goods)
            .addNamed("%europe%", europe)
            .addAmount("%amount%", arrears);

        List<ChoiceItem<BoycottAction>> choices = new ArrayList<>();
        choices.add(new ChoiceItem<>(Messages.message("payArrears"),
                BoycottAction.PAY_ARREARS));
        choices.add(new ChoiceItem<>(Messages.message("boycottedGoods.dumpGoods"),
                BoycottAction.DUMP_CARGO));

        return getChoice(true, null, Utility.getDefaultTextArea(template),
                         GUI.createImageIcon(imageLibrary.getImage(goods.getType())),
                         "cancel", choices);
    }

    /**
     * Gets the user choice when negotiating a purchase from a settlement.
     *
     * @param unit The <code>Unit</code> that is buying.
     * @param settlement The <code>Settlement</code> to buy from.
     * @param goods The <code>Goods</code> to buy.
     * @param gold The current negotiated price.
     * @param canBuy True if buy is a valid option.
     * @return The chosen action, buy, haggle, or cancel.
     */
    public BuyAction getBuyChoice(Unit unit, Settlement settlement,
                                  Goods goods, int gold, boolean canBuy) {
        StringTemplate template = StringTemplate.template("buy.text")
            .addStringTemplate("%nation%", settlement.getOwner().getNationName())
            .addStringTemplate("%goods%", goods.getLabel(true))
            .addAmount("%gold%", gold);

        List<ChoiceItem<BuyAction>> choices = new ArrayList<>();
        choices.add(new ChoiceItem<>(Messages.message("buy.takeOffer"),
                                     BuyAction.BUY, canBuy));
        choices.add(new ChoiceItem<>(Messages.message("buy.moreGold"),
                                     BuyAction.HAGGLE));

        return getChoice(true, unit.getTile(), Utility.getDefaultTextArea(template),
                         GUI.createImageIcon(imageLibrary.getImage(goods.getType())),
                         "cancel", choices);
    }

    /**
     * Gets the user choice for claiming a tile.
     *
     * @param tile The <code>Tile</code> to claim.
     * @param player The <code>Player</code> that is claiming.
     * @param price An asking price, if any.
     * @param owner The <code>Player</code> that owns the land.
     * @return The chosen action, accept, steal or cancel.
     */
    public ClaimAction getClaimChoice(Tile tile, Player player, int price,
                                      Player owner) {
        List<ChoiceItem<ClaimAction>> choices = new ArrayList<>();
        StringTemplate template;
        if (owner.hasContacted(player)) {
            template = StringTemplate.template("indianLand.text")
                .addStringTemplate("%player%", owner.getNationName());
            StringTemplate pay = StringTemplate.template("indianLand.pay")
                .addAmount("%amount%", price);
            choices.add(new ChoiceItem<>(Messages.message(pay),
                                         ClaimAction.ACCEPT,
                                         player.checkGold(price)));
        } else {
            template = StringTemplate.template("indianLand.unknown");
        }

        choices.add(new ChoiceItem<>(Messages.message("indianLand.take"),
                                     ClaimAction.STEAL));

        return getChoice(true, tile, Utility.getDefaultTextArea(template),
                         GUI.createImageIcon(imageLibrary.getImage(owner.getNation())),
                         "indianLand.cancel", choices);
    }

    /**
     * Get the user choice when trading with a native settlement.
     *
     * @param settlement The native settlement to trade with.
     * @param template A <code>StringTemplate</code> containing the message
     *     to display.
     * @param canBuy Show a "buy" option.
     * @param canSell Show a "sell" option.
     * @param canGift Show a "gift" option.
     * @return The chosen action, buy, sell, gift or cancel.
     */
    public TradeAction getIndianSettlementTradeChoice(Settlement settlement,
                                                      StringTemplate template,
                                                      boolean canBuy,
                                                      boolean canSell,
                                                      boolean canGift) {

        ArrayList<ChoiceItem<TradeAction>> choices = new ArrayList<>();
        if (canBuy) {
            choices.add(new ChoiceItem<>(Messages.message("tradeProposition.toBuy"),
                                         TradeAction.BUY, canBuy));
        }
        if (canSell) {
            choices.add(new ChoiceItem<>(Messages.message("tradeProposition.toSell"),
                                         TradeAction.SELL, canSell));
        }
        if (canGift) {
            choices.add(new ChoiceItem<>(Messages.message("tradeProposition.toGift"),
                                         TradeAction.GIFT, canGift));
        }
        if (choices.isEmpty()) return null;

        return getChoice(true, settlement.getTile(),
                         Utility.getDefaultTextArea(template),
                         GUI.createImageIcon(
                             imageLibrary.getSettlementImage(settlement)),
                         "cancel", choices);
    }

    /**
     * Get the user choice of what to do with a missionary at a native
     * settlement.
     *
     * @param unit The <code>Unit</code> speaking to the settlement.
     * @param settlement The <code>IndianSettlement</code> being visited.
     * @param canEstablish Is establish a valid option.
     * @param canDenounce Is denounce a valid option.
     * @return The chosen action, establish mission, denounce, incite
     *     or cancel.
     */
    public MissionaryAction getMissionaryChoice(Unit unit,
                                                IndianSettlement settlement,
                                                boolean canEstablish,
                                                boolean canDenounce) {
        StringBuilder sb = new StringBuilder(256);
        StringTemplate t = settlement.getAlarmLevelLabel(unit.getOwner());
        sb.append(Messages.message(t)).append("\n\n");
        t = StringTemplate.template("missionarySettlement.question")
            .addName("%settlement%", settlement.getName());
        sb.append(Messages.message(t));

        List<ChoiceItem<MissionaryAction>> choices = new ArrayList<>();
        if (canEstablish) {
            choices.add(new ChoiceItem<>(Messages.message("missionarySettlement.establish"),
                                         MissionaryAction.ESTABLISH_MISSION,
                                         canEstablish));
        }
        if (canDenounce) {
            choices.add(new ChoiceItem<>(Messages.message("missionarySettlement.heresy"),
                                         MissionaryAction.DENOUNCE_HERESY,
                                         canDenounce));
        }
        choices.add(new ChoiceItem<>(Messages.message("missionarySettlement.incite"),
                                     MissionaryAction.INCITE_INDIANS));

        return getChoice(true, unit.getTile(), Utility.getDefaultTextArea(sb.toString()),
                         GUI.createImageIcon(
                             imageLibrary.getSettlementImage(settlement)),
                         "cancel", choices);
    }

    /**
     * Get a name for a new colony for a player.
     *
     * @param player The <code>Player</code> to get the colony name for.
     * @param tile The <code>Tile</code> for the new colony.
     * @return A colony name, or null if the user has reconsidered.
     */
    public String getNewColonyName(Player player, Tile tile) {
        String suggested = player.getSettlementName(null);
        String name = getInput(true, tile, StringTemplate
            .template("nameColony.text"), suggested,
            "accept", "cancel");
        if (name == null) {
            // Cancelled
        } else if (name.isEmpty()) {
            showInformationMessage("enterSomeText"); // 0-length is invalid
        } else if (player.getSettlementByName(name) != null) {
            // Must be unique
            showInformationMessage(tile, StringTemplate
                .template("nameColony.notUnique")
                .addName("%name%", name));
        } else {
            return name;
        }
        player.putSettlementName(suggested);
        return null;
    }

    /**
     * Get the user choice for what to do with a scout at a foreign colony.
     *
     * @param colony The <code>Colony</code> to be scouted.
     * @param unit The <code>Unit</code> that is scouting.
     * @param neg True if negotation is a valid choice.
     * @return The selected action, either negotiate, spy, attack or cancel.
     */
    public ScoutColonyAction getScoutForeignColonyChoice(Colony colony,
                                                         Unit unit,
                                                         boolean neg) {
        StringTemplate template = StringTemplate.template("scoutColony.text")
            .addStringTemplate("%unit%", unit.getLabel(Unit.UnitLabelType.NATIONAL))
            .addName("%colony%", colony.getName());

        List<ChoiceItem<ScoutColonyAction>> choices = new ArrayList<>();
        choices.add(new ChoiceItem<>(Messages.message("scoutColony.negotiate"),
                                     ScoutColonyAction.FOREIGN_COLONY_NEGOTIATE,
                                     neg));
        choices.add(new ChoiceItem<>(Messages.message("scoutColony.spy"),
                                     ScoutColonyAction.FOREIGN_COLONY_SPY));
        choices.add(new ChoiceItem<>(Messages.message("scoutColony.attack"),
                                     ScoutColonyAction.FOREIGN_COLONY_ATTACK));

        return getChoice(true, unit.getTile(), Utility.getDefaultTextArea(template),
                         GUI.createImageIcon(
                             imageLibrary.getSettlementImage(colony)),
                         "cancel", choices);
    }

    /**
     * Get the user choice for what to do at a native settlement.
     *
     * @param settlement The <code>IndianSettlement</code> to be scouted.
     * @param numberString The number of settlements in the settlement
     *     owner nation.
     * @return The chosen action, speak, tribute, attack or cancel.
     */
    public ScoutIndianSettlementAction getScoutIndianSettlementChoice(IndianSettlement settlement,
        String numberString) {
        final Player player = freeColClient.getMyPlayer();
        final Player owner = settlement.getOwner();

        StringBuilder sb = new StringBuilder(400);
        sb.append(Messages.message(settlement.getAlarmLevelLabel(player)))
            .append("\n\n");
        String key = ((IndianNationType)owner.getNationType())
            .getSettlementTypeKey(true);
        sb.append(Messages.message(StringTemplate
                .template("scoutSettlement.greetings")
                .addStringTemplate("%nation%", owner.getNationName())
                .addName("%settlement%", settlement.getName())
                .addName("%number%", numberString)
                .add("%settlementType%", key)))
            .append(" ");
        if (settlement.getLearnableSkill() != null) {
            sb.append(Messages.message(StringTemplate
                    .template("scoutSettlement.skill")
                    .addNamed("%skill%", settlement.getLearnableSkill())))
                .append(" ");
        }
        GoodsType[] wantedGoods = settlement.getWantedGoods();
        int present = 0;
        for (; present < wantedGoods.length; present++) {
            if (wantedGoods[present] == null) break;
        }
        if (present > 0) {
            StringTemplate t = StringTemplate.template("scoutSettlement.trade."
                + Integer.toString(present));
            for (int i = 0; i < present; i++) {
                String tradeKey = "%goods" + Integer.toString(i+1) + "%";
                t.addNamed(tradeKey, wantedGoods[i]);
            }
            sb.append(Messages.message(t)).append("\n\n");
        }

        List<ChoiceItem<ScoutIndianSettlementAction>> choices
            = new ArrayList<>();
        choices.add(new ChoiceItem<>(Messages.message("scoutSettlement.speak"),
                                     ScoutIndianSettlementAction.INDIAN_SETTLEMENT_SPEAK));
        choices.add(new ChoiceItem<>(Messages.message("scoutSettlement.tribute"),
                                     ScoutIndianSettlementAction.INDIAN_SETTLEMENT_TRIBUTE));
        choices.add(new ChoiceItem<>(Messages.message("scoutSettlement.attack"),
                                     ScoutIndianSettlementAction.INDIAN_SETTLEMENT_ATTACK));

        return getChoice(true, settlement.getTile(),
                         Utility.getDefaultTextArea(sb.toString()),
                         GUI.createImageIcon(
                             imageLibrary.getSettlementImage(settlement)),
                         "cancel", choices);
    }

    /**
     * Get the user choice for negotiating a sale to a settlement.
     *
     * @param unit The <code>Unit</code> that is selling.
     * @param settlement The <code>Settlement</code> to sell to.
     * @param goods The <code>Goods</code> to sell.
     * @param gold The current negotiated price.
     * @return The chosen action, sell, gift or haggle, or null.
     */
    public SellAction getSellChoice(Unit unit, Settlement settlement,
                                    Goods goods, int gold) {
        StringTemplate goodsTemplate = goods.getLabel(true);
        StringTemplate template = StringTemplate.template("sell.text")
            .addStringTemplate("%nation%", settlement.getOwner().getNationName())
            .addStringTemplate("%goods%", goodsTemplate)
            .addAmount("%gold%", gold);

        List<ChoiceItem<SellAction>> choices = new ArrayList<>();
        choices.add(new ChoiceItem<>(Messages.message("sell.takeOffer"),
                                     SellAction.SELL));
        choices.add(new ChoiceItem<>(Messages.message("sell.moreGold"),
                                     SellAction.HAGGLE));
        choices.add(new ChoiceItem<>(Messages.message(StringTemplate
                    .template("sell.gift")
                    .addStringTemplate("%goods%", goodsTemplate)),
                SellAction.GIFT));

        return getChoice(true, unit.getTile(), Utility.getDefaultTextArea(template),
                         GUI.createImageIcon(imageLibrary.getImage(goods.getType())),
                         "cancel", choices);
    }


    /**
     * General choice dialog.
     *
     * @param modal Is this a modal dialog?
     * @param tile An optional <code>Tile</code> to expose.
     * @param explain An object explaining the choice.
     * @param icon An optional icon for the dialog.
     * @param cancelKey A key for the "cancel" button.
     * @param choices A list a <code>ChoiceItem</code>s to choose from.
     * @return The selected value of the selected <code>ChoiceItem</code>,
     *     or null if cancelled.
     */
    public <T> T getChoice(boolean modal, Tile tile, Object explain,
                           ImageIcon icon,
                           String cancelKey, List<ChoiceItem<T>> choices) {
        if (canvas == null) return null;

        return canvas.showChoiceDialog(modal, tile, explain, icon,
                                       cancelKey, choices);
    }

    /**
     * General input dialog.
     *
     * @param modal Is this a modal dialog?
     * @param tile An optional <code>Tile</code> to expose.
     * @param template A <code>StringTemplate</code> explaining the choice.
     * @param defaultValue The default value to show initially.
     * @param okKey A key for the "ok" button.
     * @param cancelKey A key for the "cancel" button.
     * @return The chosen value.
     */
    public String getInput(boolean modal, Tile tile, StringTemplate template,
                           String defaultValue, String okKey, String cancelKey) {
        if (canvas == null) return null;

        return canvas.showInputDialog(modal, tile, template, defaultValue,
                                      okKey, cancelKey);
    }


    // Trivial delegations to Canvas

    public void closeMainPanel() {
        if (canvas == null) return;
        canvas.closeMainPanel();
    }

    public void closeMenus() {
        if (canvas == null) return;
        canvas.closeMenus();
    }

    public void closeStatusPanel() {
        if (canvas == null) return;
        canvas.closeStatusPanel();
    }

    public boolean containsInGameComponents() {
        if (canvas == null) return false;
        return canvas.containsInGameComponents();
    }

    public void displayChat(String senderName, String message,
                            boolean privateChat) {
        if (canvas == null) return;
        canvas.displayChat(senderName, message, privateChat);
    }

    /**
     * Show the appropriate panel for an object.
     *
     * TODO: Improve OO.
     *
     * @param fco The <code>FreeColObject</code> to display.
     */
    public void displayObject(FreeColObject fco) {
        if (canvas == null) return;
        if (fco instanceof Colony) {
            canvas.showColonyPanel((Colony)fco, null);
        } else if (fco instanceof Europe) {
            canvas.showEuropePanel();
        } else if (fco instanceof IndianSettlement) {
            canvas.showIndianSettlementPanel((IndianSettlement)fco);
        } else if (fco instanceof Tile) {
            setFocus((Tile)fco);
        } else if (fco instanceof Unit) {
            displayObject((FreeColObject)(((Unit)fco).getLocation()));
        } else if (fco instanceof WorkLocation) {
            displayObject(((WorkLocation)fco).getColony());
        }
    }

    public LoadingSavegameDialog getLoadingSavegameDialog() {
        if (canvas == null) return null;
        return canvas.getLoadingSavegameDialog();
    }

    public boolean isClientOptionsDialogShowing() {
        return canvas != null && canvas.isClientOptionsDialogShowing();
    }

    public boolean isMapboardActionsEnabled() {
        return canvas != null && canvas.isMapboardActionsEnabled();
    }

    public boolean isShowingSubPanel() {
        if (canvas == null) return false;
        return canvas.isShowingSubPanel();
    }

    public void paintImmediatelyCanvasIn(Rectangle rectangle) {
        if (canvas == null) return;
        canvas.paintImmediately(rectangle);
    }

    public void paintImmediatelyCanvasInItsBounds() {
        if (canvas == null) return;
        canvas.paintImmediately(canvas.getBounds());
    }

    public void refreshPlayersTable() {
        if (canvas == null) return;
        canvas.refreshPlayersTable();
    }

    public void removeFromCanvas(Component component) {
        if (canvas == null) return;
        canvas.remove(component);
    }

    public void removeInGameComponents() {
        if (canvas == null) return;
        canvas.removeInGameComponents();
    }

    public void removeTradeRoutePanel(TradeRoutePanel panel) {
        if (canvas == null) return;
        canvas.removeTradeRoutePanel(panel);
    }

    public void requestFocusForSubPanel() {
        if (canvas == null) return;
        canvas.getShowingSubPanel().requestFocus();
    }

    public boolean requestFocusInWindow() {
        if (canvas == null) return false;
        return canvas.requestFocusInWindow();
    }

    public void restoreSavedSize(Component comp, int w, int h) {
        if (canvas == null) return;
        canvas.restoreSavedSize(comp, new Dimension(w, h));
    }

    public void restoreSavedSize(Component comp, Dimension size) {
        if (canvas == null) return;
        canvas.restoreSavedSize(comp, size);
    }

    public void returnToTitle() {
        if (canvas == null) return;
        canvas.returnToTitle();
        freeColClient.getSoundController().playSound("sound.intro.general");
    }

    public void showAboutPanel() {
        if (canvas == null) return;
        canvas.showAboutPanel();
    }

    public void showBuildQueuePanel(Colony colony) {
        if (canvas == null) return;
        canvas.showBuildQueuePanel(colony);
    }

    public void showBuildQueuePanel(Colony colony, Runnable callBack) {
        if (canvas == null) return;
        canvas.showBuildQueuePanel(colony, callBack);
    }

    public void showCaptureGoodsDialog(final Unit unit, List<Goods> gl,
                                       final String defenderId) {
        if (canvas == null) return;
        canvas.showCaptureGoodsDialog(unit, gl,
            new DialogHandler<List<Goods>>() {
                @Override
                public void handle(List<Goods> gl) {
                    igc().lootCargo(unit, gl, defenderId);
                }
            });
    }

    public void showChatPanel() {
        if (canvas == null) return;
        canvas.showChatPanel();
    }

    public void showChooseFoundingFatherDialog(final List<FoundingFather> ffs) {
        if (canvas == null) return;
        canvas.showChooseFoundingFatherDialog(ffs,
            new DialogHandler<FoundingFather>() {
                @Override
                public void handle(FoundingFather ff) {
                    igc().chooseFoundingFather(ffs, ff);
                }
            });
    }

    public OptionGroup showClientOptionsDialog() {
        if (canvas == null) return null;
        OptionGroup group = canvas.showClientOptionsDialog();
        if (!freeColClient.isInGame()) showMainPanel(null);
        return group;
    }

    public ColonyPanel showColonyPanel(Colony colony, Unit unit) {
        if (canvas == null) return null;
        return canvas.showColonyPanel(colony, unit);
    }

    public void showColopediaPanel(String nodeId) {
        if (canvas == null) return;
        canvas.showColopediaPanel(nodeId);
    }

    public ColorChooserPanel showColorChooserPanel(ActionListener al) {
        if (canvas == null) return null;
        return canvas.showColorChooserPanel(al);
    }

    public void showCompactLabourReport() {
        if (canvas == null) return;
        canvas.showCompactLabourReport();
    }

    public void showCompactLabourReport(UnitData unitData) {
        if (canvas == null) return;
        canvas.showCompactLabourReport(unitData);
    }

    public void showDeclarationPanel() {
        if (canvas == null) return;
        canvas.showDeclarationPanel();
    }

    public OptionGroup showDifficultyDialog() {
        if (canvas == null) return null;
        return canvas.showDifficultyDialog();
    }

    public OptionGroup showDifficultyDialog(Specification spec,
                                            OptionGroup group) {
        if (canvas == null) return null;
        return canvas.showDifficultyDialog(spec, group);
    }

    public void showDumpCargoDialog(Unit unit) {
        if (canvas == null) return;
        canvas.showDumpCargoDialog(unit,
            new DialogHandler<List<Goods>>() {
                @Override
                public void handle(List<Goods> goodsList) {
                    for (Goods g : goodsList) igc().unloadCargo(g, true);
                }
            });
    }

    public boolean showEditOptionDialog(Option option) {
        if (canvas == null) return false;
        return canvas.showEditOptionDialog(option);
    }

    public void showEmigrationDialog(final Player player, final int n,
                                     final boolean fountainOfYouth) {
        if (canvas == null) return;
        canvas.showEmigrationDialog(player, fountainOfYouth,
            new DialogHandler<Integer>() {
                @Override
                public void handle(Integer value) {
                    // Value should be a valid slot
                    igc().emigrate(player,
                        Europe.MigrationType.convertToMigrantSlot(value));
                    if (n > 1) {
                        showEmigrationDialog(player, n-1, fountainOfYouth);
                    }
                }
            });
    }

    public void showEndTurnDialog(final List<Unit> units) {
        if (canvas == null) return;
        canvas.showEndTurnDialog(units,
            new DialogHandler<Boolean>() {
                @Override
                public void handle(Boolean value) {
                    if (value != null && value) {
                        igc().endTurn(false);
                    }
                }
            });
    }

    public void showErrorMessage(StringTemplate template) {
        if (canvas == null) return;
        canvas.showErrorMessage(Messages.message(template));
    }

    public void showErrorMessage(String messageId) {
        if (canvas == null) return;
        canvas.showErrorMessage(messageId);
    }

    public void showErrorMessage(String messageID, String message) {
        if (canvas == null) return;
        canvas.showErrorMessage(messageID, message);
    }

    public void showEuropePanel() {
        if (canvas == null) return;
        canvas.showEuropePanel();
    }

    public void showEventPanel(String header, String image, String footer) {
        if (canvas == null) return;
        canvas.showEventPanel(header, image, footer);
    }

    public void showFindSettlementPanel() {
        if (canvas == null) return;
        canvas.showFindSettlementPanel();
    }

    public OptionGroup showGameOptionsDialog(boolean editable, boolean custom) {
        if (canvas == null) return null;
        return canvas.showGameOptionsDialog(editable, custom);
    }

    public void showHighScoresPanel(String messageId, List<HighScore> scores) {
        if (canvas == null) return;
        canvas.showHighScoresPanel(messageId, scores);
    }

    public void showIndianSettlementPanel(IndianSettlement indianSettlement) {
        if (canvas == null) return;
        canvas.showIndianSettlementPanel(indianSettlement);
    }

    public void showInformationMessage(String messageId) {
        alertSound();
        if (canvas == null) return;
        canvas.showInformationMessage(messageId);
    }

    public void showInformationMessage(StringTemplate template) {
        alertSound();
        if (canvas == null) return;
        canvas.showInformationMessage(template);
    }

    public void showInformationMessage(ModelMessage message) {
        alertSound();
        if (canvas == null) return;
        canvas.showInformationMessage(message);
    }

    public void showInformationMessage(Settlement displayObject,
                                       String messageId) {
        showInformationMessage(displayObject, StringTemplate.key(messageId));
    }

    public void showInformationMessage(Settlement displayObject,
                                       StringTemplate template) {
        alertSound();
        if (canvas == null) return;
        ImageIcon icon = null;
        Tile tile = null;
        if(displayObject != null) {
            icon = createImageIcon(imageLibrary.getSettlementImage(displayObject));
            tile = displayObject.getTile();
        }
        canvas.showInformationMessage(displayObject, tile, icon, template);
    }

    public void showInformationMessage(Unit displayObject,
                                       String messageId) {
        showInformationMessage(displayObject, StringTemplate.key(messageId));
    }

    public void showInformationMessage(Unit displayObject,
                                       StringTemplate template) {
        alertSound();
        if (canvas == null) return;
        ImageIcon icon = null;
        Tile tile = null;
        if(displayObject != null) {
            icon = createImageIcon(imageLibrary.getUnitImage(displayObject));
            tile = displayObject.getTile();
        }
        canvas.showInformationMessage(displayObject, tile, icon, template);
    }

    public void showInformationMessage(Tile displayObject,
                                       String messageId) {
        showInformationMessage(displayObject, StringTemplate.key(messageId));
    }

    public void showInformationMessage(Tile displayObject,
                                       StringTemplate template) {
        alertSound();
        if (canvas == null) return;
        canvas.showInformationMessage(displayObject, displayObject, null, template);
    }

    public void showInformationMessage(FreeColObject displayObject,
                                       String messageId) {
        alertSound();
        if (canvas == null) return;
        canvas.showInformationMessage(displayObject, messageId);
    }

    public void showInformationMessage(FreeColObject displayObject,
                                       StringTemplate template) {
        alertSound();
        if (canvas == null) return;
        canvas.showInformationMessage(displayObject, template);
    }

    public File showLoadDialog(File directory) {
        if (canvas == null) return null;
        return canvas.showLoadDialog(directory);
    }

    public File showLoadDialog(File directory, FileFilter[] fileFilters) {
        if (canvas == null) return null;
        return canvas.showLoadDialog(directory, fileFilters);
    }

    public File showLoadSaveFileDialog() {
        File file = showLoadDialog(FreeColDirectories.getSaveDirectory());
        if (file != null && !file.isFile()) {
            showErrorMessage("error.noSuchFile");
            file = null;
        }
        return file;
    }
            
    public boolean showLoadingSavegameDialog(boolean publicServer,
                                             boolean singlePlayer) {
        if (canvas == null) return false;
        return canvas.showLoadingSavegameDialog(publicServer, singlePlayer);
    }

    public void showLogFilePanel() {
        if (canvas == null) return;
        canvas.showLogFilePanel();
    }

    public void showMainPanel(String userMsg) {
        if (canvas == null) return;
        canvas.showMainPanel(userMsg);
    }

    public OptionGroup showMapGeneratorOptionsDialog(boolean editable) {
        if (canvas == null) return null;
        return canvas.showMapGeneratorOptionsDialog(editable);
    }

    public Dimension showMapSizeDialog() {
        if (canvas == null) return null;
        return canvas.showMapSizeDialog();
    }

    public void showModelMessages(List<ModelMessage> modelMessages) {
        if (canvas == null) return;
        canvas.showModelMessages(modelMessages);
    }

    public void showMonarchDialog(final MonarchAction action,
                                  StringTemplate template, String monarchKey) {
        if (canvas == null) return;
        canvas.showMonarchDialog(action, template, monarchKey,
            new DialogHandler<Boolean>() {
                @Override
                public void handle(Boolean b) {
                    igc().monarchAction(action, b);
                    updateMenuBar();
                }
            });
    }

    public void showNameNewLandDialog(String key, final String defaultName,
                                      final Unit unit) {
        if (canvas == null) return;
        canvas.showNameNewLandDialog(key, defaultName, unit,
            new DialogHandler<String>() {
                @Override
                public void handle(String name) {
                    if (name == null || name.isEmpty()) name = defaultName;
                    igc().nameNewLand(unit, name);
                }
            });
    }

    public void showNameNewRegionDialog(StringTemplate template,
                                        final String defaultName,
                                        final Unit unit, final Tile tile,
                                        final Region region) {
        if (canvas == null) return;
        canvas.showNameNewRegionDialog(template, defaultName, unit,
            new DialogHandler<String>() {
                @Override
                public void handle(String name) {
                    if (name == null || name.isEmpty()) name = defaultName;
                    igc().nameNewRegion(tile, unit, region, name);
                }
            });
    }

    public void showFirstContactDialog(final Player player, final Player other,
                                       final Tile tile, int settlementCount) {
        if (canvas == null) return;
        canvas.showFirstContactDialog(player, other, tile, settlementCount,
            new DialogHandler<Boolean>() {
                @Override
                public void handle(Boolean b) {
                    igc().firstContact(player, other, tile, b);
                }
            });
    }

    public DiplomaticTrade showDiplomaticTradeDialog(FreeColGameObject our,
                                                     FreeColGameObject other,
                                                     DiplomaticTrade agreement,
                                                     StringTemplate comment) {
        if (canvas == null) return null;
        return canvas.showDiplomaticTradeDialog(our, other, agreement, comment);
    }

    public void showNewPanel() {
        if (canvas == null) return;
        canvas.showNewPanel();
    }

    public void showNewPanel(Specification specification) {
        if (canvas == null) return;
        canvas.showNewPanel(specification);
    }

    public void showSpyColonyPanel(final Tile tile, Runnable callback) {
        if (canvas == null) return;
        ColonyPanel panel = canvas.showSpyColonyPanel(tile);
        panel.addClosingCallback(callback);
    }

    public Parameters showParametersDialog() {
        if (canvas == null) return null;
        return canvas.showParametersDialog();
    }

    public boolean showPreCombatDialog(Unit attacker,
                                       FreeColGameObject defender, Tile tile) {
        if (canvas == null) return false;
        return canvas.showPreCombatDialog(attacker, defender, tile);
    }

    public void showPurchasePanel() {
        if (canvas == null) return;
        canvas.showPurchasePanel();
    }

    public void showRecruitPanel() {
        if (canvas == null) return;
        canvas.showRecruitPanel();
    }

    public void showReportCargoPanel() {
        if (canvas == null) return;
        canvas.showReportCargoPanel();
    }

    public void showReportColonyPanel() {
        if (canvas == null) return;
        canvas.showReportColonyPanel();
    }

    public void showReportContinentalCongressPanel() {
        if (canvas == null) return;
        canvas.showReportContinentalCongressPanel();
    }

    public void showReportEducationPanel() {
        if (canvas == null) return;
        canvas.showReportEducationPanel();
    }

    public void showReportExplorationPanel() {
        if (canvas == null) return;
        canvas.showReportExplorationPanel();
    }

    public void showReportForeignAffairPanel() {
        if (canvas == null) return;
        canvas.showReportForeignAffairPanel();
    }

    public void showReportHistoryPanel() {
        if (canvas == null) return;
        canvas.showReportHistoryPanel();
    }

    public void showReportIndianPanel() {
        if (canvas == null) return;
        canvas.showReportIndianPanel();
    }

    public void showReportLabourDetailPanel(UnitType unitType,
            Map<UnitType, Map<Location, Integer>> data,
            TypeCountMap<UnitType> unitCount, List<Colony> colonies) {
        if (canvas == null) return;
        canvas.showReportLabourDetailPanel(unitType, data, unitCount,
                                           colonies);
    }

    public void showReportLabourPanel() {
        if (canvas == null) return;
        canvas.showReportLabourPanel();
    }

    public void showReportMilitaryPanel() {
        if (canvas == null) return;
        canvas.showReportMilitaryPanel();
    }

    public void showReportNavalPanel() {
        if (canvas == null) return;
        canvas.showReportNavalPanel();
    }

    public void showReportProductionPanel() {
        if (canvas == null) return;
        canvas.showReportProductionPanel();
    }

    public void showReportReligiousPanel() {
        if (canvas == null) return;
        canvas.showReportReligiousPanel();
    }

    public void showReportRequirementsPanel() {
        if (canvas == null) return;
        canvas.showReportRequirementsPanel();
    }

    public void showReportTradePanel() {
        if (canvas == null) return;
        canvas.showReportTradePanel();
    }

    public void showReportTurnPanel(List<ModelMessage> messages) {
        if (canvas == null) return;
        canvas.showReportTurnPanel(messages);
    }

    public File showSaveDialog(File directory, String defaultName) {
        if (canvas == null) return null;
        return canvas.showSaveDialog(directory, defaultName);
    }

    public File showSaveDialog(File directory, FileFilter[] fileFilters,
                               String defaultName, String extension) {
        if (canvas == null) return null;
        return canvas.showSaveDialog(directory, fileFilters, defaultName,
                                     extension);
    }

    public Dimension showScaleMapSizeDialog() {
        if (canvas == null) return null;
        return canvas.showScaleMapSizeDialog();
    }

    public int showSelectAmountDialog(GoodsType goodsType, int available,
                                      int defaultAmount, boolean needToPay) {
        if (canvas == null) return -1;
        return canvas.showSelectAmountDialog(goodsType, available,
                                             defaultAmount, needToPay);
    }

    public int showSelectTributeAmountDialog(StringTemplate question,
                                             int maximum) {
        if (canvas == null) return -1;
        return canvas.showSelectTributeAmountDialog(question, maximum);
    }

    public Location showSelectDestinationDialog(Unit unit) {
        if (canvas == null) return null;
        return canvas.showSelectDestinationDialog(unit);
    }

    public void showServerListPanel(List<ServerInfo> serverList) {
        if (canvas == null) return;
        canvas.showServerListPanel(serverList);
    }

    public void showStartGamePanel(Game game, Player player,
                                   boolean singlePlayerMode) {
        if (canvas == null) return;
        canvas.showStartGamePanel(game, player, singlePlayerMode);
    }

    public void showStatisticsPanel() {
        if (canvas == null) return;
        canvas.showStatisticsPanel();
    }

    public void showStatusPanel(String message) {
        if (canvas == null) return;
        canvas.showStatusPanel(message);
    }

    public void showTilePanel(Tile tile) {
        if (canvas == null) return;
        canvas.showTilePanel(tile);
    }

    public void showTilePopUpAtSelectedTile() {
        if (canvas == null || mapViewer == null) return;
        TerrainCursor cursor = mapViewer.getCursor();
        canvas.showTilePopup(mapViewer.getSelectedTile(),
            cursor.getCanvasX(), cursor.getCanvasY());
    }

    public void showTradeRoutePanel(Unit unit) {
        if (canvas == null) return;
        canvas.showTradeRoutePanel(unit);
    }

    public void showTradeRouteInputPanel(TradeRoute newRoute,
                                         Runnable callBack) {
        if (canvas == null) return;
        canvas.showTradeRouteInputPanel(newRoute, callBack);
    }

    public void showTrainPanel() {
        if (canvas == null) return;
        canvas.showTrainPanel();
    }

    public void showVictoryDialog() {
        if (canvas == null) return;
        canvas.showVictoryDialog(new DialogHandler<Boolean>() {
                @Override
                public void handle(Boolean result) {
                    igc().victory(result);
                }
            });
    }

    public boolean showWarehouseDialog(Colony colony) {
        if (canvas == null) return false;
        return canvas.showWarehouseDialog(colony);
    }

    public void showWorkProductionPanel(Unit unit) {
        if (canvas == null) return;
        canvas.showWorkProductionPanel(unit);
    }

    public void updateEuropeanSubpanels() {
        if (canvas == null) return;
        canvas.updateEuropeanSubpanels();
    }

    public void updateGameOptions() {
        if (canvas == null) return;
        canvas.updateGameOptions();
    }

    public void updateMapGeneratorOptions() {
        if (canvas == null) return;
        canvas.updateMapGeneratorOptions();
    }

    // Trivial delegations to MapViewer

    public void centerActiveUnit() {
        if (mapViewer == null) return;
        mapViewer.centerActiveUnit();
    }

    public void changeViewMode(int newViewMode) {
        if (mapViewer == null) return;
        mapViewer.changeViewMode(newViewMode);
    }

    public void executeWithUnitOutForAnimation(final Unit unit,
                                               final Tile sourceTile,
                                               final OutForAnimationCallback r) {
        if (mapViewer == null) return;
        mapViewer.executeWithUnitOutForAnimation(unit, sourceTile, r);
    }

    public Unit getActiveUnit() {
        if (mapViewer == null) return null;
        return mapViewer.getActiveUnit();
    }

    public Tile getFocus() {
        if (mapViewer == null) return null;
        return mapViewer.getFocus();
    }

    public float getMapScale() {
        if (mapViewer == null) return 1.0f;
        return mapViewer.getMapScale();
    }

    public Tile getSelectedTile() {
        if (mapViewer == null) return null;
        return mapViewer.getSelectedTile();
    }

    public Rectangle getTileBounds(Tile tile) {
        if (mapViewer == null) return null;
        return mapViewer.calculateTileBounds(tile);
    }

    public Point getTilePosition(Tile tile) {
        if (mapViewer == null) return null;
        return mapViewer.calculateTilePosition(tile);
    }

    public int getViewMode() {
        if (mapViewer == null) return -1;
        return mapViewer.getViewMode();
    }

    public boolean onScreen(Tile tileToCheck) {
        if (mapViewer == null) return true; // Lets pretend.
        return mapViewer.onScreen(tileToCheck);
    }

    public void setFocus(Tile tileToFocus) {
        if (mapViewer == null) return;
        mapViewer.setFocus(tileToFocus);
    }

    public void setFocusImmediately(Tile tileToFocus) {
        if (mapViewer == null) return;
        mapViewer.setFocusImmediately(tileToFocus);
    }

    public boolean setSelectedTile(Tile newTileToSelect,
                                   boolean clearGoToOrders) {
        if (mapViewer == null) return true; // Pretending again.
        return mapViewer.setSelectedTile(newTileToSelect, clearGoToOrders);
    }

    public void toggleViewMode() {
        if (mapViewer == null) return;
        mapViewer.toggleViewMode();
    }


    // Forwarding to SoundController, only for gui classes in need of sound

    /**
     * Play a sound.
     *
     * @param sound The sound resource to play, or if null stop playing.
     */
    public void playSound(String sound) {
        freeColClient.getSoundController().playSound(sound);
    }

    /**
     * Plays an alert sound for an information message if the
     * option for it is turned on.
     */
    private void alertSound() {
        if (freeColClient.getClientOptions()
            .getBoolean(ClientOptions.AUDIO_ALERTS)) {
            freeColClient.getSoundController()
                .playSound("sound.event.alertSound");
        }
    }

    /**
     * Get the label text for the sound player mixer.
     *
     * Needed by the audio mixer option UI.
     *
     * @return The text.
     */
    public String getSoundMixerLabelText() {
        return freeColClient.getSoundController().getSoundMixerLabelText();
    }

}
