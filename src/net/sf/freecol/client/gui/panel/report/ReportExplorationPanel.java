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

import java.awt.Font;
import java.util.Comparator;
import java.util.function.Function;

import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.FontLibrary;
import net.sf.freecol.client.gui.panel.*;
import net.sf.freecol.common.model.Region;
import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * This panel displays the exploration report.
 */
public final class ReportExplorationPanel extends ReportPanel {

    /** Comparator for discovered regions, by descending turn and score. */
    private static final Comparator<Region> regionComparator
        = Comparator.comparingInt((Region r) ->
            r.getDiscoveredIn().getNumber()).reversed()
                .thenComparingInt(Region::getScoreValue).reversed();


    /**
     * The constructor that will add the items to this panel.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    public ReportExplorationPanel(FreeColClient freeColClient) {
        super(freeColClient, "reportExplorationAction");

        // Display Panel
        reportPanel.removeAll();
        reportPanel.setLayout(new MigLayout("wrap 5, fillx", "", ""));

        // Header Row
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
        
        // Content Rows
        // TODO: Display "None" if no contents, though this would be rare.
        for (Region region : transform(getGame().getMap().getRegions(),
                                       isNotNull(Region::getDiscoveredIn),
                                       Function.<Region>identity(),
                                       regionComparator)) {
            reportPanel.add(new JLabel(region.getName()));
            reportPanel.add(Utility.localizedLabel(region.getType().getNameKey()));
            reportPanel.add(Utility.localizedLabel(region.getDiscoveredIn()
                    .getLabel()));
            reportPanel.add(Utility.localizedLabel(region.getDiscoveredBy()
                    .getCountryLabel()));
            reportPanel.add(new JLabel(String.valueOf(region.getScoreValue())));
        }
    }
}
