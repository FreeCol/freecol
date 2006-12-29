package net.sf.freecol.client.gui.panel;

import java.awt.event.ActionListener;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Monarch;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.WorkLocation;

import cz.autel.dmi.*;

/**
 * This panel displays the Military Report.
 */
public final class ReportMilitaryPanel extends ReportPanel implements ActionListener {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2006 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    /**
     * The constructor that will add the items to this panel.
     * @param parent The parent of this panel.
     */
    public ReportMilitaryPanel(Canvas parent) {
        super(parent, Messages.message("menuBar.report.military"));
    }

    /**
     * Prepares this panel to be displayed.
     */
    public void initialize() {
        Player player = parent.getClient().getMyPlayer();

        List colonies = player.getSettlements();
        Collections.sort(colonies, parent.getClient().getClientOptions().getColonyComparator());
        ArrayList colonyNames = new ArrayList();
        Iterator colonyIterator = colonies.iterator();
        while (colonyIterator.hasNext()) {
            colonyNames.add(((Colony) colonyIterator.next()).getName());
        }

        ArrayList otherNames = new ArrayList();

        HashMap<String, List<Unit>> locations = new HashMap<String, List<Unit>>();

        // Display Panel
        reportPanel.removeAll();

        Iterator units = player.getUnitIterator();
        while (units.hasNext()) {
            Unit unit = (Unit) units.next();
            int type = unit.getType();

            if (unit.getType() == Unit.ARTILLERY ||
                unit.getType() == Unit.DAMAGED_ARTILLERY ||
                unit.isArmed()) {

                Location location = unit.getLocation();
                String locationName = null;
                if (unit.getDestination() != null) {
                    locationName = Messages.message("goingTo", new String[][] {{"%location%", unit.getDestination().getLocationName()}});
                } else {
                    locationName = location.getLocationName();
                } 
                if (locationName != null) {
                    List unitList = locations.get(locationName);
                    if (unitList == null) {
                        unitList = new ArrayList();
                        locations.put(locationName, unitList);
                    }
                    unitList.add(unit);
                    if (!(colonyNames.contains(locationName) ||
                          otherNames.contains(locationName))) {
                        otherNames.add(locationName);
                    }
                }
            }
        }

        Collections.sort(otherNames);
        colonyNames.addAll(otherNames);

        int[] widths = new int[] {0, 12, 0};
        int[] heights = new int[colonyNames.size() + 2];
        heights[1] = 12;
        int row = 1;

        int colonyColumn = 1;
        int unitColumn = 3;

        reportPanel.setLayout(new HIGLayout(widths, heights));
        HIGConstraints higConst = new HIGConstraints();

        // REF
        Player refPlayer = player.getREFPlayer();
        int[] ref = player.getMonarch().getREF();
        int[] refUnitType = new int[] {
            Monarch.ARTILLERY,
            Monarch.DRAGOON,
            Monarch.INFANTRY };
        int[] libraryUnitType = new int[] {
            ImageLibrary.ARTILLERY,
            ImageLibrary.KINGS_CAVALRY,
            ImageLibrary.KINGS_REGULAR };
        JPanel refPanel = new JPanel(new GridLayout(0, 12));
        refPanel.setBorder(BorderFactory.createTitledBorder(refPlayer.getNationAsString()));
        for (int index = 0; index < refUnitType.length; index++) {
            for (int count = 0; count < ref[refUnitType[index]]; count++) {
                refPanel.add(buildUnitLabel(libraryUnitType[index], 0.66f));
            }
        }
        reportPanel.add(refPanel, higConst.rcwh(row, colonyColumn, widths.length, 1));

        row += 2;

        colonyIterator = colonyNames.iterator();
        while (colonyIterator.hasNext()) {
            String colony = (String) colonyIterator.next();
            JLabel colonyLabel = new JLabel(colony);
            reportPanel.add(colonyLabel, higConst.rc(row, colonyColumn));
            JPanel unitPanel = new JPanel(new GridLayout(0, 10));
            List unitList = locations.get(colony);
            if (unitList == null) {
                colonyLabel.setForeground(Color.GRAY);
            } else {       
                Collections.sort(unitList, getUnitTypeComparator());
                Iterator unitIterator = unitList.iterator();
                while (unitIterator.hasNext()) {
                    Unit unit = (Unit) unitIterator.next();
                    unitPanel.add(new UnitLabel(unit, parent, true));
                }
                reportPanel.add(unitPanel, higConst.rc(row, unitColumn, "l"));
            }
            row++;
        }

    }
}

