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

import java.util.List;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.resources.ResourceManager;


/**
 * This panel is used to show information about an Indian settlement.
 */
public final class IndianSettlementPanel extends FreeColPanel {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(IndianSettlementPanel.class.getName());


    /**
     * Creates a panel to show information about a native settlement.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param is The {@code IndianSettlement} to display.
     */
    public IndianSettlementPanel(FreeColClient freeColClient,
                                 IndianSettlement is) {
        super(freeColClient, null,
              new MigLayout("wrap 2, gapx 20", "", ""));

        ImageLibrary lib = getImageLibrary();
        JLabel settlementLabel = new JLabel(new ImageIcon(lib
                .getScaledSettlementImage(is)));
        final Player indian = is.getOwner();
        final Player player = getMyPlayer();
        boolean contacted = is.hasContacted(player);
        boolean visited = is.hasVisited(player);
        String text = Messages.message(is.getLocationLabelFor(player))
            + ", "
            + Messages.message(StringTemplate
                .template(is.isCapital()
                    ? "indianSettlementPanel.indianCapital"
                    : "indianSettlementPanel.indianSettlement")
                .addStringTemplate("%nation%", indian.getNationLabel()));
        Tension tension = is.getAlarm(player);
        if (tension != null) text += " (" + Messages.getName(tension) + ")";
        if (is.worthScouting(player)) {
            text += ResourceManager.getString("unscoutedIndianSettlement");
        }
        settlementLabel.setText(text);
        add(settlementLabel);

        final Unit missionary = is.getMissionary();
        if (missionary != null) {
            add(Utility.localizedLabel(missionary.getLabel(Unit.UnitLabelType.NATIONAL),
                new ImageIcon(lib.getSmallUnitImage(missionary)),
                JLabel.CENTER));
        }

        add(Utility.localizedLabel("indianSettlementPanel.learnableSkill"),
            "newline");
        final UnitType skillType = is.getLearnableSkill();
        add(Utility.localizedLabel(is.getLearnableSkillLabel(visited),
                ((visited && skillType != null)
                    ? new ImageIcon(lib.getSmallUnitTypeImage(skillType))
                    : null),
                JLabel.CENTER));

        add(Utility.localizedLabel("indianSettlementPanel.mostHated"),
            "newline");
        final Player mostHated = is.getMostHated();
        add(Utility.localizedLabel(is.getMostHatedLabel(contacted),
                ((contacted && mostHated != null)
                    ? new ImageIcon(lib.getSmallNationImage(mostHated.getNation()))
                    : null),
                JLabel.CENTER));

        GoodsType gt;
        List<StringTemplate> wants = is.getWantedGoodsLabel(0, player);
        add(Utility.localizedLabel("indianSettlementPanel.highlyWanted"),
            "newline");
        JLabel label = Utility.localizedLabel(wants.get(0),
            ((visited && (gt = is.getWantedGoods(0)) != null)
                ? new ImageIcon(lib.getScaledGoodsTypeImage(gt))
                : null),
            JLabel.CENTER);
        if (wants.size() > 1) Utility.localizeToolTip(label, wants.get(1));
        add(label);
        add(Utility.localizedLabel("indianSettlementPanel.otherWanted"),
            "newline");
        final int n = is.getWantedGoodsCount();
        String x = "split " + ((n <= 1) ? "1" : Integer.toString(n-1));
        for (int i = 1; i < n; i++) {
            wants = is.getWantedGoodsLabel(i, player);
            label = Utility.localizedLabel(wants.get(0),
                ((visited && (gt = is.getWantedGoods(i)) != null)
                    ? new ImageIcon(lib.getScaledGoodsTypeImage(gt))
                    : null),
                JLabel.CENTER);
            if (wants.size() > 1) Utility.localizeToolTip(label, wants.get(1));
            add(label, x);
            x = null;
        }

        add(okButton, "newline 20, span, tag ok");

        setSize(getPreferredSize());
    }
}
