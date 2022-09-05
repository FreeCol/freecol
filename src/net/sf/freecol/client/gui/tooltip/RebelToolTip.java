/**
 *  Copyright (C) 2002-2022   The FreeCol Team
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

package net.sf.freecol.client.gui.tooltip;

import static net.sf.freecol.common.util.CollectionUtils.forEach;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JToolTip;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.ModifierFormat;
import net.sf.freecol.client.gui.label.ProductionLabel;
import net.sf.freecol.client.gui.panel.FreeColProgressBar;
import net.sf.freecol.client.gui.panel.MigPanel;
import net.sf.freecol.client.gui.panel.Utility;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.FeatureContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.common.option.GameOptions;


/**
 * This panel provides detailed information about rebels in a colony.
 */
public class RebelToolTip extends JToolTip {

    /**
     * Creates a RebelToolTip.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param colony The {@code Colony} for which to display information.
     */
    public RebelToolTip(FreeColClient freeColClient, Colony colony) {
        final Specification spec = colony.getSpecification();
        final List<GoodsType> libertyGoods = spec.getLibertyGoodsTypeList();
        final int population = colony.getUnitCount();
        final int solPercent = colony.getSonsOfLiberty();
        final int rebelCount = Colony.calculateRebelCount(population, solPercent);
        final Turn turn = colony.getGame().getTurn();
        
        setLayout(new BorderLayout());
        final MigPanel content = new MigPanel(new MigLayout("fill, wrap 3", "[300px][50px, right][50px, right]", ""));

        content.add(Utility.localizedLabel(StringTemplate
                .template("rebelToolTip.rebelLabel")
                .addName("%number%", "")));

        content.add(new JLabel(Integer.toString(rebelCount)));

        content.add(new JLabel(solPercent + "%"));

        content.add(Utility.localizedLabel(StringTemplate
                .template("rebelToolTip.royalistLabel")
                .addName("%number%", "")));

        content.add(new JLabel(Integer.toString(population - rebelCount)));

        content.add(new JLabel((100 - solPercent) + "%"));

        int libertyProduction = 0;
        for (GoodsType goodsType : libertyGoods) {
            content.add(new JLabel(Messages.getName(goodsType)));
            int production = colony.getNetProductionOf(goodsType);
            libertyProduction += production;
            content.add(new ProductionLabel(freeColClient,
                    new AbstractGoods(goodsType, production)), "span 2");
        }
        libertyProduction = (int) FeatureContainer
                .applyModifiers((float) libertyProduction, turn,
                        colony.getOwner().getModifiers(Modifier.LIBERTY));
        forEach(colony.getOwner().getModifiers(Modifier.LIBERTY), m -> {
                for (JLabel j : ModifierFormat.getModifierLabels(m, null, turn)) {
                    content.add(j);
                }
            });

        boolean capped = spec.getBoolean(GameOptions.BELL_ACCUMULATION_CAPPED)
                && colony.getSonsOfLiberty() >= 100;
        final int liberty = colony.getLiberty();
        final int modulo = liberty % Colony.LIBERTY_PER_REBEL;
        FreeColProgressBar progress
            = new FreeColProgressBar(freeColClient, libertyGoods.get(0), 0,
                                     Colony.LIBERTY_PER_REBEL, modulo,
                                     ((capped) ? 0 : libertyProduction));
        content.add(progress, "span 3, alignx center, height 20:");

        int turnsNext = -1, turns50 = -1, turns100 = -1;
        if (libertyProduction > 0 && !capped) {
            List<Integer> bonus = colony.rebelHelper(libertyProduction);
            turnsNext = bonus.get(0);
            turns50 = bonus.get(1);
            turns100 = bonus.get(2);
        }

        final String na = Messages.message("notApplicable");
        content.add(Utility.localizedLabel("rebelToolTip.nextMember"));
        content.add(new JLabel((turnsNext < 0) ? na
                : String.valueOf((int)Math.ceil(turnsNext))), "skip");

        // Displays the number of turns until 50% of the population is Rebel
        // If the colony has passed 50%, then do not display notice.
        if (turns50 > 0) {
            content.add(Utility.localizedLabel("rebelToolTip.50percent"));
            content.add(new JLabel(String.valueOf((int)Math.ceil(turns50))), "skip");
        }

        // DDisplays the number of turns until 100% of the population is Rebel
        // If the colony has passed 100%, then do not display notice.
        if (turns100 > 0) {
            content.add(Utility.localizedLabel("rebelToolTip.100percent"));
            content.add(new JLabel(String.valueOf((int)Math.ceil(turns100))), "skip");
        }

        final int grow = colony.getPreferredSizeChange();
        if (grow >= Colony.CHANGE_UPPER_BOUND) {
            content.add(Utility.localizedLabel("rebelToolTip.changeMore"));
            content.add(Utility.localizedLabel("many"), "skip");
        } else if (grow >= 0) {
            content.add(Utility.localizedLabel("rebelToolTip.changeMore"));
            content.add(new JLabel(String.valueOf(grow)), "skip");
        } else { // grow < 0
            content.add(Utility.localizedLabel("rebelToolTip.changeLess"));
            content.add(new JLabel(String.valueOf(-grow)), "skip");
        }
        setPreferredSize(content.getPreferredSize());   
        add(content, BorderLayout.CENTER);
    }

    // Override Component

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeNotify() {
        super.removeNotify();

        removeAll();
        setLayout(null);
    }
}
