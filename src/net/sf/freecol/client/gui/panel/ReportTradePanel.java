package net.sf.freecol.client.gui.panel;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.List;
import java.util.Iterator;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.border.MatteBorder;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import cz.autel.dmi.HIGLayout;

/**
 * This panel displays the Trade Report.
 */
public final class ReportTradePanel extends ReportPanel implements ActionListener {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2006 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    /** How many colums are defined per label. */
    private static final int columnsPerLabel = 2;
    /** How many additional rows are defined. */
    private static final int extraRows = 5; // labels and separators
    /** How many additional columns are defined. */
    private static final int extraColumns = 1; // labels
    /** How many columns are defined all together. */
    private static final int columns = columnsPerLabel * Goods.NUMBER_OF_TYPES + extraColumns;
    /** How much space to leave between labels. */
    private static final int columnSeparatorWidth = 5;
    /** How wide the margins should be. */
    private static final int marginWidth = 12;
    /** The widths of the columns. */
    private static final int[] widths = new int[columns];
    /** The heights of the rows. */
    private static int[] heights;

    private final JLabel salesLabel;
    private final JLabel beforeTaxesLabel;
    private final JLabel afterTaxesLabel;
    private final MatteBorder myBorder = new MatteBorder(0, 0, 1, 0, Color.BLACK);
    private List<Settlement> colonies;

    /**
     * The constructor that will add the items to this panel.
     * @param parent The parent of this panel.
     */
    public ReportTradePanel(Canvas parent) {
        super(parent, Messages.message("menuBar.report.trade"));

        salesLabel = new JLabel(Messages.message("report.trade.unitsSold"), JLabel.TRAILING);
        beforeTaxesLabel = new JLabel(Messages.message("report.trade.beforeTaxes"), JLabel.TRAILING);
        afterTaxesLabel = new JLabel(Messages.message("report.trade.afterTaxes"), JLabel.TRAILING);

        // indexed from 1
        widths[0] = 0; // labels
        for (int label = 0; label < Goods.NUMBER_OF_TYPES; label++) {
            widths[columnsPerLabel * label + extraColumns] = columnSeparatorWidth;
            widths[columnsPerLabel * label + extraColumns + 1] = 0;
        }

        heights = null;
    }

    /**
     * Prepares this panel to be displayed.
     */
    public void initialize() {
        Player player = getCanvas().getClient().getMyPlayer();
        Market market = player.getGame().getMarket();

        // Display Panel
        reportPanel.removeAll();

        colonies = player.getSettlements();
        Collections.sort(colonies, getCanvas().getClient().getClientOptions().getColonyComparator());

        heights = new int[colonies.size() + extraRows];
        int labelColumn = 1;

        heights[extraRows - 1] = marginWidth; //separator

        reportPanel.setLayout(new HIGLayout(widths, heights));

        reportPanel.add(salesLabel, higConst.rc(2, labelColumn));
        reportPanel.add(beforeTaxesLabel, higConst.rc(3, labelColumn));
        reportPanel.add(afterTaxesLabel, higConst.rc(4, labelColumn));

        JLabel currentLabel;
        for (int goodsType = 0; goodsType < Goods.NUMBER_OF_TYPES; goodsType++) {
            int column = columnsPerLabel * (goodsType + 1) + extraColumns;
            int sales = player.getSales(goodsType);
            int beforeTaxes = player.getIncomeBeforeTaxes(goodsType);
            int afterTaxes = player.getIncomeAfterTaxes(goodsType);
            MarketLabel marketLabel = new MarketLabel(goodsType, market, getCanvas());
            marketLabel.setVerticalTextPosition(JLabel.BOTTOM);
            marketLabel.setHorizontalTextPosition(JLabel.CENTER);
            reportPanel.add(marketLabel, higConst.rc(1, column));

            currentLabel = new JLabel(String.valueOf(sales), JLabel.TRAILING);
            if (sales < 0) {
                currentLabel.setForeground(Color.RED);
            }
            reportPanel.add(currentLabel, higConst.rc(2, column));

            currentLabel = new JLabel(String.valueOf(beforeTaxes), JLabel.TRAILING);
            if (beforeTaxes < 0) {
                currentLabel.setForeground(Color.RED);
            }
            reportPanel.add(currentLabel, higConst.rc(3, column));

            currentLabel = new JLabel(String.valueOf(afterTaxes), JLabel.TRAILING);
            if (afterTaxes < 0) {
                currentLabel.setForeground(Color.RED);
            }
            reportPanel.add(currentLabel, higConst.rc(4, column));
        }

        Iterator colonyIterator = colonies.iterator();
        int row = 6;
        int colonyIndex = 0;
        while (colonyIterator.hasNext()) {
            Colony colony = (Colony) colonyIterator.next();
            JButton colonyButton = new JButton(colony.getName());
            colonyButton.setActionCommand(String.valueOf(colonyIndex));
            colonyButton.addActionListener(this);
            if (colony.getBuilding(Building.CUSTOM_HOUSE).isBuilt()) {
                colonyButton.setForeground(Color.BLUE);
            }
            reportPanel.add(colonyButton, higConst.rc(row, labelColumn));
            int adjustment = colony.getWarehouseCapacity() / 100;
            int[] lowLevel = colony.getLowLevel();
            int[] highLevel = colony.getHighLevel();
        
            for (int goodsType = 0; goodsType < Goods.NUMBER_OF_TYPES; goodsType++) {
                int column = columnsPerLabel * (goodsType + 1) + extraColumns;
                int amount = colony.getGoodsCount(goodsType);
                JLabel goodsLabel = new JLabel(String.valueOf(amount),
                                               JLabel.TRAILING);
                if (amount < lowLevel[goodsType] * adjustment ||
                    amount > highLevel[goodsType] * adjustment) {
                    goodsLabel.setForeground(Color.RED);
                } else if (amount > 200) {
                    goodsLabel.setForeground(Color.BLUE);
                } else if (amount > 100) {
                    goodsLabel.setForeground(Color.GREEN);
                }
                if (colony.getExports(goodsType)) {
                    goodsLabel.setBorder(myBorder);
                }
                reportPanel.add(goodsLabel, higConst.rc(row, column));
            }
            row++;
            colonyIndex++;
        }
    }


    /**
     * This function analyses an event and calls the right methods to take
     * care of the user's requests.
     * @param event The incoming ActionEvent.
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        int action = Integer.valueOf(command).intValue();
        if (action == OK) {
            super.actionPerformed(event);
        } else {
            getCanvas().showColonyPanel((Colony) colonies.get(action));
        }
    }
}

