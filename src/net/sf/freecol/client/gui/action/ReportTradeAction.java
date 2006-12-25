

package net.sf.freecol.client.gui.action;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.panel.ReportTradePanel;


/**
 * 
 */
public class ReportTradeAction extends MapboardAction {
	private static final Logger logger = Logger.getLogger(ReportTradeAction.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    public static final String ID = ReportTradeAction.class.toString();
    
    /**
     * Creates this action.
     * @param freeColClient The main controller object for the client.
     */
    ReportTradeAction(FreeColClient freeColClient) {
    	super(freeColClient, "menuBar.report.trade", null, KeyEvent.VK_T);        
    }
    
    /**
     * Checks if this action should be enabled.
     * 
     * @return true if this action should be enabled.
     */
    protected boolean shouldBeEnabled() {
    	return true;
    }    
    
    /**
     * Returns the id of this <code>Option</code>.
     * 
     * @return 
     */
    public String getId() {
        return ID;
    }

    /**
     * Applies this action.
     * @param e The <code>ActionEvent</code>.
     */
    public void actionPerformed(ActionEvent e) {
        freeColClient.getCanvas().showReportPanel(ReportTradePanel.class.getName());
    }
}
