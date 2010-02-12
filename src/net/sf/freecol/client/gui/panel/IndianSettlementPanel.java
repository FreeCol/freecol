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

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;

import net.miginfocom.swing.MigLayout;

/**
 * This panel is used to show information about an Indian settlement.
 */
public final class IndianSettlementPanel extends FreeColPanel {

    private static final Logger logger = Logger.getLogger(IndianSettlementPanel.class.getName());

    /**
     * The constructor that will add the items to this panel.
     */
    public IndianSettlementPanel(final Canvas canvas, IndianSettlement settlement) {
        
        super(canvas);

        setLayout(new MigLayout("wrap 2, gapx 20", "", ""));
        
        JLabel settlementLabel = new JLabel(canvas.getImageIcon(settlement, false));
        Player indian = settlement.getOwner();
        String text = settlement.getName();
        if (settlement.isCapital()){
            text += ", " + Messages.message("indianCapital", "%nation%", indian.getNationAsString());
        } else {
            text += ", " + Messages.message("indianSettlement", "%nation%", indian.getNationAsString());
        }

        Player player = getMyPlayer();
        boolean contacted = player.hasContacted(indian);
        Tension tension = settlement.getAlarm(player);
        if (!contacted) {
            text += " (" + Messages.message("notContacted") + ")";
        } else if (tension != null) {
            text += " (" + tension.toString() + ")";
        }
        settlementLabel.setText(text);
        add(settlementLabel);

        Unit missionary = settlement.getMissionary();
        if (missionary != null) {
            add(new JLabel(Messages.message("model.unit.nationUnit",
                                            "%nation%", missionary.getOwner().getNationAsString(),
                                            "%unit%", missionary.getName()),
                           canvas.getImageIcon(missionary, true), JLabel.CENTER));
        }

        add(new JLabel(Messages.message("indianSettlement.learnableSkill")), "newline");
        UnitType skill = settlement.getLearnableSkill();
        if (contacted) {
            if (skill == null) {
                if (settlement.hasBeenVisited(player)) {
                    add(new JLabel(Messages.message("indianSettlement.skillNone")));
                } else {
                    add(new JLabel(Messages.message("indianSettlement.skillUnknown")));
                }
            } else {
                add(new JLabel(skill.getName(), canvas.getImageIcon(skill, true), JLabel.CENTER));
            }
        } else {
            add(new JLabel(Messages.message("indianSettlement.skillUnknown")));
        }

        GoodsType[] wantedGoods = settlement.getWantedGoods();
        add(new JLabel(Messages.message("indianSettlement.highlyWanted")), "newline");
        if (wantedGoods.length == 0 || wantedGoods[0] == null) {
            add(new JLabel(Messages.message("indianSettlement.wantedGoodsUnknown")));
        } else {
            add(new JLabel(wantedGoods[0].getName(), canvas.getImageIcon(wantedGoods[0], false),
                           JLabel.CENTER));
        }

        add(new JLabel(Messages.message("indianSettlement.otherWanted")), "newline");
        if (wantedGoods.length <= 1 || wantedGoods[1] == null) {
            add(new JLabel(Messages.message("indianSettlement.wantedGoodsUnknown")));
        } else {
            int i, n = 1;
            for (i = 2; i < wantedGoods.length; i++) {
                if (wantedGoods[i] != null) n++;
            }
            add(new JLabel(wantedGoods[1].getName(), canvas.getImageIcon(wantedGoods[1], false),
                           JLabel.CENTER),
                "split " + Integer.toString(n));
            for (i = 2; i < wantedGoods.length; i++) {
                if (wantedGoods[i] != null) {
                    add(new JLabel(wantedGoods[i].getName(),
                                   canvas.getImageIcon(wantedGoods[i], false),
                                   JLabel.CENTER));
                }
            }
        }

        add(okButton, "newline 20, span, tag ok");

        setSize(getPreferredSize());
    }

}
