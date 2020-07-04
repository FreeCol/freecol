/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.MapTransform;
import net.sf.freecol.client.gui.animation.Animation;
import net.sf.freecol.client.gui.animation.Animations;
// Special panels and dialogs
import net.sf.freecol.client.gui.dialog.FreeColDialog;
import net.sf.freecol.client.gui.dialog.Parameters;
import net.sf.freecol.client.gui.panel.ColonyPanel;
import net.sf.freecol.client.gui.panel.ColorChooserPanel;
import net.sf.freecol.client.gui.panel.FreeColPanel;
import net.sf.freecol.client.gui.panel.MapControls;
import net.sf.freecol.client.gui.panel.report.LabourData.UnitData;
import net.sf.freecol.client.gui.panel.InformationPanel;

import net.sf.freecol.client.gui.panel.Utility;
import net.sf.freecol.client.gui.plaf.FreeColLookAndFeel;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.metaserver.ServerInfo;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Constants.IndianDemandAction;
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
import static net.sf.freecol.common.util.StringUtils.*;
import net.sf.freecol.common.util.Utils;

/**
 * A wrapper providing functionality for the overall GUI using Java Swing.
 */
public class SwingGUI extends GUI {

    /** Number of pixels that must be moved before a goto is enabled. */
    private static final int DRAG_THRESHOLD = 16;

    /** The graphics device to display to. */
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

    /** The various sorts of map controls. */
    private MapControls mapControls;

    /** The canvas that implements much of the functionality. */
    private Canvas canvas;

    /** The widgets wrapper that handles specific panels and dialogs. */
    private Widgets widgets;

    /** Where the map was drag-clicked. */
    private Point dragPoint;

    /** The splash screen. */
    private SplashScreen splash;


    /**
     * Create the GUI.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param scaleFactor The scale factor for the GUI.
     */
    public SwingGUI(FreeColClient freeColClient, float scaleFactor) {
        super(freeColClient, scaleFactor);

        this.graphicsDevice = Utils.getGoodGraphicsDevice();
        if (this.graphicsDevice == null) {
            FreeCol.fatal(logger, "Could not find a GraphicsDevice!");
        }
        this.tileViewer = new TileViewer(freeColClient);
        // Defer remaining initializations, possibly to startGUI
        this.mapViewer = null;
        this.mapControls = null;
        this.canvas = null;
        this.widgets = null;
        this.dragPoint = null;
        this.splash = null;
        logger.info("GUI constructed using scale factor " + scaleFactor);
    }


    // Internals

    /**
     * Perform a single animation.
     *
     * @param a The {@code Animation} to perform.
     */
    private void animate(Animation a) {
        // The tile must be visible
        List<Tile> tiles = a.getTiles();
        final Tile tile = tiles.get(0);
        if (!this.mapViewer.onScreen(tile)) {
            this.mapViewer.changeFocus(tile);
            paintImmediately();
        }

        // Calculate the union of the bounds for all the tiles in the
        // animation, this is the area that will need to be repainted
        // as the animation progresses 
        Rectangle bounds = this.mapViewer.calculateTileBounds(tile);
        for (Tile t : tiles.subList(1, tiles.size())) {
            bounds = bounds.union(this.mapViewer.calculateTileBounds(t));
        }
        
        // Get the unit label, add to canvas if not already there
        final Unit unit = a.getUnit();
        boolean newLabel = !this.mapViewer.isOutForAnimation(unit);
        JLabel unitLabel = this.mapViewer.enterUnitOutForAnimation(unit, tile);
        if (newLabel) this.canvas.animationLabel(unitLabel, true);

        // Define a callback to wrap Canvas.paintImmediately(Rectangle)
        final Canvas can = this.canvas;
        final Rectangle aBounds = bounds;
        final Animations.Procedure painter = new Animations.Procedure() {
                public void execute() {
                    can.paintImmediately(aBounds);
                }
            };

        try { // Delegate to the animation
            a.executeWithLabel(unitLabel, painter);
            
        } finally { // Make sure we release the label again
            this.mapViewer.releaseUnitOutForAnimation(unit);
            
            if (!this.mapViewer.isOutForAnimation(unit)) {
                this.canvas.animationLabel(unitLabel, false);
            }
        }
    }
    
    /**
     * Perform some animations.
     *
     * @param animations The {@code Animation}s to perform.
     */
    private void animations(final List<Animation> animations) {
        if (animations.isEmpty()) return;

        // Special case for first animation, which should respect the
        // ALWAYS_CENTER option.  We assume the others remain sufficiently
        // visible because calling paintImmediately every time would be slow
        final boolean center = getClientOptions()
            .getBoolean(ClientOptions.ALWAYS_CENTER);
        Tile first = animations.get(0).getTiles().get(0);
        if (!this.mapViewer.onScreen(first)
            || (center && first != getFocus())) {
            this.mapViewer.changeFocus(first);
            paintImmediately();
        }
           
        invokeNowOrWait(() -> {
                for (Animation a : animations) animate(a);
                refresh();
            });
    }

    /**
     * Gets the point at which the map was clicked for a drag.
     *
     * @return The Point where the mouse was initially clicked.
     */
    private Point getDragPoint() {
        return this.dragPoint;
    }

    /**
     * Sets the point at which the map was clicked for a drag.
     *
     * @param x The mouse x position.
     * @param y The mouse y position.
     */
    private void setDragPoint(int x, int y) {
        this.dragPoint = new Point(x, y);
    }

    /**
     * Change the view mode.
     *
     * Always stop the blinking cursor if leaving MOVE_UNITS mode,
     * but leave turning it on to changeActiveUnit, as there is no
     * point enabling it if the active unit is null.     
     *
     * @param newViewMode The new {@code ViewMode}.
     * @return True if the view mode changed.
     */
    private boolean changeViewMode(ViewMode newViewMode) {
        ViewMode oldViewMode = getViewMode();
        if (newViewMode == oldViewMode) return false;
        this.mapViewer.setViewMode(newViewMode);
        return true;
    }

    /**
     * Change the active unit.
     *
     * If the unit changes, cancel any current gotos.
     *
     * @param newUnit The new active {@code Unit}.
     * @return True if the active unit changed.
     */
    private boolean changeActiveUnit(Unit newUnit) {
        final Unit oldUnit = getActiveUnit();
        if (newUnit == oldUnit) return false;
        
        this.mapViewer.setActiveUnit(newUnit);
        clearGotoPath();
        return true;
    }

    /**
     * Change the selected tile.
     *
     * Also moves the focus tile to the selected tile if it differs
     * and either is not on screen, or with the focus override.
     *
     * When the selected tile moves, the cursor needs to be redrawn, so
     * a refresh of the old (if any) and new (if any) tile occurs.
     *
     * @param newTile The new {@code Tile} to select.
     * @param refocus If true force a refocus on all tile changes.
     * @return True if the tile was changed.
     */
    private boolean changeSelectedTile(Tile newTile, boolean refocus) {
        final Tile oldTile = getSelectedTile();
        final Tile oldFocus = getFocus();
        refocus = newTile != null
            && newTile != oldFocus
            && (oldFocus == null || refocus
                || !this.mapViewer.onScreen(newTile));
        if (refocus) setFocus(newTile);
        if (newTile == oldTile) return false;
        this.mapViewer.setSelectedTile(newTile);
        if (oldTile != null) refreshTile(oldTile);
        if (newTile != null) refreshTile(newTile);
        return true;
    }

    /**
     * Update the path for the active unit.
     */
    private void updateUnitPath() {
        final Unit active = getActiveUnit();
        if (active == null) return;

        Location destination = active.getDestination();
        PathNode path = null;
        if (destination != null
            && !((FreeColGameObject)destination).isDisposed()
            && !Map.isSameLocation(active.getLocation(), destination)) {
            try {
                path = active.findPath(destination);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Path fail", e);
                active.setDestination(null);
            }
        }
        setUnitPath(path);
    }

    /**
     * Is mouse movement differnce above the drag threshold?
     *
     * @param x The new mouse x position.
     * @param y The new mouse y position.
     * @return True if the mouse has been dragged.
     */
    public boolean isDrag(int x, int y) {
        final Point drag = getDragPoint();
        if (drag == null) return false;
        int deltaX = Math.abs(x - drag.x);
        int deltaY = Math.abs(y - drag.y);
        return deltaX >= DRAG_THRESHOLD || deltaY >= DRAG_THRESHOLD;
    }

    /**
     * Update the current goto to a given tile.
     *
     * @param tile The new goto {@code Tile}.
     */     
    private void updateGotoTile(Tile tile) {
        final Unit unit = getActiveUnit();
        if (tile == null || unit == null) {
            clearGotoPath();
        } else if (canvas.isGotoStarted()) {
            // Do nothing if the tile has not changed.
            PathNode oldPath = mapViewer.getGotoPath();
            Tile lastTile = (oldPath == null) ? null
                : oldPath.getLastNode().getTile();
            if (lastTile == tile) return;

            // Do not show a path if it will be invalid, avoiding calling
            // the expensive path finder if possible.
            PathNode newPath = (unit.getTile() == tile
                || !tile.isExplored()
                || !unit.getSimpleMoveType(tile).isLegal()) ? null
                : unit.findPath(tile);
            mapViewer.changeGotoPath(newPath);
        }
    }

    private void resetMapZoom() {
        //super.resetMapZoom();
        this.mapViewer.resetMapScale();
        refresh();
    }
   
    // Implement GUI

    // Simple accessors

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
        canvas.toggleFrame();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void displaySplashScreen(final InputStream splashStream) {
        if (splashStream != null) {
            try {
                this.splash = new SplashScreen(this.graphicsDevice,
                                               splashStream);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Splash screen failure", e);
            }
            this.splash.setVisible(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void hideSplashScreen() {
        if (this.splash != null) {
            this.splash.setVisible(false);
            this.splash = null;
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
    public void quitGUI() {
        if (canvas != null) {
            canvas.quit();
        }
    }

    /** 
     * {@inheritDoc}
     */
    @Override
    public void reconnectGUI(Unit active, Tile tile) {
        canvas.requestFocusInWindow();
        canvas.initializeInGame();
        enableMapControls(getClientOptions()
            .getBoolean(ClientOptions.DISPLAY_MAP_CONTROLS));
        closeMenus();
        clearGotoPath();
        resetMenuBar();
        resetMapZoom(); // This should refresh the map
        // Update the view, somehow.  Try really hard to find a tile
        // to focus on
        if (active != null) {
            changeView(active);
            if (tile == null) {
                tile = active.getTile();
                if (tile == null) {
                    tile = active.getOwner().getFallbackTile();
                }
            }
        } else if (tile != null) {
            changeView(tile);
        } else {
            changeView((Unit)null);
        }
        this.mapViewer.changeFocus(tile);
    }
        
    /**
     * {@inheritDoc}
     */
    @Override
    public void removeInGameComponents() {
        changeActiveUnit(null);
        changeSelectedTile(null, false);
        canvas.removeInGameComponents();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showOpeningVideo(final String userMsg) {
        canvas.playVideo("video.opening",
                         !getSoundController().canPlaySound(),
                         () -> {
                             playSound("sound.intro.general");
                             showMainPanel(userMsg);
                         });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startGUI(final Dimension desiredWindowSize) {
        final FreeColClient fcc = getFreeColClient();
        final ClientOptions opts = getClientOptions();
        this.mapViewer = new MapViewer(fcc);
        this.mapControls = MapControls.newInstance(fcc);
        this.canvas = new Canvas(getFreeColClient(), graphicsDevice,
                                 desiredWindowSize, this.mapViewer,
                                 this.mapControls);
        this.widgets = new Widgets(fcc, mapViewer.getImageLibrary(),
                                   this.canvas);

        // Now that there is a canvas, prepare for language changes.
        opts.getOption(ClientOptions.LANGUAGE, LanguageOption.class)
            .addPropertyChangeListener((PropertyChangeEvent e) -> {
                Language language = (Language)e.getNewValue();
                logger.info("Set language to: " + language);
                if (Messages.AUTOMATIC.equalsIgnoreCase(language.getKey())) {
                    showInformationPanel("info.autodetectLanguageSelected");
                } else {
                    Locale l = language.getLocale();
                    Messages.loadMessageBundle(l);
                    Messages.loadModMessageBundle(l);
                    showInformationPanel(StringTemplate
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
        resetMapZoom(); // Reset zoom to the default
        canvas.startMapEditorGUI();
        canvas.showMapEditorTransformPanel();
    }


    // Animation handling

    /**
     * {@inheritDoc}
     */
    @Override
    public void animateUnitAttack(Unit attacker, Unit defender,
                                  Tile attackerTile, Tile defenderTile,
                                  boolean success) {
        animations(Animations.unitAttack(getFreeColClient(),
                                         attacker, defender,
                                         attackerTile, defenderTile,
                                         success, this.mapViewer.getScale()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void animateUnitMove(Unit unit, Tile srcTile, Tile dstTile) {
        animations(Animations.unitMove(getFreeColClient(),
                                       unit, srcTile, dstTile,
                                       this.mapViewer.getScale()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Point getAnimationPosition(JLabel unitLabel, Tile tile) {
        return this.mapViewer.getAnimationPosition(unitLabel, tile);
    }


    // Dialog primitives

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean confirm(String textKey, String okKey, String cancelKey) {
        return widgets.showConfirmDialog(null, StringTemplate.key(textKey),
                                        null, okKey, cancelKey);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean confirm(Tile tile, StringTemplate tmpl, ImageIcon icon,
                           String okKey, String cancelKey) {
        return widgets.showConfirmDialog(tile, tmpl, icon, okKey, cancelKey);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected <T> T getChoice(Tile tile, StringTemplate tmpl, ImageIcon icon,
                              String cancelKey, List<ChoiceItem<T>> choices) {
        return widgets.showChoiceDialog(tile, tmpl, icon, cancelKey, choices);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getInput(Tile tile, StringTemplate tmpl, String defaultValue,
                           String okKey, String cancelKey) {
        return widgets.showInputDialog(tile, tmpl, defaultValue,
                                       okKey, cancelKey);
    }


    // Focus control

    /**
     * {@inheritDoc}
     */
    @Override
    public Tile getFocus() {
        return this.mapViewer.getFocus();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFocus(Tile tileToFocus) {
        this.mapViewer.changeFocus(tileToFocus);
        canvas.refresh();
    }


    // General GUI manipulation

    /**
     * {@inheritDoc}
     */
    @Override
    public void paintImmediately() {
        canvas.paintImmediately(canvas.getBounds());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void refresh() {
        this.mapViewer.forceReposition();
        canvas.refresh();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void refreshTile(Tile tile) {
        if (tile.getX() >= 0 && tile.getY() >= 0) {
            canvas.repaint(this.mapViewer.calculateTileBounds(tile));
        }
    }
    

    // Path handling

    /**
     * {@inheritDoc}
     */
    @Override
    public void setUnitPath(PathNode path) {
        this.mapViewer.setUnitPath(path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void activateGotoPath() {
        Unit unit = getActiveUnit();
        if (unit == null) return;

        // Enter "goto mode" if not already activated; otherwise cancel it
        if (canvas.isGotoStarted()) {
            clearGotoPath();
        } else {
            canvas.startGoto();

            // Draw the path to the current mouse position, if the
            // mouse is over the screen; see also
            // CanvasMouseMotionListener.
            Point pt = canvas.getMousePosition();
            updateGotoTile((pt == null) ? null
                : canvas.convertToMapTile(pt.x, pt.y));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearGotoPath() {
        canvas.stopGoto();
        updateUnitPath();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isGotoStarted() {
        return canvas.isGotoStarted();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void performGoto(Tile tile) {
        if (canvas.isGotoStarted()) canvas.stopGoto();

        final Unit active = getActiveUnit();
        if (active == null) return;

        if (tile != null && active.getTile() != tile) {
            canvas.startGoto();
            updateGotoTile(tile);
            traverseGotoPath();
        }
        updateUnitPath();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void performGoto(int x, int y) {
        performGoto(canvas.convertToMapTile(x, y));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void traverseGotoPath() {
        final Unit unit = getActiveUnit();
        if (unit == null || !canvas.isGotoStarted()) return;

        final PathNode path = canvas.stopGoto();
        if (path == null) {
            igc().clearGotoOrders(unit);
        } else {
            igc().goToTile(unit, path);
        }
        // Only update the path if the unit is still active
        if (unit == getActiveUnit()) updateUnitPath();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateGoto(int x, int y, boolean start) {
        if (start && isDrag(x, y)) {
            canvas.startGoto();
        }
        if (canvas.isGotoStarted()) {
            updateGotoTile(canvas.convertToMapTile(x, y));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void prepareDrag(int x, int y) {
        if (canvas.isGotoStarted()) canvas.stopGoto();
        setDragPoint(x, y);
        canvas.requestFocus();
    }
    
    
    // MapControls

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canZoomInMapControls() {
        if (this.mapControls == null) return false;
        return this.mapControls.canZoomInMapControls();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canZoomOutMapControls() {
        if (this.mapControls == null) return false;
        return this.mapControls.canZoomOutMapControls();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void enableMapControls(boolean enable) {
        if (this.mapControls == null) return;
        if (enable) {
            this.canvas.addMapControls();
        } else {
            this.canvas.removeMapControls();
        }
        updateMapControls();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void miniMapToggleViewControls() {
        if (this.mapControls == null) return;
        this.mapControls.toggleView();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void miniMapToggleFogOfWarControls() {
        if (this.mapControls == null) return;
        this.mapControls.toggleFogOfWar();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateMapControls() {
        if (this.mapControls == null) return;
        this.mapControls.update(getActiveUnit());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void zoomInMapControls() {
        if (this.mapControls == null) return;
        this.mapControls.zoomIn();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void zoomOutMapControls() {
        if (this.mapControls == null) return;
        this.mapControls.zoomOut();
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
        canvas.resetMenuBar();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateMenuBar() {
        canvas.updateMenuBar();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showPopupMenu(JPopupMenu menu, int x, int y) {
        menu.show(canvas, x, y);
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
    public ViewMode getViewMode() {
        return this.mapViewer.getViewMode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Unit getActiveUnit() {
        return this.mapViewer.getActiveUnit();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Tile getSelectedTile() {
        return this.mapViewer.getSelectedTile();
    }

    /**
     * Finish a view mode change.
     *
     * @param update Update the map controls if true.
     */
    private void changeDone(boolean update) {
        if (getViewMode() == ViewMode.MOVE_UNITS
            && getActiveUnit() != null) {
            this.mapViewer.startCursorBlinking();
        } else {
            this.mapViewer.stopCursorBlinking();
        }
            
        if (update) updateMapControls();
        updateMenuBar();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void changeView(Tile tile) {
        boolean change = changeViewMode(ViewMode.TERRAIN);
        // Do not change active unit, we might come back to it
        change |= changeSelectedTile(tile, getClientOptions()
            .getBoolean(ClientOptions.ALWAYS_CENTER));
        changeDone(change);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void changeView(Unit unit) {
        boolean change = changeViewMode(ViewMode.MOVE_UNITS);
        change |= changeActiveUnit(unit);
        if (unit != null) {
            // Bring the selected tile along with the unit.
            change |= changeSelectedTile(unit.getTile(), true);
        }
        changeDone(change);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void changeView(MapTransform mt) {
        boolean change = changeViewMode(ViewMode.MAP_TRANSFORM);
        // Do not change selected tile
        changeDone(change);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void changeView() {
        boolean change = changeViewMode(ViewMode.END_TURN);
        change |= changeActiveUnit(null);
        change |= changeSelectedTile(null, false);
        changeDone(change);
    }



    // Zoom control

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canZoomInMap() {
        return !this.mapViewer.isAtMaxMapScale();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canZoomOutMap() {
        return !this.mapViewer.isAtMinMapScale();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void zoomInMap() {
        super.zoomInMap();
        this.mapViewer.increaseMapScale();
        refresh();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void zoomOutMap() {
        super.zoomOutMap();
        this.mapViewer.decreaseMapScale();
        refresh();
    }


    // Highest level panel and dialog handling

    /**
     * {@inheritDoc}
     */
    @Override
    public void clickAt(int count, int x, int y) {
        // This could be a drag, which would have already been processed
        // in @see CanvasMouseListener#mouseReleased
        if (count == 1 && isDrag(x, y)) return;

        final Tile tile = canvas.convertToMapTile(x, y);
        if (tile == null) return;
        Unit other = null;

        if (!tile.isExplored()) { // Select unexplored tiles
            changeView(tile);
        } else if (tile.hasSettlement()) { // Pop up settlements if any
            showTileSettlement(tile);
            changeView(tile);
        } else if ((other = this.mapViewer.findUnitInFront(tile)) != null) {
            if (getMyPlayer().owns(other)) {
                // If there is one of the player units present, select it,
                // unless we are on the same tile as the active unit,
                // in which case select the active unit if not in units mode
                // otherwise the unit *after* the active.
                final Unit active = getActiveUnit();
                if (active != null && active.getTile() == tile) {
                    if (getViewMode() != ViewMode.MOVE_UNITS) {
                        other = active;
                    } else {
                        List<Unit> units = tile.getUnitList();
                        while (!units.isEmpty()) {
                            Unit u = units.remove(0);
                            if (u == active) {
                                if (!units.isEmpty()) other = units.remove(0);
                                break;
                            }
                        }
                    }
                }
                changeView(other);
            } else { // Select the tile under the unit if it is not ours
                changeView(tile);
            }
        } else { // Otherwise select the tile in terrain mode on multiclick
            if (count > 1) changeView(tile);
        }
    }    

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
        widgets.closeStatusPanel();
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
            widgets.showColonyPanel((Colony)fco, null);
        } else if (fco instanceof Europe) {
            widgets.showEuropePanel();
        } else if (fco instanceof IndianSettlement) {
            widgets.showIndianSettlementPanel((IndianSettlement)fco);
        } else if (fco instanceof Tile) {
            setFocus((Tile)fco);
        } else if (fco instanceof Unit) {
            Location loc = ((Unit)fco).up();
            if (loc instanceof Colony) {
                widgets.showColonyPanel((Colony)loc, (Unit)fco);
            } else {
                displayObject((FreeColObject)loc);
            }
        } else if (fco instanceof WorkLocation) {
            widgets.showColonyPanel(((WorkLocation)fco).getColony(), null);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void displayStartChat(Player player, String message,
                                 boolean privateChat) {
        widgets.displayStartChat(player.getName(), message, privateChat);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isClientOptionsDialogShowing() {
        return widgets.isClientOptionsDialogShowing();
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
    public void refreshPlayersTable() {
        widgets.refreshPlayersTable();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeComponent(Component component) {
        canvas.remove(component);
        if (!isShowingSubPanel()) canvas.requestFocusInWindow();
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
    public void removeTradeRoutePanel(FreeColPanel panel) {
        canvas.remove(panel);
        widgets.cancelTradeRouteInput();
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
        widgets.showAboutPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColPanel showBuildQueuePanel(Colony colony) {
        return widgets.showBuildQueuePanel(colony);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showCaptureGoodsDialog(final Unit unit, List<Goods> gl,
                                       DialogHandler<List<Goods>> handler) {
        widgets.showCaptureGoodsDialog(unit, gl, handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showChatPanel() {
        if (getFreeColClient().getSinglePlayer()) return; // chat with who?
        widgets.showChatPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showChooseFoundingFatherDialog(final List<FoundingFather> ffs,
                DialogHandler<FoundingFather> handler) {
        widgets.showChooseFoundingFatherDialog(ffs, handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showClientOptionsDialog() {
        OptionGroup group = null;
        try {
            group = widgets.showClientOptionsDialog();
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
        return widgets.showColonyPanel(colony, unit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showColopediaPanel(String nodeId) {
        widgets.showColopediaPanel(nodeId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ColorChooserPanel showColorChooserPanel(ActionListener al) {
        return widgets.showColorChooserPanel(al);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showCompactLabourReport() {
        widgets.showCompactLabourReport();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showCompactLabourReport(UnitData unitData) {
        widgets.showCompactLabourReport(unitData);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> showConfirmDeclarationDialog() {
        return widgets.showConfirmDeclarationDialog();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showDeclarationPanel() {
        widgets.showDeclarationPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OptionGroup showDifficultyDialog(Specification spec,
                                            OptionGroup group,
                                            boolean editable) {
        return widgets.showDifficultyDialog(spec, group, editable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showDumpCargoDialog(Unit unit,
                                    DialogHandler<List<Goods>> handler) {
        widgets.showDumpCargoDialog(unit, handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean showEditOptionDialog(Option option) {
        return widgets.showEditOptionDialog(option);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IndianSettlement showEditSettlementDialog(IndianSettlement settlement) {
        return widgets.showEditSettlementDialog(settlement);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showEmigrationDialog(final Player player,
                                     final boolean fountainOfYouth,
                                     DialogHandler<Integer> handler) {
        widgets.showEmigrationDialog(player, fountainOfYouth, handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showEndTurnDialog(final List<Unit> units,
                                  DialogHandler<Boolean> handler) {
        widgets.showEndTurnDialog(units, handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void showErrorMessage(String message, Runnable callback) {
        widgets.showErrorMessage(message, callback);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showEuropePanel() {
        widgets.showEuropePanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showEventPanel(String header, String image, String footer) {
        widgets.showEventPanel(header, image, footer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showFindSettlementPanel() {
        widgets.showFindSettlementPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showFirstContactDialog(final Player player, final Player other,
                                       final Tile tile, int settlementCount,
                                       DialogHandler<Boolean> handler) {
        widgets.showFirstContactDialog(player, other, tile, settlementCount,
                                       handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OptionGroup showGameOptionsDialog(boolean editable) {
        return widgets.showGameOptionsDialog(editable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showHighScoresPanel(String messageId, List<HighScore> scores) {
        widgets.showHighScoresPanel(messageId, scores);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showIndianSettlement(IndianSettlement indianSettlement) {
        widgets.showIndianSettlementPanel(indianSettlement);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InformationPanel showInformationPanel(FreeColObject displayObject,
                                                 StringTemplate template) {
        super.showInformationPanel(displayObject, template);
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
        return widgets.showInformationPanel(displayObject, tile, icon, template);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public File showLoadDialog(File directory, String extension) {
        return widgets.showLoadDialog(directory, extension);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LoadingSavegameInfo showLoadingSavegameDialog(boolean publicServer,
                                                         boolean singlePlayer) {
        return widgets.showLoadingSavegameDialog(publicServer, singlePlayer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showLogFilePanel() {
        widgets.showLogFilePanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showMainPanel(String userMsg) {
        canvas.showMainPanel();
        if (userMsg != null) showInformationPanel(userMsg);
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
        return widgets.showMapGeneratorOptionsDialog(editable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Dimension showMapSizeDialog() {
        return widgets.showMapSizeDialog();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showModelMessages(List<ModelMessage> modelMessages) {
        widgets.showModelMessages(modelMessages);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showMonarchDialog(final MonarchAction action,
                                  StringTemplate template, String monarchKey,
                                  DialogHandler<Boolean> handler) {
        widgets.showMonarchDialog(action, template, monarchKey, handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showNamingDialog(StringTemplate template,
                                      final String defaultName,
                                      final Unit unit,
                                      DialogHandler<String> handler) {
        widgets.showNamingDialog(template, defaultName, unit, handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showNativeDemandDialog(Unit unit, Colony colony,
                                       GoodsType type, int amount,
                                       DialogHandler<Boolean> handler) {
        widgets.showNativeDemandDialog(unit, colony, type, amount, handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DiplomaticTrade showNegotiationDialog(FreeColGameObject our,
                                                     FreeColGameObject other,
                                                     DiplomaticTrade agreement,
                                                     StringTemplate comment) {
        return widgets.showNegotiationDialog(our, other, agreement, comment);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showNewPanel(Specification spec) {
        widgets.showNewPanel(spec);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Parameters showParametersDialog() {
        return widgets.showParametersDialog();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean showPreCombatDialog(Unit attacker,
                                       FreeColGameObject defender, Tile tile) {
        return widgets.showPreCombatDialog(attacker, defender, tile);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showPurchasePanel() {
        widgets.showPurchasePanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showRecruitPanel() {
        widgets.showRecruitPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showReportCargoPanel() {
        widgets.showReportCargoPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showReportColonyPanel() {
        widgets.showReportColonyPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showReportContinentalCongressPanel() {
        widgets.showReportContinentalCongressPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showReportEducationPanel() {
        widgets.showReportEducationPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showReportExplorationPanel() {
        widgets.showReportExplorationPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showReportForeignAffairPanel() {
        widgets.showReportForeignAffairPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showReportHistoryPanel() {
        widgets.showReportHistoryPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showReportIndianPanel() {
        widgets.showReportIndianPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showReportLabourPanel() {
        widgets.showReportLabourPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showReportLabourDetailPanel(UnitType unitType,
            java.util.Map<UnitType, java.util.Map<Location, Integer>> data,
            TypeCountMap<UnitType> unitCount, List<Colony> colonies) {
        widgets.showReportLabourDetailPanel(unitType, data, unitCount,
                                            colonies);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showReportMilitaryPanel() {
        widgets.showReportMilitaryPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showReportNavalPanel() {
        widgets.showReportNavalPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showReportProductionPanel() {
        widgets.showReportProductionPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showReportReligiousPanel() {
        widgets.showReportReligiousPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showReportRequirementsPanel() {
        widgets.showReportRequirementsPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showReportTradePanel() {
        widgets.showReportTradePanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showReportTurnPanel(List<ModelMessage> messages) {
        widgets.showReportTurnPanel(messages);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String showRiverStyleDialog(List<String> styles) {
        return widgets.showRiverStyleDialog(styles);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public File showSaveDialog(File directory, String defaultName) {
        return widgets.showSaveDialog(directory, defaultName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Dimension showScaleMapSizeDialog() {
        return widgets.showScaleMapSizeDialog();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int showSelectAmountDialog(GoodsType goodsType, int available,
                                      int defaultAmount, boolean needToPay) {
        return widgets.showSelectAmountDialog(goodsType, available,
                                             defaultAmount, needToPay);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Location showSelectDestinationDialog(Unit unit) {
        return widgets.showSelectDestinationDialog(unit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int showSelectTributeAmountDialog(StringTemplate question,
                                             int maximum) {
        return widgets.showSelectTributeAmountDialog(question, maximum);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showServerListPanel(List<ServerInfo> serverList) {
        canvas.closeMenus();
        widgets.showServerListPanel(serverList);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showStartGamePanel(Game game, Player player,
                                   boolean singlePlayerMode) {
        if (game == null) {
            logger.warning("StartGamePanel requires game != null.");
        } else if (player == null) {
            logger.warning("StartGamePanel requires player != null.");
        } else {
            widgets.showStartGamePanel(singlePlayerMode);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showStatisticsPanel(java.util.Map<String, String> serverStats,
                                    java.util.Map<String, String> clientStats) {
        widgets.showStatisticsPanel(serverStats, clientStats);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showStatusPanel(String message) {
        widgets.showStatusPanel(message);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showTilePanel(Tile tile) {
        widgets.showTilePanel(tile);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showTilePopup(int x, int y) {
        final Tile tile = canvas.convertToMapTile(x, y);
        if (tile == null) return;
        if (!canvas.showTilePopup(tile) && tile.isExplored()) {
            widgets.showTilePanel(tile);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColPanel showTradeRouteInputPanel(TradeRoute newRoute) {
        return widgets.showTradeRouteInputPanel(newRoute);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showTradeRoutePanel(Unit unit) {
        widgets.showTradeRoutePanel(unit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showTrainPanel() {
        widgets.showTrainPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showVictoryDialog(DialogHandler<Boolean> handler) {
        widgets.showVictoryDialog(handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean showWarehouseDialog(Colony colony) {
        return widgets.showWarehouseDialog(colony);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showWorkProductionPanel(Unit unit) {
        widgets.showWorkProductionPanel(unit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateEuropeanSubpanels() {
        widgets.updateEuropeanSubpanels();
    }
}
