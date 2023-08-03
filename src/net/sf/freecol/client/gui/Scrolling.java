package net.sf.freecol.client.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseEvent;

import javax.swing.SwingUtilities;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.FreeColClientHolder;
import net.sf.freecol.client.gui.mapviewer.MapAsyncPainter;
import net.sf.freecol.common.model.Direction;

/**
 * Handles scrolling on the main map.
 */
public final class Scrolling extends FreeColClientHolder {

    /** Space to auto-scroll. */
    protected static final int AUTO_SCROLL_SPACE = 6;

    /** Space to drag-scroll. */
    private static final int DRAG_SCROLL_SPACE = 100;
    
    private final Canvas canvas;
    
    /** The scroll thread itself. */
    private volatile ScrollThread scrollThread = null;
    private volatile Object scrollThreadLock = new Object();
    
    
    public Scrolling(FreeColClient freeColClient, Canvas canvas) {
        super(freeColClient);
        this.canvas = canvas;
    }
    
    
    /**
     * Auto-scroll to a mouse position if necessary.
     *
     * @param e The {@code MouseEvent} initiating the scroll.
     */
    public void performAutoScrollIfActive(MouseEvent e) {
        if (e.getComponent().isEnabled() && getClientOptions().getBoolean(ClientOptions.AUTO_SCROLL)) {
            final int autoScrollSpace = getGUI().getFixedImageLibrary().scaleInt(AUTO_SCROLL_SPACE);
            scroll(e, autoScrollSpace);
        } else {
            stopScrollIfScrollIsActive();
        }
    }

    /**
     * Drag-scroll to a mouse position if necessary.
     *
     * @param e The {@code MouseEvent} initiating the scroll.
     */
    public void performDragScrollIfActive(MouseEvent e) {
        if (e.getComponent().isEnabled() && getClientOptions().getBoolean(ClientOptions.MAP_SCROLL_ON_DRAG)) {
            final int dragScrollSpace = getGUI().getFixedImageLibrary().scaleInt(DRAG_SCROLL_SPACE);
            scroll(e, dragScrollSpace);
        } else {
            stopScrollIfScrollIsActive();
        }
    }
    
    
    /**
     * Stop scrolling.
     */
    public void stopScrollIfScrollIsActive() {
        if (scrollThread != null) {
            synchronized (scrollThreadLock) {
                if (scrollThread != null) {
                    scrollThread.abort();
                }
                scrollThread = null;
            }
        }
        getGUI().stopMapAsyncPainter();
    }

    /**
     * Scroll the map if the given (x,y) coordinate is close to an edge.
     *
     * @param e The {@code MouseEvent} initiating the scroll.
     * @param scrollSpace The clearance from the relevant edge
     */
    private void scroll(MouseEvent e, int scrollSpace) {
        final Point panePoint = SwingUtilities.convertPoint((Component) e.getSource(), e.getPoint(), getRootComponent());
        final Direction direction = getScrollDirection(panePoint.x, panePoint.y, scrollSpace);
        
        if (direction == null) {
            stopScrollIfScrollIsActive();
            return;
        }
        
        if (isAsyncPainterEnabled()) {
            final MapAsyncPainter mapAsyncPainter = getGUI().useMapAsyncPainter();
            mapAsyncPainter.setScrollDirection(direction);
            return;
        }
        
        synchronized (scrollThreadLock) {
            if (scrollThread != null && !scrollThread.isAborted()) {
                scrollThread.setDirection(direction);
                return;
            }

            stopScrollIfScrollIsActive();
            scrollThread = new ScrollThread(getFreeColClient());
            scrollThread.setDirection(direction);
            scrollThread.start();
        }
    }
    
    private boolean isAsyncPainterEnabled() {
        return true;
    }
    
    /**
     * Work out what direction to scroll the map if a coordinate is close
     * to an edge.
     *
     * @param x The x coordinate.
     * @param y The y coordinate.
     * @param scrollSpace The clearance from the relevant edge
     * @return The {@code Direction} to scroll, or null if not.
     */
    private Direction getScrollDirection(int x, int y, int scrollSpace) {
        Direction ret;
        final Dimension size = getRootComponent().getSize();
        if (x < scrollSpace && y < scrollSpace) { // Upper-Left
            ret = Direction.NW;
        } else if (x >= size.width - scrollSpace
            && y < scrollSpace) { // Upper-Right
            ret = Direction.NE;
        } else if (x >= size.width - scrollSpace
            && y >= size.height - scrollSpace) { // Bottom-Right
            ret = Direction.SE;
        } else if (x < scrollSpace
            && y >= size.height - scrollSpace) { // Bottom-Left
            ret = Direction.SW;
        } else if (y < scrollSpace) { // Top
            ret = Direction.N;
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
    
    private Component getRootComponent() {
        return canvas.getParentFrame().getRootPane();
    }
}
