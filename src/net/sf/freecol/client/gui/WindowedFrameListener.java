/*
 *  WindowedFrameListener.java - The WindowListener for the WindowedFrame class.
 *
 *  Copyright (C) 2002  The FreeCol Team
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Library General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package net.sf.freecol.client.gui;

import java.awt.event.WindowListener;
import java.awt.event.WindowEvent;

/**
* The WindowListener for the WindowedFrame class.
*/
public final class WindowedFrameListener implements WindowListener {
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
