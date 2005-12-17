package net.sf.freecol.client.gui.panel;

import java.awt.event.ActionListener;

import javax.swing.JLabel;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Player;

/**
 * This panel displays the Foreign Affairs Report.
 */
public final class ReportReligiousPanel extends ReportPanel implements ActionListener {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
    /**
     * The constructor that will add the items to this panel.
     * @param parent The parent of this panel.
     */
    public ReportReligiousPanel(Canvas parent) {
        super(parent, "Religious Advisor");
    }

    /**
     * Prepares this panel to be displayed.
     */
    public void initialize() {
        Player player = parent.getClient().getMyPlayer();
        int crosses = player.getCrosses();
        int required = player.getCrossesRequired();
        // Display Panel
        reportPanel.removeAll();
//      reportPanel.setLayout(new GridLayout(8, 1));
        String report = "<html><p align=center>" + Messages.message("crosses") + " ";
        report += crosses + " / " + required;
        report += "</html>";
        JLabel label;
        label = new JLabel(report);
        label.setVerticalAlignment(JLabel.TOP);
        label.setVerticalTextPosition(JLabel.TOP);
        reportPanel.add(label);
        reportPanel.doLayout();
    }
}
