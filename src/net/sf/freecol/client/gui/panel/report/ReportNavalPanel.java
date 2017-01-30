/**
 *  Copyright (C) 2002-2017   The FreeCol Team
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

import java.util.List;

import javax.swing.JLabel;
import javax.swing.JSeparator;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.panel.*;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.util.*;


/**
 * This panel displays the Naval Report.
 */
public final class ReportNavalPanel extends ReportUnitPanel {


    /**
     * The constructor that will add the items to this panel.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    public ReportNavalPanel(FreeColClient freeColClient) {
        super(freeColClient, "reportNavalAction", false);
    }


    protected boolean isReportable(UnitType unitType) {
        return unitType.isNaval()
                && unitType.isAvailableTo(getMyPlayer());
    }

    protected boolean isReportable(Unit unit) {
        return unit.isNaval();
    }

    // Implement ReportUnitPanel

    /**
     * {@inheritDoc}
     */
    @Override
    protected void gatherData() {
        for (Unit unit : CollectionUtils.transform(getMyPlayer().getUnits(),
                                                   u -> isReportable(u))) {
            addUnit(unit, "naval");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addREFUnits() {
        final Specification spec = getSpecification();
        final Player player = getMyPlayer();
        final Nation refNation = player.getNation().getREFNation();

        reportPanel.add(new JLabel(Messages.getName(refNation)), SPAN_SPLIT_2);
        reportPanel.add(new JSeparator(JSeparator.HORIZONTAL), "growx");

        List<AbstractUnit> refUnits = player.getREFUnits();
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
        final Player player = getMyPlayer();

        reportPanel.add(Utility.localizedLabel(player.getForcesLabel()), NL_SPAN_SPLIT_2);
        reportPanel.add(new JSeparator(JSeparator.HORIZONTAL), "growx");

        for (UnitType unitType : getSpecification().getUnitTypeList()) {
            if (!isReportable(unitType)) continue;
            AbstractUnit au = new AbstractUnit(unitType,
                                               Specification.DEFAULT_ROLE_ID,
                                               getCount("naval", unitType));
            reportPanel.add(createUnitTypeLabel(au), "sg");
        }
    }
}
