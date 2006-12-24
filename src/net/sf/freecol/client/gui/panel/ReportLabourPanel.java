package net.sf.freecol.client.gui.panel;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.ImageLibrary;
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
 * This panel displays the Labour Report.
 */
public final class ReportLabourPanel extends ReportPanel implements ActionListener {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
    // This is copied from net.sf.freecol.client.gui.ImageLibrary where it is private 
    private static final int FREE_COLONIST = 0,
                            EXPERT_FARMER = 1,
                            EXPERT_FISHERMAN = 2,
                            EXPERT_FUR_TRAPPER = 3,
                            EXPERT_SILVER_MINER = 4,
                            EXPERT_LUMBER_JACK = 5,
                            EXPERT_ORE_MINER = 6,
                            MASTER_SUGAR_PLANTER = 7,
                            MASTER_COTTON_PLANTER = 8,
                            MASTER_TOBACCO_PLANTER = 9,

                            FIREBRAND_PREACHER = 10,
                            ELDER_STATESMAN = 11,
                        
                            MASTER_CARPENTER = 12,
                            MASTER_DISTILLER = 13,
                            MASTER_WEAVER = 14,
                            MASTER_TOBACCONIST = 15,
                            MASTER_FUR_TRADER = 16,
                            MASTER_BLACKSMITH = 17,
                            MASTER_GUNSMITH = 18,
                        
                            SEASONED_SCOUT_NOT_MOUNTED = 19,
                            HARDY_PIONEER_NO_TOOLS = 20,
                            UNARMED_VETERAN_SOLDIER = 21,
                            JESUIT_MISSIONARY = 22,
                            MISSIONARY_FREE_COLONIST = 23,
                        
                            SEASONED_SCOUT_MOUNTED = 24,
                            HARDY_PIONEER_WITH_TOOLS = 25,
                            FREE_COLONIST_WITH_TOOLS = 26,
                            INDENTURED_SERVANT = 27,
                            PETTY_CRIMINAL = 28,

                            INDIAN_CONVERT = 29,
                            BRAVE = 30,
                        
                            UNARMED_COLONIAL_REGULAR = 31,
                            UNARMED_KINGS_REGULAR = 32,
                        
                            SOLDIER = 33,
                            VETERAN_SOLDIER = 34,
                            COLONIAL_REGULAR = 35,
                            KINGS_REGULAR = 36,
                            UNARMED_DRAGOON = 37,
                            UNARMED_VETERAN_DRAGOON = 38,
                            UNARMED_COLONIAL_CAVALRY = 39,
                            UNARMED_KINGS_CAVALRY = 40,
                            DRAGOON = 41,
                            VETERAN_DRAGOON = 42,
                            COLONIAL_CAVALRY = 43,
                            KINGS_CAVALRY = 44,
                        
                            ARMED_BRAVE = 45,
                            MOUNTED_BRAVE = 46,
                            INDIAN_DRAGOON = 47,
                        
                            CARAVEL = 48,
                            FRIGATE = 49,
                            GALLEON = 50,
                            MAN_O_WAR = 51,
                            MERCHANTMAN = 52,
                            PRIVATEER = 53,
                        
                            ARTILLERY = 54,
                            DAMAGED_ARTILLERY = 55,
                            TREASURE_TRAIN = 56,
                            WAGON_TRAIN = 57,
                        
                            MILKMAID = 58,
                            JESUIT_MISSIONARY_NO_CROSS = 59,
                        
                            UNIT_GRAPHICS_COUNT = 60;

    private final ImageLibrary library;
    
    private int[] unitCount;
    private Hashtable[] unitLocations;

    /**
     * The constructor that will add the items to this panel.
     * @param parent The parent of this panel.
     */
    public ReportLabourPanel(Canvas parent) {
        super(parent, Messages.message("report.labour"));
        this.library = (ImageLibrary) parent.getImageProvider();
    }

    /**
     * Prepares this panel to be displayed.
     */
    public void initialize() {
        // Count Units
        unitCount = new int[Unit.UNIT_COUNT];
        unitLocations = new Hashtable[Unit.UNIT_COUNT];
        for (int index = 0; index < Unit.UNIT_COUNT; index++) {
            unitLocations[index] = new Hashtable();
        }
        Player player = parent.getClient().getMyPlayer();
        Iterator units = player.getUnitIterator();
        while (units.hasNext()) {
            Unit unit = (Unit) units.next();
            int type = unit.getType();
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
                if (unitLocations[type].containsKey(locationName)) {
                    int oldValue = ((Integer) unitLocations[type].get(locationName)).intValue();
                    unitLocations[type].put(locationName, new Integer(oldValue + 1));
                } else {
                    unitLocations[type].put(locationName, new Integer(1));
                }
            }

            unitCount[unit.getType()]++;
        }

        // Display Panel
        reportPanel.removeAll();
        reportPanel.setLayout(new GridLayout(0, 3));
        buildLabourLabel(Unit.FREE_COLONIST,          FREE_COLONIST,            1f); //ImageLibrary.FREE_COLONIST);
        buildLabourLabel(Unit.INDENTURED_SERVANT,     INDENTURED_SERVANT,       1f); //ImageLibrary.INDENTURED_SERVANT);
        buildLabourLabel(Unit.PETTY_CRIMINAL,         PETTY_CRIMINAL,           1f); //ImageLibrary.PETTY_CRIMINAL);
        buildLabourLabel(Unit.INDIAN_CONVERT,         INDIAN_CONVERT,           1f); //ImageLibrary.INDIAN_CONVERT);
        buildLabourLabel(Unit.EXPERT_FARMER,          EXPERT_FARMER,            1f); //ImageLibrary.EXPERT_FARMER);
        buildLabourLabel(Unit.EXPERT_FISHERMAN,       EXPERT_FISHERMAN,         1f); //ImageLibrary.EXPERT_FISHERMAN);
        buildLabourLabel(Unit.MASTER_SUGAR_PLANTER,   MASTER_SUGAR_PLANTER,     1f); //ImageLibrary.MASTER_SUGAR_PLANTER);
        buildLabourLabel(Unit.MASTER_DISTILLER,       MASTER_DISTILLER,         1f); //ImageLibrary.MASTER_DISTILLER);
        buildLabourLabel(Unit.EXPERT_LUMBER_JACK,     EXPERT_LUMBER_JACK,       1f); //ImageLibrary.EXPERT_LUMBER_JACK);
        buildLabourLabel(Unit.MASTER_CARPENTER,       MASTER_CARPENTER,         1f); //ImageLibrary.MASTER_CARPENTER);
        buildLabourLabel(Unit.MASTER_TOBACCO_PLANTER, MASTER_TOBACCO_PLANTER,   1f); //ImageLibrary.MASTER_TOBACCO_PLANTER);
        buildLabourLabel(Unit.MASTER_TOBACCONIST,     MASTER_TOBACCONIST,       1f); //ImageLibrary.MASTER_TOBACCONIST);
        buildLabourLabel(Unit.EXPERT_FUR_TRAPPER,     EXPERT_FUR_TRAPPER,       1f); //ImageLibrary.EXPERT_FUR_TRAPPER);
        buildLabourLabel(Unit.MASTER_FUR_TRADER,      MASTER_FUR_TRADER,        1f); //ImageLibrary.MASTER_FUR_TRADER);
        buildLabourLabel(Unit.MASTER_COTTON_PLANTER,  MASTER_COTTON_PLANTER,    1f); //ImageLibrary.MASTER_COTTON_PLANTER);
        buildLabourLabel(Unit.MASTER_WEAVER,          MASTER_WEAVER,            1f); //ImageLibrary.MASTER_WEAVER);
        buildLabourLabel(Unit.EXPERT_ORE_MINER,       EXPERT_ORE_MINER,         1f); //ImageLibrary.EXPERT_ORE_MINER);
        buildLabourLabel(Unit.MASTER_BLACKSMITH,      MASTER_BLACKSMITH,        1f); //ImageLibrary.MASTER_BLACKSMITH);
        buildLabourLabel(Unit.MASTER_GUNSMITH,        MASTER_GUNSMITH,          1f); //ImageLibrary.MASTER_GUNSMITH);
        buildLabourLabel(Unit.EXPERT_SILVER_MINER,    EXPERT_SILVER_MINER,      1f); //ImageLibrary.EXPERT_SILVER_MINER);
        buildLabourLabel(Unit.HARDY_PIONEER,          HARDY_PIONEER_WITH_TOOLS, 1f); //ImageLibrary.HARDY_PIONEER_WITH_TOOLS);
        buildLabourLabel(Unit.VETERAN_SOLDIER,        VETERAN_SOLDIER,          1f); //ImageLibrary.VETERAN_SOLDIER);
        buildLabourLabel(Unit.SEASONED_SCOUT,         SEASONED_SCOUT_NOT_MOUNTED, 1f); //ImageLibrary.SEASONED_SCOUT_NOT_MOUNTED);
        buildLabourLabel(Unit.JESUIT_MISSIONARY,      JESUIT_MISSIONARY,        1f); //ImageLibrary.JESUIT_MISSIONARY);
        buildLabourLabel(Unit.ELDER_STATESMAN,        ELDER_STATESMAN,          1f); //ImageLibrary.ELDER_STATESMAN);
        buildLabourLabel(Unit.FIREBRAND_PREACHER,     FIREBRAND_PREACHER,       1f); //ImageLibrary.FIREBRAND_PREACHER);
        reportPanel.doLayout();
    }

    /**
     * Builds the button for the given unit.
     * @param unit
     * @param unitIcon
     * @param scale
     */
    private void buildLabourLabel(int unit, int unitIcon, float scale) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);

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
            panel.add(imageLabel);
        }

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);

        int[] widths = {0, 5, 0};
        int[] heights = null;
        int colonyColumn = 1;
        int countColumn = 3;
        int keys = 0;
        if (unitLocations[unit] != null) {
            keys = unitLocations[unit].size();
        }
        if (keys == 0) {
            heights = new int[] {0};
        } else {
            heights = new int[keys + 2];
            for (int index = 0; index < heights.length; index++) {
                heights[index] = 0;
            }
            heights[1] = 5;
        }

        textPanel.setLayout(new HIGLayout(widths, heights));
        HIGConstraints higConst = new HIGConstraints();

        // summary
        int row = 1;
        JLabel unitLabel = new JLabel(Unit.getName(unit)); 
        textPanel.add(unitLabel, higConst.rc(row, colonyColumn));

        if (unitCount[unit] == 0) {
            unitLabel.setForeground(Color.GRAY);
        } else {
            textPanel.add(new JLabel(String.valueOf(unitCount[unit])),
                          higConst.rc(row, countColumn));

            row = 3;
            List keyList = Collections.list(unitLocations[unit].keys());
            Collections.sort(keyList);
            Iterator keyIterator = keyList.iterator();
            while (keyIterator.hasNext()) {
                String name = (String) keyIterator.next();
                JLabel colonyLabel = new JLabel(name);
                colonyLabel.setForeground(Color.GRAY);
                textPanel.add(colonyLabel, higConst.rc(row, colonyColumn));
                JLabel countLabel = new JLabel(unitLocations[unit].get(name).toString());
                countLabel.setForeground(Color.GRAY);
                textPanel.add(countLabel, higConst.rc(row, countColumn));
                row++;
            }
        }

        panel.add(textPanel);
        reportPanel.add(panel);
    }
}
