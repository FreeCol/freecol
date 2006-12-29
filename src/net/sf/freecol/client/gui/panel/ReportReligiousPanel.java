package net.sf.freecol.client.gui.panel;

import java.awt.event.ActionListener;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Player;

/**
 * This panel displays the Religious Report.
 */
public final class ReportReligiousPanel extends ReportPanel implements ActionListener {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2006 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";


    private final ReportProductionPanel religiousReportPanel;

    /**
     * The constructor that will add the items to this panel.
     * @param parent The parent of this panel.
     */
    public ReportReligiousPanel(Canvas parent) {
        super(parent, Messages.message("menuBar.report.religion"));

        reportPanel.setLayout(new BoxLayout(reportPanel, BoxLayout.Y_AXIS));
        religiousReportPanel = new ReportProductionPanel(Goods.CROSSES, parent);
    }

    /**
     * Prepares this panel to be displayed.
     */
    public void initialize() {
        Player player = parent.getClient().getMyPlayer();

        // Display Panel
        reportPanel.removeAll();

        String summary = Messages.message("crosses") + ": " + player.getCrosses() + " / " + player.getCrossesRequired();
        reportPanel.add(new JLabel(summary));
        reportPanel.add(Box.createRigidArea(new Dimension(0, 24)));

        religiousReportPanel.initialize();
        reportPanel.add(religiousReportPanel);
    }
}

