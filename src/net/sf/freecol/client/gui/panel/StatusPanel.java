
package net.sf.freecol.client.gui.panel;

import java.awt.FlowLayout;
import java.util.logging.Logger;

import javax.swing.JLabel;

import net.sf.freecol.client.gui.Canvas;


/**
* A <code>Panel</code> for showing status information on screen.
*/
public final class StatusPanel extends FreeColPanel {
    private static final Logger logger = Logger.getLogger(StatusPanel.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";


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
