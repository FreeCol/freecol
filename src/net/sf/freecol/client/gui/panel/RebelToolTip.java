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

import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JToolTip;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.Specification;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.GoodsType;

import net.miginfocom.swing.MigLayout;

/**
 * This panel provides detailed information about rebels in a colony.
 */
public class RebelToolTip extends JToolTip {

    /**
     * Creates this RebelToolTip.
     * 
     * @param colony the colony for which to display information
     * @param parent a <code>Canvas</code> value
     */
    public RebelToolTip(Colony colony, Canvas parent) {

        setLayout(new MigLayout("fillx, wrap 3", "[][right][right]", ""));

        int members = colony.getMembers();
        int rebels = colony.getSoL();

        add(new JLabel(Messages.message("colonyPanel.rebelLabel", "%number%", "")));
        add(new JLabel(Integer.toString(members)));
        add(new JLabel(Integer.toString(rebels) + "%"));
        add(new JLabel(Messages.message("colonyPanel.royalistLabel", "%number%", "")));
        add(new JLabel(Integer.toString(colony.getUnitCount() - members)));
        add(new JLabel(Integer.toString(colony.getTory()) + "%"));

        int libertyProduction = 0;
        for (GoodsType goodsType : Specification.getSpecification().getLibertyGoodsTypeList()) {
            add(new JLabel(Messages.message(goodsType.getNameKey())));
            int netProduction = colony.getProductionNetOf(goodsType);
            libertyProduction += netProduction;
            add(new ProductionLabel(goodsType, netProduction, parent), "span 2");
        }

        float turns100 = 0;
        float turns50 = 0;
        float turnsNext = 0;

        if (libertyProduction > 0) {
            int liberty = colony.getLiberty();
            int requiredLiberty = Colony.LIBERTY_PER_REBEL * colony.getUnitCount();

            if (liberty < requiredLiberty) {
                turns100 = (requiredLiberty - liberty) / libertyProduction;
            }

            requiredLiberty = requiredLiberty / 2;
            if (liberty < requiredLiberty) {
                turns50 = (requiredLiberty - liberty) / libertyProduction;
            }

            if (members < colony.getUnitCount()) {
                requiredLiberty  = Colony.LIBERTY_PER_REBEL * (members + 1);
                if (liberty < requiredLiberty) {
                    turnsNext = (requiredLiberty - liberty) / libertyProduction;
                }
            }
        }

        String na = Messages.message("notApplicable.short");
        add(new JLabel(Messages.message("report.nextMember")));
        add(new JLabel(turnsNext == 0 ? na : Integer.toString((int) Math.ceil(turnsNext))), "skip");
        add(new JLabel(Messages.message("report.50percent")));
        add(new JLabel(turns50 == 0 ? na : Integer.toString((int) Math.ceil(turns50))), "skip");
        add(new JLabel(Messages.message("report.100percent")));
        add(new JLabel(turns100 == 0 ? na : Integer.toString((int) Math.ceil(turns100))), "skip");

    }


    public Dimension getPreferredSize() {
        return new Dimension(350, 250);
    }
}


