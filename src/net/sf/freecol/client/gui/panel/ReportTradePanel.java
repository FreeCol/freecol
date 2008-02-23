/**
 *  Copyright (C) 2002-2007  The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.client.gui.panel;

import cz.autel.dmi.HIGLayout;
import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * This panel displays the Trade Report.
 */
public final class ReportTradePanel extends ReportPanel implements ActionListener {


    /**
     * How many colums are defined per label.
     */
    private static final int columnsPerLabel = 1;

    /**
     * How many additional rows are defined.
     */
    private static final int extraRows = 7; // labels and separators
    private static final int extraBottomRows = 2;

    /**
     * How many additional columns are defined.
     */
    private static final int extraColumns = 1; // labels

    /**
     * How many columns are defined all together.
     */
    private static final int columns = columnsPerLabel * FreeCol.getSpecification().numberOfGoodsTypes() + extraColumns;

    /**
     * How wide the margins should be.
     */
    private static final int marginWidth = 12;

    private final JLabel salesLabel;

    private final JLabel beforeTaxesLabel;

    private final JLabel afterTaxesLabel;

    private List<Colony> colonies;

    private final JPanel goodsHeader = new JPanel();

    /**
     * The constructor that will add the items to this panel.
     *
     * @param parent The parent of this panel.
     */
    public ReportTradePanel(Canvas parent) {
        super(parent, Messages.message("menuBar.report.trade"));
        setSize(getMinimumSize());

        salesLabel = new JLabel(Messages.message("report.trade.unitsSold"), JLabel.TRAILING);
        salesLabel.setBorder(FreeColPanel.LEFTCELLBORDER);
        beforeTaxesLabel = new JLabel(Messages.message("report.trade.beforeTaxes"), JLabel.TRAILING);
        beforeTaxesLabel.setBorder(FreeColPanel.LEFTCELLBORDER);
        afterTaxesLabel = new JLabel(Messages.message("report.trade.afterTaxes"), JLabel.TRAILING);
        afterTaxesLabel.setBorder(FreeColPanel.LEFTCELLBORDER);

        goodsHeader.setBorder(new EmptyBorder(20, 20, 0, 20));
        scrollPane.setColumnHeaderView(goodsHeader);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(900, 750);
    }

    @Override
    protected Border createBorder() {
        return new EmptyBorder(0, 20, 20, 20);
    }

    /**
     * Prepares this panel to be displayed.
     */
    public void initialize() {
        Player player = getCanvas().getClient().getMyPlayer();
        Market market = player.getMarket();

        // Display Panel
        reportPanel.removeAll();
        goodsHeader.removeAll();

        colonies = player.getColonies();
        Collections.sort(colonies, getCanvas().getClient().getClientOptions().getColonyComparator());

        int[] widths = new int[columns];

        //align the labels with the goodsHeader
        widths[0] = 170;
        for (int i = 1; i < columns; i++) {
            widths[i] = 40;
        }

        int[] heights = new int[colonies.size() * 2 + extraRows + extraBottomRows];

        int labelColumn = 1;

        heights[extraRows - 1] = marginWidth; // separator
        heights[heights.length - 2] = marginWidth;

        reportPanel.setLayout(new HIGLayout(widths, heights));
        goodsHeader.setLayout(new HIGLayout(widths, new int[1]));

        JLabel emptyLabel = new JLabel();
        emptyLabel.setBorder(FreeColPanel.TOPLEFTCELLBORDER);
        goodsHeader.add(emptyLabel, higConst.rc(1, labelColumn));

        reportPanel.add(salesLabel, higConst.rc(1, labelColumn));
        reportPanel.add(beforeTaxesLabel, higConst.rc(2, labelColumn));
        reportPanel.add(afterTaxesLabel, higConst.rc(3, labelColumn));

        JLabel cargoUnitsLabel = new JLabel(Messages.message("report.trade.cargoUnits"), JLabel.TRAILING);
        cargoUnitsLabel.setBorder(FreeColPanel.LEFTCELLBORDER);
        JLabel totalUnitsLabel = new JLabel(Messages.message("report.trade.totalUnits"), JLabel.TRAILING);
        totalUnitsLabel.setBorder(FreeColPanel.LEFTCELLBORDER);
        JLabel totalDeltaLabel = new JLabel(Messages.message("report.trade.totalDelta"), JLabel.TRAILING);
        totalDeltaLabel.setBorder(FreeColPanel.LEFTCELLBORDER);

        reportPanel.add(cargoUnitsLabel, higConst.rc(4, labelColumn));
        reportPanel.add(totalUnitsLabel, higConst.rc(5, labelColumn));
        reportPanel.add(totalDeltaLabel, higConst.rc(6, labelColumn));

        JLabel currentLabel;
        int column = extraColumns;
        for (GoodsType goodsType : FreeCol.getSpecification().getGoodsTypeList()) {
            if (!goodsType.isStorable()) {
                continue;
            }
            column++;
            int sales = player.getSales(goodsType);
            int beforeTaxes = player.getIncomeBeforeTaxes(goodsType);
            int afterTaxes = player.getIncomeAfterTaxes(goodsType);
            MarketLabel marketLabel = new MarketLabel(goodsType, market, getCanvas());
            marketLabel.setBorder(FreeColPanel.TOPCELLBORDER);
            marketLabel.setVerticalTextPosition(JLabel.BOTTOM);
            marketLabel.setHorizontalTextPosition(JLabel.CENTER);

            goodsHeader.add(marketLabel, higConst.rc(1, column));

            currentLabel = new JLabel(String.valueOf(sales), JLabel.TRAILING);
            currentLabel.setBorder(FreeColPanel.CELLBORDER);
            if (sales < 0) {
                currentLabel.setForeground(Color.RED);
            }
            reportPanel.add(currentLabel, higConst.rc(1, column));

            currentLabel = new JLabel(String.valueOf(beforeTaxes), JLabel.TRAILING);
            currentLabel.setBorder(FreeColPanel.CELLBORDER);
            if (beforeTaxes < 0) {
                currentLabel.setForeground(Color.RED);
            }
            reportPanel.add(currentLabel, higConst.rc(2, column));

            currentLabel = new JLabel(String.valueOf(afterTaxes), JLabel.TRAILING);
            currentLabel.setBorder(FreeColPanel.CELLBORDER);
            if (afterTaxes < 0) {
                currentLabel.setForeground(Color.RED);
            }
            reportPanel.add(currentLabel, higConst.rc(3, column));

            int cargoUnits = 0;
            for (Iterator<Unit> iterator = player.getUnitIterator(); iterator.hasNext();) {
                Unit unit = iterator.next();
                if (unit.isCarrier()) {
                    cargoUnits += unit.getGoodsContainer().getGoodsCount(goodsType);
                }
            }

            int totalUnits = cargoUnits;
            int deltaUnits = 0;
            for (Colony colony : colonies) {
                deltaUnits += colony.getProductionNetOf(goodsType);
                totalUnits += colony.getGoodsCount(goodsType);
            }

            JLabel cargoUnitsAmount = new JLabel(String.valueOf(cargoUnits), JLabel.TRAILING);
            cargoUnitsAmount.setBorder(FreeColPanel.CELLBORDER);
            reportPanel.add(cargoUnitsAmount, higConst.rc(4, column));

            JLabel totalUnitsAmount = new JLabel(String.valueOf(totalUnits), JLabel.TRAILING);
            totalUnitsAmount.setBorder(FreeColPanel.CELLBORDER);
            reportPanel.add(totalUnitsAmount, higConst.rc(5, column));

            JLabel deltaUnitsAmount = new JLabel(String.valueOf(deltaUnits), JLabel.TRAILING);
            if (deltaUnits < 0) {
                deltaUnitsAmount.setForeground(Color.RED);
            } else if (deltaUnits > 0) {
                deltaUnitsAmount.setText("+" + deltaUnits);
            }
            deltaUnitsAmount.setBorder(FreeColPanel.CELLBORDER);
            reportPanel.add(deltaUnitsAmount, higConst.rc(6, column));
        }

        int row = extraRows + 1;

        for (int colonyIndex = 0; colonyIndex < colonies.size(); colonyIndex++) {
            Colony colony = colonies.get(colonyIndex);
            JButton colonyButton = createColonyButton(colonyIndex);
            reportPanel.add(colonyButton, higConst.rcwh(row, labelColumn, 1, 2));
            column = extraColumns;
            for (GoodsType goodsType : FreeCol.getSpecification().getGoodsTypeList()) {
                if (!goodsType.isStorable()) {
                    continue;
                }
                column++;
                int amount = colony.getGoodsCount(goodsType);
                JLabel goodsLabel = new JLabel(String.valueOf(amount), JLabel.TRAILING);
                if (colonyIndex == 0) {
                    goodsLabel.setBorder(FreeColPanel.TOPCELLBORDER);
                } else {
                    goodsLabel.setBorder(FreeColPanel.CELLBORDER);
                }
                if (colony.getExportData(goodsType).isExported()) {
                    goodsLabel.setText("*" + String.valueOf(amount));
                }
                reportPanel.add(goodsLabel, higConst.rc(row, column));

                int production = colony.getProductionNetOf(goodsType);

                JLabel productionLabel = new JLabel(String.valueOf(production), JLabel.TRAILING);
                if (production < 0) {
                    productionLabel.setForeground(Color.RED);
                } else if (production > 0) {
                    productionLabel.setText("+" + production);
                }

                StringBuffer toolTip = new StringBuffer();
                for (String warning : colony.getWarnings(goodsType, amount, production)) {
                    if (toolTip.length() > 0) {
                        toolTip.append(" - ");
                    }
                    toolTip.append(warning);
                    productionLabel.setForeground(Color.MAGENTA);
                    productionLabel.setToolTipText(toolTip.toString());
                }

                productionLabel.setBorder(FreeColPanel.CELLBORDER);
                reportPanel.add(productionLabel, higConst.rc(row + 1, column));
            }
            row += 2;
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
