package net.sf.freecol.client.gui.panel;

import java.awt.Color;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Player;

import cz.autel.dmi.HIGLayout;

/**
 * This panel displays the Trade Report.
 */
public final class ReportTradePanel extends ReportPanel implements ActionListener {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    /** How many colums are defined per label. */
    private final int columnsPerLabel = 2;
    /** How many additional columns are defined. */
    private final int extraColumns = 3; // labels and margins
    /** How many additional rows are defined. */
    private final int extraRows = 7; // labels and margins
    /** How many columns are defined all together. */
    private final int columns = columnsPerLabel * Goods.NUMBER_OF_TYPES + extraColumns;
    /** How much space to leave between labels. */
    private final int columnSeparatorWidth = 5;
    /** How wide the margins should be. */
    private final int marginWidth = 12;
    /** The widths of the columns. */
    private final int widths[] = new int[columns];
    /** The heights of the rows. */
    private final int heights[];

    private final JLabel[] goodsLabels;
    private final JLabel salesLabel;
    private final JLabel beforeTaxesLabel;
    private final JLabel afterTaxesLabel;
    private final JScrollPane scrollPane;
    private final JPanel tradeReportPanel;

    /**
     * The constructor that will add the items to this panel.
     * @param parent The parent of this panel.
     */
    public ReportTradePanel(Canvas parent) {
        super(parent, Messages.message("menuBar.report.trade"));
        goodsLabels = new JLabel[Goods.NUMBER_OF_TYPES];
        for (int i = 0; i < goodsLabels.length; i++) {
            goodsLabels[i] = new JLabel(parent.getImageProvider().getGoodsImageIcon(i));
        }
            
        salesLabel = new JLabel(Messages.message("report.trade.unitsSold"), JLabel.TRAILING);
        beforeTaxesLabel = new JLabel(Messages.message("report.trade.beforeTaxes"), JLabel.TRAILING);
        afterTaxesLabel = new JLabel(Messages.message("report.trade.afterTaxes"), JLabel.TRAILING);

        widths[0] = marginWidth; // left margin
        widths[1] = 0; // labels
        for (int w = 0; w < goodsLabels.length; w++) {
            widths[columnsPerLabel * w + 2] = columnSeparatorWidth;
            widths[columnsPerLabel * w + 3] = 0;
        }
        widths[widths.length - 1] = marginWidth; // right margin

        heights = null;

        tradeReportPanel = new JPanel();
        scrollPane = new JScrollPane(tradeReportPanel,
                                     JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                     JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    }

    /**
     * Prepares this panel to be displayed.
     */
    public void initialize() {
        Player player = parent.getClient().getMyPlayer();
        // Display Panel
        reportPanel.removeAll();

        Iterator colonyIterator = player.getColonyIterator();
        ArrayList colonies = new ArrayList();
        while (colonyIterator.hasNext()) {
            colonies.add(colonyIterator.next());
        }
        int heights[] = new int[colonies.size() + extraRows];
        for (int h = 0; h < heights.length; h++) {
            heights[h] = 0;
        }
        heights[0] = marginWidth;
        heights[5] = marginWidth;
        heights[heights.length - 1] = marginWidth;

        tradeReportPanel.setLayout(new HIGLayout(widths, heights));

        tradeReportPanel.add(salesLabel, higConst.rc(3, 2));
        tradeReportPanel.add(beforeTaxesLabel, higConst.rc(4, 2));
        tradeReportPanel.add(afterTaxesLabel, higConst.rc(5, 2));

        int sales, beforeTaxes, afterTaxes;
        JLabel currentLabel;
        for (int i = 0; i < Goods.NUMBER_OF_TYPES; i++) {
            int column = columnsPerLabel * i + 4;
            sales = player.getSales(i);
            beforeTaxes = player.getIncomeBeforeTaxes(i);
            afterTaxes = player.getIncomeAfterTaxes(i);
            tradeReportPanel.add(goodsLabels[i], higConst.rc(2, column));
            
            currentLabel = new JLabel(String.valueOf(sales), JLabel.TRAILING);
            if (sales < 0) {
                currentLabel.setForeground(Color.RED);
            }
            tradeReportPanel.add(currentLabel, higConst.rc(3, column));

            currentLabel = new JLabel(String.valueOf(beforeTaxes), JLabel.TRAILING);
            if (beforeTaxes < 0) {
                currentLabel.setForeground(Color.RED);
            }
            tradeReportPanel.add(currentLabel, higConst.rc(4, column));

            currentLabel = new JLabel(String.valueOf(afterTaxes), JLabel.TRAILING);
            if (afterTaxes < 0) {
                currentLabel.setForeground(Color.RED);
            }
            tradeReportPanel.add(currentLabel, higConst.rc(5, column));
        }

        colonyIterator = colonies.iterator();
        int row = extraRows;
        while (colonyIterator.hasNext()) {
            Colony colony = (Colony) colonyIterator.next();
            currentLabel = new JLabel(colony.getName());
            tradeReportPanel.add(currentLabel, higConst.rc(row, 2));
            for (int i = 0; i < Goods.NUMBER_OF_TYPES; i++) {
                int column = columnsPerLabel * i + 4;
                int amount = colony.getGoodsCount(i);
                currentLabel = new JLabel(String.valueOf(amount),
                                          JLabel.TRAILING);
                if (amount > 100) {
                    currentLabel.setForeground(Color.GREEN);
                } else if (amount > 200) {
                    currentLabel.setForeground(Color.BLUE);
                }
                tradeReportPanel.add(currentLabel, higConst.rc(row, column));
            }
            row++;
        }

        reportPanel.add(tradeReportPanel);
    }
}
