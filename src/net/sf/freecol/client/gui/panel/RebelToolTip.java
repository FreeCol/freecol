/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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

import javax.swing.JLabel;
import javax.swing.JToolTip;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;


/**
 * This panel provides detailed information about rebels in a colony.
 */
public class RebelToolTip extends JToolTip {

    /**
     * Creates a RebelToolTip.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param gui The <code>GUI</code> to display on.
     * @param colony The <code>Colony</code> for which to display information.
     */
    public RebelToolTip(FreeColClient freeColClient, GUI gui, Colony colony) {
        final Specification spec = colony.getSpecification();
        final int population = colony.getUnitCount();
        final int solPercent = colony.getSoL();
        final int rebels = Colony.calculateRebels(population, solPercent);
        StringTemplate t;

        setLayout(new MigLayout("fillx, wrap 3", "[][right][right]", ""));

        t = StringTemplate.template("colonyPanel.rebelLabel")
                          .addName("%number%", "");
        add(new JLabel(Messages.message(t)));

        add(new JLabel(Integer.toString(rebels)));

        add(new JLabel(solPercent + "%"));

        t = StringTemplate.template("colonyPanel.royalistLabel")
                          .addName("%number%", "");
        add(new JLabel(Messages.message(t)));

        add(new JLabel(Integer.toString(population - rebels)));

        add(new JLabel(colony.getTory() + "%"));

        int libertyProduction = 0;
        for (GoodsType goodsType : spec.getLibertyGoodsTypeList()) {
            add(new JLabel(Messages.message(goodsType.getNameKey())));
            int production = colony.getNetProductionOf(goodsType);
            libertyProduction += production;
            add(new ProductionLabel(freeColClient, gui, goodsType, production),
                "span 2");
        }

        final int liberty = colony.getLiberty();
        final int modulo = liberty % Colony.LIBERTY_PER_REBEL;
        final int width = (int)getPreferredSize().getWidth() - 32;
        FreeColProgressBar progress = new FreeColProgressBar(gui, null, 0, 
            Colony.LIBERTY_PER_REBEL, modulo, libertyProduction);
        progress.setPreferredSize(new Dimension(width, 20));
        add(progress, "span 3");

        double turns100 = -1.0;
        double turns50 = -1.0;
        double turnsNext = -1.0;
        if (libertyProduction > 0) {
            int requiredLiberty = Colony.LIBERTY_PER_REBEL
                * colony.getUnitCount();

            if (liberty < requiredLiberty) {
                turns100 = (requiredLiberty - liberty)
                    / (double)libertyProduction;
            }

            requiredLiberty = requiredLiberty / 2;
            if (liberty < requiredLiberty) {
                turns50 = (requiredLiberty - liberty)
                    / (double)libertyProduction;
            }

            if (rebels < population) {
                requiredLiberty = Colony.LIBERTY_PER_REBEL * (rebels + 1);
                if (liberty < requiredLiberty) {
                    turnsNext = (requiredLiberty - liberty)
                        / (double)libertyProduction;
                }
            }
        }

        final String na = Messages.message("notApplicable.short");
        add(new JLabel(Messages.message("report.nextMember")));
        add(new JLabel((turnsNext < 0) ? na
                : Integer.toString((int)Math.ceil(turnsNext))), "skip");

        add(new JLabel(Messages.message("report.50percent")));
        add(new JLabel((turns50 < 0) ? na
                : Integer.toString((int)Math.ceil(turns50))), "skip");

        add(new JLabel(Messages.message("report.100percent")));
        add(new JLabel((turns100 < 0) ? na
                : Integer.toString((int)Math.ceil(turns100))), "skip");

        final int grow = colony.getPreferredSizeChange();
        if (grow > 0) {
            add(new JLabel(Messages.message("report.changeMore")));
            add(new JLabel(Integer.toString(grow)), "skip");
        } else if (grow < 0) {
            add(new JLabel(Messages.message("report.changeLess")));
            add(new JLabel(Integer.toString(-grow)), "skip");
        }
    }


    public Dimension getPreferredSize() {
        return new Dimension(350, 250);
    }
}
