/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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
import java.awt.event.MouseMotionListener;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Tile;

/**
 * Listens to the mouse being moved at the level of the Canvas.
 */
public final class CanvasMouseMotionListener implements MouseMotionListener {

    private static final Logger logger = Logger.getLogger(CanvasMouseMotionListener.class.getName());

    // Temporary variable for checking if we need to recalculate the path when
    // dragging units.
    private Tile lastTile;
    
    private final MapViewer mapViewer;

    private ScrollThread scrollThread;

    private FreeColClient freeColClient;

    // private static final int SCROLLSPACE = 3;
    private static final int DRAG_SCROLLSPACE = 100;

	private static final int AUTO_SCROLLSPACE = 1;


    /**
     * The constructor to use.
     *
     * @param canvas The component this object gets created for.
     * @param mapViewer The GUI that holds information such as screen resolution.
     * @param m The Map that is currently being drawn on the Canvas (by the
     *            GUI).
     */
    public CanvasMouseMotionListener(FreeColClient freeColClient, MapViewer mapViewer) {
        this.freeColClient = freeColClient;
        this.mapViewer = mapViewer;
        this.scrollThread = null;
    }

    /**
     * Invoked when the mouse has been moved.
     *
     * @param e The MouseEvent that holds all the information.
     */
    public void mouseMoved(MouseEvent e) {

    	if (e.getComponent().isEnabled()
          && freeColClient.getClientOptions()
          .getBoolean(ClientOptions.AUTO_SCROLL)) {
				auto_scroll(e.getX(), e.getY());
        } else if (scrollThread != null) {
            scrollThread.stopScrolling();
            scrollThread = null;
        }

        if (mapViewer.isGotoStarted()) {
            if (mapViewer.getActiveUnit() == null) {
                mapViewer.stopGoto();
            }

            Tile tile = mapViewer.convertToMapTile(e.getX(), e.getY());

            if (tile != null) {
                if (lastTile != tile) {
                    lastTile = tile;
                    if (mapViewer.getActiveUnit() != null
                        && mapViewer.getActiveUnit().getTile() != tile) {
                        PathNode dragPath = mapViewer.getActiveUnit().findPath(tile);
                        mapViewer.setGotoPath(dragPath);
                    } else {
                        mapViewer.setGotoPath(null);
                    }
                }
            }
        }
    }

	/**
     * Invoked when the mouse has been dragged.
     *
     * @param e The MouseEvent that holds all the information.
     */
    public void mouseDragged(MouseEvent e) {

        if (e.getComponent().isEnabled()
            && freeColClient.getClientOptions()
            .getBoolean(ClientOptions.MAP_SCROLL_ON_DRAG)) {
				drag_scroll(e.getX(), e.getY());
        } else if (scrollThread != null) {
            scrollThread.stopScrolling();
            scrollThread = null;
        }

        Tile tile = mapViewer.convertToMapTile(e.getX(), e.getY());
        if (tile != null &&
            (e.getModifiers() & MouseEvent.BUTTON1_MASK) == MouseEvent.BUTTON1_MASK) {
            // only perform the goto for the left mouse button
            if (mapViewer.isGotoStarted()) {
                if (mapViewer.getActiveUnit() == null) {
                    mapViewer.stopGoto();
                } else {
                    if (lastTile != tile) {
                        lastTile = tile;
                        if (mapViewer.getActiveUnit().getTile() != tile) {
                            PathNode dragPath = mapViewer.getActiveUnit().findPath(tile);
                            mapViewer.setGotoPath(dragPath);
                        } else {
                            mapViewer.setGotoPath(null);
                        }
                    }
                }
            } else {
                mapViewer.startGoto();
            }
        }
    }

	private void auto_scroll(int x, int y){
		scroll(x, y, AUTO_SCROLLSPACE);
	}

	private void drag_scroll(int x, int y){
		scroll(x, y, DRAG_SCROLLSPACE);
	}

	private void scroll(int x, int y, int scrollspace) {

		/*
         * if (y < canvas.getMenuBarHeight()) { if (scrollThread != null) {
         * scrollThread.stopScrolling(); scrollThread = null; } return; } else
         * if (y < canvas.getMenuBarHeight() + SCROLLSPACE) { y -=
         * canvas.getMenuBarHeight(); }
         */

		Direction direction;
        if ((x < scrollspace) && (y < scrollspace)) {
            // Upper-Left
            direction = Direction.NW;
        } else if ((x >= mapViewer.getWidth() - scrollspace) && (y < scrollspace)) {
            // Upper-Right
            direction = Direction.NE;
        } else if ((x >= mapViewer.getWidth() - scrollspace) && (y >= mapViewer.getHeight() - scrollspace)) {
            // Bottom-Right
            direction = Direction.SE;
        } else if ((x < scrollspace) && (y >= mapViewer.getHeight() - scrollspace)) {
            // Bottom-Left
            direction = Direction.SW;
        } else if (y < scrollspace) {
            // Top
            direction = Direction.N;
        } else if (x >= mapViewer.getWidth() - scrollspace) {
            // Right
            direction = Direction.E;
        } else if (y >= mapViewer.getHeight() - scrollspace) {
            // Bottom
            direction = Direction.S;
        } else if (x < scrollspace) {
            // Left
            direction = Direction.W;
        } else {
            // Center
            if (scrollThread != null) {
                scrollThread.stopScrolling();
                scrollThread = null;
            }
            return;
        }

        if (scrollThread != null) {
            // continue scrolling in a (perhaps new) direction
            scrollThread.setDirection(direction);
        } else {
            // start scrolling in a direction
            scrollThread = new ScrollThread(mapViewer);
            scrollThread.setDirection(direction);
            scrollThread.start();
        }
	}

    /**
     * Scrolls the view of the Map by moving its focus.
     */
    private class ScrollThread extends Thread {

         private final MapViewer mapViewer;

        private Direction direction;

        private boolean cont;


        /**
         * The constructor to use.
         *
         * @param m The Map that needs to be scrolled.
         * @param mapViewer The GUI that holds information such as screen resolution.
         */
        public ScrollThread(MapViewer mapViewer) {
            super(FreeCol.CLIENT_THREAD+"Mouse scroller");
            this.mapViewer = mapViewer;
            this.cont = true;
        }

        /**
         * Sets the direction in which this ScrollThread will scroll.
         *
         * @param d The direction in which this ScrollThread will scroll.
         */
        public void setDirection(Direction d) {
            direction = d;
        }

        /**
         * Makes this ScrollThread stop doing what it is supposed to do.
         */
        public void stopScrolling() {
            cont = false;
        }

        /**
         * Performs the actual scrolling.
         */
        @Override
        public void run() {
            do {
                try {
                    sleep(100);
                } catch (InterruptedException e) {
                }

                try {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        public void run() {
                            mapViewer.scrollMap(direction);
                        }
                    });
                } catch (InvocationTargetException e) {
                    logger.log(Level.WARNING, "Scroll thread caught error", e);
                    cont = false;
                } catch (InterruptedException e) {
                    logger.log(Level.WARNING, "Scroll thread interrupted", e);
                    cont = false;
                }
            } while (cont);
        }
    }
}
