/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.TypeCountMap;
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

    private List<Colony> colonies;

    private final JPanel goodsHeader = new JPanel() {
        @Override
        public String getUIClassID() {
            return "ReportPanelUI";
        }
    };


    /**
     * The constructor that will add the items to this panel.
     *
     * @param parent The parent of this panel.
     */
    public ReportTradePanel(Canvas parent) {
        super(parent, Messages.message("reportTradeAction.name"));
        setSize(getMinimumSize());

        Player player = getMyPlayer();
        colonies = getFreeColClient().getClientOptions()
            .getSortedColonies(player);

        goodsHeader.setBorder(new EmptyBorder(20, 20, 0, 20));
        scrollPane.setColumnHeaderView(goodsHeader);

        for (GoodsType goodsType : getSpecification().getGoodsTypeList()) {
            if (goodsType.isStorable()) {
                storableGoods.add(goodsType);
            }
        }
        Market market = player.getMarket();

        // Display Panel
        reportPanel.removeAll();
        goodsHeader.removeAll();

        String layoutConstraints = "insets 0, gap 0 0";
        String columnConstraints = "[170!, fill][42!, fill]";
        String rowConstraints = "[fill]";

        reportPanel.setLayout(new MigLayout(layoutConstraints, columnConstraints, rowConstraints));
        goodsHeader.setLayout(new MigLayout(layoutConstraints, columnConstraints, rowConstraints));
        goodsHeader.setOpaque(true);

        JLabel emptyLabel = new JLabel();
        emptyLabel.setBorder(FreeColPanel.TOPLEFTCELLBORDER);
        goodsHeader.add(emptyLabel, "cell 0 0");

        reportPanel.add(createLeftLabel("report.trade.unitsSold"), "cell 0 0");
        reportPanel.add(createLeftLabel("report.trade.beforeTaxes"), "cell 0 1");
        reportPanel.add(createLeftLabel("report.trade.afterTaxes"), "cell 0 2");
        reportPanel.add(createLeftLabel("report.trade.cargoUnits"), "cell 0 3");
        reportPanel.add(createLeftLabel("report.trade.totalUnits"), "cell 0 4");
        reportPanel.add(createLeftLabel("report.trade.totalDelta"), "cell 0 5");

        TypeCountMap<GoodsType> totalUnits = new TypeCountMap<GoodsType>();
        TypeCountMap<GoodsType> deltaUnits = new TypeCountMap<GoodsType>();
        TypeCountMap<GoodsType> cargoUnits = new TypeCountMap<GoodsType>();

        for (Iterator<Unit> iterator = player.getUnitIterator(); iterator.hasNext();) {
            Unit unit = iterator.next();
            if (unit.isCarrier()) {
                for (Goods goods : unit.getGoodsContainer().getCompactGoods()) {
                    cargoUnits.incrementCount(goods.getType(), goods.getAmount());
                    totalUnits.incrementCount(goods.getType(), goods.getAmount());
                }
            }
        }

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

            reportPanel.add(createNumberLabel(sales), "cell " + column + " 0");
            reportPanel.add(createNumberLabel(beforeTaxes), "cell " + column + " 1");
            reportPanel.add(createNumberLabel(afterTaxes), "cell " + column + " 2");
            reportPanel.add(createNumberLabel(cargoUnits.getCount(goodsType)), "cell " + column + " 3");

        }

        int row = 6;

        for (int colonyIndex = 0; colonyIndex < colonies.size(); colonyIndex++) {
            Colony colony = colonies.get(colonyIndex);
            for (GoodsType goodsType : getSpecification().getGoodsTypeList()) {
                deltaUnits.incrementCount(goodsType, colony.getNetProductionOf(goodsType));
            }
            for (Goods goods : colony.getGoodsContainer().getCompactGoods()) {
                totalUnits.incrementCount(goods.getType(), goods.getAmount());
            }
            JButton colonyButton = createColonyButton(colony, colonyIndex);
            reportPanel.add(colonyButton, "cell 0 " + row + " 1 2");
            column = 0;
            for (GoodsType goodsType : storableGoods) {
                column++;
                int amount = colony.getGoodsCount(goodsType);
                JLabel goodsLabel = new JLabel(String.valueOf(amount), JLabel.TRAILING);
                goodsLabel.setBorder(colonyIndex == 0 ? FreeColPanel.TOPCELLBORDER : FreeColPanel.CELLBORDER);
                if (colony.getExportData(goodsType).isExported()) {
                    goodsLabel.setText("*" + String.valueOf(amount));
                }
                reportPanel.add(goodsLabel, "cell " + column + " " + row);

                int production = colony.getNetProductionOf(goodsType);

                JLabel productionLabel = createNumberLabel(production, true);

                StringBuffer toolTip = new StringBuffer();
                for (StringTemplate warning : colony.getWarnings(goodsType, amount, production)) {
                    if (toolTip.length() > 0) {
                        toolTip.append(" - ");
                    }
                    toolTip.append(Messages.message(warning));
                    productionLabel.setForeground(Color.MAGENTA);
                    productionLabel.setToolTipText(toolTip.toString());
                }

                reportPanel.add(productionLabel, "cell " + column + " " + (row + 1));
            }
            row += 2;
        }

        row++;
        reportPanel.add(new JLabel(Messages.message("report.trade.hasCustomHouse")),
                        "cell 0 " + row + ", span");

        column = 0;
        for (GoodsType goodsType : storableGoods) {
            column++;
            reportPanel.add(createNumberLabel(totalUnits.getCount(goodsType)),
                            "cell " + column + " 4");
            reportPanel.add(createNumberLabel(deltaUnits.getCount(goodsType), true),
                            "cell " + column + " 5, wrap 20");
        }
    }

    private JLabel createLeftLabel(String key) {
        JLabel result = new JLabel(Messages.message(key), JLabel.TRAILING);
        result.setBorder(FreeColPanel.LEFTCELLBORDER);
        return result;
    }

    private JLabel createNumberLabel(int value) {
        return createNumberLabel(value, false);
    }

    private JLabel createNumberLabel(int value, boolean alwaysAddSign) {
        JLabel result = new JLabel(String.valueOf(value), JLabel.TRAILING);
        result.setBorder(FreeColPanel.CELLBORDER);
        if (value < 0) {
            result.setForeground(Color.RED);
        } else if (alwaysAddSign && value > 0) {
            result.setText("+" + value);
        }
        return result;
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
