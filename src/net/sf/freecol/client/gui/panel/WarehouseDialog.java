/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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

import java.awt.Component;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.panel.MigPanel;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ExportData;
import net.sf.freecol.common.model.ExportData.ExportState;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;


/**
 * A dialog to display a colony warehouse.
 */
public final class WarehouseDialog extends FreeColConfirmDialog {

    private static final Logger logger = Logger.getLogger(WarehouseDialog.class.getName());

    private class WarehouseGoodsPanel extends MigPanel {

        private final Colony colony;

        private final GoodsType goodsType;

        private final JComboBox<ExportState> exportBox;

        private final JSpinner lowLevel;

        private final JSpinner highLevel;

        private final JSpinner exportLevel;


        public WarehouseGoodsPanel(FreeColClient freeColClient, Colony colony,
                                   GoodsType goodsType) {
            super("WarehouseGoodsPanelUI");

            this.colony = colony;
            this.goodsType = goodsType;

            setLayout(new MigLayout("wrap 2", "", ""));
            setOpaque(false);
            setBorder(GUI.localizedBorder(goodsType.getNameKey()));
            GUI.padBorder(this, 6,6,6,6);

            final boolean enhanced = false;
            // Placeholder for:
            //   freeColClient.getClientOptions()
            //       .getBoolean(ClientOptions.ENHANCED_TRADE_ROUTES);
            final ExportData exportData = colony.getExportData(goodsType);

            // goods label
            Goods goods = new Goods(colony.getGame(), colony, goodsType,
                                    colony.getGoodsCount(goodsType));
            GoodsLabel goodsLabel = new GoodsLabel(goods, getGUI());
            goodsLabel.setHorizontalAlignment(JLabel.LEADING);
            add(goodsLabel, "span 1 2");

            // low level settings
            SpinnerNumberModel lowLevelModel
                = new SpinnerNumberModel(exportData.getLowLevel(), 0, 100, 1);
            this.lowLevel = new JSpinner(lowLevelModel);
            GUI.localizeToolTip(this.lowLevel,
                "warehouseDialog.lowLevel.shortDescription");
            add(this.lowLevel);

            // high level settings
            SpinnerNumberModel highLevelModel
                = new SpinnerNumberModel(exportData.getHighLevel(), 0, 100, 1);
            this.highLevel = new JSpinner(highLevelModel);
            GUI.localizeToolTip(this.highLevel,
                "warehouseDialog.highLevel.shortDescription");
            add(this.highLevel);

            // export status
            this.exportBox = new JComboBox<ExportState>();
            GUI.localizeToolTip(this.exportBox,
                "warehouseDialog.export.shortDescription");
            this.exportBox.addItem(ExportState.IMPORT);
            if (colony.hasAbility(Ability.EXPORT)) {
                this.exportBox.addItem(ExportState.EXPORT);
                if (enhanced) {
                    this.exportBox.addItem(ExportState.MAINTAIN);
                }
            }
            this.exportBox.setSelectedItem(exportData.getExportState());
            GUI.localizeToolTip(this.exportBox,
                "warehouseDialog.export.shortDescription");
            add(this.exportBox);

            // export level settings
            SpinnerNumberModel exportLevelModel
                = new SpinnerNumberModel(exportData.getExportLevel(), 0,
                                         colony.getWarehouseCapacity(), 1);
            this.exportLevel = new JSpinner(exportLevelModel);
            this.exportLevel.setToolTipText(Messages
                .getShortDescription("warehouseDialog.exportLevel"));
            add(this.exportLevel);

            setSize(getPreferredSize());
        }

        public void saveSettings() {
            final ExportData exportData
                = this.colony.getExportData(this.goodsType);
            int lowLevelValue = ((SpinnerNumberModel)this.lowLevel.getModel())
                .getNumber().intValue();
            int highLevelValue = ((SpinnerNumberModel)this.highLevel.getModel())
                .getNumber().intValue();
            int exportLevelValue = ((SpinnerNumberModel)this.exportLevel.getModel())
                .getNumber().intValue();
            ExportState state = (ExportState)this.exportBox.getSelectedItem();
            boolean changed = (state != exportData.getExportState())
                || (lowLevelValue != exportData.getLowLevel())
                || (highLevelValue != exportData.getHighLevel())
                || (exportLevelValue != exportData.getExportLevel());

            exportData.setExportState(state);
            exportData.setLowLevel(lowLevelValue);
            exportData.setHighLevel(highLevelValue);
            exportData.setExportLevel(exportLevelValue);
            if (changed) {
                freeColClient.getInGameController()
                    .setGoodsLevels(colony, goodsType);
            }
        }
    }

    private JPanel warehousePanel;


    /**
     * Creates a dialog to display the warehouse.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param colony The <code>Colony</code> containing the warehouse.
     */
    public WarehouseDialog(FreeColClient freeColClient, Colony colony) {
        super(freeColClient);

        warehousePanel = new MigPanel(new MigLayout("wrap 4"));
        warehousePanel.setOpaque(false);
        for (GoodsType type : freeColClient.getGame().getSpecification()
                 .getGoodsTypeList()) {
            if (type.isStorable()) {
                warehousePanel.add(new WarehouseGoodsPanel(freeColClient,
                                                           colony, type));
            }
        }

        JScrollPane scrollPane = new JScrollPane(warehousePanel,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);

        MigPanel panel = new MigPanel(new MigLayout("fill, wrap 1", "", ""));
        String str = Messages.message("warehouseDialog.name");
        panel.add(GUI.getDefaultHeader(str), "align center");
        panel.add(scrollPane, "grow");
        panel.setSize(panel.getPreferredSize());

        ImageIcon icon = getGUI().getImageLibrary().getImageIcon(colony, true);
        initializeConfirmDialog(true, panel, icon, "ok", "cancel");
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
}
