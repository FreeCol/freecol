/**
 *  Copyright (C) 2002-2017   The FreeCol Team
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
import java.util.logging.Level;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JWindow;
import javax.swing.Timer;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.animation.Animations;
import net.sf.freecol.client.gui.dialog.CaptureGoodsDialog;
import net.sf.freecol.client.gui.dialog.FreeColDialog;
import net.sf.freecol.client.gui.dialog.Parameters;
import net.sf.freecol.client.gui.panel.BuildQueuePanel;
import net.sf.freecol.client.gui.panel.ColonyPanel;
import net.sf.freecol.client.gui.panel.ColorChooserPanel;
import net.sf.freecol.client.gui.panel.CornerMapControls;
import net.sf.freecol.client.gui.panel.report.LabourData.UnitData;
import net.sf.freecol.client.gui.panel.MapControls;
import net.sf.freecol.client.gui.panel.TradeRouteInputPanel;
import net.sf.freecol.client.gui.panel.Utility;
import net.sf.freecol.client.gui.plaf.FreeColLookAndFeel;
import net.sf.freecol.client.gui.video.VideoComponent;
import net.sf.freecol.client.gui.video.VideoListener;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.metaserver.ServerInfo;
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
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Monarch.MonarchAction;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.PathNode;
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
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param scaleFactor The scale factor for the GUI.
     */
    public SwingGUI(FreeColClient freeColClient, float scaleFactor) {
        super(freeColClient, scaleFactor);
        
        graphicsDevice = getGoodGraphicsDevice();
        logger.info("GUI constructed using scale factor " + scaleFactor);
    }


    // Internals

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
     * Internal version of setSelectedTile allowing focus override.
     *
     * @param newTile The new {@code Tile} to select.
     * @param refocus If true, always refocus.
     */
    private void setSelectedTile(Tile newTile, boolean refocus) {
        final Tile oldTile = getSelectedTile();
        refocus = newTile != null && (refocus || !mapViewer.onScreen(newTile)
            || getClientOptions().getBoolean(ClientOptions.ALWAYS_CENTER));
        if (refocus) {
            setFocus(newTile);
        } else {
            if (oldTile != null) refreshTile(oldTile);
            if (newTile != null) refreshTile(newTile);
        }
        mapViewer.setSelectedTile(newTile);
    }

    // TODO
    private void setFocusImmediately(Tile tileToFocus) {
        mapViewer.setFocus(tileToFocus);
        Dimension size = canvas.getSize();
        canvas.paintImmediately(0, 0, size.width, size.height);
    }


    // Implement GUI

    // Simple accessors

    /**
     * {@inheritDoc}
     */
    @Override
    public Canvas getCanvas() {
        return this.canvas;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageLibrary getTileImageLibrary() {
        return tileViewer.getImageLibrary();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isWindowed() {
        return this.canvas.isWindowed();
    }


    // Initialization and teardown

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
    public void reconnect(Unit active, Tile tile) {
        setupMouseListeners();
        requestFocusInWindow();
        canvas.initializeInGame();
        enableMapControls(getClientOptions()
            .getBoolean(ClientOptions.DISPLAY_MAP_CONTROLS));
        closeMenus();
        clearGotoPath();
        resetMenuBar();
        resetMapZoom(); // This should refresh the map
        setActiveUnit(active);
        if (getActiveUnit() != null) {
            centerActiveUnit();
        } else {
            setViewMode(GUI.VIEW_TERRAIN_MODE);
            setSelectedTile(tile);
        }
    }
        
    /**
     * {@inheritDoc}
     */
    @Override
    public void removeInGameComponents() {
        canvas.removeInGameComponents();
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
    public void showOpeningVideo(final String userMsg) {
        canvas.closeMenus();
        final Video video = ImageLibrary.getVideo("video.opening");
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
     * {@inheritDoc}
     */
    @Override
    public void startGUI(final Dimension desiredWindowSize) {
        final ClientOptions opts = getClientOptions();
        this.mapViewer = new MapViewer(getFreeColClient());
        this.canvas = new Canvas(getFreeColClient(), graphicsDevice, this,
                                 desiredWindowSize, mapViewer);
        this.tileViewer = new TileViewer(getFreeColClient());

        // Now that there is a canvas, prepare for language changes.
        opts.getOption(ClientOptions.LANGUAGE, LanguageOption.class)
            .addPropertyChangeListener((PropertyChangeEvent e) -> {
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
        // No longer doing anything special for pmoffscreen et al as
        // changing these in-game does not change the now initialized
        // graphics pipeline.

        logger.info("GUI started.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startMapEditorGUI() {
        canvas.startMapEditorGUI();
    }


    // Animation handling

    /**
     * {@inheritDoc}
     */
    @Override
    public void animateUnitAttack(Unit attacker, Unit defender,
                                  Tile attackerTile, Tile defenderTile,
                                  boolean success) {
        // Note: we used to focus the map on the unit even when
        // animation is off as long as the center-active-unit option
        // was set.  However IR#115 requested that if animation is off
        // that we display nothing so as to speed up the other player
        // moves as much as possible.
        final FreeColClient fcc = getFreeColClient();
        if (fcc.getAnimationSpeed(attacker.getOwner()) <= 0
            && fcc.getAnimationSpeed(defender.getOwner()) <= 0) return;

        Animations.unitAttack(fcc, attacker, defender,
                              attackerTile, defenderTile, success);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void animateUnitMove(Unit unit, Tile srcTile, Tile dstTile) {
        // Note: we used to focus the map on the unit even when
        // animation is off as long as the center-active-unit option
        // was set.  However IR#115 requested that if animation is off
        // that we display nothing so as to speed up the other player
        // moves as much as possible.
        final FreeColClient fcc = getFreeColClient();
        if (fcc.getAnimationSpeed(unit.getOwner()) <= 0) return;

        Animations.unitMove(fcc, unit, srcTile, dstTile);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void executeWithUnitOutForAnimation(Unit unit, Tile sourceTile,
                                               OutForAnimationCallback r) {
        invokeNowOrWait(() -> {
                requireFocus(sourceTile);
                paintImmediately();
                mapViewer.executeWithUnitOutForAnimation(unit, sourceTile, r);
            });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Point getAnimationPosition(int labelWidth,int labelHeight,
                                      Point tileP) {
        return mapViewer.calculateUnitLabelPositionInTile(labelWidth, labelHeight, tileP);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getAnimationScale() {
        return mapViewer.getImageLibrary().getScaleFactor();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Rectangle getAnimationTileBounds(Tile tile) {
        return mapViewer.calculateTileBounds(tile);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Point getAnimationTilePosition(Tile tile) {
        return mapViewer.calculateTilePosition(tile, false);
    }


    // Dialog primitives

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
                           ImageIcon icon, String okKey, String cancelKey) {
        return canvas.showConfirmDialog(tile,
            Utility.localizedTextArea(template), icon, okKey, cancelKey);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected <T> T getChoice(Tile tile, Object explain, ImageIcon icon,
                           String cancelKey, List<ChoiceItem<T>> choices) {
        return canvas.showChoiceDialog(tile, explain, icon,
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


    // Focus control

    /**
     * {@inheritDoc}
     */
    @Override
    public Tile getFocus() {
        return mapViewer.getFocus();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean requestFocusForSubPanel() {
        return canvas.getShowingSubPanel().requestFocusInWindow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean requestFocusInWindow() {
        return canvas.requestFocusInWindow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean requireFocus(Tile tile) {
        // Account for the ALWAYS_CENTER client option.
        final boolean required = getClientOptions()
            .getBoolean(ClientOptions.ALWAYS_CENTER);
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
    public void setFocus(Tile tileToFocus) {
        mapViewer.setFocus(tileToFocus);
        canvas.refresh();
    }


    // General GUI manipulation

    /**
     * {@inheritDoc}
     */
    @Override
    public void paintImmediately() {
        paintImmediately(canvas.getBounds());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void paintImmediately(Rectangle rectangle) {
        canvas.paintImmediately(rectangle);
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
    @Override
    public void refreshTile(Tile tile) {
        if (tile.getX() >= 0 && tile.getY() >= 0) {
            canvas.repaint(mapViewer.calculateTileBounds(tile));
        }
    }
    

    // Goto-path handling

    /**
     * {@inheritDoc}
     */
    @Override
    public void activateGotoPath() {
        Unit unit = getActiveUnit();
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
            updateGotoPath((pt == null) ? null
                : canvas.convertToMapTile(pt.x, pt.y));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearGotoPath() {
        canvas.stopGoto();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void traverseGotoPath() {
        final Unit unit = getActiveUnit();
        if (unit == null || !canvas.isGotoStarted()) return;

        final PathNode path = canvas.getGotoPath();
        canvas.stopGoto();
        if (path == null) {
            igc().clearGotoOrders(unit);
        } else {
            igc().goToTile(unit, path);
            // FIXME? Unit may have been deselected on reaching destination
            setActiveUnit(unit);
        }
        canvas.updateUnitPath();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateGotoPath(Tile tile) {
        final Unit unit = getActiveUnit();
        if (tile == null || unit == null) {
            canvas.stopGoto();
            return;
        }

        if (!canvas.isGotoStarted()) return;

        // Do nothing if the tile has not changed.
        PathNode oldPath = canvas.getGotoPath();
        Tile lastTile = (oldPath == null) ? null
            : oldPath.getLastNode().getTile();
        if (lastTile == tile) return;

        // Do not show a path if it will be invalid, avoiding calling
        // the expensive path finder if possible.
        PathNode newPath = (unit.getTile() == tile
            || !tile.isExplored()
            || !unit.getSimpleMoveType(tile).isLegal()) ? null
            : unit.findPath(tile);
        canvas.setGotoPath(newPath);
    }


    // MapControls handling

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

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateMapControls() {
        if (mapControls != null) mapControls.update();
    }

    /**
     * {@inheritDoc}
     */
    @Override
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


    // Menu handling
    
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
    public void resetMenuBar() {
        getFreeColClient().updateActions();
        canvas.resetMenuBar();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateMenuBar() {
        getFreeColClient().updateActions();
        canvas.updateMenuBar();
    }


    // Tile image manipulation

    /**
     * {@inheritDoc}
     */
    @Override
    public BufferedImage createTileImageWithOverlayAndForest(TileType type,
                                                             Dimension size) {
        return tileViewer.createTileImageWithOverlayAndForest(type, size);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BufferedImage createTileImageWithBeachBorderAndItems(Tile tile) {
        return tileViewer.createTileImageWithBeachBorderAndItems(tile);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BufferedImage createTileImage(Tile tile, Player player) {
        return tileViewer.createTileImage(tile, player);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BufferedImage createColonyTileImage(Tile tile, Colony colony) {
        return tileViewer.createColonyTileImage(tile, colony);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void displayColonyTiles(Graphics2D g, Tile[][] tiles, Colony colony) {
        tileViewer.displayColonyTiles(g, tiles, colony);
    }


    // View mode handling

    /**
     * {@inheritDoc}
     */
    @Override
    public int getViewMode() {
        return (mapViewer == null) ? -1 : mapViewer.getViewMode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setViewMode(int newViewMode) {
        mapViewer.changeViewMode(newViewMode);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Unit getActiveUnit() {
        return (mapViewer == null) ? null : mapViewer.getActiveUnit();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setActiveUnit(Unit unit) {
        final Unit old = getActiveUnit();

        Tile tile = null;
        if (unit == null || (tile = unit.getTile()) == null) {
            canvas.stopGoto();
        }
        mapViewer.setActiveUnit(unit);
        // Automatic mode switch when switching to/from null active unit
        if (unit != null && old == null) {
            setViewMode(GUI.MOVE_UNITS_MODE);
            // Bring the selected tile along with the unit
            if (tile != getSelectedTile()) {
                setSelectedTile(tile, tile != null
                    && getClientOptions().getBoolean(ClientOptions.JUMP_TO_ACTIVE_UNIT));
            }
        } else if (unit == null && old != null) {
            tile = getSelectedTile();
            if (tile != null) setViewMode(GUI.VIEW_TERRAIN_MODE);
        }

        updateMapControls();
        updateMenuBar();
        
        // TODO: why do we have to refresh the entire canvas?
        if (unit != null && !getMyPlayer().owns(unit)) {
            canvas.refresh();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void centerActiveUnit() {
        final Unit active = getActiveUnit();
        if (active != null && active.getTile() != null) {
            setFocus(active.getTile());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Tile getSelectedTile() {
        return mapViewer.getSelectedTile();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSelectedTile(Tile newTile) {
        setSelectedTile(newTile, false);
        updateMapControls();
        updateMenuBar();
    }


    // Zoom control

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canZoomInMap() {
        return mapViewer != null && !mapViewer.isAtMaxMapScale();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canZoomOutMap() {
        return mapViewer != null && !mapViewer.isAtMinMapScale();
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


    // Highest level panel and dialog handling

    /**
     * {@inheritDoc}
     */
    @Override
    public void closePanel(String panel) {
        canvas.closePanel(panel);
    }

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
    public void closeStatusPanel() {
        canvas.closeStatusPanel();
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
    public void displayChat(Player player, String message,
                            boolean privateChat) {
        canvas.displayChat(new GUIMessage(
            player.getName() + ": " + message, player.getNationColor()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void displayObject(FreeColObject fco) {
        // TODO: Improve OO.
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
    public void displayStartChat(Player player, String message,
                                 boolean privateChat) {
        canvas.displayStartChat(player.getName(), message, privateChat);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isClientOptionsDialogShowing() {
        return canvas != null && canvas.isClientOptionsDialogShowing();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isShowingSubPanel() {
        return canvas != null && canvas.getShowingSubPanel() != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onClosingErrorPanel(Runnable callback) {
        return canvas != null && canvas.onClosingErrorPanel(callback);
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
    @Override
    public void removeComponent(Component component) {
        canvas.remove(component);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeDialog(FreeColDialog<?> fcd) {
        canvas.dialogRemove(fcd);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restoreSavedSize(Component comp, Dimension size) {
        canvas.restoreSavedSize(comp, size);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showAboutPanel() {
        canvas.showAboutPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BuildQueuePanel showBuildQueuePanel(Colony colony) {
        return canvas.showBuildQueuePanel(colony);
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
            resetMenuBar();
            if (group != null) {
                // Immediately redraw the minimap if that was updated.
                updateMapControls();
            }
        }
        FreeColClient fcc = getFreeColClient();
        if (fcc.isMapEditor()) {
            startMapEditorGUI();
        } else if (fcc.isInGame()) {
            ; // do nothing
        } else {
            showMainPanel(null); // back to the main panel
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ColonyPanel showColonyPanel(Colony colony, Unit unit) {
        return canvas.showColonyPanel(colony, unit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showColopediaPanel(String nodeId) {
        canvas.showColopediaPanel(nodeId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
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

    /**
     * {@inheritDoc}
     */
    @Override
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
    public OptionGroup showDifficultyDialog(Specification spec,
                                            OptionGroup group,
                                            boolean editable) {
        return canvas.showDifficultyDialog(spec, group, editable);
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
    protected void showErrorMessage(String message, Runnable callback) {
        canvas.showErrorMessage(message, callback);
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
    public OptionGroup showGameOptionsDialog(boolean editable) {
        return canvas.showGameOptionsDialog(editable);
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
    public void showIndianSettlement(IndianSettlement indianSettlement) {
        canvas.showIndianSettlementPanel(indianSettlement);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showInformationMessage(FreeColObject displayObject,
                                       StringTemplate template) {
        super.showInformationMessage(displayObject, template);
        ImageIcon icon = null;
        Tile tile = null;
        if (displayObject instanceof Settlement) {
            icon = new ImageIcon(imageLibrary.getScaledSettlementImage((Settlement)displayObject));
            tile = ((Settlement)displayObject).getTile();
        } else if (displayObject instanceof Tile) {
            icon = null;
            tile = (Tile)displayObject;
        } else if (displayObject instanceof Unit) {
            icon = new ImageIcon(imageLibrary.getScaledUnitImage((Unit)displayObject));
            tile = ((Unit)displayObject).getTile();
        }
        canvas.showInformationMessage(displayObject, tile, icon, template);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public File showLoadDialog(File directory, String extension) {
        return canvas.showLoadDialog(directory, extension);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LoadingSavegameInfo showLoadingSavegameDialog(boolean publicServer,
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
    public void showMainTitle() {
        canvas.mainTitle();
        playSound("sound.intro.general");
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
    public void showNativeDemandDialog(Unit unit, Colony colony,
                                       GoodsType type, int amount,
                                       DialogHandler<Boolean> handler) {
        canvas.showNativeDemandDialog(unit, colony, type, amount, handler);
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
    public void showNewPanel(Specification spec) {
        canvas.showNewPanel(spec);
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void showPurchasePanel() {
        canvas.showPurchasePanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
    public void showReportLabourDetailPanel(UnitType unitType,
            java.util.Map<UnitType, java.util.Map<Location, Integer>> data,
            TypeCountMap<UnitType> unitCount, List<Colony> colonies) {
        canvas.showReportLabourDetailPanel(unitType, data, unitCount,
                                           colonies);
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
    public String showRiverStyleDialog(List<String> styles) {
        return canvas.showRiverStyleDialog(styles);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public File showSaveDialog(File directory, String defaultName) {
        return canvas.showSaveDialog(directory, defaultName);
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
    public Location showSelectDestinationDialog(Unit unit) {
        return canvas.showSelectDestinationDialog(unit);
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
    public void showStatisticsPanel(java.util.Map<String, String> serverStats,
                                    java.util.Map<String, String> clientStats) {
        canvas.showStatisticsPanel(serverStats, clientStats);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showStatusPanel(String message) {
        canvas.showStatusPanel(message);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showTilePanel(Tile tile) {
        canvas.showTilePanel(tile);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showTilePopUpAtSelectedTile() {
        Tile tile = mapViewer.getSelectedTile();
        if (tile == null) return;
        canvas.showTilePopup(tile, mapViewer.calculateTilePosition(tile, true));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TradeRouteInputPanel showTradeRouteInputPanel(TradeRoute newRoute) {
        return canvas.showTradeRouteInputPanel(newRoute);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showTradeRoutePanel(Unit unit) {
        canvas.showTradeRoutePanel(unit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
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

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean showWarehouseDialog(Colony colony) {
        return canvas.showWarehouseDialog(colony);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showWorkProductionPanel(Unit unit) {
        canvas.showWorkProductionPanel(unit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateEuropeanSubpanels() {
        canvas.updateEuropeanSubpanels();
    }
}
