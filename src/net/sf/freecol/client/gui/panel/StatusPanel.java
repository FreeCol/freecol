/*
 *  StatusPanel.java - Class for showing status information on screen.
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

package net.sf.freecol.client.gui.panel;

import javax.swing.*;
import java.awt.FlowLayout;
import javax.swing.border.BevelBorder;
import java.util.logging.Logger;

import net.sf.freecol.client.gui.Canvas;


/**
* A <code>Panel</code> for showing status information on screen.
*/
public final class StatusPanel extends FreeColPanel {
    private static final Logger logger = Logger.getLogger(StatusPanel.class.getName());


    private final Canvas        parent;
    private final JLabel        statusLabel;


    
    

    /**
    * Creates a new <code>StatusPanel</code>.
    * @param parent The parent of this panel.
    */
    public StatusPanel(Canvas parent) {
        super(new FlowLayout());

        setFocusCycleRoot(false);
        setFocusable(false);
        
        this.parent = parent;

        statusLabel = new JLabel();
        add(statusLabel);

        setSize(260, 60);
    }
    
    
    
    
    
    /**
    * Sets a new status message to be displayed by this
    * <code>StatusPanel</code>.
    *
    * @param message The message to be displayed.
    */
    public void setStatusMessage(String message) {
        statusLabel.setText(message);
        setSize(getPreferredSize());
    }
}
