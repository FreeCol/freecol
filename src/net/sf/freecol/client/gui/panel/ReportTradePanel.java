package net.sf.freecol.client.gui.panel;

import java.awt.Color;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.MatteBorder;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Market;
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
    private final int[] widths = new int[columns];
    /** The heights of the rows. */
    private final int[] heights;

    private final JLabel[] goodsLabels;
    //    private final JLabel priceLabel;
    private final JLabel salesLabel;
    private final JLabel beforeTaxesLabel;
    private final JLabel afterTaxesLabel;
    private final JScrollPane scrollPane;
    private final JPanel tradeReportPanel;
    private final MatteBorder myBorder = new MatteBorder(0, 0, 1, 0, Color.BLACK);

    private Canvas parent;

    /**
     * The constructor that will add the items to this panel.
     * @param parent The parent of this panel.
     */
    public ReportTradePanel(Canvas parent) {
        super(parent, Messages.message("menuBar.report.trade"));
        this.parent = parent;
        goodsLabels = new JLabel[Goods.NUMBER_OF_TYPES];

        //priceLabel = new JLabel(Messages.message("report.trade.prices"), JLabel.TRAILING);
        salesLabel = new JLabel(Messages.message("report.trade.unitsSold"), JLabel.TRAILING);
        beforeTaxesLabel = new JLabel(Messages.message("report.trade.beforeTaxes"), JLabel.TRAILING);
        afterTaxesLabel = new JLabel(Messages.message("report.trade.afterTaxes"), JLabel.TRAILING);

        // indexed from 1
        widths[0] = marginWidth; // left margin
        widths[1] = 0; // labels
        widths[widths.length - 1] = marginWidth; // right margin

        for (int label = 0; label < goodsLabels.length; label++) {
            //goodsLabels[i] = new JLabel(parent.getImageProvider().getGoodsImageIcon(label));
            widths[columnsPerLabel * label + 2] = columnSeparatorWidth;
            widths[columnsPerLabel * label + 3] = 0;
        }

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
        Market market = player.getGame().getMarket();
        // Display Panel
        reportPanel.removeAll();
        tradeReportPanel.removeAll();

        Iterator colonyIterator = player.getColonyIterator();
        ArrayList colonies = new ArrayList();
        while (colonyIterator.hasNext()) {
            colonies.add(colonyIterator.next());
        }
        int[] heights = new int[colonies.size() + extraRows];
        
        for (int h = 1; h < heights.length; h++) {
            heights[h] = 0;
        }

        heights[0] = marginWidth;
        heights[5] = marginWidth; //separator
        heights[heights.length - 1] = marginWidth;

        tradeReportPanel.setLayout(new HIGLayout(widths, heights));

        //tradeReportPanel.add(priceLabel, higConst.rc(3, 2));
        tradeReportPanel.add(salesLabel, higConst.rc(3, 2));
        tradeReportPanel.add(beforeTaxesLabel, higConst.rc(4, 2));
        tradeReportPanel.add(afterTaxesLabel, higConst.rc(5, 2));

        JLabel currentLabel;
        for (int goodsType = 0; goodsType < Goods.NUMBER_OF_TYPES; goodsType++) {
            int column = columnsPerLabel * goodsType + 4;
            int sales = player.getSales(goodsType);
            int beforeTaxes = player.getIncomeBeforeTaxes(goodsType);
            int afterTaxes = player.getIncomeAfterTaxes(goodsType);
            MarketLabel marketLabel = new MarketLabel(goodsType, market, parent);
            marketLabel.setVerticalTextPosition(JLabel.BOTTOM);
            marketLabel.setHorizontalTextPosition(JLabel.CENTER);
            tradeReportPanel.add(marketLabel, higConst.rc(2, column));

            //            currentLabel = new JLabel(String.valueOf(market.paidForSale(goodsType)) + "/" +
            //                        String.valueOf(market.costToBuy(goodsType)), JLabel.TRAILING);
            // tradeReportPanel.add(currentLabel, higConst.rc(3, column));

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
        int row = 7;
        while (colonyIterator.hasNext()) {
            Colony colony = (Colony) colonyIterator.next();
            JLabel colonyLabel = new JLabel(colony.getName());
            if (colony.getBuilding(Building.CUSTOM_HOUSE).isBuilt()) {
                colonyLabel.setBorder(myBorder);
            }
            tradeReportPanel.add(colonyLabel, higConst.rc(row, 2));
            for (int goodsType = 0; goodsType < Goods.NUMBER_OF_TYPES; goodsType++) {
                int column = columnsPerLabel * goodsType + 4;
                int amount = colony.getGoodsCount(goodsType);
                JLabel goodsLabel = new JLabel(String.valueOf(amount),
                                               JLabel.TRAILING);
                if (amount > 100) {
                    goodsLabel.setForeground(Color.GREEN);
                } else if (amount > 200) {
                    goodsLabel.setForeground(Color.BLUE);
                }
                if (colony.getExports(goodsType)) {
                    goodsLabel.setBorder(myBorder);
                }
                tradeReportPanel.add(goodsLabel, higConst.rc(row, column));
            }
            row++;
        }

        reportPanel.add(tradeReportPanel);
    }
}

