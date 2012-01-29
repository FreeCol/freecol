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
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JTextArea;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.UnitType;

/**
 * The panel that allows a user to recruit people in Europe.
 */
public final class RecruitDialog extends FreeColDialog<Integer> implements ActionListener {

    private static Logger logger = Logger.getLogger(RecruitDialog.class.getName());

    private static final int RECRUIT_CANCEL = -1;

    private static final int NUMBER_OF_PERSONS = 3;

    private final JButton[] person = new JButton[NUMBER_OF_PERSONS];

    private final JTextArea question;

    /**
     * The constructor to use.
     * @param freeColClient 
     */
    public RecruitDialog(FreeColClient freeColClient, GUI gui) {
        super(freeColClient, gui);

        setFocusCycleRoot(true);

        question = getDefaultTextArea(Messages.message("recruitDialog.clickOn"));

        for (int index = 0; index < NUMBER_OF_PERSONS; index++) {
            person[index] = new JButton();
            person[index].setActionCommand(String.valueOf(index));
            enterPressesWhenFocused(person[index]);
            person[index].addActionListener(this);
            person[index].setIconTextGap(margin);
        }

        initialize();

    }


    /**
     * Updates this panel's labels so that the information it displays is up to
     * date.
     */
    public void initialize() {

        setLayout(new MigLayout("wrap 1", "", ""));

        add(question, "wrap 20");

        int recruitPrice = 0;
        Player player = getMyPlayer();
        if ((getGame() != null) && (player != null)) {

            int production = 0;
            for (Colony colony : player.getColonies()) {
                production += colony.getProductionOf(getSpecification()
                                                     .getGoodsType("model.goods.crosses"));
            }
            int turns = 100;
            if (production > 0) {
                int immigrationRequired = (player.getImmigrationRequired() - player.getImmigration());
                turns = immigrationRequired / production;
                if (immigrationRequired % production > 0) {
                    turns++;
                }
            }
            recruitPrice = player.getRecruitPrice();

            question.setText(Messages.message(StringTemplate.template("recruitDialog.clickOn")
                                              .addAmount("%money%", recruitPrice)
                                              .addAmount("%number%", turns)));

            for (int index = 0; index < NUMBER_OF_PERSONS; index++) {
                UnitType unitType = player.getEurope().getRecruitable(index);
                ImageIcon unitIcon = getLibrary().getUnitImageIcon(unitType, 0.66);
                person[index].setText(Messages.message(unitType.getNameKey()));
                person[index].setIcon(unitIcon);
                person[index].setEnabled(player.checkGold(recruitPrice));

                add(person[index], "growx");
            }
        }

        add(cancelButton, "newline 20, tag cancel");

        setSize(getPreferredSize());

    }

    @Override
    public void requestFocus() {
        cancelButton.requestFocus();
    }


    /**
     * Analyzes an event and calls the right external methods to take care of
     * the user's request.
     *
     * @param event The incoming action event
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        if (CANCEL.equals(command)) {
            setResponse(new Integer(-1));
        } else {
            try {
                int action = Integer.parseInt(command);
                if (action >= 0 && action < NUMBER_OF_PERSONS) {
                    getController().recruitUnitInEurope(action);
                    setResponse(new Integer(0));
                } else {
                    logger.warning("Invalid action command");
                    setResponse(new Integer(-1));
                }
            } catch (NumberFormatException e) {
                logger.warning("Invalid action number");
                setResponse(new Integer(-1));
            }
        }
    }
}
