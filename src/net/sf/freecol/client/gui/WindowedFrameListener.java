
package net.sf.freecol.client.gui;

import java.awt.event.WindowListener;
import java.awt.event.WindowEvent;

/**
* The WindowListener for the WindowedFrame class.
*/
public final class WindowedFrameListener implements WindowListener {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2004 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private final Canvas parent;
    
    /**
    * Constructs this WindowListener.
    */
    public WindowedFrameListener(Canvas canvas) {
        parent = canvas;
    }
    
    /**
    * Invoked when the window gets activated.
    * @param event The event that has information on the action.
    */
    public void windowActivated(WindowEvent event) {
    }
    
    /**
    * Invoked when the window gets closed.
    * @param event The event that has information on the action.
    */
    public void windowClosed(WindowEvent event) {
    }
    
    /**
    * Invoked when the window is closing.
    * @param event The event that has information on the action.
    */
    public void windowClosing(WindowEvent event) {
        parent.quit();
    }
    
    /**
    * Invoked when the window gets deactivated.
    * @param event The event that has information on the action.
    */
    public void windowDeactivated(WindowEvent event) {
    }
    
    /**
    * Invoked when the window gets deiconified.
    * @param event The event that has information on the action.
    */
    public void windowDeiconified(WindowEvent event) {
    }
    
    /**
    * Invoked when the window gets iconified.
    * @param event The event that has information on the action.
    */
    public void windowIconified(WindowEvent event) {
    }
    
    /**
    * Invoked when the window gets opened.
    * @param event The event that has information on the action.
    */
    public void windowOpened(WindowEvent event) {
    }
}
