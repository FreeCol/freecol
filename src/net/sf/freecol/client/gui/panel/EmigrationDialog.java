/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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
public final class EmigrationDialog extends FreeColOldDialog<Integer> {

    private static final Logger logger = Logger.getLogger(EmigrationDialog.class.getName());

    private static final int NUMBER_OF_PERSONS = 3;

    private static final JButton[] person = new JButton[NUMBER_OF_PERSONS];

    private JTextArea question;


    /**
     * The constructor to use.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public EmigrationDialog(FreeColClient freeColClient) {
        super(freeColClient, new MigLayout("wrap 1", "[fill]", ""));

        for (int index = 0; index < NUMBER_OF_PERSONS; index++) {
            person[index] = new JButton();
            person[index].setActionCommand(String.valueOf(index));
            person[index].addActionListener(this);
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
        question = GUI.getDefaultTextArea(Messages.message("chooseImmigrant"));
        if (fountainOfYouth) {
            question.insert(Messages.message("lostCityRumour.fountainOfYouth")
                            + "\n\n", 0);
        }

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


    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent event) {
        final String command = event.getActionCommand();
        try {
            int action = Integer.valueOf(command).intValue();
            if (action >= 0 && action < NUMBER_OF_PERSONS) {
                setResponse(new Integer(action));
            } else {
                super.actionPerformed(event);
            }
        } catch (NumberFormatException e) {
            logger.warning("Invalid ActionEvent, not a number: " + command);
        }
    }


    // Override Component

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeNotify() {
        super.removeNotify();

        removeAll();
        if (person != null) {
            for (int i = 0; i < person.length; i++) person[i] = null;
        }
    }
}
