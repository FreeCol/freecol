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

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

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
 * The panel that allows a user to train people in Europe.
 */

public final class TrainDialog extends FreeColDialog implements ActionListener {



    private static Logger logger = Logger.getLogger(TrainDialog.class.getName());

    private static final int TRAIN_CANCEL = -1;

    private final Comparator<UnitType> priceComparator = new Comparator<UnitType>() {
        public int compare(UnitType type1, UnitType type2) {
            return type1.getPrice() - type2.getPrice();
        }
    };
    
    @SuppressWarnings("unused")
    private final Canvas parent;

    private final FreeColClient freeColClient;

    private final InGameController inGameController;
    
    private final JButton cancel = new JButton(Messages.message("trainDialog.cancel"));

    private final ArrayList<UnitType> trainableUnits = new ArrayList<UnitType>();

    private final ArrayList<JLabel> prices = new ArrayList<JLabel>();

    private final ArrayList<JButton> buttons = new ArrayList<JButton>();
    

    /**
     * The constructor to use.
     */
    public TrainDialog(Canvas parent) {
        super();
        this.parent = parent;
        this.freeColClient = parent.getClient();
        this.inGameController = freeColClient.getInGameController();
        setFocusCycleRoot(true);

        ImageLibrary library = parent.getGUI().getImageLibrary();

        List<UnitType> unitTypes = FreeCol.getSpecification().getUnitTypeList();
        for(UnitType unitType : unitTypes) {
            if (unitType.getSkill() > 0 && unitType.hasPrice()) {
                trainableUnits.add(unitType);
            }
        }
        Collections.sort(trainableUnits, priceComparator);

        setLayout(new HIGLayout(new int[] { 0 }, new int[] { 0, margin, 0, margin, 0 }));

        int rows = trainableUnits.size();
        if (rows % 2 == 0) {
            rows--;
        }

        int[] widths = new int[] { 0, margin, 0, margin, 0, margin, 0 };
        int[] heights = new int[rows];

        for (int index = 0; index < rows / 2; index++) {
            heights[2 * index + 1] = margin;
        }

        int[] labelColumn = { 1, 5 };
        int[] buttonColumn = { 3, 7 };
        JPanel trainPanel = new JPanel(new HIGLayout(widths, heights));

        int row = 1;
        int counter = 0;
        for (UnitType unitType : trainableUnits) {
            int graphicsType = ImageLibrary.getUnitGraphicsType(unitType.getIndex(), false, false, false, false);
            JButton newButton = new JButton(unitType.getName(), 
                                            library.getScaledUnitImageIcon(graphicsType, 0.66f));
            newButton.setActionCommand(unitType.getId());
            newButton.addActionListener(this);
            newButton.setIconTextGap(margin);
            enterPressesWhenFocused(newButton);
            buttons.add(newButton);
            
            JLabel newLabel = new JLabel();
            prices.add(newLabel);
            trainPanel.add(newLabel, higConst.rc(row, labelColumn[counter]));
            trainPanel.add(newButton, higConst.rc(row, buttonColumn[counter]));
            if (counter == 1) {
                counter = 0;
                row += 2;
            } else {
                counter = 1;
            }
        }
        trainPanel.setSize(trainPanel.getPreferredSize());

        JLabel question = new JLabel(Messages.message("trainDialog.clickOn"));

        cancel.setActionCommand(String.valueOf(TRAIN_CANCEL));
        cancel.addActionListener(this);
        enterPressesWhenFocused(cancel);

        add(question, higConst.rc(1, 1, ""));
        add(trainPanel, higConst.rc(3, 1));
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
        int numberOfTypes = trainableUnits.size();
        for (int index = 0; index < numberOfTypes; index++) {
            JButton button = buttons.get(index);
            UnitType unitType = FreeCol.getSpecification().getUnitType(button.getActionCommand());
            
            int price = player.getEurope().getUnitPrice(unitType);
            prices.get(index).setText(String.valueOf(price));
            if (trainableUnits.get(index).getPrice() > player.getGold()) {
                button.setEnabled(false);
            } else {
                button.setEnabled(true);
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
            inGameController.trainUnitInEurope(unitType);
            setResponse(new Integer(0));
        } else {
            setResponse(new Integer(-1));
        }
    }
}
