package net.sf.freecol.client.gui.panel;

import java.awt.event.ActionListener;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Location;
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


    //private final ReportProductionPanel militaryReportPanel;
    private Canvas parent;

    /**
     * The constructor that will add the items to this panel.
     * @param parent The parent of this panel.
     */
    public ReportMilitaryPanel(Canvas parent) {
        super(parent, Messages.message("menuBar.report.military"));
        this.parent = parent;

        //reportPanel.setLayout(new BoxLayout(reportPanel, BoxLayout.Y_AXIS));
        //militaryReportPanel = new ReportProductionPanel(Goods.CROSSES, parent);
    }

    /**
     * Prepares this panel to be displayed.
     */
    public void initialize() {
        Player player = parent.getClient().getMyPlayer();
        TreeMap<String, List<Unit>> locations = new TreeMap<String, List<Unit>>();

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

                if (location instanceof WorkLocation) {
                    locationName = ((WorkLocation) location).getColony().getName();
                } else if (location instanceof Europe) {
                    locationName = player.getEurope().toString();
                } else if (location instanceof Tile &&
                           ((Tile) location).getSettlement() != null) {
                    locationName = ((Colony) ((Tile) location).getSettlement()).getName();
                } else if (location instanceof Unit) {
                    locationName = Messages.message("report.atSea");
                } else {
                    locationName = Messages.message("report.onLand");
                }

                if (locationName != null) {
                    List unitList = locations.get(locationName);
                    if (unitList == null) {
                        unitList = new ArrayList();
                        locations.put(locationName, unitList);
                    }
                    unitList.add(unit);
                    //   unitCount[unit.getType()]++;
                }
            }
        }

        Set keySet = locations.keySet();

        int[] widths = new int[] {0, 12, 0};
        int[] heights = new int[keySet.size()];
        int row = 1;
        int colonyColumn = 1;
        int unitColumn = 3;
        reportPanel.setLayout(new HIGLayout(widths, heights));

        Iterator keyIterator = keySet.iterator();
        while (keyIterator.hasNext()) {
            String colony = (String) keyIterator.next();
            reportPanel.add(new JLabel(colony), higConst.rc(row, colonyColumn));
            JPanel unitPanel = new JPanel();
            List unitList = locations.get(colony);
            Collections.sort(unitList, new Comparator<Unit> () {
                public int compare(Unit unit1, Unit unit2) {
                    return unit2.getType() - unit1.getType();
                }
            });
            Iterator unitIterator = unitList.iterator();
            while (unitIterator.hasNext()) {
                Unit unit = (Unit) unitIterator.next();
                unitPanel.add(new UnitLabel(unit, parent));
            }
            reportPanel.add(unitPanel, higConst.rc(row, unitColumn, "l"));
            row++;
        }

    }
}

