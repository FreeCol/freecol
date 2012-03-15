package net.sf.freecol.client.gui;

import java.awt.event.MouseEvent;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.model.Map.Direction;

public class AbstractCanvasListener {

    protected final MapViewer mapViewer;
    protected ScrollThread scrollThread;
    protected FreeColClient freeColClient;
    private static final int DRAG_SCROLLSPACE = 100;
    private static final int AUTO_SCROLLSPACE = 1;

    public AbstractCanvasListener(FreeColClient freeColClient, MapViewer mapViewer) {
        this.freeColClient = freeColClient;
        this.mapViewer = mapViewer;
        this.scrollThread = null;
    }

    protected void autoScroll(int x, int y) {
    	scroll(x, y, AUTO_SCROLLSPACE);
    }

    protected void dragScroll(int x, int y) {
    	scroll(x, y, DRAG_SCROLLSPACE);
    }

    protected void performAutoScrollIfActive(MouseEvent e) {
        if (e.getComponent().isEnabled()
          && freeColClient.getClientOptions()
          .getBoolean(ClientOptions.AUTO_SCROLL)) {
    			autoScroll(e.getX(), e.getY());
        } else
            stopScrollIfScrollIsActive();
    }

    protected void performDragScrollIfActive(MouseEvent e) {
        if (e.getComponent().isEnabled()
            && freeColClient.getClientOptions()
            .getBoolean(ClientOptions.MAP_SCROLL_ON_DRAG)) {
    			dragScroll(e.getX(), e.getY());
        } else
            stopScrollIfScrollIsActive();
    }

    protected void stopScrollIfScrollIsActive() {
        if (scrollThread != null) {
            scrollThread.stopScrolling();
            scrollThread = null;
        }
    }

    private void scroll(int x, int y, int scrollspace) {

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
            stopScrollIfScrollIsActive();
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

}