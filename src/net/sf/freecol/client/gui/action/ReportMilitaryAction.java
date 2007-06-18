

package net.sf.freecol.client.gui.action;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import javax.swing.KeyStroke;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.panel.ReportMilitaryPanel;


/**
 * 
 */
public class ReportMilitaryAction extends MapboardAction {
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(ReportMilitaryAction.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    public static final String ID = "reportMilitaryAction";
    
    /**
     * Creates this action.
     * @param freeColClient The main controller object for the client.
     */
    ReportMilitaryAction(FreeColClient freeColClient) {
        super(freeColClient, "menuBar.report.military", null, KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0));
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
     * @return "reportMilitaryAction"
     */
    public String getId() {
        return ID;
    }

    /**
     * Applies this action.
     * @param e The <code>ActionEvent</code>.
     */
    public void actionPerformed(ActionEvent e) {
        freeColClient.getCanvas().showReportPanel(ReportMilitaryPanel.class.getName());
    }
}
