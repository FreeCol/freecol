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

package net.sf.freecol.server.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Tile;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Objects of this class describes the plan the AI has for a <code>Colony</code>.
 * 
 * <br>
 * <br>
 * 
 * A <code>ColonyPlan</code> contains {@link WorkLocationPlan}s which defines
 * the production of each {@link Building} and {@link ColonyTile}.
 * 
 * @see Colony
 */
public class ColonyPlan {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(ColonyPlan.class.getName());

    // What is this supposed to be? Is it the maximum number of units
    // per building?
    private static final int MAX_LEVEL = 3;

    /**
     * The FreeColGameObject this AIObject contains AI-information for.
     */
    private Colony colony;

    private AIMain aiMain;

    private ArrayList<WorkLocationPlan> workLocationPlans = new ArrayList<WorkLocationPlan>();


    /**
     * Creates a new <code>ColonyPlan</code>.
     * 
     * @param aiMain The main AI-object.
     * @param colony The colony to make a <code>ColonyPlan</code> for.
     */
    public ColonyPlan(AIMain aiMain, Colony colony) {
        if (colony == null) {
            throw new IllegalArgumentException("Parameter 'colony' must not be 'null'.");
        }
        this.aiMain = aiMain;
        this.colony = colony;
    }

    /**
     * Creates a new <code>ColonyPlan</code>.
     * 
     * @param aiMain The main AI-object.
     * @param element An <code>Element</code> containing an XML-representation
     *            of this object.
     */
    public ColonyPlan(AIMain aiMain, Element element) {
        this.aiMain = aiMain;
        readFromXMLElement(element);
    }

    /**
     * Returns the <code>WorkLocationPlan</code>s associated with this
     * <code>ColonyPlan</code>.
     * 
     * @return The list of <code>WorkLocationPlan</code>s .
     */
    public List<WorkLocationPlan> getWorkLocationPlans() {
        return new ArrayList<WorkLocationPlan>(workLocationPlans);
    }

    /**
     * Returns the <code>WorkLocationPlan</code>s associated with this
     * <code>ColonyPlan</code> sorted by production in a decreasing order.
     * 
     * @return The list of <code>WorkLocationPlan</code>s .
     */
    public List<WorkLocationPlan> getSortedWorkLocationPlans() {
        List<WorkLocationPlan> workLocationPlans = getWorkLocationPlans();
        Collections.sort(workLocationPlans, new Comparator<WorkLocationPlan>() {
            public int compare(WorkLocationPlan o, WorkLocationPlan p) {
                return p.getProductionOf(p.getGoodsType()) - o.getProductionOf(o.getGoodsType());
            }
        });

        return workLocationPlans;
    }

    /**
     * Gets an <code>Iterator</code> for everything to be built in the
     * <code>Colony</code>.
     * 
     * @return An iterator containing all the <code>Buildable</code> sorted by
     *         priority (highest priority first).
     */
    public Iterator<BuildableType> getBuildable() {
        List<BuildableType> buildList = new ArrayList<BuildableType>();

        BuildingType docks = null;
        BuildingType customHouse = null;
        BuildingType carpenter = null;
        BuildingType stables = null;
        BuildingType stockade = null;
        BuildingType armory = null;
        BuildingType schoolhouse = null;
        for (BuildingType type : FreeCol.getSpecification().getBuildingTypeList()) {
            if (type.getUpgradesFrom() != null) {
                continue;
            }
            if (type.hasAbility("model.ability.produceInWater")) {
                docks = type;
            } else if (type.hasAbility("model.ability.export")) {
                customHouse = type;
            } else if (type.hasAbility("model.ability.teach")) {
                schoolhouse = type;
            } else if (type.getProducedGoodsType() == Goods.HAMMERS) {
                carpenter = type;
            } else if (type.getProducedGoodsType() == Goods.HORSES) {
                stables = type;
            } else if (type.getProducedGoodsType() == Goods.MUSKETS) {
                armory = type;
            } else if (!type.getModifierSet("model.modifier.defence").isEmpty()) {
                stockade = type;
            }
        }

        String ability = "model.ability.produceInWater";
        if (!colony.hasAbility(ability)) {
            if (colony.canBuild(docks)) {
                buildList.add(docks);
            }
        }

        Iterator<WorkLocationPlan> wlpIt = getSortedWorkLocationPlans().iterator();
        while (wlpIt.hasNext()) {
            WorkLocationPlan wlp = wlpIt.next();
            if (wlp.getWorkLocation() instanceof Building) {
                Building b = (Building) wlp.getWorkLocation();
                if (b.canBuildNext()) {
                    buildList.add(b.getType().getUpgradesTo());
                }

                GoodsType outputType = b.getGoodsOutputType();
                if (outputType != null) {
                    for (Building building : colony.getBuildings()) {
                        if (!building.getType().getModifierSet(outputType.getId()).isEmpty()
                                && building.canBuildNext()) {
                            buildList.add(building.getType().getUpgradesTo());
                        }
                    }
                }
            }
        }

        Building buildingForExport = null;
        ability = "model.ability.export";
        if (!colony.hasAbility(ability)) {
            if (colony.canBuild(customHouse) &&
                colony.getGoodsContainer().hasReachedCapacity(colony.getWarehouseCapacity())) {
                buildList.add(customHouse);
            }
        }

        // Check if we should improve the warehouse:
        Building building = colony.getWarehouse();
        if (building.canBuildNext()) {
            if (colony.getGoodsContainer().hasReachedCapacity(colony.getWarehouseCapacity())) {
                buildList.add(0, building.getType().getUpgradesTo());
            } else {
                buildList.add(building.getType());
            }
        }

        building = colony.getBuildingForProducing(Goods.HAMMERS);
        if (buildList.size() > 3) {
            if (building == null) {
                if (colony.canBuild(carpenter)) {
                    buildList.add(0, carpenter);
                }
            } else if (building.canBuildNext()) {
                buildList.add(0, building.getType().getUpgradesTo());
            }
        }

        building = colony.getBuildingForProducing(Goods.HORSES);
        if (colony.getProductionOf(Goods.HORSES) > 2) {
            if (building == null) {
                if (colony.canBuild(stables)) {
                    buildList.add(stables);
                }
            } else if (building.canBuildNext()) {
                buildList.add(building.getType().getUpgradesTo());
            }
        }

        building = colony.getStockade();
        if (building == null) {
            if (colony.canBuild(stockade)) {
                buildList.add(stockade);
            }
        } else if (building.canBuildNext()) {
            buildList.add(building.getType().getUpgradesTo());
        }

        building = colony.getBuildingForProducing(Goods.MUSKETS);
        if (building == null) {
            if (colony.canBuild(armory)) {
                buildList.add(armory);
            }
        } else if (building.canBuildNext()) {
            buildList.add(building.getType().getUpgradesTo());
        }
        
        buildList.add(FreeCol.getSpecification().getUnitType("model.unit.artillery"));

        ability = "model.ability.teach";
        if (!colony.hasAbility(ability)) {
            if (colony.canBuild(schoolhouse)) {
                buildList.add(schoolhouse);
            }
        }

        return buildList.iterator();
    }

    /**
     * Gets the main AI-object.
     * 
     * @return The main AI-object.
     */
    public AIMain getAIMain() {
        return aiMain;
    }

    /**
     * Get the <code>Game</code> this object is associated to.
     * 
     * @return The <code>Game</code>.
     */
    public Game getGame() {
        return aiMain.getGame();
    }

    /**
     * Creates a plan for this colony. That is; determines what type of goods
     * each tile should produce and what type of goods that should be
     * manufactored.
     */
    public void create() {
        
        // TODO: Erik - adapt plan to colony profile
        // Colonies should be able to specialize, determine role by colony
        // resources, buildings and specialists
        
        workLocationPlans.clear();
        Building carpenter = colony.getBuildingForProducing(Goods.HAMMERS);
        Building townHall = colony.getBuildingForProducing(Goods.BELLS);

        // Choose the best production for each tile:
        for (ColonyTile ct : colony.getColonyTiles()) {

            if (ct.getWorkTile().getOwningSettlement() != null &&
                ct.getWorkTile().getOwningSettlement() != colony || ct.isColonyCenterTile()) {
                continue;
            }

            GoodsType goodsType = getBestGoodsToProduce(ct.getWorkTile());
            WorkLocationPlan wlp = new WorkLocationPlan(getAIMain(), ct, goodsType);
            workLocationPlans.add(wlp);
        }

        // Ensure that we produce lumber:
        if (getProductionOf(Goods.LUMBER) <= 0) {
            WorkLocationPlan bestChoice = null;
            int highestPotential = 0;

            Iterator<WorkLocationPlan> wlpIterator = workLocationPlans.iterator();
            while (wlpIterator.hasNext()) {
                WorkLocationPlan wlp = wlpIterator.next();
                // TODO: find out about unit working here, if any (?)
                if (wlp.getWorkLocation() instanceof ColonyTile
                    && ((ColonyTile) wlp.getWorkLocation()).getWorkTile().potential(Goods.LUMBER, null) > highestPotential) {
                    highestPotential = ((ColonyTile) wlp.getWorkLocation()).getWorkTile().potential(Goods.LUMBER, null);
                    bestChoice = wlp;
                }
            }
            if (highestPotential > 0) {
                // this must be true because it is the only way to
                // increase highestPotential
                assert bestChoice != null;
                bestChoice.setGoodsType(Goods.LUMBER);
            }
        }

        // Determine the primary and secondary types of goods:
        GoodsType primaryRawMaterial = null;
        int primaryRawMaterialProduction = 0;
        GoodsType secondaryRawMaterial = null;
        int secondaryRawMaterialProduction = 0;
        List<GoodsType> goodsTypeList = FreeCol.getSpecification().getGoodsTypeList();
        for (GoodsType goodsType : goodsTypeList) {
            if (goodsType.getProducedMaterial() == null) {
                continue;
            }
            if (getProductionOf(goodsType) > primaryRawMaterialProduction) {
                secondaryRawMaterial = primaryRawMaterial;
                secondaryRawMaterialProduction = primaryRawMaterialProduction;
                primaryRawMaterial = goodsType;
                primaryRawMaterialProduction = getProductionOf(goodsType);
            } else if (getProductionOf(goodsType) > secondaryRawMaterialProduction) {
                secondaryRawMaterial = goodsType;
                secondaryRawMaterialProduction = getProductionOf(goodsType);
            }
        }

        // Produce food instead of goods not being primary, secondary, lumber,
        // ore or silver:
        // Stop producing if the amount of goods being produced is too low:
        Iterator<WorkLocationPlan> wlpIterator = workLocationPlans.iterator();
        while (wlpIterator.hasNext()) {
            WorkLocationPlan wlp = wlpIterator.next();
            if (!(wlp.getWorkLocation() instanceof ColonyTile)) {
                continue;
            }
            if (wlp.getGoodsType() == primaryRawMaterial || wlp.getGoodsType() == secondaryRawMaterial
                    || wlp.getGoodsType() == Goods.LUMBER || wlp.getGoodsType() == Goods.ORE
                    || wlp.getGoodsType() == Goods.SILVER) {
                continue;
            }
            // TODO: find out about unit working here, if any (?)
            if (((ColonyTile) wlp.getWorkLocation()).getWorkTile().potential(Goods.FOOD, null) <= 2) {
                if (wlp.getGoodsType() == null) {
                    // on arctic tiles nothing can be produced
                    wlpIterator.remove();
                } else if (wlp.getProductionOf(wlp.getGoodsType()) <= 2) {
                    // just a poor location
                    wlpIterator.remove();
                }
                continue;
            }

            wlp.setGoodsType(Goods.FOOD);
        }

        // Place a carpenter:
        if (getProductionOf(Goods.LUMBER) > 0) {
            WorkLocationPlan wlp = new WorkLocationPlan(getAIMain(),
                    colony.getBuildingForProducing(Goods.HAMMERS), Goods.HAMMERS);
            workLocationPlans.add(wlp);
        }

        // Place a statesman:
        WorkLocationPlan townHallWlp = new WorkLocationPlan(getAIMain(), townHall, Goods.BELLS);
        workLocationPlans.add(townHallWlp);

        // Place a colonist to manufacture the primary goods:
        if (primaryRawMaterial != null) {
            GoodsType producedGoods = primaryRawMaterial.getProducedMaterial();
            Building b = colony.getBuildingForProducing(producedGoods);
            if (b != null) {
                WorkLocationPlan wlp = new WorkLocationPlan(getAIMain(), b, producedGoods);
                workLocationPlans.add(wlp);
            }
        }

        // Remove the secondary goods if we need food:
        if (getFoodProduction() < workLocationPlans.size() * Colony.FOOD_CONSUMPTION &&
            secondaryRawMaterial.isNewWorldGoodsType()) {
            Iterator<WorkLocationPlan> wlpIterator2 = workLocationPlans.iterator();
            while (wlpIterator2.hasNext()) {
                WorkLocationPlan wlp = wlpIterator2.next();
                if (wlp.getWorkLocation() instanceof ColonyTile && wlp.getGoodsType() == secondaryRawMaterial) {
                    Tile t = ((ColonyTile) wlp.getWorkLocation()).getWorkTile();
                    // TODO: find out about unit working here, if any (?)
                    if (t.getMaximumPotential(Goods.FOOD, null) > 2) {
                        wlp.setGoodsType(Goods.FOOD);
                    } else {
                        wlpIterator2.remove();
                    }
                }
            }
        }

        // Remove the workers on the primary goods one-by-one if we need food:
        if (getFoodProduction() < workLocationPlans.size() * Colony.FOOD_CONSUMPTION) {
            Iterator<WorkLocationPlan> wlpIterator2 = workLocationPlans.iterator();
            while (wlpIterator2.hasNext() && getFoodProduction() < workLocationPlans.size() * Colony.FOOD_CONSUMPTION) {
                WorkLocationPlan wlp = wlpIterator2.next();
                if (wlp.getWorkLocation() instanceof ColonyTile && wlp.getGoodsType() == primaryRawMaterial) {
                    Tile t = ((ColonyTile) wlp.getWorkLocation()).getWorkTile();
                    // TODO: find out about unit working here, if any (?)
                    if (t.getMaximumPotential(Goods.FOOD, null) > 2) {
                        wlp.setGoodsType(Goods.FOOD);
                    } else {
                        wlpIterator2.remove();
                    }
                }
            }
        }

        // Remove the manufacturer if we still lack food:
        if (getFoodProduction() < workLocationPlans.size() * Colony.FOOD_CONSUMPTION) {
            Iterator<WorkLocationPlan> wlpIterator2 = workLocationPlans.iterator();
            while (wlpIterator2.hasNext() && getFoodProduction() < workLocationPlans.size() * Colony.FOOD_CONSUMPTION) {
                WorkLocationPlan wlp = wlpIterator2.next();
                if (wlp.getWorkLocation() instanceof Building) {
                    Building b = (Building) wlp.getWorkLocation();
                    if (b != carpenter && b != townHall) {
                        wlpIterator2.remove();
                    }
                }
            }
        }

        // Remove the lumberjacks if we still lack food:
        if (getFoodProduction() < workLocationPlans.size() * Colony.FOOD_CONSUMPTION) {
            Iterator<WorkLocationPlan> wlpIterator2 = workLocationPlans.iterator();
            while (wlpIterator2.hasNext() && getFoodProduction() < workLocationPlans.size() * Colony.FOOD_CONSUMPTION) {
                WorkLocationPlan wlp = wlpIterator2.next();
                if (wlp.getWorkLocation() instanceof ColonyTile && wlp.getGoodsType() == Goods.LUMBER) {
                    wlpIterator2.remove();
                }
            }
        }

        // Remove the carpenter if we have no lumber or lack food:
        // TODO: Erik - run short on lumber as long as there is a stockpile!
        if (getProductionOf(Goods.LUMBER) < 1 || getFoodProduction() < workLocationPlans.size() * Colony.FOOD_CONSUMPTION) {
            Iterator<WorkLocationPlan> wlpIterator2 = workLocationPlans.iterator();
            while (wlpIterator2.hasNext() && getFoodProduction() < workLocationPlans.size() * Colony.FOOD_CONSUMPTION) {
                WorkLocationPlan wlp = wlpIterator2.next();
                if (wlp.getWorkLocation() instanceof Building) {
                    Building b = (Building) wlp.getWorkLocation();
                    if (b == carpenter) {
                        wlpIterator2.remove();
                    }
                }
            }
        }

        // Remove all other colonists in buildings if we still are lacking food:
        if (getFoodProduction() < workLocationPlans.size() * Colony.FOOD_CONSUMPTION) {
            Iterator<WorkLocationPlan> wlpIterator2 = workLocationPlans.iterator();
            while (wlpIterator2.hasNext() && getFoodProduction() < workLocationPlans.size() * Colony.FOOD_CONSUMPTION) {
                WorkLocationPlan wlp = wlpIterator2.next();
                if (wlp.getWorkLocation() instanceof Building) {
                    wlpIterator2.remove();
                }
            }
        }

        int primaryWorkers = 1;
        int secondaryWorkers = 0;
        int carpenters = 1;
        int gunsmiths = 0;
        boolean colonistAdded = true;
        while (colonistAdded) {
            boolean blacksmithAdded = false;

            // Add a manufacturer for the secondary type of goods:
            if (getFoodProduction() >= workLocationPlans.size() * Colony.FOOD_CONSUMPTION + 2 &&
                secondaryRawMaterial != null &&
                12 * secondaryWorkers + 6 <= getProductionOf(secondaryRawMaterial) &&
                secondaryWorkers <= MAX_LEVEL) {
                GoodsType producedGoods = secondaryRawMaterial.getProducedMaterial();
                Building b = colony.getBuildingForProducing(producedGoods);
                if (b != null) {
                    WorkLocationPlan wlp = new WorkLocationPlan(getAIMain(), b, producedGoods);
                    workLocationPlans.add(wlp);
                    colonistAdded = true;
                    secondaryWorkers++;
                    if (secondaryRawMaterial == Goods.ORE) {
                        blacksmithAdded = true;
                    }
                }
            }

            // Add a manufacturer for the primary type of goods:
            if (getFoodProduction() >= workLocationPlans.size() * Colony.FOOD_CONSUMPTION + 2 && primaryRawMaterial != null
                    && 12 * primaryWorkers + 6 <= getProductionOf(primaryRawMaterial)
                    && primaryWorkers <= MAX_LEVEL) {
                GoodsType producedGoods = primaryRawMaterial.getProducedMaterial();
                Building b = colony.getBuildingForProducing(producedGoods);
                if (b != null) {
                    WorkLocationPlan wlp = new WorkLocationPlan(getAIMain(), b, producedGoods);
                    workLocationPlans.add(wlp);
                    colonistAdded = true;
                    primaryWorkers++;
                    if (primaryRawMaterial == Goods.ORE) {
                        blacksmithAdded = true;
                    }
                }
            }

            // Add a gunsmith:
            if (blacksmithAdded && getFoodProduction() >= workLocationPlans.size() * Colony.FOOD_CONSUMPTION + 2
                    && gunsmiths < MAX_LEVEL) {
                Building b = colony.getBuildingForProducing(Goods.MUSKETS);
                if (b != null) {
                    WorkLocationPlan wlp = new WorkLocationPlan(getAIMain(), b, Goods.MUSKETS);
                    workLocationPlans.add(wlp);
                    colonistAdded = true;
                    gunsmiths++;
                }
            }

            // Add carpenters:
            if (getFoodProduction() >= workLocationPlans.size() * Colony.FOOD_CONSUMPTION + 2
                    && 12 * carpenters + 6 <= getProductionOf(Goods.LUMBER) && carpenters <= MAX_LEVEL) {
                if (carpenter != null) {
                    WorkLocationPlan wlp = new WorkLocationPlan(getAIMain(), carpenter, Goods.HAMMERS);
                    workLocationPlans.add(wlp);
                    colonistAdded = true;
                    carpenters++;
                }
            }

            // TODO: Add worker to armory.

            colonistAdded = false;
        }

        // TODO: Add statesman
        // TODO: Add teacher
        // TODO: Add preacher
    }

    /**
     * Returns the production of the given type of goods according to this plan.
     * 
     * @param goodsType The type of goods to check the production for.
     * @return The maximum possible production of the given type of goods
     *         according to this <code>ColonyPlan</code>.
     */
    public int getProductionOf(GoodsType goodsType) {
        int amount = 0;

        Iterator<WorkLocationPlan> wlpIterator = workLocationPlans.iterator();
        while (wlpIterator.hasNext()) {
            WorkLocationPlan wlp = wlpIterator.next();
            amount += wlp.getProductionOf(goodsType);
        }

        // Add values for the center tile:
        if (goodsType == colony.getTile().primaryGoods() ||
            goodsType == colony.getTile().secondaryGoods()) {
            // TODO: find out about unit working here, if any (?)
            amount += colony.getTile().getMaximumPotential(goodsType, null);
        }

        return amount;
    }

    /**
     * Returns the production of food accoring to this plan.
     * 
     * @return The maximum possible food production
     *         according to this <code>ColonyPlan</code>.
     */
    public int getFoodProduction() {
        int amount = 0;
        for (GoodsType foodType : FreeCol.getSpecification().getGoodsFood()) {
            Iterator<WorkLocationPlan> wlpIterator = workLocationPlans.iterator();
            while (wlpIterator.hasNext()) {
                WorkLocationPlan wlp = wlpIterator.next();
                amount += wlp.getProductionOf(foodType);
            }

            // Add values for the center tile:
            if (foodType == colony.getTile().primaryGoods() ||
                foodType == colony.getTile().secondaryGoods()) {
                // TODO: find out about unit working here, if any (?)
                amount += colony.getTile().getMaximumPotential(foodType, null);
            }
        }

        return amount;
    }

    /**
     * Determines the best goods to produce on a given <code>Tile</code>
     * within this colony.
     * 
     * @param t The <code>Tile</code>.
     * @return The type of goods.
     */
    private GoodsType getBestGoodsToProduce(Tile t) {
        if (t.hasResource()) {
            return t.getTileItemContainer().getResource().getBestGoodsType();
        } else {
            List<AbstractGoods> sortedPotentials = t.getSortedPotential();
            if (sortedPotentials.isEmpty()) {
                return null;
            } else {
                return sortedPotentials.get(0).getType();
            }
        }
    }

    /**
     * Gets the <code>Colony</code> this <code>ColonyPlan</code> controls.
     * 
     * @return The <code>Colony</code>.
     */
    public Colony getColony() {
        return colony;
    }

    /**
     * Creates an XML-representation of this object.
     * 
     * @param document The <code>Document</code> in which the
     *            XML-representation should be created.
     * @return The XML-representation.
     */
    public Element toXMLElement(Document document) {
        Element element = document.createElement(getXMLElementTagName());

        element.setAttribute("ID", colony.getId());

        return element;
    }

    /**
     * Updates this object from an XML-representation of a
     * <code>ColonyPlan</code>.
     * 
     * @param element The XML-representation.
     */
    public void readFromXMLElement(Element element) {
        colony = (Colony) getAIMain().getFreeColGameObject(element.getAttribute("ID"));
    }

    /**
     * Returns the tag name of the root element representing this object.
     * 
     * @return "colonyPlan"
     */
    public static String getXMLElementTagName() {
        return "colonyPlan";
    }
}
