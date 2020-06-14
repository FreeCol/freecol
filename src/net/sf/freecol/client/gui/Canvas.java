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

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JLayeredPane;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicInternalFrameUI;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.action.FreeColAction;
import net.sf.freecol.client.gui.dialog.FreeColDialog;
import net.sf.freecol.client.gui.panel.MapControls;
import net.sf.freecol.client.gui.panel.Utility;
import net.sf.freecol.client.gui.menu.InGameMenuBar;
import net.sf.freecol.client.gui.menu.MapEditorMenuBar;
import net.sf.freecol.client.gui.menu.MenuMouseMotionListener;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.metaserver.ServerInfo;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.option.IntegerOption;
import net.sf.freecol.common.option.Option;
import net.sf.freecol.common.option.OptionGroup;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.common.util.Introspector;
import static net.sf.freecol.common.util.StringUtils.*;

// Special case panels, TODO: can we move these to Widgets?
import net.sf.freecol.client.gui.panel.ChatPanel;
import net.sf.freecol.client.gui.panel.ColonyPanel;
import net.sf.freecol.client.gui.panel.ErrorPanel;
import net.sf.freecol.client.gui.panel.FreeColPanel;
import net.sf.freecol.client.gui.panel.MainPanel;
import net.sf.freecol.client.gui.panel.MapEditorTransformPanel;
import net.sf.freecol.client.gui.panel.ServerListPanel;
import net.sf.freecol.client.gui.panel.StartGamePanel;
import net.sf.freecol.client.gui.panel.StatisticsPanel;
import net.sf.freecol.client.gui.panel.StatusPanel;
import net.sf.freecol.client.gui.panel.TradeRouteInputPanel;


/**
 * The main container for the other GUI components in FreeCol. This
 * container is where the panels, dialogs and menus are added. In
 * addition, this is the component in which the map graphics are
 * displayed.
 * <p>
 * <b>Displaying panels and a dialogs</b>
 * <p>
 * {@code Canvas} contains methods to display various panels and dialogs.
 * Most of these methods use {@link net.sf.freecol.common.i18n i18n} to get
 * localized text.  Dialogs return results, and may be modal or non-modal.
 */
public final class Canvas extends JDesktopPane {

    private static final Logger logger = Logger.getLogger(Canvas.class.getName());

    public static enum PopupPosition {
        ORIGIN,
        CENTERED,
        CENTERED_LEFT,
        CENTERED_RIGHT,
    }

    /** Number of tries to find a clear spot on the canvas. */
    private static final int MAXTRY = 3;

    /** Number of pixels that must be moved before a goto is enabled. */
    private static final int DRAG_THRESHOLD = 16;

    /** The cursor to show for goto operations. */
    private static final java.awt.Cursor GO_CURSOR
        = (java.awt.Cursor)UIManager.get("cursor.go");
    
    /** A class for frames being used as tool boxes. */
    private static class ToolBoxFrame extends JInternalFrame {}

    /** The game client. */
    private final FreeColClient freeColClient;

    /** The graphics device to display to. */
    private final GraphicsDevice graphicsDevice;

    /** The image library to create icons etc with. */
    private final ImageLibrary imageLibrary;

    /** Is the canvas in windowed mode? */
    private boolean windowed;

    /** The parent frame, either a window or the full screen. */
    private FreeColFrame parentFrame;

    /** Remember the current size (from getSize()), check for changes. */
    private Dimension oldSize = null;

    private final MapViewer mapViewer;

    private MapControls mapControls;

    /** Where the map was drag-clicked. */
    private Point dragPoint = null;

    /** Has a goto operation started? */
    private boolean gotoStarted = false;

    private GrayLayer greyLayer;

    /** Cached panels.  TODO: check if we still need these */
    private MainPanel mainPanel;

    private final StartGamePanel startGamePanel;

    private final StatusPanel statusPanel;

    private final ChatPanel chatPanel;

    private final ChatDisplay chatDisplay;

    private final ServerListPanel serverListPanel;

    /** The dialogs in view. */
    private final List<FreeColDialog<?>> dialogs = new ArrayList<>();

    /**
     * Determine whether to use full screen mode, or size of window.
     *
     * @param gd The {@code GraphicsDevice} to display to.
     * @param desiredSize An optional window {@code Dimension}.
     * @return Null if full screen is to be used, otherwise a window
     *     bounds {@code Rectangle}
     */
    private static boolean checkWindowed(GraphicsDevice gd, Dimension desiredSize) {
        boolean ret;
        if (desiredSize == null) {
            if (gd.isFullScreenSupported()) {
                logger.info("Full screen mode used.");
                ret = false;
            } else {
                logger.warning("Full screen mode not supported.");
                System.err.println(Messages.message("client.fullScreen"));
                ret = true;
            }
        } else {
            logger.info("Windowed mode used.");
            ret = true;
        }
        return ret;
    }


    /**
     * The constructor to use.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param graphicsDevice The used graphics device.
     * @param imageLibrary The {@code ImageLibrary} to use.
     * @param desiredSize The desired size of the parent frame.
     * @param mapViewer The object responsible of drawing the map onto
     *     this component.
     */
    public Canvas(final FreeColClient freeColClient,
                  final GraphicsDevice graphicsDevice,
                  final ImageLibrary imageLibrary,
                  final Dimension desiredSize,
                  MapViewer mapViewer) {
        this.freeColClient = freeColClient;
        this.graphicsDevice = graphicsDevice;
        this.imageLibrary = imageLibrary;

        // Determine if windowed mode should be used and set the window size.
        this.windowed = checkWindowed(graphicsDevice, desiredSize);
        Rectangle windowBounds = null;
        if (this.windowed && desiredSize != null
            && desiredSize.width > 0 && desiredSize.height > 0) {
            windowBounds = new Rectangle(desiredSize);
        }

        this.parentFrame = createFrame(null, windowBounds);
        this.oldSize = getSize();
        this.mapViewer = mapViewer;
        this.mapControls = null;
        this.greyLayer = new GrayLayer(freeColClient);
        
        setDoubleBuffered(true);
        setOpaque(false);
        setLayout(null);
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        createKeyBindings();

        chatDisplay = new ChatDisplay();
        chatPanel = new ChatPanel(freeColClient);
        serverListPanel = new ServerListPanel(freeColClient,
            freeColClient.getConnectController());
        startGamePanel = new StartGamePanel(freeColClient);
        statusPanel = new StatusPanel(freeColClient);

        mapViewer.startCursorBlinking();
        logger.info("Canvas created woth bounds: " + windowBounds);
    }

    // Simple utilities
    
    /**
     * Check if there is a map to display.
     *
     * @return True if there is a map available.
     */
    private boolean hasMap() {
        return this.freeColClient != null
            && this.freeColClient.getGame() != null
            && this.freeColClient.getGame().getMap() != null;
    }

    // Windowed mode?

    /**
     * Are we in windowed mode?
     *
     * @return True if in windowed mode.
     */
    public boolean isWindowed() {
        return this.windowed;
    }

    /**
     * Toggle windowed flag.
     */
    private void toggleWindowed() {
        this.windowed = !this.windowed;
    }
    

    // Frame handling and size handling

    /**
     * Get the parent frame.
     *
     * Do not use this except inside Widgets (which is really just an
     * extension of Canvas).
     *
     * @return The parent {@code FreeColFrame}.
     */
    public FreeColFrame getParentFrame() {
        return this.parentFrame;
    }
    
    /**
     * Create a new frame with a given menu bar and bounds.
     *
     * @param menuBar The new frames {@code JMenuBar}.
     * @param windowBounds The new frame bounding {@code Rectangle}.
     * @return The new {@code FreeColFrame}.
     */
    private FreeColFrame createFrame(JMenuBar menuBar, Rectangle windowBounds) {
        // FIXME: Check this:
        // User might have moved window to new screen in a
        // multi-screen setup, so make this.gd point to the current screen.
        FreeColFrame fcf
            = new FreeColFrame(this.freeColClient, this.graphicsDevice,
                               menuBar, isWindowed(), windowBounds);
        fcf.getContentPane().add(this);
        fcf.setVisible(true);
        return fcf;
    }

    /**
     * Destroy the current frame.
     */
    private void destroyFrame() {
        if (this.parentFrame != null) {
            this.parentFrame.setVisible(false);
            if (!isWindowed()) this.parentFrame.exitFullScreen();
            this.parentFrame.dispose();
            this.parentFrame = null;
        }
    }

    /**
     * Toggle the frame between windowed and non-windowed modes.
     */
    public void toggleFrame() {
        JMenuBar menuBar = null;
        Rectangle windowBounds = null;
        if (this.parentFrame != null) {
            menuBar = this.parentFrame.getJMenuBar();
            windowBounds = this.parentFrame.getBounds();
        }
        destroyFrame();
        toggleWindowed();
        this.parentFrame = createFrame(menuBar, windowBounds);
    }

    /**
     * Has the canvas been resized?
     *
     * @return The new {@code Dimension} for the canvas.
     */
    private Dimension checkResize() {
        Dimension newSize = getSize();
        if (this.oldSize.width == newSize.width
            && this.oldSize.height == newSize.height) return null;
        
        this.oldSize = newSize;
        return newSize;
    }


    // Map controls
    
    public boolean canZoomInMapControls() {
        return mapControls != null && mapControls.canZoomInMapControls();
    }

    public boolean canZoomOutMapControls() {
        return mapControls != null && mapControls.canZoomOutMapControls();
    }

    public void enableMapControls(boolean enable) {
        // Always instantiate in game.
        if (enable && mapControls == null) {
            String className = this.freeColClient.getClientOptions()
                .getString(ClientOptions.MAP_CONTROLS);
            final String panelName = "net.sf.freecol.client.gui.panel."
                + lastPart(className, ".");
            try {
                mapControls = (MapControls)Introspector.instantiate(panelName,
                    new Class[] { FreeColClient.class },
                    new Object[] { this.freeColClient });
                mapControls.addToComponent(this);
                mapControls.update();
                logger.info("Instantiated " + panelName);
            } catch (Introspector.IntrospectorException ie) {
                logger.log(Level.WARNING, "Failed in make map controls for: "
                    + panelName, ie);
            }
        } else if (!enable && mapControls != null) {
            mapControls.removeFromComponent(this);
            mapControls = null;
        }
    }

    public void miniMapToggleViewControls() {
        if (mapControls == null) return;
        mapControls.toggleView();
    }

    public void miniMapToggleFogOfWarControls() {
        if (mapControls == null) return;
        mapControls.toggleFogOfWar();
    }

    public void updateMapControls() {
        if (mapControls != null) mapControls.update();
    }

    public void updateMapControlsInCanvas() {
        if (mapControls == null) return;
        mapControls.removeFromComponent(this);
        mapControls.addToComponent(this);
    }

    public void zoomInMapControls() {
        if (mapControls == null) return;
        mapControls.zoomIn();
    }

    public void zoomOutMapControls() {
        if (mapControls == null) return;
        mapControls.zoomOut();
    }


    // Map viewer
    
    /**
     * Scroll the map in the given direction.
     *
     * @param direction The {@code Direction} to scroll in.
     * @return True if scrolling occurred.
     */
    public boolean scrollMap(Direction direction) {
        return mapViewer.scrollMap(direction);
    }

    /**
     * Converts the given screen coordinates to Map coordinates.
     * It checks to see to which Tile the given pixel 'belongs'.
     *
     * @param x The x-coordinate in pixels.
     * @param y The y-coordinate in pixels.
     * @return The Tile that is located at the given position on the screen.
     */
    public Tile convertToMapTile(int x, int y) {
        return mapViewer.convertToMapTile(x, y);
    }


    // Drag and goto
    
    /**
     * Gets the point at which the map was clicked for a drag.
     *
     * @return The Point where the mouse was initially clicked.
     */
    public Point getDragPoint() {
        return this.dragPoint;
    }

    /**
     * Sets the point at which the map was clicked for a drag.
     *
     * @param x The mouse x position.
     * @param y The mouse y position.
     */
    public void setDragPoint(int x, int y) {
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
     * Checks if there is currently a goto operation on the mapboard.
     *
     * @return True if a goto operation is in progress.
     */
    public boolean isGotoStarted() {
        return this.gotoStarted;
    }

    /**
     * Starts a goto operation.
     */
    public void startGoto() {
        this.gotoStarted = true;
        setCursor(GO_CURSOR);
        mapViewer.changeGotoPath(null);
        refresh();
    }

    /**
     * Stops any ongoing goto operation.
     *
     * @return The old goto path if any.
     */
    public PathNode stopGoto() {
        PathNode ret = (this.gotoStarted) ? mapViewer.getGotoPath() : null;
        this.gotoStarted = false;
        setCursor(null);
        mapViewer.changeGotoPath(null);
        refresh();
        return ret;
    }

    /**
     * Change the goto path for a unit to a new tile.
     *
     * @param unit The {@code Unit} that is travelling.
     * @param tile The new {@code Tile} to go to.
     */       
    public void changeGoto(Unit unit, Tile tile) {
        if (!isGotoStarted()) return;

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


    // Internals

    /**
     * Adds a component on this Canvas inside a frame.
     *
     * @param comp The component to add to the canvas.
     * @param toolBox Should be set to true if the resulting frame is
     *     used as a toolbox (that is: it should not be counted as a
     *     frame).
     * @param popupPosition A preferred {@code PopupPosition}.
     * @param resizable Whether this component can be resized.
     * @return The {@code JInternalFrame} that was created and added.
     */
    private JInternalFrame addAsFrame(JComponent comp, boolean toolBox,
                                      PopupPosition popupPosition,
                                      boolean resizable) {
        final int FRAME_EMPTY_SPACE = 60;

        final JInternalFrame f = (toolBox) ? new ToolBoxFrame()
            : new JInternalFrame();
        Container con = f.getContentPane();
        if (con instanceof JComponent) {
            JComponent c = (JComponent)con;
            c.setOpaque(false);
            c.setBorder(null);
        }

        if (comp.getBorder() != null) {
            if (comp.getBorder() instanceof EmptyBorder) {
                f.setBorder(Utility.blankBorder(10, 10, 10, 10));
            } else {
                f.setBorder(comp.getBorder());
                comp.setBorder(Utility.blankBorder(5, 5, 5, 5));
            }
        } else {
            f.setBorder(null);
        }

        final FrameMotionListener fml = new FrameMotionListener(f);
        comp.addMouseMotionListener(fml);
        comp.addMouseListener(fml);
        if (f.getUI() instanceof BasicInternalFrameUI) {
            BasicInternalFrameUI biu = (BasicInternalFrameUI) f.getUI();
            biu.setNorthPane(null);
            biu.setSouthPane(null);
            biu.setWestPane(null);
            biu.setEastPane(null);
        }

        f.getContentPane().add(comp);
        f.setOpaque(false);
        f.pack();
        int width = f.getWidth();
        int height = f.getHeight();
        if (width > getWidth() - FRAME_EMPTY_SPACE) {
            width = Math.min(width, getWidth());
        }
        if (height > getHeight() - FRAME_EMPTY_SPACE) {
            height = Math.min(height, getHeight());
        }
        f.setSize(width, height);
        Point p = chooseLocation(comp, width, height, popupPosition);
        f.setLocation(p);
        this.addToCanvas(f, MODAL_LAYER);
        f.setName(comp.getClass().getSimpleName());

        f.setFrameIcon(null);
        f.setVisible(true);
        f.setResizable(resizable);
        try {
            f.setSelected(true);
        } catch (java.beans.PropertyVetoException e) {}

        return f;
    }

    /**
     * Adds a component centered on this Canvas.
     *
     * @param comp The {@code Component} to add to this canvas.
     * @param i The layer to add the component to (see JLayeredPane).
     */
    private void addCentered(Component comp, Integer i) {
        comp.setLocation((getWidth() - comp.getWidth()) / 2,
                         (getHeight() - comp.getHeight()) / 2);
        this.add(comp, i);
    }

    /**
     * Adds a component to this Canvas.
     *
     * @param comp The {@code Component} to add to this canvas.
     * @param i The layer to add the component to (see JLayeredPane).
     */
    private void add(Component comp, Integer i) {
        addToCanvas(comp, i);
        updateMenuBar();
    }

    /**
     * Adds a component to this Canvas updating the menus.
     *
     * Make sure the status panel is not present unless the component
     * *is* the status panel.
     *
     * @param comp The {@code Component} to add to this canvas.
     * @param i The layer to add the component to (see JLayeredPane).
     */
    private void addToCanvas(Component comp, Integer i) {
        if (statusPanel.isVisible()) {
            if (comp == statusPanel) return;
            if (!(comp instanceof JMenuItem)) removeFromCanvas(statusPanel);
        }

        if (i == null) i = JLayeredPane.DEFAULT_LAYER;
        try {
            super.add(comp, i);
        } catch (Exception e) {
            logger.log(Level.WARNING, "addToCanvas(" + comp + ", " + i
                + ") failed.", e);
        }
    }

    /**
     * Choose a location for a component.
     *
     * @param comp The {@code Component} to place.
     * @param width The component width to use.
     * @param height The component height to use.
     * @param popupPosition An optional {@code PopupPosition} hint.
     * @return A suitable {@code Point} to place the component.
     */
    private Point chooseLocation(Component comp, int width, int height, 
                                 PopupPosition popupPosition) {
        Point p = null;
        if ((comp instanceof FreeColPanel)
            && (p = getSavedPosition(comp)) != null) {
            // Sanity check stuff coming out of client options.
            if (p.getX() < 0
                || p.getX() >= getWidth() - width
                || p.getY() < 0
                || p.getY() >= getHeight() - height) {
                p = null;
            }
        }
        int x = 0, y = 0;
        if (p != null) {
            x = (int)p.getX();
            y = (int)p.getY();
        } else if (popupPosition != null) {
            switch (popupPosition) {
            case CENTERED:
                x = (getWidth() - width) / 2;
                y = (getHeight() - height) / 2;
                break;
            case CENTERED_LEFT:
                x = (getWidth() - width) / 4;
                y = (getHeight() - height) / 2;
                break;
            case CENTERED_RIGHT:
                x = ((getWidth() - width) * 3) / 4;
                y = (getHeight() - height) / 2;
                break;
            case ORIGIN:
                x = y = 0;
                break;
            }
        }
        if ((p = getClearSpace(x, y, width, height, MAXTRY)) != null
            && p.x >= 0 && p.x < getWidth()
            && p.y >= 0 && p.y < getHeight()) {
            x = p.x;
            y = p.y;
        }
        return new Point(x, y);
    }

    /**
     * Create key bindings for all actions.
     */
    private void createKeyBindings() {
        for (Option option : freeColClient.getActionManager().getOptions()) {
            FreeColAction action = (FreeColAction) option;
            getInputMap().put(action.getAccelerator(), action.getId());
            getActionMap().put(action.getId(), action);
        }
    }

    /**
     * Try to find some free space on the canvas for a component,
     * starting at x,y.
     *
     * @param x A starting x coordinate.
     * @param y A starting y coordinate.
     * @param w The component width to use.
     * @param h The component height to use.
     * @param tries The number of attempts to find a clear space.
     * @return A {@code Point} to place the component at or null
     *     on failure.
     */
    private Point getClearSpace(final int x, final int y,
                                final int w, final int h, int tries) {
        final Rectangle bounds = this.getBounds();
        if (!bounds.contains(x, y)) return null;

        tries = 3 * tries + 1; // 3 new candidates per level
        List<Point> todo = new ArrayList<>();
        Point p = new Point(x, y);
        todo.add(p);

        List<Component> allComponents
            = transform(this.getComponents(),
                        c -> !(c instanceof GrayLayer) && c.isValid());
        allComponents.addAll(dialogs);

        // Find the position with the least overlap
        int bestScore = Integer.MAX_VALUE;
        Point best = p;
        while (!todo.isEmpty()) {
            p = todo.remove(0);
            Rectangle r = new Rectangle(p.x, p.y, w, h);
            if (!bounds.contains(r)) {
                continue;
            }

            // Find the most overlapping component at this position,
            // as well as the globally least.
            int foundScore = 0;
            Component found = null;
            for (Component c : allComponents) {
                Rectangle rb = c.getBounds();
                if (rb.intersects(r)) {
                    Rectangle rr = rb.intersection(r);
                    int score = (int)Math.round(rr.getWidth() * rr.getHeight());
                    if (foundScore < score) {
                        foundScore = score;
                        found = c;
                    }
                }
            }
            if (found == null) { // Can not improve on no overlap, return now
                return p;
            }
            if (bestScore > foundScore) {
                bestScore = foundScore;
                best = p;
            }
            // Guarantee eventual completion
            if (--tries <= 0) break;

            int n = todo.size(),
                // Some alternative new positions
                //   0: move right/down to avoid the collision
                //   1: move as far as possible right/down
                //   2: wrap back to the far left
                x0 = found.getX() + found.getWidth() + 1,
                y0 = found.getY() + found.getHeight() + 1,
                x1 = bounds.x + bounds.width - w - 1,
                y1 = bounds.y + bounds.height - h - 1,
                x2 = bounds.x,
                y2 = bounds.y;
            boolean x0ok = bounds.contains(x0 + w, y),
                y0ok = bounds.contains(x, y0 + h),
                x1ok = bounds.contains(x1, y),
                y1ok = bounds.contains(x, y1);
            todo.add(n, new Point((x0ok) ? x0 : (x1ok) ? x1 : x2,
                                  (y0ok) ? y0 : (y1ok) ? y1 : y2));
            todo.add(n, new Point(x, (y0ok) ? y0 : (y1ok) ? y1 : y2));
            todo.add(n, new Point((x0ok) ? x0 : (x1ok) ? x1 : x2, y));
        }
        return best;
    }

    /**
     * Gets the internal frame for the given component.
     *
     * @param c The {@code Component}.
     * @return The given component if this is an internal frame or the
     *     first parent that is an internal frame.  Returns
     *     {@code null} if no internal frame is found.
     */
    private JInternalFrame getInternalFrame(final Component c) {
        Component temp = c;

        while (temp != null && !(temp instanceof JInternalFrame)) {
            temp = temp.getParent();
        }
        return (JInternalFrame) temp;
    }

    /**
     * Make a tile visible, then determine corresponding position to popup
     * a panel.
     *
     * @param tile A {@code Tile} to be made visible.
     * @return A {@code PopupPosition} for a panel to be displayed.
     */
    private PopupPosition setOffsetFocus(Tile tile) {
        if (tile == null) return PopupPosition.CENTERED;
        int where = mapViewer.setOffsetFocus(tile);
        return (where > 0) ? PopupPosition.CENTERED_LEFT
            : (where < 0) ? PopupPosition.CENTERED_RIGHT
            : PopupPosition.CENTERED;
    }

    /**
     * Gets the saved position of a component.
     *
     * @param comp The {@code Component} to use.
     * @return The saved position as a {@code Point}, or null if no
     *     saved position is found.
     */
    private Point getSavedPosition(Component comp) {
        final ClientOptions co = freeColClient.getClientOptions();
        if (co == null) return null;
        try {
            if (!co.getBoolean(ClientOptions.REMEMBER_PANEL_POSITIONS)) {
                return null;
            }
        } catch (Exception e) {}
        return co.getPanelPosition(comp.getClass().getName());
    }

    /**
     * Get the saved size of a component.
     *
     * @param comp The {@code Component} to use.
     * @return A {@code Dimension} for the component or null if
     *     no saved size is found.
     */
    private Dimension getSavedSize(Component comp) {
        final ClientOptions co = freeColClient.getClientOptions();
        if (co == null) return null;
        try {
            if (!co.getBoolean(ClientOptions.REMEMBER_PANEL_SIZES)) {
                return null;
            }
        } catch (Exception e) {}
        return co.getPanelSize(comp.getClass().getName());
    }

    /**
     * A component is closing.  Some components need position and size
     * to be saved.
     *
     * @param c The closing {@code Component}.
     * @param jif The enclosing {@code JInternalFrame}.
     */
    private void notifyClose(Component c, JInternalFrame jif) {
        if (c instanceof FreeColPanel) {
            FreeColPanel fcp = (FreeColPanel)c;
            fcp.firePropertyChange("closing", false, true);
            
            savePosition(fcp, jif.getLocation());
            saveSize(fcp, fcp.getSize());
        }
    }

    /**
     * Save an {@code int} value to the saved ClientOptions,
     * using the name of the components class plus the given key as
     * and identifier.
     *
     * @param className The class name for the component.
     * @param key The key to save.
     * @param value The value to save.
     */
    private void saveInteger(String className, String key, int value) {
        if (freeColClient == null) return;
        final ClientOptions co = freeColClient.getClientOptions();
        if (co == null) return;
        final OptionGroup etc = co.getOptionGroup(ClientOptions.ETC);
        if (etc == null) return;
        
        // Insist the option is present
        if (!etc.hasOption(className + key, IntegerOption.class)) {
            Specification specification = (freeColClient.getGame() == null)
                ? null : freeColClient.getGame().getSpecification();
            etc.add(new IntegerOption(className + key, specification));
        }
        // Set the value
        etc.setInteger(className + key, value);
    }

    /**
     * Save the position of a component.
     *
     * @param comp The {@code Component} to use.
     * @param position The position to save.
     */
    private void savePosition(Component comp, Point position) {
        try {
            if (!freeColClient.getClientOptions()
                .getBoolean(ClientOptions.REMEMBER_PANEL_POSITIONS)) return;
        } catch (Exception e) {}

        String className = comp.getClass().getName();
        saveInteger(className, ".x", position.x);
        saveInteger(className, ".y", position.y);
    }

    /**
     * Save the size of a component.
     *
     * @param comp The {@code Component} to use.
     * @param size The {@code Dimension} to save.
     */
    private void saveSize(Component comp, Dimension size) {
        try {
            if (!freeColClient.getClientOptions()
                .getBoolean(ClientOptions.REMEMBER_PANEL_SIZES)) return;
        } catch (Exception e) {}

        String className = comp.getClass().getName();
        saveInteger(className, ".w", size.width);
        saveInteger(className, ".h", size.height);
    }

    /**
     * Restart blinking on the map.  Switching it on again needs to check
     * for the presence of other dialogs.
     */
    private void restartBlinking() {
        if (mapViewer.getViewMode() != GUI.ViewMode.MOVE_UNITS) return;
        for (FreeColDialog<?> f : dialogs) {
            if (f.isModal()) return;
        }
        mapViewer.startCursorBlinking();
    }

    /**
     * Stop blinking on the map.
     */
    private void stopBlinking() {
        mapViewer.stopCursorBlinking();
    }


    // Dialog display, only public for Widgets
    
    /**
     * Gets any currently displayed colony panel for the specified colony.
     *
     * This is distinct from {@link #getExistingFreeColPanel} because
     * there can be multiple ColonyPanels.
     *
     * @param colony The {@code Colony} to check.
     * @return A currently displayed colony panel, or null if not found.
     */
    public ColonyPanel getColonyPanel(Colony colony) {
        for (Component c1 : getComponents()) {
            if (c1 instanceof JInternalFrame) {
                for (Component c2 : ((JInternalFrame) c1).getContentPane()
                         .getComponents()) {
                    if (c2 instanceof ColonyPanel
                        && ((ColonyPanel)c2).getColony() == colony) {
                        return (ColonyPanel)c2;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Gets a currently displayed FreeColPanel of a given type.
     *
     * @param <T> The actual panel type.
     * @param type The type of {@code FreeColPanel} to look for.
     * @return A currently displayed {@code FreeColPanel} of the
     *     requested type, or null if none found.
     */
    public <T extends FreeColPanel> T getExistingFreeColPanel(Class<T> type) {
        for (Component c1 : getComponents()) {
            if (c1 instanceof JInternalFrame) {
                for (Component c2 : ((JInternalFrame)c1).getContentPane()
                         .getComponents()) {
                    try {
                        T ret = type.cast(c2);
                        if (ret != null) {
                            final JInternalFrame jif = (JInternalFrame)c1;
                            SwingUtilities.invokeLater(() -> {
                                    jif.toFront();
                                    jif.repaint();
                                });
                            return ret;
                        }
                    } catch (ClassCastException cce) {}
                }
            }
        }
        return null;
    }

    /**
     * Get a currentlydisplayed FreeColDialog of a given type.
     *
     * @param type The class of dialog to look for.
     * @return The {@code FreeColDialog} found, or null if none present.
     */
    public FreeColDialog<?> getExistingFreeColDialog(Class<?> type) {
        for (FreeColDialog<?> d : dialogs) {
            if (d.getClass() == type) return d;
        }
        return null;
    }

    /**
     * Displays the given dialog, optionally making sure a tile is visible.
     *
     * @param <T> The type to be returned from the dialog.
     * @param freeColDialog The dialog to be displayed
     * @param tile An optional {@code Tile} to make visible (not
     *     under the dialog!)
     * @return The {@link FreeColDialog#getResponse reponse} returned by
     *     the dialog.
     */
    public <T> T showFreeColDialog(FreeColDialog<T> freeColDialog,
                                   Tile tile) {
        viewFreeColDialog(freeColDialog, tile);
        T response = freeColDialog.getResponse();
        remove(freeColDialog);
        dialogRemove(freeColDialog);
        if (freeColDialog.isModal()) restartBlinking();
        return response;
    }

    /**
     * Displays the given panel, making sure a tile is visible.
     *
     * @param panel The panel to be displayed
     * @param tile A {@code Tile} to make visible (not under the panel!)
     * @param resizable Should the panel be resizable?
     */
    public void showFreeColPanel(FreeColPanel panel, Tile tile,
                                 boolean resizable) {
        showSubPanel(panel, setOffsetFocus(tile), resizable);
    }

    /**
     * Displays a {@code FreeColPanel}.
     *
     * @param panel {@code FreeColPanel}, panel to show
     * @param resizable Should the panel be resizable?
     */
    public void showSubPanel(FreeColPanel panel, boolean resizable) {
        showSubPanel(panel, PopupPosition.CENTERED, resizable);
    }

    /**
     * Displays a {@code FreeColPanel} at a generalized position.
     *
     * @param panel {@code FreeColPanel}, panel to show
     * @param popupPosition {@code PopupPosition} The generalized
     *     position to place the panel.
     * @param resizable Should the panel be resizable?
     */
    public void showSubPanel(FreeColPanel panel, PopupPosition popupPosition,
                             boolean resizable) {
        repaint();
        addAsFrame(panel, false, popupPosition, resizable);
        panel.requestFocus();
    }

    /**
     * Displays the given dialog, optionally making sure a tile is visible.
     *
     * @param <T> The type to be returned from the dialog.
     * @param freeColDialog The dialog to be displayed
     * @param tile An optional {@code Tile} to make visible (not
     *     under the dialog!)
     */
    public <T> void viewFreeColDialog(final FreeColDialog<T> freeColDialog,
                                      Tile tile) {
        PopupPosition pp = setOffsetFocus(tile);

        // TODO: Remove compatibility code when all non-modal dialogs
        //       have been converted into panels.
        if (!freeColDialog.isModal()) {
            int canvasWidth = getWidth();
            int dialogWidth = freeColDialog.getWidth();
            if(dialogWidth*2 <= canvasWidth) {
                Point location = freeColDialog.getLocation();
                if(pp == PopupPosition.CENTERED_LEFT) {
                    freeColDialog.setLocation(location.x - canvasWidth/4,
                                              location.y);
                } else if(pp == PopupPosition.CENTERED_RIGHT) {
                    freeColDialog.setLocation(location.x + canvasWidth/4,
                                              location.y);
                }
            }
        }

        dialogAdd(freeColDialog);
        if (freeColDialog.isModal()) stopBlinking();
        freeColDialog.requestFocus();
        freeColDialog.setVisible(true);
    }

    
    // Public API

    
    /**
     * Map editor initialization.
     */
    public void startMapEditorGUI() {
        this.parentFrame.setMenuBar(new MapEditorMenuBar(this.freeColClient,
                new MenuMouseMotionListener(this.freeColClient, this)));
        CanvasMapEditorMouseListener listener
            = new CanvasMapEditorMouseListener(freeColClient, this);
        addMouseListener(listener);
        addMouseMotionListener(listener);
    }

    /**
     * In game initializations.
     */
    public void initializeInGame() {
        this.parentFrame.setMenuBar(new InGameMenuBar(this.freeColClient,
                new MenuMouseMotionListener(this.freeColClient, this)));
    }

    /**
     * Reset the menu bar.
     */
    public void resetMenuBar() {
        this.freeColClient.updateActions();
        this.parentFrame.resetMenuBar();
    }

    /**
     * Update the menu bar.
     */
    public void updateMenuBar() {
        this.freeColClient.updateActions();
        this.parentFrame.updateMenuBar();
    }

    /**
     * Quit the canvas.
     */
    public void quit() {
        destroyFrame();
    }

    /**
     * Shows the given video Component.
     *
     * @param vp The video Component.
     * @param ml A MouseListener for stopping the video.
     * @param kl A KeyListener for stopping the video.
     */
    public void addVideo(final Component vp,
                         final MouseListener ml,
                         final KeyListener kl) {
        addMouseListener(ml);
        addKeyListener(kl);
        addCentered(vp, JLayeredPane.PALETTE_LAYER);
    }

    /**
     * Closes all the menus that are currently open.
     */
    public void closeMenus() {
        for (JInternalFrame jif : getAllFrames()) {
            for (Component c : jif.getContentPane().getComponents()) {
                notifyClose(c, jif);
            }
            jif.dispose();
        }
        while (!dialogs.isEmpty()) {
            FreeColDialog<?> dialog = dialogs.remove(0);
            dialog.dispose();
        }
    }

    /**
     * Close a panel by class name.
     *
     * @param panel The panel to close.
     */
    public void closePanel(String panel) {
        if (panel.endsWith("Panel")) {
            for (Component c1 : getComponents()) {
                if (c1 instanceof JInternalFrame) {
                    for (Component c2 : ((JInternalFrame)c1).getContentPane()
                             .getComponents()) {
                        if (panel.equals(c2.getClass().getName())) {
                            notifyClose(c2, (JInternalFrame)c1);
                            return;
                        }
                    }
                }
            }
        } else if (panel.endsWith("Dialog")) {
            for (FreeColDialog<?> fcd : new ArrayList<>(dialogs)) {
                if (panel.equals(fcd.getClass().getName())) {
                    dialogs.remove(fcd);
                    fcd.dispose();
                    return;
                }
            }
        }        
    }

    /**
     * Closes the {@link MainPanel}.
     */
    public void closeMainPanel() {
       if (mainPanel != null) {
          remove(mainPanel);
          mainPanel = null;
       }
    }

    /**
     * Closes the {@code StatusPanel}.
     *
     * @see #showStatusPanel
     */
    public void closeStatusPanel() {
        if (statusPanel.isVisible()) {
            remove(statusPanel);
        }
    }

    /**
     * Tells that a chat message was received.
     *
     * @param message The chat message.
     */
    public void displayChat(GUIMessage message) {
        chatDisplay.addMessage(message);
        repaint(0, 0, getWidth(), getHeight());
    }

    /**
     * Add a dialog to the current dialog list.
     *
     * @param fcd The dialog to add.
     */
    private void dialogAdd(FreeColDialog<?> fcd) {
        dialogs.add(fcd);
    }

    /**
     * Remove a dialog from the current dialog list.
     *
     * @param fcd The dialog to remove.
     */
    public void dialogRemove(FreeColDialog<?> fcd) {
        dialogs.remove(fcd);
    }

    /**
     * Get any panel this {@code Canvas} is displaying.
     *
     * @return A {@code Component} the {@code Canvas} is
     *         displaying, or null if none found.
     */
    public Component getShowingSubPanel() {
        for (Component c : getComponents()) {
            if (c instanceof ToolBoxFrame) {
                continue;
            }
            if (c instanceof JInternalFrame) {
                return c;
            } else if (c instanceof JInternalFrame.JDesktopIcon) {
                return c;
            }
        }
        return null;
    }

    /**
     * Attach a closing callback to any current error panel.
     *
     * @param callback The {@code Runnable} to attach.
     * @return True if an error panel was present.
     */
    public boolean onClosingErrorPanel(Runnable callback) {
        ErrorPanel ep = getExistingFreeColPanel(ErrorPanel.class);
        if (ep == null) return false;
        ep.addClosingCallback(callback);
        return true;
    }

    /**
     * Checks if this {@code Canvas} displaying another panel.
     * <p>
     * Note that the previous implementation could throw exceptions
     * in some cases, thus the change.
     *
     * @return {@code true} if the {@code Canvas} is displaying an
     *         internal frame.
     */
    private boolean isShowingSubPanel() {
        return getShowingSubPanel() != null;
    }

    /**
     * Refresh this canvas.
     */
    public void refresh() {
        repaint(0, 0, getWidth(), getHeight());
    }

    /**
     * Removes the given component from this canvas without
     * updating the menu bar.
     *
     * @param comp The {@code Component} to remove.
     */
    public void removeFromCanvas(Component comp) {
        if (comp == null) return;

        final Rectangle updateBounds = comp.getBounds();
        final JInternalFrame jif = getInternalFrame(comp);
        if (jif != null) {
            notifyClose(comp, jif);
        }
        if (jif != null && jif != comp) {
            jif.dispose();
        } else {
            // Java 1.7.0 as seen on Fedora with:
            //   Java version: 1.7.0_40
            //   Java WM version: 24.0-b56
            // crashes here deep in the java libraries.
            try {
                super.remove(comp);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Java crash", e);
            }
        }
        repaint(updateBounds.x, updateBounds.y,
                updateBounds.width, updateBounds.height);
    }

    /**
     * Removes components that is only used when in game.
     */
    public void removeInGameComponents() {
        // remove listeners, they will be added when launching the new game...
        KeyListener[] keyListeners = getKeyListeners();
        for (KeyListener keyListener : keyListeners) {
            removeKeyListener(keyListener);
        }

        MouseListener[] mouseListeners = getMouseListeners();
        for (MouseListener mouseListener : mouseListeners) {
            removeMouseListener(mouseListener);
        }

        MouseMotionListener[] mouseMotionListeners = getMouseMotionListeners();
        for (MouseMotionListener mouseMotionListener : mouseMotionListeners) {
            removeMouseMotionListener(mouseMotionListener);
        }

        for (Component c : getComponents()) {
            removeFromCanvas(c);
        }
    }

    /**
     * Set preferred size to saved size, or to the given
     * {@code Dimension} if no saved size was found. Call this
     * method in the constructor of a FreeColPanel in order to
     * remember its size and position.
     *
     * @param comp The {@code Component} to use.
     * @param d The {@code Dimension} to use as default.
     */
    public void restoreSavedSize(Component comp, Dimension d) {
        final Dimension pref = comp.getPreferredSize();
        final Dimension sugg = (d == null) ? pref : d;
        boolean save = false;

        Dimension size = getSavedSize(comp);
        if (size == null) {
            size = new Dimension(pref);
            save = true;
        }

        // Fix up broken/outdated saved sizes
        if(size.width < sugg.width) {
            size.width = sugg.width;
            save = true;
        }
        if(size.height < sugg.height) {
            size.height = sugg.height;
            save = true;
        }
        if(size.width < pref.width) {
            size.width = pref.width;
            save = true;
        }
        if(size.height < pref.height) {
            size.height = pref.height;
            save = true;
        }

        if(save) {
            saveSize(comp, size);
        }

        if (!pref.equals(size)) {
            comp.setPreferredSize(size);
        }
    }

    public void setupMouseListeners() {
        addMouseListener(new CanvasMouseListener(freeColClient));
        addMouseMotionListener(new CanvasMouseMotionListener(freeColClient, this));
    }
    
    /**
     * Closes all panels, changes the background and shows the main menu.
     */
    public void mainTitle() {
        // FIXME: check if the GUI object knows that we're not
        // inGame. (Retrieve value of GUI::inGame.)  If GUI thinks
        // we're still in the game then log an error because at this
        // point the GUI should have been informed.
        removeInGameComponents();
        showMainPanel();
        repaint();
    }

    /**
     * Refresh the player's table (called when a new player is added
     * from PreGameInputHandler.addPlayer).
     */
    public void refreshPlayersTable() {
        startGamePanel.refreshPlayersTable();
    }

    /**
     * Displays the {@code ChatPanel}.
     *
     * @see ChatPanel
     */
    public void showChatPanel() {
        // FIXME: does it have state, or can we create a new one?
        if (freeColClient.getSinglePlayer()) return; // chat with who?
        showSubPanel(chatPanel, true);
    }

    /**
     * Shows the {@code MainPanel}.
     */
    public void showMainPanel() {
        closeMenus();
        this.parentFrame.removeMenuBar();
        mainPanel = new MainPanel(freeColClient);
        addCentered(mainPanel, JLayeredPane.DEFAULT_LAYER);
        mainPanel.requestFocus();
    }

    /**
     * Display the map editor transform panel.
     */
    public void showMapEditorTransformPanel() {
        MapEditorTransformPanel panel
            = new MapEditorTransformPanel(freeColClient);
        JInternalFrame f = addAsFrame(panel, true, PopupPosition.CENTERED,
                                      false);
        f.setLocation(f.getX(), 50);
        repaint();
    }

    /**
     * Displays the {@code ServerListPanel}.
     *
     * @param serverList The list containing the servers retrieved from the
     *     metaserver.
     * @see ServerListPanel
     */
    public void showServerListPanel(List<ServerInfo> serverList) {
        closeMenus();
        serverListPanel.initialize(serverList);
        showSubPanel(serverListPanel, true);
    }

    /**
     * Displays the {@code StartGamePanel}.
     *
     * @param singlePlayerMode True to start a single player game.
     * @see StartGamePanel
     */
    public void showStartGamePanel(boolean singlePlayerMode) {
        closeMenus();
        startGamePanel.initialize(singlePlayerMode);
        showSubPanel(startGamePanel, false);
    }

    /**
     * Shows a status message that cannot be dismissed.  The panel
     * will be removed when another component is added to this
     * {@code Canvas}.  This includes all the
     * {@code showXXX}-methods. In addition,
     * {@link #closeStatusPanel()} also removes this panel.
     *
     * @param message The text message to display on the status panel.
     * @see StatusPanel
     */
    public void showStatusPanel(String message) {
        statusPanel.setStatusMessage(message);
        addCentered(statusPanel, JLayeredPane.POPUP_LAYER);
    }

    /**
     * Shows a tile popup.
     *
     * @param tile The {@code Tile} where the popup occurred.
     * @return True if the popup was shown.
     */
    public boolean showTilePopup(Tile tile) {
        TilePopup tp = new TilePopup(freeColClient, this, tile);
        if (tp.hasItem()) {
            Point point = mapViewer.calculateTilePosition(tile, true);
            tp.show(this, point.x, point.y);
            tp.repaint();
            return true;
        }
        return false;
    }

    /**
     * Display the trade route input panel for a given trade route.
     *
     * @param newRoute The {@code TradeRoute} to display.
     * @return The {@code TradeRouteInputPanel}.
     */
    public TradeRouteInputPanel showTradeRouteInputPanel(TradeRoute newRoute) {
        TradeRouteInputPanel panel
            = new TradeRouteInputPanel(freeColClient, newRoute);
        showSubPanel(panel, null, true);
        return panel;
    }

    // Override JComponent

    /**
     * {@inheritDoc}
     */
    @Override
    public void paintComponent(Graphics g) {
        Dimension newSize = checkResize();
        if (newSize != null) {
            updateMapControlsInCanvas();
            mapViewer.changeSize(newSize);
        }

        Graphics2D g2d = (Graphics2D) g;
        chatDisplay.removeOldMessages();
        Dimension size = getSize();

        if (freeColClient.isMapEditor()) {
            if (hasMap()) {
                mapViewer.displayMap(g2d);
            } else {
                g2d.setColor(Color.BLACK);
                g2d.fillRect(0, 0, size.width, size.height);
            }

        } else if (freeColClient.isInGame() && hasMap()) {
            mapViewer.displayMap(g2d);

            // Toggle grey layer
            if (freeColClient.currentPlayerIsMyPlayer()) {
                if (greyLayer.getParent() != null) {
                    removeFromCanvas(greyLayer);
                }
            } else {
                greyLayer.setBounds(0, 0, size.width, size.height);
                greyLayer.setPlayer(freeColClient.getGame().getCurrentPlayer());
                if (greyLayer.getParent() == null) {
                    addToCanvas(greyLayer, JLayeredPane.DRAG_LAYER);
                }
            }

            // paint chat display
            chatDisplay.display(g2d, this.imageLibrary, size);

        } else {
            /* main menu */
            // TODO: Check if its right to sometimes have an unfocused map
            //       ingame and end up here after clicking outside map.
            final Image bgImage = ImageLibrary.getCanvasBackgroundImage();
            if (bgImage != null) {
                // Get the background without scaling, to avoid wasting
                // memory needlessly keeping an unbounded number of rescaled
                // versions of the largest image in FreeCol, forever.
                // Draw background image with scaling.
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2d.drawImage(bgImage, 0, 0, size.width, size.height, this);
                String versionStr = "v. " + FreeCol.getVersion();
                Font oldFont = g2d.getFont();
                Color oldColor = g2d.getColor();
                Font newFont = oldFont.deriveFont(Font.BOLD);
                TextLayout layout = new TextLayout(versionStr, newFont,
                    g2d.getFontRenderContext());
                Rectangle2D bounds = layout.getBounds();
                float x = size.width - (float) bounds.getWidth() - 5;
                float y = size.height - (float) bounds.getHeight();
                g2d.setColor(Color.white);
                layout.draw(g2d, x, y);
                g2d.setFont(oldFont);
                g2d.setColor(oldColor);
            } else {
                g2d.setColor(Color.BLACK);
                g2d.fillRect(0, 0, size.width, size.height);
            }
        }
    }


    // Override Container

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove(Component comp) {
        removeFromCanvas(comp);
        updateMenuBar();
    }
}
