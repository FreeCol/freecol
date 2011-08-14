/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.server.ai.ColonyProfile.ProfileType;

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

    // gets multiplied by the number of fish produced
    public static final int DOCKS_PRIORITY = 10;
    public static final int ARTILLERY_PRIORITY = 10;
    public static final int CHURCH_PRIORITY = 15;
    public static final int WAGON_TRAIN_PRIORITY = 20;
    public static final int SCHOOL_PRIORITY = 30;
    public static final int UPGRADE_PRIORITY = 50;
    public static final int CUSTOMS_HOUSE_PRIORITY = 60;
    public static final int TOWN_HALL_PRIORITY = 75;
    public static final int WAREHOUSE_PRIORITY = 90;
    public static final int BUILDING_PRIORITY = 120;

    // What is this supposed to be? Is it the maximum number of units
    // per building?
    private static final int MAX_LEVEL = 3;
    private static final int MIN_RAW_GOODS_THRESHOLD = 20;

    /**
     * The FreeColGameObject this AIObject contains AI-information for.
     */
    private Colony colony;

    private AIMain aiMain;

    private ArrayList<WorkLocationPlan> workLocationPlans = new ArrayList<WorkLocationPlan>();

    private GoodsType primaryRawMaterial = null;

    private GoodsType secondaryRawMaterial = null;

    /**
     * Describe profile here.
     */
    private ColonyProfile profile;



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
        selectProfile();
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
     * Get the <code>Profile</code> value.
     *
     * @return a <code>ColonyProfile</code> value
     */
    public final ColonyProfile getProfile() {
        return profile;
    }

    /**
     * Set the <code>Profile</code> value.
     *
     * @param newProfile The new Profile value.
     */
    public final void setProfile(final ColonyProfile newProfile) {
        this.profile = newProfile;
    }

    public class Buildable implements Comparable<Buildable> {
        BuildableType type;
        int priority;

        public Buildable(BuildableType type, int priority) {
            this.type = type;
            this.priority = priority;
        }

        public int compareTo(Buildable other) {
            return other.priority - priority;
        }
    }

    /**
     * Gets an <code>Iterator</code> for everything to be built in the
     * <code>Colony</code>.
     *
     * @return An iterator containing all the <code>Buildable</code> sorted by
     *         priority (highest priority first).
     */
    public Iterator<BuildableType> getBuildable() {

        // don't build anything in colonies likely to be abandoned
        if (profile.getType() == ProfileType.OUTPOST) {
            List<BuildableType> result = Collections.emptyList();
            return result.iterator();
        }

        List<Buildable> buildables = new ArrayList<Buildable>();

        List<BuildingType> docks = new ArrayList<BuildingType>();
        List<BuildingType> customs = new ArrayList<BuildingType>();
        List<BuildingType> builders = new ArrayList<BuildingType>();
        List<BuildingType> defence = new ArrayList<BuildingType>();
        List<BuildingType> military = new ArrayList<BuildingType>();
        List<BuildingType> schools = new ArrayList<BuildingType>();
        List<BuildingType> churches = new ArrayList<BuildingType>();
        List<BuildingType> townHalls = new ArrayList<BuildingType>();

        for (BuildingType type : colony.getSpecification().getBuildingTypeList()) {
            if (type.hasAbility(Ability.PRODUCE_IN_WATER)) {
                docks.add(type);
            }
            if (type.hasAbility(Ability.EXPORT)) {
                customs.add(type);
            }
            if (type.hasAbility(Ability.CAN_TEACH)) {
                schools.add(type);
            }
            if (!type.getModifierSet("model.modifier.defence").isEmpty()) {
                defence.add(type);
            }
            if (type.getProducedGoodsType() != null) {
                GoodsType output = type.getProducedGoodsType();
                if (output.isBuildingMaterial()) {
                    builders.add(type);
                }
                if (output.isMilitaryGoods()) {
                    military.add(type);
                }
                if (output.isLibertyType()) {
                    townHalls.add(type);
                }
                if (output.isImmigrationType()) {
                    churches.add(type);
                }
            }
        }

        List<UnitType> buildableDefenders = new ArrayList<UnitType>();
        UnitType bestWagon = null;
        for (UnitType unitType : colony.getSpecification().getUnitTypeList()) {
            if (!unitType.hasAbility(Ability.NAVAL_UNIT)
                && colony.canBuild(unitType)) {
                if (unitType.getDefence() > UnitType.DEFAULT_DEFENCE
                    && !unitType.getGoodsRequired().isEmpty()) {
                    buildableDefenders.add(unitType);
                }
                if (unitType.hasAbility(Ability.CARRY_GOODS)
                    && !unitType.hasAbility(Ability.NAVAL_UNIT)
                    && (bestWagon == null || unitType.getSpace() > bestWagon.getSpace())) {
                    bestWagon = unitType;
                }
            }
        }

        int wagonTrains = 0;
        for (Unit unit: colony.getOwner().getUnits()) {
            if (unit.hasAbility(Ability.CARRY_GOODS) && !unit.isNaval()) {
                wagonTrains++;
            }
        }

        if (!colony.isConnected()) {
            // disconnected colonies need transportation
            if (bestWagon != null) {
                int disconnectedColonies = 0;
                for (Colony otherColony : colony.getOwner().getColonies()) {
                    if (!otherColony.isConnected()) {
                        disconnectedColonies++;
                    }
                }
                if (disconnectedColonies > wagonTrains) {
                    buildables.add(new Buildable(bestWagon, WAGON_TRAIN_PRIORITY
                                                 * (disconnectedColonies - wagonTrains)));
                }
            }
        } else if (!colony.hasAbility(Ability.PRODUCE_IN_WATER)) {
            // coastal colonies need docks
            int potential = 0;
            for (ColonyTile colonyTile : colony.getColonyTiles()) {
                Tile tile = colonyTile.getWorkTile();
                if (!tile.isLand()) {
                    for (AbstractGoods goods : tile.getSortedPotential()) {
                        if (goods.getType().isFoodType()) {
                            potential += goods.getAmount();
                            break;
                        }
                    }
                }
            }
            for (BuildingType buildingType : docks) {
                if (colony.canBuild(buildingType)) {
                    buildables.add(new Buildable(buildingType, potential * DOCKS_PRIORITY));
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
                    buildables.add(new Buildable(b.getType().getUpgradesTo(), UPGRADE_PRIORITY));
                }

                // this handles buildings that increase the production
                // of other buildings (printing press, newspaper)
                GoodsType outputType = b.getGoodsOutputType();
                if (outputType != null) {
                    for (BuildingType otherType : colony.getSpecification()
                             .getBuildingTypeList()) {
                        if (!otherType.getModifierSet(outputType.getId()).isEmpty()
                            && colony.canBuild(otherType)) {
                            int priority = (colony.getBuilding(otherType) == null) ?
                                2 * UPGRADE_PRIORITY : UPGRADE_PRIORITY;
                            buildables.add(new Buildable(otherType, priority));
                        }
                    }
                }
            }
        }

        // build custom house as soon as possible, in order to free
        // transports for other duties (and avoid pirates, and
        // possibly boycotts)
        if (!colony.hasAbility(Ability.EXPORT)) {
            for (BuildingType buildingType : customs) {
                if (colony.canBuild(buildingType)) {
                    buildables.add(new Buildable(buildingType, CUSTOMS_HOUSE_PRIORITY));
                    break;
                }
            }
        }

        // improve production of building materials
        for (BuildingType buildingType : builders) {
            if (colony.canBuild(buildingType)) {
                int priority = BUILDING_PRIORITY;
                // reduce priority for armory and stables
                if (buildingType.getProducedGoodsType() != null
                    && buildingType.getProducedGoodsType().isMilitaryGoods()) {
                    priority /= 2;
                }
                buildables.add(new Buildable(buildingType, priority));
            }
        }

        // Check if we should improve the warehouse:
        Building building = colony.getWarehouse();
        if (building.canBuildNext()) {
            int priority = colony.getGoodsContainer()
                .hasReachedCapacity(colony.getWarehouseCapacity()) ?
                2 * WAREHOUSE_PRIORITY : WAREHOUSE_PRIORITY;
            buildables.add(new Buildable(building.getType().getUpgradesTo(), priority));
        } else if (bestWagon != null && wagonTrains < 4 * colony.getOwner().getColonies().size()) {
            buildables.add(new Buildable(bestWagon, WAGON_TRAIN_PRIORITY));
        }

        // improve defences
        for (BuildingType buildingType : defence) {
            if (colony.canBuild(buildingType)) {
                int priority = (colony.getBuilding(buildingType) == null
                                || profile.getType() == ProfileType.LARGE
                                || profile.getType() == ProfileType.CAPITAL) ?
                    2 * UPGRADE_PRIORITY : UPGRADE_PRIORITY;
                buildables.add(new Buildable(buildingType, priority));
            }
        }

        // improve military production
        for (BuildingType buildingType : military) {
            if (colony.canBuild(buildingType)) {
                if (colony.getBuilding(buildingType) == null
                    && (buildingType.getConsumedGoodsType() == null
                        || buildingType.getConsumedGoodsType().isFarmed())) {
                    buildables.add(new Buildable(buildingType, UPGRADE_PRIORITY));
                } else {
                    buildables.add(new Buildable(buildingType, UPGRADE_PRIORITY / 2));
                }
            }
        }

        // add artillery if necessary
        // TODO: at some point, we will have to add ships
        if (aiMain.getAIColony(colony).isBadlyDefended()) {
            for (UnitType unitType : buildableDefenders) {
                if (colony.canBuild(unitType)) {
                    int priority = (profile.getType() == ProfileType.LARGE
                                    || profile.getType() == ProfileType.CAPITAL) ?
                        2 * ARTILLERY_PRIORITY : ARTILLERY_PRIORITY;
                    buildables.add(new Buildable(unitType, priority));
                    break;
                }
            }
        }

        // improve education
        if (profile.getType() != ProfileType.SMALL) {
            for (BuildingType buildingType : schools) {
                if (colony.canBuild(buildingType)) {
                    int priority = SCHOOL_PRIORITY;
                    if (colony.getBuilding(buildingType) != null) {
                        if (profile.getType() == ProfileType.MEDIUM) {
                            priority /= 2;
                        }
                        if (buildingType.getUpgradesTo() == null) {
                            if (profile.getType() != ProfileType.CAPITAL) {
                                continue;
                            }
                        }
                    }
                    buildables.add(new Buildable(buildingType, priority));
                }
            }
        }

        // improve town hall (not required for standard rule set,
        // since town hall can not be upgraded)
        for (BuildingType buildingType : townHalls) {
            if (colony.canBuild(buildingType)) {
                int priority = (colony.getBuilding(buildingType) == null) ?
                    2 * TOWN_HALL_PRIORITY : TOWN_HALL_PRIORITY;
                buildables.add(new Buildable(buildingType, priority));
            }
        }

        // improve churches
        for (BuildingType buildingType : churches) {
            if (colony.canBuild(buildingType)) {
                int priority = (colony.getBuilding(buildingType) == null) ?
                    2 * CHURCH_PRIORITY : CHURCH_PRIORITY;
                buildables.add(new Buildable(buildingType, priority));
            }
        }


        Collections.sort(buildables);
        List<BuildableType> result = new ArrayList<BuildableType>();
        Set<BuildableType> found = new HashSet<BuildableType>();
        for (Buildable buildable : buildables) {
            if (!found.contains(buildable.type)) {
                result.add(buildable.type);
                found.add(buildable.type);
            }
        }
        return result.iterator();
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

        workLocationPlans.clear();
        if (profile.getType() == ProfileType.OUTPOST) {
            GoodsType goodsType;
            ColonyTile productionTile;
            if ((goodsType = profile.getPreferredProduction().get(0)) != null
                && (productionTile = getBestTileToProduce(goodsType)) != null) {
                workLocationPlans.add(new WorkLocationPlan(getAIMain(),
                                                           productionTile,
                                                           goodsType));
            }
            return;
        }

        GoodsType bells = colony.getSpecification().getGoodsType("model.goods.bells");
        GoodsType food = colony.getSpecification().getGoodsType("model.goods.grain");
        GoodsType lumber = colony.getSpecification().getGoodsType("model.goods.lumber");
        GoodsType muskets = colony.getSpecification().getGoodsType("model.goods.muskets");
        GoodsType ore = colony.getSpecification().getGoodsType("model.goods.ore");
        GoodsType silver = colony.getSpecification().getGoodsType("model.goods.silver");

        Building townHall = colony.getBuildingForProducing(bells);

        // Choose the best production for each tile:
        for (ColonyTile ct : colony.getColonyTiles()) {
            if (ct.isColonyCenterTile()
                || (ct.getWorkTile().getOwningSettlement() != null
                    && ct.getWorkTile().getOwningSettlement() != colony)) {
                continue;
            }

            GoodsType goodsType = getBestGoodsToProduce(ct.getWorkTile());
            if (goodsType != null) {
                WorkLocationPlan wlp = new WorkLocationPlan(getAIMain(),
                                                            ct, goodsType);
                workLocationPlans.add(wlp);
            }
        }

        // We need to find what, if any, is still required for what we
        // are building
        GoodsType buildingReq = null;
        GoodsType buildingRawMat = null;
        Building buildingReqProducer = null;
        final GoodsType hammersType = colony.getSpecification().getGoodsType("model.goods.hammers");
        final GoodsType lumberType = colony.getSpecification().getGoodsType("model.goods.lumber");
        final GoodsType oreType = colony.getSpecification().getGoodsType("model.goods.ore");

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
                    && ((ColonyTile) wlp.getWorkLocation()).getWorkTile().potential(buildingRawMat, null)
                    > highestPotential) {
                    highestPotential = ((ColonyTile) wlp.getWorkLocation()).getWorkTile()
                        .potential(buildingRawMat, null);
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
        List<GoodsType> goodsTypeList = colony.getSpecification().getGoodsTypeList();
        for (GoodsType goodsType : goodsTypeList) {
            // only consider goods that can be transformed
            // do not consider hammers as a valid transformation
            if ((goodsType.getProducedMaterial() == null
                 && !goodsType.isFoodType())
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
                    || wlp.getGoodsType() == lumber || wlp.getGoodsType() == ore
                    || wlp.getGoodsType() == silver) {
                continue;
            }
            // TODO: find out about unit working here, if any (?)
            if (((ColonyTile) wlp.getWorkLocation()).getWorkTile().potential(food, null) <= 2) {
                if (wlp.getGoodsType() == null) {
                    // on arctic tiles nothing can be produced
                    wlpIterator.remove();
                } else if (wlp.getProductionOf(wlp.getGoodsType()) <= 2) {
                    // just a poor location
                    wlpIterator.remove();
                }
                continue;
            }

            wlp.setGoodsType(food);
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
        WorkLocationPlan townHallWlp = new WorkLocationPlan(getAIMain(), townHall, bells);
        workLocationPlans.add(townHallWlp);

        // Place a colonist to manufacture the primary goods:
        if (primaryRawMaterial != null) {
            GoodsType producedGoods = primaryRawMaterial.getProducedMaterial();
            Building b = colony.getBuildingForProducing(producedGoods);
            if (producedGoods != null && b != null) {
                WorkLocationPlan wlp = new WorkLocationPlan(getAIMain(), b, producedGoods);
                workLocationPlans.add(wlp);
            }
        }

        // Remove the secondary goods if we need food:
        if (secondaryRawMaterial != null
                && needsFood()
                && secondaryRawMaterial.isNewWorldGoodsType()) {
            Iterator<WorkLocationPlan> wlpIterator2 = workLocationPlans.iterator();
            while (wlpIterator2.hasNext()) {
                WorkLocationPlan wlp = wlpIterator2.next();
                if (wlp.getWorkLocation() instanceof ColonyTile && wlp.getGoodsType() == secondaryRawMaterial) {
                    Tile t = ((ColonyTile) wlp.getWorkLocation()).getWorkTile();
                    // TODO: find out about unit working here, if any (?)
                    if (t.getMaximumPotential(food, null) > 2) {
                        wlp.setGoodsType(food);
                    } else {
                        wlpIterator2.remove();
                    }
                }
            }
        }

        // Remove the workers on the primary goods one-by-one if we need food:
        if (needsFood()) {
            Iterator<WorkLocationPlan> wlpIterator2 = workLocationPlans.iterator();
            while (wlpIterator2.hasNext() && needsFood()) {
                WorkLocationPlan wlp = wlpIterator2.next();
                if (wlp.getWorkLocation() instanceof ColonyTile && wlp.getGoodsType() == primaryRawMaterial) {
                    Tile t = ((ColonyTile) wlp.getWorkLocation()).getWorkTile();
                    // TODO: find out about unit working here, if any (?)
                    if (t.getMaximumPotential(food, null) > 2) {
                        wlp.setGoodsType(food);
                    } else {
                        wlpIterator2.remove();
                    }
                }
            }
        }

        // Remove the manufacturer if we still lack food:
        if (needsFood()) {
            Iterator<WorkLocationPlan> wlpIterator2 = workLocationPlans.iterator();
            while (wlpIterator2.hasNext() && needsFood()) {
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
        if (needsFood()) {
            Iterator<WorkLocationPlan> wlpIterator2 = workLocationPlans.iterator();
            while (wlpIterator2.hasNext() && needsFood()) {
                WorkLocationPlan wlp = wlpIterator2.next();
                if (wlp.getWorkLocation() instanceof ColonyTile && wlp.getGoodsType() == buildMatToGo) {
                    wlpIterator2.remove();
                }
            }
            // still lacking food, removing the rest
            if (needsFood()) {
                buildMatToGo = (buildMatToGo == buildingRawMat)? buildingReq : buildingRawMat;

                wlpIterator2 = workLocationPlans.iterator();
                while (wlpIterator2.hasNext() && needsFood()) {
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
        if (getFoodProduction() < getNextFoodConsumption()) {
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
            if (getFoodProduction() >= getNextFoodConsumption() &&
                secondaryRawMaterial != null &&
                12 * secondaryWorkers + 6 <= getProductionOf(secondaryRawMaterial) &&
                secondaryWorkers <= MAX_LEVEL) {
                GoodsType producedGoods = secondaryRawMaterial.getProducedMaterial();
                Building b = colony.getBuildingForProducing(producedGoods);
                if (producedGoods != null && b != null) {
                    WorkLocationPlan wlp = new WorkLocationPlan(getAIMain(), b, producedGoods);
                    workLocationPlans.add(wlp);
                    colonistAdded = true;
                    secondaryWorkers++;
                    if (secondaryRawMaterial == ore) {
                        blacksmithAdded = true;
                    }
                }
            }

            // Add a manufacturer for the primary type of goods:
            if (getFoodProduction() >= getNextFoodConsumption()
                && primaryRawMaterial != null
                && 12 * primaryWorkers + 6 <= getProductionOf(primaryRawMaterial)
                && primaryWorkers <= MAX_LEVEL) {
                GoodsType producedGoods = primaryRawMaterial.getProducedMaterial();
                Building b = colony.getBuildingForProducing(producedGoods);
                if (producedGoods != null && b != null) {
                    WorkLocationPlan wlp = new WorkLocationPlan(getAIMain(), b, producedGoods);
                    workLocationPlans.add(wlp);
                    colonistAdded = true;
                    primaryWorkers++;
                    if (primaryRawMaterial == ore) {
                        blacksmithAdded = true;
                    }
                }
            }

            // Add a gunsmith:
            if (blacksmithAdded
                && getFoodProduction() >= getNextFoodConsumption()
                && gunsmiths < MAX_LEVEL) {
                Building b = colony.getBuildingForProducing(muskets);
                if (b != null) {
                    WorkLocationPlan wlp = new WorkLocationPlan(getAIMain(), b, muskets);
                    workLocationPlans.add(wlp);
                    colonistAdded = true;
                    gunsmiths++;
                }
            }

            // Add builders
            if (getFoodProduction() >= getNextFoodConsumption()
                && buildingReq != null
                && buildingReqProducer != null
                && buildingReqProducer.getProduction() * builders <= getProductionOf(buildingRawMat)
                && buildingReqProducer.getUnitCapacity() < builders) {
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

    private int getFoodConsumption() {
        return workLocationPlans.size() * colony.getOwner().getMaximumFoodConsumption();
    }

    private int getNextFoodConsumption() {
        return (workLocationPlans.size() + 1) * colony.getOwner().getMaximumFoodConsumption();
    }

    private boolean needsFood() {
        return (getFoodProduction() < getFoodConsumption());
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
        if (colony.getTile().getType().isPrimaryGoodsType(goodsType)
            || colony.getTile().getType().isSecondaryGoodsType(goodsType)) {
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
        for (GoodsType foodType : colony.getSpecification().getFoodGoodsTypeList()) {
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

    private ColonyTile getBestTileToProduce(GoodsType goodsType) {
        int bestProduction = -1;
        ColonyTile bestTile = null;
        for (ColonyTile ct : colony.getColonyTiles()) {
            Tile tile = ct.getWorkTile();
            if ((tile.getOwningSettlement() == null
                 || tile.getOwningSettlement() == colony)
                && !ct.isColonyCenterTile()) {
                int production = tile.potential(goodsType, null);
                if (bestTile == null || bestProduction < production) {
                    bestTile = ct;
                    bestProduction = production;
                }
            }
        }
        if (bestProduction > 0) {
            return bestTile;
        } else {
            return null;
        }
    }

    public class Production {
        ColonyTile colonyTile;
        GoodsType goodsType;

        public Production(ColonyTile ct, GoodsType gt) {
            colonyTile = ct;
            goodsType = gt;
        }
    }

    public Production getBestProduction(UnitType unitType) {
        Market market = colony.getOwner().getMarket();
        Production bestProduction = null;
        int value = -1;
        for (ColonyTile ct : colony.getColonyTiles()) {
            Tile tile = ct.getWorkTile();
            if ((tile.getOwningSettlement() == null
                 || tile.getOwningSettlement() == colony)
                && !ct.isColonyCenterTile()) {
                for (GoodsType goodsType : colony.getSpecification().getFarmedGoodsTypeList()) {
                    int production = market.getSalePrice(goodsType, tile.potential(goodsType, unitType));
                    if (bestProduction == null || value < production) {
                        value = production;
                        bestProduction = new Production(ct, goodsType);
                    }
                }
            }
        }
        return bestProduction;
    }


    public void adjustProductionAndManufacture(){
        List<GoodsType> rawMatList = new ArrayList<GoodsType>();

        final GoodsType hammersType = colony.getSpecification().getGoodsType("model.goods.hammers");
        final GoodsType lumberType = colony.getSpecification().getGoodsType("model.goods.lumber");
        final GoodsType oreType = colony.getSpecification().getGoodsType("model.goods.ore");

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

            if (factory.isFull()) return;
            Unit u = iter.next();
            // this particular unit cannot be added to this building
            if(!factory.canAdd(u.getType())){
                continue;
            }

            // get  the production values if the unit is shifted
            int rawProd = colony.getNetProductionOf(rawMat)
                - ((ColonyTile)u.getWorkTile()).getProductionOf(u, rawMat);
            int mfnProd = colony.getNetProductionOf(producedGoods)
                + factory.getAdditionalProductionNextTurn(u);
            if(stockRawMat < 50 && rawProd < mfnProd){
                return;
            }

            u.setLocation(factory);
            u.setWorkType(producedGoods);
            producers.remove(u);
        }
    }

    public GoodsType getBuildingReqGoods(){
        BuildableType currBuild = colony.getCurrentlyBuilding();
        if(currBuild == null){
            return null;
        }
        final GoodsType hammersType = colony.getSpecification().getGoodsType("model.goods.hammers");
        final GoodsType toolsType = colony.getSpecification().getGoodsType("model.goods.tools");

        if(colony.getGoodsCount(hammersType) < currBuild.getAmountRequiredOf(hammersType)){
            return hammersType;
        } else{
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

    private void selectProfile() {
        List<GoodsType> preferredProduction = new ArrayList<GoodsType>();
        preferredProduction.addAll(getGame().getSpecification().getFoodGoodsTypeList());
        preferredProduction.addAll(getGame().getSpecification().getLibertyGoodsTypeList());
        ProfileType type = ProfileType.SMALL;
        int size = colony.getUnitCount();
        if (size > 8) {
            type = ProfileType.LARGE;
        } else if (size > 12) {
            type = ProfileType.CAPITAL;
        }
        profile = new ColonyProfile(type, preferredProduction);
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

        element.setAttribute(FreeColObject.ID_ATTRIBUTE, colony.getId());

        return element;
    }

    /**
     * Updates this object from an XML-representation of a
     * <code>ColonyPlan</code>.
     *
     * @param element The XML-representation.
     */
    public void readFromXMLElement(Element element) {
        colony = (Colony) getAIMain()
            .getFreeColGameObject(element.getAttribute(FreeColObject.ID_ATTRIBUTE));
        // TODO: serialize profile
        selectProfile();
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
        sb.append("\n\nPROFILE:\n");
        sb.append(profile.getType().toString());
        sb.append("\n");
        for (GoodsType goodsType : profile.getPreferredProduction()) {
            sb.append(goodsType.getNameKey());
            sb.append("\n");
        }
        sb.append("\n\nWORK LOCATIONS:\n");
        for (WorkLocationPlan p : getSortedWorkLocationPlans()) {
            sb.append(p.getGoodsType().getNameKey() + " (" + p.getWorkLocation() + ")\n");
        }
        sb.append("\n\nBUILD QUEUE:\n");
        final Iterator<BuildableType> it = getBuildable();
        while (it.hasNext()) {
            final BuildableType b = it.next();
            sb.append(b.getNameKey());
            sb.append('\n');
        }
        return sb.toString();
    }
}
