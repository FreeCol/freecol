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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.JLabel;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Region;

import net.miginfocom.swing.MigLayout;

/**
 * This panel displays the Foreign Affairs Report.
 */
public final class ReportExplorationPanel extends ReportPanel {

    // only use this for regions that have already been discovered!
    private static final Comparator<Region> regionComparator = new Comparator<Region>() {
        public int compare(Region region1, Region region2) {
            int number1 = region1.getDiscoveredIn().getNumber();
            int number2 = region2.getDiscoveredIn().getNumber();
            if (number1 == number2) {
                return region2.getScoreValue() - region1.getScoreValue();
            } else {
                return number2 - number1;
            }
        }
    };

    /**
     * The constructor that will add the items to this panel.
     * @param parent The parent of this panel.
     */
    public ReportExplorationPanel(Canvas parent) {
        super(parent, Messages.message("reportExplorationAction.name"));

        // Display Panel
        reportPanel.removeAll();

        List<Region> regions = new ArrayList<Region>();
        for (Region region : getGame().getMap().getRegions()) {
            if (region.getDiscoveredIn() != null) {
                regions.add(region);
            }
        }
        Collections.sort(regions, regionComparator);

        reportPanel.setLayout(new MigLayout("wrap 5, fillx", "", ""));

        // headline
        reportPanel.add(new JLabel(Messages.message("report.exploration.nameOfRegion")));
        reportPanel.add(new JLabel(Messages.message("report.exploration.typeOfRegion")));
        reportPanel.add(new JLabel(Messages.message("report.exploration.discoveredIn")));
        reportPanel.add(new JLabel(Messages.message("report.exploration.discoveredBy")));
        reportPanel.add(new JLabel(Messages.message("report.exploration.valueOfRegion")));

        for (Region region : regions) {
            reportPanel.add(new JLabel(region.getName()));
            reportPanel.add(localizedLabel(region.getTypeNameKey()));
            reportPanel.add(localizedLabel(region.getDiscoveredIn().getLabel()));
            reportPanel.add(localizedLabel(region.getDiscoveredBy().getNationName()));
            reportPanel.add(new JLabel(String.valueOf(region.getScoreValue())));
        }
    }
}
