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

import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JSeparator;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianNationType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;

import net.miginfocom.swing.MigLayout;

/**
 * This panel displays the Native Affairs Advisor.
 */
public final class ReportIndianPanel extends ReportPanel {

    /**
     * The constructor that will add the items to this panel.
     * @param parent The parent of this panel.
     */
    public ReportIndianPanel(Canvas parent) {
        super(parent, Messages.message("reportIndianAction.name"));
        Player player = getMyPlayer();
        reportPanel.setLayout(new MigLayout("wrap 5, fillx", "[]20px[center]", ""));
        for (Player opponent : getGame().getPlayers()) {
            if (opponent.isIndian() && !opponent.isDead() && player.hasContacted(opponent)) {
                buildIndianAdvisorPanel(player, opponent);
            }
        }
        scrollPane.getViewport().setOpaque(false);
        reportPanel.setOpaque(true);
        reportPanel.doLayout();
    }

    /**
     * Describe <code>buildIndianAdvisorPanel</code> method here.
     *
     * @param player a <code>Player</code> value
     * @param opponent a <code>Player</code> value
     */
    private void buildIndianAdvisorPanel(Player player, Player opponent) {

        reportPanel.add(localizedLabel("report.indian.nameOfTribe"));
        reportPanel.add(localizedLabel(opponent.getNationName()), "span 2, wrap");
        reportPanel.add(localizedLabel("report.indian.chieftain"));
        reportPanel.add(new JLabel(Messages.message(opponent.getName())), "span 2, wrap");
        reportPanel.add(localizedLabel("report.indian.typeOfSettlements"));
        reportPanel.add(localizedLabel(opponent.getNationType().getCapitalType().getId() + ".name"),
                        "span 2, wrap");
        reportPanel.add(localizedLabel("report.indian.numberOfSettlements"));
        reportPanel.add(new JLabel(String.valueOf(opponent.getSettlements().size())), "span 2, wrap");
        reportPanel.add(new JLabel(Messages.message("report.indian.tension")+":"));
        reportPanel.add(localizedLabel(opponent.getTension(player).toString()), "span 2, wrap");

        reportPanel.add(new JSeparator(JSeparator.HORIZONTAL), "span, growx");

        int numberOfSettlements = opponent.getIndianSettlements().size();
        if (numberOfSettlements > 0) {
            reportPanel.add(localizedLabel("Settlement"), "newline 10");
            reportPanel.add(localizedLabel("mission"));
            reportPanel.add(localizedLabel("report.indian.tension"));
            reportPanel.add(localizedLabel("report.indian.skillTaught"));
            reportPanel.add(localizedLabel("report.indian.tradeInterests"));
            List<IndianSettlement> settlements = new ArrayList<IndianSettlement>(numberOfSettlements);
            for (IndianSettlement settlement : opponent.getIndianSettlements()) {
                if (settlement.isCapital()) {
                    settlements.add(0, settlement);
                } else {
                    settlements.add(settlement);
                }
            }
            for (IndianSettlement settlement : settlements) {
                boolean known = settlement.getTile().isExplored();
                boolean visited = player.hasVisited(settlement);
                String locationName = Messages.message(settlement.getNameFor(player));
                if (known && settlement.isCapital()) {
                    locationName += "*";
                }
                JButton settlementButton = getLinkButton(locationName, null, settlement.getTile().getId());
                settlementButton.addActionListener(this);
                reportPanel.add(settlementButton, "newline 15");

                JLabel missionLabel = new JLabel();
                Unit missionary = settlement.getMissionary();
                if (missionary != null) {
                    boolean expert = missionary.hasAbility("model.ability.expertMissionary");
                    missionLabel.setIcon(new ImageIcon(getLibrary().getMissionChip(missionary, expert, 1)));
                    String text = Messages.message(StringTemplate.template("model.unit.nationUnit")
                                                   .addStringTemplate("%nation%", missionary.getOwner().getNationName())
                                                   .addStringTemplate("%unit%", Messages.getLabel(missionary)));
                    missionLabel.setToolTipText(text);
                }
                reportPanel.add(missionLabel);

                String messageId = settlement.getShortAlarmLevelMessageId(player);
                reportPanel.add(localizedLabel(messageId));

                JLabel skillLabel = new JLabel();
                UnitType skillType = settlement.getLearnableSkill();
                String skillString;
                if (visited) {
                    if (skillType == null) {
                        skillString = "indianSettlement.skillNone";
                    } else {
                        skillString = skillType.getNameKey();
                        ImageIcon skillImage = getLibrary().getUnitImageIcon(skillType, 0.66);
                        skillLabel.setIcon(skillImage);
                    }
                } else {
                    skillString = "indianSettlement.skillUnknown";
                }
                skillLabel.setText(Messages.message(skillString));
                reportPanel.add(skillLabel);

                GoodsType[] wantedGoods = settlement.getWantedGoods();
                if (visited && wantedGoods[0] != null) {
                    JLabel goodsLabel = localizedLabel(wantedGoods[0].getNameKey());
                    goodsLabel.setIcon(new ImageIcon(getLibrary().getGoodsImage(wantedGoods[0], 0.66)));
                    String split = "split " + String.valueOf(wantedGoods.length);
                    reportPanel.add(goodsLabel, split);
                    for (int i = 1; i < wantedGoods.length; i++) {
                        if (wantedGoods[i] != null) {
                            String sale = player.getLastSaleString(settlement, wantedGoods[i]);
                            goodsLabel = new JLabel(Messages.message(wantedGoods[i].getNameKey())
                                                    + ((sale == null) ? "" : " " + sale));
                            goodsLabel.setIcon(getLibrary().getScaledGoodsImageIcon(wantedGoods[i], 0.5));
                            reportPanel.add(goodsLabel);
                        }
                    }
                } else {
                    reportPanel.add(localizedLabel("indianSettlement.wantedGoodsUnknown"));
                }
            }
        } else {
            reportPanel.add(localizedLabel("report.indian.noKnownSettlements"));
        }
        reportPanel.add(new JSeparator(JSeparator.HORIZONTAL), "newline 10, span, growx");
    }
}
