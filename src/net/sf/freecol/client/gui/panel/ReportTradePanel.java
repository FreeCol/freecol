package net.sf.freecol.client.gui.panel;

import java.awt.Color;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import net.sf.freecol.FreeCol;
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
    public static final String COPYRIGHT = "Copyright (C) 2003-2006 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    /** How many colums are defined per label. */
    private static final int columnsPerLabel = 2;

    /** How many additional rows are defined. */
    private static final int extraRows = 5; // labels and separators
    private static final int extraBottomRows = 2;

    /** How many additional columns are defined. */
    private static final int extraColumns = 1; // labels

    /** How many columns are defined all together. */
    private static final int columns = columnsPerLabel * Goods.NUMBER_OF_TYPES + extraColumns;

    /** How wide the margins should be. */
    private static final int marginWidth = 12;

    private final JLabel salesLabel;

    private final JLabel beforeTaxesLabel;

    private final JLabel afterTaxesLabel;

    private List<Colony> colonies;


    /**
     * The constructor that will add the items to this panel.
     * 
     * @param parent The parent of this panel.
     */
    public ReportTradePanel(Canvas parent) {
        super(parent, Messages.message("menuBar.report.trade"));

        salesLabel = new JLabel(Messages.message("report.trade.unitsSold"), JLabel.TRAILING);
        salesLabel.setBorder(FreeColPanel.LEFTCELLBORDER);
        beforeTaxesLabel = new JLabel(Messages.message("report.trade.beforeTaxes"), JLabel.TRAILING);
        beforeTaxesLabel.setBorder(FreeColPanel.LEFTCELLBORDER);
        afterTaxesLabel = new JLabel(Messages.message("report.trade.afterTaxes"), JLabel.TRAILING);
        afterTaxesLabel.setBorder(FreeColPanel.LEFTCELLBORDER);
    }

    /**
     * Prepares this panel to be displayed.
     */
    public void initialize() {
        Player player = getCanvas().getClient().getMyPlayer();
        Market market = player.getMarket();

        // Display Panel
        reportPanel.removeAll();

        colonies = player.getColonies();
        Collections.sort(colonies, getCanvas().getClient().getClientOptions().getColonyComparator());

        int[] widths = new int[columns];
        int[] heights = new int[colonies.size() + extraRows + extraBottomRows];

        int labelColumn = 1;

        heights[extraRows - 1] = marginWidth; // separator
        heights[heights.length - 2] = marginWidth;

        reportPanel.setLayout(new HIGLayout(widths, heights));
        JLabel emptyLabel = new JLabel();
        emptyLabel.setBorder(FreeColPanel.TOPLEFTCELLBORDER);
        reportPanel.add(emptyLabel, higConst.rc(1, labelColumn));
        reportPanel.add(salesLabel, higConst.rc(2, labelColumn));
        reportPanel.add(beforeTaxesLabel, higConst.rc(3, labelColumn));
        reportPanel.add(afterTaxesLabel, higConst.rc(4, labelColumn));

        JLabel currentLabel;
        for (int goodsIndex = 0; goodsIndex < Goods.NUMBER_OF_TYPES; goodsIndex++) {
            int column = goodsIndex + extraColumns + 1;
            int sales = player.getSales(goodsIndex);
            int beforeTaxes = player.getIncomeBeforeTaxes(goodsIndex);
            int afterTaxes = player.getIncomeAfterTaxes(goodsIndex);
            MarketLabel marketLabel = new MarketLabel(FreeCol.getSpecification().getGoodsType(goodsIndex), market, getCanvas());
            marketLabel.setBorder(FreeColPanel.TOPCELLBORDER);
            marketLabel.setVerticalTextPosition(JLabel.BOTTOM);
            marketLabel.setHorizontalTextPosition(JLabel.CENTER);
            reportPanel.add(marketLabel, higConst.rc(1, column));

            currentLabel = new JLabel(String.valueOf(sales), JLabel.TRAILING);
            currentLabel.setBorder(FreeColPanel.CELLBORDER);
            if (sales < 0) {
                currentLabel.setForeground(Color.RED);
            }
            reportPanel.add(currentLabel, higConst.rc(2, column));

            currentLabel = new JLabel(String.valueOf(beforeTaxes), JLabel.TRAILING);
            currentLabel.setBorder(FreeColPanel.CELLBORDER);
            if (beforeTaxes < 0) {
                currentLabel.setForeground(Color.RED);
            }
            reportPanel.add(currentLabel, higConst.rc(3, column));

            currentLabel = new JLabel(String.valueOf(afterTaxes), JLabel.TRAILING);
            currentLabel.setBorder(FreeColPanel.CELLBORDER);
            if (afterTaxes < 0) {
                currentLabel.setForeground(Color.RED);
            }
            reportPanel.add(currentLabel, higConst.rc(4, column));
        }

        int row = extraRows + 1;

        for (int colonyIndex = 0; colonyIndex < colonies.size(); colonyIndex++) {
            Colony colony = colonies.get(colonyIndex);
            JButton colonyButton = createColonyButton(colonyIndex);
            reportPanel.add(colonyButton, higConst.rc(row, labelColumn));
            for (int goodsIndex = 0; goodsIndex < Goods.NUMBER_OF_TYPES; goodsIndex++) {
                int column = goodsIndex + 1 + extraColumns;
                int amount = colony.getGoodsCount(goodsIndex);
                JLabel goodsLabel = new JLabel(String.valueOf(amount), JLabel.TRAILING);
                if (colonyIndex == 0) {
                    goodsLabel.setBorder(FreeColPanel.TOPCELLBORDER);
                } else {
                    goodsLabel.setBorder(FreeColPanel.CELLBORDER);
                }
                if (colony.getExports(goodsIndex)) {
                    goodsLabel.setText("*" + String.valueOf(amount));
                }
                if (amount > 200) {
                    goodsLabel.setForeground(Color.BLUE);
                } else if (amount > 100) {
                    goodsLabel.setForeground(Color.GREEN);
                }
                reportPanel.add(goodsLabel, higConst.rc(row, column));
            }
            row++;
        }

        row++;
        reportPanel.add(new JLabel(Messages.message("report.trade.hasCustomHouse")),
                        higConst.rcwh(row, 1, widths.length, 1));
    }

    private JButton createColonyButton(int index) {

        JButton button = new JButton();
        String name = colonies.get(index).getName();
        if (colonies.get(index).hasAbility("model.ability.export")) {
            name += "*";
        }
        button.setText(name);
        button.setOpaque(false);
        button.setForeground(LINK_COLOR);
        button.setHorizontalAlignment(SwingConstants.LEADING);
        button.setAlignmentY(0.8f);
        if (index == 0) {
            button.setBorder(FreeColPanel.TOPLEFTCELLBORDER);
        } else {
            button.setBorder(FreeColPanel.LEFTCELLBORDER);
        }

        button.setActionCommand(String.valueOf(index));
        button.addActionListener(this);
        return button;
    }

    /**
     * This function analyses an event and calls the right methods to take care
     * of the user's requests.
     * 
     * @param event The incoming ActionEvent.
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        int action = Integer.valueOf(command).intValue();
        if (action == OK) {
            super.actionPerformed(event);
        } else {
            getCanvas().showColonyPanel(colonies.get(action));
        }
    }
}
