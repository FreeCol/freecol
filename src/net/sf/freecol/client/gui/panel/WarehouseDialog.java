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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ExportData;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;

import net.miginfocom.swing.MigLayout;

/**
 * Asks the user if he's sure he wants to quit.
 */
public final class WarehouseDialog extends FreeColDialog<Boolean> {

    private static final Logger logger = Logger.getLogger(WarehouseDialog.class.getName());

    private final JPanel warehouseDialog;


    /**
     * The constructor that will add the items to this panel.
     *
     * @param parent The parent of this panel.
     */
    public WarehouseDialog(Canvas parent, Colony colony) {
        super(parent.getFreeColClient(), parent);

        warehouseDialog = new JPanel(new MigLayout("wrap 4"));
        warehouseDialog.setOpaque(false);

        JScrollPane scrollPane = new JScrollPane(warehouseDialog,
                                                 JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                 JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement( 16 );
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);

        setCancelComponent(cancelButton);

        setLayout(new MigLayout("fill, wrap 1", "", ""));

        add(getDefaultHeader(Messages.message("warehouseDialog.name")), "align center");
        add(scrollPane, "grow");
        add(okButton, "newline 20, split 2, tag ok");
        add(cancelButton, "tag cancel");

        for (GoodsType goodsType : getSpecification().getGoodsTypeList()) {
            if (goodsType.isStorable()) {
                warehouseDialog.add(new WarehouseGoodsPanel(colony, goodsType));
            }
        }

        setSize(getPreferredSize());
    }

    /**
     * This function analyses an event and calls the right methods to take care
     * of the user's requests.
     *
     * @param event The incoming ActionEvent.
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        if (OK.equals(command)) {
            setResponse(Boolean.TRUE);
            for (Component c : warehouseDialog.getComponents()) {
                if (c instanceof WarehouseGoodsPanel) {
                    ((WarehouseGoodsPanel) c).saveSettings();
                }
            }
        } else if (CANCEL.equals(command)) {
            getCanvas().remove(this);
            setResponse(Boolean.FALSE);
        } else {
            logger.warning("Invalid ActionCommand: " + command);
        }
    }


    public class WarehouseGoodsPanel extends JPanel {

        private final Colony colony;

        private final GoodsType goodsType;

        private final JCheckBox export;

        private final JSpinner lowLevel;

        private final JSpinner highLevel;

        private final JSpinner exportLevel;


        public WarehouseGoodsPanel(Colony colony, GoodsType goodsType) {

            this.colony = colony;
            this.goodsType = goodsType;

            setLayout(new MigLayout("wrap 2", "", ""));
            setOpaque(false);
            String goodsName = Messages.message(goodsType.getNameKey());
            setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(goodsName),
                    BorderFactory.createEmptyBorder(6, 6, 6, 6)));

            ExportData exportData = colony.getExportData(goodsType);

            // goods label
            Goods goods = new Goods(colony.getGame(), colony, goodsType,
                                    colony.getGoodsContainer().getGoodsCount(goodsType));
            GoodsLabel goodsLabel = new GoodsLabel(goods, getCanvas());
            goodsLabel.setHorizontalAlignment(JLabel.LEADING);
            add(goodsLabel, "span 1 2");

            // low level settings
            SpinnerNumberModel lowLevelModel = new SpinnerNumberModel(exportData.getLowLevel(), 0, 100, 1);
            lowLevel = new JSpinner(lowLevelModel);
            lowLevel.setToolTipText(Messages.message("warehouseDialog.lowLevel.shortDescription"));
            add(lowLevel);

            // high level settings
            SpinnerNumberModel highLevelModel = new SpinnerNumberModel(exportData.getHighLevel(), 0, 100, 1);
            highLevel = new JSpinner(highLevelModel);
            highLevel.setToolTipText(Messages.message("warehouseDialog.highLevel.shortDescription"));
            add(highLevel);

            // export checkbox
            export = new JCheckBox(Messages.message("warehouseDialog.export"), exportData.isExported());
            export.setToolTipText(Messages.message("warehouseDialog.export.shortDescription"));
            if (!colony.hasAbility(Ability.EXPORT)) {
                export.setEnabled(false);
            }
            add(export);

            // export level settings
            SpinnerNumberModel exportLevelModel = new SpinnerNumberModel(exportData.getExportLevel(), 0, colony
                    .getWarehouseCapacity(), 1);
            exportLevel = new JSpinner(exportLevelModel);
            exportLevel.setToolTipText(Messages.message("warehouseDialog.exportLevel.shortDescription"));
            add(exportLevel);

            setSize(getPreferredSize());
        }

        public void saveSettings() {
            int lowLevelValue = ((SpinnerNumberModel) lowLevel.getModel()).getNumber().intValue();
            int highLevelValue = ((SpinnerNumberModel) highLevel.getModel()).getNumber().intValue();
            int exportLevelValue = ((SpinnerNumberModel) exportLevel.getModel()).getNumber().intValue();
            ExportData exportData = colony.getExportData(goodsType);
            boolean changed = (export.isSelected() != exportData.isExported())
                || (lowLevelValue != exportData.getLowLevel())
                || (highLevelValue != exportData.getHighLevel())
                || (exportLevelValue != exportData.getExportLevel());

            exportData.setExported(export.isSelected());
            exportData.setLowLevel(lowLevelValue);
            exportData.setHighLevel(highLevelValue);
            exportData.setExportLevel(exportLevelValue);
            if (changed) {
                getController().setGoodsLevels(colony, goodsType);
            }
        }

        @Override
        public String getUIClassID() {
            return "WarehouseGoodsPanelUI";
        }
    }
}
