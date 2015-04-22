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

import java.awt.Dimension;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JToolTip;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.FeatureContainer;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Turn;


/**
 * This panel provides detailed information about rebels in a colony.
 */
public class RebelToolTip extends JToolTip {

    /**
     * Creates a RebelToolTip.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param colony The <code>Colony</code> for which to display information.
     */
    public RebelToolTip(FreeColClient freeColClient, Colony colony) {
        final Specification spec = colony.getSpecification();
        final int population = colony.getUnitCount();
        final int solPercent = colony.getSoL();
        final int rebels = Colony.calculateRebels(population, solPercent);
        final Turn turn = colony.getGame().getTurn();
        StringTemplate t;

        setLayout(new MigLayout("fillx, wrap 3", "[][right][right]", ""));
        // TODO: Calculate this from the size of the components
        setPreferredSize(new Dimension(425,325));

        add(Utility.localizedLabel(StringTemplate
                .template("rebelToolTip.rebelLabel")
                .addName("%number%", "")));

        add(new JLabel(Integer.toString(rebels)));

        add(new JLabel(solPercent + "%"));

        add(Utility.localizedLabel(StringTemplate
                .template("rebelToolTip.royalistLabel")
                .addName("%number%", "")));

        add(new JLabel(Integer.toString(population - rebels)));

        add(new JLabel(colony.getTory() + "%"));

        Set<Modifier> modifiers = colony.getOwner()
            .getModifiers(Modifier.LIBERTY);
        int libertyProduction = 0;
        for (GoodsType goodsType : spec.getLibertyGoodsTypeList()) {
            add(new JLabel(Messages.getName(goodsType)));
            int production = colony.getNetProductionOf(goodsType);
            libertyProduction += production;
            add(new ProductionLabel(freeColClient,
                                    new AbstractGoods(goodsType, production)),
                "span 2");
        }
        libertyProduction = (int)FeatureContainer
            .applyModifiers((float)libertyProduction, turn, modifiers);
        for (Modifier m : modifiers) {
            JLabel[] labels = ModifierFormat.getModifierLabels(m, null, turn);
            for (JLabel j : labels) add(j);
        }

        boolean capped = spec.getBoolean(GameOptions.BELL_ACCUMULATION_CAPPED)
            && colony.getSoL() >= 100;
        final int liberty = colony.getLiberty();
        final int modulo = liberty % Colony.LIBERTY_PER_REBEL;
        final int width = (int)getPreferredSize().getWidth() - 32;
        FreeColProgressBar progress
            = new FreeColProgressBar(null, 0, 
                Colony.LIBERTY_PER_REBEL, modulo,
                ((capped) ? 0 : libertyProduction));
        progress.setPreferredSize(new Dimension(width, 20));
        add(progress, "span 3");

        double turnsNext = -1.0;
        double turns100 = -1.0;
        double turns50 = -1.0;
        if (libertyProduction > 0 && !capped) {
            int requiredLiberty = Colony.LIBERTY_PER_REBEL - modulo;

            turnsNext = (1 + requiredLiberty) / (double)libertyProduction;

            requiredLiberty = Colony.LIBERTY_PER_REBEL * colony.getUnitCount();
            if (liberty < requiredLiberty) {
                turns100 = (1 + requiredLiberty - liberty)
                    / (double)libertyProduction;
            }

            requiredLiberty /= 2;
            if (liberty < requiredLiberty) {
                turns50 = (1 + requiredLiberty - liberty)
                    / (double)libertyProduction;
            }
        }

        final String na = Messages.message("notApplicable");
        add(Utility.localizedLabel("rebelToolTip.nextMember"));
        add(new JLabel((turnsNext < 0) ? na
                : Integer.toString((int)Math.ceil(turnsNext))), "skip");

        add(Utility.localizedLabel("rebelToolTip.50percent"));
        add(new JLabel((turns50 < 0) ? na
                : Integer.toString((int)Math.ceil(turns50))), "skip");

        add(Utility.localizedLabel("rebelToolTip.100percent"));
        add(new JLabel((turns100 < 0) ? na
                : Integer.toString((int)Math.ceil(turns100))), "skip");

        final int grow = colony.getPreferredSizeChange();
        if (grow > 0) {
            add(Utility.localizedLabel("rebelToolTip.changeMore"));
            add(new JLabel(Integer.toString(grow)), "skip");
        } else if (grow < 0) {
            add(Utility.localizedLabel("rebelToolTip.changeLess"));
            add(new JLabel(Integer.toString(-grow)), "skip");
        }
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
