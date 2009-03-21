/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Tile;

/**
 * Listens to the mouse being moved at the level of the Canvas.
 */
public final class CanvasMapEditorMouseMotionListener implements MouseMotionListener {

    private static final Logger logger = Logger.getLogger(CanvasMapEditorMouseMotionListener.class.getName());

    private final Canvas canvas;

    private final GUI gui;

    private ScrollThread scrollThread;

    private static final int DRAG_SCROLLSPACE = 100;

	private static final int AUTO_SCROLLSPACE = 1;

    /**
     * The constructor to use.
     * 
     * @param canvas The component this object gets created for.
     * @param g The GUI that holds information such as screen resolution.
     */
    public CanvasMapEditorMouseMotionListener(Canvas canvas, GUI g) {
        this.canvas = canvas;
        gui = g;
        scrollThread = null;
    }
    
    
    /**
     * This method can be called to make sure the map is loaded
     * There is no point executing mouse events if the map is not loaded
     */
    private Map getMap() {
        Map map = null;
        if (canvas.getClient().getGame() != null)
            map = canvas.getClient().getGame().getMap();
        return map;
    }

    /**
     * Invoked when the mouse has been moved.
     * 
     * @param e The MouseEvent that holds all the information.
     */
    public void mouseMoved(MouseEvent e) {
        if (getMap() == null) {
            return;
        }
		
		if (e.getComponent().isEnabled() && 
   			canvas.getClient().getClientOptions().getBoolean(ClientOptions.AUTO_SCROLL)) {
				auto_scroll(e.getX(), e.getY());
        } else if (scrollThread != null) {
            scrollThread.stopScrolling();
            scrollThread = null;
        }
    }
	
	/**
     * Invoked when the mouse has been dragged.
     * 
     * @param e The MouseEvent that holds all the information.
     */
    public void mouseDragged(MouseEvent e) {
        if (getMap() == null) {
            return;
        }
		
        Map.Position p = gui.convertToMapCoordinates(e.getX(), e.getY());

        if (e.getComponent().isEnabled() &&
			 canvas.getClient().getClientOptions().getBoolean(ClientOptions.MAP_SCROLL_ON_DRAG)) {
				drag_scroll(e.getX(), e.getY());
        } else if (scrollThread != null) {
            scrollThread.stopScrolling();
            scrollThread = null;
        }

        if (p == null || !getMap().isValid(p)) {
            return;
        }
        
        if ((e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) == 0) {
            return;
        }

        Tile tile = getMap().getTile(p);
        if (tile != null) {
            canvas.getClient().getMapEditorController().transform(tile);
            canvas.refresh();
        }
    }
	
	private void auto_scroll(int x, int y){
		scroll(x, y, AUTO_SCROLLSPACE);
	}
	
	private void drag_scroll(int x, int y){
		scroll(x, y, DRAG_SCROLLSPACE);
	}

    private void scroll(int x, int y, int scrollspace) {
        if (getMap() == null) {
            return;
        }
		
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
        } else if ((x >= gui.getWidth() - scrollspace) && (y < scrollspace)) {
            // Upper-Right
            direction = Direction.NE;
        } else if ((x >= gui.getWidth() - scrollspace) && (y >= gui.getHeight() - scrollspace)) {
            // Bottom-Right
            direction = Direction.SE;
        } else if ((x < scrollspace) && (y >= gui.getHeight() - scrollspace)) {
            // Bottom-Left
            direction = Direction.SW;
        } else if (y < scrollspace) {
            // Top
            direction = Direction.N;
        } else if (x >= gui.getWidth() - scrollspace) {
            // Right
            direction = Direction.E;
        } else if (y >= gui.getHeight() - scrollspace) {
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
            scrollThread = new ScrollThread(getMap(), gui);
            scrollThread.setDirection(direction);
            scrollThread.start();
        }
    }


    /**
     * Scrolls the view of the Map by moving its focus.
     */
    private class ScrollThread extends Thread {

        private final Map map;

        private final GUI gui;

        private Direction direction;

        private boolean cont;


        /**
         * The constructor to use.
         * 
         * @param m The Map that needs to be scrolled.
         * @param g The GUI that holds information such as screen resolution.
         */
        public ScrollThread(Map m, GUI g) {
            super(FreeCol.CLIENT_THREAD+"Mouse scroller");
            map = m;
            gui = g;
            cont = true;
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
        public void run() {
            do {
                try {
                    sleep(100);
                } catch (InterruptedException e) {
                }

                try {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        public void run() {
                            try {
                                int x, y;
                                Tile t = map.getTile(gui.getFocus().getX(), gui.getFocus().getY());
                                if (t == null) {
                                    return;
                                }

                                t = map.getNeighbourOrNull(direction, t);
                                if (t == null) {
                                    return;
                                }

                                if (gui.isMapNearTop(t.getY()) && gui.isMapNearTop(gui.getFocus().getY())) {
                                    if (t.getY() > gui.getFocus().getY()) {
                                        y = t.getY();
                                        do {
                                            y += 2;
                                        } while (gui.isMapNearTop(y));
                                    } else {
                                        y = gui.getFocus().getY();
                                    }
                                } else if (gui.isMapNearBottom(t.getY()) && gui.isMapNearBottom(gui.getFocus().getY())) {
                                    if (t.getY() < gui.getFocus().getY()) {
                                        y = t.getY();
                                        do {
                                            y -= 2;
                                        } while (gui.isMapNearBottom(y));
                                    } else {
                                        y = gui.getFocus().getY();
                                    }
                                } else {
                                    y = t.getY();
                                }

                                if (gui.isMapNearLeft(t.getX(), t.getY())
                                        && gui.isMapNearLeft(gui.getFocus().getX(), gui.getFocus().getY())) {
                                    if (t.getX() > gui.getFocus().getX()) {
                                        x = t.getX();
                                        do {
                                            x++;
                                        } while (gui.isMapNearLeft(x, y));
                                    } else {
                                        x = gui.getFocus().getX();
                                    }
                                } else if (gui.isMapNearRight(t.getX(), t.getY())
                                        && gui.isMapNearRight(gui.getFocus().getX(), gui.getFocus().getY())) {
                                    if (t.getX() < gui.getFocus().getX()) {
                                        x = t.getX();
                                        do {
                                            x--;
                                        } while (gui.isMapNearRight(x, y));
                                    } else {
                                        x = gui.getFocus().getX();
                                    }
                                } else {
                                    x = t.getX();
                                }

                                gui.setFocus(x, y);
                            } catch (Exception e) {
                                logger.log(Level.WARNING, "Exception while scrolling!", e);
                            }
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
