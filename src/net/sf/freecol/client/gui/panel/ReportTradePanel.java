package net.sf.freecol.client.gui.panel;

import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.GridLayout;

import javax.swing.JLabel;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Player;

/**
 * This panel displays the Trade Report.
 */
public final class ReportTradePanel extends ReportPanel implements ActionListener {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private final JLabel[] goodsLabels;
    private final JLabel blankLabel;
    private final JLabel salesLabel;
    private final JLabel beforeTaxesLabel;
    private final JLabel afterTaxesLabel;
    

    /**
     * The constructor that will add the items to this panel.
     * @param parent The parent of this panel.
     */
    public ReportTradePanel(Canvas parent) {
        super(parent, "Trade Advisor");
        goodsLabels = new JLabel[Goods.NUMBER_OF_TYPES];
        for (int i = 0; i < Goods.NUMBER_OF_TYPES; i++) {
            goodsLabels[i] = new JLabel(parent.getImageProvider().getGoodsImageIcon(i));
        }
        blankLabel = new JLabel();
        salesLabel = new JLabel(Messages.message("report.trade.unitsSold"), JLabel.TRAILING);
        beforeTaxesLabel = new JLabel(Messages.message("report.trade.beforeTaxes"), JLabel.TRAILING);
        afterTaxesLabel = new JLabel(Messages.message("report.trade.afterTaxes"), JLabel.TRAILING);
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
        reportPanel.setLayout(new GridLayout(Goods.NUMBER_OF_TYPES + 1, 4));
        reportPanel.add(blankLabel);
        reportPanel.add(salesLabel);
        reportPanel.add(beforeTaxesLabel);
        reportPanel.add(afterTaxesLabel);

        int sales, beforeTaxes, afterTaxes;
        JLabel currentLabel;
        for (int i = 0; i < Goods.NUMBER_OF_TYPES; i++) {
            sales = player.getSales(i);
            beforeTaxes = player.getIncomeBeforeTaxes(i);
            afterTaxes = player.getIncomeAfterTaxes(i);
            reportPanel.add(goodsLabels[i]);
            
            currentLabel = new JLabel(String.valueOf(sales), JLabel.TRAILING);
            if (sales < 0) {
                currentLabel.setForeground(Color.RED);
            }
            reportPanel.add(currentLabel);

            currentLabel = new JLabel(String.valueOf(beforeTaxes), JLabel.TRAILING);
            if (beforeTaxes < 0) {
                currentLabel.setForeground(Color.RED);
            }
            reportPanel.add(currentLabel);

            currentLabel = new JLabel(String.valueOf(afterTaxes), JLabel.TRAILING);
            if (afterTaxes < 0) {
                currentLabel.setForeground(Color.RED);
            }
            reportPanel.add(currentLabel);
        }

        reportPanel.doLayout();
    }
}
