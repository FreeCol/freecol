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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import net.sf.freecol.client.control.InGameController;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Unit;
import cz.autel.dmi.HIGLayout;

/**
 * This panel is used to show information about a tile.
 */
public final class DumpCargoDialog extends FreeColDialog<Boolean> implements ActionListener {

    private static final Logger logger = Logger.getLogger(DumpCargoDialog.class.getName());

    private static final int OK = 0;

    private static final int CANCEL = 1;

    private final Canvas parent;

    private final JLabel header;

    private final JButton okButton;

    private final JButton cancelButton;

    private List<Goods> goodsList;

    private List<JCheckBox> checkBoxes;

    /**
     * The constructor that will add the items to this panel.
     * 
     * @param parent The parent panel.
     */
    public DumpCargoDialog(Canvas parent) {
        this.parent = parent;

        header = new JLabel("", SwingConstants.CENTER);
        header.setFont(mediumHeaderFont);
        header.setText(Messages.message("dumpGoods"));
        header.setAlignmentX(Component.CENTER_ALIGNMENT);

        okButton = new JButton(Messages.message("ok"));
        okButton.setActionCommand(String.valueOf(OK));
        okButton.addActionListener(this);
        okButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        cancelButton = new JButton("cancel");
        cancelButton.setActionCommand(String.valueOf(CANCEL));
        cancelButton.addActionListener(this);
        cancelButton.setAlignmentX(Component.CENTER_ALIGNMENT);

    }

    public void requestFocus() {
        okButton.requestFocus();
    }

    /**
     * Describe <code>initialize</code> method here.
     *
     * @param unit an <code>Unit</code> value
     */
    public void initialize(Unit unit) {
        removeAll();

        goodsList = unit.getGoodsList();
        checkBoxes = new ArrayList<JCheckBox>(goodsList.size());

        int[] widths = { 0 };
        int[] heights = new int[2 * goodsList.size() + 1];
        for (int index = 1; index < heights.length; index += 2) {
            heights[index] = margin;
        }
        setLayout(new HIGLayout(widths, heights));

        int row = 1;
        ImageLibrary library = parent.getGUI().getImageLibrary();
        for (Goods goods : goodsList) {
            // TODO: find out why check box is not displayed when icon
            // is present
            JCheckBox checkBox = new JCheckBox(goods.toString(),
                                               //library.getGoodsImageIcon(goods.getType()),
                                               true);
            checkBoxes.add(checkBox);
            add(checkBox, higConst.rc(row, 1));
            row += 2;
        }

        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, higConst.rc(row, 1));

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
        try {
            switch (Integer.valueOf(command).intValue()) {
            case OK:
                InGameController inGameController = parent.getClient().getInGameController();
                for (int index = 0; index < checkBoxes.size(); index++) {
                    if (checkBoxes.get(index).isSelected()) {
                        inGameController.unloadCargo(goodsList.get(index));
                    }
                }
                setResponse(Boolean.TRUE);
                break;
            case CANCEL:
                setResponse(Boolean.FALSE);
                break;
            default:
                logger.warning("Invalid Actioncommand: invalid number.");
            }
        } catch (NumberFormatException e) {
            logger.warning("Invalid Actioncommand: not a number.");
        }
    }

}
