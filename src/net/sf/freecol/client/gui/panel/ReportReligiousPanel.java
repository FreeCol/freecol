package net.sf.freecol.client.gui.panel;

import java.awt.event.ActionListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Player;

import cz.autel.dmi.HIGLayout;

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

        int[] widths = new int[] {0};
        int[] heights = new int[] {0, 12, 0};
        reportPanel.setLayout(new HIGLayout(widths, heights));
        religiousReportPanel = new ReportProductionPanel(Goods.CROSSES, getCanvas(), this);
    }

    /**
     * Prepares this panel to be displayed.
     */
    public void initialize() {
        Player player = getCanvas().getClient().getMyPlayer();

        // Display Panel
        reportPanel.removeAll();
        religiousReportPanel.initialize();
        
        JPanel summaryPanel = new JPanel();
        summaryPanel.setOpaque(false);
        summaryPanel.add(new JLabel(Messages.message("crosses")));
        int crosses = player.getCrosses();
        int required = player.getCrossesRequired();
        int production = religiousReportPanel.getTotalProduction();

        FreeColProgressBar progressBar = new FreeColProgressBar(getCanvas(), Goods.CROSSES);
        progressBar.update(0, required, crosses, production);
        summaryPanel.add(progressBar);

        reportPanel.add(summaryPanel, higConst.rc(1, 1));
        reportPanel.add(religiousReportPanel, higConst.rc(3, 1));
    }
}

