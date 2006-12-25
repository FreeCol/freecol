package net.sf.freecol.client.gui.panel;

import java.awt.event.ActionListener;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.border.EmptyBorder;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.Player;
import cz.autel.dmi.*;

/**
 * This panel displays the ContinentalCongress Report.
 */
public final class ReportContinentalCongressPanel extends ReportPanel implements ActionListener {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2006 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    static final String title = Messages.message("report.continentalCongress.title");
    static final String none = Messages.message("report.continentalCongress.none");

    private final ReportProductionPanel continentalCongressReportPanel;
    private final JPanel summaryPanel;
    private final JPanel fatherPanel;
    private Canvas parent;

    /**
     * The constructor that will add the items to this panel.
     * @param parent The parent of this panel.
     */
    public ReportContinentalCongressPanel(Canvas parent) {
        super(parent, title);
        this.parent = parent;

        reportPanel.setLayout(new BoxLayout(reportPanel, BoxLayout.Y_AXIS));

        summaryPanel = new JPanel();
        int[] widths = {0, 12, 0};
        int[] heights = {0, 0, 0};
        summaryPanel.setLayout(new HIGLayout(widths, heights));

        fatherPanel = new JPanel();
        fatherPanel.setLayout(new GridLayout(0, 4));

        continentalCongressReportPanel = new ReportProductionPanel(Goods.BELLS, parent);
    }

    /**
     * Prepares this panel to be displayed.
     */
    public void initialize() {
        Player player = parent.getClient().getMyPlayer();

        // Display Panel
        summaryPanel.removeAll();
        fatherPanel.removeAll();
        reportPanel.removeAll();

        HIGConstraints higConst = new HIGConstraints();

        // summary
        summaryPanel.add(new JLabel(Messages.message("report.continentalCongress.recruiting")),
                         higConst.rc(1, 1));
        if (player.getCurrentFather() == FoundingFather.NONE) {
            summaryPanel.add(new JLabel(none), higConst.rc(1, 3));
        } else {
            JLabel currentFatherLabel = new JLabel(Messages.message(FoundingFather.getName(player.getCurrentFather())));
            currentFatherLabel.setToolTipText(Messages.message(FoundingFather.getDescription(player.getCurrentFather())));
            summaryPanel.add(currentFatherLabel, higConst.rc(1, 3));
        }
        summaryPanel.add(new JLabel(Messages.message("report.continentalCongress.bellsCurrent")),
                         higConst.rc(2, 1));
        Goods current = new Goods(Goods.BELLS);
        current.setAmount(player.getBells());
        GoodsLabel currentLabel = new GoodsLabel(current, parent);
        currentLabel.setHorizontalAlignment(JLabel.LEADING);
        summaryPanel.add(currentLabel, higConst.rc(2, 3));
        summaryPanel.add(new JLabel(Messages.message("report.continentalCongress.bellsRequired")),
                         higConst.rc(3, 1));
        Goods total = new Goods(Goods.BELLS);
        total.setAmount(player.getTotalFoundingFatherCost());
        GoodsLabel totalLabel = new GoodsLabel(total, parent);
        totalLabel.setHorizontalAlignment(JLabel.LEADING);
        summaryPanel.add(totalLabel, higConst.rc(3, 3));
        summaryPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        reportPanel.add(summaryPanel);
        reportPanel.add(Box.createRigidArea(new Dimension(0, 24)));

        // founding fathers
        if (player.getFatherCount() < 1) {
            JLabel fatherLabel = new JLabel(none);
            fatherLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            fatherPanel.add(fatherLabel);
        } else {
            for (int fatherId = 0; fatherId < FoundingFather.FATHER_COUNT; fatherId++) {
                if (player.hasFather(fatherId)) {
                    JLabel fatherLabel = new JLabel(Messages.message(FoundingFather.getName(fatherId)));
                    fatherLabel.setToolTipText(Messages.message(FoundingFather.getDescription(fatherId)));
                    fatherLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                    fatherPanel.add(fatherLabel);
                }
            }
        }
        fatherPanel.setBorder(BorderFactory.createTitledBorder(player.getNationAsString() + " " + title));
        fatherPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        reportPanel.add(fatherPanel);
        reportPanel.add(Box.createRigidArea(new Dimension(0, 24)));

        // production per colony
        continentalCongressReportPanel.initialize();
        continentalCongressReportPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        reportPanel.add(continentalCongressReportPanel);
    }
}

