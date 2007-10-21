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

import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;

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
        this.aiMain = aiMain;
        this.colony = colony;

        if (colony == null) {
            throw new NullPointerException("colony == null");
        }
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
    @SuppressWarnings("unchecked")
    public List<WorkLocationPlan> getWorkLocationPlans() {
        return (List<WorkLocationPlan>) workLocationPlans.clone();
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
    public Iterator<Building> getBuildable() {
        ArrayList<Building> buildList = new ArrayList<Building>();

        String ability = "model.ability.produceInWater";
        if (!colony.hasAbility(ability)) {
            Iterator<Building> iterator = colony.getBuildingIterator();
            while (iterator.hasNext()) {
                Building building = iterator.next();
                if (building.hasAbility(ability) && building.canBuildNext()) {
                    buildList.add(building);
                }
            }
        }

        Iterator<WorkLocationPlan> wlpIt = getSortedWorkLocationPlans().iterator();
        while (wlpIt.hasNext()) {
            WorkLocationPlan wlp = wlpIt.next();
            if (wlp.getWorkLocation() instanceof Building) {
                Building b = (Building) wlp.getWorkLocation();
                if (b.canBuildNext()) {
                    buildList.add(b);
                }

                GoodsType outputType = b.getGoodsOutputType();
                if (outputType != null) {
                    Iterator<Building> iterator = colony.getBuildingIterator();
                    while (iterator.hasNext()) {
                        Building building = iterator.next();
                        if (building.getType().getModifier(outputType.getId()) != null
                                && building.canBuildNext()) {
                            buildList.add(building);
                        }
                    }
                }
            }
        }

        Building buildingForExport = null;
        ability = "model.ability.export";
        if (!colony.hasAbility(ability)) {
            Iterator<Building> iterator = colony.getBuildingIterator();
            while (iterator.hasNext()) {
                Building building = iterator.next();
                if (building.hasAbility(ability)) {
                    buildingForExport = building;
                    if (building.canBuildNext() &&
                            colony.getGoodsContainer().hasReachedCapacity(colony.getWarehouseCapacity())) {
                        buildList.add(building);
                    }
                }
            }
        }

        // Check if we should improve the warehouse:
        Building building = colony.getWarehouse();
        if (building.canBuildNext()) {
            if (colony.getGoodsContainer().hasReachedCapacity(colony.getWarehouseCapacity())) {
                buildList.add(0, building);
            } else {
                buildList.add(building);
            }
        }

        building = colony.getBuildingForProducing(Goods.HAMMERS);
        if (buildList.size() > 3 && building.canBuildNext()) {
            buildList.add(0, building);
        }

        building = colony.getBuildingForProducing(Goods.HORSES);
        if (colony.getProductionOf(Goods.HORSES) > 2 && building.canBuildNext()) {
            buildList.add(building);
        }

        building = colony.getStockade();
        if (building.canBuildNext()) {
            buildList.add(building);
        }

        /** TODO: fix this
        building = colony.getBuildingForProducing(Goods.MUSKETS);
        if (building.canBuildNext() && !building.isBuilt()) {
            buildList.add(building);
        }
        */
        
        // TODO: create a interface buildable and add artillery to buildList
        //buildList.add(new Integer(Colony.BUILDING_UNIT_ADDITION + Unit.ARTILLERY));

        // buildList.add(new Integer(Building.SCHOOLHOUSE));

        if (buildingForExport.canBuildNext()) {
            buildList.add(buildingForExport);
        }

        buildList.add(null);

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
        Iterator<ColonyTile> colonyTileIterator = getColony().getColonyTileIterator();
        while (colonyTileIterator.hasNext()) {
            ColonyTile ct = colonyTileIterator.next();

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
                if (wlp.getWorkLocation() instanceof ColonyTile
                        && ((ColonyTile) wlp.getWorkLocation()).getWorkTile().potential(Goods.LUMBER) > highestPotential) {
                    highestPotential = ((ColonyTile) wlp.getWorkLocation()).getWorkTile().potential(Goods.LUMBER);
                    bestChoice = wlp;
                }
            }
            if (highestPotential > 0) {
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
            if (((ColonyTile) wlp.getWorkLocation()).getWorkTile().potential(Goods.FOOD) <= 2) {
                if (wlp.getProductionOf(wlp.getGoodsType()) <= 2) {
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
        if (getProductionOf(Goods.FOOD) < workLocationPlans.size() * 2
                && (secondaryRawMaterial == Goods.SUGAR || secondaryRawMaterial == Goods.TOBACCO
                        || secondaryRawMaterial == Goods.COTTON || secondaryRawMaterial == Goods.FURS)) {

            Iterator<WorkLocationPlan> wlpIterator2 = workLocationPlans.iterator();
            while (wlpIterator2.hasNext()) {
                WorkLocationPlan wlp = wlpIterator2.next();
                if (wlp.getWorkLocation() instanceof ColonyTile && wlp.getGoodsType() == secondaryRawMaterial) {
                    Tile t = ((ColonyTile) wlp.getWorkLocation()).getWorkTile();
                    if (t.getMaximumPotential(Goods.FOOD) > 2) {
                        wlp.setGoodsType(Goods.FOOD);
                    } else {
                        wlpIterator2.remove();
                    }
                }
            }
        }

        // Remove the workers on the primary goods one-by-one if we need food:
        if (getProductionOf(Goods.FOOD) < workLocationPlans.size() * 2) {
            Iterator<WorkLocationPlan> wlpIterator2 = workLocationPlans.iterator();
            while (wlpIterator2.hasNext() && getProductionOf(Goods.FOOD) < workLocationPlans.size() * 2) {
                WorkLocationPlan wlp = wlpIterator2.next();
                if (wlp.getWorkLocation() instanceof ColonyTile && wlp.getGoodsType() == primaryRawMaterial) {
                    Tile t = ((ColonyTile) wlp.getWorkLocation()).getWorkTile();
                    if (t.getMaximumPotential(Goods.FOOD) > 2) {
                        wlp.setGoodsType(Goods.FOOD);
                    } else {
                        wlpIterator2.remove();
                    }
                }
            }
        }

        // Remove the manufacturer if we still lack food:
        if (getProductionOf(Goods.FOOD) < workLocationPlans.size() * 2) {
            Iterator<WorkLocationPlan> wlpIterator2 = workLocationPlans.iterator();
            while (wlpIterator2.hasNext() && getProductionOf(Goods.FOOD) < workLocationPlans.size() * 2) {
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
        if (getProductionOf(Goods.FOOD) < workLocationPlans.size() * 2) {
            Iterator<WorkLocationPlan> wlpIterator2 = workLocationPlans.iterator();
            while (wlpIterator2.hasNext() && getProductionOf(Goods.FOOD) < workLocationPlans.size() * 2) {
                WorkLocationPlan wlp = wlpIterator2.next();
                if (wlp.getWorkLocation() instanceof ColonyTile && wlp.getGoodsType() == Goods.LUMBER) {
                    wlpIterator2.remove();
                }
            }
        }

        // Remove the carpenter if we have no lumber or lack food:
        // TODO: Erik - run short on lumber as long as there is a stockpile!
        if (getProductionOf(Goods.LUMBER) < 1 || getProductionOf(Goods.FOOD) < workLocationPlans.size() * 2) {
            Iterator<WorkLocationPlan> wlpIterator2 = workLocationPlans.iterator();
            while (wlpIterator2.hasNext() && getProductionOf(Goods.FOOD) < workLocationPlans.size() * 2) {
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
        if (getProductionOf(Goods.FOOD) < workLocationPlans.size() * 2) {
            Iterator<WorkLocationPlan> wlpIterator2 = workLocationPlans.iterator();
            while (wlpIterator2.hasNext() && getProductionOf(Goods.FOOD) < workLocationPlans.size() * 2) {
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
            if (getProductionOf(Goods.FOOD) >= workLocationPlans.size() * 2 + 2 && secondaryRawMaterial != null
                    && 12 * secondaryWorkers + 6 <= getProductionOf(secondaryRawMaterial)
                    && secondaryWorkers <= Building.MAX_LEVEL) {
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
            if (getProductionOf(Goods.FOOD) >= workLocationPlans.size() * 2 + 2 && primaryRawMaterial != null
                    && 12 * primaryWorkers + 6 <= getProductionOf(primaryRawMaterial)
                    && primaryWorkers <= Building.MAX_LEVEL) {
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
            if (blacksmithAdded && getProductionOf(Goods.FOOD) >= workLocationPlans.size() * 2 + 2
                    && gunsmiths < Building.MAX_LEVEL) {
                Building b = colony.getBuildingForProducing(Goods.MUSKETS);
                if (b != null) {
                    WorkLocationPlan wlp = new WorkLocationPlan(getAIMain(), b, Goods.MUSKETS);
                    workLocationPlans.add(wlp);
                    colonistAdded = true;
                    gunsmiths++;
                }
            }

            // Add carpenters:
            if (getProductionOf(Goods.FOOD) >= workLocationPlans.size() * 2 + 2
                    && 12 * carpenters + 6 <= getProductionOf(Goods.LUMBER) && carpenters <= Building.MAX_LEVEL) {
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
     * Returns the production of the given type of goods accoring to this plan.
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
        if (goodsType == Goods.FOOD) {
            amount += colony.getTile().getMaximumPotential(Goods.FOOD);
        } else if (goodsType == colony.getTile().secondaryGoods()) {
            amount += colony.getTile().getMaximumPotential(goodsType);
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
        }
        GoodsType[] top = Tile.getSortedGoodsTop(t.getType(), t.getTileItemContainer(), t.getFishBonus());
        return top[0];
    }
/*
        if (t.isForested() && t.hasBonus()) {
            if (t.getType() == Tile.GRASSLANDS || t.getType() == Tile.SAVANNAH) {
                return Goods.LUMBER;
            } else {
                return Goods.FURS;
            }
        }
        if (t.getAddition() == Tile.ADD_HILLS) {
            return Goods.ORE;
        }
        if (t.getAddition() == Tile.ADD_MOUNTAINS) {
            if (t.hasBonus()) {
                return Goods.SILVER;
            } else {
                return Goods.ORE;
            }
        }
        if (!t.isLand()) {
            return Goods.FOOD;
        }
        if (t.getType() == Tile.DESERT) {
            if (t.hasBonus()) {
                return Goods.FOOD;
            } else {
                return Goods.ORE;
            }
        }
        switch (t.getType()) {
        case Tile.SWAMP:
        case Tile.PLAINS:
        case Tile.TUNDRA:
        case Tile.MARSH:
            return Goods.FOOD;
        case Tile.PRAIRIE:
            return Goods.COTTON;
        case Tile.GRASSLANDS:
            return Goods.TOBACCO;
        case Tile.SAVANNAH:
            return Goods.SUGAR;
        case Tile.ARCTIC:
        default:
            return Goods.ORE;
        }
    }
*/

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
