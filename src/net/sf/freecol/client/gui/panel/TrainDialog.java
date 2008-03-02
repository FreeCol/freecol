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
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.InGameController;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.i18n.Messages;

import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.UnitType;

import cz.autel.dmi.HIGLayout;

/**
 * The panel that allows a user to train people in Europe.
 */

public final class TrainDialog extends FreeColDialog implements ActionListener {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(TrainDialog.class.getName());

    private static final int NUMBER_OF_COLUMNS = 3;

    private static final String TRAIN_DONE = "DONE";

    private final Canvas parent;

    private final FreeColClient freeColClient;

    private final InGameController inGameController;
    
    private final JButton done = new JButton(Messages.message("trainDialog.done"));

    private final JPanel trainPanel = new JPanel(new GridLayout(0, NUMBER_OF_COLUMNS));

    private final List<UnitType> trainableUnits = new ArrayList<UnitType>();

    private final Comparator<UnitType> unitPriceComparator;

    private static final int[] buttonWidths = new int[] { 0, 6, 0 };
    private static final int[] buttonHeights = new int[] { 24, 24 };
    private static final HIGLayout buttonLayout = new HIGLayout(buttonWidths, buttonHeights);


    /**
     * The constructor to use.
     */
    public TrainDialog(Canvas parent) {
        super();
        this.parent = parent;
        this.freeColClient = parent.getClient();
        this.inGameController = freeColClient.getInGameController();
        setFocusCycleRoot(true);

        final Europe europe = freeColClient.getMyPlayer().getEurope();
        unitPriceComparator = new Comparator<UnitType>() {
            public int compare(final UnitType type1, final UnitType type2) {
                return (europe.getUnitPrice(type1) - 
                        europe.getUnitPrice(type2));
            }
        };

        trainableUnits.addAll(FreeCol.getSpecification().getUnitTypesTrainedInEurope());

        setLayout(new HIGLayout(new int[] { 0 }, new int[] { 0, 3 * margin, 0, 3 * margin, 0 }));

        final JLabel question = new JLabel(Messages.message("trainDialog.clickOn"));

        done.setActionCommand(String.valueOf(TRAIN_DONE));
        done.addActionListener(this);
        enterPressesWhenFocused(done);

        add(question, higConst.rc(1, 1, ""));
        add(trainPanel, higConst.rc(3, 1));
        add(done, higConst.rc(5, 1, ""));

    }

    public void requestFocus() {
        done.requestFocus();
    }

    /**
     * Updates this panel's labels so that the information it displays is up to
     * date.
     */
    public void initialize() {

        trainPanel.removeAll();

        final Player player = freeColClient.getMyPlayer();
        final Europe europe = player.getEurope();

        final ImageLibrary library = parent.getGUI().getImageLibrary();

        // price may have changed
        Collections.sort(trainableUnits, unitPriceComparator);

        for (UnitType unitType : trainableUnits) {
            int price = europe.getUnitPrice(unitType);
            JButton newButton = new JButton();
            newButton.setLayout(buttonLayout);
            ImageIcon unitIcon = library.getUnitImageIcon(unitType, (price > player.getGold()));
            JLabel unitName = new JLabel(unitType.getName());
            JLabel unitPrice = new JLabel(Messages.message("goldAmount", "%amount%", 
                                                           String.valueOf(price)));
            if (price > player.getGold()) {
                unitName.setEnabled(false);
                unitPrice.setEnabled(false);
                newButton.setEnabled(false);
            }
            newButton.add(new JLabel(library.getScaledImageIcon(unitIcon, 0.66f)),
                          higConst.rcwh(1, 1, 1, 2));
            newButton.add(unitName, higConst.rc(1, 3));
            newButton.add(unitPrice, higConst.rc(2, 3));
            newButton.setActionCommand(unitType.getId());
            newButton.addActionListener(this);
            enterPressesWhenFocused(newButton);
            trainPanel.add(newButton);
        }
        trainPanel.setSize(trainPanel.getPreferredSize());
        setSize(getPreferredSize());
        revalidate();
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
            inGameController.trainUnitInEurope(unitType);
            initialize();
        }
    }
}
