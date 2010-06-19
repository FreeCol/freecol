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

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Unit;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.miginfocom.swing.MigLayout;


/**
 * This panel displays the Trade Report.
 */
public final class ReportTradePanel extends ReportPanel {

    /**
     * Storable goods types.
     */
    private List<GoodsType> storableGoods = new ArrayList<GoodsType>();

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
        super(parent, Messages.message("reportTradeAction.name"));
        setSize(getMinimumSize());

        salesLabel = new JLabel(Messages.message("report.trade.unitsSold"), JLabel.TRAILING);
        salesLabel.setBorder(FreeColPanel.LEFTCELLBORDER);
        beforeTaxesLabel = new JLabel(Messages.message("report.trade.beforeTaxes"), JLabel.TRAILING);
        beforeTaxesLabel.setBorder(FreeColPanel.LEFTCELLBORDER);
        afterTaxesLabel = new JLabel(Messages.message("report.trade.afterTaxes"), JLabel.TRAILING);
        afterTaxesLabel.setBorder(FreeColPanel.LEFTCELLBORDER);

        goodsHeader.setBorder(new EmptyBorder(20, 20, 0, 20));
        scrollPane.setColumnHeaderView(goodsHeader);

        for (GoodsType goodsType : getSpecification().getGoodsTypeList()) {
            if (goodsType.isStorable()) {
                storableGoods.add(goodsType);
            }
        }
        Player player = getMyPlayer();
        Market market = player.getMarket();

        // Display Panel
        reportPanel.removeAll();
        goodsHeader.removeAll();

        colonies = player.getColonies();
        Collections.sort(colonies, getClient().getClientOptions().getColonyComparator());

        String layoutConstraints = "insets 0, gap 0 0";
        String columnConstraints = "[170!, fill][42!, fill]";
        String rowConstraints = "[fill]";

        reportPanel.setLayout(new MigLayout(layoutConstraints, columnConstraints, rowConstraints));
        goodsHeader.setLayout(new MigLayout(layoutConstraints, columnConstraints, rowConstraints));

        JLabel emptyLabel = new JLabel();
        emptyLabel.setBorder(FreeColPanel.TOPLEFTCELLBORDER);
        goodsHeader.add(emptyLabel, "cell 0 0");

        reportPanel.add(salesLabel, "cell 0 0");
        reportPanel.add(beforeTaxesLabel, "cell 0 1");
        reportPanel.add(afterTaxesLabel, "cell 0 2");

        JLabel cargoUnitsLabel = new JLabel(Messages.message("report.trade.cargoUnits"), JLabel.TRAILING);
        cargoUnitsLabel.setBorder(FreeColPanel.LEFTCELLBORDER);
        JLabel totalUnitsLabel = new JLabel(Messages.message("report.trade.totalUnits"), JLabel.TRAILING);
        totalUnitsLabel.setBorder(FreeColPanel.LEFTCELLBORDER);
        JLabel totalDeltaLabel = new JLabel(Messages.message("report.trade.totalDelta"), JLabel.TRAILING);
        totalDeltaLabel.setBorder(FreeColPanel.LEFTCELLBORDER);

        reportPanel.add(cargoUnitsLabel, "cell 0 3");
        reportPanel.add(totalUnitsLabel, "cell 0 4");
        reportPanel.add(totalDeltaLabel, "cell 0 5");

        JLabel currentLabel;
        int column = 0;
        for (GoodsType goodsType : storableGoods) {
            column++;
            int sales = player.getSales(goodsType);
            int beforeTaxes = player.getIncomeBeforeTaxes(goodsType);
            int afterTaxes = player.getIncomeAfterTaxes(goodsType);
            MarketLabel marketLabel = new MarketLabel(goodsType, market, getCanvas());
            marketLabel.setBorder(FreeColPanel.TOPCELLBORDER);
            marketLabel.setVerticalTextPosition(JLabel.BOTTOM);
            marketLabel.setHorizontalTextPosition(JLabel.CENTER);

            goodsHeader.add(marketLabel);

            currentLabel = new JLabel(String.valueOf(sales), JLabel.TRAILING);
            currentLabel.setBorder(FreeColPanel.CELLBORDER);
            if (sales < 0) {
                currentLabel.setForeground(Color.RED);
            }
            reportPanel.add(currentLabel, "cell " + column + " 0");

            currentLabel = new JLabel(String.valueOf(beforeTaxes), JLabel.TRAILING);
            currentLabel.setBorder(FreeColPanel.CELLBORDER);
            if (beforeTaxes < 0) {
                currentLabel.setForeground(Color.RED);
            }
            reportPanel.add(currentLabel, "cell " + column + " 1");

            currentLabel = new JLabel(String.valueOf(afterTaxes), JLabel.TRAILING);
            currentLabel.setBorder(FreeColPanel.CELLBORDER);
            if (afterTaxes < 0) {
                currentLabel.setForeground(Color.RED);
            }
            reportPanel.add(currentLabel, "cell " + column + " 2");

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
            reportPanel.add(cargoUnitsAmount, "cell " + column + " 3");

            JLabel totalUnitsAmount = new JLabel(String.valueOf(totalUnits), JLabel.TRAILING);
            totalUnitsAmount.setBorder(FreeColPanel.CELLBORDER);
            reportPanel.add(totalUnitsAmount, "cell " + column + " 4");

            JLabel deltaUnitsAmount = new JLabel(String.valueOf(deltaUnits), JLabel.TRAILING);
            if (deltaUnits < 0) {
                deltaUnitsAmount.setForeground(Color.RED);
            } else if (deltaUnits > 0) {
                deltaUnitsAmount.setText("+" + deltaUnits);
            }
            deltaUnitsAmount.setBorder(FreeColPanel.CELLBORDER);
            reportPanel.add(deltaUnitsAmount, "cell " + column + " 5, wrap 20");
        }

        int row = 6;

        for (int colonyIndex = 0; colonyIndex < colonies.size(); colonyIndex++) {
            Colony colony = colonies.get(colonyIndex);
            JButton colonyButton = createColonyButton(colony, colonyIndex);
            reportPanel.add(colonyButton, "cell 0 " + row + " 1 2");
            column = 0;
            for (GoodsType goodsType : getSpecification().getGoodsTypeList()) {
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
                reportPanel.add(goodsLabel, "cell " + column + " " + row);

                int production = colony.getProductionNetOf(goodsType);

                JLabel productionLabel = new JLabel(String.valueOf(production), JLabel.TRAILING);
                if (production < 0) {
                    productionLabel.setForeground(Color.RED);
                } else if (production > 0) {
                    productionLabel.setText("+" + production);
                }

                StringBuffer toolTip = new StringBuffer();
                for (StringTemplate warning : colony.getWarnings(goodsType, amount, production)) {
                    if (toolTip.length() > 0) {
                        toolTip.append(" - ");
                    }
                    toolTip.append(Messages.message(warning));
                    productionLabel.setForeground(Color.MAGENTA);
                    productionLabel.setToolTipText(toolTip.toString());
                }

                productionLabel.setBorder(FreeColPanel.CELLBORDER);
                reportPanel.add(productionLabel, "cell " + column + " " + (row + 1));
            }
            row += 2;
        }

        row++;
        reportPanel.add(new JLabel(Messages.message("report.trade.hasCustomHouse")),
                        "cell 0 " + row + ", span");
    }

    private JButton createColonyButton(Colony colony, int index) {

        JButton button = new JButton();
        String name = colony.getName();
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

        button.setActionCommand(colony.getId());
        button.addActionListener(this);
        return button;
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(900, 750);
    }

    @Override
    protected Border createBorder() {
        return new EmptyBorder(0, 20, 20, 20);
    }

}
