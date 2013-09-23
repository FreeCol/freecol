/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.AttributeSet;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas.BoycottAction;
import net.sf.freecol.client.gui.Canvas.BuyAction;
import net.sf.freecol.client.gui.Canvas.ClaimAction;
import net.sf.freecol.client.gui.Canvas.EventType;
import net.sf.freecol.client.gui.Canvas.MissionaryAction;
import net.sf.freecol.client.gui.Canvas.ScoutColonyAction;
import net.sf.freecol.client.gui.Canvas.ScoutIndianSettlementAction;
import net.sf.freecol.client.gui.Canvas.SellAction;
import net.sf.freecol.client.gui.Canvas.TradeAction;
import net.sf.freecol.client.gui.animation.Animations;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.menu.FreeColMenuBar;
import net.sf.freecol.client.gui.menu.InGameMenuBar;
import net.sf.freecol.client.gui.menu.MapEditorMenuBar;
import net.sf.freecol.client.gui.panel.ChoiceItem;
import net.sf.freecol.client.gui.panel.ColonyPanel;
import net.sf.freecol.client.gui.panel.CornerMapControls;
import net.sf.freecol.client.gui.panel.EuropePanel;
import net.sf.freecol.client.gui.panel.MiniMap;
import net.sf.freecol.client.gui.panel.LabourData.UnitData;
import net.sf.freecol.client.gui.panel.LoadingSavegameDialog;
import net.sf.freecol.client.gui.panel.MapControls;
import net.sf.freecol.client.gui.panel.Parameters;
import net.sf.freecol.client.gui.sound.SoundPlayer;
import net.sf.freecol.common.ServerInfo;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Monarch.MonarchAction;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.TypeCountMap;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.option.AudioMixerOption;
import net.sf.freecol.common.option.BooleanOption;
import net.sf.freecol.common.option.LanguageOption;
import net.sf.freecol.common.option.LanguageOption.Language;
import net.sf.freecol.common.option.Option;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.common.option.PercentageOption;
import net.sf.freecol.common.resources.ResourceManager;


/**
 * A wrapper for the overall GUI.
 */
public class GUI {

    private static final Logger logger = Logger.getLogger(GUI.class.getName());

    /** How many columns (em-widths) to use in the text area. */
    private static final int DEFAULT_TEXT_COLUMNS = 20;

    /** The space not being used in windowed mode. */
    private static final int DEFAULT_SCREEN_INSET_WIDTH  = 0;
    private static final int DEFAULT_SCREEN_INSET_HEIGHT = 32;
    private static final int DEFAULT_WINDOW_INSET_WIDTH  = 6;
    private static final int DEFAULT_WINDOW_INSET_HEIGHT = 30;

    /** View modes. */
    public static final int MOVE_UNITS_MODE = 0;
    public static final int VIEW_TERRAIN_MODE = 1;

    /** The color to use for borders. */
    public static final Color BORDER_COLOR
        = ResourceManager.getColor("lookAndFeel.border.color");

    /** The color to use for links. */
    public static final Color LINK_COLOR
        = ResourceManager.getColor("lookAndFeel.link.color");

    /** The color to use for things the player probably should not do. */
    public static final Color WARNING_COLOR
        = ResourceManager.getColor("lookAndFeel.warning.color");

    /** Font to use for text areas. */
    public static final Font DEFAULT_FONT
        = ResourceManager.getFont("NormalFont", 13f);
    /** Bold version of the default font. */
    public static final Font DEFAULT_BOLD_FONT
        = DEFAULT_FONT.deriveFont(Font.BOLD);
    /** Header fonts for reports etc. */
    public static final Font SMALL_HEADER_FONT
        = ResourceManager.getFont("HeaderFont", 24f);
    public static final Font MEDIUM_HEADER_FONT
        = ResourceManager.getFont("HeaderFont", 36f);
    public static final Font BIG_HEADER_FONT
        = ResourceManager.getFont("HeaderFont", 48f);

    /** The margin to use for a link button. */
    public static final Insets EMPTY_MARGIN = new Insets(0, 0, 0, 0);

    /** A style context to use for panels and dialogs. */
    public static StyleContext STYLE_CONTEXT = new StyleContext();
    static {
        Style defaultStyle = StyleContext.getDefaultStyleContext()
            .getStyle(StyleContext.DEFAULT_STYLE);

        Style regular = STYLE_CONTEXT.addStyle("regular", defaultStyle);
        StyleConstants.setFontFamily(regular, "NormalFont");
        StyleConstants.setFontSize(regular, 13);

        Style buttonStyle = STYLE_CONTEXT.addStyle("button", regular);
        StyleConstants.setForeground(buttonStyle, LINK_COLOR);

        Style right = STYLE_CONTEXT.addStyle("right", regular);
        StyleConstants.setAlignment(right, StyleConstants.ALIGN_RIGHT);
    }



    /** The client for the game. */
    private FreeColClient freeColClient;

    /** The canvas that implements much of the functionality. */
    private Canvas canvas;

    /**
     * This is the MapViewer instance used to paint the colony tiles
     * in the ColonyPanel and other panels.  It should not be scaled
     * along with the default MapViewer.
     */
    private MapViewer colonyTileGUI;

    /** The parent frame, either a window or the full screen. */
    private FreeColFrame frame;

    /** The graphics device for the screen. */
    private GraphicsDevice gd;

    /** An image library to use. */
    private ImageLibrary imageLibrary;

    /**
     * The MapViewer instance used to paint the main map.
     * This does need to be scaled.
     */
    private MapViewer mapViewer;

    private MapControls mapControls;

    private SoundPlayer soundPlayer;

    private JWindow splash;

    private boolean windowed;

    private Rectangle windowBounds;


    /**
     * Create the GUI.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public GUI(FreeColClient freeColClient) {
        this.freeColClient = freeColClient;
        this.imageLibrary = new ImageLibrary();
    }


    // Simple accessors

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

    /**
     * Can this client play sounds?
     *
     * @return True if there is a sound player present.
     */
    public boolean canPlaySound() {
        return soundPlayer != null;
    }

    /**
     * Get the sound player.
     * Needed for access to the mixer by {@link #AudioMixerOptionUI}.
     *
     * @return The current <code>SoundPlayer</code>.
     */
    public SoundPlayer getSoundPlayer() {
        return soundPlayer;
    }

    public boolean isWindowed() {
        return windowed;
    }

    public void setWindowed(boolean windowed) {
        this.windowed = windowed;
    }

    public Rectangle getWindowBounds() {
        return windowBounds;
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
     * Change the windowed mode.
     *
     * @param windowed Use <code>true</code> for windowed mode
     *     and <code>false</code> for fullscreen mode.
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

        this.frame = FreeColFrame.createFreeColFrame(freeColClient, canvas, gd,
                                                     windowed);
        frame.setJMenuBar(menuBar);
        frame.setCanvas(canvas);
        frame.updateBounds(getWindowBounds());

        mapViewer.forceReposition();
        canvas.updateSizes();
        frame.setVisible(true);
    }

    /**
     * Create a thumbnail for the minimap.
     */
    public BufferedImage createMiniMapThumbNail() {
        MiniMap miniMap = new MiniMap(freeColClient);
        miniMap.setTileSize(MiniMap.MAX_TILE_SIZE);
        int width = freeColClient.getGame().getMap().getWidth()
            * MiniMap.MAX_TILE_SIZE + MiniMap.MAX_TILE_SIZE/2;
        int height = freeColClient.getGame().getMap().getHeight()
            * MiniMap.MAX_TILE_SIZE / 4;
        BufferedImage image = new BufferedImage(width, height,
                                                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        miniMap.paintMap(g2d);

        // TODO: this can probably done more efficiently
        // by applying a suitable AffineTransform to the
        // Graphics2D
        double scaledWidth = Math.min((64 * width) / height, 128);
        BufferedImage scaledImage = new BufferedImage((int) scaledWidth, 64,
            BufferedImage.TYPE_INT_ARGB);
        scaledImage.createGraphics().drawImage(image, 0, 0,
            (int)scaledWidth, 64, null);
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
        if (mapViewer == null || canvas == null) return;
        mapViewer.addMessage(new GUIMessage(player.getName() + ": " + message,
                                            player.getNationColor()));
        canvas.repaint(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    /**
     * Delegate the colony tile display to the colony tile GUI.
     *
     * @param g The <code>Graphics</code> to display on.
     * @param tile The <code>Tile</code> to display.
     * @param colony The <code>Colony</code> using the tile.
     */
    public void displayColonyTile(Graphics2D g, Tile tile, Colony colony) {
        if (colonyTileGUI == null) return;
        colonyTileGUI.displayColonyTile(g, tile, colony);
    }

    /**
     * Display the splash screen.
     *
     * @param splashFilename The name of the file to find the image in.
     */
    public void displaySplashScreen(final String splashFilename) {
        splash = null;
        if (splashFilename == null) return;
        try {
            Image im = Toolkit.getDefaultToolkit().getImage(splashFilename);
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

    /**
     * Simple delegation of image icon retrieval.
     *
     * @param display The object to find an icon for.
     * @param small Choose a small icon?
     * @return The <code>ImageIcon</code> found.
     */
    public ImageIcon getImageIcon(Object display, boolean small) {
        return imageLibrary.getImageIcon(display, small);
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
     * Plays some sound. Parameter == null stops playing a sound.
     *
     * @param sound The sound resource to play or <b>null</b>
     */
    public void playSound(String sound) {
        if (!canPlaySound()) return;
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

    /**
     * Quit the GUI.  All that is required is to exit the full screen.
     */
    public void quit() throws Exception {
        if (gd == null) return;
        if (!isWindowed()) gd.setFullScreenWindow(null);
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
            canvas.repaint(mapViewer.getTileBounds(tile));
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

    /**
     * Scale the map.
     *
     * @param scale The scale factor to apply.
     */
    public void scaleMap(float scale) {
        if (mapViewer == null) return;
        mapViewer.scaleMap(scale);
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
        if (unit != null
            && !freeColClient.getMyPlayer().owns(unit)) {
            canvas.repaint(0, 0, canvas.getWidth(), canvas.getHeight());
        }
    }

    /**
     * Set up the menu bar once in game.
     */
    public void setupInGameMenuBar() {
        if (frame == null || canvas == null) return;
        frame.setJMenuBar(new InGameMenuBar(freeColClient));
        frame.paintAll(canvas.getGraphics());
    }

    /**
     * Set up the mouse listeners for the canvas and map viewer.
     */
    public void setupMouseListeners() {
        if (canvas == null || mapViewer == null) return;
        canvas.setupMouseListeners(mapViewer);
    }

    /**
     * Starts the GUI by creating and displaying the GUI-objects.
     *
     * @param innerWindowSize The desired size of the GUI window.
     * @param sound Enable sound if true.
     */
    public void startGUI(Dimension innerWindowSize, boolean sound) {
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

        gd = getDefaultScreenDevice();
        if (gd == null) {
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

        this.mapViewer = new MapViewer(freeColClient, innerWindowSize,
                                       imageLibrary);
        this.canvas = new Canvas(freeColClient, innerWindowSize, mapViewer);
        this.colonyTileGUI = new MapViewer(freeColClient, innerWindowSize,
                                           imageLibrary);

        changeWindowedMode(isWindowed());
        frame.setIconImage(ResourceManager.getImage("FrameIcon.image"));

        // Now that there is a canvas, prepare for language changes.
        LanguageOption o = (LanguageOption)freeColClient.getClientOptions()
            .getOption(ClientOptions.LANGUAGE);
        o.addPropertyChangeListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent e) {
                    Language language = (Language)e.getNewValue();
                    logger.info("Set language to: " + language);
                    if (language.getKey().equals(LanguageOption.AUTO)) {
                        showInformationMessage("autodetectLanguageSelected");
                    } else {
                        Locale l = language.getLocale();
                        Messages.setMessageBundle(l);
                        Messages.setModMessageBundle(l);
                        showInformationMessage(StringTemplate.template("newLanguageSelected")
                            .addName("%language%", l.getDisplayName()));
                    }
                }
            });

        mapViewer.startCursorBlinking();
    }

    /**
     * Start the GUI for the map editor.
     */
    public void startMapEditorGUI() {
        if (frame == null || canvas == null) return;

        // We may need to reset the zoom value to the default value
        scaleMap(2f);

        frame.setJMenuBar(new MapEditorMenuBar(freeColClient));
        canvas.showMapEditorTransformPanel();

        CanvasMapEditorMouseListener listener
            = new CanvasMapEditorMouseListener(freeColClient, canvas);
        canvas.addMouseListener(listener);
        canvas.addMouseMotionListener(listener);
    }

    /**
     * Update the menu bar.
     */
    public void updateMenuBar() {
        if (frame != null && frame.getJMenuBar() != null) {
            ((FreeColMenuBar)frame.getJMenuBar()).update();
        }
    }


    // Static utilities.

    /**
     * Determine whether full screen mode can be used.
     *
     * @return True if full screen is available.
     */
    public static boolean checkFullScreen() {
        GraphicsDevice gd = getDefaultScreenDevice();
        return gd != null && gd.isFullScreenSupported();
    }

    /**
     * Estimate size of client area when using the full screen.
     *
     * @return A suitable window size.
     */
    public static Dimension determineFullScreenSize() {
        GraphicsDevice gd = getDefaultScreenDevice();
        if (gd == null) return null;
        Rectangle bounds = gd.getDefaultConfiguration().getBounds();
        return new Dimension(bounds.width - bounds.x,
                             bounds.height - bounds.y);
    }

    /**
     * Estimate size of client area for a window of maximum size.
     *
     * @return A suitable window size.
     */
    public static Dimension determineWindowSize() {
        final GraphicsEnvironment lge
            = GraphicsEnvironment.getLocalGraphicsEnvironment();
        final GraphicsDevice gd = getDefaultScreenDevice();
        if (gd == null) return null;

        // Get max size of window including border.
        Rectangle bounds = lge.getMaximumWindowBounds();

        // Do we trust getMaximumWindowBounds?
        // Check the insets for evidence the taskbar has been missed.
        Insets insets = Toolkit.getDefaultToolkit()
            .getScreenInsets(gd.getDefaultConfiguration());
        if (insets != null && insets.top <= 0 && insets.bottom <= 0) {
            bounds.height -= DEFAULT_SCREEN_INSET_HEIGHT;
        }
        if (insets != null && insets.left <= 0 && insets.right <= 0) {
            bounds.width -= DEFAULT_SCREEN_INSET_WIDTH;
        }

        // TODO: find better way to get size of window title and
        // border.  The information is only available from getInsets
        // when a window is already displayable.
        Dimension size
            = new Dimension(bounds.width - DEFAULT_WINDOW_INSET_WIDTH,
                            bounds.height - DEFAULT_WINDOW_INSET_HEIGHT);
        logger.info("Screen = " + Toolkit.getDefaultToolkit().getScreenSize()
            + "\nBounds = " + gd.getDefaultConfiguration().getBounds()
            + "\nMaxBounds = " + lge.getMaximumWindowBounds()
            + "\nInsets = " + insets
            + "\n => " + size);
        return size;
    }

    /**
     * Gets a default header for panels.
     *
     * @param text The text to display.
     * @return A suitable <code>JLabel</code>.
     */
    public static JLabel getDefaultHeader(String text) {
        JLabel header = new JLabel(text, JLabel.CENTER);
        header.setFont(BIG_HEADER_FONT);
        header.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        return header;
    }

    /**
     * Get the default screen device.
     *
     * @return The default screen device, or null if none available
     *     (as in headless mode).
     */
    public static GraphicsDevice getDefaultScreenDevice() {
        final GraphicsEnvironment lge
            = GraphicsEnvironment.getLocalGraphicsEnvironment();
        try {
            return lge.getDefaultScreenDevice();
        } catch (HeadlessException he) {}
        return null;
    }

    /**
     * Return a button suitable for linking to another panel
     * (e.g. ColopediaPanel).
     *
     * @param text a <code>String</code> value
     * @param icon an <code>Icon</code> value
     * @param action a <code>String</code> value
     * @return a <code>JButton</code> value
     */
    public static JButton getLinkButton(String text, Icon icon, String action) {
        JButton button = new JButton(text, icon);
        button.setMargin(EMPTY_MARGIN);
        button.setOpaque(false);
        button.setForeground(LINK_COLOR);
        button.setAlignmentY(0.8f);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setActionCommand(action);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    /**
     * Gets a text area with standard settings suitable for use in FreeCol
     * panels.
     *
     * @param text The text to display in the text area.
     * @return A suitable text area.
     */
    public static JTextArea getDefaultTextArea(String text) {
        return getDefaultTextArea(text, DEFAULT_TEXT_COLUMNS);
    }

    /**
     * Gets a text area with standard settings suitable for use in FreeCol
     * panels.
     *
     * @param text The text to display in the text area.
     * @param columns The em-width number of columns to display the text in.
     * @return A suitable text area.
     */
    public static JTextArea getDefaultTextArea(String text, int columns) {
        JTextArea textArea = new JTextArea(text);
        textArea.setColumns(columns);
        textArea.setOpaque(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFocusable(false);
        textArea.setFont(DEFAULT_FONT);
        // necessary because of resizing
        textArea.setSize(textArea.getPreferredSize());
        return textArea;
    }

    /**
     * Get a <code>JTextPane</code> with default styles.
     *
     * @return The default <code>JTextPane</code> to use.
     */
    public static JTextPane getDefaultTextPane() {
        return getDefaultTextPane(null);
    }

    /**
     * Get a <code>JTextPane</code> with default styles and given text.
     *
     * @param text The text to display.
     * @return A suitable <code>JTextPane</code>.
     */
    public static JTextPane getDefaultTextPane(String text) {
        DefaultStyledDocument document
            = new DefaultStyledDocument(GUI.STYLE_CONTEXT) {
                    @Override
                    public Font getFont(AttributeSet attr) {
                        Font font = ResourceManager.getFont(StyleConstants
                            .getFontFamily(attr),
                            StyleConstants.getFontSize(attr));
                        if (font == null) return super.getFont(attr);
                        int fontStyle = Font.PLAIN;
                        if (StyleConstants.isBold(attr)) {
                            fontStyle |= Font.BOLD;
                        }
                        if (StyleConstants.isItalic(attr)) {
                            fontStyle |= Font.ITALIC;
                        }
                        return (fontStyle == Font.PLAIN) ? font
                            : font.deriveFont(fontStyle);
                    }
                };

        JTextPane textPane = new JTextPane(document);
        textPane.setOpaque(false);
        textPane.setEditable(false);
        textPane.setLogicalStyle(STYLE_CONTEXT.getStyle("regular"));

        textPane.setText(text);
        return textPane;
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

    public void showMapControls(boolean value) {
        if (canvas == null) return;

        if (value && freeColClient.isInGame()) {
            if (mapControls == null) {
                try {
                    String className = freeColClient.getClientOptions()
                        .getString(ClientOptions.MAP_CONTROLS);
                    String panelName = "net.sf.freecol.client.gui.panel."
                        + className;
                    Class<?> controls = Class.forName(panelName);
                    mapControls = (MapControls)controls
                        .getConstructor(FreeColClient.class, GUI.class)
                        .newInstance(freeColClient, this);
                } catch (Exception e) {
                    mapControls = new CornerMapControls(freeColClient);
                }
            }
            mapControls.update();
        }
        if (mapControls != null) {
            if (value) {
                if (!mapControls.isShowing()) {
                    mapControls.addToComponent(canvas);
                }
                mapControls.update();
            } else {
                if (mapControls.isShowing()) {
                    mapControls.removeFromComponent(canvas);
                }
            }
        }
    }

    public void updateMapControls() {
        if (mapControls != null) mapControls.update();
    }

    public void updateMapControlsInCanvas() {
        if (mapControls != null && mapControls.isShowing()) {
            mapControls.removeFromComponent(canvas);
            mapControls.addToComponent(canvas);
        }
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

    public void errorMessage(StringTemplate template) {
        if (canvas == null) return;
        canvas.errorMessage(Messages.message(template));
    }

    public void errorMessage(String messageId) {
        if (canvas == null) return;
        canvas.errorMessage(messageId);
    }

    public void errorMessage(String messageID, String message) {
        if (canvas == null) return;
        canvas.errorMessage(messageID, message);
    }

    public LoadingSavegameDialog getLoadingSavegameDialog() {
        if (canvas == null) return null;
        return canvas.getLoadingSavegameDialog();
    }

    public boolean isClientOptionsDialogShowing() {
        return canvas != null && !canvas.isClientOptionsDialogShowing();
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

    public void requestFocusForSubPanel() {
        if (canvas == null) return;
        canvas.getShowingSubPanel().requestFocus();
    }

    public boolean requestFocusInWindow() {
        if (canvas == null) return false;
        return canvas.requestFocusInWindow();
    }

    public void returnToTitle() {
        if (canvas == null) return;
        canvas.returnToTitle();
    }

    public void showAboutPanel() {
        if (canvas == null) return;
        canvas.showAboutPanel();
    }

    public ScoutIndianSettlementAction
        showArmedUnitIndianSettlementDialog(IndianSettlement settlement) {
        if (canvas == null) return ScoutIndianSettlementAction.CANCEL;
        return canvas.showArmedUnitIndianSettlementDialog(settlement);
    }

    public BoycottAction showBoycottedGoodsDialog(Goods goods, Europe europe) {
        if (canvas == null) return BoycottAction.CANCEL;
        return canvas.showBoycottedGoodsDialog(goods, europe);
    }

    public void showBuildQueuePanel(Colony colony) {
        if (canvas == null) return;
        canvas.showBuildQueuePanel(colony);
    }

    public void showBuildQueuePanel(Colony colony, Runnable callBack) {
        if (canvas == null) return;
        canvas.showBuildQueuePanel(colony, callBack);
    }

    public BuyAction showBuyDialog(Unit unit, Settlement settlement,
            Goods goods, int gold, boolean canBuy) {
        if (canvas == null) return BuyAction.CANCEL;
        return canvas.showBuyDialog(unit, settlement, goods, gold, canBuy);
    }

    public List<Goods> showCaptureGoodsDialog(Unit winner, List<Goods> loot) {
        if (canvas == null) return Collections.emptyList();
        return canvas.showCaptureGoodsDialog(winner, loot);
    }

    public void showChatPanel() {
        if (canvas == null) return;
        canvas.showChatPanel();
    }

    public <T> T showChoiceDialog(Tile tile, String text, String cancelText,
                                  List<ChoiceItem<T>> choices) {
        if (canvas == null) return null;
        return canvas.showChoiceDialog(tile, text, cancelText, choices);
    }

    public MonarchAction showChooseMonarchActionDialog(String monarchTitle,
        List<ChoiceItem<MonarchAction>> actions) {
        if (canvas == null) return MonarchAction.NO_ACTION;
        return canvas.showChooseMonarchActionDialog(monarchTitle, actions);
    }

    public FoundingFather showChooseFoundingFatherDialog(List<ChoiceItem<FoundingFather>> fathers,
                                                         String fatherTitle) {
        if (canvas == null) return null;
        return canvas.showChooseFoundingFatherDialog(fathers, fatherTitle);
    }

    public FoundingFather
        showChooseFoundingFatherDialog(List<FoundingFather> ffs) {
        if (canvas == null) return null;
        return canvas.showChooseFoundingFatherDialog(ffs);
    }

    public ClaimAction showClaimDialog(Tile tile, Player player, int price,
            Player owner, boolean canAccept) {
        if (canvas == null) return ClaimAction.CANCEL;
        return canvas.showClaimDialog(tile, player, price, owner, canAccept);
    }

    public OptionGroup showClientOptionsDialog() {
        if (canvas == null) return null;
        return canvas.showClientOptionsDialog();
    }

    public ColonyPanel showColonyPanel(Colony colony) {
        if (canvas == null) return null;
        return canvas.showColonyPanel(colony);
    }

    public void showColopediaPanel(String nodeId) {
        if (canvas == null) return;
        canvas.showColopediaPanel(nodeId);
    }

    public void showCompactLabourReport() {
        if (canvas == null) return;
        canvas.showCompactLabourReport();
    }

    public void showCompactLabourReport(UnitData unitData) {
        if (canvas == null) return;
        canvas.showCompactLabourReport(unitData);
    }

    public List<String> showConfirmDeclarationDialog() {
        if (canvas == null) return Collections.emptyList();
        return canvas.showConfirmDeclarationDialog();
    }

    public boolean showConfirmDialog(String text,
                                     String okText, String cancelText) {
        if (canvas == null) return false;
        return canvas.showConfirmDialog(null, Messages.message(text),
                                        null, okText, cancelText);
    }

    public boolean showConfirmDialog(Tile tile,
                                     StringTemplate template, Object obj,
                                     String okText, String cancelText) {
        if (canvas == null) return false;
        return canvas.showConfirmDialog(tile, Messages.message(template),
                                        getImageIcon(obj, false),
                                        okText, cancelText);
    }

    public void showDeclarationPanel() {
        if (canvas == null) return;
        canvas.showDeclarationPanel();
    }

    public void showDifficultyDialog(boolean editable) {
        if (canvas == null) return;
        canvas.showDifficultyDialog(editable);
    }

    public void showDifficultyDialog(Specification spec, OptionGroup group) {
        if (canvas == null) return;
        canvas.showDifficultyDialog(spec, group);
    }

    public List<Goods> showDumpCargoDialog(Unit unit) {
        if (canvas == null) return Collections.emptyList();
        return canvas.showDumpCargoDialog(unit);
    }

    public boolean showEditOptionDialog(Option option) {
        if (canvas == null) return false;
        return canvas.showEditOptionDialog(option);
    }

    public int showEmigrationDialog(boolean fountainOfYouth) {
        if (canvas == null) return 0;
        return canvas.showEmigrationDialog(fountainOfYouth);
    }

    public boolean showEndTurnDialog(List<Unit> units) {
        if (canvas == null) return false;
        return canvas.showEndTurnDialog(units);
    }

    public int showEuropeDialog(EuropePanel.EuropeAction europeAction) {
        if (canvas == null) return -1;
        return canvas.showEuropeDialog(europeAction);
    }

    public void showEuropePanel() {
        if (canvas == null) return;
        canvas.showEuropePanel();
    }

    public void showEventPanel(EventType type) {
        if (canvas == null) return;
        canvas.showEventPanel(type);
    }

    public void showFindSettlementDialog() {
        if (canvas == null) return;
        canvas.showFindSettlementDialog();
    }

    public void showGameOptionsDialog(boolean editable,
                                      boolean loadCustomOptions) {
        if (canvas == null) return;
        canvas.showGameOptionsDialog(editable, loadCustomOptions);
    }

    public void showHighScoresPanel(String messageId) {
        if (canvas == null) return;
        canvas.showHighScoresPanel(messageId);
    }

    public void showIndianSettlementPanel(IndianSettlement indianSettlement) {
        if (canvas == null) return;
        canvas.showIndianSettlementPanel(indianSettlement);
    }

    public TradeAction showIndianSettlementTradeDialog(Settlement settlement,
        boolean canBuy, boolean canSell, boolean canGift) {
        if (canvas == null) return TradeAction.CANCEL;
        return canvas.showIndianSettlementTradeDialog(settlement,
                                                      canBuy, canSell, canGift);
    }

    public void showInformationMessage(FreeColObject displayObject,
                                       String messageId) {
        if (canvas == null) return;
        canvas.showInformationMessage(displayObject, messageId);
    }

    public void showInformationMessage(FreeColObject displayObject,
                                       StringTemplate template) {
        if (canvas == null) return;
        canvas.showInformationMessage(displayObject, template);
    }

    public void showInformationMessage(ModelMessage message) {
        if (canvas == null) return;
        canvas.showInformationMessage(message);
    }

    public void showInformationMessage(String messageId) {
        if (canvas == null) return;
        canvas.showInformationMessage(messageId);
    }

    public void showInformationMessage(StringTemplate template) {
        if (canvas == null) return;
        canvas.showInformationMessage(template);
    }

    public String showInputDialog(Tile tile, StringTemplate text,
                                  String defaultValue,
                                  String okText, String cancelText,
                                  boolean rejectEmptyString) {
        if (canvas == null) return null;
        return canvas.showInputDialog(tile, text, defaultValue,
                                      okText, cancelText, rejectEmptyString);
    }

    public File showLoadDialog(File directory) {
        if (canvas == null) return null;
        return canvas.showLoadDialog(directory);
    }

    public File showLoadDialog(File directory, FileFilter[] fileFilters) {
        if (canvas == null) return null;
        return canvas.showLoadDialog(directory, fileFilters);
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

    public OptionGroup showMapGeneratorOptionsDialog(OptionGroup mgo,
                                                     boolean editable,
                                                     boolean loadCustom) {
        if (canvas == null) return null;
        return canvas.showMapGeneratorOptionsDialog(mgo, editable, loadCustom);
    }

    public Dimension showMapSizeDialog() {
        if (canvas == null) return null;
        return canvas.showMapSizeDialog();
    }

    public void showModelMessages(ModelMessage... modelMessages) {
        if (canvas == null) return;
        canvas.showModelMessages(modelMessages);
    }

    public boolean showMonarchDialog(MonarchAction action,
                                     StringTemplate replace) {
        if (canvas == null) return false;
        return canvas.showMonarchDialog(action, replace);
    }

    public DiplomaticTrade showNegotiationDialog(Unit unit,
                                                 Settlement settlement,
                                                 DiplomaticTrade agreement) {
        if (canvas == null) return null;
        return canvas.showNegotiationDialog(unit, settlement, agreement);
    }

    public void showNewPanel() {
        if (canvas == null) return;
        canvas.showNewPanel();
    }

    public void showNewPanel(Specification specification) {
        if (canvas == null) return;
        canvas.showNewPanel(specification);
    }

    public void showOpeningVideoPanel() {
        if (canvas == null) return;
        canvas.showOpeningVideoPanel();
    }

    public Parameters showParametersDialog() {
        if (canvas == null) return null;
        return canvas.showParametersDialog();
    }

    public boolean showPreCombatDialog(FreeColGameObject attacker,
                                       FreeColGameObject defender, Tile tile) {
        if (canvas == null) return false;
        return canvas.showPreCombatDialog(attacker, defender, tile);
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

    public void showReportTurnPanel(ModelMessage... messages) {
        if (canvas == null) return;
        canvas.showReportTurnPanel(messages);
    }

    public File showSaveDialog(File directory, String defaultName) {
        if (canvas == null) return null;
        return canvas.showSaveDialog(directory, defaultName);
    }

    public File showSaveDialog(File directory, String standardName,
                               FileFilter[] fileFilters, String defaultName) {
        if (canvas == null) return null;
        return canvas.showSaveDialog(directory, standardName,
                                     fileFilters, defaultName);
    }

    public Dimension showScaleMapSizeDialog() {
        if (canvas == null) return null;
        return canvas.showScaleMapSizeDialog();
    }

    public ScoutColonyAction showScoutForeignColonyDialog(Colony colony,
        Unit unit, boolean canNegotiate) {
        if (canvas == null) return ScoutColonyAction.CANCEL;
        return canvas.showScoutForeignColonyDialog(colony, unit, canNegotiate);
    }

    public ScoutIndianSettlementAction
        showScoutIndianSettlementDialog(IndianSettlement settlement,
                                        String number) {
        if (canvas == null) return ScoutIndianSettlementAction.CANCEL;
        return canvas.showScoutIndianSettlementDialog(settlement, number);
    }

    public int showSelectAmountDialog(GoodsType goodsType, int available,
                                      int defaultAmount, boolean needToPay) {
        if (canvas == null) return -1;
        return canvas.showSelectAmountDialog(goodsType, available,
                                             defaultAmount, needToPay);
    }

    public Location showSelectDestinationDialog(Unit unit) {
        if (canvas == null) return null;
        return canvas.showSelectDestinationDialog(unit);
    }

    public SellAction showSellDialog(Unit unit, Settlement settlement,
                                     Goods goods, int gold) {
        if (canvas == null) return SellAction.CANCEL;
        return canvas.showSellDialog(unit, settlement, goods, gold);
    }

    public void showServerListPanel(List<ServerInfo> serverList) {
        if (canvas == null) return;
        canvas.showServerListPanel(serverList);
    }

    public <T> T showSimpleChoiceDialog(Tile tile, String text,
                                        String cancelText, List<T> objects) {
        if (canvas == null) return null;
        return canvas.showSimpleChoiceDialog(tile, text, cancelText, objects);
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
        canvas.showTilePopup(getSelectedTile(),
                mapViewer.getCursor().getCanvasX(),
                mapViewer.getCursor().getCanvasY());
    }

    public boolean showTradeRouteDialog(Unit unit) {
        if (canvas == null) return false;
        return canvas.showTradeRouteDialog(unit);
    }

    public boolean showTradeRouteInputDialog(TradeRoute newRoute) {
        if (canvas == null) return false;
        return canvas.showTradeRouteInputDialog(newRoute);
    }

    public MissionaryAction showUseMissionaryDialog(Unit unit,
                                                    IndianSettlement settlement,
                                                    boolean canEstablish,
                                                    boolean canDenounce) {
        if (canvas == null) return MissionaryAction.CANCEL;
        return canvas.showUseMissionaryDialog(unit, settlement,
                                              canEstablish, canDenounce);
    }

    public void showVictoryPanel() {
        if (canvas == null) return;
        canvas.showVictoryPanel();
    }

    public boolean showWarehouseDialog(Colony colony) {
        if (canvas == null) return false;
        return canvas.showWarehouseDialog(colony);
    }

    public void showWorkProductionPanel(Unit unit) {
        if (canvas == null) return;
        canvas.showWorkProductionPanel(unit);
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

    public int getCurrentViewMode() {
        if (mapViewer == null) return -1;
        return mapViewer.getView();
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
        return mapViewer.getTileBounds(tile);
    }

    public Point getTilePosition(Tile tile) {
        if (mapViewer == null) return null;
        return mapViewer.getTilePosition(tile);
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
}
