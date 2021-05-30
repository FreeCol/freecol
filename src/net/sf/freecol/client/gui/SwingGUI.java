/**
 *  Copyright (C) 2002-2021   The FreeCol Team
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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.InputStream;
import java.util.function.Predicate;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.Map;

import javax.swing.filechooser.FileFilter;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.MapTransform;
import net.sf.freecol.client.gui.animation.Animation;
import net.sf.freecol.client.gui.animation.Animations;
// Special dialogs and panels
import net.sf.freecol.client.gui.dialog.ClientOptionsDialog;
import net.sf.freecol.client.gui.dialog.FreeColDialog;
import net.sf.freecol.client.gui.dialog.Parameters;
import net.sf.freecol.client.gui.panel.ColonyPanel;
import net.sf.freecol.client.gui.panel.ColorChooserPanel;
import net.sf.freecol.client.gui.panel.FreeColPanel;
import net.sf.freecol.client.gui.panel.MapControls;
import net.sf.freecol.client.gui.panel.report.LabourData.UnitData;
import net.sf.freecol.client.gui.panel.InformationPanel;
import net.sf.freecol.client.gui.panel.PurchasePanel;
import net.sf.freecol.client.gui.panel.RecruitPanel;
import net.sf.freecol.client.gui.panel.StartGamePanel;
import net.sf.freecol.client.gui.panel.StatusPanel;
import net.sf.freecol.client.gui.panel.TradeRouteInputPanel;
import net.sf.freecol.client.gui.panel.TrainPanel;

import net.sf.freecol.client.gui.panel.Utility;
import net.sf.freecol.client.gui.plaf.FreeColLookAndFeel;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.debug.DebugUtils;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColDataFile;
import net.sf.freecol.common.metaserver.ServerInfo;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Constants.IndianDemandAction;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.Direction;
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
import net.sf.freecol.common.resources.ImageCache;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.common.util.Introspector;
import static net.sf.freecol.common.util.StringUtils.*;
import net.sf.freecol.common.util.Utils;


/**
 * A wrapper providing functionality for the overall GUI using Java Swing.
 */
public class SwingGUI extends GUI {

    /** A rough position to place dialogs and panels on the canvas. */
    public static enum PopupPosition {
        ORIGIN,
        CENTERED,
        CENTERED_LEFT,
        CENTERED_RIGHT,
    }

    /** European subpanel classes. */
    private static final List<Class<? extends FreeColPanel>> EUROPE_CLASSES
        = makeUnmodifiableList(RecruitPanel.class,
                               PurchasePanel.class,
                               TrainPanel.class);

    /** Number of pixels that must be moved before a goto is enabled. */
    private static final int DRAG_THRESHOLD = 16;

    /** The graphics device to display to. */
    private final GraphicsDevice graphicsDevice;

    /** A persistent image cache. */
    private final ImageCache imageCache;

    /** The fixed/unscaled image library used by panels et al. */
    private ImageLibrary fixedImageLibrary;
    
   /** The scaled image library used by the map. */
    private ImageLibrary scaledImageLibrary;

    /**
     * This is the TileViewer instance used for tiles in panels.
     * It uses the fixed ImageLibrary.
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

    /** Has a goto operation started? */
    private boolean gotoStarted = false;

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
        this.imageCache = new ImageCache();
        this.scaledImageLibrary = new ImageLibrary(scaleFactor, this.imageCache);
        this.fixedImageLibrary = new ImageLibrary(scaleFactor, this.imageCache);
        this.tileViewer = new TileViewer(freeColClient, fixedImageLibrary);
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
        // Insist that tiles are visible (assume changeFocus centers
        // sufficiently well that if one is good the rest will be)
        final List<Tile> tiles = a.getTiles();
        for (Tile t : tiles) {
            if (!this.mapViewer.onScreen(t)) {
                this.mapViewer.changeFocus(t);
                break;
            }
        }
        // Always completely update the screen before starting the animation
        // as focus may have changed either here or in animations()
        paintImmediately();

        // Calculate the union of the bounds for all the tiles in the
        // animation, this is the area that will need to be repainted
        // as the animation progresses 
        Rectangle bounds = null;
        for (Tile t : tiles) {
            Rectangle r = this.mapViewer.calculateTileBounds(t);
            bounds = (bounds == null) ? r : bounds.union(r);
        }
        // Get the unit label, add to canvas if not already there, and
        // update the animation with the locations for the label for each
        // of the animation's tiles
        final Unit unit = a.getUnit();
        boolean newLabel = !this.mapViewer.isOutForAnimation(unit);
        JLabel unitLabel = this.mapViewer.enterUnitOutForAnimation(unit);
        List<Point> points = transform(tiles, alwaysTrue(),
            (t) -> this.mapViewer.getAnimationPosition(unitLabel, t));
        a.setPoints(points);
        unitLabel.setLocation(points.get(0)); // set location before adding
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
        if (center && first != getFocus()) {
            this.mapViewer.changeFocus(first);
        }
           
        invokeNowOrWait(() -> {
                for (Animation a : animations) animate(a);
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
     * Get the map scale.
     *
     * Used by:
     *
     * @return The scale for the (rescalable) map.
     */
    private final float getMapScale() {
        ImageLibrary lib = this.scaledImageLibrary;
        return (lib == null) ? 1.0f : lib.getScaleFactor();
    }

    /**
     * Get a rough position to place a dialog given a tile which we wish
     * to remain visible.
     *
     * @param tile The {@code Tile} to expose.
     * @return A suitable {@code PopupPosition}.
     */
    private PopupPosition getPopupPosition(Tile tile) {
        if (tile == null) return PopupPosition.CENTERED;
        int where = this.mapViewer.setOffsetFocus(tile);
        return (where > 0) ? PopupPosition.CENTERED_LEFT
            : (where < 0) ? PopupPosition.CENTERED_RIGHT
            : PopupPosition.CENTERED;
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
            && !active.isAtLocation(destination)) {
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
     * Starts a goto operation.
     */
    private void startGoto() {
        this.gotoStarted = true;
        this.canvas.setCursor(Canvas.GO_CURSOR);
        this.mapViewer.changeGotoPath(null);
    }

    /**
     * Stops any ongoing goto operation.
     *
     * @return The old goto path if any.
     */
    private PathNode stopGoto() {
        PathNode ret = (this.gotoStarted) ? this.mapViewer.getGotoPath() : null;
        this.gotoStarted = false;
        this.canvas.setCursor(null);
        this.mapViewer.changeGotoPath(null);
        return ret;
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
        } else if (isGotoStarted()) {
            // Do nothing if the tile has not changed.
            PathNode oldPath = this.mapViewer.getGotoPath();
            Tile lastTile = (oldPath == null) ? null
                : oldPath.getLastNode().getTile();
            if (lastTile == tile) return;

            // Do not show a path if it will be invalid, avoiding calling
            // the expensive path finder if possible.
            PathNode newPath = (unit.getTile() == tile
                || !tile.isExplored()
                || !unit.getSimpleMoveType(tile).isLegal()) ? null
                : unit.findPath(tile);
            this.mapViewer.changeGotoPath(newPath);
        }
    }

    /**
     * Paint the whole canvas now.
     */
    public void paintImmediately() {
        this.canvas.paintImmediately(this.canvas.getBounds());
    }

    /**
     * Schedule a tile to be repainted.
     *
     * @param tile The {@code tile} to paint.
     */
    private void refreshTile(Tile tile) {
        if (tile != null) {
            this.canvas.repaint(this.mapViewer.calculateTileBounds(tile));
        }
    }

    /**
     * Reset the map zoom and refresh the canvas.
     */
    private void resetMapZoom() {
        if (this.scaledImageLibrary.getScaleFactor()
            != ImageLibrary.NORMAL_SCALE) {
            this.mapViewer.changeScale(ImageLibrary.NORMAL_SCALE);
            refresh();
        }
    }
   

    // Implement GUI

    // Simple accessors

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageLibrary getFixedImageLibrary() {
        return this.fixedImageLibrary;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageLibrary getScaledImageLibrary() {
        return this.scaledImageLibrary;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isWindowed() {
        return this.canvas.isWindowed();
    }


    // Invocation methods

    /**
     * {@inheritDoc}
     */
    @Override
    public void invokeNowOrLater(Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void invokeNowOrWait(Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(runnable);
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Client GUI interaction", ex);
            }
        }
    }


    // Initialization and teardown

    /**
     * {@inheritDoc}
     */
    @Override
    public void changeWindowedMode() {
        this.canvas.toggleFrame();
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
        Font font = FontLibrary.createMainFont(fontName);
        if (font == null) {
            throw new FreeColException("Unable to create main font: "
                + fontName);
        }
        FreeColLookAndFeel.installFont(font);
        Utility.initStyleContext(font);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void quitGUI() {
        if (this.canvas != null) {
            this.canvas.quit();
        }
    }

    /** 
     * {@inheritDoc}
     */
    @Override
    public void reconnectGUI(Unit active, Tile tile) {
        this.canvas.requestFocusInWindow();
        this.canvas.initializeInGame();
        enableMapControls(getClientOptions()
            .getBoolean(ClientOptions.DISPLAY_MAP_CONTROLS));
        closeMenus();
        clearGotoPath();
        this.canvas.resetMenuBar();
        resetMapZoom(); // This should refresh the map
        // Update the view, somehow.  Try really hard to find a tile
        // to focus on
        if (active != null) {
            changeView(active, false);
            if (tile == null) {
                tile = active.getTile();
                if (tile == null) {
                    tile = active.getOwner().getFallbackTile();
                }
            }
        } else if (tile != null) {
            changeView(tile);
        } else {
            changeView((Unit)null, false);
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
        this.canvas.removeInGameComponents();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showOpeningVideo(final String userMsg, Runnable callback) {
        final boolean play = getFreeColClient().getSoundController()
            .canPlaySound();
        this.canvas.playVideo("video.opening", !play, callback);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startGUI(final Dimension desiredWindowSize) {
        final FreeColClient fcc = getFreeColClient();
        final ClientOptions opts = getClientOptions();
        this.mapControls = MapControls.newInstance(fcc);
        final ActionListener al = (ActionEvent ae) ->
            this.refreshTile(this.mapViewer.getCursorTile());
        this.mapViewer = new MapViewer(fcc, this.scaledImageLibrary, al);
        this.canvas = new Canvas(getFreeColClient(), this.graphicsDevice,
                                 desiredWindowSize, this.mapViewer,
                                 this.mapControls);
        this.widgets = new Widgets(fcc, this.canvas);

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
        this.mapViewer.startCursorBlinking();
        logger.info("GUI started.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startMapEditorGUI() {
        resetMapZoom(); // Reset zoom to the default
        this.canvas.startMapEditorGUI();
        this.canvas.showMapEditorTransformPanel();
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
                                         success, getMapScale()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void animateUnitMove(Unit unit, Tile srcTile, Tile dstTile) {
        animations(Animations.unitMove(getFreeColClient(),
                                       unit, srcTile, dstTile, getMapScale()));
    }


    // Dialog primitives

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean confirm(Tile tile, StringTemplate tmpl, ImageIcon icon,
                           String okKey, String cancelKey) {
        return this.widgets.confirm(tmpl, icon, okKey, cancelKey,
                                    getPopupPosition(tile));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected <T> T getChoice(Tile tile, StringTemplate tmpl, ImageIcon icon,
                              String cancelKey, List<ChoiceItem<T>> choices) {
        return this.widgets.getChoice(tmpl, icon, cancelKey, choices,
                                      getPopupPosition(tile));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getInput(Tile tile, StringTemplate tmpl, String defaultValue,
                           String okKey, String cancelKey) {
        return this.widgets.getInput(tmpl, defaultValue, okKey, cancelKey,
                                     getPopupPosition(tile));
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
        refresh();
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
        getFreeColClient().toggleClientOption(ClientOptions.MINIMAP_TOGGLE_BORDERS);
        this.mapControls.repaint();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void miniMapToggleFogOfWarControls() {
        if (this.mapControls == null) return;
        getFreeColClient().toggleClientOption(ClientOptions.MINIMAP_TOGGLE_FOG_OF_WAR);
        this.mapControls.repaint();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateMapControls() {
        if (this.mapControls == null) return;
        this.mapControls.update(getViewMode(), getActiveUnit(),
                                getSelectedTile());
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
        this.canvas.closeMenus();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateMenuBar() {
        this.canvas.updateMenuBar();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showPopupMenu(JPopupMenu menu, int x, int y) {
        menu.show(this.canvas, x, y);
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
        if (isGotoStarted()) {
            clearGotoPath();
        } else {
            startGoto();
            
            // Draw the path to the current mouse position, if the
            // mouse is over the screen; see also
            // CanvasMouseMotionListener.
            Point pt = this.canvas.getMousePosition();
            updateGotoTile((pt == null) ? null
                : tileAt(pt.x, pt.y));
            refresh();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearGotoPath() {
        stopGoto();
        updateUnitPath();
        refresh();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isGotoStarted() {
        return this.gotoStarted;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void performGoto(Tile tile) {
        if (isGotoStarted()) stopGoto();

        final Unit active = getActiveUnit();
        if (active != null) {
            if (tile != null && active.getTile() != tile) {
                startGoto();
                updateGotoTile(tile);
                traverseGotoPath();
            }
            updateUnitPath();
        }
        refresh();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void performGoto(int x, int y) {
        performGoto(tileAt(x, y));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void traverseGotoPath() {
        final Unit unit = getActiveUnit();
        if (unit == null || !isGotoStarted()) return;

        final PathNode path = stopGoto();
        if (path == null) {
            igc().clearGotoOrders(unit);
        } else {
            igc().goToTile(unit, path);
        }
        // Only update the path if the unit is still active
        if (unit == getActiveUnit()) updateUnitPath();
        refresh();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateGoto(int x, int y, boolean start) {
        if (start && isDrag(x, y)) {
            startGoto();
        }
        if (isGotoStarted()) {
            updateGotoTile(tileAt(x, y));
        }
        refresh();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void prepareDrag(int x, int y) {
        if (isGotoStarted()) {
            stopGoto();
            refresh();
        }
        setDragPoint(x, y);
        this.canvas.requestFocus();
    }
    

    // Scrolling

    /**
     * {@inheritDoc}
     */
    @Override
    public Direction getScrollDirection(int x, int y, int scrollSpace,
                                        boolean ignoreTop) {
        Direction ret;
        final Dimension size = this.canvas.getSize();
        if (x < scrollSpace && y < scrollSpace) { // Upper-Left
            ret = !ignoreTop ? Direction.NW : Direction.W;
        } else if (x >= size.width - scrollSpace
            && y < scrollSpace) { // Upper-Right
            ret = !ignoreTop ? Direction.NE : Direction.E;
        } else if (x >= size.width - scrollSpace
            && y >= size.height - scrollSpace) { // Bottom-Right
            ret = Direction.SE;
        } else if (x < scrollSpace
            && y >= size.height - scrollSpace) { // Bottom-Left
            ret = Direction.SW;
        } else if (y < scrollSpace) { // Top
            ret = !ignoreTop ? Direction.N : null;
        } else if (x >= size.width - scrollSpace) { // Right
            ret = Direction.E;
        } else if (y >= size.height - scrollSpace) { // Bottom
            ret = Direction.S;
        } else if (x < scrollSpace) { // Left
            ret = Direction.W;
        } else {
            ret = null;
        }
        return ret;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean scrollMap(Direction direction) {
        boolean ret = this.mapViewer.scrollMap(direction);
        refresh();
        return ret;
    }


    // Tile image manipulation

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
    public void displayColonyTiles(Graphics2D g2d, Tile[][] tiles,
                                   Colony colony) {
        this.tileViewer.displayColonyTiles(g2d, tiles, colony);
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
    public void changeView(Unit unit, boolean force) {
        boolean change = changeViewMode(ViewMode.MOVE_UNITS);
        change |= changeActiveUnit(unit);
        if (unit != null) {
            // Bring the selected tile along with the unit.
            change |= changeSelectedTile(unit.getTile(), true);
        }
        changeDone(change || force);
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
        return this.scaledImageLibrary.getScaleFactor() < ImageLibrary.MAX_SCALE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canZoomOutMap() {
        return this.scaledImageLibrary.getScaleFactor() > ImageLibrary.MIN_SCALE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void zoomInMap() {
        float scale = this.scaledImageLibrary.getScaleFactor();
        float newScale = scale + ImageLibrary.SCALE_STEP;
        if (scale < newScale && newScale <= ImageLibrary.MAX_SCALE) {
            this.mapViewer.changeScale(newScale);
            refresh();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void zoomOutMap() {
        float scale = this.scaledImageLibrary.getScaleFactor();
        float newScale = scale - ImageLibrary.SCALE_STEP;
        if (ImageLibrary.MIN_SCALE <= newScale && newScale < scale) {
            this.mapViewer.changeScale(newScale);
            refresh();
        }
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

        final Tile tile = tileAt(x, y);
        if (tile == null) return;
        Unit other = null;

        if (!tile.isExplored()) { // Select unexplored tiles
            changeView(tile);
        } else if (tile.hasSettlement()) { // Pop up settlements if any
            Settlement settlement = tile.getSettlement();
            if (settlement instanceof Colony) {
                if (getMyPlayer().owns(settlement)) {
                    showColonyPanel((Colony)settlement, null);
                } else {
                    DebugUtils.showForeignColony(getFreeColClient(),
                                                 (Colony)settlement);
                }
            } else if (settlement instanceof IndianSettlement) {
                showIndianSettlementPanel((IndianSettlement)settlement);
            }
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
                changeView(other, false);
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
        this.canvas.closePanel(panel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void closeMainPanel() {
        this.canvas.closeMainPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void closeStatusPanel() {
        StatusPanel panel
            = this.canvas.getExistingFreeColPanel(StatusPanel.class);
        if (panel != null) {
            this.canvas.removeFromCanvas(panel);
            this.canvas.requestFocusInWindow();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void displayChat(String sender, String message, Color color,
                            boolean privateChat) {
        this.mapViewer.displayChat(new GUIMessage(sender + ": " + message,
                                                  color));
        refresh();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void displayObject(FreeColObject fco) {
        // TODO: Improve OO.
        if (fco instanceof Colony) {
            showColonyPanel((Colony)fco, null);
        } else if (fco instanceof Europe) {
            showEuropePanel();
        } else if (fco instanceof IndianSettlement) {
            showIndianSettlementPanel((IndianSettlement)fco);
        } else if (fco instanceof Tile) {
            setFocus((Tile)fco);
        } else if (fco instanceof Unit) {
            Location loc = ((Unit)fco).up();
            if (loc instanceof Colony) {
                showColonyPanel((Colony)loc, (Unit)fco);
            } else {
                displayObject((FreeColObject)loc);
            }
        } else if (fco instanceof WorkLocation) {
            showColonyPanel(((WorkLocation)fco).getColony(), null);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void displayStartChat(String sender, String message,
                                 boolean privateChat) {
        StartGamePanel panel
            = this.canvas.getExistingFreeColPanel(StartGamePanel.class);
        if (panel != null) {
            panel.displayChat(sender, message, privateChat);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isClientOptionsDialogShowing() {
        return this.canvas.getExistingFreeColDialog(ClientOptionsDialog.class) != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPanelShowing() {
        return this.canvas != null && this.canvas.getShowingPanel() != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void refresh() {
        this.mapViewer.forceReposition();
        this.canvas.repaint(0, 0, this.canvas.getWidth(), this.canvas.getHeight());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void refreshPlayersTable() {
        StartGamePanel panel
            = this.canvas.getExistingFreeColPanel(StartGamePanel.class);
        if (panel != null) panel.refreshPlayersTable();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeComponent(Component component) {
        this.canvas.remove(component);
        if (!isPanelShowing()) this.canvas.requestFocusInWindow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeDialog(FreeColDialog<?> fcd) {
        this.canvas.dialogRemove(fcd);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeTradeRoutePanel(FreeColPanel panel) {
        this.canvas.remove(panel);
        TradeRouteInputPanel tripPanel
            = this.canvas.getExistingFreeColPanel(TradeRouteInputPanel.class);
        if (tripPanel != null) tripPanel.cancelTradeRoute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restoreSavedSize(Component comp, Dimension size) {
        this.canvas.restoreSavedSize(comp, size);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showTilePopup(Tile tile) {
        if (tile == null || !tile.isExplored()) return;
        TilePopup tp = new TilePopup(getFreeColClient(), tile);
        if (tp.hasItem()) {
            Point point = this.mapViewer.calculateTilePosition(tile, true);
            tp.show(this.canvas, point.x, point.y);
            tp.repaint();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Tile tileAt(int x, int y) {
        return this.mapViewer.convertToMapTile(x, y);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateEuropeanSubpanels() {
        for (Class<? extends FreeColPanel> c: EUROPE_CLASSES) {
            FreeColPanel p = this.canvas.getExistingFreeColPanel(c);
            if (p != null) {
                // TODO: remember how to write generic code, avoid
                // introspection
                try {
                    Introspector.invokeVoidMethod(p, "update");
                } catch (Exception e) {
                    ; // "can not happen"
                }
            }
        }
    }


    // Panel display, usually used just by the associated action,
    // mostly delegated to Widgets

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColPanel showAboutPanel() {
        return this.widgets.showAboutPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColPanel showBuildQueuePanel(Colony colony) {
        return this.widgets.showBuildQueuePanel(colony);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showCaptureGoodsDialog(final Unit unit, List<Goods> gl,
                                       DialogHandler<List<Goods>> handler) {
        this.widgets.showCaptureGoodsDialog(unit, gl, handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColPanel showChatPanel() {
        return (getFreeColClient().getSinglePlayer()) ? null
            : this.widgets.showChatPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showChooseFoundingFatherDialog(final List<FoundingFather> ffs,
                                               DialogHandler<FoundingFather> handler) {
        this.widgets.showChooseFoundingFatherDialog(ffs, handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showClientOptionsDialog() {
        final FreeColClient fcc = getFreeColClient();
        OptionGroup group = null;
        try {
            ClientOptionsDialog dialog
                = new ClientOptionsDialog(fcc, this.canvas.getParentFrame());
            group = this.canvas.showFreeColDialog(dialog, null);
        } finally {
            this.canvas.resetMenuBar();
            if (group != null) {
                // Immediately redraw the minimap if that was updated.
                updateMapControls();
            }
        }
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
    public FreeColPanel showColonyPanel(final Colony colony, Unit unit) {
        if (colony == null) return null;
        final Predicate<Component> pred = (c) ->
            (c instanceof ColonyPanel
                && ((ColonyPanel)c).getColony() == colony);
        ColonyPanel panel = (ColonyPanel)this.canvas.getMatchingComponent(pred);
        if (panel == null) {
            try {
                panel = new ColonyPanel(getFreeColClient(), colony);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Exception in ColonyPanel for "
                    + colony.getId(), e);
                return null;
            }
            this.canvas.showFreeColPanel(panel,
                getPopupPosition(colony.getTile()), true);
        } else {
            panel.requestFocus();
        }
        if (unit != null) panel.setSelectedUnit(unit);
        return panel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColPanel showColopediaPanel(String nodeId) {
        return this.widgets.showColopediaPanel(nodeId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColPanel showColorChooserPanel(ActionListener al) {
        return this.widgets.showColorChooserPanel(al);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColPanel showCompactLabourReport() {
        return this.widgets.showCompactLabourReport();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColPanel showCompactLabourReport(UnitData unitData) {
        return this.widgets.showCompactLabourReport(unitData);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> showConfirmDeclarationDialog() {
        return this.widgets.showConfirmDeclarationDialog();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColPanel showDeclarationPanel() {
        return this.widgets.showDeclarationPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OptionGroup showDifficultyDialog(Specification spec,
                                            OptionGroup group,
                                            boolean editable) {
        OptionGroup ret
            = this.widgets.showDifficultyDialog(spec, group, editable);
        if (ret != null) FreeCol.setDifficulty(ret);
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showDumpCargoDialog(Unit unit,
                                    DialogHandler<List<Goods>> handler) {
        this.widgets.showDumpCargoDialog(unit, getPopupPosition(unit.getTile()),
                                         handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean showEditOptionDialog(Option option) {
        return this.widgets.showEditOptionDialog(option);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IndianSettlement showEditSettlementDialog(IndianSettlement is) {
        return this.widgets.showEditSettlementDialog(is);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showEmigrationDialog(final Player player,
                                     final boolean fountainOfYouth,
                                     DialogHandler<Integer> handler) {
        this.widgets.showEmigrationDialog(player, fountainOfYouth, handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showEndTurnDialog(final List<Unit> units,
                                  DialogHandler<Boolean> handler) {
        this.widgets.showEndTurnDialog(units, handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColPanel showErrorPanel(String message, Runnable callback) {
        return this.widgets.showErrorPanel(message)
            .addClosingCallback(callback);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColPanel showEuropePanel() {
        return this.widgets.showEuropePanel(() -> {
                for (Class<? extends FreeColPanel> c: EUROPE_CLASSES) {
                    FreeColPanel p = this.canvas.getExistingFreeColPanel(c);
                    if (p != null) this.canvas.remove(p);
                }
            });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColPanel showEventPanel(String header, String image,
                                       String footer) {
        return this.widgets.showEventPanel(header, image, footer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColPanel showFindSettlementPanel() {
        return this.widgets.showFindSettlementPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showFirstContactDialog(final Player player, final Player other,
                                       final Tile tile, int settlementCount,
                                       DialogHandler<Boolean> handler) {
        this.widgets.showFirstContactDialog(player, other, tile,
                                            settlementCount,
                                            getPopupPosition(tile), handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OptionGroup showGameOptionsDialog(boolean editable) {
        return this.widgets.showGameOptionsDialog(editable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColPanel showHighScoresPanel(String messageId,
                                            List<HighScore> scores) {
        return this.widgets.showHighScoresPanel(messageId, scores);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColPanel showIndianSettlementPanel(IndianSettlement is) {
        return this.widgets.showIndianSettlementPanel(is,
            getPopupPosition(is.getTile()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColPanel showInformationPanel(FreeColObject displayObject,
                                             StringTemplate template) {
        ImageIcon icon = null;
        Tile tile = null;
        if (displayObject != null) {
            icon = this.fixedImageLibrary.getObjectImageIcon(displayObject);
            tile = (displayObject instanceof Location)
                ? ((Location)displayObject).getTile()
                : null;
        }
        if (getClientOptions().getBoolean(ClientOptions.AUDIO_ALERTS)) {
            playSound("sound.event.alertSound");
        }
        return this.widgets.showInformationPanel(displayObject,
                                                 getPopupPosition(tile),
                                                 icon, template);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public File showLoadDialog(File directory, String extension) {
        FileFilter[] filters = new FileFilter[] {
            FreeColDataFile.getFileFilter(extension)
        };
        File file = null;
        for (;;) {
            file = this.widgets.showLoadDialog(directory, filters);
            if (file == null || file.isFile()) break;
            String err = Messages.message(FreeCol.badFile("error.noSuchFile",
                                                          file));
            showErrorPanel(err, null);
        }
        return file;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LoadingSavegameInfo showLoadingSavegameDialog(boolean publicServer,
                                                         boolean singlePlayer) {
        return this.widgets.showLoadingSavegameDialog(publicServer,
                                                      singlePlayer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColPanel showLogFilePanel() {
        return this.widgets.showLogFilePanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColPanel showMainPanel(String userMsg) {
        FreeColPanel panel = this.canvas.showMainPanel();
        if (userMsg != null) showInformationPanel(userMsg);
        return panel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showMainTitle() {
        this.canvas.mainTitle();
        playSound("sound.intro.general");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OptionGroup showMapGeneratorOptionsDialog(boolean editable) {
        return this.widgets.showMapGeneratorOptionsDialog(editable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Dimension showMapSizeDialog() {
        return this.widgets.showMapSizeDialog();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColPanel showModelMessages(List<ModelMessage> modelMessages) {
        if (modelMessages.isEmpty()) return null;
        final Game game = getGame();
        int n = modelMessages.size();
        String[] texts = new String[n];
        FreeColObject[] fcos = new FreeColObject[n];
        ImageIcon[] icons = new ImageIcon[n];
        Tile tile = null;
        for (int i = 0; i < n; i++) {
            ModelMessage m = modelMessages.get(i);
            texts[i] = Messages.message(m);
            fcos[i] = game.getMessageSource(m);
            icons[i] = this.fixedImageLibrary
                .getObjectImageIcon(game.getMessageDisplay(m));
            if (tile == null && fcos[i] instanceof Location) {
                tile = ((Location)fcos[i]).getTile();
            }
        }
        InformationPanel panel
            = new InformationPanel(getFreeColClient(), texts, fcos, icons);
        return this.canvas.showFreeColPanel(panel,
            getPopupPosition(tile), true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showMonarchDialog(final MonarchAction action,
                                  StringTemplate template, String monarchKey,
                                  DialogHandler<Boolean> handler) {
        this.widgets.showMonarchDialog(action, template, monarchKey, handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showNamingDialog(StringTemplate template,
                                      final String defaultName,
                                      final Unit unit,
                                      DialogHandler<String> handler) {
        this.widgets.showNamingDialog(template, defaultName,
                                      getPopupPosition(unit.getTile()),
                                      handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showNativeDemandDialog(Unit unit, Colony colony,
                                       GoodsType type, int amount,
                                       DialogHandler<Boolean> handler) {
        this.widgets.showNativeDemandDialog(unit, colony, type, amount,
                                            getPopupPosition(unit.getTile()),
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
        if ((!(our instanceof Unit) && !(our instanceof Colony))
            || (!(other instanceof Unit) && !(other instanceof Colony))
            || (our instanceof Colony && other instanceof Colony)) {
            throw new RuntimeException("Bad DTD args: " + our + ", " + other);
        }
        return this.widgets.showNegotiationDialog(our, other, agreement,
            comment, getPopupPosition(((Location)our).getTile()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColPanel showNewPanel(Specification spec) {
        return this.widgets.showNewPanel(spec);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Parameters showParametersDialog() {
        return this.widgets.showParametersDialog();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean showPreCombatDialog(Unit attacker,
                                       FreeColGameObject defender, Tile tile) {
        return this.widgets.showPreCombatDialog(attacker, defender,
                                                getPopupPosition(tile));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColPanel showPurchasePanel() {
        return this.widgets.showPurchasePanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColPanel showRecruitPanel() {
        return this.widgets.showRecruitPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColPanel showReportCargoPanel() {
        return this.widgets.showReportCargoPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColPanel showReportColonyPanel() {
        boolean compact;
        try {
            compact = getFreeColClient().getClientOptions()
                .getInteger(ClientOptions.COLONY_REPORT)
                == ClientOptions.COLONY_REPORT_COMPACT;
        } catch (Exception e) {
            compact = false;
        }
        return this.widgets.showReportColonyPanel(compact);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColPanel showReportContinentalCongressPanel() {
        return this.widgets.showReportContinentalCongressPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColPanel showReportEducationPanel() {
        return this.widgets.showReportEducationPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColPanel showReportExplorationPanel() {
        return this.widgets.showReportExplorationPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColPanel showReportForeignAffairPanel() {
        return this.widgets.showReportForeignAffairPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColPanel showReportHistoryPanel() {
        return this.widgets.showReportHistoryPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColPanel showReportIndianPanel() {
        return this.widgets.showReportIndianPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColPanel showReportLabourPanel() {
        return this.widgets.showReportLabourPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColPanel showReportLabourDetailPanel(UnitType unitType,
            Map<UnitType, java.util.Map<Location, Integer>> data,
            TypeCountMap<UnitType> unitCount, List<Colony> colonies) {
        return this.widgets.showReportLabourDetailPanel(unitType, data,
                                                        unitCount, colonies);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColPanel showReportMilitaryPanel() {
        return this.widgets.showReportMilitaryPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColPanel showReportNavalPanel() {
        return this.widgets.showReportNavalPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColPanel showReportProductionPanel() {
        return this.widgets.showReportProductionPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColPanel showReportReligiousPanel() {
        return this.widgets.showReportReligiousPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColPanel showReportRequirementsPanel() {
        return this.widgets.showReportRequirementsPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColPanel showReportTradePanel() {
        return this.widgets.showReportTradePanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColPanel showReportTurnPanel(List<ModelMessage> messages) {
        return this.widgets.showReportTurnPanel(messages);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String showRiverStyleDialog(List<String> styles) {
        return this.widgets.showRiverStyleDialog(styles);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public File showSaveDialog(File directory, String defaultName) {
        String extension = lastPart(defaultName, ".");
        FileFilter[] filters = new FileFilter[] {
            FreeColDataFile.getFileFilter(extension)
        };
        return this.widgets.showSaveDialog(directory, filters, defaultName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Dimension showScaleMapSizeDialog() {
        return this.widgets.showScaleMapSizeDialog();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int showSelectAmountDialog(GoodsType goodsType, int available,
                                      int defaultAmount, boolean needToPay) {
        return this.widgets.showSelectAmountDialog(goodsType, available,
                                                   defaultAmount, needToPay);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Location showSelectDestinationDialog(Unit unit) {
        return this.widgets.showSelectDestinationDialog(unit,
            getPopupPosition(unit.getTile()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int showSelectTributeAmountDialog(StringTemplate question,
                                             int maximum) {
        return this.widgets.showSelectTributeAmountDialog(question, maximum);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColPanel showServerListPanel(List<ServerInfo> serverList) {
        this.canvas.closeMenus();
        return this.widgets.showServerListPanel(serverList);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColPanel showStartGamePanel(Game game, Player player,
                                           boolean singlePlayerMode) {
        if (game == null) {
            logger.warning("StartGamePanel requires game != null.");
        } else if (player == null) {
            logger.warning("StartGamePanel requires player != null.");
        } else {
            this.canvas.closeMenus();
            StartGamePanel panel
                = this.canvas.getExistingFreeColPanel(StartGamePanel.class);
            if (panel == null) {
                panel = new StartGamePanel(getFreeColClient());
            }
            panel.initialize(singlePlayerMode);
            return this.canvas.showFreeColPanel(panel,
                PopupPosition.CENTERED, false);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColPanel showStatisticsPanel(Map<String, String> serverStats,
                                            Map<String, String> clientStats) {
        return this.widgets.showStatisticsPanel(serverStats, clientStats);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColPanel showStatusPanel(String message) {
        StatusPanel panel
            = this.canvas.getExistingFreeColPanel(StatusPanel.class);
        if (panel == null) {
            panel = new StatusPanel(getFreeColClient());
        } else {
            this.canvas.removeFromCanvas(panel);
        }
        panel.setStatusMessage(message);
        return this.canvas.showFreeColPanel(panel,
            PopupPosition.CENTERED, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColPanel showTilePanel(Tile tile) {
        return this.widgets.showTilePanel(tile);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColPanel showTradeRouteInputPanel(TradeRoute newRoute) {
        return this.widgets.showTradeRouteInputPanel(newRoute);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColPanel showTradeRoutePanel(Unit unit) {
        return this.widgets.showTradeRoutePanel(unit,
            getPopupPosition((unit == null) ? null : unit.getTile()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColPanel showTrainPanel() {
        return this.widgets.showTrainPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showVictoryDialog(DialogHandler<Boolean> handler) {
        this.widgets.showVictoryDialog(handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean showWarehouseDialog(Colony colony) {
        return this.widgets.showWarehouseDialog(colony);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColPanel showWorkProductionPanel(Unit unit) {
        return this.widgets.showWorkProductionPanel(unit);
    }
}
