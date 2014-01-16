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
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JTextArea;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.UnitType;

import net.miginfocom.swing.MigLayout;


/**
 * The panel that allows a user to recruit people in Europe.
 */
public final class RecruitPanel extends FreeColPanel {

    private static Logger logger = Logger.getLogger(RecruitPanel.class.getName());

    /** The main text area. */
    private final JTextArea question;

    /** The array of recruitable units. */
    private final JButton[] person = new JButton[Europe.RECRUIT_COUNT];

    /** Is there at least one recruitable unit? */
    private boolean shouldEnable = false;


    /**
     * The constructor to use.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public RecruitPanel(FreeColClient freeColClient) {
        super(freeColClient, new MigLayout("wrap 1", "", ""));

        question = GUI.getDefaultTextArea("");
        for (int i = 0; i < Europe.RECRUIT_COUNT; i++) {
            person[i] = new JButton();
            person[i].setActionCommand(String.valueOf(i));
            person[i].addActionListener(this);
            person[i].setIconTextGap(MARGIN);
        }
        update();
    }


    /**
     * Updates this panel's labels so that the information it displays
     * is up to date.
     */
    public void update() {
        removeAll();

        final Player player = getMyPlayer();
        final Europe europe = player.getEurope();

        int production = 0;
        for (Colony colony : player.getColonies()) {
            production += colony.getTotalProductionOf(getSpecification()
                .getGoodsType("model.goods.crosses"));
        }
        int turns = 100;
        if (production > 0) {
            int immigrationRequired = player.getImmigrationRequired()
                - player.getImmigration();
            turns = immigrationRequired / production;
            if (immigrationRequired % production > 0) turns++;
        }
        int recruitPrice = player.getRecruitPrice();
        String header
            = Messages.message(StringTemplate.template("recruitPanel.clickOn")
                .addAmount("%money%", recruitPrice)
                .addAmount("%number%", turns));
        question.setText(header);
        add(question, "wrap 20");

        shouldEnable = false;
        for (int i = 0; i < Europe.RECRUIT_COUNT; i++) {
            UnitType unitType = europe.getRecruitable(i);
            ImageIcon icon = getLibrary().getUnitImageIcon(unitType, 0.66);
            boolean enable = player.checkGold(recruitPrice);
            person[i].setText(Messages.message(unitType.getNameKey()));
            person[i].setIcon(icon);
            person[i].setEnabled(enable);
            add(person[i], "growx");
            shouldEnable |= enable;
        }

        okButton.setText(Messages.message("recruitPanel.ok"));
        add(okButton, "newline 20, tag ok");

        setSize(getPreferredSize());
        revalidate();
    }


    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        final String command = event.getActionCommand();
        if (OK.equals(command)) {
            super.actionPerformed(event);
        } else {
            try {
                int index = Integer.parseInt(command);
                if (index >= 0 && index < Europe.RECRUIT_COUNT) {
                    getController().recruitUnitInEurope(index);
                    getGUI().updateEuropeanSubpanels();
                    if (!shouldEnable) getGUI().removeFromCanvas(this);
                    return;
                }
            } catch (NumberFormatException e) {}
            logger.warning("Invalid action command: " + command);
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
            for (int i = 0; i < person.length; i++) {
                person[i] = null;
            }
        }
    }
}
