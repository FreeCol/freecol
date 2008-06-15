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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.InGameController;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.GoodsType;
import cz.autel.dmi.HIGLayout;

/**
 * The panel that allows a user to recruit people in Europe.
 */
public final class SelectAmountDialog extends FreeColDialog implements ActionListener {



    private static Logger logger = Logger.getLogger(SelectAmountDialog.class.getName());

    private static final int SELECT_CANCEL = -1;

    private final JButton cancel, ok;

    private final JPanel buttonPanel;

    private final JTextArea question;

    private final JComboBox comboBox;

    private final FreeColClient freeColClient;

    private final InGameController inGameController;


    /**
     * The constructor to use.
     */
    public SelectAmountDialog(Canvas parent, GoodsType goodsType, int available, boolean needToPay) {
        super(parent);

        this.freeColClient = parent.getClient();
        this.inGameController = freeColClient.getInGameController();
        setFocusCycleRoot(true);

        question = getDefaultTextArea(Messages.message("goodsTransfer.text"));

        if (needToPay) {
            int gold = parent.getClient().getMyPlayer().getGold();
            int price = parent.getClient().getMyPlayer().getMarket().costToBuy(goodsType);
            available = Math.min(available, gold/price);
        }
        int[] amounts = {20, 40, 50, 60, 80, 100};

        Vector<Integer> values = new Vector<Integer>();
        for (int index = 0; index < amounts.length; index++) {
            if (amounts[index] < available) {
                values.add(amounts[index]);
            } else {
                values.add(available);
                break;
            }
        }

        comboBox = new JComboBox(values);
        comboBox.setEditable(true);
        comboBox.addActionListener(this);

        ok = new JButton(Messages.message("ok"));
        enterPressesWhenFocused(ok);
        ok.addActionListener(this);

        cancel = new JButton(Messages.message("cancel"));
        enterPressesWhenFocused(cancel);
        cancel.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    setResponse(new Integer(SELECT_CANCEL));
                }
            });
        setCancelComponent(cancel);

        buttonPanel = new JPanel();
        buttonPanel.add(ok);
        buttonPanel.add(cancel);

        initialize();

    }

    public void requestFocus() {
        cancel.requestFocus();
    }

    /**
     * Updates this panel's labels so that the information it displays is up to
     * date.
     */
    public void initialize() {

        int[] widths = new int[] { 0 };
        int[] heights = new int[5];
        for (int index = 1; index < heights.length; index += 2) {
            heights[index] = margin;
        }
        setLayout(new HIGLayout(widths, heights));

        int row = 1;
        int column = 1;

        add(question, higConst.rc(row, column));
        row += 2;
        add(comboBox, higConst.rc(row, column));
        row += 2;
        add(buttonPanel, higConst.rc(row, column));

        setSize(getPreferredSize());

    }

    /**
     * Analyzes an event and calls the right external methods to take care of
     * the user's request.
     * 
     * @param event The incoming action event
     */
    public void actionPerformed(ActionEvent event) {
        Object item = comboBox.getSelectedItem();
        if (item instanceof Integer) {
            setResponse(item);
        } else if (item instanceof String) {
            try {
                setResponse(Integer.valueOf((String) item));
            } catch (NumberFormatException e) {
                // do nothing
            }
        }
    }
}
