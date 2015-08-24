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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JTextArea;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Player;


/**
 * The panel that allows a choice of goods amount.
 */
public final class SelectAmountDialog extends FreeColInputDialog<Integer> {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(SelectAmountDialog.class.getName());

    private static final int SELECT_CANCEL = -1;

    /** The default amounts to try. */
    private static final int[] amounts = { 20, 40, 50, 60, 80, 100 };

    /** The combo box to input the amount through. */
    private final JComboBox<Integer> comboBox;


    /**
     * The constructor to use.
     *
     * @param freeColClient The enclosing <code>FreeColClient</code>.
     * @param frame The owner frame.
     * @param goodsType The <code>GoodsType</code> to select an amount of.
     * @param available The amount of goods available.
     * @param defaultAmount The amount to select to start with.
     * @param pay If true, check the player has sufficient funds.
     */
    public SelectAmountDialog(FreeColClient freeColClient, JFrame frame,
            GoodsType goodsType, int available, int defaultAmount, boolean pay) {
        super(freeColClient, frame);

        if (pay) {
            final Player player = getMyPlayer();
            final int gold = player.getGold();
            int price = player.getMarket().getCostToBuy(goodsType);
            available = Math.min(available, gold/price);
        }

        JTextArea question
            = Utility.localizedTextArea("selectAmountDialog.text");

        int defaultIndex = -1;
        List<Integer> values = new ArrayList<>();
        for (int index = 0; index < amounts.length; index++) {
            if (amounts[index] < available) {
                if (amounts[index] == defaultAmount) defaultIndex = index;
                values.add(amounts[index]);
            } else {
                if (available == defaultAmount) defaultIndex = index;
                values.add(available);
                break;
            }
        }
        if (defaultAmount > 0 && defaultIndex < 0) {
            for (int index = 0; index < values.size(); index++) {
                if (defaultAmount < values.get(index)) {
                    values.add(index, defaultAmount);
                    defaultIndex = index;
                    break;
                }
            }
        }
        this.comboBox = new JComboBox<>(values.toArray(new Integer[0]));
        this.comboBox.setEditable(true);
        if (defaultIndex >= 0) this.comboBox.setSelectedIndex(defaultIndex);

        MigPanel panel = new MigPanel(new MigLayout("wrap 1", "", ""));
        panel.add(question);
        panel.add(this.comboBox, "wrap 20, growx");
        panel.setSize(panel.getPreferredSize());

        initializeInputDialog(frame, true, panel, null, "ok", "cancel");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Integer getInputValue() {
        Object value = this.comboBox.getSelectedItem();
        return (value instanceof Integer) ? (Integer)value : -1;
    }


    // Override Component

    /**
     * {@inheritDoc}
     */
    @Override
    public void requestFocus() {
        this.comboBox.requestFocus();
    }
}
