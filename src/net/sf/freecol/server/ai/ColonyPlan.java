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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import net.sf.freecol.common.Specification;
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
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;

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
    private static final int MIN_RAW_GOODS_THRESHOLD = 20;
    
    private static final GoodsType hammersType = Specification.getSpecification().getGoodsType("model.goods.hammers");
    private static final GoodsType toolsType = Specification.getSpecification().getGoodsType("model.goods.tools");
    private static final GoodsType lumberType = Specification.getSpecification().getGoodsType("model.goods.lumber");
    private static final GoodsType oreType = Specification.getSpecification().getGoodsType("model.goods.ore");

    /**
     * The FreeColGameObject this AIObject contains AI-information for.
     */
    private Colony colony;

    private AIMain aiMain;

    private ArrayList<WorkLocationPlan> workLocationPlans = new ArrayList<WorkLocationPlan>();

    private GoodsType primaryRawMaterial = null;
    
    private GoodsType secondaryRawMaterial = null;
    
    
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
        Collections.sort(workLocationPlans);

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

        Set<BuildableType> lowPriority = new LinkedHashSet<BuildableType>();
        Set<BuildableType> highPriority = new LinkedHashSet<BuildableType>();
        Set<BuildableType> normalPriority = new LinkedHashSet<BuildableType>();

        List<BuildingType> docks = new ArrayList<BuildingType>();
        List<BuildingType> customs = new ArrayList<BuildingType>();
        List<BuildingType> builders = new ArrayList<BuildingType>();
        List<BuildingType> defence = new ArrayList<BuildingType>();
        List<BuildingType> military = new ArrayList<BuildingType>();
        List<BuildingType> schools = new ArrayList<BuildingType>();

        for (BuildingType type : Specification.getSpecification().getBuildingTypeList()) {
            if (type.hasAbility("model.ability.produceInWater")) {
                docks.add(type);
            } 
            if (type.hasAbility("model.ability.export")) {
                customs.add(type);
            } 
            if (type.hasAbility("model.ability.teach")) {
                schools.add(type);
            }
            if (!type.getModifierSet("model.modifier.defence").isEmpty()) {
                defence.add(type);
            }
            if (type.getProducedGoodsType() != null) {
                if (type.getProducedGoodsType().isBuildingMaterial()) {
                    builders.add(type);
                }
                if (type.getProducedGoodsType().isMilitaryGoods()) {
                    military.add(type);
                }
            }
        }

        List<UnitType> buildableDefenders = new ArrayList<UnitType>();
        UnitType bestWagon = null;
        for (UnitType unitType : Specification.getSpecification().getUnitTypeList()) {
            if (unitType.getDefence() > UnitType.DEFAULT_DEFENCE
                && !unitType.hasAbility("model.ability.navalUnit")
                && !unitType.getGoodsRequired().isEmpty()) {
                buildableDefenders.add(unitType);
            }
            if (unitType.hasAbility("model.ability.carryGoods")
                && !unitType.hasAbility("model.ability.navalUnit")
                && colony.canBuild(unitType)
                && (bestWagon == null || unitType.getSpace() > bestWagon.getSpace())) {
                bestWagon = unitType;
            }
        }

        int wagonTrains = 0;
        for (Unit unit: colony.getOwner().getUnits()) {
            if (unit.hasAbility("model.ability.carryGoods") && !unit.isNaval()) {
                wagonTrains++;
            }
        }

        if (colony.isLandLocked()) {
            // landlocked colonies need transportation
            int landLockedColonies = 0;
            for (Colony otherColony : colony.getOwner().getColonies()) {
                if (otherColony.isLandLocked()) {
                    landLockedColonies++;
                }
            }
            if (bestWagon != null) {
                if (landLockedColonies > 2 * wagonTrains) {
                    highPriority.add(bestWagon);
                } else if (landLockedColonies > wagonTrains) {
                    normalPriority.add(bestWagon);
                }
            }
        } else if (!colony.hasAbility("model.ability.produceInWater")) {
            // coastal colonies need docks
            int potential = 0;
            for (ColonyTile colonyTile : colony.getColonyTiles()) {
                if (!colonyTile.getTile().isLand()) {
                    List<AbstractGoods> production = colonyTile.getTile().getSortedPotential();
                    if (!production.isEmpty()) {
                        potential += production.get(0).getAmount();
                    }
                }
            }
            for (BuildingType buildingType : docks) {
                if (colony.canBuild(buildingType)) {
                    if (potential > 15) {
                        highPriority.add(buildingType);
                    } else if (potential > 10) {
                        normalPriority.add(buildingType);
                    } else {
                        lowPriority.add(buildingType);
                    }
                    break;
                }
            }
        }


        // if we are using a building, we should upgrade it, if possible
        Iterator<WorkLocationPlan> wlpIt = getSortedWorkLocationPlans().iterator();
        while (wlpIt.hasNext()) {
            WorkLocationPlan wlp = wlpIt.next();
            if (wlp.getWorkLocation() instanceof Building) {
                Building b = (Building) wlp.getWorkLocation();
                if (b.canBuildNext()) {
                    normalPriority.add(b.getType().getUpgradesTo());
                }

                GoodsType outputType = b.getGoodsOutputType();
                if (outputType != null) {
                    for (Building building : colony.getBuildings()) {
                        if (!building.getType().getModifierSet(outputType.getId()).isEmpty()
                            && building.canBuildNext()) {
                            normalPriority.add(building.getType().getUpgradesTo());
                        }
                    }
                }
            }
        }

        // build custom house as soon as possible, in order to free
        // transports for other duties (and avoid pirates, and
        // possibly boycotts)
        if (!colony.hasAbility("model.ability.export")) {
            for (BuildingType buildingType : customs) {
                if (colony.canBuild(buildingType)) {
                    highPriority.add(buildingType);
                    break;
                }
            }
        }

        // improve production of building materials
        for (BuildingType buildingType : builders) {
            if (colony.canBuild(buildingType)) {
                if (colony.getBuilding(buildingType) == null) {
                    highPriority.add(buildingType);
                } else {
                    normalPriority.add(buildingType);
                }
            }
        }

        // Check if we should improve the warehouse:
        Building building = colony.getWarehouse();
        if (building.canBuildNext()) {
            if (colony.getGoodsContainer().hasReachedCapacity(colony.getWarehouseCapacity())) {
                highPriority.add(building.getType().getUpgradesTo());
            } else {
                normalPriority.add(building.getType());
            }
        } else if (bestWagon != null && wagonTrains < 4 * colony.getOwner().getColonies().size()) {
            lowPriority.add(bestWagon);
        }

        // improve defences
        for (BuildingType buildingType : defence) {
            if (colony.canBuild(buildingType)) {
                if (colony.getBuilding(buildingType) == null) {
                    highPriority.add(buildingType);
                } else {
                    normalPriority.add(buildingType);
                }
            }
        }

        // improve military production
        for (BuildingType buildingType : military) {
            if (colony.canBuild(buildingType)) {
                if (colony.getBuilding(buildingType) == null
                    && (buildingType.getConsumedGoodsType() == null 
                        || buildingType.getConsumedGoodsType().isFarmed())) {
                    normalPriority.add(buildingType);
                } else {
                    lowPriority.add(buildingType);
                }
            }
        }

        // add artillery if necessary
        // TODO: at some point, we will have to add ships
        int defenders = 0;
        for (Unit unit : colony.getTile().getUnitList()) {
            if (unit.isDefensiveUnit()) {
                defenders++;
            }
        }

        if (defenders < 5) {
            for (UnitType unitType : buildableDefenders) {
                if (colony.canBuild(unitType)) {
                    highPriority.add(unitType);
                    break;
                }
            }
        } else if (defenders < 10) {
            for (UnitType unitType : buildableDefenders) {
                if (colony.canBuild(unitType)) {
                    normalPriority.add(unitType);
                    break;
                }
            }
        }        

        // improve education
        for (BuildingType buildingType : schools) {
            if (colony.canBuild(buildingType)) {
                if (colony.getBuilding(buildingType) == null) {
                    normalPriority.add(buildingType);
                } else {
                    lowPriority.add(buildingType);
                }
            }
        }

        highPriority.addAll(normalPriority);
        highPriority.addAll(lowPriority);

        // catch all
        for (BuildingType buildingType : Specification.getSpecification().getBuildingTypeList()) {
            if (colony.canBuild(buildingType) && !highPriority.contains(buildingType)) {
                highPriority.add(buildingType);
            }
        }
        for (UnitType unitType : buildableDefenders) {
            if (colony.canBuild(unitType) && !highPriority.contains(unitType)) {
                highPriority.add(unitType);
            }
        }

        return highPriority.iterator();
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
     * manufactured.
     */
    public void create() {
        
        // TODO: Erik - adapt plan to colony profile
        // Colonies should be able to specialize, determine role by colony
        // resources, buildings and specialists
                
        workLocationPlans.clear();
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
        
        // We need to find what, if any, is still required for what we are building
        GoodsType buildingReq = null;
        GoodsType buildingRawMat = null;
        Building buildingReqProducer = null;
        
        buildingReq = getBuildingReqGoods();
        
        if(buildingReq != null){
            if(buildingReq == hammersType){
                buildingRawMat = lumberType;
            }
            else{
                buildingRawMat = oreType;
            }
            buildingReqProducer = colony.getBuildingForProducing(buildingReq);
        }

        // Try to ensure that we produce the raw material necessary for
        //what we are building
        boolean buildingRawMatReq = buildingRawMat != null 
                                    && colony.getGoodsCount(buildingRawMat) < MIN_RAW_GOODS_THRESHOLD
                                    && getProductionOf(buildingRawMat) <= 0; 
        
        if(buildingRawMatReq) {
            WorkLocationPlan bestChoice = null;
            int highestPotential = 0;

            Iterator<WorkLocationPlan> wlpIterator = workLocationPlans.iterator();
            while (wlpIterator.hasNext()) {
                WorkLocationPlan wlp = wlpIterator.next();
                // TODO: find out about unit working here, if any (?)
                if (wlp.getWorkLocation() instanceof ColonyTile
                    && ((ColonyTile) wlp.getWorkLocation()).getWorkTile().potential(buildingRawMat, null) > highestPotential) {
                    highestPotential = ((ColonyTile) wlp.getWorkLocation()).getWorkTile().potential(buildingRawMat, null);
                    bestChoice = wlp;
                }
            }
            if (highestPotential > 0) {
                // this must be true because it is the only way to
                // increase highestPotential
                assert bestChoice != null;
                bestChoice.setGoodsType(buildingRawMat);
            }
        }

        // Determine the primary and secondary types of goods:
        primaryRawMaterial = null;
        secondaryRawMaterial = null;
        int primaryRawMaterialProduction = 0;
        int secondaryRawMaterialProduction = 0;
        List<GoodsType> goodsTypeList = Specification.getSpecification().getGoodsTypeList();
        for (GoodsType goodsType : goodsTypeList) {
            // only consider goods that can be transformed
            // do not consider hammers as a valid transformation
            if (goodsType.getProducedMaterial() == null 
                    || goodsType.getProducedMaterial() == hammersType) {
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

        // Produce the goods required for what is being built, if:
        //     - anything is being built, and
        //     - there is either production or stock of the raw material
        if(buildingReq != null && 
            (getProductionOf(buildingRawMat) > 0 
              || colony.getGoodsCount(buildingRawMat) > 0)){
            WorkLocationPlan wlp = new WorkLocationPlan(getAIMain(),
                    colony.getBuildingForProducing(buildingReq), buildingReq);
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
                    if ( b != buildingReqProducer && b != townHall) {
                        wlpIterator2.remove();
                    }
                }
            }
        }

        // Still lacking food
        // Remove the producers of the raw and/or non-raw materials required for the build
        // The decision of which to start depends on existence or not of stock of
        //raw materials
        GoodsType buildMatToGo = buildingReq;
        if(colony.getGoodsCount(buildingRawMat) > 0){
            buildMatToGo = buildingRawMat;
        }
        if (getFoodProduction() < workLocationPlans.size() * Colony.FOOD_CONSUMPTION) {
            Iterator<WorkLocationPlan> wlpIterator2 = workLocationPlans.iterator();
            while (wlpIterator2.hasNext() && getFoodProduction() < workLocationPlans.size() * Colony.FOOD_CONSUMPTION) {
                WorkLocationPlan wlp = wlpIterator2.next();
                if (wlp.getWorkLocation() instanceof ColonyTile && wlp.getGoodsType() == buildMatToGo) {
                    wlpIterator2.remove();
                }
            }
            // still lacking food, removing the rest
            if (getFoodProduction() < workLocationPlans.size() * Colony.FOOD_CONSUMPTION) {
                buildMatToGo = (buildMatToGo == buildingRawMat)? buildingReq : buildingRawMat;
                
                wlpIterator2 = workLocationPlans.iterator();
                while (wlpIterator2.hasNext() && getFoodProduction() < workLocationPlans.size() * Colony.FOOD_CONSUMPTION) {
                    WorkLocationPlan wlp = wlpIterator2.next();
                    if (wlp.getWorkLocation() instanceof ColonyTile && wlp.getGoodsType() == buildMatToGo) {
                        wlpIterator2.remove();
                    }
                }
            }
        }        
        
        // Primary allocations done
        // Beginning secondary allocations
        
        // Not enough food for more allocations, save work and stop here
        if(getFoodProduction() < workLocationPlans.size() * Colony.FOOD_CONSUMPTION + 2){
            return;
        }

        int primaryWorkers = 1;
        int secondaryWorkers = 0;
        int builders = 1;
        int gunsmiths = 0;
        boolean colonistAdded = true;
        //XXX: This loop does not work, only goes through once, not as intended
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

            // Add builders
            if (getFoodProduction() >= workLocationPlans.size() * Colony.FOOD_CONSUMPTION + 2
                    && buildingReqProducer != null 
                    && buildingReqProducer.getProduction() * builders <= getProductionOf(buildingRawMat) 
                    && buildingReqProducer.getMaxUnits() < builders) {
                WorkLocationPlan wlp = new WorkLocationPlan(getAIMain(), buildingReqProducer, buildingReq);
                workLocationPlans.add(wlp);
                colonistAdded = true;
                builders++;
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
     * Returns the production of food according to this plan.
     * 
     * @return The maximum possible food production
     *         according to this <code>ColonyPlan</code>.
     */
    public int getFoodProduction() {
        int amount = 0;
        for (GoodsType foodType : Specification.getSpecification().getGoodsFood()) {
            amount += getProductionOf(foodType);
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
    
    public void adjustProductionAndManufacture(){
        List<GoodsType> rawMatList = new ArrayList<GoodsType>(); 
    
        if(getBuildingReqGoods() == hammersType){
            rawMatList.add(lumberType);
        }
        rawMatList.add(oreType);
        
        if (primaryRawMaterial != null
                && primaryRawMaterial != lumberType
                && primaryRawMaterial != oreType
                && !primaryRawMaterial.isFoodType()) {
            rawMatList.add(primaryRawMaterial);
        }
        
        if (secondaryRawMaterial != null
                && secondaryRawMaterial != lumberType
                && secondaryRawMaterial != oreType
                && !secondaryRawMaterial.isFoodType()) {
            rawMatList.add(secondaryRawMaterial);
        }
        
        for(GoodsType rawMat : rawMatList){
            GoodsType producedGoods = rawMat.getProducedMaterial();
            if(producedGoods == null){
                continue;
            }
            adjustProductionAndManufactureFor(rawMat,producedGoods);
        }
    }
    
    public void adjustProductionAndManufactureFor(GoodsType rawMat, GoodsType producedGoods){
        Building factory = colony.getBuildingForProducing(producedGoods);
        if(factory == null){
            return;
        }
        
        List<Unit> producers = new ArrayList<Unit>();
        int stockRawMat = colony.getGoodsCount(rawMat);
       
        for(ColonyTile t : colony.getColonyTiles()){
            if(t.isColonyCenterTile()){
                continue;
            }
            Unit u = t.getUnit();
            if(u == null){
                continue;
            }
            if(u.getWorkType() != rawMat){
                continue;
            }
            producers.add(u); 
        }
        
        if(producers.size() == 0){
            return;
        }

        // Creates comparator to order the list of producers by their production (ascending)
        Comparator<Unit> comp = new Comparator<Unit>(){
                public int compare(Unit u1, Unit u2){
                    GoodsType goodsType = u1.getWorkType();
                    int prodU1 = ((ColonyTile) u1.getLocation()).getProductionOf(u1, goodsType);
                    int prodU2 = ((ColonyTile) u2.getLocation()).getProductionOf(u2, goodsType);
                    
                    if(prodU1 > prodU2){
                        return 1;
                    }
                    if(prodU1 < prodU2){
                        return -1;
                    }
                    return 0;
                }
        };
        Collections.sort(producers, comp);
        
        // shift units gathering raw materials to production of manufactured goods
        Iterator<Unit> iter = new ArrayList<Unit>(producers).iterator();
        while(iter.hasNext()){
            // not enough stock of raw material and workers
            if(stockRawMat < 50 && producers.size() < 2){
                return;
            }
            
            if(factory.getUnitCount() == factory.getMaxUnits()){
                return;
            }
            Unit u = iter.next();
            // this particular unit cannot be added to this building
            if(!factory.canAdd(u.getType())){
                continue;
            }

            // get  the production values if the unit is shifted 
            int rawProd = colony.getProductionNextTurn(rawMat) - ((ColonyTile)u.getWorkTile()).getProductionOf(u, rawMat);
            int mfnProd = colony.getProductionNextTurn(producedGoods) + factory.getAdditionalProductionNextTurn(u);
            if(stockRawMat < 50 && rawProd < mfnProd){
                return;
            }
            
            u.work(factory);
            u.setWorkType(producedGoods);
            producers.remove(u);
        }
    }
    
    public GoodsType getBuildingReqGoods(){
        BuildableType currBuild = colony.getCurrentlyBuilding();
        if(currBuild == null){
            return null;
        }
        
        if(colony.getGoodsCount(hammersType) < currBuild.getAmountRequiredOf(hammersType)){
            return hammersType;
        }
        else{
            return toolsType;
        }
    }
    
    public GoodsType getPrimaryRawMaterial(){
        return primaryRawMaterial;
    }

    public GoodsType getSecondaryRawMaterial(){
        return secondaryRawMaterial;
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
    
    /**
     * Creates a <code>String</code> representation of this plan.
     */
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ColonyPlan for " + colony.getName() + " " + colony.getTile().getPosition());
        sb.append("\n-----\n\nWORK LOCATIONS:\n");
        for (WorkLocationPlan p : getSortedWorkLocationPlans()) {
            sb.append(p.getGoodsType().getName() + " (" + p.getWorkLocation() + ")\n");
        }
        sb.append("\n\nBUILD QUEUE:\n");
        final Iterator<BuildableType> it = getBuildable();
        while (it.hasNext()) {
            final BuildableType b = it.next();
            sb.append(b.getName());
            sb.append('\n');
        }
        return sb.toString();
    }
}
