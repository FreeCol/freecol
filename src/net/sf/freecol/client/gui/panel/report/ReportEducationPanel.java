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

import java.awt.Color;
import java.awt.FlowLayout;
import java.util.List;
import java.util.function.Function;

import javax.swing.JPanel;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.label.UnitLabel;
import net.sf.freecol.client.gui.panel.*;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Unit;
import static net.sf.freecol.common.util.CollectionUtils.*;

import net.miginfocom.swing.MigLayout;


/**
 * This panel displays the Education Report.
 */
public final class ReportEducationPanel extends ReportPanel {

    /**
     * Creates the education report.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    public ReportEducationPanel(FreeColClient freeColClient) {
        super(freeColClient, "reportEducationAction");

        reportPanel.setLayout(new MigLayout("wrap 2, fill",
                                            "[]20[fill, growprio 200]"));
        final Player player = getMyPlayer();
        List<Colony> colonies = player.getColonyList();
        for (Colony colony : colonies) {
            for (Building building : colony.getBuildings()) {
                if (building.canTeach()) {
                    reportPanel.add(createColonyButton(colony),
                                    "newline, split 2, flowy");
                    BuildingPanel bp = new BuildingPanel(freeColClient, building);
                    bp.initialize();
                    reportPanel.add(bp);
                    JPanel teacherPanel = getPanel("report.education.teachers");
                    List<Unit> teachers = transform(colony.getUnits(),
                        u -> building.canAdd(u),
                        Function.<Unit>identity(),
                        Unit.increasingSkillComparator);
                    for (Unit u : teachers) {
                        teacherPanel.add(new UnitLabel(freeColClient, u,
                                                       true, true));
                    }
                    reportPanel.add(teacherPanel, "split 2, flowy, grow");
                    JPanel studentPanel = getPanel("report.education.students");
                    for (Unit unit : colony.getUnitList()) {
                        Unit teacher = find(teachers, u -> unit.canBeStudent(u));
                        if (teacher != null) {
                            UnitLabel ul = new UnitLabel(freeColClient, unit,
                                                         true, true);
                            studentPanel.add(ul);
                            Utility.localizeToolTip(ul, StringTemplate
                                .template("report.education.tooltip")
                                .addNamed("%skill%",
                                    unit.getTeachingType(teacher)));
                        }
                    }
                    reportPanel.add(studentPanel, "grow");
                }
            }
        }
    }

    private JPanel getPanel(String key) {
        JPanel result = new JPanel(new FlowLayout(FlowLayout.LEADING));
        result.setOpaque(false);
        result.setBorder(Utility.localizedBorder(key, Color.GRAY));
        return result;
    }
}
