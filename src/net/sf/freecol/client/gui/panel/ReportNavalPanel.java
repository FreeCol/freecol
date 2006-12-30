package net.sf.freecol.client.gui.panel;

import java.awt.event.ActionListener;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Monarch;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;

import cz.autel.dmi.*;

/**
 * This panel displays the Naval Report.
 */
public final class ReportNavalPanel extends ReportPanel implements ActionListener {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2006 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    /**
     * The constructor that will add the items to this panel.
     * @param parent The parent of this panel.
     */
    public ReportNavalPanel(Canvas parent) {
        super(parent, Messages.message("menuBar.report.naval"));
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

        int capacity = 0;
        Iterator units = player.getUnitIterator();
        while (units.hasNext()) {
            Unit unit = (Unit) units.next();
            int type = unit.getType();

            if (unit.isNaval()) {

                capacity += unit.getInitialSpaceLeft();
                Location location = unit.getLocation();
                String locationName = null;
                if (unit.getState() == Unit.TO_AMERICA) {
                    locationName = Messages.message("goingToAmerica");
                } else if (unit.getState() == Unit.TO_EUROPE) {
                    locationName = Messages.message("goingToEurope");
                } else if (unit.getDestination() != null) {
                    locationName = Messages.message("sailingTo", new String[][] {{"%location%", unit.getDestination().getLocationName()}});
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
        int[] heights = new int[locations.keySet().size() + 2];
        heights[1] = 12;
        int row = 1;

        int colonyColumn = 1;
        int unitColumn = 3;

        reportPanel.setLayout(new HIGLayout(widths, heights));
        HIGConstraints higConst = new HIGConstraints();

        // REF
        Player refPlayer = player.getREFPlayer();
        int[] ref = player.getMonarch().getREF();
        JPanel refPanel = new JPanel(new GridLayout(0, 7));
        refPanel.setBorder(BorderFactory.createTitledBorder(refPlayer.getNationAsString()));
        for (int count = 0; count < ref[Monarch.MAN_OF_WAR]; count++) {
            refPanel.add(buildUnitLabel(ImageLibrary.MAN_O_WAR, 0.66f));
        }
        reportPanel.add(refPanel, higConst.rc(row, unitColumn));

        row += 2;

        Iterator locationIterator = colonyNames.iterator();
        while (locationIterator.hasNext()) {
            String location = (String) locationIterator.next();
            List unitList = locations.get(location);
            if (unitList != null) {
                JLabel locationLabel = new JLabel(location);
                reportPanel.add(locationLabel, higConst.rc(row, colonyColumn));
                JPanel unitPanel = new JPanel(new GridLayout(0, 7));
                Collections.sort(unitList, getUnitTypeComparator());
                Iterator unitIterator = unitList.iterator();
                while (unitIterator.hasNext()) {
                    UnitLabel unitLabel = new UnitLabel((Unit) unitIterator.next(), parent, true);
                    // this is necessary because UnitLabel deselects carriers
                    unitLabel.setSelected(true);
                    unitPanel.add(unitLabel);
                }
                reportPanel.add(unitPanel, higConst.rc(row, unitColumn, "l"));
                locations.remove(location);
                row++;
            }
        }

        

    }
}

