package net.sf.freecol.client.gui;

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.model.Map.Direction;

public class ScrollThread extends Thread {

    public static final String COPYRIGHT = "Copyright (C) 2003-2012 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";
    
    private static final Logger logger = Logger.getLogger(ScrollThread.class.getName());

    /**
     * Scrolls the view of the Map by moving its focus.
     */

    private final MapViewer mapViewer;

    private Direction direction;

    private boolean cont;


    /**
     * The constructor to use.
     * 
     * @param mapViewer The GUI that holds information such as screen
     *            resolution.
     */
    public ScrollThread(MapViewer mapViewer) {
        super(FreeCol.CLIENT_THREAD + "Mouse scroller");
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
