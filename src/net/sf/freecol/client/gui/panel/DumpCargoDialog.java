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
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Unit;

import net.miginfocom.swing.MigLayout;

/**
 * This panel is used to show information about a tile.
 */
public final class DumpCargoDialog extends FreeColDialog<Boolean> implements ActionListener {

    private static final Logger logger = Logger.getLogger(DumpCargoDialog.class.getName());

    private static final int OK = 0;

    private static final int CANCEL = 1;

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
        super(parent);

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

        setLayout(new MigLayout("wrap 1", "", ""));

        int row = 1;
        for (Goods goods : goodsList) {
            // TODO: find out why check box is not displayed when icon
            // is present
            JCheckBox checkBox = new JCheckBox(goods.toString(),
                                               //getLibrary().getGoodsImageIcon(goods.getType()),
                                               true);
            checkBoxes.add(checkBox);
            add(checkBox);
        }

        add(okButton, "newline 20, span, split 2, tag ok");
        add(cancelButton, "tag cancel");

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
                InGameController inGameController = getCanvas().getClient().getInGameController();
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
