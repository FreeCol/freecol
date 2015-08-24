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

import java.awt.Font;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.FontLibrary;
import net.sf.freecol.common.model.Region;


/**
 * This panel displays the exploration report.
 */
public final class ReportExplorationPanel extends ReportPanel {

    // only use this for regions that have already been discovered!
    private static final Comparator<Region> regionComparator = new Comparator<Region>() {
        @Override
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
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public ReportExplorationPanel(FreeColClient freeColClient) {
        super(freeColClient, "reportExplorationAction");

        // Display Panel
        reportPanel.removeAll();

        List<Region> regions = new ArrayList<>();
        for (Region region : getGame().getMap().getRegions()) {
            if (region.getDiscoveredIn() != null) {
                regions.add(region);
            }
        }
        Collections.sort(regions, regionComparator);

        reportPanel.setLayout(new MigLayout("wrap 5, fillx", "", ""));

        /**
         * Header Row
         */
        Font font = FontLibrary.createFont(FontLibrary.FontType.NORMAL,
            FontLibrary.FontSize.TINY, Font.BOLD, getImageLibrary().getScaleFactor());
        JLabel nameOfRegion = Utility.localizedLabel("report.exploration.nameOfRegion");
        nameOfRegion.setFont(font);
        reportPanel.add(nameOfRegion);
        JLabel typeOfRegion = Utility.localizedLabel("report.exploration.typeOfRegion");
        typeOfRegion.setFont(font);
        reportPanel.add(typeOfRegion);
        JLabel discoveredIn = Utility.localizedLabel("report.exploration.discoveredIn");
        discoveredIn.setFont(font);
        reportPanel.add(discoveredIn);
        JLabel discoveredBy = Utility.localizedLabel("report.exploration.discoveredBy");
        discoveredBy.setFont(font);
        reportPanel.add(discoveredBy);
        JLabel valueOfRegion = Utility.localizedLabel("report.exploration.valueOfRegion");
        valueOfRegion.setFont(font);
        reportPanel.add(valueOfRegion);
        
        /**
         * Content Rows
         * 
         * TODO: Display "None" if no contents, though this would be rare.
         */
        for (Region region : regions) {
            reportPanel.add(new JLabel(region.getName()));
            reportPanel.add(Utility.localizedLabel(region.getType()));
            reportPanel.add(Utility.localizedLabel(region.getDiscoveredIn()
                    .getLabel()));
            reportPanel.add(Utility.localizedLabel(region.getDiscoveredBy()
                    .getCountryLabel()));
            reportPanel.add(new JLabel(String.valueOf(region.getScoreValue())));
        }
    }
}
