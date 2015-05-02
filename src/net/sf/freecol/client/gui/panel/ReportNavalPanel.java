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

import java.util.List;

import javax.swing.JLabel;
import javax.swing.JSeparator;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;


/**
 * This panel displays the Naval Report.
 */
public final class ReportNavalPanel extends ReportUnitPanel {


    /**
     * The constructor that will add the items to this panel.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public ReportNavalPanel(FreeColClient freeColClient) {
        super(freeColClient, "reportNavalAction", false);
    }


    private boolean reportable(UnitType unitType) {
        return unitType.isNaval()
            && unitType.isAvailableTo(getMyPlayer());
    }

    private boolean reportable(Unit unit) {
        return unit.isNaval();
    }

    // Implement ReportUnitPanel

    /**
     * {@inheritDoc}
     */
    @Override
    protected void gatherData() {
        for (Unit unit : getMyPlayer().getUnits()) {
            if (reportable(unit)) {
                addUnit(unit, "naval");
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addREFUnits() {
        final Specification spec = getSpecification();
        final Nation refNation = getMyPlayer().getNation().getREFNation();

        reportPanel.add(new JLabel(Messages.getName(refNation)),
                        "span, split 2");
        reportPanel.add(new JSeparator(JSeparator.HORIZONTAL), "growx");

        List<AbstractUnit> refUnits = igc().getREFUnits();
        if (refUnits != null) {
            for (AbstractUnit au : refUnits) {
                if (au.getType(spec).isNaval()) {
                    reportPanel.add(createUnitTypeLabel(au), "sg");
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addOwnUnits() {
        final Specification spec = getSpecification();
        final Player player = getMyPlayer();

        reportPanel.add(Utility.localizedLabel(player.getForcesLabel()),
            "newline, span, split 2");
        reportPanel.add(new JSeparator(JSeparator.HORIZONTAL), "growx");

        for (UnitType unitType : getSpecification().getUnitTypeList()) {
            if (!reportable(unitType)) continue;
            AbstractUnit au = new AbstractUnit(unitType,
                                               Specification.DEFAULT_ROLE_ID,
                                               getCount("naval", unitType));
            reportPanel.add(createUnitTypeLabel(au), "sg");
        }
    }
}
