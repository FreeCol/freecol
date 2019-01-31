/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.ImageIcon;

import javax.swing.JButton;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.StringTemplate;


/**
 * The panel that allows a user to recruit people in Europe.
 */
public final class RecruitPanel extends FreeColPanel {

    private static final Logger logger = Logger.getLogger(RecruitPanel.class.getName());

    /** The buttons for the recruitable units. */
    private final JButton[] person;

    /** Is there at least one recruitable unit? */
    private boolean shouldEnable = false;


    /**
     * The constructor to use.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    public RecruitPanel(FreeColClient freeColClient) {
        super(freeColClient, null, new MigLayout("wrap 1", "", ""));

        List<AbstractUnit> recruitables = getMyPlayer().getEurope()
            .getExpandedRecruitables(false);
        this.person = new JButton[recruitables.size()];
        for (int i = 0; i < person.length; i++) {
            this.person[i] = new JButton();
            this.person[i].setActionCommand(String.valueOf(i));
            this.person[i].addActionListener(this);
            this.person[i].setIconTextGap(MARGIN);
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
        int recruitPrice = player.getEuropeanRecruitPrice();
        add(Utility.localizedTextArea(StringTemplate
                .template("recruitPanel.clickOn")
                .addAmount("%money%", recruitPrice)
                .addAmount("%number%", turns)),
            "wrap 20");

        shouldEnable = false;
        List<AbstractUnit> recruitables = europe.getExpandedRecruitables(false);
        for (int i = 0; i < this.person.length; i++) {
            boolean enable = player.checkGold(recruitPrice);
            AbstractUnit au = recruitables.get(i);
            this.person[i].setText(Messages.message(au.getSingleLabel()));
            this.person[i].setIcon(new ImageIcon(getSmallAbstractUnitImage(au)));
            this.person[i].setEnabled(enable);
            add(this.person[i], "growx");
            shouldEnable |= enable;
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
                    if (!shouldEnable) getGUI().removeComponent(this);
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
