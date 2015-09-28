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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.BasicInternalFrameUI;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.action.FreeColAction;
import net.sf.freecol.client.gui.panel.*;
import net.sf.freecol.client.gui.panel.LabourData.UnitData;
import net.sf.freecol.common.ServerInfo;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.Direction;
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
import net.sf.freecol.common.option.IntegerOption;
import net.sf.freecol.common.option.Option;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.common.resources.ResourceManager;


/**
 * The main container for the other GUI components in FreeCol. This
 * container is where the panels, dialogs and menus are added. In
 * addition, this is the component in which the map graphics are
 * displayed.
 *
 * <b>Displaying panels and a dialogs</b> <br>
 * <br>
 * <code>Canvas</code> contains methods to display various panels and dialogs.
 * Most of these methods use {@link net.sf.freecol.common.i18n i18n} to get
 * localized text.  Dialogs return results, and may be modal or non-modal.
 */
public final class Canvas extends JDesktopPane {

    private static final Logger logger = Logger.getLogger(Canvas.class.getName());

    /** A wrapper class for non-modal dialogs. */
    private class DialogCallback<T> implements Runnable {
        
        /** The dialog to show. */
        private final FreeColDialog<T> fcd;

        /** An optional tile to guide the dialog placement. */
        private final Tile tile;

        /** The handler for the dialog response. */
        private final DialogHandler<T> handler;


        /**
         * Constructor.
         */
        public DialogCallback(FreeColDialog<T> fcd, Tile tile,
                              DialogHandler<T> handler) {
            this.fcd = fcd;
            this.tile = tile;
            this.handler = handler;
        }


        // Implement Runnable

        @Override
        public void run() {
            // Display the dialog...
            viewFreeColDialog(fcd, tile);
            // ...and use another thread to wait for a dialog response...
            new Thread(fcd.toString()) {
                @Override
                public void run() {
                    while (!fcd.responded()) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {}
                    }
                    // ...before handling the result.
                    handler.handle(fcd.getResponse());
                }
            }.start();
        }
    };

    private static enum PopupPosition {
        ORIGIN,
        CENTERED,
        CENTERED_LEFT,
        CENTERED_RIGHT,
    }

    /** Number of tries to find a clear spot on the canvas. */
    private static final int MAXTRY = 3;

    /** A class for frames being used as tool boxes. */
    private static class ToolBoxFrame extends JInternalFrame {}

    /** The game client. */
    private final FreeColClient freeColClient;

    /** The parent GUI. */
    private final SwingGUI gui;

    private final GraphicsDevice graphicsDevice;

    /** The parent frame, either a window or the full screen. */
    private FreeColFrame frame;

    private boolean windowed;

    private MainPanel mainPanel;

    private final StartGamePanel startGamePanel;

    private final StatusPanel statusPanel;

    private final ChatPanel chatPanel;

    private final ChatDisplay chatDisplay;

    private final MapViewer mapViewer;

    private Point gotoDragPoint;

    private GrayLayer greyLayer;

    private final ServerListPanel serverListPanel;

    /** Used to detect resizing. */
    private Dimension oldSize = null;

    private boolean clientOptionsDialogShowing = false;

    private LoadingSavegameDialog loadingSavegameDialog;

    /** Filters for loadable game files. */
    private FileFilter[] fileFilters = null;

    /** The dialogs in view. */
    private final List<FreeColDialog<?>> dialogs = new ArrayList<>();


    /**
     * The constructor to use.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param graphicsDevice The used graphics device.
     * @param gui The gui.
     * @param desiredSize The desired size of the frame.
     * @param mapViewer The object responsible of drawing the map onto
     *     this component.
     */
    Canvas(final FreeColClient freeColClient,
           final GraphicsDevice graphicsDevice,
           final SwingGUI gui,
           final Dimension desiredSize,
           MapViewer mapViewer) {
        this.freeColClient = freeColClient;
        this.gui = gui;
        this.graphicsDevice = graphicsDevice;
        chatDisplay = new ChatDisplay();
        this.mapViewer = mapViewer;

        // Determine if windowed mode should be used and set the window size.
        Rectangle windowBounds = null;
        if (desiredSize == null) {
            if(graphicsDevice.isFullScreenSupported()) {
                windowed = false;
                logger.info("Full screen mode used.");
            } else {
                windowed = true;
                logger.warning("Full screen mode not supported.");
                System.err.println(Messages.message("client.fullScreen"));
            }
        } else {
            windowed = true;
            if(desiredSize.width > 0 && desiredSize.height > 0) {
                windowBounds = new Rectangle(desiredSize);
                logger.info("Windowed mode using desired window size of " + desiredSize);
            } else {
                logger.info("Windowed mode used.");
            }
        }

        setDoubleBuffered(true);
        setOpaque(false);
        setLayout(null);

        startGamePanel = new StartGamePanel(freeColClient);
        serverListPanel = new ServerListPanel(freeColClient,
            freeColClient.getConnectController());
        statusPanel = new StatusPanel(freeColClient);
        chatPanel = new ChatPanel(freeColClient);

        setFocusable(true);
        setFocusTraversalKeysEnabled(false);

        createKeyBindings();
        createFrame(null, windowBounds);
        mapViewer.startCursorBlinking();
        logger.info("Canvas created.");
    }

    boolean isWindowed() {
        return windowed;
    }

    /**
     * Change the windowed mode.
     */
    void changeWindowedMode() {
        // Clean up the old frame
        JMenuBar menuBar = null;
        Rectangle windowBounds = null;
        if (frame != null) {
            menuBar = frame.getJMenuBar();
            if (windowed) {
                windowBounds = frame.getBounds();
            }
            frame.setVisible(false);
            frame.dispose();
        }
        windowed = !windowed;

        createFrame(menuBar, windowBounds);
    }

    private void createFrame(JMenuBar menuBar, Rectangle windowBounds) {
        // FIXME: Check this:
        // User might have moved window to new screen in a
        // multi-screen setup, so make this.gd point to the current screen.
        frame = new FreeColFrame(freeColClient, graphicsDevice,
            menuBar, this, windowed, windowBounds);
        updateSizes();
        frame.setVisible(true);
    }

    /**
     * Start the GUI for the map editor.
     */
    void startMapEditorGUI() {
        if (frame == null) return;

        // We may need to reset the zoom value to the default value
        gui.resetMapZoom();

        frame.setMapEditorMenuBar();
        showMapEditorTransformPanel();

        CanvasMapEditorMouseListener listener
            = new CanvasMapEditorMouseListener(freeColClient, this);
        addMouseListener(listener);
        addMouseMotionListener(listener);
    }

    /**
     * Quit the GUI.  All that is required is to exit the full screen.
     */
    void quit() throws Exception {
        if (frame != null && !windowed) {
            frame.exitFullScreen();
        }
    }

    /**
     * In game initializations.
     */
    void initializeInGame() {
        if (frame == null) return;
        frame.setInGameMenuBar();
    }

    /**
     * Reset the menu bar.
     */
    void resetMenuBar() {
        if (frame == null) return;
        frame.resetMenuBar();
    }

    /**
     * Update the menu bar.
     */
    void updateMenuBar() {
        if (frame == null) return;
        frame.updateMenuBar();
    }

    /**
     * Scroll the map in the given direction.
     *
     * @param direction The <code>Direction</code> to scroll in.
     * @return True if scrolling occurred.
     */
    boolean scrollMap(Direction direction) {
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
    Tile convertToMapTile(int x, int y) {
        return mapViewer.convertToMapTile(x, y);
    }

    /**
     * Get the view mode.
     *
     * @return The view mode.
     */
    public int getViewMode() {
        return mapViewer.getViewMode();
    }

    /**
     * Gets the active unit.
     *
     * @return The <code>Unit</code>.
     */
    Unit getActiveUnit() {
        return mapViewer.getActiveUnit();
    }

    /**
     * Set the current active unit path.
     *
     * @param path The current <code>PathNode</code>.
     */
    void setCurrentPath(PathNode path) {
        mapViewer.setCurrentPath(path);
    }

    /**
     * Sets the path of the active unit to display it.
     */
    void updateCurrentPathForActiveUnit() {
        mapViewer.updateCurrentPathForActiveUnit();
    }

    /**
     * Gets the point at which the map was clicked for a drag.
     *
     * @return The Point where the mouse was initially clicked.
     */
    Point getDragPoint() {
        return gotoDragPoint;
    }

    /**
     * Sets the point at which the map was clicked for a drag.
     *
     * @param x The mouse's x position.
     * @param y The mouse's y position.
     */
    void setDragPoint(int x, int y) {
        gotoDragPoint = new Point(x, y);
    }

    /**
     * Checks if there is currently a goto operation on the mapboard.
     *
     * @return True if a goto operation is in progress.
     */
    boolean isGotoStarted() {
        return mapViewer.isGotoStarted();
    }

    /**
     * Gets the path to be drawn on the map.
     *
     * @return The path that should be drawn on the map or
     *     <code>null</code> if no path should be drawn.
     */
    PathNode getGotoPath() {
        return mapViewer.getGotoPath();
    }

    /**
     * Sets the path to be drawn on the map.
     *
     * @param gotoPath The path that should be drawn on the map
     *     or <code>null</code> if no path should be drawn.
     */
    void setGotoPath(PathNode gotoPath) {
        mapViewer.setGotoPath(gotoPath);
        refresh();
    }

    /**
     * Starts a goto operation.
     */
    void startGoto() {
        setCursor((java.awt.Cursor)UIManager.get("cursor.go"));
        mapViewer.startGoto();
        refresh();
    }

    /**
     * Stops any ongoing goto operation.
     */
    void stopGoto() {
        setCursor(null);
        mapViewer.stopGoto();
        refresh();
    }


    // Internals

    /**
     * Adds a component on this Canvas inside a frame.
     *
     * @param comp The component to add to the canvas.
     * @param toolBox Should be set to true if the resulting frame is
     *     used as a toolbox (that is: it should not be counted as a
     *     frame).
     * @param popupPosition A preferred <code>PopupPosition</code>.
     * @param resizable Whether this component can be resized.
     * @return The <code>JInternalFrame</code> that was created and added.
     */
    private JInternalFrame addAsFrame(JComponent comp, boolean toolBox,
                                      PopupPosition popupPosition,
                                      boolean resizable) {
        final int FRAME_EMPTY_SPACE = 60;

        final JInternalFrame f = (toolBox) ? new ToolBoxFrame()
            : new JInternalFrame();
        if (f.getContentPane() instanceof JComponent) {
            JComponent c = (JComponent) f.getContentPane();
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
        this.add(f, MODAL_LAYER);
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
     * @param comp The <code>Component</code> to add to this canvas.
     * @param i The layer to add the component to (see JLayeredPane).
     */
    private void addCentered(Component comp, Integer i) {
        comp.setLocation((getWidth() - comp.getWidth()) / 2,
                         (getHeight() - comp.getHeight()) / 2);

        this.add(comp, i);
    }

    /**
     * Adds a component to this Canvas.  Removes the statusPanel if
     * visible (and <code>comp != statusPanel</code>).
     *
     * @param comp The <code>Component</code> to add to this canvas.
     * @param i The layer to add the component to (see JLayeredPane).
     */
    private void addToCanvas(Component comp, Integer i) {
        if (comp != statusPanel && !(comp instanceof JMenuItem)
            && statusPanel.isVisible()) {
            removeFromCanvas(statusPanel);
        }

        try {
            super.add(comp, (i == null) ? JLayeredPane.DEFAULT_LAYER : i);
        } catch (Exception e) {
            logger.log(Level.WARNING, "addToCanvas(" + comp + ", " + i
                + ") failed.", e);
        }
    }

    /**
     * Choose a location for a component.
     *
     * @param comp The <code>Component</code> to place.
     * @param width The component width to use.
     * @param height The component height to use.
     * @param popupPosition An optional <code>PopupPosition</code> hint.
     * @return A suitable <code>Point</code> to place the component.
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
     * @return A <code>Point</code> to place the component at or null
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

        List<Component> allComponents = Arrays.stream(this.getComponents())
            .filter(c -> !(c instanceof GrayLayer) && c.isValid())
            .collect(Collectors.toList());
        for (FreeColDialog<?> fcd : dialogs) allComponents.add(fcd);

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
                if (c.getBounds().intersects(r)) {
                    Rectangle rr = c.getBounds().intersection(r);
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
     * Gets any currently displayed colony panel for the specified colony.
     *
     * This is distinct from {@link #getExistingFreeColPanel} because
     * there can be multiple ColonyPanels.
     *
     * @param colony The <code>Colony</code> to check.
     * @return A currently displayed colony panel, or null if not found.
     */
    private ColonyPanel getColonyPanel(Colony colony) {
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
     * Gets the internal frame for the given component.
     *
     * @param c The <code>Component</code>.
     * @return The given component if this is an internal frame or the
     *     first parent that is an internal frame.  Returns
     *     <code>null</code> if no internal frame is found.
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
     * @param tile A <code>Tile</code> to be made visible.
     * @return A <code>PopupPosition</code> for a panel to be displayed.
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
     * @param comp The <code>Component</code> to use.
     * @return The saved position as a <code>Point</code>, or null if no
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

        String className = comp.getClass().getName();
        try {
            return new Point(co.getInteger(className + ".x"),
                             co.getInteger(className + ".y"));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get the saved size of a component.
     *
     * @param comp The <code>Component</code> to use.
     * @return A <code>Dimension</code> for the component or null if
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

        String className = comp.getClass().getName();
        try {
            return new Dimension(co.getInteger(className + ".w"),
                                 co.getInteger(className + ".h"));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Initialize the file filters to filter for saved games.
     *
     * @return File filters for the FreeCol save game extension.
     */
    private FileFilter[] getFileFilters() {
        if (fileFilters == null) {
            String s = Messages.message("filter.savedGames");
            fileFilters = new FileFilter[] {
                new FileNameExtensionFilter(s, FreeCol.FREECOL_SAVE_EXTENSION)
            };
        }
        return fileFilters;
    }

    /**
     * A component is closing.  Some components need position and size
     * to be saved.
     *
     * @param c The closing <code>Component</code>.
     * @param frame The enclosing <code>JInternalFrame</code>.
     */
    private void notifyClose(Component c, JInternalFrame frame) {
        if (frame == null) return;

        if (c instanceof FreeColPanel) {
            FreeColPanel fcp = (FreeColPanel)c;
            fcp.firePropertyChange("closing", false, true);
            
            savePosition(fcp, frame.getLocation());
            saveSize(fcp, fcp.getSize());
        }
    }

    /**
     * Remove the panels derived from the EuropePanel.
     */
    private void removeEuropeanSubpanels() {
        FreeColPanel panel;
        if ((panel = getExistingFreeColPanel(RecruitPanel.class)) != null)
            removeFromCanvas(panel);
        if ((panel = getExistingFreeColPanel(PurchasePanel.class)) != null)
            removeFromCanvas(panel);
        if ((panel = getExistingFreeColPanel(TrainPanel.class)) != null)
            removeFromCanvas(panel);
    }

    /**
     * Save an <code>int</code> value to the saved ClientOptions,
     * using the name of the components class plus the given key as
     * and identifier.
     *
     * @param className The class name for the component.
     * @param key The key to save.
     * @param value The value to save.
     */
    private void saveInteger(String className, String key, int value) {
        if (freeColClient != null
            && freeColClient.getClientOptions() != null) {
            Option o = freeColClient.getClientOptions()
                .getOption(className + key);
            if (o == null) {
                Specification specification = (freeColClient.getGame() == null)
                    ? null : freeColClient.getGame().getSpecification();
                IntegerOption io = new IntegerOption(className + key,
                                                     specification);
                io.setValue(value);
                freeColClient.getClientOptions().add(io);
            } else if (o instanceof IntegerOption) {
                ((IntegerOption)o).setValue(value);
            }
        }
    }

    /**
     * Save the position of a component.
     *
     * @param comp The <code>Component</code> to use.
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
     * @param comp The <code>Component</code> to use.
     * @param size The <code>Dimension</code> to save.
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
        if (mapViewer.getViewMode() != GUI.MOVE_UNITS_MODE) return;
        for (FreeColDialog<?> f : dialogs) {
            if (f.isModal()) return;
        }
        mapViewer.restartBlinking();
    }

    /**
     * Stop blinking on the map.
     */
    private void stopBlinking() {
        mapViewer.stopBlinking();
    }

    /**
     * Displays the given dialog, optionally making sure a tile is visible.
     *
     * @param freeColDialog The dialog to be displayed
     * @param tile An optional <code>Tile</code> to make visible (not
     *     under the dialog!)
     * @return The {@link FreeColDialog#getResponse reponse} returned by
     *     the dialog.
     */
    private <T> T showFreeColDialog(FreeColDialog<T> freeColDialog,
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
     * @param tile A <code>Tile</code> to make visible (not under the panel!)
     * @param resizable Should the panel be resizable?
     */
    private void showFreeColPanel(FreeColPanel panel, Tile tile,
                                  boolean resizable) {
        showSubPanel(panel, setOffsetFocus(tile), resizable);
    }

    /**
     * Displays a <code>FreeColPanel</code>.
     *
     * @param panel <code>FreeColPanel</code>, panel to show
     * @param resizable Should the panel be resizable?
     */
    private void showSubPanel(FreeColPanel panel, boolean resizable) {
        showSubPanel(panel, PopupPosition.CENTERED, resizable);
    }

    /**
     * Displays a <code>FreeColPanel</code> at a generalized position.
     *
     * @param panel <code>FreeColPanel</code>, panel to show
     * @param popupPosition <code>PopupPosition</code> The generalized
     *     position to place the panel.
     * @param resizable Should the panel be resizable?
     */
    private void showSubPanel(FreeColPanel panel, PopupPosition popupPosition,
                              boolean resizable) {
        repaint();
        addAsFrame(panel, false, popupPosition, resizable);
        panel.requestFocus();
    }


    // Public API

    /**
     * Adds a component to this Canvas.
     *
     * @param comp The <code>Component</code> to add.
     * @return The component argument.
     */
    @Override
    public Component add(Component comp) {
        this.add(comp, JLayeredPane.DEFAULT_LAYER);
        return comp;
    }

    /**
     * Adds a component to this Canvas.
     *
     * @param comp The <code>Component</code> to add to this canvas.
     * @param i The layer to add the component to (see JLayeredPane).
     */
    public void add(Component comp, Integer i) {
        addToCanvas(comp, i);
        gui.updateMenuBar();
    }

    /**
     * Closes all the menus that are currently open.
     */
    void closeMenus() {
        for (JInternalFrame frame : getAllFrames()) {
            for (Component c : frame.getContentPane().getComponents()) {
                notifyClose(c, frame);
            }
            frame.dispose();
        }
        while (!dialogs.isEmpty()) {
            FreeColDialog<?> dialog = dialogs.remove(0);
            dialog.dispose();
        }
    }

    /**
     * Closes the {@link MainPanel}.
     */
    void closeMainPanel() {
       if (mainPanel != null) {
          remove(mainPanel);
          mainPanel = null;
       }
    }

    /**
     * Closes the <code>StatusPanel</code>.
     *
     * @see #showStatusPanel
     */
    void closeStatusPanel() {
        if (statusPanel.isVisible()) {
            remove(statusPanel);
        }
    }

    /**
     * Checks if this <code>Canvas</code> contains any in game components.
     *
     * @return <code>true</code> if there is any in game components.
     */
    boolean containsInGameComponents() {
        KeyListener[] keyListeners = getKeyListeners();
        if (keyListeners.length > 0) {
            return true;
        }

        MouseListener[] mouseListeners = getMouseListeners();
        if (mouseListeners.length > 0) {
            return true;
        }

        MouseMotionListener[] mouseMotionListeners = getMouseMotionListeners();
        if (mouseMotionListeners.length > 0) {
            return true;
        }

        return false;
    }

    /**
     * Tells that a chat message was received.
     *
     * @param message The chat message.
     */
    void displayChatMessage(GUIMessage message) {
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
    void dialogRemove(FreeColDialog<?> fcd) {
        dialogs.remove(fcd);
    }

    /**
     * Gets a currently displayed FreeColPanel of a given type.
     *
     * @param type The type of <code>FreeColPanel</code> to look for.
     * @return A currently displayed <code>FreeColPanel</code> of the
     *     requested type, or null if none found.
     */
    private <T extends FreeColPanel> T getExistingFreeColPanel(Class<T> type) {
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
     * Gets the last <code>LoadingSavegameDialog</code>.
     *
     * FIXME: clean this up
     *
     * @return The <code>LoadingSavegameDialog</code>.
     */
    LoadingSavegameDialog getLoadingSavegameDialog() {
        return loadingSavegameDialog;
    }

    /**
     * Get any panel this <code>Canvas</code> is displaying.
     *
     * @return A <code>Component</code> the <code>Canvas</code> is
     *         displaying, or null if none found.
     */
    Component getShowingSubPanel() {
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
     * Checks if a client options dialog is present.
     *
     * @return True if the client options are showing.
     */
    boolean isClientOptionsDialogShowing() {
        return clientOptionsDialogShowing;
    }

    /**
     * Checks if mapboard actions should be enabled.
     *
     * @return <code>true</code> if no internal frames are open.
     */
    boolean isMapboardActionsEnabled() {
        return !isShowingSubPanel();
    }

    /**
     * Checks if this <code>Canvas</code> displaying another panel.
     * <p>
     * Note that the previous implementation could throw exceptions
     * in some cases, thus the change.
     *
     * @return <code>true</code> if the <code>Canvas</code> is displaying an
     *         internal frame.
     */
    boolean isShowingSubPanel() {
        return getShowingSubPanel() != null;
    }

    /**
     * Refresh this canvas.
     */
    void refresh() {
        repaint(0, 0, getWidth(), getHeight());
    }

    /**
     * Removes the given component from this canvas.
     *
     * @param comp The <code>Component</code> to remove.
     */
    public void removeFromCanvas(Component comp) {
        if (comp == null) return;

        final Rectangle updateBounds = comp.getBounds();
        final JInternalFrame frame = getInternalFrame(comp);
        notifyClose(comp, frame);
        if (frame != null && frame != comp) {
            frame.dispose();
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
    void removeInGameComponents() {
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
     * <code>Dimension</code> if no saved size was found. Call this
     * method in the constructor of a FreeColPanel in order to
     * remember its size and position.
     *
     * @param comp The <code>Component</code> to use.
     * @param d The <code>Dimension</code> to use as default.
     */
    void restoreSavedSize(Component comp, Dimension d) {
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

    /**
     * Closes all panels, changes the background and shows the main menu.
     */
    void returnToTitle() {
        // FIXME: check if the GUI object knows that we're not
        // inGame. (Retrieve value of GUI::inGame.)  If GUI thinks
        // we're still in the game then log an error because at this
        // point the GUI should have been informed.
        removeInGameComponents();
        showMainPanel(null);
        repaint();
    }

    void setupMouseListeners() {
        addMouseListener(new CanvasMouseListener(freeColClient, this));
        addMouseMotionListener(new CanvasMouseMotionListener(freeColClient, this));
    }

    /**
     * Updates the sizes of the components on this Canvas.
     */
    private void updateSizes() {
        if (oldSize == null
                || oldSize.width != getWidth()
                || oldSize.height != getHeight()) {
            gui.updateMapControlsInCanvas();
            mapViewer.setSize(getSize());
            mapViewer.forceReposition();
            oldSize = getSize();
        }
    }


    // Override JComponent

    /**
     * {@inheritDoc}
     */
    @Override
    public void paintComponent(Graphics g) {
        updateSizes();
        Graphics2D g2d = (Graphics2D) g;
        chatDisplay.removeOldMessages();

        Dimension size = getSize();
        if ((freeColClient.getGame() != null)
                && (freeColClient.getGame().getMap() != null)
                && (mapViewer.getFocus() != null)
                && freeColClient.isInGame()) {
            /* ingame view */

            // paint map
            mapViewer.displayMap(g2d);

            // Grey out the map if it is not my turn (and a multiplayer game).
            if (!freeColClient.isMapEditor() && freeColClient.getGame() != null
                    && !freeColClient.currentPlayerIsMyPlayer()) {
                if (greyLayer == null) {
                    greyLayer = new GrayLayer(freeColClient);
                }
                if (greyLayer.getParent() == null) {
                    add(greyLayer, JLayeredPane.DRAG_LAYER);
                }
                greyLayer.setBounds(0, 0, size.width, size.height);
                greyLayer.setPlayer(freeColClient.getGame().getCurrentPlayer());
            } else {
                if (greyLayer != null && greyLayer.getParent() != null) {
                    removeFromCanvas(greyLayer);
                }
            }

            // paint chat display
            chatDisplay.display(g2d, mapViewer.getImageLibrary(), size);

        } else {
            if (!freeColClient.isMapEditor()) {
                /* main menu */
                // TODO: Check if its right to sometimes have an unfocused map
                //       ingame and end up here after clicking outside map.

                final String bgImageKey = "image.flavor.Canvas.map";
                if (ResourceManager.hasImageResource(bgImageKey)) {
                    // Get the background without scaling, to avoid wasting
                    // memory needlessly keeping an unbounded number of rescaled
                    // versions of the largest image in FreeCol, forever.
                    final Image bgImage = ResourceManager.getImage(bgImageKey);
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

            } else {
                /* map editor??? */
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
        gui.updateMenuBar();
        if (comp != statusPanel && !isShowingSubPanel()) {
            requestFocus();
        }
    }


    // Special handling for the startGamePanel.

    /**
     * Refresh the player's table (called when a new player is added
     * from PreGameInputHandler.addPlayer).
     */
    void refreshPlayersTable() {
        startGamePanel.refreshPlayersTable();
    }

    /**
     * Update the game options in the start panel.
     */
    void updateGameOptions() {
        startGamePanel.updateGameOptions();
    }

    /**
     * Update the map generator options in the start panel.
     */
    void updateMapGeneratorOptions() {
        startGamePanel.updateMapGeneratorOptions();
    }


    // Dialog display

    /**
     * Displays a modal dialog with text and a choice of options.
     *
     * @param tile An optional <code>Tile</code> to make visible (not
     *     under the dialog!)
     * @param obj An object that explains the choice for the user.
     * @param icon An optional icon to display.
     * @param cancelKey Key for the text of the optional cancel button.
     * @param choices The <code>List</code> containing the ChoiceItems to
     *            create buttons for.
     * @return The corresponding member of the values array to the selected
     *     option.
     */
    <T> T showChoiceDialog(Tile tile, Object obj, ImageIcon icon,
                           String cancelKey, List<ChoiceItem<T>> choices) {
        FreeColChoiceDialog<T> fcd
            = new FreeColChoiceDialog<>(freeColClient, frame, true, obj, icon,
                                         cancelKey, choices);
        return showFreeColDialog(fcd, tile);
    }

    /**
     * Displays a modal dialog with a text and a ok/cancel option.
     *
     * @param tile An optional <code>Tile</code> to make visible (not
     *     under the dialog!)
     * @param obj An object that explains the choice for the user.
     * @param icon An optional icon to display.
     * @param okKey The text displayed on the "ok"-button.
     * @param cancelKey The text displayed on the "cancel"-button.
     * @return True if the user clicked the "ok"-button.
     */
    boolean showConfirmDialog(Tile tile, Object obj, ImageIcon icon,
                              String okKey, String cancelKey) {
        FreeColConfirmDialog fcd
            = new FreeColConfirmDialog(freeColClient, frame, true, obj, icon,
                                       okKey, cancelKey);
        return showFreeColDialog(fcd, tile);
    }

    /**
     * Displays a modal dialog with a text field and a ok/cancel option.
     *
     * @param tile An optional tile to make visible (not under the dialog).
     * @param template A <code>StringTemplate</code> that explains the
     *     action to the user.
     * @param defaultValue The default value appearing in the text field.
     * @param okKey A key displayed on the "ok"-button.
     * @param cancelKey A key displayed on the optional "cancel"-button.
     * @return The text the user entered, or null if cancelled.
     */
    String showInputDialog(Tile tile, StringTemplate template,
                           String defaultValue,
                           String okKey, String cancelKey) {
        FreeColStringInputDialog fcd
            = new FreeColStringInputDialog(freeColClient, frame, true,
                                           Messages.message(template),
                                           defaultValue, okKey, cancelKey);
        return showFreeColDialog(fcd, tile);
    }

    /**
     * Displays the given dialog, optionally making sure a tile is visible.
     *
     * @param freeColDialog The dialog to be displayed
     * @param tile An optional <code>Tile</code> to make visible (not
     *     under the dialog!)
     */
    private <T> void viewFreeColDialog(final FreeColDialog<T> freeColDialog,
                                      Tile tile) {
        PopupPosition pp = setOffsetFocus(tile);

        // TODO: Remove compatibility code when all non-modal dialogs
        //       have been converted into panels.
        if(!freeColDialog.isModal()) {
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


    // Simple front ends to display each panel or dialog.

    void removeTradeRoutePanel(TradeRoutePanel panel) {
        remove(panel);
        TradeRouteInputPanel trip
            = getExistingFreeColPanel(TradeRouteInputPanel.class);
        if (trip != null) trip.cancelTradeRoute();
    }
        
    /**
     * Display the AboutPanel.
     */
    void showAboutPanel() {
        showSubPanel(new AboutPanel(freeColClient), false);
    }

    /**
     * Show the BuildQueuePanel for a given colony.
     *
     * @param colony The <code>Colony</code> to show the build queue of.
     */
    void showBuildQueuePanel(Colony colony) {
        BuildQueuePanel panel = getExistingFreeColPanel(BuildQueuePanel.class);
        if (panel == null || panel.getColony() != colony) {
            showSubPanel(new BuildQueuePanel(freeColClient, colony), true);
        }
    }

    /**
     * Show a build queue panel, with a special callback when it is closed.
     *
     * @param colony The <code>Colony</code> to show the build queue of.
     * @param callBack The <code>Runnable</code> that is run when the
     *     panel closes.
     */
    void showBuildQueuePanel(Colony colony, Runnable callBack) {
        FreeColPanel panel = new BuildQueuePanel(freeColClient, colony);
        panel.addClosingCallback(callBack);
        showSubPanel(panel, true);
    }

    /**
     * Display the <code>CaptureGoodsDialog</code>.
     *
     * @param unit The <code>Unit</code> capturing goods.
     * @param gl The list of <code>Goods</code> to choose from.
     * @param handler A <code>DialogHandler</code> for the dialog response.
     */
    void showCaptureGoodsDialog(Unit unit, List<Goods> gl,
                                DialogHandler<List<Goods>> handler) {
        SwingUtilities.invokeLater(
            new DialogCallback<>(
                new CaptureGoodsDialog(freeColClient, frame, unit, gl),
                null, handler));
    }

    /**
     * Displays the <code>ChatPanel</code>.
     *
     * @see ChatPanel
     */
    void showChatPanel() {
        // FIXME: does it have state, or can we create a new one?
        if (freeColClient.isSinglePlayer()) return; // chat with who?
        showSubPanel(chatPanel, true);
    }

    /**
     * Displays the <code>ChooseFoundingFatherDialog</code>.
     *
     * @param ffs The <code>FoundingFather</code>s to choose from.
     * @param handler A <code>DialogHandler</code> for the dialog response.
     */
    void showChooseFoundingFatherDialog(List<FoundingFather> ffs,
                                        DialogHandler<FoundingFather> handler) {
        SwingUtilities.invokeLater(
            new DialogCallback<>(
                new ChooseFoundingFatherDialog(freeColClient, frame, ffs),
                null, handler));
    }

    /**
     * Displays a dialog for setting client options.
     *
     * @return The modified <code>OptionGroup</code>, or null if not modified.
     */
    OptionGroup showClientOptionsDialog() {
        ClientOptionsDialog dialog = new ClientOptionsDialog(freeColClient, frame);
        OptionGroup group = null;
        clientOptionsDialogShowing = true;
        try {
            group = showFreeColDialog(dialog, null);
        } finally {
            clientOptionsDialogShowing = false;
        }
        return group;
    }

    /**
     * Displays the colony panel of the given <code>Colony</code>.
     * Defends against duplicates as this can duplicate messages
     * generated by multiple property change listeners registered
     * against the same colony.
     *
     * @param colony The colony whose panel needs to be displayed.
     * @param unit An optional <code>Unit</code> to select within the panel.
     * @return The colony panel.
     * @see ColonyPanel
     */
    public ColonyPanel showColonyPanel(Colony colony, Unit unit) {
        ColonyPanel panel = getColonyPanel(colony);
        if (panel == null) {
            panel = new ColonyPanel(freeColClient, colony);
            showFreeColPanel(panel, colony.getTile(), true);
        } else {
            panel.requestFocus();
        }
        if (unit != null) panel.setSelectedUnit(unit);
        return panel;
    }

    /**
     * Show the colopedia entry for a given node.
     *
     * @param nodeId The node identifier to display.
     */
    void showColopediaPanel(String nodeId) {
        showSubPanel(new ColopediaPanel(freeColClient, nodeId), true);
    }

    /**
     * Show a <code>ColorChooserPanel</code>.
     *
     * @param al An <code>ActionListener</code> to handle panel button
     *     presses.
     * @return The <code>ColorChooserPanel</code> created.
     */
    ColorChooserPanel showColorChooserPanel(ActionListener al) {
        ColorChooserPanel ccp = new ColorChooserPanel(freeColClient, al);
        showFreeColPanel(ccp, null, false);
        return ccp;
    }

    /**
     * Show the compact labour report.
     */
    void showCompactLabourReport() {
        CompactLabourReport details = new CompactLabourReport(freeColClient);
        details.initialize();
        showSubPanel(details, false);

    }

    /**
     * Show the compat labour report for the specified unit data.
     *
     * @param unitData The <code>UnitData</code> to display.
     */
    void showCompactLabourReport(UnitData unitData) {
        CompactLabourReport details = new CompactLabourReport(freeColClient,
                                                              unitData);
        details.initialize();
        showSubPanel(details, false);
    }

    /**
     * Display a dialog to confirm a declaration of independence.
     *
     * @return A list of names for a new nation.
     */
    List<String> showConfirmDeclarationDialog() {
        return showFreeColDialog(new ConfirmDeclarationDialog(freeColClient, frame),
                                 null);
    }

    /**
     * Display a panel showing the declaration of independence with
     * animated signature.
     */
    void showDeclarationPanel() {
        showSubPanel(new DeclarationPanel(freeColClient),
                     PopupPosition.CENTERED, false);
    }

    /**
     * Display the difficulty dialog for a given group.
     *
     * @param spec The enclosing <code>Specification</code>.
     * @param group The <code>OptionGroup</code> containing the difficulty.
     * @param editable If the options should be editable.
     * @return The resulting <code>OptionGroup</code>.
     */
    OptionGroup showDifficultyDialog(Specification spec,
                                     OptionGroup group, boolean editable) {
        return showFreeColDialog(new DifficultyDialog(freeColClient, frame,
                spec, group, editable),
            null);
    }

    /**
     * Displays the <code>DumpCargoDialog</code>.
     *
     * @param unit The <code>Unit</code> that is dumping.
     * @param handler A <code>DialogHandler</code> for the dialog response.
     */
    void showDumpCargoDialog(Unit unit, DialogHandler<List<Goods>> handler) {
        SwingUtilities.invokeLater(
            new DialogCallback<>(
                new DumpCargoDialog(freeColClient, frame, unit),
                unit.getTile(), handler));
    }

    /**
     * Display the EditOptionDialog.
     *
     * @param option The <code>Option</code> to edit.
     * @return The response returned by the dialog.
     */
    boolean showEditOptionDialog(Option option) {
        return showFreeColDialog(new EditOptionDialog(freeColClient, frame, option),
                                 null);
    }

    /**
     * Display the EditSettlementDialog.
     *
     * @param settlement The <code>IndianSettlement</code> to edit.
     */
    void showEditSettlementDialog(IndianSettlement settlement) {
        showFreeColDialog(new EditSettlementDialog(freeColClient, frame, settlement),
                          null);
    }

    /**
     * Shows the panel that allows the user to choose which unit will emigrate
     * from Europe.
     *
     * @param player The <code>Player</code> whose unit is emigrating.
     * @param fountainOfYouth Is this dialog displayed as a result of a
     *     fountain of youth.
     * @param handler A <code>DialogHandler</code> for the dialog response.
     */
    void showEmigrationDialog(Player player, boolean fountainOfYouth,
                              DialogHandler<Integer> handler) {
        SwingUtilities.invokeLater(
            new DialogCallback<>(
                new EmigrationDialog(freeColClient, frame, player.getEurope(),
                                     fountainOfYouth),
                null, handler));
    }

    /**
     * Display the EndTurnDialog with given units that could still move.
     *
     * @param units A list of <code>Unit</code>s that could still move.
     * @param handler A <code>DialogHandler</code> for the dialog response.
     */
    void showEndTurnDialog(List<Unit> units,
                                  DialogHandler<Boolean> handler) {
        SwingUtilities.invokeLater(
            new DialogCallback<>(
                new EndTurnDialog(freeColClient, frame, units),
                null, handler));
    }

    /**
     * Displays an error message.
     *
     * @param messageId The i18n-keyname of the error message to display.
     */
    void showErrorMessage(String messageId) {
        showErrorMessage(messageId, "Unspecified error: " + messageId);
    }

    /**
     * Displays an error message.
     *
     * @param messageId The i18n-keyname of the error message to display.
     * @param message An alternative (possibly non-i18n) message to
     *     display if the resource specified by <code>messageId</code>
     *     is unavailable.
     */
    void showErrorMessage(String messageId, String message) {
        String display = null;
        if (messageId != null) {
            display = Messages.message(messageId);
        }
        if (display == null || display.isEmpty()) display = message;
        ErrorPanel errorPanel = new ErrorPanel(freeColClient, display);
        showSubPanel(errorPanel, true);
    }

    /**
     * Displays the <code>EuropePanel</code>.
     *
     * @see EuropePanel
     */
    void showEuropePanel() {
        if (freeColClient.getGame() == null) return;
        EuropePanel panel = getExistingFreeColPanel(EuropePanel.class);
        if (panel == null) {
            panel = new EuropePanel(freeColClient, (getHeight() > 780));
            panel.addClosingCallback(() -> { removeEuropeanSubpanels(); });
            showSubPanel(panel, true);
        }
    }

    /**
     * Display an event panel.
     *
     * @param header The title.
     * @param image A resource key for the image to display.
     * @param footer Optional footer text.
     */
    void showEventPanel(String header, String image, String footer) {
        showSubPanel(new EventPanel(freeColClient, header, image, footer),
                     PopupPosition.CENTERED, false);
    }

    /**
     * Display the FindSettlementPanel.
     */
    void showFindSettlementPanel() {
        showSubPanel(new FindSettlementPanel(freeColClient),
                     PopupPosition.ORIGIN, true);
    }

    /**
     * Display the first contact dialog (which is really just a
     * non-modal confirm dialog).
     *
     * @param player The <code>Player</code> making contact.
     * @param other The <code>Player</code> to contact.
     * @param tile An optional <code>Tile</code> on offer.
     * @param settlementCount The number of settlements the other
     *     player has (from the server, other.getNumberOfSettlements()
     *     is wrong here!).
     * @param handler A <code>DialogHandler</code> for the dialog response.
     */
    void showFirstContactDialog(Player player, Player other,
                                Tile tile, int settlementCount,
                                DialogHandler<Boolean> handler) {
        SwingUtilities.invokeLater(
            new DialogCallback<>(
                new FirstContactDialog(freeColClient, frame, player, other, tile,
                                       settlementCount),
                tile, handler));
    }

    /**
     * Detailed view of a foreign colony when in debug mode.
     *
     * @param settlement The <code>Settlement</code> with the colony
     */
    void showForeignColony(Settlement settlement) {
        if (settlement instanceof Colony) {
            Colony colony = freeColClient.getFreeColServer().getGame()
                .getFreeColGameObject(settlement.getId(), Colony.class);
            showColonyPanel(colony, null);
        }
    }

    /**
     * Display the GameOptionsDialog.
     *
     * @param editable Should the game options be editable?
     * @param custom Whether to load custom options.
     * @return The <code>OptionGroup</code> selected.
     */
    OptionGroup showGameOptionsDialog(boolean editable,
                                             boolean custom) {
        GameOptionsDialog god = new GameOptionsDialog(freeColClient, frame, editable,
                                                      custom);
        return showFreeColDialog(god, null);
    }

    /**
     * Displays the high scores panel.
     *
     * @param messageId An optional message to add to the high scores panel.
     * @param scores The list of <code>HighScore</code>s to display.
     */
    void showHighScoresPanel(String messageId, List<HighScore> scores) {
        showSubPanel(new ReportHighScoresPanel(freeColClient, messageId, scores),
                     PopupPosition.CENTERED, true);
    }

    /**
     * Displays the panel of the given native settlement.
     *
     * @param indianSettlement The <code>IndianSettlement</code> to display.
     */
    void showIndianSettlementPanel(IndianSettlement indianSettlement) {
        IndianSettlementPanel panel
            = new IndianSettlementPanel(freeColClient, indianSettlement);
        showFreeColPanel(panel, indianSettlement.getTile(), true);
    }

    /**
     * Make image icon from an image.
     * Use only if you know having null is possible!
     *
     * @param image The <code>Image</code> to create an icon for.
     * @return The <code>ImageIcon</code>.
     */
    private static ImageIcon createImageIcon(Image image) {
        return (image==null) ? null : new ImageIcon(image);
    }

    /**
     * Make image icon from an object.
     *
     * @param display The FreeColObject to find an icon for.
     * @return The <code>ImageIcon</code> found.
     */
    private ImageIcon createObjectImageIcon(FreeColObject display) {
        return (display == null) ? null
            : createImageIcon(gui.getImageLibrary().getObjectImage(display, 2f));
    }

    /**
     * Shows a message with some information and an "OK"-button.
     *
     * @param displayObject Optional object for displaying an icon.
     * @param template The <code>StringTemplate</code> to display.
     */
    void showInformationMessage(FreeColObject displayObject,
                                       StringTemplate template) {
        ImageIcon icon = null;
        Tile tile = null;
        if(displayObject != null) {
            icon = createObjectImageIcon(displayObject);
            tile = (displayObject instanceof Location)
                ? ((Location)displayObject).getTile()
                : null;
        }
        showInformationMessage(displayObject, tile, icon, template);
    }

    /**
     * Shows a message with some information and an "OK"-button.
     *
     * @param displayObject Optional object for displaying.
     * @param tile The Tile the object is at.
     * @param icon The icon to display for the object.
     * @param template The <code>StringTemplate</code> to display.
     */
    void showInformationMessage(FreeColObject displayObject,
                                       Tile tile, ImageIcon icon,
                                       StringTemplate template) {
        String text = Messages.message(template);
        showFreeColPanel(new InformationPanel(freeColClient, text, 
                                              displayObject, icon),
                         tile, true);
    }

    /**
     * Displays a dialog where the user may choose a file.
     *
     * @param directory The directory containing the files.
     * @param filters The file filters which the user can select in the dialog.
     * @return The selected <code>File</code>.
     */
    File showLoadDialog(File directory, FileFilter[] filters) {
        if (filters == null) filters = getFileFilters();
        File response = null;
        for (;;) {
            response = showFreeColDialog(new LoadDialog(freeColClient, frame,
                                                        directory, filters),
                                         null);
            if (response == null || response.isFile()) break;
            showErrorMessage("error.noSuchFile");
        }
        return response;
    }

    /**
     * Displays a dialog for setting options when loading a savegame.  The
     * settings can be retrieved directly from {@link LoadingSavegameDialog}
     * after calling this method.
     *
     * @param publicServer Default value.
     * @param singlePlayer Default value.
     * @return <code>true</code> if the "ok"-button was pressed and
     *         <code>false</code> otherwise.
     */
    boolean showLoadingSavegameDialog(boolean publicServer,
                                             boolean singlePlayer) {
        loadingSavegameDialog = new LoadingSavegameDialog(freeColClient, frame);
        return showFreeColDialog(loadingSavegameDialog, null);
    }

    /**
     * Show a panel containing the log file.
     */
    void showLogFilePanel() {
        showSubPanel(new ErrorPanel(freeColClient), true);

    }

    /**
     * Shows the <code>MainPanel</code>.
     *
     * @param userMsg An option message key to show.
     * @see MainPanel
     */
    void showMainPanel(String userMsg) {
        closeMenus();
        frame.removeMenuBar();
        mainPanel = new MainPanel(freeColClient);
        addCentered(mainPanel, JLayeredPane.DEFAULT_LAYER);
        if (userMsg != null) gui.showInformationMessage(userMsg);
        mainPanel.requestFocus();
    }

    /**
     * Display the map editor transform panel.
     */
    void showMapEditorTransformPanel() {
        JInternalFrame f = addAsFrame(new MapEditorTransformPanel(freeColClient),
            true, PopupPosition.CENTERED, false);
        f.setLocation(f.getX(), 50);
        repaint();
    }

    /**
     * Display the map generator options dialog.
     *
     * @param editable Should these options be editable.
     * @return The <code>OptionGroup</code> as edited.
     */
    OptionGroup showMapGeneratorOptionsDialog(boolean editable) {
        MapGeneratorOptionsDialog mgod
            = new MapGeneratorOptionsDialog(freeColClient, frame, editable);
        return showFreeColDialog(mgod, null);
    }

    /**
     * Display the map size dialog.
     * 
     * @return The response returned by the dialog.
     */
    Dimension showMapSizeDialog() {
        return showFreeColDialog(new MapSizeDialog(freeColClient, frame), null);
    }

    /**
     * Displays a number of ModelMessages.
     *
     * @param messages The <code>ModelMessage</code>s to display.
     */
    void showModelMessages(List<ModelMessage> messages) {
        if (messages.isEmpty()) return;
        final Game game = freeColClient.getGame();
        int n = messages.size();
        String[] texts = new String[n];
        FreeColObject[] fcos = new FreeColObject[n];
        ImageIcon[] icons = new ImageIcon[n];
        Tile tile = null;
        for (int i = 0; i < n; i++) {
            ModelMessage m = messages.get(i);
            texts[i] = Messages.message(m);
            fcos[i] = game.getMessageSource(m);
            icons[i] = createObjectImageIcon(game.getMessageDisplay(m));
            if (tile == null && fcos[i] instanceof Location) {
                tile = ((Location)fcos[i]).getTile();
            }
        }

        showFreeColPanel(new InformationPanel(freeColClient, texts, fcos,
                                              icons),
                         tile, true);
    }

    /**
     * Display the monarch dialog.
     *
     * @param action The <code>MonarchAction</code> underway.
     * @param template The <code>StringTemplate</code> describing the
     *     situation.
     * @param monarchKey The resource key for the monarch image.
     * @param handler A <code>DialogHandler</code> for the dialog response.
     */
    void showMonarchDialog(MonarchAction action,
                           StringTemplate template, String monarchKey,
                           DialogHandler<Boolean> handler) {
        SwingUtilities.invokeLater(
            new DialogCallback<>(
                new MonarchDialog(freeColClient, frame, action, template, monarchKey),
                null, handler));
    }

    /**
     * Display a dialog to set a new name for something.
     *
     * @param template A <code>StringTemplate</code> for the message
     *     to explain the dialog.
     * @param defaultName The default name.
     * @param unit The <code>Unit</code> discovering it.
     * @param handler A <code>DialogHandler</code> for the dialog response.
     */
    void showNamingDialog(StringTemplate template, String defaultName,
                          Unit unit, DialogHandler<String> handler) {
        SwingUtilities.invokeLater(
            new DialogCallback<>(
                new FreeColStringInputDialog(freeColClient, frame, false,
                                             Messages.message(template),
                                             defaultName, "ok", null),
                unit.getTile(), handler));
    }

    /**
     * Displays the <code>NegotiationDialog</code>.
     *
     * @param our Our <code>FreeColGameObject</code> that is negotiating.
     * @param other The other <code>FreeColGameObject</code>.
     * @param agreement The current <code>DiplomaticTrade</code> agreement.
     * @param comment An optional <code>StringTemplate</code> containing a
     *     commentary message.
     * @return An updated agreement.
     */
    DiplomaticTrade showNegotiationDialog(FreeColGameObject our,
                                          FreeColGameObject other,
                                          DiplomaticTrade agreement,
                                          StringTemplate comment) {
        if ((!(our instanceof Unit) && !(our instanceof Colony))
            || (!(other instanceof Unit) && !(other instanceof Colony))
            || (our instanceof Colony && other instanceof Colony)) {
            throw new RuntimeException("Bad DTD args: " + our + ", " + other);
        }
        NegotiationDialog dtd = new NegotiationDialog(freeColClient, frame,
            our, other, agreement, comment);
        return showFreeColDialog(dtd, ((Location)our).getTile());
    }

    /**
     * Display the NewPanel for a given optional specification.
     *
     * @param specification The <code>Specification</code> to use.
     */
    void showNewPanel(Specification specification) {
        showSubPanel(new NewPanel(freeColClient, specification), false);
    }

    /**
     * Shows the given video Component.
     *
     * @param vp The video Component.
     * @param ml A MouseListener for stopping the video.
     * @param kl A KeyListener for stopping the video.
     */
    void showVideoComponent(final Component vp,
                                   final MouseListener ml,
                                   final KeyListener kl) {
        addMouseListener(ml);
        addKeyListener(kl);
        addCentered(vp, JLayeredPane.PALETTE_LAYER);
    }

    /**
     * Display the parameters dialog.
     * 
     * @return The response returned by the dialog.
     */
    Parameters showParametersDialog() {
        return showFreeColDialog(new ParametersDialog(freeColClient, frame),
                                 null);
    }

    /**
     * Display a dialog to confirm a combat.
     *
     * @param attacker The attacker <code>Unit</code>.
     * @param defender The defender.
     * @param tile A <code>Tile</code> to make visible.
     * @return True if the combat is to proceed.
     */
    boolean showPreCombatDialog(Unit attacker,
                                       FreeColGameObject defender,
                                       Tile tile) {
        return showFreeColDialog(new PreCombatDialog(freeColClient, frame,
                                                     attacker, defender),
                                 tile);
    }

    /**
     * Displays the purchase panel.
     */
    void showPurchasePanel() {
        PurchasePanel panel = getExistingFreeColPanel(PurchasePanel.class);
        if (panel == null) {
            showFreeColPanel(new PurchasePanel(freeColClient), null, false);
        }
    }

    /**
     * Displays the recruit panel.
     */
    void showRecruitPanel() {
        RecruitPanel panel = getExistingFreeColPanel(RecruitPanel.class);
        if (panel == null) {
            showFreeColPanel(new RecruitPanel(freeColClient), null, false);
        }
    }

    /**
     * Display the labour detail panel.
     *
     * @param unitType The <code>UnitType</code> to display.
     * @param data The labour data.
     * @param unitCount A map of unit distribution.
     * @param colonies The list of player <code>Colony</code>s.
     */
    void showReportLabourDetailPanel(UnitType unitType,
        Map<UnitType, Map<Location, Integer>> data,
        TypeCountMap<UnitType> unitCount, List<Colony> colonies) {
        ReportLabourDetailPanel details
            = new ReportLabourDetailPanel(freeColClient, unitType, data,
                                          unitCount, colonies);
        details.initialize();
        showSubPanel(details, true);
    }

    /**
     * Display the river style dialog.
     *
     * @param tile An optional tile to make visible (not under the dialog).
     * @return The response returned by the dialog.
     */
    String showRiverStyleDialog(Tile tile) {
        return showFreeColDialog(new RiverStyleDialog(freeColClient, frame), tile);
    }

    /**
     * Displays a dialog where the user may choose a filename.
     *
     * @param directory The directory containing the files in which
     *     the user may overwrite.
     * @param filters The available file filters in the dialog.
     * @param defaultName Default filename for the savegame.
     * @return The selected <code>File</code>.
     */
    public File showSaveDialog(File directory, FileFilter[] filters,
                               String defaultName) {
        if (filters == null) filters = getFileFilters();
        return showFreeColDialog(new SaveDialog(freeColClient, frame, directory,
                                                filters, defaultName),
                                 null);
    }

    /**
     * Display the scale map size dialog.
     * 
     * @return The response returned by the dialog.
     */
    Dimension showScaleMapSizeDialog() {
        return showFreeColDialog(new ScaleMapSizeDialog(freeColClient, frame),
                                 null);
    }

    /**
     * Display the select-amount dialog.
     *
     * @param goodsType The <code>GoodsType</code> to select an amount of.
     * @param available The amount of goods available.
     * @param defaultAmount The amount to select to start with.
     * @param needToPay If true, check the player has sufficient funds.
     * @return The amount selected.
     */
    int showSelectAmountDialog(GoodsType goodsType, int available,
                                      int defaultAmount, boolean needToPay) {
        FreeColDialog<Integer> fcd
            = new SelectAmountDialog(freeColClient, frame, goodsType, available,
                                     defaultAmount, needToPay);
        Integer result = showFreeColDialog(fcd, null);
        return (result == null) ? -1 : result;
    }

    /**
     * display the select-tribute-amount dialog.
     *
     * @param question a <code>stringtemplate</code> describing the
     *     amount of tribute to demand.
     * @param maximum The maximum amount available.
     * @return The amount selected.
     */
    int showSelectTributeAmountDialog(StringTemplate question,
                                             int maximum) {
        FreeColDialog<Integer> fcd
            = new SelectTributeAmountDialog(freeColClient, frame, question, maximum);
        Integer result = showFreeColDialog(fcd, null);
        return (result == null) ? -1 : result;
    }

    /**
     * Display a dialog allowing the user to select a destination for
     * a given unit.
     *
     * @param unit The <code>Unit</code> to select a destination for.
     * @return A destination for the unit, or null.
     */
    Location showSelectDestinationDialog(Unit unit) {
        return showFreeColDialog(new SelectDestinationDialog(freeColClient, frame,
                unit), unit.getTile());
    }

    /**
     * Displays the <code>ServerListPanel</code>.
     *
     * @param serverList The list containing the servers retrieved from the
     *            metaserver.
     * @see ServerListPanel
     */
    void showServerListPanel(List<ServerInfo> serverList) {
        closeMenus();

        serverListPanel.initialize(serverList);
        showSubPanel(serverListPanel, true);
    }

    /**
     * Displays the colony panel of the given <code>Colony</code>
     * following a spying action.
     *
     * @param tile The <code>Tile</code> containing the colony to display.
     * @return The colony panel.
     * @see ColonyPanel
     */
    ColonyPanel showSpyColonyPanel(Tile tile) {
        Colony colony = tile.getColony();
        if (colony == null) return null;
        ColonyPanel panel = new ColonyPanel(freeColClient, colony);
        showFreeColPanel(panel, tile, true);
        return panel;
    }

    /**
     * Displays the <code>StartGamePanel</code>.
     *
     * @param game The <code>Game</code> that is about to start.
     * @param player The <code>Player</code> using this client.
     * @param singlePlayerMode True to start a single player game.
     * @see StartGamePanel
     */
    void showStartGamePanel(Game game, Player player,
                                   boolean singlePlayerMode) {
        if (game == null) {
            logger.warning("StartGamePanel requires game != null.");
        } else if (player == null) {
            logger.warning("StartGamePanel requires player != null.");
        } else {
            closeMenus();
            startGamePanel.initialize(singlePlayerMode);
            showSubPanel(startGamePanel, false);
        }
    }

    /**
     * Display the statistics panel.
     */
    void showStatisticsPanel() {
        showSubPanel(new StatisticsPanel(freeColClient), true);
    }

    /**
     * Shows a status message that cannot be dismissed.  The panel
     * will be removed when another component is added to this
     * <code>Canvas</code>.  This includes all the
     * <code>showXXX</code>-methods. In addition,
     * {@link #closeStatusPanel()} also removes this panel.
     *
     * @param message The text message to display on the status panel.
     * @see StatusPanel
     */
    void showStatusPanel(String message) {
        statusPanel.setStatusMessage(message);
        addCentered(statusPanel, JLayeredPane.POPUP_LAYER);
    }

    /**
     * Display the tile panel for a given tile.
     *
     * @param tile The <code>Tile</code> to display.
     */
    void showTilePanel(Tile tile) {
        if (tile == null || !tile.isExplored()) return;
        showSubPanel(new TilePanel(freeColClient, tile), false);
    }

    /**
     * Shows a tile popup.
     *
     * @param tile The Tile where the popup occurred.
     * @param x The x-coordinate on the screen where the popup needs to be
     *            placed.
     * @param y The y-coordinate on the screen where the popup needs to be
     *            placed.
     * @see TilePopup
     */
    void showTilePopup(Tile tile, int x, int y) {
        if (tile == null)
            return;
        TilePopup tp = new TilePopup(freeColClient, this, tile);
        if (tp.hasItem()) {
            tp.show(this, x, y);
            tp.repaint();
        } else if (tile.isExplored()) {
            showTilePanel(tile);
        }
    }

    /**
     * Display a panel to select a trade route for a unit.
     *
     * @param unit An optional <code>Unit</code> to select a trade route for.
     */
    void showTradeRoutePanel(Unit unit) {
        showFreeColPanel(new TradeRoutePanel(freeColClient, unit),
                         (unit == null) ? null : unit.getTile(), true);
    }

    /**
     * Display the trade route input panel for a given trade route.
     *
     * @param newRoute The <code>TradeRoute</code> to display.
     * @param callBack The <code>Runnable</code> that is run when the
     *     panel closes.
     */
    void showTradeRouteInputPanel(TradeRoute newRoute,
                                         Runnable callBack) {
        FreeColPanel panel = new TradeRouteInputPanel(freeColClient, newRoute);
        panel.addClosingCallback(callBack);
        showSubPanel(panel, null, true);
    }

    /**
     * Displays the training panel.
     */
    void showTrainPanel() {
        TrainPanel panel = getExistingFreeColPanel(TrainPanel.class);
        if (panel == null) {
            showFreeColPanel(new TrainPanel(freeColClient), null, false);
        }
    }

    /**
     * Display the victory dialog.
     *
     * @param handler A <code>DialogHandler</code> for the dialog response.
     */
    void showVictoryDialog(DialogHandler<Boolean> handler) {
        SwingUtilities.invokeLater(
            new DialogCallback<>(new VictoryDialog(freeColClient, frame),
                                        null, handler));
    }

    /**
     * Display the warehouse dialog for a colony.
     *
     * Run out of ColonyPanel, so the tile is already displayed.
     *
     * @param colony The <code>Colony</code> to display.
     * @return The response returned by the dialog.
     */
    boolean showWarehouseDialog(Colony colony) {
        return showFreeColDialog(new WarehouseDialog(freeColClient, frame, colony),
                                 null);
    }

    /**
     * Display the production of a unit.
     *
     * @param unit The <code>Unit</code> to display.
     */
    void showWorkProductionPanel(Unit unit) {
        showSubPanel(new WorkProductionPanel(freeColClient, unit), true);
    }

    /**
     * Update all panels derived from the EuropePanel.
     */
    void updateEuropeanSubpanels() {
        RecruitPanel rp
            = getExistingFreeColPanel(RecruitPanel.class);
        if (rp != null) rp.update();
        PurchasePanel pp
            = getExistingFreeColPanel(PurchasePanel.class);
        if (pp != null) pp.update();
        TrainPanel tp
            = getExistingFreeColPanel(TrainPanel.class);
        if (tp != null) tp.update();
    }

    // Singleton specialist reports

    void showReportCargoPanel() {
        ReportCargoPanel r
            = getExistingFreeColPanel(ReportCargoPanel.class);
        if (r == null) {
            showSubPanel(new ReportCargoPanel(freeColClient), true);
        }
    }

    void showReportColonyPanel() {
        boolean compact;
        try {
            compact = freeColClient.getClientOptions()
                .getInteger(ClientOptions.COLONY_REPORT)
                == ClientOptions.COLONY_REPORT_COMPACT;
        } catch (Exception e) {
            compact = false;
        }
        ReportPanel r = (compact)
            ? getExistingFreeColPanel(ReportCompactColonyPanel.class)
            : getExistingFreeColPanel(ReportClassicColonyPanel.class);
        if (r == null) {
            showSubPanel((compact)
                ? new ReportCompactColonyPanel(freeColClient)
                : new ReportClassicColonyPanel(freeColClient),
                true);
        }
    }

    void showReportContinentalCongressPanel() {
        ReportContinentalCongressPanel
            r = getExistingFreeColPanel(ReportContinentalCongressPanel.class);
        if (r == null) {
            showSubPanel(new ReportContinentalCongressPanel(freeColClient),
                         true);
        }
    }

    void showReportEducationPanel() {
        ReportEducationPanel r
            = getExistingFreeColPanel(ReportEducationPanel.class);
        if (r == null) {
            showSubPanel(new ReportEducationPanel(freeColClient), true);
        }
    }

    void showReportExplorationPanel() {
        ReportExplorationPanel r
            = getExistingFreeColPanel(ReportExplorationPanel.class);
        if (r == null) {
            showSubPanel(new ReportExplorationPanel(freeColClient), true);
        }
    }

    void showReportForeignAffairPanel() {
        ReportForeignAffairPanel r
            = getExistingFreeColPanel(ReportForeignAffairPanel.class);
        if (r == null) {
            showSubPanel(new ReportForeignAffairPanel(freeColClient), true);
        }
    }

    void showReportHistoryPanel() {
        ReportHistoryPanel r
            = getExistingFreeColPanel(ReportHistoryPanel.class);
        if (r == null) {
            showSubPanel(new ReportHistoryPanel(freeColClient), true);
        }
    }

    void showReportIndianPanel() {
        ReportIndianPanel r
            = getExistingFreeColPanel(ReportIndianPanel.class);
        if (r == null) {
            showSubPanel(new ReportIndianPanel(freeColClient), true);
        }
    }

    void showReportLabourPanel() {
        ReportLabourPanel r
            = getExistingFreeColPanel(ReportLabourPanel.class);
        if (r == null) {
            showSubPanel(new ReportLabourPanel(freeColClient), true);
        }
    }

    void showReportMilitaryPanel() {
        ReportMilitaryPanel r
            = getExistingFreeColPanel(ReportMilitaryPanel.class);
        if (r == null) {
            showSubPanel(new ReportMilitaryPanel(freeColClient), true);
        }
    }

    void showReportNavalPanel() {
        ReportNavalPanel r
            = getExistingFreeColPanel(ReportNavalPanel.class);
        if (r == null) {
            showSubPanel(new ReportNavalPanel(freeColClient), true);
        }
    }

    void showReportProductionPanel() {
        ReportProductionPanel r
            = getExistingFreeColPanel(ReportProductionPanel.class);
        if (r == null) {
            showSubPanel(new ReportProductionPanel(freeColClient), true);
        }
    }

    void showReportReligiousPanel() {
        ReportReligiousPanel r
            = getExistingFreeColPanel(ReportReligiousPanel.class);
        if (r == null) {
            showSubPanel(new ReportReligiousPanel(freeColClient), true);
        }
    }

    void showReportRequirementsPanel() {
        ReportRequirementsPanel r
            = getExistingFreeColPanel(ReportRequirementsPanel.class);
        if (r == null) {
            showSubPanel(new ReportRequirementsPanel(freeColClient), true);
        }
    }

    void showReportTradePanel() {
        ReportTradePanel r
            = getExistingFreeColPanel(ReportTradePanel.class);
        if (r == null) {
            showSubPanel(new ReportTradePanel(freeColClient), true);
        }
    }

    /**
     * Show the turn report.
     *
     * @param messages The <code>ModelMessage</code>s to show.
     */
    void showReportTurnPanel(List<ModelMessage> messages) {
        ReportTurnPanel r = getExistingFreeColPanel(ReportTurnPanel.class);
        if (r == null) {
            showSubPanel(new ReportTurnPanel(freeColClient, messages), true);
        } else {
            r.setMessages(messages);
        }
    }
}
