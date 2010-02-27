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
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JTextArea;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.UnitType;

import net.miginfocom.swing.MigLayout;

/**
 * The panel that allows a user to recruit people in Europe.
 */
public final class RecruitDialog extends FreeColDialog<Integer> implements ActionListener {

    private static Logger logger = Logger.getLogger(RecruitDialog.class.getName());

    private static final int RECRUIT_CANCEL = -1;

    private static final int NUMBER_OF_PERSONS = 3;

    private final JButton[] person = new JButton[NUMBER_OF_PERSONS];

    private final JButton cancel;

    private final JTextArea question;

    /**
     * The constructor to use.
     */
    public RecruitDialog(Canvas parent) {
        super(parent);

        setFocusCycleRoot(true);

        question = getDefaultTextArea(Messages.message("recruitDialog.clickOn"));

        for (int index = 0; index < NUMBER_OF_PERSONS; index++) {
            person[index] = new JButton();
            person[index].setActionCommand(String.valueOf(index));
            enterPressesWhenFocused(person[index]);
            person[index].addActionListener(this);
            person[index].setIconTextGap(margin);
        }

        cancel = new JButton(Messages.message("cancel"));
        cancel.setActionCommand(String.valueOf(RECRUIT_CANCEL));
        enterPressesWhenFocused(cancel);
        cancel.addActionListener(this);
        setCancelComponent(cancel);

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

        setLayout(new MigLayout("wrap 1", "", ""));

        add(question, "wrap 20");

        int recruitPrice = 0;
        Player player = getMyPlayer();
        if ((getGame() != null) && (player != null)) {

            int production = 0;
            for (Colony colony : player.getColonies()) {
                production += colony.getProductionOf(Specification.getSpecification()
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

            question.setText(Messages.message("recruitDialog.clickOn",
                                              "%money%", String.valueOf(recruitPrice),
                                              "%number%", String.valueOf(turns)));

            for (int index = 0; index < NUMBER_OF_PERSONS; index++) {
                UnitType unitType = player.getEurope().getRecruitable(index);
                ImageIcon unitIcon = getLibrary().getUnitImageIcon(unitType);
                person[index].setText(Messages.message(unitType.getNameKey()));
                person[index].setIcon(getLibrary().getScaledImageIcon(unitIcon, 0.66f));

                if (recruitPrice > player.getGold()) {
                    person[index].setEnabled(false);
                } else {
                    person[index].setEnabled(true);
                }

                add(person[index], "growx");
            }
        }

        add(cancel, "newline 20, tag cancel");

        setSize(getPreferredSize());

    }

    /**
     * Analyzes an event and calls the right external methods to take care of
     * the user's request.
     * 
     * @param event The incoming action event
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        try {
            int action = Integer.valueOf(command).intValue();
            if (action == RECRUIT_CANCEL) {
                setResponse(new Integer(-1));
            } else if (action >= 0 && action < NUMBER_OF_PERSONS) {
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
