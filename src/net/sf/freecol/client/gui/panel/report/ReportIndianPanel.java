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

package net.sf.freecol.client.gui.panel.report;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JSeparator;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.FontLibrary;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.panel.*;
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
import net.sf.freecol.common.util.*;


/**
 * This panel displays the Native Affairs Advisor.
 */
public final class ReportIndianPanel extends ReportPanel {

    private static final String[] headlines = {
        "settlement",
        "mission",
        "report.indian.tension",
        "skillTaught",
        "report.indian.mostHated",
        "report.indian.tradeInterests"
    };


    /**
     * The constructor that will add the items to this panel.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    public ReportIndianPanel(FreeColClient freeColClient) {
        super(freeColClient, "reportIndianAction");

        Player player = getMyPlayer();
        reportPanel.setLayout(new MigLayout("wrap 6, fillx, insets 0",
                                            "[]20px[center]", "[top]"));
        boolean needsSeperator = false;
        for (Player opponent : CollectionUtils.transform(getGame().getLiveNativePlayers(),
                                         p -> player.hasContacted(p))) {
            if (needsSeperator) {
                reportPanel.add(new JSeparator(JSeparator.HORIZONTAL),
                    "newline 20, span, growx, wrap 20");
            }
            buildIndianAdvisorPanel(player, opponent);
            needsSeperator = true;
        }
        scrollPane.getViewport().setOpaque(false);
        reportPanel.setOpaque(true);
        reportPanel.doLayout();
    }

    private void buildIndianAdvisorPanel(Player player, Player opponent) {
        final NationSummary ns = igc().nationSummary(opponent);
        List<IndianSettlement> nativeSettlements
            = opponent.getIndianSettlementList();
        String numSettlements = String.valueOf(nativeSettlements.size())
            + " / " + ((ns == null) ? "?"
                : String.valueOf(ns.getNumberOfSettlements()));

        ImageLibrary lib = getImageLibrary();
        JLabel villageLabel = new JLabel();
        villageLabel.setIcon(new ImageIcon(lib
                .getScaledSettlementTypeImage(opponent.getNationType().getCapitalType())));
        reportPanel.add(villageLabel, SPAN_SPLIT_2);
        JLabel headline = Utility.localizedLabel(opponent.getNationLabel());
        headline.setFont(FontLibrary.createFont(FontLibrary.FontType.NORMAL,
            FontLibrary.FontSize.SMALL, Font.BOLD, lib.getScaleFactor()));
        reportPanel.add(headline, "wrap 20, aligny center");
        JLabel label = Utility.localizedLabel("report.indian.chieftain");
        Font font = FontLibrary.createFont(FontLibrary.FontType.NORMAL,
            FontLibrary.FontSize.TINY, Font.BOLD, lib.getScaleFactor());
        label.setFont(font);
        reportPanel.add(label);
        reportPanel.add(Utility.localizedLabel(opponent.getName()), "left, wrap");
        label = Utility.localizedLabel("report.indian.typeOfSettlements");
        label.setFont(font);
        reportPanel.add(label);
        reportPanel.add(Utility.localizedLabel(Messages.nameKey(opponent
                .getNationType().getCapitalType().getId())), "left, wrap");
        label = Utility.localizedLabel("report.indian.numberOfSettlements");
        reportPanel.add(label);
        label.setFont(font);
        reportPanel.add(new JLabel(numSettlements), "left, wrap");
        label = Utility.localizedLabel("report.indian.tribeTension");
        reportPanel.add(label);
        label.setFont(font);
        reportPanel.add(Utility.localizedLabel(StringTemplate
                .template("report.indian.tensionStance")
                .addNamed("%tension%", opponent.getTension(player))
                .addNamed("%stance%", opponent.getStance(player))),
            "left, wrap 20");

        if (nativeSettlements.isEmpty()) {
            reportPanel.add(Utility
                    .localizedLabel("report.indian.noKnownSettlements"));
        } else {
            for (String key : headlines) {
                JLabel head = Utility.localizedLabel(key);
                head.setFont(font);
                reportPanel.add(head);
            }
            List<IndianSettlement> settlements
                = new ArrayList<>(nativeSettlements.size());
            for (IndianSettlement is : nativeSettlements) {
                if (is.isCapital()) {
                    settlements.add(0, is);
                } else {
                    settlements.add(is);
                }
            }
            for (IndianSettlement is : settlements) {
                final Tile tile = is.getTile();
                final boolean known = tile.isExplored();
                final boolean contacted = is.hasContacted(player);
                final boolean visited = is.hasVisited(player);
                // final boolean scouted = is.hasScouted(player);
                String locationName
                    = Messages.message(is.getLocationLabelFor(player));
                if (known && is.isCapital()) {
                    locationName += ResourceManager
                            .getString("indianSettlementChip.capital");
                }
                if (is.worthScouting(player)) {
                    locationName += ResourceManager
                            .getString("unscoutedIndianSettlement");
                }
                JButton settlementButton = Utility.getLinkButton(locationName,
                    null, is.getTile().getId());
                Utility.localizeToolTip(settlementButton, is.getTile()
                    .getLocationLabelFor(player));
                settlementButton.addActionListener(this);
                reportPanel.add(settlementButton, "newline 15");

                final Unit missionary = is.getMissionary();
                JLabel missionLabel = new JLabel("");
                if (missionary != null) {
                    BufferedImage dummy = new BufferedImage(1, 1,
                        BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g = dummy.createGraphics();
                    missionLabel.setIcon(new ImageIcon(lib
                            .getMissionChip(g, missionary.getOwner(),
                                missionary.hasAbility(Ability
                                        .EXPERT_MISSIONARY))));
                    g.dispose();
                    Utility.localizeToolTip(missionLabel, Messages
                        .message(missionary.getLabel(Unit.UnitLabelType
                                .NATIONAL)));
                }
                reportPanel.add(missionLabel);

                reportPanel.add(Utility
                    .localizedLabel(is.getAlarmLevelKey(player)));

                final UnitType skillType = is.getLearnableSkill();
                JLabel skillLabel;
                if (visited && skillType != null) {
                    skillLabel = new JLabel("");
                    skillLabel.setIcon(new ImageIcon(lib
                            .getSmallUnitTypeImage(skillType)));
                    Utility.localizeToolTip(skillLabel, Messages
                        .message(is.getLearnableSkillLabel(visited)));
                } else {
                    skillLabel = Utility.localizedLabel(is
                        .getLearnableSkillLabel(visited));
                }
                reportPanel.add(skillLabel);

                reportPanel.add(Utility.localizedLabel(is
                        .getMostHatedLabel(contacted)));

                final int n = (visited) ? is.getWantedGoodsCount() : 1;
                String x = (n > 1) ? "split " + Integer.toString(n) : null;
                for (int i = 0; i < n; i++) {
                    JLabel goodsLabel;
                    GoodsType gt;
                    List<StringTemplate> gl = is.getWantedGoodsLabel(i, player);
                    if (visited && (gt = is.getWantedGoods(i)) != null) {
                        goodsLabel = new JLabel("");
                        goodsLabel.setIcon(new ImageIcon(lib
                                .getSmallGoodsTypeImage(gt)));
                        Utility.localizeToolTip(goodsLabel,
                            Messages.message(gl.get(0)));
                    } else {
                        goodsLabel = Utility.localizedLabel(gl.get(0));
                    }
                    reportPanel.add(goodsLabel, x);
                    x = null;
                }
            }                
        }
    }
}
