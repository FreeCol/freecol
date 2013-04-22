/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JTextArea;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.UnitType;


/**
 * The panel that allows a user to choose which unit will emigrate from Europe.
 */
public final class EmigrationPanel extends FreeColDialog<Integer> {

    private static final Logger logger = Logger.getLogger(EmigrationPanel.class.getName());

    private static final int NUMBER_OF_PERSONS = 3;

    private static final JButton[] person = new JButton[NUMBER_OF_PERSONS];

    private JTextArea question = getDefaultTextArea(Messages.message("chooseImmigrant"));


    /**
     * The constructor to use.
     * @param freeColClient 
     */
    public EmigrationPanel(FreeColClient freeColClient, GUI gui) {
        super(freeColClient, gui);
        for (int index = 0; index < NUMBER_OF_PERSONS; index++) {
            person[index] = new JButton();
            person[index].setActionCommand(String.valueOf(index));
            person[index].addActionListener(this);
            enterPressesWhenFocused(person[index]);
        }
    }

    public void requestFocus() {
        person[0].requestFocus();
    }

    /**
     * Updates this panel's labels so that the information it displays is up to
     * date.
     * 
     * @param europe The Europe Object where we can find the units that are
     *            prepared to emigrate.
     * @param fountainOfYouth a <code>boolean</code> value
     */
    public void initialize(Europe europe, boolean fountainOfYouth) {

        if (fountainOfYouth) {
            question.insert(Messages.message("lostCityRumour.fountainOfYouth")
                            + "\n\n", 0);
        }

        setLayout(new MigLayout("wrap 1", "[fill]", ""));

        add(question, "wrap 20");

        for (int index = 0; index < NUMBER_OF_PERSONS; index++) {
            UnitType unitType = europe.getRecruitable(index);
            ImageIcon unitIcon = getLibrary().getUnitImageIcon(unitType, 0.66);
            person[index].setText(Messages.message(unitType.getNameKey()));
            person[index].setIcon(unitIcon);

            add(person[index]);
        }

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
            int action = Integer.valueOf(command).intValue();
            if (action >= 0 && action < NUMBER_OF_PERSONS) {
                setResponse(new Integer(action));
            } else {
                logger.warning("Invalid Actioncommand: invalid number.");
            }
        } catch (NumberFormatException e) {
            logger.warning("Invalid Actioncommand: not a number: " + command);
        }
    }

}
