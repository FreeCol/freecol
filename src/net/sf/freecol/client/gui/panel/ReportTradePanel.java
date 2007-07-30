package net.sf.freecol.client.gui.panel;

import java.awt.Color;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.List;

import javax.swing.border.Border;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

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

    private static final Border border1 =
        BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 1, 1, LINK_COLOR),
                                           BorderFactory.createEmptyBorder(2, 2, 2, 2));
    private static final Border border2 = 
        BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, LINK_COLOR),
                                           BorderFactory.createEmptyBorder(2, 2, 2, 2));
    private static final Border border3 = 
        BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 1, 1, 1, LINK_COLOR),
                                           BorderFactory.createEmptyBorder(2, 2, 2, 2));
    private static final Border border4 = 
        BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, LINK_COLOR),
                                           BorderFactory.createEmptyBorder(2, 2, 2, 2));



    /**
     * The constructor that will add the items to this panel.
     * 
     * @param parent The parent of this panel.
     */
    public ReportTradePanel(Canvas parent) {
        super(parent, Messages.message("menuBar.report.trade"));

        salesLabel = new JLabel(Messages.message("report.trade.unitsSold"), JLabel.TRAILING);
        salesLabel.setBorder(border3);
        beforeTaxesLabel = new JLabel(Messages.message("report.trade.beforeTaxes"), JLabel.TRAILING);
        beforeTaxesLabel.setBorder(border3);
        afterTaxesLabel = new JLabel(Messages.message("report.trade.afterTaxes"), JLabel.TRAILING);
        afterTaxesLabel.setBorder(border3);
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
        emptyLabel.setBorder(border4);
        reportPanel.add(emptyLabel, higConst.rc(1, labelColumn));
        reportPanel.add(salesLabel, higConst.rc(2, labelColumn));
        reportPanel.add(beforeTaxesLabel, higConst.rc(3, labelColumn));
        reportPanel.add(afterTaxesLabel, higConst.rc(4, labelColumn));

        JLabel currentLabel;
        for (int goodsType = 0; goodsType < Goods.NUMBER_OF_TYPES; goodsType++) {
            int column = goodsType + extraColumns + 1;
            int sales = player.getSales(goodsType);
            int beforeTaxes = player.getIncomeBeforeTaxes(goodsType);
            int afterTaxes = player.getIncomeAfterTaxes(goodsType);
            MarketLabel marketLabel = new MarketLabel(goodsType, market, getCanvas());
            marketLabel.setBorder(border1);
            marketLabel.setVerticalTextPosition(JLabel.BOTTOM);
            marketLabel.setHorizontalTextPosition(JLabel.CENTER);
            reportPanel.add(marketLabel, higConst.rc(1, column));

            currentLabel = new JLabel(String.valueOf(sales), JLabel.TRAILING);
            currentLabel.setBorder(border2);
            if (sales < 0) {
                currentLabel.setForeground(Color.RED);
            }
            reportPanel.add(currentLabel, higConst.rc(2, column));

            currentLabel = new JLabel(String.valueOf(beforeTaxes), JLabel.TRAILING);
            currentLabel.setBorder(border2);
            if (beforeTaxes < 0) {
                currentLabel.setForeground(Color.RED);
            }
            reportPanel.add(currentLabel, higConst.rc(3, column));

            currentLabel = new JLabel(String.valueOf(afterTaxes), JLabel.TRAILING);
            currentLabel.setBorder(border2);
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
            for (int goodsType = 0; goodsType < Goods.NUMBER_OF_TYPES; goodsType++) {
                int column = goodsType + 1 + extraColumns;
                int amount = colony.getGoodsCount(goodsType);
                JLabel goodsLabel = new JLabel(String.valueOf(amount), JLabel.TRAILING);
                if (colonyIndex == 0) {
                    goodsLabel.setBorder(border1);
                } else {
                    goodsLabel.setBorder(border2);
                }
                if (colony.getExports(goodsType)) {
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
        if (colonies.get(index).getBuilding(Building.CUSTOM_HOUSE).isBuilt()) {
            button.setText(colonies.get(index).getName() + "*");
        } else {
            button.setText(colonies.get(index).getName());
        }
        button.setOpaque(false);
        button.setForeground(LINK_COLOR);
        button.setHorizontalAlignment(SwingConstants.LEADING);
        button.setAlignmentY(0.8f);
        if (index == 0) {
            button.setBorder(border4);
        } else {
            button.setBorder(border3);
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
