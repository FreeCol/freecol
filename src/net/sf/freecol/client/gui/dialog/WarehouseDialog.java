/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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

package net.sf.freecol.client.gui.dialog;

import java.awt.Color;
import java.awt.Component;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.label.GoodsLabel;
import net.sf.freecol.client.gui.panel.*;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ExportData;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.option.GameOptions;


/**
 * A dialog to display a colony warehouse.
 */
public final class WarehouseDialog extends FreeColConfirmDialog {

    private static final Logger logger = Logger.getLogger(WarehouseDialog.class.getName());

    private JPanel warehousePanel;


    /**
     * Creates a dialog to display the warehouse.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param frame The owner frame.
     * @param colony The {@code Colony} containing the warehouse.
     */
    public WarehouseDialog(FreeColClient freeColClient, JFrame frame,
            Colony colony) {
        super(freeColClient, frame);

        warehousePanel = new MigPanel(new MigLayout("wrap 4"));
        warehousePanel.setOpaque(false);
        for (GoodsType type : freeColClient.getGame().getSpecification()
                 .getStorableGoodsTypeList()) {
            warehousePanel.add(new WarehouseGoodsPanel(freeColClient,
                                                       colony, type));
        }

        JScrollPane scrollPane = new JScrollPane(warehousePanel,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);

        JPanel panel = new MigPanel(new MigLayout("fill, wrap 1", "", ""));
        panel.add(Utility.localizedHeader(Messages.nameKey("warehouseDialog"), false),
                  "align center");
        panel.add(scrollPane, "grow");
        panel.setSize(panel.getPreferredSize());

        ImageIcon icon = new ImageIcon(
            getImageLibrary().getSmallSettlementImage(colony));
        initializeConfirmDialog(frame, true, panel, icon, "ok", "cancel");
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean getResponse() {
        Boolean result = super.getResponse();
        if (result) {
            for (Component c : warehousePanel.getComponents()) {
                if (c instanceof WarehouseGoodsPanel) {
                    ((WarehouseGoodsPanel)c).saveSettings();
                }
            }
        }
        warehousePanel = null;
        return result;
    }


    private class WarehouseGoodsPanel extends MigPanel {

        private final Colony colony;

        private final GoodsType goodsType;

        private final JCheckBox export;

        private final JSpinner lowLevel;

        private final JSpinner highLevel;

        private final JSpinner importLevel;
        
        private final JSpinner exportLevel;


        public WarehouseGoodsPanel(FreeColClient freeColClient, Colony colony,
                                   GoodsType goodsType) {
            super("WarehouseGoodsPanelUI", new MigLayout("wrap 2", "", ""));

            final boolean enhancedTradeRoutes = colony.getSpecification()
                .getBoolean(GameOptions.ENHANCED_TRADE_ROUTES);
            this.colony = colony;
            this.goodsType = goodsType;
            final int capacity = colony.getWarehouseCapacity();
            final int maxCapacity = 300; // TODO: magic number
            
            setOpaque(false);
            setBorder(Utility.localizedBorder(goodsType, new Color(0)));
            Utility.padBorder(this, 6,6,6,6);

            ExportData exportData = colony.getExportData(goodsType);

            // goods label
            Goods goods = new Goods(colony.getGame(), colony, goodsType,
                                    colony.getGoodsCount(goodsType));
            GoodsLabel goodsLabel = new GoodsLabel(freeColClient.getGUI(),
                                                   goods);
            goodsLabel.setHorizontalAlignment(JLabel.LEADING);
            add(goodsLabel, "span 1 2");

            // low level settings
            String str;
            SpinnerNumberModel lowLevelModel
                = new SpinnerNumberModel(exportData.getLowLevel(), 0, 100, 1);
            lowLevel = new JSpinner(lowLevelModel);
            Utility.localizeToolTip(lowLevel,
                "warehouseDialog.lowLevel.shortDescription");
            add(lowLevel);

            // high level settings
            SpinnerNumberModel highLevelModel
                = new SpinnerNumberModel(exportData.getHighLevel(), 0, 100, 1);
            highLevel = new JSpinner(highLevelModel);
            Utility.localizeToolTip(highLevel,
                "warehouseDialog.highLevel.shortDescription");
            add(highLevel);

            if (enhancedTradeRoutes) { // import level settings
                int importInit = exportData.getEffectiveImportLevel(capacity);
                SpinnerNumberModel importLevelModel
                    = new SpinnerNumberModel(importInit, 0,
                        (goodsType.limitIgnored()) ? maxCapacity : capacity, 1);
                importLevel = new JSpinner(importLevelModel);
                Utility.localizeToolTip(importLevel,
                    "warehouseDialog.importLevel.shortDescription");
                add(importLevel);
            } else {
                importLevel = null;
            }

            // export checkbox
            export = new JCheckBox(Messages.message("warehouseDialog.export"),
                                   exportData.getExported());
            Utility.localizeToolTip(export,
                "warehouseDialog.export.shortDescription");
            if (!colony.hasAbility(Ability.EXPORT)) {
                export.setEnabled(false);
            }
            add(export);

            // export level settings
            SpinnerNumberModel exportLevelModel
                = new SpinnerNumberModel(exportData.getExportLevel(), 0,
                    (goodsType.limitIgnored()) ? maxCapacity : capacity, 1);
            exportLevel = new JSpinner(exportLevelModel);
            Utility.localizeToolTip(exportLevel,
                "warehouseDialog.exportLevel.shortDescription");
            add(exportLevel);

            setSize(getPreferredSize());
        }

        public void saveSettings() {
            int lowLevelValue = ((SpinnerNumberModel)lowLevel.getModel())
                .getNumber().intValue();
            int highLevelValue = ((SpinnerNumberModel)highLevel.getModel())
                .getNumber().intValue();
            int importLevelValue = (importLevel == null) ? -1
                : ((SpinnerNumberModel)importLevel.getModel())
                    .getNumber().intValue();
            int exportLevelValue = ((SpinnerNumberModel)exportLevel.getModel())
                .getNumber().intValue();
            ExportData exportData = colony.getExportData(goodsType);
            int importValue = exportData.getEffectiveImportLevel(colony.getWarehouseCapacity());
            boolean changed = (export.isSelected() != exportData.getExported())
                || (lowLevelValue != exportData.getLowLevel())
                || (highLevelValue != exportData.getHighLevel())
                || (importLevel != null && importLevelValue != importValue)
                || (exportLevelValue != exportData.getExportLevel());
            exportData.setExported(export.isSelected());
            exportData.setLowLevel(lowLevelValue);
            exportData.setHighLevel(highLevelValue);
            exportData.setImportLevel(importLevelValue);
            exportData.setExportLevel(exportLevelValue);
            if (changed) {
                freeColClient.getInGameController()
                    .setGoodsLevels(colony, goodsType);
            }
        }
    }
}
