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

import java.awt.Color;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import cz.autel.dmi.HIGConstraints;
import cz.autel.dmi.HIGLayout;

/**
 * This panel displays the Production Report.
 */
public final class ReportProductionPanel extends JPanel implements ActionListener {



    /** How many additional rows are defined. */
    private final int extraRows = 2;

    /** How much space to leave between labels. */
    private final int columnSeparatorWidth = 12;

    /** How wide the margins should be. */
    private final int marginWidth = 12;

    /** The widths of the columns. */
    private final int[] widths;

    /** The heights of the rows. */
    // private final int[] heights;
    private static final HIGConstraints higConst = new HIGConstraints();

    private Canvas parent;

    private List<Colony> colonies;

    private final GoodsType goodsType;

    private final ReportPanel reportPanel;

    private int totalProduction;


    /**
     * The constructor that will add the items to this panel.
     * 
     * @param parent The parent of this panel.
     */
    public ReportProductionPanel(GoodsType goodsType, Canvas parent, ReportPanel reportPanel) {
        this.goodsType = goodsType;
        this.parent = parent;
        this.reportPanel = reportPanel;

        if (goodsType == Goods.BELLS) {
            /*
            widths = new int[] { 0, columnSeparatorWidth, 0, columnSeparatorWidth, 0, columnSeparatorWidth, 0,
                    columnSeparatorWidth, 0, columnSeparatorWidth, 0, columnSeparatorWidth, 0, columnSeparatorWidth, 0 };
            */
            widths = new int[8];
        } else if (goodsType == Goods.CROSSES) {
            /*
            widths = new int[] { 0, columnSeparatorWidth, 0, columnSeparatorWidth, 0, columnSeparatorWidth, 0,
                    columnSeparatorWidth, 0 };
            */
            widths = new int[5];
        } else {
            /*
            widths = new int[] { 0, columnSeparatorWidth, 0, columnSeparatorWidth, 0 };
            */
            widths = new int[3];
        }
        // heights = null;
        totalProduction = 0;
        setOpaque(false);
    }

    /**
     * Prepares this panel to be displayed.
     */
    public void initialize() {
        Player player = parent.getClient().getMyPlayer();
        
        int buildingCount[] = new int[FreeCol.getSpecification().numberOfBuildingTypes()];
        List<BuildingType> buildingTypes = new ArrayList<BuildingType>();
        for (BuildingType buildingType : FreeCol.getSpecification().getBuildingTypeList()) {
            if ((buildingType.getProducedGoodsType() == goodsType ||
                 !buildingType.getModifierSet(goodsType.getId()).isEmpty()) &&
                !buildingType.getGoodsRequired().isEmpty()) {
                buildingTypes.add(buildingType);
                buildingCount[buildingType.getIndex()] = 0;
            }
        }

        int colonyColumn = 1;
        int productionColumn = 2;
        int unitColumn = 3;
        int buildingColumn = unitColumn + 1;
        int percentageColumn = buildingColumn + buildingTypes.size();
        int solColumn = percentageColumn + 1;
        int toryColumn = solColumn + 1;

        // Display Panel
        removeAll();

        colonies = player.getColonies();
        int numberOfColonies = colonies.size();
        int[] heights = new int[numberOfColonies + extraRows];

        for (int index = extraRows; index < heights.length; index++) {
            /** TODO: replace this magic number by some value from the
             * ImageLibrary. At the moment this is difficult, as the
             * unit images in the library are not sorted according to
             * type. For this purpose, however, one would need to
             * consider only ships for the naval report and only units
             * able to defend colonies for the military report. The
             * value of 64 is large enough to accommodate the 2/3
             * scale version of all unit graphics.
             */
            heights[index] = 64;
        }
        

        setLayout(new HIGLayout(widths, heights));

        JLabel newLabel;
        // labels
        newLabel = new JLabel(Messages.message("Colony"));
        newLabel.setBorder(FreeColPanel.TOPLEFTCELLBORDER);
        add(newLabel, higConst.rc(1, colonyColumn));

        newLabel = new JLabel(Messages.message("report.production"));
        newLabel.setBorder(FreeColPanel.TOPCELLBORDER);
        add(newLabel, higConst.rc(1, productionColumn));

        newLabel = new JLabel(Messages.message("report.units"));
        newLabel.setBorder(FreeColPanel.TOPCELLBORDER);
        add(newLabel, higConst.rc(1, unitColumn));

        int column = buildingColumn;
        for (BuildingType buildingType : buildingTypes) {
            newLabel = new JLabel(buildingType.getName());
            newLabel.setBorder(FreeColPanel.TOPCELLBORDER);
            add(newLabel, higConst.rc(1, column));
            column++;
        }
        
        if (goodsType == Goods.BELLS) {
            newLabel = new JLabel(Messages.message("sonsOfLiberty") + "%");
            newLabel.setBorder(FreeColPanel.TOPCELLBORDER);
            add(newLabel, higConst.rc(1, percentageColumn));

            newLabel = new JLabel(Messages.message("sonsOfLiberty"));
            newLabel.setBorder(FreeColPanel.TOPCELLBORDER);
            add(newLabel, higConst.rc(1, solColumn));

            newLabel = new JLabel(Messages.message("tory"));
            newLabel.setBorder(FreeColPanel.TOPCELLBORDER);
            add(newLabel, higConst.rc(1, toryColumn));
        }
            

        int row = extraRows + 1;
        totalProduction = 0;
        int totalUnits = 0;

        int percentageCount = 0;
        int solCount = 0;
        int toryCount = 0;

        Collections.sort(colonies, parent.getClient().getClientOptions().getColonyComparator());

        for (int colonyIndex = 0; colonyIndex < colonies.size(); colonyIndex++) {

            Colony colony = colonies.get(colonyIndex);

            // colonyButton
            add(createColonyButton(colonyIndex), higConst.rc(row, colonyColumn));

            // production
            int newValue = colony.getProductionOf(goodsType);
            totalProduction += newValue;
            Goods goods = new Goods(colony.getGame(), colony, goodsType, newValue);
            GoodsLabel goodsLabel = new GoodsLabel(goods, parent);
            goodsLabel.setHorizontalAlignment(JLabel.LEADING);
            goodsLabel.setBorder(FreeColPanel.CELLBORDER);
            add(goodsLabel, higConst.rc(row, productionColumn));

            // units
            Building building = colony.getBuildingForProducing(goodsType);
            JPanel unitPanel = new JPanel();
            unitPanel.setBorder(FreeColPanel.CELLBORDER);
            unitPanel.setOpaque(false);
            if (building != null) {
                Iterator<Unit> unitIterator = building.getUnitIterator();
                while (unitIterator.hasNext()) {
                    Unit unit = unitIterator.next();
                    UnitLabel label = new UnitLabel(unit, parent, true);
                    unitPanel.add(label);
                    totalUnits++;
                }
            }
            add(unitPanel, higConst.rc(row, unitColumn));

            
            column = buildingColumn;
            for (BuildingType buildingType : buildingTypes) {
                Building b = colony.getBuilding(buildingType);
                int level = 0;
                if (b!=null)
                    level = b.getLevel();
                newLabel = new JLabel();
                newLabel.setBorder(FreeColPanel.CELLBORDER);
                if (level == buildingType.getLevel()) {
                    newLabel.setText("X");
                    buildingCount[buildingType.getIndex()]++;
                }
                add(newLabel, higConst.rc(row, column));
                column++;
            }

            // special
            if (goodsType == Goods.BELLS) {
                int percentage = colony.getSoL();
                percentageCount += percentage;
                int count = colony.getUnitCount();
                int sol = percentage * count / 100;
                solCount += sol;
                int tories = count - sol;
                toryCount += tories;

                newLabel = new JLabel(String.valueOf(percentage), JLabel.TRAILING);
                newLabel.setBorder(FreeColPanel.CELLBORDER);
                add(newLabel, higConst.rc(row, percentageColumn));
                newLabel = new JLabel(String.valueOf(sol), JLabel.TRAILING);
                newLabel.setBorder(FreeColPanel.CELLBORDER);
                add(newLabel, higConst.rc(row, solColumn));
                newLabel = new JLabel(String.valueOf(tories), JLabel.TRAILING);
                newLabel.setBorder(FreeColPanel.CELLBORDER);
                add(newLabel, higConst.rc(row, toryColumn));
            }


            row++;
        }

        row = 2;
        // summary
        JLabel allColonies = new JLabel(Messages.message("report.allColonies", "%number%",
                String.valueOf(numberOfColonies)));
        allColonies.setForeground(Color.BLUE);
        allColonies.setBorder(FreeColPanel.LEFTCELLBORDER);
        add(allColonies, higConst.rc(row, colonyColumn));

        Goods allGoods = new Goods(parent.getClient().getGame(), null, goodsType, totalProduction);
        GoodsLabel allGoodsLabel = new GoodsLabel(allGoods, parent);
        allGoodsLabel.setHorizontalAlignment(JLabel.LEADING);
        allGoodsLabel.setBorder(FreeColPanel.CELLBORDER);
        add(allGoodsLabel, higConst.rc(row, productionColumn));

        newLabel = new JLabel(String.valueOf(totalUnits));
        newLabel.setBorder(FreeColPanel.CELLBORDER);
        add(newLabel, higConst.rc(row, unitColumn));

        column = buildingColumn;
        for (BuildingType buildingType : buildingTypes) {
            int count = buildingCount[buildingType.getIndex()];
            newLabel = new JLabel(String.valueOf(count), JLabel.TRAILING);
            newLabel.setBorder(FreeColPanel.CELLBORDER);
            add(newLabel, higConst.rc(row, column));
            column++;
        }
        
        if (goodsType == Goods.BELLS) {
            int percentage = 0;
            if (numberOfColonies > 0) {
                percentage = percentageCount / numberOfColonies;
            }
            newLabel = new JLabel(String.valueOf(percentage), JLabel.TRAILING);
            newLabel.setBorder(FreeColPanel.CELLBORDER);
            add(newLabel, higConst.rc(row, percentageColumn));
            newLabel = new JLabel(String.valueOf(solCount), JLabel.TRAILING);
            newLabel.setBorder(FreeColPanel.CELLBORDER);
            add(newLabel, higConst.rc(row, solColumn));
            newLabel = new JLabel(String.valueOf(toryCount), JLabel.TRAILING);
            newLabel.setBorder(FreeColPanel.CELLBORDER);
            add(newLabel, higConst.rc(row, toryColumn));
        }
    }


    private JButton createColonyButton(int index) {

        JButton button = new JButton(colonies.get(index).getName());
        button.setMargin(new Insets(0,0,0,0));
        button.setOpaque(false);
        button.setForeground(FreeColPanel.LINK_COLOR);
        button.setAlignmentY(0.8f);
        button.setHorizontalAlignment(SwingConstants.LEADING);
        button.setBorder(FreeColPanel.LEFTCELLBORDER);
        button.setActionCommand(String.valueOf(index));
        button.addActionListener(this);
        return button;
    }

    /**
     * This function analyses an event and calls the right methods to take care
     * of the user's requests.
     * 
     * @param event The incoming ActionEvent.
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        int action = Integer.valueOf(command).intValue();
        if (action == ReportPanel.OK) {
            reportPanel.actionPerformed(event);
        } else {
            parent.showColonyPanel(colonies.get(action));
        }
    }

    public int getTotalProduction() {
        return totalProduction;
    }

}
