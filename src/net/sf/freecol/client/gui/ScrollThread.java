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

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.model.Direction;


/**
 * Scrolls the view of the Map by moving its focus.
 */
public class ScrollThread extends Thread {

    private static final Logger logger = Logger.getLogger(ScrollThread.class.getName());

    /** Delay between scroll steps. */
    private static final int SCROLL_DELAY = 100; // ms

    /** The Canvas containing the map to scroll. */
    private final Canvas canvas;

    /** The direction to scroll in. */
    private Direction direction = null;


    /**
     * The constructor to use.
     * 
     * @param canvas The Canvas containing the map to scroll.
     */
    public ScrollThread(Canvas canvas) {
        super(FreeCol.CLIENT_THREAD + "Mouse scroller");
        this.canvas = canvas;
    }

    /**
     * Sets the direction in which this ScrollThread will scroll.
     * 
     * @param d The <code>Direction</code> in which this ScrollThread
     *     will scroll.
     */
    public void setDirection(Direction d) {
        direction = d;
    }

    /**
     * Performs the actual scrolling.
     * Run until interrupted or scrolling fails.
     */
    @Override
    public void run() {
        while (direction != null) {
            try {
                SwingUtilities.invokeAndWait(() -> {
                        if (!canvas.scrollMap(direction)) direction = null;
                    });
            } catch (InvocationTargetException e) {
                logger.log(Level.WARNING, "Scroll thread caught error", e);
                break;
            } catch (InterruptedException e) {
                break; // It is normal for AbstractCanvasListener to interrupt.
            }

            try {
                sleep(SCROLL_DELAY);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}
