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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JTextPane;
import javax.swing.JViewport;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;

import net.miginfocom.swing.MigLayout;

/**
 * This panel displays the Advanced Colony Report.
 */
public final class ReportRequirementsPanel extends ReportPanel {

    /**
     * A list of all the player's colonies.
     */
    private List<Colony> colonies;

    /**
     * Records the number of units indexed by colony and unit type.
     */
    private int[][] unitCount;

    /**
     * Records whether a colony can train a type of unit.
     */
    private boolean[][] canTrain; 

    /**
     * Records surplus production indexed by colony and goods type.
     */
    private int[][] surplus;


    /**
     * The constructor that will add the items to this panel.
     * 
     * @param parent The parent of this panel.
     */
    public ReportRequirementsPanel(Canvas parent) {
        super(parent, Messages.message("reportRequirementsAction.name"));
        Player player = getMyPlayer();
        colonies = player.getColonies();
        Collections.sort(colonies, getClient().getClientOptions().getColonyComparator());

        // Display Panel

        // create a text pane
        JTextPane textPane = getDefaultTextPane();
        StyledDocument doc = textPane.getStyledDocument();

        int numberColonies = colonies.size();
        int numberUnitTypes = getSpecification().numberOfUnitTypes();
        int numberGoodsTypes = getSpecification().numberOfGoodsTypes();
        unitCount = new int[numberColonies][numberUnitTypes];
        canTrain = new boolean[numberColonies][numberUnitTypes];
        surplus = new int[numberColonies][numberGoodsTypes];

        // check which colonies can train which units
        int unitTypeIndex = 0;
        List<GoodsType> goodsTypes = getSpecification().getGoodsTypeList();
        for (int colonyIndex = 0; colonyIndex < numberColonies; colonyIndex++) {
            Colony colony = colonies.get(colonyIndex);
            for (Unit unit : colony.getUnitList()) {
                unitTypeIndex = unit.getType().getIndex();
                unitCount[colonyIndex][unitTypeIndex]++;
                canTrain[colonyIndex][unitTypeIndex] = colony.canTrain(unit.getType());
            }
            for (GoodsType goodsType : goodsTypes) {
                surplus[colonyIndex][goodsType.getIndex()] = colony.getProductionNetOf(goodsType);
            }
        }

        for (int colonyIndex = 0; colonyIndex < numberColonies; colonyIndex++) {
            Colony colony = colonies.get(colonyIndex);

            // colonyLabel
            try {
                if (colonyIndex != 0) {
                    doc.insertString(doc.getLength(), "\n\n", doc.getStyle("regular"));
                }
                StyleConstants.setComponent(doc.getStyle("button"), createColonyButton(colony, true));
                doc.insertString(doc.getLength(), " ", doc.getStyle("button"));
            } catch(Exception e) {
                logger.warning(e.toString());
            }

            boolean[] missingExpertWarning = new boolean[numberUnitTypes];
            boolean[] badAssignmentWarning = new boolean[numberUnitTypes];
            boolean[] productionWarning = new boolean[numberGoodsTypes];
            boolean hasWarning = false;

            // check if all unit requirements are met
            for (Unit expert : colony.getUnitList()) {
                if (expert.getSkillLevel() > 0)
                {
                    GoodsType production = expert.getWorkType();
                    GoodsType expertise = expert.getType().getExpertProduction();
                    if (production != null && expertise != null && production != expertise) {
                        // we have an expert not doing the job of their expertise
                        //    check if there is a non-expert doing the job instead
                        for (Unit nonExpert : colony.getUnitList()) {
                            if ((nonExpert.getWorkType() == expertise) && (nonExpert.getType() != expert.getType())) {
                                // we've found a unit of a different type doing the job of this expert's expertise
                                //  now check if the production would be better if the units swapped positions
                                int expertProductionNow = 0;
                                int nonExpertProductionNow = 0;
                                int expertProductionPotential = 0;
                                int nonExpertProductionPotential = 0;
                                
                                // get the current and potential productions for the work location of the expert
                                if (expert.getWorkTile() != null) {
                                    expertProductionNow = expert.getWorkTile().getProductionOf(expert, expertise);
                                    nonExpertProductionPotential = expert.getWorkTile().getProductionOf(nonExpert, expertise);
                                } else if (expert.getWorkBuilding() != null) {
                                    expertProductionNow = expert.getWorkBuilding().getUnitProductivity(expert);
                                    nonExpertProductionPotential = expert.getWorkBuilding().getUnitProductivity(nonExpert);
                                }
                                
                                // get the current and potential productions for the work location of the non-expert
                                if (nonExpert.getWorkTile() != null) {
                                    nonExpertProductionNow = nonExpert.getWorkTile().getProductionOf(nonExpert, expertise);
                                    expertProductionPotential = nonExpert.getWorkTile().getProductionOf(expert, expertise);
                                } else if (nonExpert.getWorkBuilding() != null) {
                                    nonExpertProductionNow = nonExpert.getWorkBuilding().getUnitProductivity(nonExpert);
                                    expertProductionPotential = nonExpert.getWorkBuilding().getUnitProductivity(expert);
                                }
                                
                                // let the player know if the two units would be more productive were they to swap roles
                                int expertIndex = expert.getType().getIndex();
                                if ((expertProductionNow + nonExpertProductionNow) < (expertProductionPotential + nonExpertProductionPotential) && !badAssignmentWarning[expertIndex]) {
                                    addBadAssignmentWarning(doc, colonyIndex, expert, nonExpert);
                                    badAssignmentWarning[expertIndex] = true;
                                    hasWarning = true;
                                }
                            }
                        }
                    }
                }
            }
            for (ColonyTile colonyTile : colony.getColonyTiles()) {
                Unit unit = colonyTile.getUnit();
                if (unit != null) {
                    GoodsType workType = unit.getWorkType();
                    UnitType expert = getSpecification().getExpertForProducing(workType);
                    int expertIndex = expert.getIndex();
                    if (unitCount[colonyIndex][expertIndex] == 0 && !missingExpertWarning[expertIndex]) {
                        addExpertWarning(doc, colonyIndex, workType, expert);
                        missingExpertWarning[expertIndex] = true;
                        hasWarning = true;
                    }
                }
            } 
            for (Building building : colony.getBuildings()) {
                GoodsType goodsType = building.getGoodsOutputType();
                UnitType expert = building.getExpertUnitType();
                    
                if (goodsType != null && expert != null) {
                    // check if this building has no expert producing goods
                    int expertIndex = expert.getIndex();
                    if (building.getFirstUnit() != null &&
                        !missingExpertWarning[expertIndex] &&
                        unitCount[colonyIndex][expertIndex] == 0) {
                        addExpertWarning(doc, colonyIndex, goodsType, expert);
                        missingExpertWarning[expertIndex] = true;
                        hasWarning = true;
                    }
                }
                if (goodsType != null) {
                    // not enough input
                    int goodsIndex = goodsType.getIndex();
                    if (building.getProductionNextTurn() < building.getMaximumProduction() &&
                        !productionWarning[goodsIndex]) {
                        addProductionWarning(doc, colonyIndex, goodsType, building.getGoodsInputType());
                        productionWarning[goodsIndex] = true;
                        hasWarning = true;
                    }
                }
            }

            if (!hasWarning) {
                try {
                    doc.insertString(doc.getLength(), "\n\n" + Messages.message("report.requirements.met"),
                                     doc.getStyle("regular"));
                } catch(Exception e) {
                    logger.warning(e.toString());
                }
            }

            // text area
            int width = ((JViewport) reportPanel.getParent()).getWidth();
            reportPanel.setLayout(new MigLayout("width " + width + "!"));
            reportPanel.add(textPane);

        }
        textPane.setCaretPosition(0);

    }

    private void addBadAssignmentWarning(StyledDocument doc, int colonyIndex, Unit expert, Unit nonExpert) {
        GoodsType expertGoods = expert.getWorkType();
        GoodsType nonExpertGoods = nonExpert.getWorkType();
        String colonyName = colonies.get(colonyIndex).getName();
        String expertName = Messages.message(expert.getType().getNameKey());
        String nonExpertName = Messages.message(nonExpert.getType().getNameKey());
        String expertProductionName = Messages.message(expertGoods.getWorkingAsKey());
        String nonExpertProductionName = Messages.message(nonExpertGoods.getWorkingAsKey());
        String newMessage = Messages.message("report.requirements.badAssignment",
            "%colony%", colonyName,
            "%expert%", expertName,
            "%expertWork%", expertProductionName,
            "%nonExpert%", nonExpertName,
            "%nonExpertWork%", nonExpertProductionName);

        try {
            doc.insertString(doc.getLength(), "\n\n" + newMessage, doc.getStyle("regular"));
        } catch(Exception e) {
            logger.warning(e.toString());
        }
    }

    private void addExpertWarning(StyledDocument doc, int colonyIndex, GoodsType goodsType, UnitType workType) {
        String expertName = Messages.message(workType.getNameKey());
        String colonyName = colonies.get(colonyIndex).getName();
        String goods = Messages.message(goodsType.getNameKey());
        String newMessage = Messages.message("report.requirements.noExpert", "%colony%", colonyName, "%goods%", goods,
                "%unit%", expertName);

        try {
            doc.insertString(doc.getLength(), "\n\n" + newMessage, doc.getStyle("regular"));

            ArrayList<Colony> severalExperts = new ArrayList<Colony>();
            ArrayList<Colony> canTrainExperts = new ArrayList<Colony>();
            for (int index = 0; index < colonies.size(); index++) {
                if (unitCount[index][workType.getIndex()] > 1) {
                    severalExperts.add(colonies.get(index));
                }
                if (canTrain[index][workType.getIndex()]) {
                    canTrainExperts.add(colonies.get(index));
                }
            }

            if (!severalExperts.isEmpty()) {
                doc.insertString(doc.getLength(), 
                        "\n" + Messages.message("report.requirements.severalExperts", "%unit%", expertName) + " ", 
                        doc.getStyle("regular"));
                int lastExpertsIndex = severalExperts.size() - 1;
                for (int index = 0; index <= lastExpertsIndex; index++) {
                    Colony colony = severalExperts.get(index);
                    StyleConstants.setComponent(doc.getStyle("button"), createColonyButton(colony, false));
                    doc.insertString(doc.getLength(), " ", doc.getStyle("button"));
                    if (index != lastExpertsIndex) {
                        doc.insertString(doc.getLength(), ", ", doc.getStyle("regular"));
                    }
                }
            }

            if (!canTrainExperts.isEmpty()) {
                doc.insertString(doc.getLength(), 
                        "\n" + Messages.message("report.requirements.canTrainExperts", "%unit%", expertName) + " ", 
                        doc.getStyle("regular"));
                int lastExpertsIndex = canTrainExperts.size() - 1;
                for (int index = 0; index <= lastExpertsIndex; index++) {
                    Colony colony = canTrainExperts.get(index);
                    StyleConstants.setComponent(doc.getStyle("button"), createColonyButton(colony, false));
                    doc.insertString(doc.getLength(), " ", doc.getStyle("button"));
                    if (index != lastExpertsIndex) {
                        doc.insertString(doc.getLength(), ", ", doc.getStyle("regular"));
                    }
                }
            }
  
        } catch(Exception e) {
            logger.warning(e.toString());
        }
        
    }

    private void addProductionWarning(StyledDocument doc, int colonyIndex, GoodsType output, GoodsType input) {
        String colonyName = colonies.get(colonyIndex).getName();
        String newMessage = Messages.message(StringTemplate.template("report.requirements.missingGoods")
                                             .addName("%colony%", colonyName)
                                             .add("%goods%", output.getNameKey())
                                             .add("%input%", input.getNameKey()));

        try {
            doc.insertString(doc.getLength(), "\n\n" + newMessage, doc.getStyle("regular"));

            ArrayList<Colony> withSurplus = new ArrayList<Colony>();
            ArrayList<Integer> theSurplus = new ArrayList<Integer>();
            for (int index = 0; index < colonies.size(); index++) {
                if (surplus[index][input.getIndex()] > 0) {
                    withSurplus.add(colonies.get(index));
                    theSurplus.add(new Integer(surplus[index][input.getIndex()]));
                }
            }

            if (!withSurplus.isEmpty()) {
                doc.insertString(doc.getLength(),
                                 "\n" + Messages.message(StringTemplate.template("report.requirements.surplus")
                                                  .add("%goods%", input.getNameKey())) + " ",
                                 doc.getStyle("regular"));
                for (int index = 0; index < withSurplus.size() - 1; index++) {
                    Colony colony = withSurplus.get(index);
                    String amount = " (" + theSurplus.get(index) + ")";
                    StyleConstants.setComponent(doc.getStyle("button"), createColonyButton(colony, amount, false));
                    doc.insertString(doc.getLength(), " ", doc.getStyle("button"));
                    doc.insertString(doc.getLength(), ", ", doc.getStyle("regular"));
                }
                Colony colony = withSurplus.get(withSurplus.size() - 1);
                String amount = " (" + theSurplus.get(theSurplus.size() - 1) + ")";
                StyleConstants.setComponent(doc.getStyle("button"), createColonyButton(colony, amount, false));
                doc.insertString(doc.getLength(), " ", doc.getStyle("button"));
            }


        } catch(Exception e) {
            logger.warning(e.toString());
        }
        
    }

    private JButton createColonyButton(Colony colony, boolean headline) {
        return createColonyButton(colony, "", headline);
    }

    private JButton createColonyButton(Colony colony, String info, boolean headline) {
        JButton button = getLinkButton(colony.getName() + info, null, colony.getId());
        if (headline) {
            button.setFont(smallHeaderFont);
        }
        button.addActionListener(this);
        return button;
    }


}
