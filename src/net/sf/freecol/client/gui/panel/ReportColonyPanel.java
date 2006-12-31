package net.sf.freecol.client.gui.panel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Color;
import java.awt.GridLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Unit;

import cz.autel.dmi.HIGLayout;

/**
 * This panel displays the Colony Report.
 */
public final class ReportColonyPanel extends ReportPanel implements ActionListener {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2006 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private List<Settlement> colonies;

    /**
     * The constructor that will add the items to this panel.
     * @param parent The parent of this panel.
     */
    public ReportColonyPanel(Canvas parent) {
        super(parent, Messages.message("menuBar.report.colony"));
    }

    /**
     * Prepares this panel to be displayed.
     */
    public void initialize() {
        Player player = parent.getClient().getMyPlayer();
        colonies = player.getSettlements();

        // Display Panel
        reportPanel.removeAll();

        int rowsPerColony = 4;
        int separator = 24;
        int widths[] = new int[] {0, 12, 0};
        int heights[] = new int[colonies.size() * rowsPerColony];
        for (int i = 0; i < colonies.size(); i++) {
            heights[i * rowsPerColony + 3] = separator;
        }

        reportPanel.setLayout(new HIGLayout(widths, heights));

        int row = 1;
        int colonyColumn = 1;
        int panelColumn = 3;
        int colonyIndex = 0;
        Collections.sort(colonies, parent.getClient().getClientOptions().getColonyComparator());
        Iterator colonyIterator = colonies.iterator();
        while (colonyIterator.hasNext()) {
            Colony colony = (Colony) colonyIterator.next();

            // colonyLabel
            JButton colonyButton = new JButton(colony.getName());
            colonyButton.setActionCommand(String.valueOf(colonyIndex));
            colonyButton.addActionListener(this);
            reportPanel.add(colonyButton, higConst.rc(row, colonyColumn));

            // units
            JPanel unitPanel = new JPanel(new GridLayout(0, 10));
            ArrayList<Unit> unitList = new ArrayList<Unit>();
            Iterator unitIterator = colony.getUnitIterator();
            while (unitIterator.hasNext()) {
                unitList.add((Unit) unitIterator.next());
            }
            Collections.sort(unitList, getUnitTypeComparator());
            unitIterator = unitList.iterator();
            while (unitIterator.hasNext()) {
                Unit unit = (Unit) unitIterator.next();
                UnitLabel unitLabel = new UnitLabel(unit, parent, true, true);
                unitPanel.add(unitLabel);
            }
            reportPanel.add(unitPanel, higConst.rc(row, panelColumn));
            row++;
            
            // production
            JPanel goodsPanel = new JPanel(new GridLayout(0, 10));
            for (int goodsType = 0; goodsType < Goods.NUMBER_OF_ALL_TYPES; goodsType++) {
                int newValue = colony.getProductionOf(goodsType);
                if (newValue > 0) {
                    Goods goods = new Goods(colony.getGame(), colony, goodsType, newValue);
                    //goods.setAmount(newValue);
                    GoodsLabel goodsLabel = new GoodsLabel(goods, parent);
                    goodsLabel.setHorizontalAlignment(JLabel.LEADING);
                    goodsPanel.add(goodsLabel);
                }
            }
            reportPanel.add(goodsPanel, higConst.rc(row, panelColumn));
            row++;

            // buildings
            JPanel buildingPanel = new JPanel(new GridLayout(0, 4));
            int currentType = colony.getCurrentlyBuilding();
            for (int buildingType = 0; buildingType < Building.NUMBER_OF_TYPES; buildingType++) {
                Building building = colony.getBuilding(buildingType);
                if (building.getLevel() != Building.NOT_BUILT) {
                    buildingPanel.add(new JLabel(building.getName()));
                } 
                if (buildingType == currentType) {
                    JLabel buildingLabel = new JLabel(building.getNextName());
                    buildingLabel.setForeground(Color.GRAY);
                    buildingPanel.add(buildingLabel);
                }
            }
            if (currentType >= Colony.BUILDING_UNIT_ADDITION) {
                JLabel unitLabel = new JLabel(Unit.getName(currentType - Colony.BUILDING_UNIT_ADDITION));
                unitLabel.setForeground(Color.GRAY);
                buildingPanel.add(unitLabel);
            }
            reportPanel.add(buildingPanel, higConst.rc(row, panelColumn));
            row += 2;
            colonyIndex++;
        }


    }

    /**
     * This function analyses an event and calls the right methods to take
     * care of the user's requests.
     * @param event The incoming ActionEvent.
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        int action = Integer.valueOf(command).intValue();
        if (action == OK) {
            super.actionPerformed(event);
        } else {
            parent.showColonyPanel((Colony) colonies.get(action));
        }
    }

}

