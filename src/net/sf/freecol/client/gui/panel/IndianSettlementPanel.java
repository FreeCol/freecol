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

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;

import net.miginfocom.swing.MigLayout;

/**
 * This panel is used to show information about an Indian settlement.
 */
public final class IndianSettlementPanel extends FreeColDialog<Boolean> implements ActionListener {

    private static final Logger logger = Logger.getLogger(IndianSettlementPanel.class.getName());

    private static final int OK = 0;

    private final JButton okButton;


    /**
     * The constructor that will add the items to this panel.
     */
    public IndianSettlementPanel(Canvas canvas, IndianSettlement settlement) {
        
        super(canvas);

        setLayout(new MigLayout("wrap 2, gapx 20", "", ""));
        
        okButton = new JButton(Messages.message("ok"));
        okButton.setActionCommand(String.valueOf(OK));
        okButton.addActionListener(this);

        JLabel settlementLabel = new JLabel(getCanvas().getImageIcon(settlement, false));
        String text = settlement.getLocationName();
        Tension tension = settlement.getAlarm(getCanvas().getClient().getMyPlayer());
        if (tension != null) {
            text += " (" + tension.toString() + ")";
        } else if (!getCanvas().getClient().getMyPlayer().hasContacted(settlement.getOwner())) {
            text += " (" + Messages.message("notContacted") + ")";
        }
        settlementLabel.setText(text);
        add(settlementLabel);

        Unit missionary = settlement.getMissionary();
        if (missionary != null) {
            add(new JLabel(Messages.message("model.unit.nationUnit",
                                            "%nation%", missionary.getOwner().getNationAsString(),
                                            "%unit%", missionary.getName()),
                           getCanvas().getImageIcon(missionary, true), JLabel.CENTER));
        }

        add(new JLabel(Messages.message("indianSettlement.learnableSkill")), "newline");
        UnitType skill = settlement.getLearnableSkill();
        if (skill == null) {
            if (settlement.hasBeenVisited()) {
                add(new JLabel(Messages.message("indianSettlement.skillNone")));
            } else {
                add(new JLabel(Messages.message("indianSettlement.skillUnknown")));
            }
        } else {
            add(new JLabel(skill.getName(), getCanvas().getImageIcon(skill, true), JLabel.CENTER));
        }

        GoodsType[] wantedGoods = settlement.getWantedGoods();
        add(new JLabel(Messages.message("indianSettlement.highlyWanted")), "newline");
        if (wantedGoods.length > 0 && wantedGoods[0] != null) {
            add(new JLabel(wantedGoods[0].getName(), getCanvas().getImageIcon(wantedGoods[0], false),
                           JLabel.CENTER));
        }

        add(new JLabel(Messages.message("indianSettlement.otherWanted")), "newline");
        if (wantedGoods.length > 1 && wantedGoods[1] != null) {
            add(new JLabel(wantedGoods[1].getName(), getCanvas().getImageIcon(wantedGoods[1], false),
                           JLabel.CENTER),
                "split " + (wantedGoods.length - 1));
            for (int index = 2; index < wantedGoods.length; index++) {
                add(new JLabel(wantedGoods[index].getName(), 
                               getCanvas().getImageIcon(wantedGoods[index], false),
                               JLabel.CENTER));
            }
        }

        add(okButton, "newline 20, span, tag ok");

        setSize(getPreferredSize());
    }

    public void requestFocus() {
        okButton.requestFocus();
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
                setResponse(Boolean.TRUE);
                break;
            default:
                logger.warning("Invalid Actioncommand: invalid number.");
            }
        } catch (NumberFormatException e) {
            logger.warning("Invalid Actioncommand: not a number.");
        }
    }
}
