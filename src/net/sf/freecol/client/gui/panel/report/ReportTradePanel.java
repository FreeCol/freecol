/**
 *  Copyright (C) 2002-2021   The FreeCol Team
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

package net.sf.freecol.client.gui.panel.report;

import java.awt.Color;
import java.util.Collection;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.label.GoodsLabel;
import net.sf.freecol.client.gui.label.MarketLabel;
import net.sf.freecol.client.gui.panel.*;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ExportData;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.TypeCountMap;
import net.sf.freecol.common.model.Unit;
import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * This panel displays the Trade Report.
 */
public final class ReportTradePanel extends ReportPanel {

    private final List<Colony> colonies;


    /**
     * The constructor that will add the items to this panel.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    public ReportTradePanel(FreeColClient freeColClient) {
        super(freeColClient, "reportTradeAction");

        final Player player = getMyPlayer();
        Color warnColor = ImageLibrary.getColor("color.report.trade.warn");

        this.colonies = player.getColonyList();

        JPanel goodsHeader = new MigPanel("ReportPanelUI");
        goodsHeader.setBorder(new EmptyBorder(20, 20, 0, 20));
        scrollPane.setColumnHeaderView(goodsHeader);

        final Specification spec = getSpecification();
        List<GoodsType> storableGoods = spec.getStorableGoodsTypeList();
        Market market = player.getMarket();

        // Display Panel
        reportPanel.removeAll();
        goodsHeader.removeAll();

        String layoutConstraints = "insets 0, gap 0 0";
        String columnConstraints = "[25%!, fill]["
            + (int)Math.round(ImageLibrary.ICON_SIZE.width * 1.25)
            + "!, fill]";
        String rowConstraints = "[fill]";

        reportPanel.setLayout(new MigLayout(layoutConstraints,
                                            columnConstraints, rowConstraints));
        goodsHeader.setLayout(new MigLayout(layoutConstraints,
                                            columnConstraints, rowConstraints));
        goodsHeader.setOpaque(true);

        JLabel emptyLabel = new JLabel();
        emptyLabel.setBorder(Utility.getTopLeftCellBorder());
        goodsHeader.add(emptyLabel, "cell 0 0");
        
        /**
         * Total Units Sold by Player
         */
        JLabel jl = createLeftLabel("report.trade.unitsSold");
        jl.setBorder(Utility.getTopLeftCellBorder());
        reportPanel.add(jl, "cell 0 0");
        reportPanel.add(createLeftLabel("report.trade.beforeTaxes"), "cell 0 1");
        reportPanel.add(createLeftLabel("report.trade.afterTaxes"), "cell 0 2");
        reportPanel.add(createLeftLabel("report.trade.cargoUnits"), "cell 0 3");
        reportPanel.add(createLeftLabel("report.trade.totalUnits"), "cell 0 4");
        reportPanel.add(createLeftLabel("report.trade.totalDelta"), "cell 0 5");

        TypeCountMap<GoodsType> totalUnits = new TypeCountMap<>();
        TypeCountMap<GoodsType> deltaUnits = new TypeCountMap<>();
        TypeCountMap<GoodsType> cargoUnits = new TypeCountMap<>();

        for (Unit unit : transform(player.getUnits(), Unit::isCarrier)) {
            for (Goods goods : unit.getCompactGoodsList()) {
                cargoUnits.incrementCount(goods.getType(), goods.getAmount());
                totalUnits.incrementCount(goods.getType(), goods.getAmount());
            }
        }

        int column = 0;
        for (GoodsType goodsType : storableGoods) {
            column++;
            int sales = player.getSales(goodsType);
            int beforeTaxes = player.getIncomeBeforeTaxes(goodsType);
            int afterTaxes = player.getIncomeAfterTaxes(goodsType);
            goodsHeader.add(new MarketLabel(freeColClient, goodsType, market)
                .addBorder());

            jl = createNumberLabel(sales);
            jl.setBorder(Utility.getTopCellBorder());
            reportPanel.add(jl, "cell " + column + " 0");
            reportPanel.add(createNumberLabel(beforeTaxes),
                            "cell " + column + " 1");
            reportPanel.add(createNumberLabel(afterTaxes),
                            "cell " + column + " 2");
            reportPanel.add(createNumberLabel(cargoUnits.getCount(goodsType)),
                            "cell " + column + " 3");
        }

        int row = 6;
        boolean first = true;
        for (Colony colony : colonies) {
            for (GoodsType goodsType : getSpecification().getGoodsTypeList()) {
                deltaUnits.incrementCount(goodsType, colony.getNetProductionOf(goodsType));
            }
            for (Goods goods : colony.getCompactGoodsList()) {
                totalUnits.incrementCount(goods.getType(), goods.getAmount());
            }
            JButton colonyButton = createColonyButton(colony);
            if (colony.hasAbility(Ability.EXPORT)) {
                colonyButton.setText(colonyButton.getText() + "*");
            }
            colonyButton.setBorder((first) ? Utility.getTopLeftCellBorder()
                : Utility.getLeftCellBorder());
            reportPanel.add(colonyButton, "cell 0 " + row + " 1 2");

            column = 0;
            for (GoodsType goodsType : storableGoods) {
                column++;
                int amount = colony.getGoodsCount(goodsType);
                JLabel goodsLabel = new JLabel(String.valueOf(amount),
                                               JLabel.TRAILING);
                goodsLabel.setBorder((first) ? Utility.getTopCellBorder()
                    : Utility.getCellBorder());
                goodsLabel.setForeground(ImageLibrary.getGoodsColor(goodsType, amount,
                                                             colony));
                ExportData ed = colony.getExportData(goodsType);
                if (ed.getExported()) {
                    goodsLabel.setToolTipText(Messages.message(StringTemplate
                            .template("report.trade.export")
                            .addNamed("%goods%", goodsType)
                            .addAmount("%amount%", ed.getExportLevel())));
                }
                reportPanel.add(goodsLabel, "cell " + column + " " + row);

                int production = colony.getNetProductionOf(goodsType);
                JLabel productionLabel = createNumberLabel(production, true);
                productionLabel.setForeground(ImageLibrary.getGoodsColor(goodsType,
                        production, colony));
                Collection<StringTemplate> warnings
                    = colony.getProductionWarnings(goodsType);
                if (!warnings.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (StringTemplate warning : warnings) {
                        sb.append(Messages.message(warning))
                            .append(' ');
                    }
                    sb.setLength(sb.length()-1);
                    productionLabel.setToolTipText(sb.toString());
                    productionLabel.setForeground(warnColor);
                }
                reportPanel.add(productionLabel,
                                "cell " + column + " " + (row + 1));
            }
            row += 2;
            first = false;
        }

        row++;
        reportPanel.add(Utility.localizedLabel("report.trade.hasCustomHouse"),
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
        JLabel result = Utility.localizedLabel(key);
        result.setBorder(Utility.getLeftCellBorder());
        return result;
    }

    private JLabel createNumberLabel(int value) {
        return createNumberLabel(value, false);
    }

    private JLabel createNumberLabel(int value, boolean alwaysAddSign) {
        JLabel result = new JLabel(String.valueOf(value), JLabel.TRAILING);
        result.setBorder(Utility.getCellBorder());
        if (value < 0) {
            result.setForeground(Color.RED);
        } else if (alwaysAddSign && value > 0) {
            result.setText("+" + value);
        }
        return result;
    }
}
