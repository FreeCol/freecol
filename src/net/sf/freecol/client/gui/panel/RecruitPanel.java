/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.UnitType;


/**
 * The panel that allows a user to recruit people in Europe.
 */
public final class RecruitPanel extends FreeColPanel {

    private static final Logger logger = Logger.getLogger(RecruitPanel.class.getName());

    /** The array of recruitable units. */
    private final JButton[] person;

    /** Is there at least one recruitable unit? */
    private boolean shouldEnable = false;


    /**
     * The constructor to use.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public RecruitPanel(FreeColClient freeColClient) {
        super(freeColClient, new MigLayout("wrap 1", "", ""));

        person = new JButton[getMyPlayer().getEurope().getRecruitables().size()];
        for (int i = 0; i < person.length; i++) {
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

        int production = player.getTotalImmigrationProduction();
        int turns = 100;
        if (production > 0) {
            int immigrationRequired = player.getImmigrationRequired()
                - player.getImmigration();
            turns = immigrationRequired / production;
            if (immigrationRequired % production > 0) turns++;
        }
        int recruitPrice = player.getRecruitPrice();
        add(Utility.localizedTextArea(StringTemplate
                .template("recruitPanel.clickOn")
                .addAmount("%money%", recruitPrice)
                .addAmount("%number%", turns)),
            "wrap 20");

        shouldEnable = false;
        int i = 0;
        for (UnitType ut : europe.getRecruitables()) {
            boolean enable = player.checkGold(recruitPrice);
            person[i].setText(Messages.getName(ut));
            person[i].setIcon(new ImageIcon(getImageLibrary().getSmallUnitImage(ut)));
            person[i].setEnabled(enable);
            add(person[i], "growx");
            shouldEnable |= enable;
            i++;
        }

        okButton.setText(Messages.message("close"));
        add(okButton, "newline 20, tag ok");

        setSize(getPreferredSize());
        revalidate();
    }


    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        final String command = ae.getActionCommand();
        if (OK.equals(command)) {
            super.actionPerformed(ae);
        } else {
            try {
                int index = Integer.parseInt(command);
                if (Europe.MigrationType.validMigrantIndex(index)) {
                    igc().recruitUnitInEurope(index);
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
