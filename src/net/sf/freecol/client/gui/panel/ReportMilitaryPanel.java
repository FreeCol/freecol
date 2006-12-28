package net.sf.freecol.client.gui.panel;

import java.awt.event.ActionListener;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Image;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.ImageLibrary;
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

    private final ImageLibrary library;

    /**
     * The constructor that will add the items to this panel.
     * @param parent The parent of this panel.
     */
    public ReportMilitaryPanel(Canvas parent) {
        super(parent, Messages.message("menuBar.report.military"));
        this.library = (ImageLibrary) parent.getImageProvider();
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
                }
            }
        }

        Set keySet = locations.keySet();

        int[] widths = new int[] {0, 12, 0};
        int[] heights = new int[colonies.size() + 2];
        int row = 1;

        int colonyColumn = 1;
        int unitColumn = 3;

        reportPanel.setLayout(new HIGLayout(widths, heights));
        HIGConstraints higConst = new HIGConstraints();

        Player refPlayer = player.getREFPlayer();
        JLabel refLabel = new JLabel(refPlayer.getNationAsString());
        refLabel.setForeground(Color.RED);
        reportPanel.add(refLabel, higConst.rc(row, colonyColumn));

        int[] ref = player.getMonarch().getREF();
        int[] refUnitType = new int[] {
            Monarch.MAN_OF_WAR, Monarch.ARTILLERY,
            Monarch.DRAGOON, Monarch.INFANTRY };
        int[] libraryUnitType = new int[] {
            ImageLibrary.MAN_O_WAR, ImageLibrary.ARTILLERY,
            ImageLibrary.KINGS_CAVALRY, ImageLibrary.KINGS_REGULAR };
        JPanel refPanel = new JPanel(new GridLayout(0,7));
        for (int index = 0; index < refUnitType.length; index++) {
            for (int count = 0; count < ref[refUnitType[index]]; count++) {
                refPanel.add(buildUnitLabel(libraryUnitType[index], 0.66f));
            }
        }
        reportPanel.add(refPanel, higConst.rc(row, unitColumn));


        row += 2;

        colonyIterator = colonyNames.iterator();
        while (colonyIterator.hasNext()) {
            String colony = (String) colonyIterator.next();
            JLabel colonyLabel = new JLabel(colony);
            reportPanel.add(colonyLabel, higConst.rc(row, colonyColumn));
            JPanel unitPanel = new JPanel(new GridLayout(0, 8));
            List unitList = locations.get(colony);
            if (unitList == null) {
                colonyLabel.setForeground(Color.GRAY);
            } else {       
                Collections.sort(unitList, new Comparator<Unit> () {
                    public int compare(Unit unit1, Unit unit2) {
                        return unit2.getType() - unit1.getType();
                    }
                });
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

    /**
     * Builds the button for the given unit.
     * @param unit
     * @param unitIcon
     * @param scale
     */
    private JLabel buildUnitLabel(int unitIcon, float scale) {

        JLabel imageLabel = null;
        if (unitIcon >= 0) {
            ImageIcon icon = library.getUnitImageIcon(unitIcon);
            if (scale != 1) {
              Image image;
              image = icon.getImage();
              int width = (int) (scale * image.getWidth(this));
              int height = (int) (scale * image.getHeight(this));
              image = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
              icon = new ImageIcon(image);
            }
            imageLabel = new JLabel(icon);
        }
        return imageLabel;
    }


}

