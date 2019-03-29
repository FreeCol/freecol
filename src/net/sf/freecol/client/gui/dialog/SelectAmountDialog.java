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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;

import javax.swing.InputVerifier;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.panel.*;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Player;
import static net.sf.freecol.common.util.CollectionUtils.*;


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

    /** The maximum amount of goods available. */
    private int available;

    
    /**
     * The constructor to use.
     *
     * @param freeColClient The enclosing {@code FreeColClient}.
     * @param frame The owner frame.
     * @param goodsType The {@code GoodsType} to select an amount of.
     * @param available The amount of goods available.
     * @param defaultAmount The amount to select to start with.
     * @param pay If true, check the player has sufficient funds.
     */
    public SelectAmountDialog(FreeColClient freeColClient, JFrame frame,
                              GoodsType goodsType, int available,
                              int defaultAmount, boolean pay) {
        super(freeColClient, frame);

        if (pay) {
            final Player player = getMyPlayer();
            final int gold = player.getGold();
            int price = player.getMarket().getCostToBuy(goodsType);
            available = Math.min(available, gold/price);
        }
        this.available = available;

        JTextArea question
            = Utility.localizedTextArea("selectAmountDialog.text");

        List<Integer> values = new ArrayList<>();
        int defaultIndex = -1;
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
        this.comboBox.setInputVerifier(new InputVerifier() {
                @Override @SuppressWarnings("unchecked")
                public boolean verify(JComponent input) {
                    return verifyWholeBox((JComboBox<Integer>)input);
                }
            });
        JPanel panel = new MigPanel(new MigLayout("wrap 1", "", ""));
        panel.add(question);
        panel.add(this.comboBox, "wrap 20, growx");
        panel.setSize(panel.getPreferredSize());

        initializeInputDialog(frame, true, panel, null, "ok", "cancel");
    }

    /**
     * Verify the contents of the JComboBox.
     *
     * @param box The {@code JComboBox} to verify.
     * @return True if all is well.
     */
    private boolean verifyWholeBox(JComboBox<Integer> box) {
        final int n = box.getItemCount();
        for (int i = 0; i < n; i++) {
            Integer v = box.getItemAt(i);
            if (v < 0 || v > available) return false;
        }
        return true;
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
