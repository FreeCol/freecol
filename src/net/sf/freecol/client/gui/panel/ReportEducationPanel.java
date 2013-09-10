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

import java.awt.Color;
import java.awt.FlowLayout;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Unit;


/**
 * This panel displays the Education Report.
 */
public final class ReportEducationPanel extends ReportPanel {


    /**
     * Creates the education report.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public ReportEducationPanel(FreeColClient freeColClient) {
        super(freeColClient, Messages.message("reportEducationAction.name"));

        reportPanel.setLayout(new MigLayout("wrap 2, fill",
                                            "[]20[fill, growprio 200]"));
        List<Colony> colonies = freeColClient.getMySortedColonies();
        for (Colony colony : colonies) {
            for (Building building : colony.getBuildings()) {
                if (building.canTeach()) {
                    int maxSkill = Unit.UNDEFINED;
                    reportPanel.add(createColonyButton(colony), "newline, split 2, flowy");
                    BuildingPanel bp = new BuildingPanel(getFreeColClient(), building);
                    bp.initialize();
                    reportPanel.add(bp);
                    JPanel teacherPanel = getPanel("report.education.teachers");
                    for (Unit unit : colony.getUnitList()) {
                        if (building.canAdd(unit)) {
                            teacherPanel.add(new UnitLabel(getFreeColClient(), unit, true, true));
                            maxSkill = Math.max(maxSkill, unit.getType().getSkill());
                        }
                    }
                    reportPanel.add(teacherPanel, "split 2, flowy, grow");
                    JPanel studentPanel = getPanel("report.education.students");
                    for (Unit unit : colony.getUnitList()) {
                        if (unit.getType().getEducationUnit(maxSkill) != null) {
                            studentPanel.add(new UnitLabel(getFreeColClient(), unit, true, true));
                        }
                    }
                    reportPanel.add(studentPanel, "grow");
                }
            }
        }


    }


    private JPanel getPanel(String title) {
        JPanel result = new JPanel(new FlowLayout(FlowLayout.LEFT));
        result.setOpaque(false);
        result.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY, 1),
                                                          Messages.message(title)));
        return result;
    }
}
