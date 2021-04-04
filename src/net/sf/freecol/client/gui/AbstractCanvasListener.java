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

import java.awt.Dimension;
import java.awt.event.MouseEvent;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.FreeColClientHolder;
import net.sf.freecol.common.model.Direction;


/**
 * The outline of a canvas listener.
 */
public class AbstractCanvasListener extends FreeColClientHolder {

    /** Space to auto-scroll. */
    protected static final int AUTO_SCROLL_SPACE = 4;

    /** Space to drag-scroll. */
    private static final int DRAG_SCROLL_SPACE = 100;

    /** The scroll thread itself. */
    protected ScrollThread scrollThread = null;


    /**
     * Create a new AbstractCanvasListener.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    protected AbstractCanvasListener(FreeColClient freeColClient) {
        super(freeColClient);

        this.scrollThread = null;
    }


    /**
     * Auto-scroll to a mouse position if necessary.
     *
     * @param e The {@code MouseEvent} that initiating the scroll.
     * @param ignoreTop If the top should be ignored (to avoid scrolling
     *                  when the mouse is over the top of the map, as the
     *                  menu bar above is checked already)
     */
    protected void performAutoScrollIfActive(MouseEvent e, boolean ignoreTop) {
        if (e.getComponent().isEnabled()
            && getClientOptions().getBoolean(ClientOptions.AUTO_SCROLL)) {
            scroll(e.getX(), e.getY(), AUTO_SCROLL_SPACE, ignoreTop);
        } else {
            stopScrollIfScrollIsActive();
        }
    }

    /**
     * Drag-scroll to a mouse position if necessary.
     *
     * @param e The {@code MouseEvent} that initiating the scroll.
     */
    protected void performDragScrollIfActive(MouseEvent e) {
        if (e.getComponent().isEnabled()
            && getClientOptions().getBoolean(ClientOptions.MAP_SCROLL_ON_DRAG)) {
            scroll(e.getX(), e.getY(), DRAG_SCROLL_SPACE, false);
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
     * @param ignoreTop If the top should be ignored
     */
    private void scroll(int x, int y, int scrollSpace, boolean ignoreTop) {
        Direction direction = getGUI()
            .getScrollDirection(x, y, scrollSpace, ignoreTop);

        if (direction == null) {
            stopScrollIfScrollIsActive();
        } else if (scrollThread == null || scrollThread.isInterrupted()) {
            scrollThread = new ScrollThread(getFreeColClient());
            scrollThread.setDirection(direction);
            scrollThread.start();
        } else {
            scrollThread.setDirection(direction);
        }
    }
}
