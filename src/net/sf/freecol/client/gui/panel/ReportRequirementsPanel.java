/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JTextPane;
import javax.swing.JViewport;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.ProductionInfo;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TypeCountMap;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;


/**
 * This panel displays the Advanced Colony Report.
 */
public final class ReportRequirementsPanel extends ReportPanel {

    private static final Logger logger = Logger.getLogger(ReportRequirementsPanel.class.getName());

    /**
     * A list of all the player's colonies.
     */
    private List<Colony> colonies;

    /**
     * Records the number of units indexed by colony and unit type.
     */
    private Map<Colony, TypeCountMap<UnitType>> unitCount
        = new HashMap<Colony, TypeCountMap<UnitType>>();

    /**
     * Records whether a colony can train a type of unit.
     */
    private Map<Colony, Set<UnitType>> canTrain
        = new HashMap<Colony, Set<UnitType>>();


    /**
     * The constructor that will add the items to this panel.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public ReportRequirementsPanel(FreeColClient freeColClient) {
        super(freeColClient, Messages.message("reportRequirementsAction.name"));

        colonies = getSortedColonies();

        // Display Panel

        // create a text pane
        JTextPane textPane = getDefaultTextPane();
        StyledDocument doc = textPane.getStyledDocument();

        // check which colonies can train which units
        for (Colony colony : colonies) {
            TypeCountMap<UnitType> newUnitCount = new TypeCountMap<UnitType>();
            Set<UnitType> newCanTrain = new HashSet<UnitType>();
            for (Unit unit : colony.getUnitList()) {
                newUnitCount.incrementCount(unit.getType(), 1);
                if (colony.canTrain(unit.getType())) {
                    newCanTrain.add(unit.getType());
                }
            }
            unitCount.put(colony, newUnitCount);
            canTrain.put(colony, newCanTrain);
        }

        for (Colony colony : colonies) {
            checkColony(colony, doc);
        }
        // text area
        int width = ((JViewport) reportPanel.getParent()).getWidth();
        reportPanel.setLayout(new MigLayout("width " + width + "!"));
        reportPanel.add(textPane);
        textPane.setCaretPosition(0);
    }

    private void checkColony(Colony colony, StyledDocument doc) {
        final Specification spec = getSpecification();

        try {
            if (doc.getLength() > 0) {
                doc.insertString(doc.getLength(), "\n\n",
                    doc.getStyle("regular"));
            }
            StyleConstants.setComponent(doc.getStyle("button"),
                createColonyButton(colony, true));
            doc.insertString(doc.getLength(), " ", doc.getStyle("button"));
        } catch (Exception e) {
            logger.log(Level.WARNING, "Colony check fail", e);
        }

        Set<UnitType> missingExpertWarning = new HashSet<UnitType>();
        Set<UnitType> badAssignmentWarning = new HashSet<UnitType>();
        Set<GoodsType> productionWarning = new HashSet<GoodsType>();

        // Check if all unit requirements are met.
        for (Unit expert : colony.getUnitList()) {
            if (expert.getSkillLevel() <= 0) continue;
            Unit better = colony.getBetterExpert(expert);
            if (better != null && !badAssignmentWarning.contains(expert)) {
                addBadAssignmentWarning(doc, colony, expert, better);
                badAssignmentWarning.add(expert.getType());
            }
        }

        for (ColonyTile colonyTile : colony.getColonyTiles()) {
            for (Unit unit : colonyTile.getUnitList()) {
                GoodsType workType = unit.getWorkType();
                UnitType expert = spec.getExpertForProducing(workType);
                if (unitCount.get(colony).getCount(expert) == 0
                    && !missingExpertWarning.contains(expert)) {
                    addExpertWarning(doc, colony, workType, expert);
                    missingExpertWarning.add(expert);
                }
            }
        }

        for (Building building : colony.getBuildings()) {
            for (AbstractGoods output : building.getOutputs()) {
                GoodsType goodsType = output.getType();
                UnitType expert = spec.getExpertForProducing(goodsType);

                // check if this building has no expert producing goods
                if (goodsType != null && expert != null
                    && !building.getUnitList().isEmpty()
                    && !missingExpertWarning.contains(expert)
                    && unitCount.get(colony).getCount(expert) == 0) {
                    addExpertWarning(doc, colony, goodsType, expert);
                    missingExpertWarning.add(expert);
                }
                // not enough input
                ProductionInfo info = building.getProductionInfo();
                if (goodsType != null
                    && info != null
                    && !info.hasMaximumProduction()
                    && !productionWarning.contains(goodsType)) {
                    for (AbstractGoods input : building.getInputs()) {
                        addProductionWarning(doc, colony, goodsType, input.getType());
                    }
                    productionWarning.add(goodsType);
                }
            }
        }

        List<Tile> exploreTiles = new ArrayList<Tile>();
        List<Tile> clearTiles = new ArrayList<Tile>();
        List<Tile> plowTiles = new ArrayList<Tile>();
        List<Tile> roadTiles = new ArrayList<Tile>();
        colony.getColonyTileTodo(exploreTiles, clearTiles, plowTiles,
            roadTiles);
        for (Tile t : exploreTiles) {
            addTileWarning(doc, colony, "report.requirements.exploreTile", t);
        }
        for (Tile t : clearTiles) {
            addTileWarning(doc, colony, "report.requirements.clearTile", t);
        }
        for (Tile t : plowTiles) {
            if (t == colony.getTile()) {
                addPlowCenterWarning(doc, colony);
            } else {
                addTileWarning(doc, colony, "report.requirements.plowTile", t);
            }
        }
        for (Tile t : roadTiles) {
            addTileWarning(doc, colony, "report.requirements.roadTile", t);
        }

        if (exploreTiles.isEmpty()
            && clearTiles.isEmpty()
            && plowTiles.isEmpty()
            && roadTiles.isEmpty()
            && missingExpertWarning.isEmpty()
            && badAssignmentWarning.isEmpty()
            && productionWarning.isEmpty()) {
            try {
                doc.insertString(doc.getLength(), "\n\n"
                    + Messages.message("report.requirements.met"),
                    doc.getStyle("regular"));
            } catch (Exception e) {
                logger.log(Level.WARNING, "Colony check fail", e);
            }
        }
    }

    private void addTileWarning(StyledDocument doc, Colony colony,
                                String messageId, Tile tile) {
        Direction direction = colony.getTile().getDirection(tile);
        String message = Messages.message(StringTemplate.template(messageId)
            .add("%type%", tile.getType().getNameKey())
            .add("%direction%", direction.getNameKey())
            .addName("%colony%", colony.getName()));
        try {
            doc.insertString(doc.getLength(), "\n\n" + message,
                doc.getStyle("regular"));
        } catch (Exception e) {
            logger.log(Level.WARNING, "Tile warning fail", e);
        }
    }

    private void addPlowCenterWarning(StyledDocument doc, Colony colony) {
        String message = Messages.message(StringTemplate.template("report.requirements.plowCenter")
            .addName("%colony%", colony.getName()));
        try {
            doc.insertString(doc.getLength(), "\n\n" + message,
                doc.getStyle("regular"));
        } catch (Exception e) {
            logger.log(Level.WARNING, "Center warning fail", e);
        }
    }

    private void addBadAssignmentWarning(StyledDocument doc, Colony colony, Unit expert, Unit nonExpert) {
        GoodsType expertGoods = expert.getWorkType();
        GoodsType nonExpertGoods = nonExpert.getWorkType();
        String colonyName = colony.getName();
        String expertName = Messages.message(expert.getType().getNameKey());
        String nonExpertName = Messages.message(nonExpert.getType().getNameKey());
        String expertProductionName = Messages.message(expertGoods.getWorkingAsKey());
        String nonExpertProductionName = Messages.message(nonExpertGoods.getWorkingAsKey());
        String newMessage = Messages.message(StringTemplate.template("report.requirements.badAssignment")
                                             .addName("%colony%", colonyName)
                                             .addName("%expert%", expertName)
                                             .addName("%expertWork%", expertProductionName)
                                             .addName("%nonExpert%", nonExpertName)
                                             .addName("%nonExpertWork%", nonExpertProductionName));

        try {
            doc.insertString(doc.getLength(), "\n\n" + newMessage, doc.getStyle("regular"));
        } catch (Exception e) {
            logger.log(Level.WARNING, "Bad assignment fail", e);
        }
    }

    private void addExpertWarning(StyledDocument doc, Colony c, GoodsType goodsType, UnitType workType) {
        String newMessage = Messages.message(StringTemplate.template("report.requirements.noExpert")
                                             .addName("%colony%", c.getName())
                                             .addName("%goods%", goodsType)
                                             .addName("%unit%", workType));

        try {
            doc.insertString(doc.getLength(), "\n\n" + newMessage, doc.getStyle("regular"));

            ArrayList<Colony> misusedExperts = new ArrayList<Colony>();
            ArrayList<Colony> severalExperts = new ArrayList<Colony>();
            ArrayList<Colony> canTrainExperts = new ArrayList<Colony>();
            for (Colony colony : colonies) {
                for (Unit unit : colony.getUnitList()) {
                    GoodsType expertise = unit.getType().getExpertProduction();
                    if ((unit.getSkillLevel() > 0) && (expertise == goodsType)) {
                        if (unit.getLocation() instanceof Building) {
                            if (!((Building) unit.getLocation()).produces(goodsType)) {
                                misusedExperts.add(colony);
                            }
                        } else if (expertise != unit.getWorkType()) {
                            misusedExperts.add(colony);
                        }
                    }
                }
                if (unitCount.get(colony).getCount(workType) > 1) {
                    severalExperts.add(colony);
                }
                if (canTrain.get(colony).contains(workType)) {
                    canTrainExperts.add(colony);
                }
            }

            if (!misusedExperts.isEmpty()) {
                doc.insertString(doc.getLength(), "\n"
                                 + Messages.message(StringTemplate.template("report.requirements.misusedExperts")
                                                    .addName("%unit%", workType)
                                                    .add("%work%", goodsType.getWorkingAsKey())) + " ",
                                 doc.getStyle("regular"));
                insertColonyButtons(doc, misusedExperts);
            }

            if (!severalExperts.isEmpty()) {
                doc.insertString(doc.getLength(),
                                 "\n" + Messages.message(StringTemplate.template("report.requirements.severalExperts")
                                                         .addName("%unit%", workType)) + " ",
                        doc.getStyle("regular"));
                insertColonyButtons(doc, severalExperts);
            }

            if (!canTrainExperts.isEmpty()) {
                doc.insertString(doc.getLength(),
                                 "\n" + Messages.message(StringTemplate.template("report.requirements.canTrainExperts")
                                                         .addName("%unit%", workType)) + " ",
                        doc.getStyle("regular"));
                insertColonyButtons(doc, canTrainExperts);
            }

        } catch(Exception e) {
            logger.log(Level.WARNING, "Assign experts fail", e);
        }

    }

    private void insertColonyButtons(StyledDocument doc, List<Colony> colonies) throws Exception {
        for (Colony colony : colonies) {
            StyleConstants.setComponent(doc.getStyle("button"), createColonyButton(colony, false));
            doc.insertString(doc.getLength(), " ", doc.getStyle("button"));
            doc.insertString(doc.getLength(), ", ", doc.getStyle("regular"));
        }
        doc.remove(doc.getLength() - 2, 2);
    }

    private void addProductionWarning(StyledDocument doc, Colony colony, GoodsType output, GoodsType input) {
        String colonyName = colony.getName();
        String newMessage = Messages.message(StringTemplate.template("report.requirements.missingGoods")
                                             .addName("%colony%", colonyName)
                                             .add("%goods%", output.getNameKey())
                                             .add("%input%", input.getNameKey()));

        try {
            doc.insertString(doc.getLength(), "\n\n" + newMessage, doc.getStyle("regular"));

            ArrayList<Colony> withSurplus = new ArrayList<Colony>();
            ArrayList<Integer> theSurplus = new ArrayList<Integer>();
            for (Colony col : colonies) {
                int amount = colony.getAdjustedNetProductionOf(input);
                if (amount > 0) {
                    withSurplus.add(col);
                    theSurplus.add(amount);
                }
            }

            if (!withSurplus.isEmpty()) {
                doc.insertString(doc.getLength(),
                                 "\n" + Messages.message(StringTemplate.template("report.requirements.surplus")
                                                  .add("%goods%", input.getNameKey())) + " ",
                                 doc.getStyle("regular"));
                for (int index = 0; index < withSurplus.size() - 1; index++) {
                    String amount = " (" + theSurplus.get(index) + ")";
                    StyleConstants.setComponent(doc.getStyle("button"),
                                                createColonyButton(withSurplus.get(index), amount, false));
                    doc.insertString(doc.getLength(), " ", doc.getStyle("button"));
                    doc.insertString(doc.getLength(), ", ", doc.getStyle("regular"));
                }
                Colony lastColony = withSurplus.get(withSurplus.size() - 1);
                String amount = " (" + theSurplus.get(theSurplus.size() - 1) + ")";
                StyleConstants.setComponent(doc.getStyle("button"),
                                            createColonyButton(lastColony, amount, false));
                doc.insertString(doc.getLength(), " ", doc.getStyle("button"));
            }


        } catch(Exception e) {
            logger.log(Level.WARNING, "Production warning fail", e);
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
