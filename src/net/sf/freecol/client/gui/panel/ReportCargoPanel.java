package net.sf.freecol.client.gui.panel;

import java.awt.Dimension;
import java.awt.event.ActionListener;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;

/**
 * This panel displays the Cargo Report.
 */
public final class ReportCargoPanel extends ReportPanel implements ActionListener {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private static ReportUnitPanel reportUnitPanel;

    /**
     * The constructor that will add the items to this panel.
     * @param parent The parent of this panel.
     */
    public ReportCargoPanel(Canvas parent) {
        super(parent, Messages.message("menuBar.report.cargo"));
        reportUnitPanel = new ReportUnitPanel(ReportUnitPanel.CARGO, true, getCanvas(), this);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(750, 600);
    }
    
    @Override
    public Dimension getPreferredSize() {
        return getMinimumSize();
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
