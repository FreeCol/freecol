/**
 *  Copyright (C) 2002-2024   The FreeCol Team
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

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.panel.FreeColButton;
import net.sf.freecol.client.gui.panel.FreeColButton.ButtonStyle;
import net.sf.freecol.client.gui.panel.Utility;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Player;


/**
 * A dialog that allows a choice of goods amount.
 */
public final class SelectGoodsAmountDialog {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(SelectGoodsAmountDialog.class.getName());

    /** The default amounts to try. */
    private static final int[] amounts = { 20, 40, 50, 60, 80, 100 };

    
    /**
     * Creates a goods amount dialog.
     *
     * @param freeColClient The enclosing {@code FreeColClient}.
     * @param goodsType The {@code GoodsType} to select an amount of.
     * @param available The amount of goods available.
     * @param defaultAmount The amount to select to start with.
     * @param pay If true, check the player has sufficient funds.
     */
    public static FreeColDialog<Integer> create(FreeColClient freeColClient, GoodsType goodsType, int availableAmount,
            int defaultAmount, boolean pay) {
        final FreeColDialog<Integer> dialog = new FreeColDialog<>(api -> {
            final JPanel content = new JPanel(new MigLayout("fill, wrap 1", "", ""));
            
            final int available;
            if (pay) {
                final Player player = freeColClient.getMyPlayer();
                final int gold = player.getGold();
                final int price = player.getMarket().getCostToBuy(goodsType);
                available = Math.min(availableAmount, gold/price);
            } else {
                available = availableAmount;
            }

            final JTextArea question = Utility.localizedTextArea("selectAmountDialog.text");
            content.add(question);
            
            if (goodsType != null) {
                final ImageIcon icon = new ImageIcon(freeColClient.getGUI().getFixedImageLibrary().getScaledGoodsTypeImage(goodsType));
                content.add(new JLabel(icon), "split 2");
            }

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
            
            final JComboBox<Integer> comboBox = new JComboBox<>(values.toArray(new Integer[0]));
            comboBox.setEditable(true);
            if (defaultIndex >= 0) {
                comboBox.setSelectedIndex(defaultIndex);
            }
            comboBox.setInputVerifier(new InputVerifier() {
                @Override @SuppressWarnings("unchecked")
                public boolean verify(JComponent input) {
                    return verifyWholeBox((JComboBox<Integer>)input, available);
                }
            });
            comboBox.getActionMap().put("enterPressed", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    api.setValue(determineValue(comboBox));
                }
            });
            content.add(comboBox, "wrap, growx");

            final JButton okButton = new FreeColButton(Messages.message("ok")).withButtonStyle(ButtonStyle.IMPORTANT);
            okButton.addActionListener(ae -> {
                api.setValue(determineValue(comboBox));
            });
            content.add(okButton, "newline unrel, span, split 2, tag ok");
            
            final JButton cancelButton = new FreeColButton(Messages.message("cancel"));
            cancelButton.addActionListener(ae -> {
                api.setValue(null);
            });
            content.add(cancelButton, "tag cancel");
            
            api.setInitialFocusComponent(comboBox);
            return content;
        });
        return dialog;
    }


    private static boolean verifyWholeBox(JComboBox<Integer> box, int available) {
        final int n = box.getItemCount();
        for (int i = 0; i < n; i++) {
            Integer v = box.getItemAt(i);
            if (v < 0 || v > available) return false;
        }
        return true;
    }

    private static int determineValue(JComboBox<Integer> comboBox) {
        Object value = comboBox.getSelectedItem();
        return (value instanceof Integer) ? (Integer)value : -1;
    }
}
