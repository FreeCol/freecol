/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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

import java.util.logging.Logger;

import javax.swing.JLabel;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;

import net.miginfocom.swing.MigLayout;

/**
 * This panel is used to show information about an Indian settlement.
 */
public final class IndianSettlementPanel extends FreeColPanel {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(IndianSettlementPanel.class.getName());

    /**
     * The constructor that will add the items to this panel.
     */
    public IndianSettlementPanel(final Canvas canvas, IndianSettlement settlement) {
        
        super(canvas);

        setLayout(new MigLayout("wrap 2, gapx 20", "", ""));
        
        JLabel settlementLabel = new JLabel(canvas.getImageIcon(settlement, false));
        Player indian = settlement.getOwner();
        Player player = getMyPlayer();
        boolean visited = player.hasVisited(settlement);
        String text = Messages.message(settlement.getNameFor(player)) + ", "
            + Messages.message(StringTemplate.template(settlement.isCapital()
                                                       ? "indianCapital"
                                                       : "indianSettlement")
                               .addStringTemplate("%nation%", indian.getNationName()));
        String messageId = settlement.getShortAlarmLevelMessageId(player);
        text += " (" + Messages.message(messageId) + ")";
        settlementLabel.setText(text);
        add(settlementLabel);

        Unit missionary = settlement.getMissionary();
        if (missionary != null) {
            String missionaryName = Messages.message(StringTemplate.template("model.unit.nationUnit")
                                                     .addStringTemplate("%nation%", missionary.getOwner().getNationName())
                                                     .addStringTemplate("%unit%", missionary.getLabel()));
            add(new JLabel(missionaryName, canvas.getImageIcon(missionary, true), JLabel.CENTER));
        }

        add(localizedLabel("indianSettlement.learnableSkill"), "newline");
        UnitType skillType = settlement.getLearnableSkill();
        if (visited) {
            if (skillType == null) {
                add(localizedLabel("indianSettlement.skillNone"));
            } else {
                add(new JLabel(Messages.message(skillType.getNameKey()),
                               canvas.getImageIcon(skillType, true), JLabel.CENTER));
            }
        } else {
            add(localizedLabel("indianSettlement.skillUnknown"));
        }

        GoodsType[] wantedGoods = settlement.getWantedGoods();
        String sale;
        add(localizedLabel("indianSettlement.highlyWanted"), "newline");
        if (!visited) {
            add(localizedLabel("indianSettlement.wantedGoodsUnknown"));
        } else {
            sale = player.getLastSaleString(settlement, wantedGoods[0]);
            add(new JLabel(Messages.message(wantedGoods[0].getNameKey())
                           + ((sale == null) ? "" : " " + sale),
                           canvas.getImageIcon(wantedGoods[0], false),
                           JLabel.CENTER));
        }

        add(localizedLabel("indianSettlement.otherWanted"), "newline");
        if (!visited) {
            add(localizedLabel("indianSettlement.wantedGoodsUnknown"));
        } else {
            int i, n = 1;
            for (i = 2; i < wantedGoods.length; i++) {
                if (wantedGoods[i] != null) n++;
            }
            sale = player.getLastSaleString(settlement, wantedGoods[1]);
            add(new JLabel(Messages.message(wantedGoods[1].getNameKey())
                           + ((sale == null) ? "" : " " + sale),
                           canvas.getImageIcon(wantedGoods[1], false),
                           JLabel.CENTER),
                "split " + Integer.toString(n));
            for (i = 2; i < wantedGoods.length; i++) {
                if (wantedGoods[i] != null) {
                    sale = player.getLastSaleString(settlement,wantedGoods[i]);
                    add(new JLabel(Messages.message(wantedGoods[i].getNameKey())
                                   + ((sale == null) ? "" : " " + sale),
                                   canvas.getImageIcon(wantedGoods[i], false),
                                   JLabel.CENTER));
                }
            }
        }

        add(okButton, "newline 20, span, tag ok");

        setSize(getPreferredSize());
    }

}
