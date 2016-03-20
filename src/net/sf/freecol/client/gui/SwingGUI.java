/**
 *  Copyright (C) 2002-2016   The FreeCol Team
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
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JWindow;
import javax.swing.Timer;
import javax.swing.filechooser.FileFilter;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.animation.Animations;
import net.sf.freecol.client.gui.panel.ColonyPanel;
import net.sf.freecol.client.gui.panel.ColorChooserPanel;
import net.sf.freecol.client.gui.panel.CornerMapControls;
import net.sf.freecol.client.gui.panel.FreeColDialog;
import net.sf.freecol.client.gui.panel.LabourData.UnitData;
import net.sf.freecol.client.gui.panel.MapControls;
import net.sf.freecol.client.gui.panel.Parameters;
import net.sf.freecol.client.gui.panel.TradeRoutePanel;
import net.sf.freecol.client.gui.panel.Utility;
import net.sf.freecol.client.gui.plaf.FreeColLookAndFeel;
import net.sf.freecol.client.gui.video.VideoComponent;
import net.sf.freecol.client.gui.video.VideoListener;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.ServerInfo;
import net.sf.freecol.common.i18n.Messages;
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
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Monarch.MonarchAction;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
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

import static net.sf.freecol.common.util.StringUtils.lastPart;


/**
 * A wrapper providing functionality for the overall GUI using Java Swing.
 */
public class SwingGUI extends GUI {

    private final GraphicsDevice graphicsDevice;

    /**
     * This is the TileViewer instance used to paint the map tiles
     * in the ColonyPanel and other panels.  It should not be scaled
     * along with the default MapViewer.
     */
    private TileViewer tileViewer;

    /**
     * The MapViewer instance used by canvas to paint the main map.
     * This does need to be scaled.
     */
    private MapViewer mapViewer;

    /** The canvas that implements much of the functionality. */
    private Canvas canvas;

    private MapControls mapControls;

    private JWindow splash;


    /**
     * Create the GUI.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param scaleFactor The scale factor for the GUI.
     */
    public SwingGUI(FreeColClient freeColClient, float scaleFactor) {
        super(freeColClient, scaleFactor);
        
        graphicsDevice = getGoodGraphicsDevice();
        logger.info("GUI constructed using scale factor " + scaleFactor);
    }


    // Simple accessors

    public Canvas getCanvas() {
        return canvas;
    }

    public ImageLibrary getTileImageLibrary() {
        return tileViewer.getImageLibrary();
    }

    @Override
    public boolean isWindowed() {
        return canvas.isWindowed();
    }

    // Initialization related methods

    /** 
     * {@inheritDoc}
     */
    @Override
    public void installLookAndFeel(String fontName) throws FreeColException {
        FreeColLookAndFeel fclaf = new FreeColLookAndFeel();
        FreeColLookAndFeel.install(fclaf);
        Font font = FontLibrary.createMainFont(
            fontName, imageLibrary.getScaleFactor());
        FreeColLookAndFeel.installFont(font);
        Utility.initStyleContext(font);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void quit() {
        if (canvas != null) {
            canvas.quit();
        }
    }

    /** 
     * {@inheritDoc}
     */
    @Override
    public void initializeInGame() {
        canvas.initializeInGame();
        enableMapControls(getClientOptions()
            .getBoolean(ClientOptions.DISPLAY_MAP_CONTROLS));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setupMouseListeners() {
        canvas.setupMouseListeners();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void displaySplashScreen(final InputStream splashStream) {
        splash = null;
        if (splashStream == null) return;
        try {
            BufferedImage im = ImageIO.read(splashStream);
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
     * {@inheritDoc}
     */
    @Override
    public void hideSplashScreen() {
        if (splash != null) {
            splash.setVisible(false);
            splash.dispose();
            splash = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showOpeningVideo(final String userMsg) {
        canvas.closeMenus();
        final Video video = ResourceManager.getVideo("video.opening");
        boolean muteAudio = !getSoundController().canPlaySound();
        final VideoComponent vp = new VideoComponent(video, muteAudio);

        final class AbortListener implements ActionListener, KeyListener,
            MouseListener, VideoListener {

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
            public void actionPerformed(ActionEvent ae8) {
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
                playSound("sound.intro.general");
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

        FreeColClient.fatal("Could not find a GraphicsDevice!");
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startGUI(final Dimension desiredWindowSize) {
        final ClientOptions opts = getClientOptions();

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
        usePixmaps.addPropertyChangeListener((PropertyChangeEvent e) -> {
                String newValue = e.getNewValue().toString();
                System.setProperty(pmoffscreen, newValue);
                logger.info("Set " + pmoffscreen + " to: " + newValue);
            });

        this.mapViewer = new MapViewer(getFreeColClient());
        this.canvas = new Canvas(getFreeColClient(), graphicsDevice, this,
                                 desiredWindowSize, mapViewer);
        this.tileViewer = new TileViewer(getFreeColClient());

        // Now that there is a canvas, prepare for language changes.
        LanguageOption o = (LanguageOption)getClientOptions()
            .getOption(ClientOptions.LANGUAGE);
        o.addPropertyChangeListener((PropertyChangeEvent e) -> {
                Language language = (Language)e.getNewValue();
                logger.info("Set language to: " + language);
                if (Messages.AUTOMATIC.equalsIgnoreCase(language.getKey())) {
                    showInformationMessage("info.autodetectLanguageSelected");
                } else {
                    Locale l = language.getLocale();
                    Messages.loadMessageBundle(l);
                    Messages.loadModMessageBundle(l);
                    showInformationMessage(StringTemplate
                        .template("info.newLanguageSelected")
                        .addName("%language%", l.getDisplayName()));
                }
            });

        logger.info("GUI created.");
        logger.info("Starting in Move Units View Mode");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void changeWindowedMode() {
        canvas.changeWindowedMode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startMapEditorGUI() {
        canvas.startMapEditorGUI();
    }


    // Non-trivial public routines.

    /**
     * {@inheritDoc}
     */
    @Override
    public void activateGotoPath() {
        Unit unit = getActiveUnit();

        // Action should be disabled if there is no active unit, but make sure
        if (unit == null) return;

        // Enter "goto mode" if not already activated; otherwise cancel it
        if (canvas.isGotoStarted()) {
            canvas.stopGoto();
        } else {
            canvas.startGoto();

            // Draw the path to the current mouse position, if the
            // mouse is over the screen; see also
            // CanvasMouseMotionListener.
            Point pt = canvas.getMousePosition();
            if (pt != null) {
                Tile tile = canvas.convertToMapTile(pt.x, pt.y);
                if (tile != null && unit.getTile() != tile) {
                    canvas.setGotoPath(unit.findPath(tile));
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearGotoPath() {
        Unit unit = getActiveUnit();

        // Action should be disabled if there is no active unit, but make sure
        if (unit == null) return;
        canvas.stopGoto();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void displayChatMessage(Player player, String message,
                                   boolean privateChat) {
        canvas.displayChatMessage(new GUIMessage(
            player.getName() + ": " + message, player.getNationColor()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void refresh() {
        mapViewer.forceReposition();
        canvas.refresh();
    }

    /**
     * {@inheritDoc}
     */
    public void refreshTile(Tile tile) {
        if (tile.getX() >= 0 && tile.getY() >= 0) {
            canvas.repaint(mapViewer.calculateTileBounds(tile));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resetMenuBar() {
        getFreeColClient().updateActions();
        canvas.resetMenuBar();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void resetMapZoom() {
        super.resetMapZoom();
        mapViewer.resetMapScale();
        refresh();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canZoomInMap() {
        return !mapViewer.isAtMaxMapScale();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canZoomOutMap() {
        return !mapViewer.isAtMinMapScale();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void zoomInMap() {
        super.zoomInMap();
        mapViewer.increaseMapScale();
        refresh();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void zoomOutMap() {
        super.zoomOutMap();
        mapViewer.decreaseMapScale();
        refresh();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean setActiveUnit(Unit unit) {
        boolean result = mapViewer.setActiveUnit(unit);
        updateMapControls();
        updateMenuBar();
        if (unit != null && !getMyPlayer().owns(unit)) {
            canvas.refresh();
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateMenuBar() {
        getFreeColClient().updateActions();
        canvas.updateMenuBar();
    }


    // Animation handling

    /**
     * Require the given tile to be in the onScreen()-area.
     *
     * @param tile The <code>Tile</code> to check.
     * @return True if the focus was set.
     */
    public boolean requireFocus(Tile tile) {
        // Account for the ALWAYS_CENTER client option.
        boolean required = getClientOptions().getBoolean(ClientOptions.ALWAYS_CENTER);
        if ((required && tile != getFocus()) || !mapViewer.onScreen(tile)) {
            setFocusImmediately(tile);
            return true;
        }
        return false;
    }
        
    /**
     * {@inheritDoc}
     */
    @Override
    public void animateUnitAttack(Unit attacker, Unit defender,
                                  Tile attackerTile, Tile defenderTile,
                                  boolean success) {
        requireFocus(attackerTile);
        Animations.unitAttack(getFreeColClient(), attacker, defender,
                              attackerTile, defenderTile, success);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void animateUnitMove(Unit unit, Tile srcTile, Tile dstTile) {
        requireFocus(srcTile);
        Animations.unitMove(getFreeColClient(), unit, srcTile, dstTile);
    }


    // MapControls handling

    /**
     * {@inheritDoc}
     */
    @Override
    public void enableMapControls(boolean enable) {
        // Always instantiate in game.
        if (enable && mapControls == null) {
            String className = getClientOptions().getString(ClientOptions.MAP_CONTROLS);
            try {
                final String panelName = "net.sf.freecol.client.gui.panel."
                    + lastPart(className, ".");
                Class<?> controls = Class.forName(panelName);
                mapControls = (MapControls)controls
                    .getConstructor(FreeColClient.class)
                    .newInstance(getFreeColClient());
                logger.info("Instantiated " + panelName);
            } catch (Exception e) {
                logger.log(Level.INFO, "Fallback to CornerMapControls from "
                    + className, e);
                mapControls = new CornerMapControls(getFreeColClient());
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateMapControls() {
        if (mapControls != null) mapControls.update();
    }

    public void updateMapControlsInCanvas() {
        if (mapControls == null) return;
        mapControls.removeFromComponent(canvas);
        mapControls.addToComponent(canvas);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void zoomInMapControls() {
        if (mapControls == null) return;
        mapControls.zoomIn();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void zoomOutMapControls() {
        if (mapControls == null) return;
        mapControls.zoomOut();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canZoomInMapControls() {
        return mapControls != null && mapControls.canZoomInMapControls();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canZoomOutMapControls() {
        return mapControls != null && mapControls.canZoomOutMapControls();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void miniMapToggleViewControls() {
        if (mapControls == null) return;
        mapControls.toggleView();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void miniMapToggleFogOfWarControls() {
        if (mapControls == null) return;
        mapControls.toggleFogOfWar();
    }


    // Dialogs that return values

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean confirm(String textKey, String okKey, String cancelKey) {
        return canvas.showConfirmDialog(null, Messages.message(textKey),
            null, okKey, cancelKey);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean confirm(Tile tile, StringTemplate template,
                           String okKey, String cancelKey) {
        return canvas.showConfirmDialog(tile,
            Utility.localizedTextArea(template), null, okKey, cancelKey);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean confirm(Tile tile, StringTemplate template, Unit unit,
                           String okKey, String cancelKey) {
        return canvas.showConfirmDialog(tile,
            Utility.localizedTextArea(template),
            new ImageIcon(imageLibrary.getUnitImage(unit)),
            okKey, cancelKey);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean confirm(Tile tile, StringTemplate template,
                           Settlement settlement,
                           String okKey, String cancelKey) {
        return canvas.showConfirmDialog(tile,
            Utility.localizedTextArea(template),
            new ImageIcon(imageLibrary.getSettlementImage(settlement)),
            okKey, cancelKey);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean confirm(Tile tile, StringTemplate template,
                           GoodsType goodsType,
                           String okKey, String cancelKey) {
        return canvas.showConfirmDialog(tile,
            Utility.localizedTextArea(template),
            new ImageIcon(imageLibrary.getIconImage(goodsType)),
            okKey, cancelKey);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> confirmDeclaration() {
        return canvas.showConfirmDeclarationDialog();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getChoice(Tile tile, Object explain,
                           String cancelKey, List<ChoiceItem<T>> choices) {
        return canvas.showChoiceDialog(tile, explain,
            null, cancelKey, choices);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getChoice(Tile tile, Object explain, Unit unit,
                           String cancelKey, List<ChoiceItem<T>> choices) {
        return canvas.showChoiceDialog(tile, explain,
            new ImageIcon(imageLibrary.getUnitImage(unit)),
            cancelKey, choices);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getChoice(Tile tile, Object explain, Settlement settlement,
                           String cancelKey, List<ChoiceItem<T>> choices) {
        return canvas.showChoiceDialog(tile, explain,
            new ImageIcon(imageLibrary.getSettlementImage(settlement)),
            cancelKey, choices);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getChoice(Tile tile, Object explain, GoodsType goodsType,
                           String cancelKey, List<ChoiceItem<T>> choices) {
        return canvas.showChoiceDialog(tile, explain,
            new ImageIcon(imageLibrary.getIconImage(goodsType)),
            cancelKey, choices);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getChoice(Tile tile, Object explain, Nation nation,
                           String cancelKey, List<ChoiceItem<T>> choices) {
        return canvas.showChoiceDialog(tile, explain,
            new ImageIcon(imageLibrary.getMiscIconImage(nation)),
            cancelKey, choices);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getInput(Tile tile, StringTemplate template,
                           String defaultValue,
                           String okKey, String cancelKey) {
        return canvas.showInputDialog(tile, template, defaultValue,
                                      okKey, cancelKey);
    }


    // Trivial delegations to Canvas

    /**
     * {@inheritDoc}
     */
    @Override
    public void closeMainPanel() {
        canvas.closeMainPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void closeMenus() {
        canvas.closeMenus();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void closeStatusPanel() {
        canvas.closeStatusPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsInGameComponents() {
        return canvas.containsInGameComponents();
    }

    public void dialogRemove(FreeColDialog<?> fcd) {
        canvas.dialogRemove(fcd);
    }

    /**
     * Show the appropriate panel for an object.
     *
     * TODO: Improve OO.
     *
     * @param fco The <code>FreeColObject</code> to display.
     */
    public void displayObject(FreeColObject fco) {
        if (fco instanceof Colony) {
            canvas.showColonyPanel((Colony)fco, null);
        } else if (fco instanceof Europe) {
            canvas.showEuropePanel();
        } else if (fco instanceof IndianSettlement) {
            canvas.showIndianSettlementPanel((IndianSettlement)fco);
        } else if (fco instanceof Tile) {
            setFocus((Tile)fco);
        } else if (fco instanceof Unit) {
            Location loc = ((Unit)fco).up();
            if (loc instanceof Colony) {
                canvas.showColonyPanel((Colony)loc, (Unit)fco);
            } else {
                displayObject((FreeColObject)loc);
            }
        } else if (fco instanceof WorkLocation) {
            canvas.showColonyPanel(((WorkLocation)fco).getColony(), null);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LoadingSavegameInfo getLoadingSavegameInfo() {
        return canvas.getLoadingSavegameDialog().getInfo();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isClientOptionsDialogShowing() {
        return canvas!=null && canvas.isClientOptionsDialogShowing();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isMapboardActionsEnabled() {
        return canvas!=null && canvas.isMapboardActionsEnabled();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isShowingSubPanel() {
        return canvas!=null && canvas.isShowingSubPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void paintImmediatelyCanvasIn(Rectangle rectangle) {
        canvas.paintImmediately(rectangle);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void paintImmediatelyCanvasInItsBounds() {
        canvas.paintImmediately(canvas.getBounds());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void refreshPlayersTable() {
        canvas.refreshPlayersTable();
    }

    /**
     * {@inheritDoc}
     */
    public void removeFromCanvas(Component component) {
        canvas.remove(component);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeInGameComponents() {
        canvas.removeInGameComponents();
    }

    public void removeTradeRoutePanel(TradeRoutePanel panel) {
        canvas.removeTradeRoutePanel(panel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void requestFocusForSubPanel() {
        canvas.getShowingSubPanel().requestFocus();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean requestFocusInWindow() {
        return canvas.requestFocusInWindow();
    }

    public void restoreSavedSize(Component comp, int w, int h) {
        canvas.restoreSavedSize(comp, new Dimension(w, h));
    }

    public void restoreSavedSize(Component comp, Dimension size) {
        canvas.restoreSavedSize(comp, size);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void returnToTitle() {
        canvas.returnToTitle();
        playSound("sound.intro.general");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showAboutPanel() {
        canvas.showAboutPanel();
    }

    public void showBuildQueuePanel(Colony colony) {
        canvas.showBuildQueuePanel(colony);
    }

    public void showBuildQueuePanel(Colony colony, Runnable callBack) {
        canvas.showBuildQueuePanel(colony, callBack);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showCaptureGoodsDialog(final Unit unit, List<Goods> gl,
                                       DialogHandler<List<Goods>> handler) {
        canvas.showCaptureGoodsDialog(unit, gl, handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showChatPanel() {
        canvas.showChatPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showChooseFoundingFatherDialog(final List<FoundingFather> ffs,
                DialogHandler<FoundingFather> handler) {
        canvas.showChooseFoundingFatherDialog(ffs, handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showClientOptionsDialog() {
        OptionGroup group = null;
        try {
            group = canvas.showClientOptionsDialog();
        } finally {
            if (group != null) {
                resetMenuBar();
                // Immediately redraw the minimap if that was updated.
                updateMapControls();
            }
        }
        if (!getFreeColClient().isInGame()) showMainPanel(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void showForeignColony(Settlement settlement) {
        canvas.showForeignColony(settlement);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showColonyPanel(Colony colony, Unit unit) {
        canvas.showColonyPanel(colony, unit);
    }

    public ColonyPanel showColonyPanel2(Colony colony, Unit unit) {
        return canvas.showColonyPanel(colony, unit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showColopediaPanel(String nodeId) {
        canvas.showColopediaPanel(nodeId);
    }

    public ColorChooserPanel showColorChooserPanel(ActionListener al) {
        return canvas.showColorChooserPanel(al);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showCompactLabourReport() {
        canvas.showCompactLabourReport();
    }

    public void showCompactLabourReport(UnitData unitData) {
        canvas.showCompactLabourReport(unitData);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showDeclarationPanel() {
        canvas.showDeclarationPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OptionGroup showDifficultyDialog() {
        final Specification spec = getSpecification();
        return canvas.showDifficultyDialog(spec,
            spec.getDifficultyOptionGroup(), false);
    }

    public OptionGroup showDifficultyDialog(Specification spec,
                                            OptionGroup group) {
        return canvas.showDifficultyDialog(spec, group, group.isEditable());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showDumpCargoDialog(Unit unit,
                                    DialogHandler<List<Goods>> handler) {
        canvas.showDumpCargoDialog(unit, handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean showEditOptionDialog(Option option) {
        return canvas.showEditOptionDialog(option);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showEmigrationDialog(final Player player,
                                     final boolean fountainOfYouth,
                                     DialogHandler<Integer> handler) {
        canvas.showEmigrationDialog(player, fountainOfYouth, handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showEndTurnDialog(final List<Unit> units,
                                  DialogHandler<Boolean> handler) {
        canvas.showEndTurnDialog(units, handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showErrorMessage(StringTemplate template) {
        canvas.showErrorMessage(Messages.message(template));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showErrorMessage(String messageId) {
        canvas.showErrorMessage(messageId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showErrorMessage(String messageId, String message) {
        canvas.showErrorMessage(messageId, message);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showEuropePanel() {
        canvas.showEuropePanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showEventPanel(String header, String image, String footer) {
        canvas.showEventPanel(header, image, footer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showFindSettlementPanel() {
        canvas.showFindSettlementPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OptionGroup showGameOptionsDialog(boolean editable, boolean custom) {
        return canvas.showGameOptionsDialog(editable, custom);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showHighScoresPanel(String messageId, List<HighScore> scores) {
        canvas.showHighScoresPanel(messageId, scores);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showIndianSettlementPanel(IndianSettlement indianSettlement) {
        canvas.showIndianSettlementPanel(indianSettlement);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showInformationMessage(String messageId) {
        super.showInformationMessage(messageId);
        canvas.showInformationMessage(null, null, null,
                                      StringTemplate.key(messageId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showInformationMessage(StringTemplate template) {
        super.showInformationMessage(template);
        canvas.showInformationMessage(null, null, null, template);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showInformationMessage(Settlement displayObject,
                                       StringTemplate template) {
        super.showInformationMessage(displayObject, template);
        ImageIcon icon = null;
        Tile tile = null;
        if(displayObject != null) {
            icon = new ImageIcon(imageLibrary.getSettlementImage(displayObject));
            tile = displayObject.getTile();
        }
        canvas.showInformationMessage(displayObject, tile, icon, template);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showInformationMessage(Unit displayObject,
                                       StringTemplate template) {
        super.showInformationMessage(displayObject, template);
        ImageIcon icon = null;
        Tile tile = null;
        if(displayObject != null) {
            icon = new ImageIcon(imageLibrary.getUnitImage(displayObject));
            tile = displayObject.getTile();
        }
        canvas.showInformationMessage(displayObject, tile, icon, template);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showInformationMessage(Tile displayObject,
                                       StringTemplate template) {
        super.showInformationMessage(displayObject, template);
        canvas.showInformationMessage(displayObject, displayObject, null, template);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showInformationMessage(FreeColObject displayObject,
                                       String messageId) {
        super.showInformationMessage(displayObject, messageId);
        canvas.showInformationMessage(displayObject, StringTemplate.key(messageId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showInformationMessage(FreeColObject displayObject,
                                       StringTemplate template) {
        super.showInformationMessage(displayObject, template);
        canvas.showInformationMessage(displayObject, template);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public File showLoadDialog(File directory) {
        return canvas.showLoadDialog(directory, null);
    }

    public File showLoadDialog(File directory, FileFilter[] fileFilters) {
        return canvas.showLoadDialog(directory, fileFilters);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean showLoadingSavegameDialog(boolean publicServer,
                                             boolean singlePlayer) {
        return canvas.showLoadingSavegameDialog(publicServer, singlePlayer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showLogFilePanel() {
        canvas.showLogFilePanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showMainPanel(String userMsg) {
        canvas.showMainPanel(userMsg);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OptionGroup showMapGeneratorOptionsDialog(boolean editable) {
        return canvas.showMapGeneratorOptionsDialog(editable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Dimension showMapSizeDialog() {
        return canvas.showMapSizeDialog();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showModelMessages(List<ModelMessage> modelMessages) {
        canvas.showModelMessages(modelMessages);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showMonarchDialog(final MonarchAction action,
                                  StringTemplate template, String monarchKey,
                                  DialogHandler<Boolean> handler) {
        canvas.showMonarchDialog(action, template, monarchKey, handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showNamingDialog(StringTemplate template,
                                      final String defaultName,
                                      final Unit unit,
                                      DialogHandler<String> handler) {
        canvas.showNamingDialog(template, defaultName, unit, handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showFirstContactDialog(final Player player, final Player other,
                                       final Tile tile, int settlementCount,
                                       DialogHandler<Boolean> handler) {
        canvas.showFirstContactDialog(player, other, tile, settlementCount,
                                      handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DiplomaticTrade showNegotiationDialog(FreeColGameObject our,
                                                     FreeColGameObject other,
                                                     DiplomaticTrade agreement,
                                                     StringTemplate comment) {
        return canvas.showNegotiationDialog(our, other, agreement, comment);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showNewPanel() {
        canvas.showNewPanel(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showNewPanel(Specification specification) {
        canvas.showNewPanel(specification);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showSpyColonyPanel(final Tile tile) {
        canvas.showSpyColonyPanel(tile);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Parameters showParametersDialog() {
        return canvas.showParametersDialog();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean showPreCombatDialog(Unit attacker,
                                       FreeColGameObject defender, Tile tile) {
        return canvas.showPreCombatDialog(attacker, defender, tile);
    }

    public void showPurchasePanel() {
        canvas.showPurchasePanel();
    }

    public void showRecruitPanel() {
        canvas.showRecruitPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showReportCargoPanel() {
        canvas.showReportCargoPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showReportColonyPanel() {
        canvas.showReportColonyPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showReportContinentalCongressPanel() {
        canvas.showReportContinentalCongressPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showReportEducationPanel() {
        canvas.showReportEducationPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showReportExplorationPanel() {
        canvas.showReportExplorationPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showReportForeignAffairPanel() {
        canvas.showReportForeignAffairPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showReportHistoryPanel() {
        canvas.showReportHistoryPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showReportIndianPanel() {
        canvas.showReportIndianPanel();
    }

    public void showReportLabourDetailPanel(UnitType unitType,
            Map<UnitType, Map<Location, Integer>> data,
            TypeCountMap<UnitType> unitCount, List<Colony> colonies) {
        canvas.showReportLabourDetailPanel(unitType, data, unitCount,
                                           colonies);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showReportLabourPanel() {
        canvas.showReportLabourPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showReportMilitaryPanel() {
        canvas.showReportMilitaryPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showReportNavalPanel() {
        canvas.showReportNavalPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showReportProductionPanel() {
        canvas.showReportProductionPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showReportReligiousPanel() {
        canvas.showReportReligiousPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showReportRequirementsPanel() {
        canvas.showReportRequirementsPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showReportTradePanel() {
        canvas.showReportTradePanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showReportTurnPanel(List<ModelMessage> messages) {
        canvas.showReportTurnPanel(messages);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public File showSaveDialog(File directory, String defaultName) {
        return canvas.showSaveDialog(directory, null, defaultName);
    }

    public File showSaveDialog(File directory, FileFilter[] fileFilters,
                               String defaultName) {
        return canvas.showSaveDialog(directory, fileFilters, defaultName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Dimension showScaleMapSizeDialog() {
        return canvas.showScaleMapSizeDialog();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int showSelectAmountDialog(GoodsType goodsType, int available,
                                      int defaultAmount, boolean needToPay) {
        return canvas.showSelectAmountDialog(goodsType, available,
                                             defaultAmount, needToPay);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int showSelectTributeAmountDialog(StringTemplate question,
                                             int maximum) {
        return canvas.showSelectTributeAmountDialog(question, maximum);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Location showSelectDestinationDialog(Unit unit) {
        return canvas.showSelectDestinationDialog(unit);
    }

    public void showServerListPanel(List<ServerInfo> serverList) {
        canvas.showServerListPanel(serverList);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showStartGamePanel(Game game, Player player,
                                   boolean singlePlayerMode) {
        canvas.showStartGamePanel(game, player, singlePlayerMode);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showStatisticsPanel(Map<String, String> serverStats,
                                    Map<String, String> clientStats) {
        canvas.showStatisticsPanel(serverStats, clientStats);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showStatusPanel(String message) {
        canvas.showStatusPanel(message);
    }

    public void showTilePanel(Tile tile) {
        canvas.showTilePanel(tile);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showTilePopUpAtSelectedTile() {
        Tile tile = mapViewer.getSelectedTile();
        Point point = mapViewer.calculateTilePosition(tile);
        canvas.showTilePopup(tile, point.x+mapViewer.getTileWidth(), point.y);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showTradeRoutePanel(Unit unit) {
        canvas.showTradeRoutePanel(unit);
    }

    public void showTradeRouteInputPanel(TradeRoute newRoute,
                                         Runnable callBack) {
        canvas.showTradeRouteInputPanel(newRoute, callBack);
    }

    public void showTrainPanel() {
        canvas.showTrainPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showVictoryDialog(DialogHandler<Boolean> handler) {
        canvas.showVictoryDialog(handler);
    }

    public boolean showWarehouseDialog(Colony colony) {
        return canvas.showWarehouseDialog(colony);
    }

    public void showWorkProductionPanel(Unit unit) {
        canvas.showWorkProductionPanel(unit);
    }

    public void updateEuropeanSubpanels() {
        canvas.updateEuropeanSubpanels();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateGameOptions() {
        canvas.updateGameOptions();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateMapGeneratorOptions() {
        canvas.updateMapGeneratorOptions();
    }

    // Trivial delegations to MapViewer

    /**
     * {@inheritDoc}
     */
    @Override
    public void centerActiveUnit() {
        mapViewer.centerActiveUnit();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void changeViewMode(int newViewMode) {
        mapViewer.changeViewMode(newViewMode);
    }

    public Point calculateUnitLabelPositionInTile(int labelWidth,int labelHeight,
                                                  Point tileP) {
        return mapViewer.calculateUnitLabelPositionInTile(
            labelWidth, labelHeight, tileP);
    }

    public void executeWithUnitOutForAnimation(final Unit unit,
                                               final Tile sourceTile,
                                               final OutForAnimationCallback r) {
        mapViewer.executeWithUnitOutForAnimation(unit, sourceTile, r);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Unit getActiveUnit() {
        return mapViewer==null ? null : mapViewer.getActiveUnit();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Tile getFocus() {
        return mapViewer.getFocus();
    }

    public float getMapScale() {
        return mapViewer.getImageLibrary().getScaleFactor();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Tile getSelectedTile() {
        return mapViewer.getSelectedTile();
    }

    public Rectangle getTileBounds(Tile tile) {
        return mapViewer.calculateTileBounds(tile);
    }

    public Point getTilePosition(Tile tile) {
        return mapViewer.calculateTilePosition(tile);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getViewMode() {
        return mapViewer.getViewMode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFocus(Tile tileToFocus) {
        mapViewer.setFocus(tileToFocus);
        canvas.refresh();
    }

    public void setFocusImmediately(Tile tileToFocus) {
        mapViewer.setFocus(tileToFocus);
        Dimension size = canvas.getSize();
        canvas.paintImmediately(0, 0, size.width, size.height);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean setSelectedTile(Tile newTileToSelect) {
        boolean result = mapViewer.setSelectedTile(newTileToSelect);
        updateMapControls();
        updateMenuBar();
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void toggleViewMode() {
        mapViewer.toggleViewMode();
    }

    // Forwarding to tileViewer

    public static BufferedImage createTileImageWithOverlayAndForest(
            TileType type, Dimension size) {
        return TileViewer.createTileImageWithOverlayAndForest(type, size);
    }

    public BufferedImage createTileImageWithBeachBorderAndItems(Tile tile) {
        return tileViewer.createTileImageWithBeachBorderAndItems(tile);
    }

    public BufferedImage createTileImage(Tile tile) {
        return tileViewer.createTileImage(tile);
    }

    public BufferedImage createColonyTileImage(Tile tile, Colony colony) {
        return tileViewer.createColonyTileImage(tile, colony);
    }

    public void displayColonyTiles(Graphics2D g, Tile[][] tiles, Colony colony) {
        tileViewer.displayColonyTiles(g, tiles, colony);
    }

}
