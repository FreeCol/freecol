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

import java.awt.event.MouseEvent;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.model.Map.Direction;


/**
 * The outline of a canvas listener.
 */
public class AbstractCanvasListener {

    private static final int DRAG_SCROLLSPACE = 100;
    protected static final int AUTO_SCROLLSPACE = 1;

    /** The enclosing client. */
    protected FreeColClient freeColClient;

    /** The map viewer to scroll. */
    protected final MapViewer mapViewer;

    /** The scroll thread itself. */
    protected ScrollThread scrollThread = null;


    /**
     * Create a new AbstractCanvasListener.
     *
     * @param freeColClient The <code>FreeColClient</code> to use.
     */
    public AbstractCanvasListener(FreeColClient freeColClient) {
        this.freeColClient = freeColClient;
        this.mapViewer = freeColClient.getGUI().getMapViewer();
        this.scrollThread = null;
    }


    /**
     * Auto-scroll from a given position.
     *
     * @param x The x coordinate.
     * @param y The y coordinate.
     */
    private void autoScroll(int x, int y) {
        scroll(x, y, AUTO_SCROLLSPACE);
    }

    /**
     * Drag-scroll from a given position.
     *
     * @param x The x coordinate.
     * @param y The y coordinate.
     */
    private void dragScroll(int x, int y) {
        scroll(x, y, DRAG_SCROLLSPACE);
    }

    /**
     * Auto-scroll to a mouse position if necessary.
     *
     * @param e The <code>MouseEvent</code> that initiating the scroll.
     */
    protected void performAutoScrollIfActive(MouseEvent e) {
        if (e.getComponent().isEnabled()
            && freeColClient.getClientOptions().getBoolean(ClientOptions.AUTO_SCROLL)) {
            autoScroll(e.getX(), e.getY());
        } else {
            stopScrollIfScrollIsActive();
        }
    }

    /**
     * Drag-scroll to a mouse position if necessary.
     *
     * @param e The <code>MouseEvent</code> that initiating the scroll.
     */
    protected void performDragScrollIfActive(MouseEvent e) {
        if (e.getComponent().isEnabled()
            && freeColClient.getClientOptions().getBoolean(ClientOptions.MAP_SCROLL_ON_DRAG)) {
            dragScroll(e.getX(), e.getY());
        } else {
            stopScrollIfScrollIsActive();
        }
    }

    /**
     * Stop scrolling.
     */
    protected void stopScrollIfScrollIsActive() {
        if (scrollThread != null) {
            scrollThread.interrupt();
            scrollThread = null;
        }
    }

    /**
     * Scroll the map if the given (x,y) coordinate is close to an edge.
     *
     * @param x The x coordinate.
     * @param y The y coordinate.
     * @param scrollSpace The clearance from the relevant edge
     */
    private void scroll(int x, int y, int scrollSpace) {
        Direction direction;
        if (x < scrollSpace && y < scrollSpace) { // Upper-Left
            direction = Direction.NW;
        } else if (x >= mapViewer.getWidth() - scrollSpace
            && y < scrollSpace) { // Upper-Right
            direction = Direction.NE;
        } else if (x >= mapViewer.getWidth() - scrollSpace
            && y >= mapViewer.getHeight() - scrollSpace) { // Bottom-Right
            direction = Direction.SE;
        } else if (x < scrollSpace
            && y >= mapViewer.getHeight() - scrollSpace) { // Bottom-Left
            direction = Direction.SW;
        } else if (y < scrollSpace) { // Top
            direction = Direction.N;
        } else if (x >= mapViewer.getWidth() - scrollSpace) { // Right
            direction = Direction.E;
        } else if (y >= mapViewer.getHeight() - scrollSpace) { // Bottom
            direction = Direction.S;
        } else if (x < scrollSpace) { // Left
            direction = Direction.W;
        } else {
            direction = null;
        }

        if (direction == null) {
            stopScrollIfScrollIsActive();
        } else if (scrollThread == null || scrollThread.isInterrupted()) {
            scrollThread = new ScrollThread(mapViewer);
            scrollThread.setDirection(direction);
            scrollThread.start();
        } else {
            scrollThread.setDirection(direction);
        }
    }
}
