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

import javax.swing.JButton;
import javax.swing.JLabel;

import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.UnitType;
import cz.autel.dmi.HIGLayout;

/**
 * This panel is used to show information about an Indian settlement.
 */
public final class IndianSettlementPanel extends FreeColDialog implements ActionListener {



    private static final Logger logger = Logger.getLogger(EventPanel.class.getName());

    private static final int OK = 0;

    private final JLabel skillLabel, highlyWantedLabel, otherWantedLabel;

    private final JButton okButton;


    /**
     * The constructor that will add the items to this panel.
     */
    public IndianSettlementPanel() {
        int[] w = { 10, 0, 0, 10 };
        int[] h = { 10, 0, 5, 0, 5, 0, 10, 0, 10 };
        setLayout(new HIGLayout(w, h));

        okButton = new JButton(Messages.message("ok"));
        okButton.setActionCommand(String.valueOf(OK));
        okButton.addActionListener(this);

        skillLabel = new JLabel();
        highlyWantedLabel = new JLabel();
        otherWantedLabel = new JLabel();

        add(new JLabel(Messages.message("indianSettlement.learnableSkill") + " "), higConst.rc(2, 2));
        add(skillLabel, higConst.rc(2, 3));
        add(new JLabel(Messages.message("indianSettlement.highlyWanted") + " "), higConst.rc(4, 2));
        add(highlyWantedLabel, higConst.rc(4, 3));
        add(new JLabel(Messages.message("indianSettlement.otherWanted") + " "), higConst.rc(6, 2));
        add(otherWantedLabel, higConst.rc(6, 3));
        add(okButton, higConst.rc(8, 3));
    }

    public void requestFocus() {
        okButton.requestFocus();
    }

    /**
     * Initializes the information that is being displayed on this panel. The
     * information displayed will be based on the given settlement.
     * 
     * @param settlement The IndianSettlement whose information should be
     *            displayed.
     */
    public void initialize(IndianSettlement settlement) {
        UnitType skill = settlement.getLearnableSkill();
        String skillName;
        if (skill != null) {
            skillName = skill.getName();
        } else {
            skillName = "indianSettlement.skill" + (settlement.hasBeenVisited() ? "None" : "Unknown");
        }
        skillLabel.setText(Messages.message(skillName));

        GoodsType[] wantedGoods = settlement.getWantedGoods();
        if (wantedGoods[0]!=null)
            highlyWantedLabel.setText(wantedGoods[0].getName());
        if (wantedGoods[1]!=null && wantedGoods[2]!=null)
            otherWantedLabel.setText(wantedGoods[1].getName() + ", " + wantedGoods[2].getName());
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
            switch (Integer.valueOf(command).intValue()) {
            case OK:
                setResponse(new Boolean(true));
                break;
            default:
                logger.warning("Invalid Actioncommand: invalid number.");
            }
        } catch (NumberFormatException e) {
            logger.warning("Invalid Actioncommand: not a number.");
        }
    }
}
