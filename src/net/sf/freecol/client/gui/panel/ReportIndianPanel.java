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

import java.awt.GridLayout;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianNationType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.UnitType;

import net.miginfocom.swing.MigLayout;

/**
 * This panel displays the Foreign Affairs Report.
 */
public final class ReportIndianPanel extends ReportPanel {

    /**
     * The constructor that will add the items to this panel.
     * @param parent The parent of this panel.
     */
    public ReportIndianPanel(Canvas parent) {
        super(parent, Messages.message("menuBar.report.indian"));
        Player player = getMyPlayer();
        reportPanel.setLayout(new GridLayout(0, 1));
        for (Player opponent : getGame().getPlayers()) {
            if (opponent.isIndian() && !opponent.isDead() && player.hasContacted(opponent)) {
                reportPanel.add(buildIndianAdvisorPanel(player, opponent));
            }
        }
        reportPanel.doLayout();
    }

    /**
     * Describe <code>buildIndianAdvisorPanel</code> method here.
     *
     * @param player a <code>Player</code> value
     * @param opponent a <code>Player</code> value
     */
    private JPanel buildIndianAdvisorPanel(Player player, Player opponent) {

        JPanel result = new JPanel(new MigLayout("wrap 4", "[]20px[]", ""));

        result.add(new JLabel(Messages.message("report.indian.nameOfTribe")));
        result.add(new JLabel(opponent.getNationAsString()), "wrap");
        result.add(new JLabel(Messages.message("report.indian.chieftain")));
        result.add(new JLabel(opponent.getName()), "wrap");
        result.add(new JLabel(Messages.message("report.indian.typeOfSettlements")));
        result.add(new JLabel(((IndianNationType) opponent.getNationType()).getSettlementTypeAsString()), "wrap");
        result.add(new JLabel(Messages.message("report.indian.numberOfSettlements")));
        result.add(new JLabel(String.valueOf(opponent.getSettlements().size())), "wrap");
        result.add(new JLabel(Messages.message("report.indian.tension")+":"));
        result.add(new JLabel(opponent.getTension(player).toString()), "wrap");

        result.add(new JSeparator(JSeparator.HORIZONTAL), "span, growx");
        result.add(new JLabel(Messages.message("Settlement")), "newline 10");
        result.add(new JLabel(Messages.message("report.indian.skillTaught")));
        result.add(new JLabel(Messages.message("report.indian.tension")));
        result.add(new JLabel(Messages.message("report.indian.tradeInterests")));

        for (IndianSettlement settlement : opponent.getIndianSettlements()) {
            String locationName = settlement.getName()
                + ((settlement.isCapital()) ? "*" : "")
                + ((settlement.getMissionary() != null) ? "+" : "")
                + " (" + settlement.getTile().getX()
                + ", " + settlement.getTile().getY() + ")";
            result.add(new JLabel(locationName));

            JLabel skillLabel = new JLabel();
            UnitType skillType = settlement.getLearnableSkill();
            String skill;
            if (skillType != null) {
                skill = skillType.getName();
                ImageIcon skillImage = getLibrary().getUnitImageIcon(skillType);
                skillLabel.setIcon(getLibrary().getScaledImageIcon(skillImage, 0.66f));
            } else if (settlement.hasBeenVisited(player)) {
                skill = Messages.message("indianSettlement.skillNone");
            } else {
                skill = Messages.message("indianSettlement.skillUnknown");
            }
            skillLabel.setText(skill);
            result.add(skillLabel);

            Tension tension = settlement.getAlarm(player);
            result.add(new JLabel((tension == null)
                                  ? Messages.message("indianSettlement.tensionUnknown")
                                  : tension.toString()));

            GoodsType[] wantedGoods = settlement.getWantedGoods();
            if (wantedGoods[0] == null) {
                result.add(new JLabel(Messages.message("indianSettlement.wantedGoodsUnknown")));
            } else {
                JLabel goodsLabel = new JLabel(wantedGoods[0].getName());
                goodsLabel.setIcon(getLibrary().getScaledImageIcon(getLibrary().getGoodsImageIcon(wantedGoods[0]), 0.66f));
                String split = "split " + String.valueOf(wantedGoods.length);
                result.add(goodsLabel, split);
                for (int i = 1; i < wantedGoods.length; i++) {
                    if (wantedGoods[i] != null) {
                        goodsLabel = new JLabel(wantedGoods[i].getName());
                        goodsLabel.setIcon(getLibrary().getScaledImageIcon(getLibrary().getGoodsImageIcon(wantedGoods[i]), 0.5f));
                        result.add(goodsLabel);
                    }
                }
            }
        }
        result.add(new JSeparator(JSeparator.HORIZONTAL), "newline 20, span, growx");
        return result;
    }
}
