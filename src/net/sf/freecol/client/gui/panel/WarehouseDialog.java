
package net.sf.freecol.client.gui.panel;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.GridLayout;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;

import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Goods;

import cz.autel.dmi.HIGLayout;

/**
* Asks the user if he's sure he wants to quit.
*/
public final class WarehouseDialog extends FreeColDialog implements ActionListener {
    private static final Logger logger = Logger.getLogger(WarehouseDialog.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
    private static final int    OK = 0,
                                CANCEL = 1;

    private final Canvas parent;
    private final JButton ok = new JButton(Messages.message("warehousePanel.saveSettings"));
    private final JButton cancel = new JButton(Messages.message("warehousePanel.cancel"));

    private final JPanel warehousePanel;
    private final JPanel buttonPanel;
    
    private static final int[] widths = {0, margin, 0};
    private static final int[] heights = {-5, margin, -1, margin, -3};
    private static final int labelColumn = 1;
    private static final int spinnerColumn = 3;

    /**
    * The constructor that will add the items to this panel.
    * @param parent The parent of this panel.
    */
    public WarehouseDialog(Canvas parent) {
        this.parent = parent;

        warehousePanel = new JPanel(new GridLayout(0, 4, margin, margin));
        warehousePanel.setOpaque(false);

        ok.setActionCommand(String.valueOf(OK));
        cancel.setActionCommand(String.valueOf(CANCEL));
        
        ok.addActionListener(this);
        cancel.addActionListener(this);

        ok.setMnemonic('y');
        cancel.setMnemonic('n');

        FreeColPanel.enterPressesWhenFocused(cancel);
        FreeColPanel.enterPressesWhenFocused(ok);

        setCancelComponent(cancel);

        int[] widths = {0};
        int[] heights = {0, margin, 0, margin, 0};
        setLayout(new HIGLayout(widths, heights));
        buttonPanel = new JPanel();
        buttonPanel.add(ok);
        buttonPanel.add(cancel);
        add(getDefaultHeader(Messages.message("warehousePanel.name")),
            higConst.rc(1, 1));
        add(warehousePanel, higConst.rc(3, 1));
        add(buttonPanel, higConst.rc(5, 1));

    }

    public void initialize(Colony colony) {

        warehousePanel.removeAll();
        for (int goodsType = 0; goodsType < Goods.NUMBER_OF_TYPES; goodsType++) {
            warehousePanel.add(new WarehouseGoodsPanel(colony, goodsType));
        }
        setSize(getPreferredSize());

    }

    
    public void requestFocus() {
        ok.requestFocus();
    }

    
    /**
    * This function analyses an event and calls the right methods to take
    * care of the user's requests.
    * @param event The incoming ActionEvent.
    */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        try {
            switch (Integer.valueOf(command).intValue()) {
            case OK:
                parent.remove(this);
                setResponse(new Boolean(true));
                for (Component c : warehousePanel.getComponents()) {
                    if (c instanceof WarehouseGoodsPanel) {
                        ((WarehouseGoodsPanel) c).saveSettings();
                    }
                }
                break;
            case CANCEL:
                parent.remove(this);
                setResponse(new Boolean(false));
                break;
            default:
                logger.warning("Invalid ActionCommand: invalid number.");
            }
        }
        catch (NumberFormatException e) {
            logger.warning("Invalid Actioncommand: not a number.");
        }
    }

    public class WarehouseGoodsPanel extends JPanel {

        private final Colony colony;
        private final int goodsType;

        private final JCheckBox export;
        private final JSpinner lowLevel;
        private final JSpinner highLevel;
        private final JSpinner exportLevel;

        public WarehouseGoodsPanel(Colony colony, int goodsType) {

            this.colony = colony;
            this.goodsType = goodsType;

            setLayout(new HIGLayout(widths, heights));
            setOpaque(false);
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(Goods.getName(goodsType)),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)));

            // goods label
            Goods goods = new Goods(colony.getGame(), colony, goodsType, 
                                    colony.getGoodsContainer().getGoodsCount(goodsType));
            GoodsLabel goodsLabel = new GoodsLabel(goods, parent);
            goodsLabel.setHorizontalAlignment(JLabel.LEADING);
            add(goodsLabel, higConst.rcwh(1, labelColumn, 1, 3));

            // export checkbox
            export = new JCheckBox(Messages.message("warehousePanel.export"),
                                   colony.getExports(goodsType));
            export.setToolTipText(Messages.message("warehousePanel.export.shortDescription"));
            if (colony.getBuilding(Building.CUSTOM_HOUSE).getLevel() == Building.NOT_BUILT) {
                export.setEnabled(false);
            }
            add(export, higConst.rc(5, labelColumn));

            // low level settings
            SpinnerNumberModel lowLevelModel = new SpinnerNumberModel(colony.getLowLevel()[goodsType],
                                                                      0, 100, 1);
            lowLevel = new JSpinner(lowLevelModel);
            lowLevel.setToolTipText(Messages.message("warehousePanel.lowLevel.shortDescription"));
            add(lowLevel, higConst.rc(1, spinnerColumn));

            // high level settings
            SpinnerNumberModel highLevelModel = new SpinnerNumberModel(colony.getHighLevel()[goodsType],
                                                                       0, 100, 1);
            highLevel = new JSpinner(highLevelModel);
            highLevel.setToolTipText(Messages.message("warehousePanel.highLevel.shortDescription"));
            add(highLevel, higConst.rc(3, spinnerColumn));

            // export level settings
            SpinnerNumberModel exportLevelModel = new SpinnerNumberModel(colony.getExportLevel()[goodsType],
                                                                         0, colony.getWarehouseCapacity(), 1);
            exportLevel = new JSpinner(exportLevelModel);
            exportLevel.setToolTipText(Messages.message("warehousePanel.exportLevel.shortDescription"));
            add(exportLevel, higConst.rc(5, spinnerColumn));

            setSize(getPreferredSize());
        }

        public void saveSettings() {
            if (export.isSelected() != colony.getExports(goodsType)) {
                parent.getClient().getInGameController().setExports(colony, goodsType, export.isSelected());
                colony.setExports(goodsType, export.isSelected());
            }
            colony.getLowLevel()[goodsType] = ((SpinnerNumberModel) lowLevel.getModel()).getNumber().intValue();
            colony.getHighLevel()[goodsType] = ((SpinnerNumberModel) highLevel.getModel()).getNumber().intValue();
            colony.getExportLevel()[goodsType] = ((SpinnerNumberModel) exportLevel.getModel()).getNumber().intValue();
    
        }

    }
}
