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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.InGameController;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.UnitType;
import cz.autel.dmi.HIGLayout;

/**
 * The panel that allows a user to purchase ships and artillery in Europe.
 */
public final class PurchaseDialog extends FreeColDialog implements ActionListener {



    private static Logger logger = Logger.getLogger(PurchaseDialog.class.getName());

    private static final int PURCHASE_CANCEL = 0, PURCHASE_ARTILLERY = 1, PURCHASE_CARAVEL = 2,
            PURCHASE_MERCHANTMAN = 3, PURCHASE_GALLEON = 4, PURCHASE_PRIVATEER = 5, PURCHASE_FRIGATE = 6;

    private final ArrayList<JButton> buttons;

    private final JButton cancel;

    private final ArrayList<JLabel> prices;

    private final FreeColClient freeColClient;

    private final InGameController inGameController;


    /**
     * The constructor to use.
     */
    public PurchaseDialog(Canvas parent) {
        super();
        this.freeColClient = parent.getClient();
        this.inGameController = freeColClient.getInGameController();
        setFocusCycleRoot(true);

        ImageLibrary library = parent.getGUI().getImageLibrary();

        setLayout(new HIGLayout(new int[] { 0 }, new int[] { 0, margin, 0, margin, 0 }));

        JTextArea question = getDefaultTextArea(Messages.message("purchaseDialog.clickOn"));

        ArrayList<UnitType> unitTypesForPurchasing = new ArrayList<UnitType>();
        List<UnitType> unitTypes = FreeCol.getSpecification().getUnitTypeList();
        for(UnitType unitType : unitTypes) {
            if (!unitType.hasSkill() && unitType.hasPrice()) {
                unitTypesForPurchasing.add(unitType);
            }
        }

        int numberUnits = unitTypesForPurchasing.size();
        int[] widths = new int[] { 0, margin, 0 };
        int[] heights = new int[2 * numberUnits - 1];
        for (int index = 1; index < numberUnits; index += 2) {
            heights[index] = margin;
        }
        int buttonColumn = 1;
        int labelColumn = 3;

        JPanel purchasePanel = new JPanel();
        purchasePanel.setLayout(new HIGLayout(widths, heights));

        prices = new ArrayList<JLabel>();
        buttons = new ArrayList<JButton>();
        int row = 1;
        for(UnitType unitType : unitTypesForPurchasing) {
            int graphicsType = ImageLibrary.getUnitGraphicsType(unitType.getIndex(), false, false, false, false);
            JButton button = new JButton(unitType.getName(), library.getScaledUnitImageIcon(graphicsType, 0.66f));
            button.setIconTextGap(margin);
            button.addActionListener(this);
            button.setActionCommand(unitType.getId());
            enterPressesWhenFocused(button);
            buttons.add(button);
            purchasePanel.add(button, higConst.rc(row, buttonColumn));
            
            JLabel priceLabel = new JLabel();
            prices.add(priceLabel);
            purchasePanel.add(priceLabel, higConst.rc(row, labelColumn));
            row += 2;
        }

        cancel = new JButton(Messages.message("cancel"));
        cancel.setActionCommand(String.valueOf(PURCHASE_CANCEL));
        cancel.addActionListener(this);
        enterPressesWhenFocused(cancel);
        setCancelComponent(cancel);

        add(question, higConst.rc(1, 1));
        add(purchasePanel, higConst.rc(3, 1, ""));
        add(cancel, higConst.rc(5, 1, ""));

        setSize(getPreferredSize());
    }

    public void requestFocus() {
        cancel.requestFocus();
    }

    /**
     * Updates this panel's labels so that the information it displays is up to
     * date.
     */
    public void initialize() {
        Player player = freeColClient.getMyPlayer();
        if ((freeColClient.getGame() != null) && (player != null)) {
            for(int i=0; i < prices.size(); i++) {
                JButton button = buttons.get(i);
                UnitType unitType = FreeCol.getSpecification().getUnitType(button.getActionCommand());
                
                int price = player.getEurope().getUnitPrice(unitType);
                prices.get(i).setText(Integer.toString(price));
                if (price > player.getGold()) {
                    button.setEnabled(false);
                } else {
                    button.setEnabled(true);
                }
            }
        }
    }

    /**
     * Analyzes an event and calls the right external methods to take care of
     * the user's request.
     * 
     * @param event The incoming action event
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        UnitType unitType = FreeCol.getSpecification().getUnitType(command);
        if (unitType != null) {
            inGameController.purchaseUnitFromEurope(unitType);
            setResponse(new Integer(0));
        } else {
            setResponse(new Integer(-1));
        }
    }
}
