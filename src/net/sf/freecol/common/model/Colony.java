/**
 *  Copyright (C) 2002-2014   The FreeCol Team
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

package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.ProductionInfo;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.common.util.RandomChoice;


/**
 * Represents a colony. A colony contains {@link Building}s and
 * {@link ColonyTile}s. The latter represents the tiles around the
 * <code>Colony</code> where working is possible.
 */
public class Colony extends Settlement implements Nameable {

    private static final Logger logger = Logger.getLogger(Colony.class.getName());

    public static final String REARRANGE_WORKERS = "rearrangeWorkers";
    public static final int LIBERTY_PER_REBEL = 200;

    public static enum ColonyChangeEvent {
        POPULATION_CHANGE,
        PRODUCTION_CHANGE,
        BONUS_CHANGE,
        WAREHOUSE_CHANGE,
        BUILD_QUEUE_CHANGE,
        UNIT_TYPE_CHANGE
    }

    /** Reasons for not building a buildable. */
    public static enum NoBuildReason {
        NONE,
        NOT_BUILDING,
        NOT_BUILDABLE,
        POPULATION_TOO_SMALL,
        MISSING_BUILD_ABILITY,
        MISSING_ABILITY,
        WRONG_UPGRADE,
        LIMIT_EXCEEDED
    }

    /** 
     * Simple container to define where and what a unit is working
     * on.
     */
    private static class Occupation {

        public WorkLocation workLocation;
        public ProductionType productionType;
        public GoodsType workType;

        /**
         * Create an Occupation.
         *
         * @param workLocation The <code>WorkLocation</code> to work at.
         * @param productionType The <code>ProductionType</code> to
         *     use at the work location.
         * @param workType The <code>GoodsType</code> to produce at the
         *     work location with the production type.
         */
        public Occupation(WorkLocation workLocation,
                          ProductionType productionType,
                          GoodsType workType) {
            this.workLocation = workLocation;
            this.productionType = productionType;
            this.workType = workType;
        }

        /**
         * Install a unit into this occupation.
         *
         * @param unit The <code>Unit</code> to establish.
         * @return True if the unit is installed.
         */
        public boolean install(Unit unit) {
            if (!unit.setLocation(workLocation)) return false;
            if (productionType != workLocation.getProductionType()) {
                workLocation.setProductionType(productionType);
            }
            if (workType != unit.getWorkType()) {
                unit.changeWorkType(workType);
            }
            return true;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(32);
            sb.append("[Occupation ").append(workLocation)
                //.append(" ").append(productionType)
                .append(" ").append(workType.getSuffix())
                .append("]");
            return sb.toString();
        }
    }

    /** A map of Buildings, indexed by the id of their basic type. */
    protected final java.util.Map<String, Building> buildingMap
        = new HashMap<String, Building>();

    /** A list of the ColonyTiles. */
    protected final List<ColonyTile> colonyTiles
        = new ArrayList<ColonyTile>();

    /** A map of ExportData, indexed by the ids of GoodsTypes. */
    protected final java.util.Map<String, ExportData> exportData
        = new HashMap<String, ExportData>();

    /**
     * The number of liberty points.  Liberty points are an
     * abstract game concept.  They are generated by but are not
     * identical to bells, and subject to further modification.
     */
    protected int liberty;

    /** The SoL membership this turn. */
    protected int sonsOfLiberty;

    /** The SoL membership last turn. */
    protected int oldSonsOfLiberty;

    /** The number of tories this turn. */
    protected int tories;

    /** The number of tories last turn. */
    protected int oldTories;

    /** The current production bonus. */
    protected int productionBonus;

    /**
     * The number of immigration points.  Immigration points are an
     * abstract game concept.  They are generated by but are not
     * identical to crosses.
     */
    protected int immigration;

    /** The turn in which this colony was established. */
    protected Turn established = new Turn(0);

    /** A list of items to be built. */
    protected BuildQueue<BuildableType> buildQueue
        = new BuildQueue<BuildableType>(this,
            BuildQueue.CompletionAction.REMOVE_EXCEPT_LAST,
            Consumer.COLONY_PRIORITY);

    /** The colonists that may be born. */
    protected BuildQueue<UnitType> populationQueue
        = new BuildQueue<UnitType>(this,
            BuildQueue.CompletionAction.SHUFFLE,
            Consumer.POPULATION_PRIORITY);

    // Will only be used on enemy colonies:
    protected int displayUnitCount = -1;

    // Do not serialize below.

    /** Contains information about production and consumption. */
    private ProductionCache productionCache = new ProductionCache(this);

    /** The occupation tracing status.  Do not serialize. */
    private boolean traceOccupation = false;



    /**
     * Constructor for ServerColony.
     *
     * @param game The enclosing <code>Game</code>.
     * @param owner The <code>Player</code> owning this <code>Colony</code>.
     * @param name The name of the new <code>Colony</code>.
     * @param tile The containing <code>Tile</code>.
     */
    protected Colony(Game game, Player owner, String name, Tile tile) {
        super(game, owner, name, tile);
    }

    /**
     * Create a new <code>Colony</code> with the given
     * identifier. The object should later be initialized by calling
     * either {@link #readFromXML(FreeColXMLReader)}.
     *
     * @param game The enclosing <code>Game</code>.
     * @param id The object identifier.
     */
    public Colony(Game game, String id) {
        super(game, id);
    }


    // Primitive accessors.

    /**
     * Gets a <code>List</code> of every {@link Building} in this
     * <code>Colony</code>.
     *
     * @return A list of <code>Building</code>s.
     * @see Building
     */
    public List<Building> getBuildings() {
        return new ArrayList<Building>(buildingMap.values());
    }

    /**
     * Get building of the specified general type (note: *not*
     * necessarily the exact building type supplied, but the building
     * present in the colony that is a descendant of the ultimate
     * ancestor of the specified type).
     *
     * @param type The type of the building to get.
     * @return The <code>Building</code> found.
     */
    public Building getBuilding(BuildingType type) {
        return buildingMap.get(type.getFirstLevel().getId());
    }

    /**
     * Gets a <code>List</code> of every {@link ColonyTile} in this
     * <code>Colony</code>.
     *
     * @return A list of <code>ColonyTile</code>s.
     * @see ColonyTile
     */
    public List<ColonyTile> getColonyTiles() {
        return colonyTiles;
    }

    /**
     * Get the <code>ColonyTile</code> matching the given
     * <code>Tile</code>.
     *
     * @param t The <code>Tile</code> to check.
     * @return The corresponding <code>ColonyTile</code>, or null if not found.
     */
    public ColonyTile getColonyTile(Tile t) {
        for (ColonyTile c : colonyTiles) {
            if (c.getWorkTile() == t) return c;
        }
        return null;
    }

    /**
     * Get the export date for a goods type.
     *
     * @param goodsType The <code>GoodsType</code> to check.
     * @return The required <code>ExportData</code>.
     */
    public ExportData getExportData(final GoodsType goodsType) {
        ExportData result = exportData.get(goodsType.getId());
        if (result == null) {
            result = new ExportData(goodsType);
            setExportData(result);
        }
        return result;
    }

    /**
     * Set some export data.
     *
     * @param newExportData A new <code>ExportData</code> value.
     */
    public final void setExportData(final ExportData newExportData) {
        exportData.put(newExportData.getId(), newExportData);
    }

    /**
     * Gets the liberty points.
     *
     * @return The current liberty.
     */
    public int getLiberty() {
        return liberty;
    }

    /**
     * Gets the effective liberty following modifiers.
     *
     * @return The current effective liberty.
     */
    public int getEffectiveLiberty() {
        return (int)applyModifiers((float)getLiberty(), getGame().getTurn(),
                                   getOwner().getModifiers(Modifier.LIBERTY));
    }

    /**
     * Gets the production bonus of the colony.
     *
     * @return The current production bonus of the colony.
     */
    public int getProductionBonus() {
        return productionBonus;
    }

    /**
     * Gets the immigration points.
     *
     * @return The current immigration.
     */
    public int getImmigration() {
        return immigration;
    }

    /**
     * Modify the immigration points by amount given.
     *
     * @param amount An amount of immigration.
     */
    public void modifyImmigration(int amount) {
        immigration += amount;
    }

    /**
     * Get the turn this colony was established.
     *
     * @return The establishment <code>Turn</code>.
     */
    public Turn getEstablished() {
        return established;
    }

    /**
     * Set the turn of establishment.
     *
     * @param newEstablished The new <code>Turn</code> of establishment.
     */
    public void setEstablished(final Turn newEstablished) {
        this.established = newEstablished;
    }

    /**
     * Get the <code>BuildQueue</code> contents.
     *
     * @return A list of <code>Buildable</code>s.
     */
    public List<BuildableType> getBuildQueue() {
        return buildQueue.getValues();
    }

    /**
     * Set the <code>BuildQueue</code> value.
     *
     * @param newBuildQueue The new BuildQueue value.
     */
    public void setBuildQueue(final List<BuildableType> newBuildQueue) {
        buildQueue.setValues(newBuildQueue);
    }


    // Private Occupation routines

    /**
     * Gets the occupation tracing status.
     *
     * @return The occupation tracing status.
     */
    public boolean getOccupationTrace() {
        return this.traceOccupation;
    }

    /**
     * Sets the occupation tracing status.
     *
     * @param trace The new occupation tracing status.
     * @return The original occupation tracing status.
     */
    public boolean setOccupationTrace(boolean trace) {
        boolean ret = this.traceOccupation;
        this.traceOccupation = trace;
        return ret;
    }

    /**
     * Get the lowest currently available amount of required goods
     * from a list.
     *
     * @param goodsList A list of <code>AbstractGoods</code> to require.
     * @return The minimum goods count.
     */
    private int getMinimumGoodsCount(List<AbstractGoods> goodsList) {
        if (goodsList == null || goodsList.isEmpty()) return INFINITY;
        int result = INFINITY;
        for (AbstractGoods ag : goodsList) {
            result = Math.min(result, 
                              Math.max(getGoodsCount(ag.getType()),
                                       getNetProductionOf(ag.getType())));
        }
        return result;
    }

    private void logWorkTypes(Collection<GoodsType> workTypes, LogBuilder lb) {
        lb.add("[");
        for (GoodsType gt : workTypes) {
            lb.add(gt.getSuffix(), " ");
        }
        lb.shrink(" ");
        lb.add("]");
    }
        
    private void accumulateChoices(Collection<GoodsType> workTypes,
                                   Collection<GoodsType> tried,
                                   List<Collection<GoodsType>> result) {
        workTypes.removeAll(tried);
        if (!workTypes.isEmpty()) {
            result.add(workTypes);
            tried.addAll(workTypes);
        }
    }

    private void accumulateChoice(GoodsType workType,
                                  Collection<GoodsType> tried,
                                  List<Collection<GoodsType>> result) {
        if (workType == null) return;
        accumulateChoices(workType.getEquivalentTypes(), tried, result);
    }

    /**
     * Get a list of collections of goods types, in order of priority
     * to try to produce in this colony by a given unit.
     *
     * @param unit The <code>Unit</code> to check.
     * @param userMode If a user requested this, favour the current
     *     work type, if not favour goods that the unit requires.
     * @return The list of collections of <code>GoodsType</code>s.
     */
    public List<Collection<GoodsType>> getWorkTypeChoices(Unit unit,
                                                          boolean userMode) {
        final Specification spec = getSpecification();
        List<Collection<GoodsType>> result
            = new ArrayList<Collection<GoodsType>>();
        Set<GoodsType> tried = new HashSet<GoodsType>();

        // Find the food and non-food goods types required by the unit.
        Set<GoodsType> food = new HashSet<GoodsType>();
        Set<GoodsType> nonFood = new HashSet<GoodsType>();
        for (AbstractGoods ag : unit.getType().getConsumedGoods()) {
            if (productionCache.getNetProductionOf(ag.getType())
                < ag.getAmount()) {
                if (ag.getType().isFoodType()) {
                    food.addAll(ag.getType().getEquivalentTypes());
                } else {
                    nonFood.addAll(ag.getType().getEquivalentTypes());
                }
            }
        }

        if (userMode) { // Favour current and expert types in user mode
            accumulateChoice(unit.getWorkType(), tried, result);
            accumulateChoice(unit.getType().getExpertProduction(), tried, result);
            accumulateChoice(unit.getExperienceType(), tried, result);
            accumulateChoices(food, tried, result);
            accumulateChoices(nonFood, tried, result);
        } else { // Otherwise favour the required goods types 
            accumulateChoices(food, tried, result);
            accumulateChoices(nonFood, tried, result);
            accumulateChoice(unit.getWorkType(), tried, result);
            accumulateChoice(unit.getType().getExpertProduction(), tried, result);
            accumulateChoice(unit.getExperienceType(), tried, result);
        }
        accumulateChoices(spec.getFoodGoodsTypeList(), tried, result);
        accumulateChoices(spec.getNewWorldLuxuryGoodsTypeList(), tried, result);
        accumulateChoices(spec.getGoodsTypeList(), tried, result);
        return result;
    }

    /**
     * Check a particular work location for the best available production
     * for the given unit.
     *
     * @param unit The <code>Unit</code> to produce the goods.
     * @param wl The <code>WorkLocation</code> to check.
     * @param best The best <code>Occupation</code> found so far.
     * @param bestAmount The amount of goods produced.
     * @param workTypes A collection of <code>GoodsType</code> to
     *     consider producing.
     * @param lb A <code>LogBuilder</code> to log to.
     * @return The updated best amount of production found.
     */
    private int getOccupationAt(Unit unit, WorkLocation wl,
                                Occupation best, int bestAmount,
                                Collection<GoodsType> workTypes,
                                LogBuilder lb) {
        final UnitType type = unit.getType();

        // Can the unit work at this wl?
        boolean present = unit.getLocation() == wl;
        lb.add("\n    ", wl,
            ((!present && !wl.canAdd(unit)) ? " no-add" : ""));
        if (!present && !wl.canAdd(unit)) return bestAmount;

        // Can the unit determine the production type at this WL?
        // This will be true if the unit is going to be alone or
        // if the production type is as yet unset.
        boolean alone = wl.getProductionType() == null
            || wl.isEmpty()
            || (present && wl.getUnitCount() == 1);
        lb.add(" alone=", alone);

        // Try the available production types for the best production.
        List<ProductionType> productionTypes = new ArrayList<ProductionType>();
        if (alone) {
            productionTypes.addAll(wl.getAvailableProductionTypes(false));
        } else {
            productionTypes.add(wl.getProductionType());
        }
        for (ProductionType pt : productionTypes) {
            lb.add("\n      try=", pt);
            for (GoodsType gt : workTypes) {
                if (pt.getOutput(gt) == null) continue;
                int amount = getMinimumGoodsCount(pt.getInputs());
                amount = Math.min(amount, wl.getPotentialProduction(gt, type));
                lb.add(" ", gt.getSuffix(), "=", amount,
                    "/", getMinimumGoodsCount(pt.getInputs()),
                    "/", wl.getPotentialProduction(gt, type),
                    ((bestAmount < amount) ? "!" : ""));
                if (bestAmount < amount) {
                    bestAmount = amount;
                    best.workLocation = wl;
                    best.productionType = pt;
                    best.workType = gt;
                }
            }
        }
        return bestAmount;   
    }
        
    /**
     * Gets the best occupation for a given unit to produce one of
     * a given set of goods types.
     *
     * @param unit The <code>Unit</code> to find an
     *     <code>Occupation</code> for.
     * @param workTypes A collection of <code>GoodsType</code> to
     *     consider producing.
     * @param lb A <code>LogBuilder</code> to log to.
     * @return An <code>Occupation</code> for the given unit, or null
     *     if none found.
     */
    private Occupation getOccupationFor(Unit unit,
                                        Collection<GoodsType> workTypes,
                                        LogBuilder lb) {
        if (workTypes.isEmpty()) return null;

        Occupation best = new Occupation(null, null, null);
        int bestAmount = 0;
        for (WorkLocation wl : getCurrentWorkLocations()) {
            bestAmount = getOccupationAt(unit, wl, best, bestAmount,
                                         workTypes, lb);
        }

        if (best.workLocation != null) {
            lb.add("\n  => ", best, " = ", bestAmount);
        }
        return (best.workLocation == null) ? null : best;
    }

    /**
     * Gets the best occupation for a given unit.
     *
     * @param unit The <code>Unit</code> to find an
     *     <code>Occupation</code> for.
     * @param userMode If a user requested this, favour the current
     *     work type, if not favour goods that the unit requires.
     * @param lb A <code>LogBuilder</code> to log to.
     * @return An <code>Occupation</code> for the given unit, or
     *     null if none found.
     */
    private Occupation getOccupationFor(Unit unit, boolean userMode,
                                        LogBuilder lb) {
        for (Collection<GoodsType> types
                 : getWorkTypeChoices(unit, userMode)) {
            lb.add("\n  ");
            logWorkTypes(types, lb);
            Occupation occupation = getOccupationFor(unit, types, lb);
            if (occupation != null) return occupation;
        }
        lb.add("\n  => FAILED");
        return null;
    }

    /**
     * Gets the best occupation for a given unit to produce one of
     * a given set of goods types.
     *
     * @param unit The <code>Unit</code> to find an
     *     <code>Occupation</code> for.
     * @param workTypes A collection of <code>GoodsType</code> to
     *     consider producing.
     * @return An <code>Occupation</code> for the given unit, or null
     *     if none found.
     */
    private Occupation getOccupationFor(Unit unit,
                                        Collection<GoodsType> workTypes) {
        LogBuilder lb = new LogBuilder((getOccupationTrace()) ? 64 : 0);
        lb.add(getName(), ".getOccupationFor(", unit, ", ");
        logWorkTypes(workTypes, lb);
        lb.add(")");

        Occupation occupation = getOccupationFor(unit, workTypes, lb);
        lb.log(logger, Level.WARNING);
        return occupation;
    }

    /**
     * Gets the best occupation for a given unit.
     *
     * @param unit The <code>Unit</code> to find an
     *     <code>Occupation</code> for.
     * @param userMode If a user requested this, favour the current
     *     work type, if not favour goods that the unit requires.
     * @return An <code>Occupation</code> for the given unit, or
     *     null if none found.
     */
    private Occupation getOccupationFor(Unit unit, boolean userMode) {
        LogBuilder lb = new LogBuilder((getOccupationTrace()) ? 64 : 0);
        lb.add(getName(), ".getOccupationFor(", unit, ")");

        Occupation occupation = getOccupationFor(unit, userMode, lb);
        lb.log(logger, Level.WARNING);
        return occupation;
    }

    /**
     * Gets the best occupation for a given unit at a given work location.
     *
     * @param unit The <code>Unit</code> to find an
     *     <code>Occupation</code> for.
     * @param wl The <code>WorkLocation</code> to work at.
     * @param userMode If a user requested this, favour the current
     *     work type, if not favour goods that the unit requires.
     * @return An <code>Occupation</code> for the given unit, or
     *     null if none found.
     */
    private Occupation getOccupationAt(Unit unit, WorkLocation wl,
                                       boolean userMode) {
        LogBuilder lb = new LogBuilder((getOccupationTrace()) ? 64 : 0);
        lb.add(getName(), ".getOccupationAt(", unit, ", ", wl, ")");

        Occupation best = new Occupation(null, null, null);
        int bestAmount = 0;
        for (Collection<GoodsType> types
                 : getWorkTypeChoices(unit, userMode)) {
            lb.add("\n  ");
            logWorkTypes(types, lb);
            bestAmount = getOccupationAt(unit, wl, best, bestAmount,
                                         types, lb);
            if (best.workType != null) {
                lb.add("\n  => ", best);
                break;
            }
        }
        if (best.workType == null) lb.add("\n  FAILED");
        lb.log(logger, Level.WARNING);
        return (best.workType == null) ? null : best;
    }

    /**
     * Install a unit at the best occupation for it at a given work
     * location.  Public for {@link WorkLocation#add}.
     *
     * @param unit The <code>Unit</code> to install.
     * @param wl The <code>WorkLocation</code> to install the unit.
     * @param userMode If a user requested this, favour the current
     *     work type, if not favour goods that the unit requires.
     * @return True if the installation succeeds.
     */
    public boolean setOccupationAt(Unit unit, WorkLocation wl,
                                   boolean userMode) {
        Occupation occupation = getOccupationAt(unit, wl, userMode);
        return occupation != null && occupation.install(unit);
    }


    // WorkLocations, Buildings, ColonyTiles

    /**
     * Gets a list of every work location in this colony.
     *
     * @return The list of work locations.
     */
    public List<WorkLocation> getAllWorkLocations() {
        List<WorkLocation> result = new ArrayList<WorkLocation>(colonyTiles);
        result.addAll(buildingMap.values());
        return result;
    }

    /**
     * Gets a list of all freely available work locations
     * in this colony.
     *
     * @return The list of available <code>WorkLocation</code>s.
     */
    public List<WorkLocation> getAvailableWorkLocations() {
        List<WorkLocation> result
            = new ArrayList<WorkLocation>(buildingMap.values());
        for (ColonyTile ct : colonyTiles) {
            Tile tile = ct.getWorkTile();
            if (tile.getOwningSettlement() == this
                || getOwner().canClaimForSettlement(tile)) {
                result.add(ct);
            }
        }
        return result;
    }

    /**
     * Gets a list of all current work locations in this colony.
     *
     * @return The list of current <code>WorkLocation</code>s.
     */
    public List<WorkLocation> getCurrentWorkLocations() {
        List<WorkLocation> result
            = new ArrayList<WorkLocation>(buildingMap.values());
        for (ColonyTile ct : colonyTiles) {
            Tile tile = ct.getWorkTile();
            if (tile.getOwningSettlement() == this) result.add(ct);
        }
        return result;
    }

    /**
     * Add a Building to this Colony.
     *
     * -til: Could change the tile appearance if the building is
     * stockade-type
     *
     * @param building a <code>Building</code> value
     */
    public void addBuilding(final Building building) {
        BuildingType buildingType = building.getType().getFirstLevel();
        buildingMap.put(buildingType.getId(), building);
        addFeatures(building.getType());
        invalidateCache();
    }

    /**
     * Remove a building from this Colony.
     *
     * -til: Could change the tile appearance if the building is
     * stockade-type
     *
     * @param building The <code>Building</code> to remove.
     * @return True if the building was removed.
     */
    public boolean removeBuilding(final Building building) {
        BuildingType buildingType = building.getType().getFirstLevel();
        boolean result = buildingMap.remove(buildingType.getId()) != null;
        if (result) {
            removeFeatures(building.getType());
            invalidateCache();
            checkBuildQueueIntegrity(true);
        }
        return result;
    }

    /**
     * Gets a work location with a given ability.
     *
     * @param ability An ability key.
     * @return A <code>WorkLocation</code> with the required
     *     <code>Ability</code>, or null if not found.
     */
    public WorkLocation getWorkLocationWithAbility(String ability) {
        for (WorkLocation wl : getCurrentWorkLocations()) {
            if (wl.hasAbility(ability)) return wl;
        }
        return null;
    }

    /**
     * Collect the work locations for consuming a given type of goods.
     *
     * @param goodsType The <code>GoodsType</code> to consume.
     * @return A list of <code>WorkLocation</code>s which consume
     *     the given type of goods.
     */
    public List<WorkLocation> getWorkLocationsForConsuming(GoodsType goodsType) {
        List<WorkLocation> result = new ArrayList<WorkLocation>();
        for (WorkLocation wl : getCurrentWorkLocations()) {
            for (AbstractGoods input : wl.getInputs()) {
                if (input.getType() == goodsType) result.add(wl);
            }
        }
        return result;
    }

    /**
     * Collect the work locations for producing a given type of goods.
     *
     * @param goodsType The <code>GoodsType</code> to produce.
     * @return A list of <code>WorkLocation</code>s which produce
     *     the given type of goods.
     */
    public List<WorkLocation> getWorkLocationsForProducing(GoodsType goodsType) {
        List<WorkLocation> result = new ArrayList<WorkLocation>();
        for (WorkLocation wl : getCurrentWorkLocations()) {
            for (AbstractGoods ag : wl.getOutputs()) {
                if (ag.getType() == goodsType) result.add(wl);
            }
        }
        return result;
    }

    /**
     * Find a work location for producing a given type of goods.
     * Beware that this may not be the optimal location for the
     * production, for which {@link #getWorkLocationFor} is better.
     *
     * @param goodsType The <code>GoodsType</code> to produce.
     * @return A <code>WorkLocation</code>s which produces
     *      the given type of goods, or null if not found.
     */
    public WorkLocation getWorkLocationForProducing(GoodsType goodsType) {
        List<WorkLocation> wls = getWorkLocationsForProducing(goodsType);
        return (wls.isEmpty()) ? null : wls.get(0);
    }

    /**
     * Gets the work location best suited for the given unit to
     * produce a type of goods.
     *
     * @param unit The <code>Unit</code> to get the building for.
     * @param goodsType The <code>GoodsType</code> to produce.
     * @return The best <code>WorkLocation</code> found.
     */
    public WorkLocation getWorkLocationFor(Unit unit, GoodsType goodsType) {
        if (goodsType == null) return getWorkLocationFor(unit);
        Occupation occupation
            = getOccupationFor(unit, goodsType.getEquivalentTypes());
        return (occupation == null) ? null : occupation.workLocation;
    }

    /**
     * Gets the work location best suited for the given unit.
     *
     * @param unit The <code>Unit</code> to check for.
     * @return The best <code>WorkLocation</code> found.
     */
    public WorkLocation getWorkLocationFor(Unit unit) {
        Occupation occupation = getOccupationFor(unit, false);
        return (occupation == null) ? null : occupation.workLocation;
    }

    /**
     * Get the best work type for a unit at a work location in this colony.
     *
     * @param unit The <code>Unit</code> to find a work type for.
     * @param wl The <code>WorkLocation</code> to work at.
     * @return The best work <code>GoodsType</code> for the unit, or null
     *     if none found.
     */
    public GoodsType getWorkTypeFor(Unit unit, WorkLocation wl) {
        Occupation occupation = getOccupationAt(unit, wl, true);
        return (occupation == null) ? null : occupation.workType;
    }

    /**
     * Is a tile actually in use by this colony?
     *
     * @param tile The <code>Tile</code> to test.
     * @return True if this tile is actively in use by this colony.
     */
    public boolean isTileInUse(Tile tile) {
        ColonyTile colonyTile = getColonyTile(tile);
        return colonyTile != null && !colonyTile.isEmpty();
    }

    /**
     * Get the warehouse-type building in this colony.
     *
     * @return The warehouse <code>Building</code>.
     */
    public Building getWarehouse() {
        // TODO: it should search for more than one building?
        for (Building building : buildingMap.values()) {
            if (building.getType().hasModifier(Modifier.WAREHOUSE_STORAGE)) {
                return building;
            }
        }
        return null;
    }

    /**
     * Does this colony have a stockade?
     *
     * @return True if the colony has a stockade.
     */
    public boolean hasStockade() {
        return getStockade() != null;
    }

    /**
     * Gets the stockade building in this colony.
     *
     * @return The stockade <code>Building</code>.
     */
    public Building getStockade() {
        // TODO: it should search for more than one building?
        for (Building building : buildingMap.values()) {
            if (building.getType().hasModifier(Modifier.DEFENCE)) {
                return building;
            }
        }
        return null;
    }

    /**
     * Gets the stockade key, as should be visible to the owner
     * or a player that can see this colony.
     *
     * @return The stockade key, or null if no stockade-building is present.
     */
    public String getStockadeKey() {
        Building stockade = getStockade();
        return (stockade == null) ? null : stockade.getType().getSuffix();
    }

    /**
     * Get a weighted list of natural disasters than can strike this
     * colony.  This list comprises all natural disasters that can
     * strike the colony's tiles.
     *
     * @return A weighted list of <code>Disaster</code>s.
     */
    public List<RandomChoice<Disaster>> getDisasters() {
        List<RandomChoice<Disaster>> disasters
            = new ArrayList<RandomChoice<Disaster>>();
        for (ColonyTile tile : colonyTiles) {
            disasters.addAll(tile.getWorkTile().getDisasters());
        }
        return disasters;
    }


    // What are we building?  What can we build?

    /**
     * Is a building type able to be automatically built at no cost.
     * True when the player has a modifier that collapses the cost to zero.
     *
     * @param buildingType a <code>BuildingType</code> value
     * @return True if the building is available at zero cost.
     */
    public boolean isAutomaticBuild(BuildingType buildingType) {
        float value = owner.applyModifiers(100f, getGame().getTurn(),
            Modifier.BUILDING_PRICE_BONUS, buildingType);
        return value == 0f && canBuild(buildingType);
    }

    /**
     * Gets a list of every unit type this colony may build.
     *
     * @return A list of buildable <code>UnitType</code>s.
     */
    public List<UnitType> getBuildableUnits() {
        ArrayList<UnitType> buildableUnits = new ArrayList<UnitType>();
        List<UnitType> unitTypes = getSpecification().getUnitTypeList();
        for (UnitType unitType : unitTypes) {
            if (unitType.needsGoodsToBuild() && canBuild(unitType)) {
                buildableUnits.add(unitType);
            }
        }
        return buildableUnits;
    }

    /**
     * Returns how many turns it would take to build the given
     * <code>BuildableType</code>.
     *
     * @param buildable The <code>BuildableType</code> to build.
     * @return The number of turns to build the buildable, negative if
     *     some goods are not being built, UNDEFINED if none is.
     */
    public int getTurnsToComplete(BuildableType buildable) {
        return getTurnsToComplete(buildable, null);
    }

    /**
     * Returns how many turns it would take to build the given
     * <code>BuildableType</code>.
     *
     * @param buildable The <code>BuildableType</code> to build.
     * @param needed The <code>AbstractGoods</code> needed to continue
     *     the build.
     * @return The number of turns to build the buildable, negative if
     *     some goods are not being built, UNDEFINED if none is.
     */
    public int getTurnsToComplete(BuildableType buildable,
                                  AbstractGoods needed) {
        int result = 0;
        boolean goodsMissing = false;
        boolean goodsBeingProduced = false;
        boolean productionMissing = false;

        ProductionInfo info = productionCache.getProductionInfo(buildQueue);
        for (AbstractGoods ag : buildable.getRequiredGoods()) {
            int amountNeeded = ag.getAmount();
            int amountAvailable = getGoodsCount(ag.getType());
            if (amountAvailable >= amountNeeded) continue;
            goodsMissing = true;
            int amountProduced = productionCache.getNetProductionOf(ag.getType());
            if (info != null) {
                AbstractGoods consumed = AbstractGoods.findByType(ag.getType(), info.getConsumption());
                if (consumed != null) {
                    // add the amount the build queue itself will consume
                    amountProduced += consumed.getAmount();
                }
            }
            if (amountProduced <= 0) {
                productionMissing = true;
                if (needed != null) {
                    needed.setType(ag.getType());
                    needed.setAmount(ag.getAmount());
                }
                continue;
            }
            goodsBeingProduced = true;

            int amountRemaining = amountNeeded - amountAvailable;
            int eta = amountRemaining / amountProduced;
            if (amountRemaining % amountProduced != 0) {
                eta++;
            }
            result = Math.max(result, eta);
        }
        return (!goodsMissing) ? 0
            : (!goodsBeingProduced) ? UNDEFINED
            : (productionMissing) ? -result
            : result;
    }

    /**
     * Returns <code>true</code> if this Colony can breed the given
     * type of Goods. Only animals (such as horses) are expected to be
     * breedable.
     *
     * @param goodsType a <code>GoodsType</code> value
     * @return a <code>boolean</code> value
     */
    public boolean canBreed(GoodsType goodsType) {
        int breedingNumber = goodsType.getBreedingNumber();
        return (breedingNumber < GoodsType.INFINITY &&
                breedingNumber <= getGoodsCount(goodsType));
    }

    /**
     * Gets the type of building currently being built.
     *
     * @return The type of building currently being built.
     */
    public BuildableType getCurrentlyBuilding() {
        return buildQueue.getCurrentlyBuilding();
    }

    /**
     * Sets the current type of buildable to be built and if it is a building
     * insist that there is only one in the queue.
     *
     * @param buildable The <code>BuildableType</code> to build.
     */
    public void setCurrentlyBuilding(BuildableType buildable) {
        buildQueue.setCurrentlyBuilding(buildable);
    }

    /**
     * Describe <code>canBuild</code> method here.
     *
     * @return a <code>boolean</code> value
     */
    public boolean canBuild() {
        return canBuild(getCurrentlyBuilding());
    }

    /**
     * Returns true if this Colony can build the given BuildableType.
     *
     * @param buildableType a <code>BuildableType</code> value
     * @return a <code>boolean</code> value
     */
    public boolean canBuild(BuildableType buildableType) {
        return getNoBuildReason(buildableType, null) == NoBuildReason.NONE;
    }

    /**
     * Return the reason why the give <code>BuildableType</code> can
     * not be built.
     *
     * @param buildableType A <code>BuildableType</code> to build.
     * @param assumeBuilt An optional list of other buildable types
     *     which can be assumed to be built, for the benefit of build
     *     queue checks.
     * @return A <code>NoBuildReason</code> value decribing the failure,
     *     including <code>NoBuildReason.NONE</code> on success.
     */
    public NoBuildReason getNoBuildReason(BuildableType buildableType,
                                          List<BuildableType> assumeBuilt) {
        if (buildableType == null) {
            return NoBuildReason.NOT_BUILDING;
        } else if (!buildableType.needsGoodsToBuild()) {
            return NoBuildReason.NOT_BUILDABLE;
        } else if (buildableType.getRequiredPopulation() > getUnitCount()) {
            return NoBuildReason.POPULATION_TOO_SMALL;
        } else {
            for (Entry<String, Boolean> entry
                     : buildableType.getRequiredAbilities().entrySet()) {
                if (hasAbility(entry.getKey()) != entry.getValue()) {
                    return NoBuildReason.MISSING_ABILITY;
                }
            }
            if (buildableType.getLimits() != null) {
                for (Limit limit : buildableType.getLimits()) {
                    if (!limit.evaluate(this)) {
                        return NoBuildReason.LIMIT_EXCEEDED;
                    }
                }
            }
        }
        if (assumeBuilt == null) {
            assumeBuilt = Collections.<BuildableType>emptyList();
        }
        if (buildableType instanceof BuildingType) {
            BuildingType newBuildingType = (BuildingType) buildableType;
            Building colonyBuilding = this.getBuilding(newBuildingType);
            if (colonyBuilding == null) {
                // the colony has no similar building yet
                BuildingType from = newBuildingType.getUpgradesFrom();
                if (from != null && !assumeBuilt.contains(from)) {
                    // we are trying to build an advanced factory, we
                    // should build lower level shop first
                    return NoBuildReason.WRONG_UPGRADE;
                }
            } else {
                // a building of the same family already exists
                BuildingType from = colonyBuilding.getType().getUpgradesTo();
                if (from != newBuildingType && !assumeBuilt.contains(from)) {
                    // the existing building's next upgrade is not the
                    // new one we want to build
                    return NoBuildReason.WRONG_UPGRADE;
                }
            }
        } else if (buildableType instanceof UnitType) {
            if (!buildableType.hasAbility(Ability.PERSON)
                && !hasAbility(Ability.BUILD, buildableType)) {
                return NoBuildReason.MISSING_BUILD_ABILITY;
            }
        }
        return NoBuildReason.NONE;
    }

    /**
     * Returns the price for the remaining hammers and tools for the
     * {@link Building} that is currently being built.
     *
     * @return The price.
     * @see net.sf.freecol.client.control.InGameController#payForBuilding
     */
    public int getPriceForBuilding() {
        return getPriceForBuilding(getCurrentlyBuilding());
    }

    /**
     * Gets the price for the remaining resources to build a given buildable.
     *
     * @param type The <code>BuildableType</code> to build.
     * @return The price.
     * @see net.sf.freecol.client.control.InGameController#payForBuilding
     */
    public int getPriceForBuilding(BuildableType type) {
        return priceGoodsForBuilding(getRequiredGoods(type));
    }

    /**
     * Gets a price for a map of resources to build a given buildable.
     *
     * @param required A list of required <code>AbstractGoods</code>.
     * @return The price.
     * @see net.sf.freecol.client.control.InGameController#payForBuilding
     */
    public int priceGoodsForBuilding(List<AbstractGoods> required) {
        int price = 0;
        Market market = getOwner().getMarket();
        for (AbstractGoods ag : required) {
            GoodsType goodsType = ag.getType();
            int amount = ag.getAmount();
            if (goodsType.isStorable()) {
                // TODO: magic number!
                price += (market.getBidPrice(goodsType, amount) * 110) / 100;
            } else {
                price += goodsType.getPrice() * amount;
            }
        }
        return price;
    }

    /**
     * Gets a map of the types of goods and amount thereof required to
     * finish a buildable in this colony.
     *
     * @param type The <code>BuildableType</code> to build.
     * @return The map to completion.
     */
    public List<AbstractGoods> getRequiredGoods(BuildableType type) {
        List<AbstractGoods> result = new ArrayList<AbstractGoods>();
        for (AbstractGoods goods : type.getRequiredGoods()) {
            GoodsType goodsType = goods.getType();
            int remaining = goods.getAmount() - getGoodsCount(goodsType);
            if (remaining > 0) {
                result.add(new AbstractGoods(goodsType, remaining));
            }
        }
        return result;
    }

    /**
     * Gets all the goods required to complete a build.  The list
     * includes the prerequisite raw materials as well as the direct
     * requirements (i.e. hammers, tools).  If enough of a required
     * goods is present in the colony, then that type is not returned.
     * Take care to order types with raw materials first so that we
     * can prioritize gathering what is required before manufacturing.
     *
     * Public for the benefit of AI planning and the test suite.
     *
     * @param buildable The <code>BuildableType</code> to consider.
     * @return A list of required abstract goods.
     */
    public List<AbstractGoods> getFullRequiredGoods(BuildableType buildable) {
        List<AbstractGoods> required = new ArrayList<AbstractGoods>();
        if (buildable == null) return required;
        for (AbstractGoods ag : buildable.getRequiredGoods()) {
            int amount = ag.getAmount();
            GoodsType type = ag.getType();
            while (type != null) {
                if (amount <= this.getGoodsCount(type)) break; // Shortcut
                required.add(0, new AbstractGoods(type,
                        amount - this.getGoodsCount(type)));
                type = type.getInputType();
            }
        }
        return required;
    }

    /**
     * Check if the owner can buy the remaining hammers and tools for
     * the {@link Building} that is currently being built.
     *
     * @exception IllegalStateException If the owner of this <code>Colony</code>
     *                has an insufficient amount of gold.
     * @see #getPriceForBuilding
     */
    public boolean canPayToFinishBuilding() {
        return canPayToFinishBuilding(getCurrentlyBuilding());
    }

    /**
     * Check if the owner can buy the remaining hammers and tools for
     * the {@link Building} given.
     *
     * @param buildableType a <code>BuildableType</code> value
     * @return a <code>boolean</code> value
     * @exception IllegalStateException If the owner of this <code>Colony</code>
     *                has an insufficient amount of gold.
     * @see #getPriceForBuilding
     */
    public boolean canPayToFinishBuilding(BuildableType buildableType) {
        return buildableType != null
            && getOwner().checkGold(getPriceForBuilding(buildableType));
    }


    // Liberty and the consequences

    /**
     * Adds to the liberty points by increasing the liberty goods present.
     * Used only by DebugMenu.
     *
     * @param amount The number of liberty to add.
     */
    public void addLiberty(int amount) {
        List<GoodsType> libertyTypeList = getSpecification()
            .getLibertyGoodsTypeList();
        final int uc = getUnitCount();
        if (calculateRebels(uc, sonsOfLiberty) <= uc + 1
            && amount > 0
            && !libertyTypeList.isEmpty()) {
            addGoods(libertyTypeList.get(0), amount);
        }
        updateSoL();
        updateProductionBonus();
    }

    /**
     * Modify the liberty points by amount given.
     *
     * @param amount An amount of liberty.
     */
    public void modifyLiberty(int amount) {
        liberty += amount;
        getOwner().modifyLiberty(amount);
        updateSoL();
        updateProductionBonus();
    }

    /**
     * Calculates the current SoL membership of the colony based on
     * the liberty value and colonists.
     */
    public void updateSoL() {
        int uc = getUnitCount();
        oldSonsOfLiberty = sonsOfLiberty;
        oldTories = tories;
        sonsOfLiberty = calculateSoLPercentage(uc, getEffectiveLiberty());
        tories = uc - calculateRebels(uc, sonsOfLiberty);
    }

    /**
     * Calculate the SoL membership percentage of the colony based on the
     * number of colonists and liberty.
     *
     * @param uc The proposed number of units in the colony.
     * @param liberty The amount of liberty.
     * @return The percentage of SoLs, negative if not calculable.
     */
    private static int calculateSoLPercentage(int uc, int liberty) {
        if (uc <= 0) return -1;

        int membership = (liberty * 100) / (LIBERTY_PER_REBEL * uc);
        if (membership < 0) {
            membership = 0;
        } else if (membership > 100) {
            membership = 100;
        }
        return membership;
    }

    /**
     * Calculate the SoL membership percentage of a colony.
     *
     * @return The percentage of SoLs, negative if not calculable.
     */
    public int getSoLPercentage() {
        return calculateSoLPercentage(getUnitCount(), getEffectiveLiberty());
    }

    /**
     * Calculate the number of rebels given a SoL percentage and unit count.
     *
     * @param uc The number of units in the colony.
     * @param solPercent The percentage of SoLs.
     */
    public static int calculateRebels(int uc, int solPercent) {
        return (int)Math.floor(0.01 * solPercent * uc);
    }

    /**
     * Gets the Tory membership percentage of the colony.
     *
     * @return The current Tory membership of the colony.
     */
    public int getTory() {
        return 100 - getSoL();
    }

    /**
     * Update the colony's production bonus.
     *
     * @return True if the bonus changed.
     */
    protected boolean updateProductionBonus() {
        final Specification spec = getSpecification();
        final int veryBadGovernment
            = spec.getInteger("model.option.veryBadGovernmentLimit");
        final int badGovernment
            = spec.getInteger("model.option.badGovernmentLimit");
        final int veryGoodGovernment
            = spec.getInteger("model.option.veryGoodGovernmentLimit");
        final int goodGovernment
            = spec.getInteger("model.option.goodGovernmentLimit");
        int newBonus = (sonsOfLiberty >= veryGoodGovernment) ? 2
            : (sonsOfLiberty >= goodGovernment) ? 1
            : (tories > veryBadGovernment) ? -2
            : (tories > badGovernment) ? -1
            : 0;
        if (productionBonus != newBonus) {
            invalidateCache();
            productionBonus = newBonus;
            return true;
        }
        return false;
    }

    /**
     * Gets the number of units that would be good to add/remove from this
     * colony.  That is the number of extra units that can be added without
     * damaging the production bonus, or the number of units to remove to
     * improve it.
     *
     * @return The number of units to add to the colony, or if negative
     *      the negation of the number of units to remove.
     */
    public int getPreferredSizeChange() {
        int i, limit, pop = getUnitCount();
        if (productionBonus < 0) {
            limit = pop;
            for (i = 1; i < limit; i++) {
                if (governmentChange(pop - i) == 1) break;
            }
            return -i;
        } else {
            final Specification spec = getSpecification();
            limit = spec.getInteger("model.option.badGovernmentLimit");
            for (i = 1; i < limit; i++) {
                if (governmentChange(pop + i) == -1) break;
            }
            return i - 1;
        }
    }


    // Unit manipulation and population

    /**
     * Special routine to handle non-specific add of unit to colony.
     *
     * @param unit The <code>Unit</code> to add.
     * @return True if the add succeeds.
     */
    public boolean joinColony(Unit unit) {
        Occupation occupation = getOccupationFor(unit, false);
        if (occupation == null) {
            if (!traceOccupation) {
                LogBuilder lb = new LogBuilder(64);
                getOccupationFor(unit, false, lb);
                lb.log(logger, Level.WARNING);
            }
            return false;
        }
        return occupation.install(unit);
    }

    /**
     * Can this colony reduce its population voluntarily?
     *
     * This is generally the case, but can be prevented by buildings
     * such as the stockade in classic mode.
     *
     * @return True if the population can be reduced.
     */
    public boolean canReducePopulation() {
        return getUnitCount() > applyModifiers(0f, getGame().getTurn(),
                                               Modifier.MINIMUM_COLONY_SIZE);
    }

    /**
     * Gets the message to display if the colony can not reduce its
     * population.
     *
     * @return A string to describing why a colony can not reduce its
     *     population, or null if it can.
     */
    public String getReducePopulationMessage() {
        if (canReducePopulation()) return null;
        String message = "";
        Set<Modifier> modifierSet = getModifiers(Modifier.MINIMUM_COLONY_SIZE);
        for (Modifier modifier : modifierSet) {
            FreeColObject source = modifier.getSource();
            if (source instanceof BuildingType) {
                // If the modifier source is a building type, use the
                // building in the colony, which may be of a different
                // level to the modifier source.
                // This prevents the stockade modifier from matching a
                // colony-fort, and thus the message attributing the
                // failure to reduce population to a non-existing
                // stockade, BR#3522055.
                source = getBuilding((BuildingType)source).getType();
            }
            return Messages.message(StringTemplate.template("colonyPanel.minimumColonySize")
                .addName("%object%", source));
        }
        return message;
    }

    /**
     * Returns 1, 0, or -1 to indicate that government would improve,
     * remain the same, or deteriorate if the colony had the given
     * population.
     *
     * @param unitCount The proposed population for the colony.
     * @return 1, 0 or -1.
     */
    public int governmentChange(int unitCount) {
        final Specification spec = getSpecification();
        final int veryBadGovernment
            = spec.getInteger(GameOptions.VERY_BAD_GOVERNMENT_LIMIT);
        final int badGovernment
            = spec.getInteger(GameOptions.BAD_GOVERNMENT_LIMIT);
        final int veryGoodGovernment
            = spec.getInteger(GameOptions.VERY_GOOD_GOVERNMENT_LIMIT);
        final int goodGovernment
            = spec.getInteger(GameOptions.GOOD_GOVERNMENT_LIMIT);

        int rebelPercent = calculateSoLPercentage(unitCount, getEffectiveLiberty());
        int rebelCount = calculateRebels(unitCount, rebelPercent);
        int loyalistCount = unitCount - rebelCount;

        int result = 0;
        if (rebelPercent >= veryGoodGovernment) { // There are no tories left.
            if (sonsOfLiberty < veryGoodGovernment) {
                result = 1;
            }
        } else if (rebelPercent >= goodGovernment) {
            if (sonsOfLiberty >= veryGoodGovernment) {
                result = -1;
            } else if (sonsOfLiberty < goodGovernment) {
                result = 1;
            }
        } else {
            if (sonsOfLiberty >= goodGovernment) {
                result = -1;
            } else { // Now that no bonus is applied, penalties may.
                if (loyalistCount > veryBadGovernment) {
                    if (tories <= veryBadGovernment) {
                        result = -1;
                    }
                } else if (loyalistCount > badGovernment) {
                    if (tories <= badGovernment) {
                        result = -1;
                    } else if (tories > veryBadGovernment) {
                        result = 1;
                    }
                } else {
                    if (tories > badGovernment) {
                        result = 1;
                    }
                }
            }
        }
        return result;
    }

    public ModelMessage checkForGovMgtChangeMessage() {
        final Specification spec = getSpecification();
        final int veryBadGovernment
            = spec.getInteger(GameOptions.VERY_BAD_GOVERNMENT_LIMIT);
        final int badGovernment
            = spec.getInteger(GameOptions.BAD_GOVERNMENT_LIMIT);
        final int veryGoodGovernment
            = spec.getInteger(GameOptions.VERY_GOOD_GOVERNMENT_LIMIT);
        final int goodGovernment
            = spec.getInteger(GameOptions.GOOD_GOVERNMENT_LIMIT);

        String msgId = null;
        int number = 0;
        ModelMessage.MessageType msgType = ModelMessage.MessageType.GOVERNMENT_EFFICIENCY;
        if (sonsOfLiberty >= veryGoodGovernment) {
            // there are no tories left
            if (oldSonsOfLiberty < veryGoodGovernment) {
                msgId = "model.colony.veryGoodGovernment";
                msgType = ModelMessage.MessageType.SONS_OF_LIBERTY;
                number = veryGoodGovernment;
            }
        } else if (sonsOfLiberty >= goodGovernment) {
            if (oldSonsOfLiberty == veryGoodGovernment) {
                msgId = "model.colony.lostVeryGoodGovernment";
                msgType = ModelMessage.MessageType.SONS_OF_LIBERTY;
                number = veryGoodGovernment;
            } else if (oldSonsOfLiberty < goodGovernment) {
                msgId = "model.colony.goodGovernment";
                msgType = ModelMessage.MessageType.SONS_OF_LIBERTY;
                number = goodGovernment;
            }
        } else {
            if (oldSonsOfLiberty >= goodGovernment) {
                msgId = "model.colony.lostGoodGovernment";
                msgType = ModelMessage.MessageType.SONS_OF_LIBERTY;
                number = goodGovernment;
            }

            // Now that no bonus is applied, penalties may.
            if (tories > veryBadGovernment) {
                if (oldTories <= veryBadGovernment) {
                    // government has become very bad
                    msgId = "model.colony.veryBadGovernment";
                }
            } else if (tories > badGovernment) {
                if (oldTories <= badGovernment) {
                    // government has become bad
                    msgId = "model.colony.badGovernment";
                } else if (oldTories > veryBadGovernment) {
                    // government has improved, but is still bad
                    msgId = "model.colony.governmentImproved1";
                }
            } else if (oldTories > badGovernment) {
                // government was bad, but has improved
                msgId = "model.colony.governmentImproved2";
            }
        }

        GoodsType bells = getSpecification().getGoodsType("model.goods.bells");
        return (msgId == null) ? null
            : new ModelMessage(msgType, msgId, this, bells)
            .addName("%colony%", getName())
            .addAmount("%number%", number);
    }

    /**
     * Signal to the colony that its population is changing.
     * Called from Unit.setLocation when a unit moves into or out of this
     * colony, but *not* if it is moving within the colony.
     */
    public void updatePopulation() {
        updateSoL();
        updateProductionBonus();
        if (getOwner().isAI()) {
            firePropertyChange(Colony.REARRANGE_WORKERS, true, false);
        }
    }

    /**
     * Signal to the colony that a unit is moving in or out or
     * changing its internal work location to one with a different
     * teaching ability.  This requires either checking for a new
     * teacher or student, or clearing any existing education
     * relationships.
     *
     * @param unit The <code>Unit</code> that is changing its education state.
     * @param enable If true, check for new education opportunities, otherwise
     *     clear existing ones.
     */
    public void updateEducation(Unit unit, boolean enable) {
        WorkLocation wl = unit.getWorkLocation();
        if (wl == null) {
            throw new RuntimeException("updateEducation(" + unit
                + ") unit not at work location.");
        } else if (wl.getColony() != this) {
            throw new RuntimeException("updateEducation(" + unit
                + ") unit not at work location in this colony.");
        }
        if (enable) {
            if (wl.canTeach()) {
                Unit student = unit.getStudent();
                if (student == null
                    && (student = findStudent(unit)) != null) {
                    unit.setStudent(student);
                    student.setTeacher(unit);
                    unit.setTurnsOfTraining(0);// Teacher starts teaching
                    unit.changeWorkType(null);
                }
            } else {
                Unit teacher = unit.getTeacher();
                if (teacher == null
                    && (teacher = findTeacher(unit)) != null) {
                    unit.setTeacher(teacher);
                    teacher.setStudent(unit);
                }
            }
        } else {
            if (wl.canTeach()) {
                Unit student = unit.getStudent();
                if (student != null) {
                    student.setTeacher(null);
                    unit.setStudent(null);
                    unit.setTurnsOfTraining(0);// Teacher stops teaching
                }
            } else {
                Unit teacher = unit.getTeacher();
                if (teacher != null) {
                    teacher.setStudent(null);
                    unit.setTeacher(null);
                }
            }
        }
    }

    /**
     * Does this colony have undead units?
     *
     * @return True if this colony has undead units.
     */
    public boolean isUndead() {
        Unit u = getFirstUnit();
        return u != null && u.isUndead();
    }

    /**
     * Gets the apparent number of units at this colony.
     * Used in client enemy colonies
     *
     * @return The apparent number of <code>Unit</code>s at this colony.
     */
    public int getDisplayUnitCount() {
        return (displayUnitCount > 0) ? displayUnitCount : getUnitCount();
    }

    /**
     * Sets the apparent number of units at this colony.
     * Used in client enemy colonies
     *
     * @param count The new apparent number of <code>Unit</code>s at
     *     this colony.
     */
    public void setDisplayUnitCount(int count) {
        this.displayUnitCount = count;
    }


    // Defence and offense response

    /**
     * Gets the best defender type available to this colony.
     *
     * @return The best available defender type.
     */
    public UnitType getBestDefenderType() {
        UnitType bestDefender = null;
        for (UnitType unitType : getSpecification().getUnitTypeList()) {
            if (unitType.getDefence() > 0
                && (bestDefender == null
                    || bestDefender.getDefence() < unitType.getDefence())
                && !unitType.hasAbility(Ability.NAVAL_UNIT)
                && unitType.isAvailableTo(getOwner())) {
                bestDefender = unitType;
            }
        }
        return bestDefender;
    }

    /**
     * Gets the total defence power.
     *
     * @return The total defence power.
     */
    public float getTotalDefencePower() {
        CombatModel cm = getGame().getCombatModel();
        float defence = 0.0f;
        for (Unit unit : getTile().getUnitList()) {
            if (unit.isDefensiveUnit()) {
                defence += cm.getDefencePower(null, unit);
            }
        }
        return defence;
    }

    /**
     * Determines whether this colony is sufficiently unprotected and
     * contains something worth pillaging.  To be called by CombatModels
     * when the attacker has defeated an unarmed colony defender.
     *
     * @param attacker The <code>Unit</code> that has defeated the defender.
     * @return True if the attacker can pillage this colony.
     */
    public boolean canBePillaged(Unit attacker) {
        return !hasStockade()
            && attacker.hasAbility(Ability.PILLAGE_UNPROTECTED_COLONY)
            && !(getBurnableBuildings().isEmpty()
                && getTile().getNavalUnits().isEmpty()
                && (getLootableGoodsList().isEmpty()
                    || !attacker.getType().canCarryGoods()
                    || !attacker.hasSpaceLeft())
                && !canBePlundered());
    }

    /**
     * Checks if this colony can be plundered.  That is, can it yield
     * non-zero gold.
     *
     * @return True if at least one piece of gold can be plundered from this
     *     colony.
     */
    public boolean canBePlundered() {
        return owner.checkGold(1);
    }

    /**
     * Gets the buildings in this colony that could be burned by a raid.
     *
     * @return A list of burnable buildings.
     */
    public List<Building> getBurnableBuildings() {
        List<Building> buildingList = new ArrayList<Building>();
        for (Building building : getBuildings()) {
            if (building.canBeDamaged()) buildingList.add(building);
        }
        return buildingList;
    }

    /**
     * Gets a list of all stored goods in this colony, suitable for
     * being looted.
     *
     * @return A list of lootable goods in this colony.
     */
    public List<Goods> getLootableGoodsList() {
        List<Goods> goodsList = new ArrayList<Goods>();
        for (Goods goods : getGoodsContainer().getGoods()) {
            if (goods.getType().isStorable()) goodsList.add(goods);
        }
        return goodsList;
    }

    /**
     * Returns <code>true</code> if the number of enemy combat units
     * on all tiles that belong to the colony exceeds the number of
     * friendly combat units. At the moment, only the colony owner's
     * own units are considered friendly, but that could be extended
     * to include the units of allied players.
     *
     * TODO: if a colony is under siege, it should not be possible to
     * put units outside the colony, unless those units are armed.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isUnderSiege() {
        int friendlyUnits = 0;
        int enemyUnits = 0;
        for (ColonyTile colonyTile : colonyTiles) {
            for (Unit unit : colonyTile.getWorkTile().getUnitList()) {
                if (unit.getOwner() == getOwner()) {
                    if (unit.isDefensiveUnit()) {
                        friendlyUnits++;
                    }
                } else if (getOwner().atWarWith(unit.getOwner())) {
                    if (unit.isOffensiveUnit()) {
                        enemyUnits++;
                    }
                }
            }
        }
        return enemyUnits > friendlyUnits;
    }


    // Education

    /**
     * Returns true if this colony has a schoolhouse and the unit type is a
     * skilled unit type with a skill level not exceeding the level of the
     * schoolhouse. @see Building#canAdd
     *
     * @param unit The unit to add as a teacher.
     * @return <code>true</code> if this unit type could be added.
     */
    public boolean canTrain(Unit unit) {
        return canTrain(unit.getType());
    }

    /**
     * Returns true if this colony has a schoolhouse and the unit type is a
     * skilled unit type with a skill level not exceeding the level of the
     * schoolhouse. The number of units already in the schoolhouse and
     * the availability of pupils are not taken into account. @see
     * Building#canAdd
     *
     * @param unitType The unit type to add as a teacher.
     * @return <code>true</code> if this unit type could be added.
     */
    public boolean canTrain(UnitType unitType) {
        if (!hasAbility(Ability.TEACH)) {
            return false;
        }

        for (Building building : buildingMap.values()) {
            if (building.canTeach() && building.canAddType(unitType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets a list of all teachers currently present in the school
     * building.
     *
     * @return A list of teacher <code>Unit</code>s.
     */
    public List<Unit> getTeachers() {
        List<Unit> teachers = new ArrayList<Unit>();
        for (Building building : buildingMap.values()) {
            if (building.canTeach()) {
                teachers.addAll(building.getUnitList());
            }
        }
        return teachers;
    }

    /**
     * Find a teacher for the specified student.
     * Do not search if ALLOW_STUDENT_SELECTION is true--- it is the
     * player's job then.
     *
     * @param student The student <code>Unit</code> that needs a teacher.
     * @return A potential teacher, or null of none found.
     */
    public Unit findTeacher(Unit student) {
        if (getSpecification().getBoolean(GameOptions.ALLOW_STUDENT_SELECTION))
            return null; // No automatic assignment
        for (Building building : getBuildings()) {
            if (building.canTeach()) {
                for (Unit unit : building.getUnitList()) {
                    if (unit.getStudent() == null
                        && student.canBeStudent(unit)) return unit;
                }
            }
        }
        return null;
    }

    /**
     * Find a student for the specified teacher.
     * Do not search if ALLOW_STUDENT_SELECTION is true--- its the
     * player's job then.
     *
     * @param teacher The teacher <code>Unit</code> that needs a student.
     * @return A potential student, or null of none found.
     */
    public Unit findStudent(final Unit teacher) {
        if (getSpecification().getBoolean(GameOptions.ALLOW_STUDENT_SELECTION))
            return null; // No automatic assignment
        Unit student = null;
        GoodsType expertProduction = teacher.getType().getExpertProduction();
        int skillLevel = INFINITY;
        for (Unit potentialStudent : getUnitList()) {
            /**
             * Always pick the student with the least skill first.
             * Break ties by favouring the one working in the teacher's trade,
             * otherwise first applicant wins.
             */
            if (potentialStudent.getTeacher() == null
                && potentialStudent.canBeStudent(teacher)
                && (student == null
                    || potentialStudent.getSkillLevel() < skillLevel
                    || (potentialStudent.getSkillLevel() == skillLevel
                        && potentialStudent.getWorkType() == expertProduction))) {
                student = potentialStudent;
                skillLevel = student.getSkillLevel();
            }
        }
        return student;
    }


    // Import/export

    /**
     * How much of a goods type can be exported from this colony?
     *
     * @param goodsType The <code>GoodsType</code> to export.
     * @return The amount of this type of goods available for export.
     */
    public int getExportAmount(GoodsType goodsType) {
        int present = getGoodsCount(goodsType);
        int exportable = getExportData(goodsType).getExportLevel();
        return (present < exportable) ? 0 : present - exportable;
    }

    /**
     * How much of a goods type can be imported into this colony?
     *
     * @param goodsType The <code>GoodsType</code> to import.
     * @return The amount of this type of goods that can be imported.
     */
    public int getImportAmount(GoodsType goodsType) {
        int present = getGoodsCount(goodsType);
        if (goodsType.isFoodType()) return Integer.MAX_VALUE;
        int capacity = getWarehouseCapacity();
        return (present > capacity) ? 0 : capacity - present;
    }


    // Production and consumption

    /**
     * Returns a list of all {@link Consumer}s in the colony sorted by
     * priority. Consumers include all object that consume goods,
     * e.g. Units, Buildings and BuildQueues.
     *
     * @return a list of consumers
     */
    public List<Consumer> getConsumers() {
        List<Consumer> result = new ArrayList<Consumer>();
        result.addAll(getUnitList());
        result.addAll(buildingMap.values());
        result.add(buildQueue);
        result.add(populationQueue);

        Collections.sort(result, Consumer.COMPARATOR);
        return result;
    }

    /**
     * Returns the number of goods of a given type used by the settlement
     * each turn.
     *
     * @param goodsType <code>GoodsType</code> values
     * @return an <code>int</code> value
     */
    public int getConsumptionOf(GoodsType goodsType) {
        final Specification spec = getSpecification();
        int result = super.getConsumptionOf(goodsType);
        if (spec.getGoodsType("model.goods.bells").equals(goodsType)) {
            result -= spec.getInteger("model.option.unitsThatUseNoBells");
        }
        return Math.max(0, result);
    }

    /**
     * Gets the combined production of all food types.
     *
     * @return an <code>int</code> value
     */
    public int getFoodProduction() {
        int result = 0;
        for (GoodsType foodType : getSpecification().getFoodGoodsTypeList()) {
            result += getTotalProductionOf(foodType);
        }
        return result;
    }

    /**
     * Get the current production <code>Modifier</code>, which is
     * generated from the current production bonus.
     *
     * @param goodsType The <code>GoodsType</code> to produce.
     * @return A list of suitable <code>Modifier</code>s.
     */
    public List<Modifier> getProductionModifiers(GoodsType goodsType) {
        if (productionBonus == 0) return Collections.<Modifier>emptyList();
        Modifier mod = new Modifier(goodsType.getId(), productionBonus,
                                    Modifier.ModifierType.ADDITIVE,
                                    Specification.SOL_MODIFIER_SOURCE);
        mod.setModifierIndex(Modifier.COLONY_PRODUCTION_INDEX);
        List<Modifier> result = new ArrayList<Modifier>();
        result.add(mod);
        return result;
    }

    /**
     * Returns the net production of the given GoodsType.
     *
     * @param goodsType a <code>GoodsType</code> value
     * @return an <code>int</code> value
     */
    public int getNetProductionOf(GoodsType goodsType) {
        return productionCache.getNetProductionOf(goodsType);
    }

    /**
     * Is a work location productive?
     *
     * @param workLocation The <code>WorkLocation</code> to check.
     * @return True if something is being produced at the
     *     <code>WorkLocation</code>.
     */
    public boolean isProductive(WorkLocation workLocation) {
        ProductionInfo info = productionCache.getProductionInfo(workLocation);
        return info != null && info.getProduction() != null
            && !info.getProduction().isEmpty()
            && info.getProduction().get(0).getAmount() > 0;
    }

    /**
     * Returns the net production of the given GoodsType adjusted by
     * the possible consumption of BuildQueues.
     *
     * @param goodsType a <code>GoodsType</code> value
     * @return an <code>int</code> value
     */
    public int getAdjustedNetProductionOf(GoodsType goodsType) {
        int result = productionCache.getNetProductionOf(goodsType);
        for (BuildQueue<?> queue : new BuildQueue<?>[] { buildQueue,
                                                         populationQueue }) {
            ProductionInfo info = productionCache.getProductionInfo(queue);
            if (info != null) {
                AbstractGoods goods = AbstractGoods.findByType(goodsType, info.getConsumption());
                if (goods != null) result += goods.getAmount();
            }
        }
        return result;
    }

    /**
     * Gets a copy of the current production map.
     * Useful in the server at the point net production is applied to a colony.
     *
     * @return A copy of the current production map.
     */
    protected TypeCountMap<GoodsType> getProductionMap() {
        return productionCache.getProductionMap();
    }

    /**
     * Returns the ProductionInfo for the given Object.
     *
     * @param object an <code>Object</code> value
     * @return a <code>ProductionInfo</code> value
     */
    public ProductionInfo getProductionInfo(Object object) {
        return productionCache.getProductionInfo(object);
    }

    /**
     * Invalidates the production cache.
     */
    public void invalidateCache() {
        productionCache.invalidate();
    }

    /**
     * Can this colony produce certain goods?
     *
     * @param goodsType The <code>GoodsType</code> to check production of.
     * @return True if the goods can be produced.
     */
    public boolean canProduce(GoodsType goodsType) {
        if (getNetProductionOf(goodsType) > 0) return true; // Obviously:-)

        if (goodsType.isBreedable()) {
            return getGoodsCount(goodsType) >= goodsType.getBreedingNumber();
        }

        // Is there a work location that can produce the goods, with
        // satisfied inputs and positive generic production potential?
        outer: for (WorkLocation wl : getWorkLocationsForProducing(goodsType)) {
            for (AbstractGoods ag : wl.getInputs()) {
                if (!canProduce(ag.getType())) continue outer;
            }
            if (wl.getGenericPotential(goodsType) > 0) return true;
        }
        return false;
    }

  
    // Planning support

    /**
     * Collects tiles that need exploring, plowing or road building
     * which may depend on current use within the colony.
     *
     * @param exploreTiles A list of <code>Tile</code>s to update with tiles
     *     to explore.
     * @param clearTiles A list of <code>Tile</code>s to update with tiles
     *     to clear.
     * @param plowTiles A list of <code>Tile</code>s to update with tiles
     *     to plow.
     * @param roadTiles A list of <code>Tile</code>s to update with tiles
     *     to build roads on.
     */
    public void getColonyTileTodo(List<Tile> exploreTiles,
                                  List<Tile> clearTiles, List<Tile> plowTiles,
                                  List<Tile> roadTiles) {
        final Specification spec = getSpecification();
        final TileImprovementType clearImprovement
            = spec.getTileImprovementType("model.improvement.clearForest");
        final TileImprovementType plowImprovement
            = spec.getTileImprovementType("model.improvement.plow");
        final TileImprovementType roadImprovement
            = spec.getTileImprovementType("model.improvement.road");

        for (Tile t : getTile().getSurroundingTiles(1)) {
            if (t.hasLostCityRumour()) exploreTiles.add(t);
        }

        for (ColonyTile ct : getColonyTiles()) {
            Tile t = ct.getWorkTile();
            if (t == null) continue; // Colony has not claimed the tile yet.

            if ((t.getTileItemContainer() == null
                    || t.getTileItemContainer()
                    .getImprovement(plowImprovement) == null)
                && plowImprovement.isTileTypeAllowed(t.getType())) {
                if (ct.isColonyCenterTile()) {
                    plowTiles.add(t);
                } else {
                    for (Unit u : ct.getUnitList()) {
                        if (u != null && u.getWorkType() != null
                            && plowImprovement.getBonus(u.getWorkType()) > 0) {
                            plowTiles.add(t);
                            break;
                        }
                    }
                }
            }

            // To assess whether other improvements are beneficial we
            // really need a unit, doing work, so we can compare the output
            // with and without the improvement.  This means we can skip
            // further consideration of the colony center tile.
            if (ct.isColonyCenterTile() || ct.isEmpty()) continue;

            TileType oldType = t.getType();
            TileType newType;
            if ((t.getTileItemContainer() == null
                    || t.getTileItemContainer()
                    .getImprovement(clearImprovement) == null)
                && clearImprovement.isTileTypeAllowed(t.getType())
                && (newType = clearImprovement.getChange(oldType)) != null) {
                for (Unit u : ct.getUnitList()) {
                    if (newType.getPotentialProduction(u.getWorkType(), u.getType())
                        > oldType.getPotentialProduction(u.getWorkType(), u.getType())) {
                        clearTiles.add(t);
                        break;
                    }
                }
            }

            if (t.getRoad() == null
                && roadImprovement.isTileTypeAllowed(t.getType())) {
                for (Unit u : ct.getUnitList()) {
                    if (roadImprovement.getBonus(u.getWorkType()) > 0) {
                        roadTiles.add(t);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Finds another unit in this colony that would be better at doing the
     * job of the specified unit.
     *
     * @param expert The <code>Unit</code> to consider.
     * @return A better expert, or null if none available.
     */
    public Unit getBetterExpert(Unit expert) {
        GoodsType production = expert.getWorkType();
        UnitType expertType = expert.getType();
        GoodsType expertise = expertType.getExpertProduction();
        Unit bestExpert = null;
        int bestImprovement = 0;

        if (production == null || expertise == null
            || production == expertise) return null;

        // We have an expert not doing the job of their expertise.
        // Check if there is a non-expert doing the job instead.
        for (Unit nonExpert : getUnitList()) {
            if (nonExpert.getWorkType() != expertise
                || nonExpert.getType() == expertType) continue;

            // We have found a unit of a different type doing the
            // job of this expert's expertise now check if the
            // production would be better if the units swapped
            // positions.
            int expertProductionNow = 0;
            int nonExpertProductionNow = 0;
            int expertProductionPotential = 0;
            int nonExpertProductionPotential = 0;

            // Get the current and potential productions for the
            // work location of the expert.
            WorkLocation ewl = expert.getWorkLocation();
            if (ewl != null) {
                expertProductionNow = ewl.getPotentialProduction(expertise,
                    expert.getType());
                nonExpertProductionPotential
                    = ewl.getPotentialProduction(expertise,
                        nonExpert.getType());
            }

            // Get the current and potential productions for the
            // work location of the non-expert.
            WorkLocation nwl = nonExpert.getWorkTile();
            if (nwl != null) {
                nonExpertProductionNow = nwl.getPotentialProduction(expertise,
                    nonExpert.getType());
                expertProductionPotential
                    = nwl.getPotentialProduction(expertise, expertType);
            }

            // Find the unit that achieves the best improvement.
            int improvement = expertProductionPotential
                + nonExpertProductionPotential
                - expertProductionNow
                - nonExpertProductionNow;
            if (improvement > bestImprovement) {
                bestImprovement = improvement;
                bestExpert = nonExpert;
            }
        }
        return bestExpert;
    }

    /**
     * determine if there is a problem with the production of the specified good
     *
     * @param goodsType  for this good
     * @param amount     warehouse amount
     * @param production production per turn
     * @return all warnings
     */
    public Collection<StringTemplate> getWarnings(GoodsType goodsType, int amount, int production) {

        List<StringTemplate> result = new LinkedList<StringTemplate>();

        if (goodsType.isFoodType() && goodsType.isStorable()) {
            if (amount + production < 0) {
                result.add(StringTemplate.template("model.colony.famineFeared")
                           .addName("%colony%", getName())
                           .addAmount("%number%", 0));
            }
        } else {
            //food is never wasted -> new settler is produced
            int waste = (amount + production - getWarehouseCapacity());
            if (waste > 0 && !getExportData(goodsType).isExported() && !goodsType.limitIgnored()) {
                result.add(StringTemplate.template("model.building.warehouseSoonFull")
                           .add("%goods%", goodsType.getNameKey())
                           .addName("%colony%", getName())
                           .addAmount("%amount%", waste));

            }
        }

        BuildableType currentlyBuilding = getCurrentlyBuilding();
        if (currentlyBuilding != null) {
            for (AbstractGoods goods : currentlyBuilding.getRequiredGoods()) {
                if (goods.getType().equals(goodsType) && amount < goods.getAmount()) {
                    result.add(StringTemplate.template("model.colony.buildableNeedsGoods")
                               .addName("%colony%", getName())
                               .add("%buildable%", currentlyBuilding.getNameKey())
                               .addAmount("%amount%", (goods.getAmount() - amount))
                               .add("%goodsType%", goodsType.getNameKey()));
                }
            }
        }

        for (WorkLocation wl : getWorkLocationsForProducing(goodsType)) {
            addInsufficientProductionMessage(result,
                productionCache.getProductionInfo(wl));
        }
        for (WorkLocation wl : getWorkLocationsForConsuming(goodsType)) {
            for (AbstractGoods ag : wl.getOutputs()) {
                if (!ag.getType().isStorable()) {
                    // the warnings are for a non-storable good, which
                    // is not displayed in the trade report
                    addInsufficientProductionMessage(result, 
                        productionCache.getProductionInfo(wl));
                }
            }
        }

        return result;
    }

    /**
     * adds a message about insufficient production for a building
     *
     * @param warnings A list of warnings to add to.
     * @param info The <code>ProductionInfo</code> for the work location.
     */
    private void addInsufficientProductionMessage(List<StringTemplate> warnings,
                                                  ProductionInfo info) {
        if (info == null
            || info.getMaximumProduction().isEmpty()) return;

        GoodsType outputType = info.getProduction().get(0).getType();
        int missingOutput = info.getMaximumProduction().get(0).getAmount()
            - info.getProduction().get(0).getAmount();
        if (missingOutput <= 0) return;

        GoodsType inputType = info.getConsumption().isEmpty()
            ? null : info.getConsumption().get(0).getType();
        int missingInput = info.getMaximumConsumption().get(0).getAmount()
            - info.getConsumption().get(0).getAmount();
        if (inputType == null) return;

        warnings.add(StringTemplate
            .template("model.colony.insufficientProduction")
            .addAmount("%outputAmount%", missingOutput)
            .add("%outputType%", outputType.getNameKey())
            .addName("%colony%", getName())
            .addAmount("%inputAmount%", missingInput)
            .add("%inputType%", inputType.getNameKey()));
    }

    /**
     * Creates a temporary copy of this colony for planning purposes.
     *
     * A simple colony.copy() can not work because all the colony
     * tiles will be left referring to uncopied work tiles which the
     * colony-copy does not own, which prevents them being used as
     * valid work locations.  We have to copy the colony tile (which
     * includes the colony), and fix up all the colony tile work tiles
     * to point to copies of the original tile, and fix the ownership
     * of those tiles.
     *
     * @return A scratch version of this colony.
     */
    public Colony copyColony() {
        final Game game = getGame();
        Tile tile = getTile();
        Tile tileCopy = tile.copy(game, tile.getClass());
        Colony colony = tileCopy.getColony();
        for (ColonyTile ct : colony.getColonyTiles()) {
            Tile wt;
            if (ct.isColonyCenterTile()) {
                wt = tileCopy;
            } else {
                wt = ct.getWorkTile();
                wt = wt.copy(game, wt.getClass());
                if (wt.getOwningSettlement() == this) {
                    wt.setOwningSettlement(colony);
                }
            }
            ct.setWorkTile(wt);
        }
        return colony;
    }

    /**
     * Finds the corresponding FreeColObject from another copy of this colony.
     *
     * @param fco The <code>FreeColObject</code> in the other colony.
     * @return The corresponding <code>FreeColObject</code> in this
     *     colony, or null if not found.
     */
    @SuppressWarnings("unchecked")
    public <T extends FreeColObject> T getCorresponding(T fco) {
        final String id = fco.getId();
        if (fco instanceof WorkLocation) {
            for (WorkLocation t : getAllWorkLocations()) {
                if (t.getId().equals(id)) return (T)t;
            }
        } else if (fco instanceof Tile) {
            if (getTile().getId().equals(id)) return (T)getTile();
            for (ColonyTile ct : getColonyTiles()) {
                if (ct.getWorkTile().getId().equals(id)) return (T)ct.getWorkTile();
            }
        } else if (fco instanceof Unit) {
            for (Unit t : getUnitList()) {
                if (t.getId().equals(id)) return (T)t;
            }
            for (Unit t : getTile().getUnitList()) {
                if (t.getId().equals(id)) return (T)t;
            }
        }
        return null;
    }


    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Ability> getAbilities(String id, FreeColGameObjectType type,
                                     Turn turn) {
        if (turn == null) turn = getGame().getTurn();
        Set<Ability> result = super.getAbilities(id, type, turn);
        // Owner abilities also apply to colonies
        if (owner != null) result.addAll(owner.getAbilities(id, type, turn));
        return result;
    }


    // Override FreeColGameObject

    /**
     * Dispose of this colony.
     *
     * @return A list of disposed objects.
     */
    @Override
    public List<FreeColGameObject> disposeList() {
        List<FreeColGameObject> objects = new ArrayList<FreeColGameObject>();
        for (WorkLocation workLocation : getAllWorkLocations()) {
            objects.addAll(workLocation.disposeList());
        }
        objects.addAll(super.disposeList());
        return objects;
    }


    // Interface Location (from Settlement via GoodsLocation
    //   via UnitLocation)
    //   The unit list in UnitLocation is replaced in Colonies.
    // Inherits
    //   FreeColObject.getId
    //   Settlement.getTile
    //   Settlement.getLocationName
    //   GoodsLocation.canAdd
    //   GoodsLocation.getGoodsContainer
    //   Settlement.getSettlement

    /**
     * {@inheritDoc}
     */
    @Override
    public StringTemplate getLocationNameFor(Player player) {
        // Everyone can always work out a colony name.
        return StringTemplate.name(getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean add(Locatable locatable) {
        if (locatable instanceof Unit) {
            return joinColony((Unit)locatable);
        }
        return super.add(locatable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean remove(Locatable locatable) {
        if (locatable instanceof Unit) {
            Location loc = ((Unit)locatable).getLocation();
            if (loc instanceof WorkLocation) {
                WorkLocation wl = (WorkLocation)loc;
                if (wl.getColony() == this) {
                    return wl.remove(locatable);
                }
            }                
            return false;
        }
        return super.remove(locatable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean contains(Locatable locatable) {
        if (locatable instanceof Unit) {
            for (WorkLocation wl : getAvailableWorkLocations()) {
                if (wl.contains(locatable)) return true;
            }
            return false;
        }
        return super.contains(locatable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getUnitCount() {
        int n = 0;
        for (WorkLocation wl : getCurrentWorkLocations()) {
            n += wl.getUnitCount();
        }
        return n;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Unit> getUnitList() {
        ArrayList<Unit> units = new ArrayList<Unit>();
        for (WorkLocation wl : getCurrentWorkLocations()) {
            units.addAll(wl.getUnitList());
        }
        return units;
    }

    /**
     * {@inheritDoc}
     */
    public String toShortString() {
        return getName();
    }


    // Interface UnitLocation
    // Inherits
    //   UnitLocation.getSpaceTaken [Irrelevant!]
    //   UnitLocation.moveToFront [Irrelevant!]
    //   UnitLocation.clearUnitList [Irrelevant!]
    //   Settlement.equipForRole
    //   Settlement.getNoAddReason


    // Interface GoodsLocation

    /**
     * {@inheritDoc}
     */
    public int getGoodsCapacity() {
        return (int)applyModifiers(0f, getGame().getTurn(),
                                   Modifier.WAREHOUSE_STORAGE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addGoods(GoodsType type, int amount) {
        super.addGoods(type, amount);
        productionCache.invalidate(type);
        modifySpecialGoods(type, amount);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Goods removeGoods(GoodsType type, int amount) {
        Goods removed = super.removeGoods(type, amount);
        productionCache.invalidate(type);
        if (removed != null) modifySpecialGoods(type, -removed.getAmount());
        return removed;
    }

    private void modifySpecialGoods(GoodsType goodsType, int amount) {
        final Turn turn = getGame().getTurn();
        Set<Modifier> mods;

        mods = goodsType.getModifiers(Modifier.LIBERTY);
        if (!mods.isEmpty()) {
            int liberty = (int)applyModifiers(amount, turn, mods);
            modifyLiberty(liberty);
        }

        mods = goodsType.getModifiers(Modifier.IMMIGRATION);
        if (!mods.isEmpty()) {
            int migration = (int)applyModifiers(amount, turn, mods);
            modifyImmigration(migration);
            getOwner().modifyImmigration(migration);
        }
    }


    // Settlement

    /**
     * {@inheritDoc}
     */
    public String getImageKey() {
        if (isUndead()) return "undead";

        int count = getDisplayUnitCount();
        String key = (count <= 3) ? "small"
            : (count <= 7) ? "medium"
            : "large";
        String stockade = getStockadeKey();
        if (stockade != null) key += "." + stockade;
        return "model.settlement." + key + ".image";
    }

    /**
     * {@inheritDoc}
     */
    public Unit getDefendingUnit(Unit attacker) {
        if (displayUnitCount > 0) {
            // There are units, but we don't see them
            return null;
        }

        // Note that this function will only return a unit working
        // inside the colony.  Typically, colonies are also defended
        // by units outside the colony on the same tile.  To consider
        // units outside the colony as well, use
        // @see Tile#getDefendingUnit instead.
        // 
        // Returns an arbitrary unarmed land unit unless Paul Revere
        // is present as founding father, in which case the unit can
        // be armed as well.
        List<Unit> unitList = getUnitList();

        Unit defender = null;
        float defencePower = -1.0f;
        for (Unit nextUnit : unitList) {
            float unitPower = getGame().getCombatModel()
                .getDefencePower(attacker, nextUnit);
            if (Unit.betterDefender(defender, defencePower,
                    nextUnit, unitPower)) {
                defender = nextUnit;
                defencePower = unitPower;
            }
        }
        if (defender == null) {
            throw new IllegalStateException("Colony " + getName()
                + " contains no units!");
        }
        return defender;
    }

    /**
     * {@inheritDoc}
     */
    public float getDefenceRatio() {
        return getTotalDefencePower() / (1 + getUnitCount());
    }

    /**
     * {@inheritDoc}
     */
    public boolean isBadlyDefended() {
        return getTotalDefencePower() < 0.95f * getUnitCount() - 2.5f;
    }

    /**
     * {@inheritDoc}
     */
    public RandomRange getPlunderRange(Unit attacker) {
        if (canBePlundered()) {
            int upper = (owner.getGold() * (getUnitCount() + 1))
                / (owner.getColoniesPopulation() + 1);
            if (upper > 0) return new RandomRange(100, 1, upper+1, 1);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public int getSoL() {
        return sonsOfLiberty;
    }

    /**
     * {@inheritDoc}
     */
    public int getUpkeep() {
        int upkeep = 0;
        for (Building building : buildingMap.values()) {
            upkeep += building.getType().getUpkeep();
        }
        return upkeep;
    }

    /**
     * {@inheritDoc}
     */
    public int getTotalProductionOf(GoodsType goodsType) {
        int amount = 0;
        for (WorkLocation workLocation : getCurrentWorkLocations()) {
            amount += workLocation.getTotalProductionOf(goodsType);
        }
        return amount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canProvideGoods(List<AbstractGoods> requiredGoods) {
        // Unlike priceGoods, this takes goods "reserved" for other
        // purposes into account.
        BuildableType buildable = getCurrentlyBuilding();
        for (AbstractGoods goods : requiredGoods) {
            int available = getGoodsCount(goods.getType());

            int breedingNumber = goods.getType().getBreedingNumber();
            if (breedingNumber != GoodsType.INFINITY) {
                available -= breedingNumber;
            }

            if (buildable != null) {
                available -= AbstractGoods.getCount(goods.getType(),
                    buildable.getRequiredGoods());
            }

            if (available < goods.getAmount()) return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public StringTemplate getAlarmLevelMessage(Player player) {
        Stance stance = getOwner().getStance(player);
        return StringTemplate.template("colony.tension." + stance.getKey())
            .addStringTemplate("%nation%", getOwner().getNationName());
    }


    //
    // Miscellaneous low level
    //

    /**
     * Check the integrity of the build queues.  Catches build fails
     * due to broken requirements.
     *
     * @param fix Fix problems if possible.
     * @return Negative if there are problems remaining, zero if
     *     problems were fixed, positive if no problems found at all.
     */
    private int checkBuildQueueIntegrity(boolean fix) {
        int result = 1;
        List<BuildableType> buildables = buildQueue.getValues();
        List<BuildableType> assumeBuilt = new ArrayList<BuildableType>();
        for (int i = 0; i < buildables.size(); i++) {
            BuildableType bt = buildables.get(i);
            NoBuildReason reason = getNoBuildReason(bt, assumeBuilt);
            if (reason == NoBuildReason.NONE) {
                assumeBuilt.add(bt);
            } else if (fix) {
                buildQueue.remove(i);
                result = Math.min(result, 0);
            } else {
                result = -1;
            }
        }
        List<UnitType> unitTypes = populationQueue.getValues();
        assumeBuilt.clear();
        for (int i = 0; i < unitTypes.size(); i++) {
            UnitType ut = unitTypes.get(i);
            NoBuildReason reason = getNoBuildReason(ut, assumeBuilt);
            if (reason == NoBuildReason.NONE) {
                assumeBuilt.add(ut);
            } else if (fix) {                
                populationQueue.remove(i);
                result = Math.min(result, 0);
            } else {
                result = -1;
            }
        }
        return result;
    }


    // Override FreeColGameObject

    /**
     * {@inheritDoc}
     */
    @Override
    public int checkIntegrity(boolean fix) {
        int result = super.checkIntegrity(fix);

        // @compat 0.10.x
        if (!isLandLocked() && !hasAbility(Ability.HAS_PORT)) {
            if (fix) {
                addAbility(new Ability(Ability.HAS_PORT));
                result = Math.min(result, 0);
            } else {
                result = -1;
            }
        }
        // end @compat 0.10.x

        return Math.min(result, checkBuildQueueIntegrity(fix));
    }


    // Serialization

    private static final String BUILD_QUEUE_TAG = "buildQueueItem";
    private static final String ESTABLISHED_TAG = "established";
    private static final String IMMIGRATION_TAG = "immigration";
    private static final String LIBERTY_TAG = "liberty";
    private static final String PRODUCTION_BONUS_TAG = "productionBonus";
    private static final String NAME_TAG = "name";
    private static final String OLD_SONS_OF_LIBERTY_TAG = "oldSonsOfLiberty";
    private static final String OLD_TORIES_TAG = "oldTories";
    private static final String POPULATION_QUEUE_TAG = "populationQueueItem";
    private static final String SONS_OF_LIBERTY_TAG = "sonsOfLiberty";
    private static final String TORIES_TAG = "tories";
    private static final String UNIT_COUNT_TAG = "unitCount";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        // Delegated from Settlement
        xw.writeAttribute(NAME_TAG, getName());

        xw.writeAttribute(ESTABLISHED_TAG, established.getNumber());

        if (xw.validFor(getOwner())) {

            xw.writeAttribute(SONS_OF_LIBERTY_TAG, sonsOfLiberty);
            
            xw.writeAttribute(OLD_SONS_OF_LIBERTY_TAG, oldSonsOfLiberty);

            xw.writeAttribute(TORIES_TAG, tories);

            xw.writeAttribute(OLD_TORIES_TAG, oldTories);

            xw.writeAttribute(LIBERTY_TAG, liberty);

            xw.writeAttribute(IMMIGRATION_TAG, immigration);

            xw.writeAttribute(PRODUCTION_BONUS_TAG, productionBonus);

        } else {

            int uc = getDisplayUnitCount();
            if (uc <= 0) {
                logger.warning("Unit count fail: " + uc + " id=" + getId()
                    + " unitCount=" + getUnitCount()
                    + " scope=" + xw.getWriteScope()
                    + " player=" + xw.getWriteScope().getClient() + "\n"
                    + net.sf.freecol.common.debug.FreeColDebugger.stackTraceToString());
            }
            xw.writeAttribute(UNIT_COUNT_TAG, uc);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        if (xw.validFor(getOwner())) {

            List<String> keys = new ArrayList<String>(exportData.keySet());
            Collections.sort(keys);
            for (String key : keys) {
                exportData.get(key).toXML(xw);
            }

            // Only write the features that need specific instantiation,
            // which is currently only those with increments.
            // Fixed features will be added from their origins (usually
            // buildings).
            Turn turn = getGame().getTurn();
            for (Modifier modifier : getSortedModifiers()) {
                if (modifier.hasIncrement() && !modifier.isOutOfDate(turn)) {
                    modifier.toXML(xw);
                }
            }

            for (WorkLocation workLocation : getSortedCopy(getAllWorkLocations())) {
                workLocation.toXML(xw);
            }

            for (BuildableType item : buildQueue.getValues()) { // In order!
                xw.writeStartElement(BUILD_QUEUE_TAG);

                xw.writeAttribute(ID_ATTRIBUTE_TAG, item);

                xw.writeEndElement();
            }

            for (BuildableType item : populationQueue.getValues()) { // In order
                xw.writeStartElement(POPULATION_QUEUE_TAG);

                xw.writeAttribute(ID_ATTRIBUTE_TAG, item);

                xw.writeEndElement();
            }

        } else {
            // Special case.  Serialize stockade-class buildings to
            // otherwise unprivileged clients as the stockade level is
            // visible to anyone who can see the colony.  This should
            // have no other information leaks because stockade
            // buildings have no production or units inside.
            Building stockade = getStockade();
            if (stockade != null) stockade.toXML(xw);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        established = new Turn(xr.getAttribute(ESTABLISHED_TAG, 0));

        sonsOfLiberty = xr.getAttribute(SONS_OF_LIBERTY_TAG, 0);

        oldSonsOfLiberty = xr.getAttribute(OLD_SONS_OF_LIBERTY_TAG, 0);

        tories = xr.getAttribute(TORIES_TAG, 0);

        oldTories = xr.getAttribute(OLD_TORIES_TAG, 0);

        liberty = xr.getAttribute(LIBERTY_TAG, 0);

        immigration = xr.getAttribute(IMMIGRATION_TAG, 0);

        productionBonus = xr.getAttribute(PRODUCTION_BONUS_TAG, 0);

        displayUnitCount = xr.getAttribute(UNIT_COUNT_TAG, -1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        // Clear containers.
        colonyTiles.clear();
        buildingMap.clear();
        exportData.clear();
        buildQueue.clear();
        populationQueue.clear();

        super.readChildren(xr);

        invalidateCache();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final Specification spec = getSpecification();
        final Game game = getGame();
        final String tag = xr.getLocalName();

        if (BUILD_QUEUE_TAG.equals(tag)) {
            BuildableType bt = xr.getType(spec, ID_ATTRIBUTE_TAG,
                BuildableType.class, (BuildableType)null);
            if (bt != null) buildQueue.add(bt);
            xr.closeTag(BUILD_QUEUE_TAG);

        } else if (POPULATION_QUEUE_TAG.equals(xr.getLocalName())) {
            UnitType ut = xr.getType(spec, ID_ATTRIBUTE_TAG,
                                     UnitType.class, (UnitType)null);
            if (ut != null) populationQueue.add(ut);
            xr.closeTag(POPULATION_QUEUE_TAG);

        } else if (Building.getXMLElementTagName().equals(tag)) {
            addBuilding(xr.readFreeColGameObject(game, Building.class));

        } else if (ColonyTile.getXMLElementTagName().equals(tag)) {
            colonyTiles.add(xr.readFreeColGameObject(game, ColonyTile.class));

        } else if (ExportData.getXMLElementTagName().equals(tag)) {
            ExportData data = new ExportData(xr);
            exportData.put(data.getId(), data);
        
        } else {
            super.readChild(xr);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getName();
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "colony".
     */
    public static String getXMLElementTagName() {
        return "colony";
    }
}
