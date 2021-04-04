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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.function.Predicate;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JMenuBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicInternalFrameUI;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.action.FreeColAction;
import net.sf.freecol.client.gui.dialog.FreeColDialog;
import net.sf.freecol.client.gui.menu.InGameMenuBar;
import net.sf.freecol.client.gui.menu.MapEditorMenuBar;
import net.sf.freecol.client.gui.menu.MenuMouseMotionListener;
import net.sf.freecol.client.gui.panel.MapControls;
import net.sf.freecol.client.gui.panel.Utility;
import net.sf.freecol.client.gui.SwingGUI.PopupPosition;
import net.sf.freecol.client.gui.video.VideoComponent;
import net.sf.freecol.client.gui.video.VideoListener;

import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.option.IntegerOption;
import net.sf.freecol.common.option.Option;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.common.resources.Video;
import static net.sf.freecol.common.util.CollectionUtils.*;
import static net.sf.freecol.common.util.StringUtils.*;

// Special case panels, TODO: can we move these to Widgets?
import net.sf.freecol.client.gui.panel.FreeColPanel;
import net.sf.freecol.client.gui.panel.MainPanel;
import net.sf.freecol.client.gui.panel.MapEditorTransformPanel;


/**
 * The main container for the other GUI components in FreeCol.
 * This is where lower level graphics coordination occurs.
 * Specific panels and dialogs are over in Widgets
 * (TODO) with a few exceptions.
 */
public final class Canvas extends JDesktopPane {

    private static final Logger logger = Logger.getLogger(Canvas.class.getName());

    /** Number of tries to find a clear spot on the canvas. */
    private static final int MAXTRY = 3;

    /** The cursor to show for goto operations. */
    public static final java.awt.Cursor GO_CURSOR
        = (java.awt.Cursor)UIManager.get("cursor.go");
    
    /** A class for frames being used as tool boxes. */
    private static class ToolBoxFrame extends JInternalFrame {}

    /** The game client. */
    private final FreeColClient freeColClient;

    /** The graphics device to display to. */
    private final GraphicsDevice graphicsDevice;

    /** Is the canvas in windowed mode? */
    private boolean windowed;

    /** The parent frame, either a window or the full screen. */
    private FreeColFrame parentFrame;

    /** Remember the current size (from getSize()), check for changes. */
    private Dimension oldSize;

    /**
     * The component that displays the map.
     * Ideally this would be completely separate, but the displayMap
     * (and possibly changeSize) call/s means we must retain a reference
     * to the map viewer.
     * We also use it for the Cursor, which remains in MapViewer so
     * that displayMap can draw it when active needed.
     */
    private final MapViewer mapViewer;

    /** The various sorts of map controls. */
    private MapControls mapControls;

    /** The special overlay used when it is not the player turn. */
    private GrayLayer greyLayer;

    /** The dialogs in view. */
    private final List<FreeColDialog<?>> dialogs = new ArrayList<>();

    /**
     * The main panel.  Remember this because getExistingFreeColPanel
     * gets confused across switches between the game and the map editor
     * which makes it hard to remove.
     */
    private MainPanel mainPanel;


    /**
     * The constructor to use.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param graphicsDevice The {@code GraphicsDevice} to display on.
     * @param desiredSize The desired size of the parent frame.
     * @param mapViewer The object responsible of drawing the map.
     * @param mapControls The controls on the map.
     */
    public Canvas(final FreeColClient freeColClient,
                  final GraphicsDevice graphicsDevice,
                  final Dimension desiredSize,
                  MapViewer mapViewer,
                  MapControls mapControls) {
        this.freeColClient = freeColClient;
        this.graphicsDevice = graphicsDevice;

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
        this.mapControls = mapControls;
        this.greyLayer = new GrayLayer(freeColClient);

        setDoubleBuffered(true);
        setOpaque(false);
        setLayout(null);
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        // Create key bindings for all actions
        for (Option option : freeColClient.getActionManager().getOptions()) {
            FreeColAction action = (FreeColAction)option;
            getInputMap().put(action.getAccelerator(), action.getId());
            getActionMap().put(action.getId(), action);
        }

        logger.info("Canvas created with bounds: " + windowBounds);
    }

    // Internals

    // Frames and Windows

    /**
     * Toggle windowed flag.
     */
    private void toggleWindowed() {
        this.windowed = !this.windowed;
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
     * Determine whether to use full screen or windowed mode.
     *
     * @param gd The {@code GraphicsDevice} to display to.
     * @param desiredSize An optional window {@code Dimension}.
     * @return True if windowed mode is to be used, false for full screen.
     */
    private static boolean checkWindowed(GraphicsDevice gd,
                                         Dimension desiredSize) {
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
     * @param layer The layer to add the component to (see JLayeredPane).
     */
    private void addCentered(Component comp, Integer layer) {
        comp.setLocation((this.oldSize.width - comp.getWidth()) / 2,
                         (this.oldSize.height - comp.getHeight()) / 2);
        addToLayer(comp, layer);
    }

    /**
     * Adds a component to this Canvas.
     *
     * @param comp The {@code Component} to add to this canvas.
     * @param layer The layer to add the component to (see JLayeredPane).
     */
    private void addToLayer(Component comp, Integer layer) {
        addToCanvas(comp, layer);
        updateMenuBar();
    }

    /**
     * Adds a component to this Canvas updating the menus.
     *
     * @param comp The {@code Component} to add to this canvas.
     * @param layer The layer to add the component to (see JLayeredPane).
     */
    private void addToCanvas(Component comp, Integer layer) {
        try {
        	// To avoid illegal component position exception - remove the component first
            remove(comp);
            add(comp, layer);
        } catch (Exception e) {
            logger.log(Level.WARNING, "addToCanvas("
                + comp.getClass().getSimpleName()
                + " at " + comp.getX() + "," + comp.getY()
                + " on layer " + layer + ") failed.", e);
        }
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
     * Is nothing showing?
     *
     * @return True if no dialog or panel is being shown.
     */
    private boolean nothingShowing() {
        return this.dialogs.isEmpty() && getShowingPanel() == null;
    }


    // Location, choice and persistence

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
     * Gets the saved position of a component.
     *
     * @param comp The {@code Component} to use.
     * @return The saved position as a {@code Point}, or null if no
     *     saved position is found.
     */
    private Point getSavedPosition(Component comp) {
        final ClientOptions co = this.freeColClient.getClientOptions();
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
        final ClientOptions co = this.freeColClient.getClientOptions();
        if (co == null) return null;
        try {
            if (!co.getBoolean(ClientOptions.REMEMBER_PANEL_SIZES)) {
                return null;
            }
        } catch (Exception e) {}
        return co.getPanelSize(comp.getClass().getName());
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
        if (this.freeColClient == null) return;
        final ClientOptions co = this.freeColClient.getClientOptions();
        if (co == null) return;
        final OptionGroup etc = co.getOptionGroup(ClientOptions.ETC);
        if (etc == null) return;
        
        // Insist the option is present
        if (!etc.hasOption(className + key, IntegerOption.class)) {
            Specification specification = (this.freeColClient.getGame() == null)
                ? null : this.freeColClient.getGame().getSpecification();
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
            if (!this.freeColClient.getClientOptions()
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
            if (!this.freeColClient.getClientOptions()
                .getBoolean(ClientOptions.REMEMBER_PANEL_SIZES)) return;
        } catch (Exception e) {}

        String className = comp.getClass().getName();
        saveInteger(className, ".w", size.width);
        saveInteger(className, ".h", size.height);
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

            if (nothingShowing()) updateMenuBar();
        }
    }


    // Public routines follow

    // Initialization and teardown

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
     * Map editor initialization.
     */
    public void startMapEditorGUI() {
        this.parentFrame.setMenuBar(new MapEditorMenuBar(this.freeColClient,
                new MenuMouseMotionListener(this.freeColClient)));
        CanvasMapEditorMouseListener listener
            = new CanvasMapEditorMouseListener(this.freeColClient);
        addMouseListener(listener);
        addMouseMotionListener(listener);
    }

    /**
     * In game initializations.
     */
    public void initializeInGame() {
        this.parentFrame.setMenuBar(new InGameMenuBar(this.freeColClient,
                new MenuMouseMotionListener(this.freeColClient)));
        addMouseListener(new CanvasMouseListener(this.freeColClient));
        addMouseMotionListener(new CanvasMouseMotionListener(this.freeColClient, this));
    }

    /**
     * Quit the canvas.
     */
    public void quit() {
        destroyFrame();
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


    // Animation handling
    
    /**
     * Add and remove animation labels.
     *
     * @param label A {@code JLabel} for an animation.
     * @param add If true, add the label, else remove it.
     */
    public void animationLabel(JLabel label, boolean add) {
        if (add) {
            addToCanvas(label, JLayeredPane.DEFAULT_LAYER);
        } else {
            removeFromCanvas(label);
        }
    }

    // Dialog and panel primitives, several only public for Widgets
    
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
     * Add a dialog to the current dialog list.
     *
     * @param fcd The dialog to add.
     */
    public void dialogAdd(FreeColDialog<?> fcd) {
        if (fcd.isModal()) {
            this.mapViewer.stopCursorBlinking();
        }
        dialogs.add(fcd);
    }

    /**
     * Remove a dialog from the current dialog list.
     *
     * @param fcd The dialog to remove.
     */
    public void dialogRemove(FreeColDialog<?> fcd) {
        dialogs.remove(fcd);
        if (fcd.isModal() && none(dialogs, FreeColDialog::isModal)) {
            this.mapViewer.startCursorBlinking();
        }
        if (nothingShowing()) updateMenuBar();
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
     * Gets any currently displayed component matching a predicate.
     *
     * @param pred The predicate to apply.
     * @return The first match for the predicate, or null if none found.
     */
    public Component getMatchingComponent(Predicate<Component> pred) {
        for (Component c1 : getComponents()) {
            if (c1 instanceof JInternalFrame) {
                for (Component c2 : ((JInternalFrame) c1).getContentPane()
                         .getComponents()) {
                    if (pred.test(c2)) return c2;
                }
            }
        }
        return null;
    }

    /**
     * Get any panel this {@code Canvas} is displaying.
     *
     * @return A {@code Component} the {@code Canvas} is
     *     displaying, or null if none found.
     */
    public Component getShowingPanel() {
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
     * Removes the given component from this canvas without
     * updating the menu bar.
     *
     * @param comp The {@code Component} to remove.
     */
    public void removeFromCanvas(Component comp) {
        if (comp == null) return;

        final Rectangle updateBounds = comp.getBounds();
        final JInternalFrame jif = getInternalFrame(comp);
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
        if (jif != null) { // Notify close after removing from Canvas
            notifyClose(comp, jif);
        }
        repaint(updateBounds.x, updateBounds.y,
                updateBounds.width, updateBounds.height);
    }

    /**
     * Displays the given dialog, optionally making sure a tile is visible.
     *
     * @param <T> The type to be returned from the dialog.
     * @param dialog The {@code FreeColDialog} to be displayed
     * @param pos A {@code PopupPosition} for the dialog.
     * @return The {@link FreeColDialog#getResponse reponse} returned by
     *     the dialog.
     */
    public <T> T showFreeColDialog(FreeColDialog<T> dialog, PopupPosition pos) {
        viewFreeColDialog(dialog, pos);
        T response = dialog.getResponse();
        remove(dialog);
        dialogRemove(dialog);
        return response;
    }

    /**
     * Displays a {@code FreeColPanel} at a generalized position.
     *
     * @param panel {@code FreeColPanel}, panel to show
     * @param popupPosition {@code PopupPosition} The generalized
     *     position to place the panel.
     * @param resizable Should the panel be resizable?
     * @return The panel.
     */
    public FreeColPanel showFreeColPanel(FreeColPanel panel,
                                         PopupPosition popupPosition,
                                         boolean resizable) {
        repaint();
        addAsFrame(panel, false, popupPosition, resizable);
        panel.requestFocus();
        return panel;
    }

    /**
     * Displays the given dialog, optionally making sure a tile is visible.
     *
     * @param <T> The type to be returned from the dialog.
     * @param freeColDialog The dialog to be displayed
     * @param pos A {@code PopupPosition} for the dialog.
     */
    public <T> void viewFreeColDialog(final FreeColDialog<T> freeColDialog,
                                      PopupPosition pos) {
        // TODO: Remove compatibility code when all non-modal dialogs
        //       have been converted into panels.
        if (!freeColDialog.isModal()) {
            int canvasWidth = getWidth();
            int dialogWidth = freeColDialog.getWidth();
            if (dialogWidth*2 <= canvasWidth) {
                Point location = freeColDialog.getLocation();
                if (pos == PopupPosition.CENTERED_LEFT) {
                    freeColDialog.setLocation(location.x - canvasWidth/4,
                                              location.y);
                } else if (pos == PopupPosition.CENTERED_RIGHT) {
                    freeColDialog.setLocation(location.x + canvasWidth/4,
                                              location.y);
                }
            }
        }

        dialogAdd(freeColDialog);
        freeColDialog.requestFocus();
        freeColDialog.setVisible(true);
    }


    // Frames and windowing

    /**
     * Are we in windowed mode?
     *
     * @return True if in windowed mode.
     */
    public boolean isWindowed() {
        return this.windowed;
    }

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


    // Map controls, called out of paintComponent via checkResize

    /**
     * Add the map controls.
     *
     * @return True if any were added.
     */
    public boolean addMapControls() {
        if (this.mapControls == null) return false;
        List<Component> components
            = this.mapControls.getComponentsToAdd(this.oldSize);
        for (Component c : components) {
            addToCanvas(c, JLayeredPane.MODAL_LAYER);
        }
        return !components.isEmpty();
    }

    /**
     * Remove the map controls.
     *
     * @return True if any were removed.
     */
    public boolean removeMapControls() {
        if (this.mapControls == null) return false;
        List<Component> components
            = this.mapControls.getComponentsPresent();
        boolean ret = false;
        for (Component c : this.mapControls.getComponentsPresent()) {
            removeFromCanvas(c);
            ret = true;
        }
        return ret;
    }


    // Menus

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


    // Video

    /**
     * Play the opening video.
     *
     * @param videoId An identifier for the video content.
     * @param muteAudio Mute if true.
     * @param runnable A {@code Runnable} to run on completion.
     */
    public void playVideo(String videoId, boolean muteAudio,
                          final Runnable runnable) {
        final Video video = ImageLibrary.getVideo(videoId);        
        final VideoComponent vc = new VideoComponent(video, muteAudio);

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
            public void actionPerformed(ActionEvent ae) { // from timer
                execute();
            }

            public void startTimer(int tim) {
                this.t = new Timer(tim, this);
                this.t.setRepeats(false);
                this.t.start();
            }

            private void execute() {
                removeKeyListener(this);
                removeMouseListener(this);
                vc.removeMouseListener(this);
                vc.stop();
                remove(vc);
                if (t != null) {
                    t.stop();
                }
                runnable.run();
            }
        }

        closeMenus();
        // Clicks or keyboard on the canvas and the video stop the video
        AbortListener l = new AbortListener();
        addMouseListener(l);
        addKeyListener(l);
        vc.addMouseListener(l);
        vc.addKeyListener(l);
        // The Cortado applet is failing to quit when finished, make
        // sure it eventually gets kicked.
        // Change the magic number if we change the opening video length.
        l.startTimer(80000);

        // Add video and play
        addCentered(vc, JLayeredPane.PALETTE_LAYER);
        vc.play();
    }


    // Special dialogs and panels

    /**
     * Closes the {@link MainPanel}.
     */
    public void closeMainPanel() {
        if (this.mainPanel != null) {
            remove(this.mainPanel);
            this.mainPanel = null;
        }
    }

    /**
     * Closes all panels, changes the background and shows the main menu.
     */
    public void mainTitle() {
        removeInGameComponents();
        showMainPanel();
        repaint();
    }

    /**
     * Shows the {@code MainPanel}.
     *
     * @return The main panel.
     */
    public FreeColPanel showMainPanel() {
        closeMenus();
        this.parentFrame.removeMenuBar();
        this.mainPanel = new MainPanel(this.freeColClient);
        addCentered(this.mainPanel, JLayeredPane.DEFAULT_LAYER);
        this.mainPanel.requestFocus();
        return this.mainPanel;
    }

    /**
     * Display the map editor transform panel.
     */
    public void showMapEditorTransformPanel() {
        MapEditorTransformPanel panel
            = new MapEditorTransformPanel(this.freeColClient);
        JInternalFrame f = addAsFrame(panel, true, PopupPosition.CENTERED,
                                      false);
        f.setLocation(f.getX(), 50); // move up to near the top
        repaint();
    }


    // Override JComponent

    /**
     * {@inheritDoc}
     */
    @Override
    public void paintComponent(Graphics g) {
        Dimension size = getSize();
        if (this.oldSize.width != size.width
            || this.oldSize.height != size.height) {
            logger.info("Canvas resize from " + this.oldSize
                + " to " + size);
            this.oldSize = size;
            boolean add = removeMapControls();
            this.mapViewer.changeSize(size);
            if (add) addMapControls();
        }
        boolean hasMap = this.freeColClient != null
            && this.freeColClient.getGame() != null
            && this.freeColClient.getGame().getMap() != null;
        Graphics2D g2d = (Graphics2D)g;

        if (this.freeColClient.isMapEditor()) {
            if (hasMap) {
                this.mapViewer.displayMap(g2d);
            } else {
                g2d.setColor(Color.BLACK);
                g2d.fillRect(0, 0, size.width, size.height);
            }

        } else if (this.freeColClient.isInGame() && hasMap) {
            // draw the map
            this.mapViewer.displayMap(g2d);

            // toggle grey layer if needed
            if (this.freeColClient.currentPlayerIsMyPlayer()) {
                if (greyLayer.getParent() != null) {
                    removeFromCanvas(greyLayer);
                }
            } else {
                greyLayer.setBounds(0, 0, size.width, size.height);
                greyLayer.setPlayer(this.freeColClient.getGame()
                    .getCurrentPlayer());
                if (greyLayer.getParent() == null) {
                    addToCanvas(greyLayer, JLayeredPane.DRAG_LAYER);
                }
            }

        } else { /* main menu */
            // Get the background without scaling, to avoid wasting
            // memory needlessly keeping an unbounded number of rescaled
            // versions of the largest image in FreeCol, forever.
            final Image bgImage = ImageLibrary.getCanvasBackgroundImage();
            if (bgImage != null) {
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
                g2d.setColor(Color.WHITE);
                g2d.fillRect(0, 0, size.width, size.height);
                logger.warning("Unable to load the canvas background");
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
    }
}
