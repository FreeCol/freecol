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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;

import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.UnitType;

import net.miginfocom.swing.MigLayout;


/**
 * The panel that allows a user to train people in Europe.
 */

public final class TrainDialog extends FreeColDialog<Integer> implements ActionListener {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(TrainDialog.class.getName());

    private static final String TRAIN_DONE = "DONE";

    private final JButton done = new JButton(Messages.message("trainDialog.done"));

    private final JLabel question;

    private final List<UnitType> trainableUnits = new ArrayList<UnitType>();

    private final Comparator<UnitType> unitPriceComparator;

    /**
     * The constructor to use.
     */
    public TrainDialog(Canvas parent, EuropePanel.EuropeAction europeAction) {

        super(parent);

        final Europe europe = getMyPlayer().getEurope();
        unitPriceComparator = new Comparator<UnitType>() {
            public int compare(final UnitType type1, final UnitType type2) {
                return (europe.getUnitPrice(type1) - 
                        europe.getUnitPrice(type2));
            }
        };

        switch(europeAction) {
        case TRAIN:
            trainableUnits.addAll(FreeCol.getSpecification().getUnitTypesTrainedInEurope());
            question = new JLabel(Messages.message("trainDialog.clickOn"));
            setLayout(new MigLayout("wrap 3", "[sg]", ""));
            break;
        case PURCHASE:
        default:
            trainableUnits.addAll(FreeCol.getSpecification().getUnitTypesPurchasedInEurope());
            question  = new JLabel(Messages.message("purchaseDialog.clickOn"));
            setLayout(new MigLayout("wrap 2", "[sg]", ""));
        }

        done.setActionCommand(String.valueOf(TRAIN_DONE));
        done.addActionListener(this);
        enterPressesWhenFocused(done);

    }

    /**
     * Updates this panel's labels so that the information it displays is up to
     * date.
     */
    public void initialize() {

        removeAll();
        add(question, "span, wrap 20");

        final Player player = getMyPlayer();
        final Europe europe = player.getEurope();

        // price may have changed
        Collections.sort(trainableUnits, unitPriceComparator);

        for (UnitType unitType : trainableUnits) {
            int price = europe.getUnitPrice(unitType);
            JButton newButton = new JButton();
            newButton.setLayout(new MigLayout("wrap 2", "[60]", "[30][30]"));

            ImageIcon unitIcon = getLibrary().getUnitImageIcon(unitType, (price > player.getGold()));
            JLabel unitName = localizedLabel(unitType.getNameKey());
            JLabel unitPrice = new JLabel(Messages.message("goldAmount", "%amount%", 
                                                           String.valueOf(price)));
            if (price > player.getGold()) {
                unitName.setEnabled(false);
                unitPrice.setEnabled(false);
                newButton.setEnabled(false);
            }
            newButton.add(new JLabel(getLibrary().getScaledImageIcon(unitIcon, 0.66f)), "span 1 2");
            newButton.add(unitName);
            newButton.add(unitPrice);
            newButton.setActionCommand(unitType.getId());
            newButton.addActionListener(this);
            enterPressesWhenFocused(newButton);
            add(newButton, "grow");
        }
        add(done, "newline 20, span, tag ok");
        setSize(getPreferredSize());
        revalidate();
    }

    public void requestFocus() {
        done.requestFocus();
    }

    /**
     * Analyzes an event and calls the right external methods to take care of
     * the user's request.
     * 
     * @param event The incoming action event
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        if (TRAIN_DONE.equals(command)) {
            setResponse(new Integer(-1));
        } else {
            UnitType unitType = FreeCol.getSpecification().getUnitType(command);
            getController().trainUnitInEurope(unitType);
            initialize();
        }
    }
}
