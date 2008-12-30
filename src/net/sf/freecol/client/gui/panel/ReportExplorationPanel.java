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

import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Region;

import cz.autel.dmi.HIGLayout;

/**
 * This panel displays the Foreign Affairs Report.
 */
public final class ReportExplorationPanel extends ReportPanel implements ActionListener {

    public static final int EXTRA_ROWS = 2;

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
        super(parent, Messages.message("menuBar.report.exploration"));
    }

    /**
     * Prepares this panel to be displayed.
     */
    public void initialize() {
        // Display Panel
        reportPanel.removeAll();

        List<Region> regions = new ArrayList<Region>();
        for (Region region : getCanvas().getClient().getGame().getMap().getRegions()) {
            if (region.getDiscoveredIn() != null) {
                regions.add(region);
            }
        }
        Collections.sort(regions, regionComparator);

        int heights[] = new int[2 * regions.size() + EXTRA_ROWS];
        int widths[] = new int[] { 0, 10, 0, 10, 0, 10, 0, 10, 0 };
        int regionColumn = 1;
        int typeColumn = 3;
        int turnColumn = 5;
        int playerColumn = 7;
        int valueColumn = 9;
        
        int row = 1;

        for (int index = 1; index < heights.length; index += 2) {
            heights[index] = 3;
        }

        JPanel result = new JPanel(new HIGLayout(widths, heights));
        result.setOpaque(false);

        // headline
        result.add(new JLabel(Messages.message("report.exploration.nameOfRegion")),
                   higConst.rc(row, regionColumn));
        result.add(new JLabel(Messages.message("report.exploration.typeOfRegion")),
                   higConst.rc(row, typeColumn));
        result.add(new JLabel(Messages.message("report.exploration.discoveredIn")),
                   higConst.rc(row, turnColumn));
        result.add(new JLabel(Messages.message("report.exploration.discoveredBy")),
                   higConst.rc(row, playerColumn));
        result.add(new JLabel(Messages.message("report.exploration.valueOfRegion")),
                   higConst.rc(row, valueColumn));
        row += 2;

        for (Region region : regions) {
            result.add(new JLabel(region.getName()),
                       higConst.rc(row, regionColumn));
            result.add(new JLabel(region.getTypeName()),
                       higConst.rc(row, typeColumn));
            result.add(new JLabel(region.getDiscoveredIn().toString()),
                       higConst.rc(row, turnColumn));
            result.add(new JLabel(region.getDiscoveredBy().getNationAsString()),
                       higConst.rc(row, playerColumn));
            result.add(new JLabel(String.valueOf(region.getScoreValue())),
                       higConst.rc(row, valueColumn));
            row += 2;
        }            

        reportPanel.add(result);
    }
}
