package net.sf.freecol.client.gui.panel;

import java.awt.event.ActionListener;


import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;

/**
 * This panel displays the Military Report.
 */
public final class ReportMilitaryPanel extends ReportPanel implements ActionListener {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2006 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private static ReportUnitPanel reportUnitPanel;

    /**
     * The constructor that will add the items to this panel.
     * @param parent The parent of this panel.
     */
    public ReportMilitaryPanel(Canvas parent) {
        super(parent, Messages.message("menuBar.report.military"));
        reportUnitPanel = new ReportUnitPanel(false, false, getCanvas(), this);
    }

    /**
     * Prepares this panel to be displayed.
     */
    public void initialize() {
        reportPanel.removeAll();
        reportUnitPanel.initialize();
        reportPanel.add(reportUnitPanel);
    }
}
