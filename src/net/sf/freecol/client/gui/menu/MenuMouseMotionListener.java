package net.sf.freecol.client.gui.menu;

import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.AbstractCanvasListener;
import net.sf.freecol.client.gui.MapViewer;

/**
 * This class is meant to make the autoscrolling work better, so that you don't
 * have to hover the mouse exactly one pixel below the menu bar to make it 
 * scroll up. This is the MouseMotionListener added to the menu bar, allowing
 * you to scroll by just moving the mouse all the way to the top of the screen.
 * 
 * Note: This doesn't cause it to scroll down when you reach the bottom of the 
 * menu bar, because the performAutoScrollIfActive will compare the Y 
 * coordinate to the size of the entire canvas (which should always be bigger).
 *
 */
public class MenuMouseMotionListener extends AbstractCanvasListener implements MouseMotionListener {

	public MenuMouseMotionListener(FreeColClient freeColClient, MapViewer mapViewer) {
		super(freeColClient, mapViewer);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		//Don't do anything
		return;
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		performAutoScrollIfActive(e);
	}
	

}
