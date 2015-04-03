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

import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JSeparator;
import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.FontLibrary;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.NationSummary;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.resources.ResourceManager;


/**
 * This panel displays the Native Affairs Advisor.
 */
public final class ReportIndianPanel extends ReportPanel {

    private static final String[] headlines = {
        "Settlement",
        "mission",
        "report.indian.tension",
        "report.indian.skillTaught",
        "report.indian.mostHated",
        "report.indian.tradeInterests"
    };


    /**
     * The constructor that will add the items to this panel.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public ReportIndianPanel(FreeColClient freeColClient) {
        super(freeColClient, "reportIndianAction");

        Player player = getMyPlayer();
        reportPanel.setLayout(new MigLayout("wrap 6, fillx, insets 0",
                                            "[]20px[center]", "[top]"));
        boolean needsSeperator = false;
        for (Player opponent : getGame().getLiveNativePlayers(null)) {
            if (player.hasContacted(opponent)) {
                if (needsSeperator) {
                    reportPanel.add(new JSeparator(JSeparator.HORIZONTAL),
                        "newline 20, span, growx, wrap 20");
                }
                buildIndianAdvisorPanel(player, opponent);
                needsSeperator = true;
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
        final NationSummary ns = igc().getNationSummary(opponent);
        List<IndianSettlement> nativeSettlements
            = opponent.getIndianSettlements();
        String numSettlements = String.valueOf(nativeSettlements.size())
            + " / " + String.valueOf(ns.getNumberOfSettlements());

        ImageLibrary lib = getLibrary();
        JLabel villageLabel = new JLabel();
        villageLabel.setIcon(new ImageIcon(ImageLibrary.getSettlementImage(
            opponent.getNationType().getCapitalType(),
            lib.getScalingFactor() * (2f/3f))));
        reportPanel.add(villageLabel, "span, split 2");
        JLabel headline = GUI.localizedLabel(opponent.getNationName());
        headline.setFont(FontLibrary.createFont(FontLibrary.FontType.HEADER, FontLibrary.FontSize.SMALL));
        reportPanel.add(headline, "wrap 20");
        JLabel label = GUI.localizedLabel("report.indian.chieftain");
        Font font = FontLibrary.createFont(FontLibrary.FontType.NORMAL, FontLibrary.FontSize.TINY, Font.BOLD);
        label.setFont(font);
        reportPanel.add(label);
        reportPanel.add(GUI.localizedLabel(opponent.getName()), "left, wrap");
        label = GUI.localizedLabel("report.indian.typeOfSettlements");
        label.setFont(font);
        reportPanel.add(label);
        reportPanel.add(GUI.localizedLabel(Messages.nameKey(opponent.getNationType().getCapitalType().getId())), "left, wrap");
        label = GUI.localizedLabel("report.indian.numberOfSettlements");
        reportPanel.add(label);
        label.setFont(font);
        reportPanel.add(new JLabel(numSettlements), "left, wrap");
        label = GUI.localizedLabel("report.indian.tribeTension");
        reportPanel.add(label);
        label.setFont(font);
        reportPanel.add(GUI.localizedLabel(StringTemplate
                .template("report.indian.tensionStance")
                .add("%tension%", opponent.getTension(player).getKey())
                .addStringTemplate("%stance%", opponent.getStance(player).getLabel())),
            "left, wrap 20");

        if (nativeSettlements.isEmpty()) {
            reportPanel.add(GUI.localizedLabel("report.indian.noKnownSettlements"));
        } else {
            for (String key : headlines) {
                JLabel head = GUI.localizedLabel(key);
                head.setFont(font);
                reportPanel.add(head);
            }
            List<IndianSettlement> settlements
                = new ArrayList<>(nativeSettlements.size());
            for (IndianSettlement settlement : nativeSettlements) {
                if (settlement.isCapital()) {
                    settlements.add(0, settlement);
                } else {
                    settlements.add(settlement);
                }
            }
            for (IndianSettlement settlement : settlements) {
                final Tile tile = settlement.getTile();
                final boolean known = tile.isExplored();
                final boolean contacted = settlement.hasContacted(player);
                final boolean visited = settlement.hasVisited(player);
                // final boolean scouted = settlement.hasScouted(player);
                String locationName
                    = Messages.message(settlement.getLocationLabelFor(player));
                if (known && settlement.isCapital()) {
                    locationName += ResourceManager.getString("indianSettlementChip.capital");
                }
                if (settlement.worthScouting(player)) {
                    locationName += ResourceManager.getString("unscoutedIndianSettlement");
                }
                JButton settlementButton = GUI.getLinkButton(locationName,
                    null, settlement.getTile().getId());
                settlementButton.addActionListener(this);
                reportPanel.add(settlementButton, "newline 15");

                final Unit missionary = settlement.getMissionary();
                JLabel missionLabel = new JLabel("");
                if (missionary != null) {
                    missionLabel.setIcon(new ImageIcon(lib
                            .getMissionChip(missionary.getOwner(),
                                missionary.hasAbility(Ability.EXPERT_MISSIONARY))));
                    GUI.localizeToolTip(missionLabel, Messages
                        .message(missionary.getLabel(Unit.UnitLabelType.NATIONAL)));
                }
                reportPanel.add(missionLabel);

                reportPanel.add(GUI.localizedLabel(settlement
                        .getShortAlarmLevelMessageId(player)));

                final UnitType skillType = settlement.getLearnableSkill();
                JLabel skillLabel;
                if (visited && skillType != null) {
                    skillLabel = new JLabel("");
                    skillLabel.setIcon(new ImageIcon(
                        lib.getSmallUnitImage(skillType)));
                    GUI.localizeToolTip(skillLabel, Messages
                        .message(settlement.getLearnableSkillLabel(visited)));
                } else {
                    skillLabel = GUI.localizedLabel(settlement
                        .getLearnableSkillLabel(visited));
                }
                reportPanel.add(skillLabel);

                reportPanel.add(GUI.localizedLabel(settlement
                        .getMostHatedLabel(contacted)));

                GoodsType[] wantedGoods = settlement.getWantedGoods();
                final int n = (visited) ? settlement.getWantedGoodsAmount()
                    : 1;
                String x = (n > 1) ? "split " + Integer.toString(n) : null;
                for (int i = 0; i < n; i++) {
                    JLabel goodsLabel;
                    if (visited && wantedGoods[i] != null) {
                        goodsLabel = new JLabel("");
                        goodsLabel.setIcon(new ImageIcon(ImageLibrary
                                .getGoodsImage(wantedGoods[i], 2f/3f)));
                        GUI.localizeToolTip(goodsLabel, Messages
                            .message(settlement.getWantedGoodsLabel(i, player)));
                    } else {
                        goodsLabel = GUI.localizedLabel(settlement
                            .getWantedGoodsLabel(i, player));
                    }
                    reportPanel.add(goodsLabel, x);
                    x = null;
                }
            }                
        }
    }
}
