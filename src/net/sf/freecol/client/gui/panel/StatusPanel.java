/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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


package net.sf.freecol.client.gui.panel;

import java.awt.FlowLayout;
import java.util.logging.Logger;

import javax.swing.JLabel;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;


/**
* A <code>Panel</code> for showing status information on screen.
*/
public final class StatusPanel extends FreeColPanel {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(StatusPanel.class.getName());

    private final JLabel        statusLabel;

    /**
    * Creates a new <code>StatusPanel</code>.
     * @param freeColClient 
    * @param parent The parent of this panel.
    */
    public StatusPanel(FreeColClient freeColClient, GUI gui) {
        super(freeColClient, gui, new FlowLayout());

        setFocusCycleRoot(false);
        setFocusable(false);
        
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
