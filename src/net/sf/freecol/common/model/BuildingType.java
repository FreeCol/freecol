/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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
import java.util.List;

import javax.swing.JList;
import javax.swing.ListModel;
import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Colony.NoBuildReason;
import static net.sf.freecol.common.model.Constants.*;
import net.sf.freecol.common.model.UnitLocation.NoAddReason;
import net.sf.freecol.common.option.GameOptions;
import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * Encapsulates data common to all instances of a particular kind of
 * {@link Building}, such as the number of workplaces, and the types
 * of goods it produces and consumes.
 */
public final class BuildingType extends BuildableType
        implements BaseProduction {

    public static final String TAG = "building-type";

    /** The level of building. */
    private int level = 1;

    /** The number of work places a unit can work in buildings of this type. */
    private int workPlaces = 3;

    /** The minimum unit skill to work in buildings of this type. */
    private int minSkill = UNDEFINED;
    /** The maximum unit skill to work in buildings of this type. */
    private int maxSkill = INFINITY;

    /** Upkeep per turn for buildings ot this type. */
    private int upkeep = 0;

    /** Consumption order. */
    private int priority = Consumer.BUILDING_PRIORITY;

    /** Maximum production from the "experts have connections" option. */
    private int expertConnectionProduction = 0;

    /**
     * A multiplier for any unit goods-specific bonus for this building type.
     */
    private float competenceFactor = 1.0f;

    /** The multiplier for the colony rebel bonus for this building type. */
    private float rebelFactor = 1.0f;

    /** The building type this upgrades from. */
    private BuildingType upgradesFrom = null;

    /** The building type this upgrades to. */
    private BuildingType upgradesTo = null;

    /** The possible production types of this building type. */
    private final List<ProductionType> productionTypes = new ArrayList<>();


    /**
     * Creates a new {@code BuildingType} instance.
     *
     * @param id The object identifier.
     * @param specification The {@code Specification} to refer to.
     */
    public BuildingType(String id, Specification specification) {
        super(id, specification);
    }


    /**
     * Get the level of this BuildingType.
     *
     * @return The building level.
     */
    public int getLevel() {
        return level;
    }

    /**
     * Gets the number of workplaces, that is the maximum number of
     * Units that can work in this BuildingType.
     *
     * @return The number of work places.
     */
    public int getWorkPlaces() {
        return workPlaces;
    }

    public int getMinimumSkill() {
        return this.minSkill;
    }
    public int getMaximumSkill() {
        return this.maxSkill;
    }

    /**
     * Gets the amount of gold necessary to maintain a Building of
     * this type for one turn.
     *
     * @return The per turn upkeep for this building type.
     */
    public int getUpkeep() {
        return upkeep;
    }

    /**
     * Get the maximum production for the Experts-With-Connections option.
     *
     * @return The production amount.
     */
    public int getExpertConnectionProduction() {
        return this.expertConnectionProduction;
    }

    /**
     * Get a work location specific factor to multiply any unit goods specific
     * bonuses by.
     *
     * @return The competence factor.
     */
    public float getCompetenceFactor() {
        return this.competenceFactor;
    }

    /**
     * Get a work location specific factor to multiply the colony
     * rebel bonus by.
     *
     * @return The rebel factor.
     */
    public float getRebelFactor() {
        return this.rebelFactor;
    }

    /**
     * The consumption priority of a Building of this type. The higher
     * the priority, the earlier will the Consumer be allowed to
     * consume the goods it requires.
     *
     * @return The consumption priority.
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Gets the reason why a given unit type can not be added to a
     * building of this type.
     *
     * @param unitType The {@code UnitType} to test.
     * @return The reason why adding would fail.
     */
    public NoAddReason getNoAddReason(UnitType unitType) {
        return (workPlaces == 0) ? NoAddReason.CAPACITY_EXCEEDED
                : (!unitType.hasSkill()) ? NoAddReason.MISSING_SKILL
                : (unitType.getSkill() < minSkill) ? NoAddReason.MINIMUM_SKILL
                : (unitType.getSkill() > maxSkill) ? NoAddReason.MAXIMUM_SKILL
                : NoAddReason.NONE;
    }

    /**
     * Can a unit of a given type be added to a Building of this type?
     *
     * @param unitType The {@code UnitType} to check.
     * @return True if the unit type can be added.
     */
    public boolean canAdd(UnitType unitType) {
        return getNoAddReason(unitType) == NoAddReason.NONE;
    }

    /**
     * Gets the type of the building type, which is trivially just this
     * object.
     *
     * @return This.
     */
    public FreeColSpecObjectType getType() {
        return this;
    }

    /**
     * Gets the BuildingType this BuildingType upgrades from.
     *
     * @return The {@code BuildingType} that upgrades to this one.
     */
    public BuildingType getUpgradesFrom() {
        return upgradesFrom;
    }

    /**
     * Get the BuildingType this BuildingType upgrades to.
     *
     * @return The {@code BuildingType} to upgrade to from this one.
     */
    public BuildingType getUpgradesTo() {
        return upgradesTo;
    }

    /**
     * Gets the first level of this BuildingType.
     *
     * @return The base {@code BuildingType}.
     */
    public BuildingType getFirstLevel() {
        BuildingType buildingType = this;
        while (buildingType.getUpgradesFrom() != null) {
            buildingType = buildingType.getUpgradesFrom();
        }
        return buildingType;
    }

    /**
     * Is this building type automatically built in any colony?
     *
     * @return True if this building type is automatically built.
     */
    public boolean isAutomaticBuild() {
        return !needsGoodsToBuild() && getUpgradesFrom() == null;
    }

    /**
     * Get the production type list.
     *
     * @return The {@code ProductionType} list.
     */
    protected List<ProductionType> getProductionTypes() {
        return this.productionTypes;
    }
    
    /**
     * Set the production type list.
     *
     * @param productionTypes The new {@code ProductionType} list.
     */
    protected void setProductionTypes(List<ProductionType> productionTypes) {
        this.productionTypes.clear();
        this.productionTypes.addAll(productionTypes);
    }

    /**
     * Add a production type to this building type.
     *
     * @param productionType The {@code ProductionType} to add.
     */
    public void addProductionType(ProductionType productionType) {
        if (productionType != null) productionTypes.add(productionType);
    }

    /**
     * Get the production types provided by this building type at the
     * current difficulty level.
     *
     * @param unattended Whether the production is unattended.
     * @return A list of {@code ProductionType}s.
     */
    public List<ProductionType> getAvailableProductionTypes(boolean unattended) {
        return getAvailableProductionTypes(unattended, null);
    }

    /**
     * Gets the production types available at the current difficulty
     * level.
     *
     * FIXME: TileType.getAvailableProductionTypes(boolean) uses the
     * GameOptions.TILE_PRODUCTION option.  Should we implement a
     * corresponding one for BuildingTypes?
     *
     * @param unattended Whether the production is unattended.
     * @param level The production level (NYI).
     * @return A list of {@code ProductionType}s.
     */
    public List<ProductionType> getAvailableProductionTypes(boolean unattended,
                                                            String level) {
        return transform(productionTypes,
                pt -> pt.getUnattended() == unattended
                        && pt.appliesTo(level));
    }

    /**
     * Gets the type of goods produced by this BuildingType.
     *
     * @return The produced {@code GoodsType}.
     */
    public GoodsType getProducedGoodsType() {
        if (productionTypes.isEmpty()) return null;
        AbstractGoods ag = first(first(productionTypes).getOutputs());
        return (ag == null) ? null : ag.getType();
    }

    /**
     * Is this a defence-related building type?  Such buildings
     * (stockade et al) are visible to other players.
     *
     * @return True if this is a defence related building.
     */
    public boolean isDefenceType() {
        return containsModifierKey(Modifier.DEFENCE);
    }

    /**
     * Can a tile of this type produce a given goods type?
     *
     * @param goodsType The {@code GoodsType} to produce.
     * @param unitType An optional {@code UnitType} that is to do
     *     the work, if null the unattended production is considered.
     * @return True if this tile type produces the goods.
     */
    public boolean canProduce(GoodsType goodsType, UnitType unitType) {
        return goodsType != null
                && ProductionType.canProduce(goodsType,
                getAvailableProductionTypes(unitType == null));
    }

    /**
     * Get the amount of goods of a given goods type the given unit
     * type could produce on a tile of this tile type.
     *
     * @param goodsType The {@code GoodsType} to produce.
     * @param unitType An optional {@code UnitType} that is to do
     *     the work, if null the unattended production is considered.
     * @return The amount of goods produced.
     */
    public int getPotentialProduction(GoodsType goodsType,
                                      UnitType unitType) {
        if (goodsType == null) return 0;
        int amount = getBaseProduction(null, goodsType, unitType);
        amount = (int)apply(amount, null, goodsType.getId(), unitType);
        return (amount < 0) ? 0 : amount;
    }

    /**
     * {@inheritDoc}
     */
    public NoBuildReason canBeBuiltInColony(Colony colony,
        List<BuildableType> assumeBuilt) {
        Building colonyBuilding = colony.getBuilding(this);
        if (colonyBuilding == null) {
            // the colony has no similar building yet
            BuildingType from = this.getUpgradesFrom();
            if (from != null && !assumeBuilt.contains(from)) {
                // we are trying to build an advanced factory, we
                // should build lower level shop first
                return Colony.NoBuildReason.WRONG_UPGRADE;
            }
        } else {
            // a building of the same family already exists
            BuildingType from = colonyBuilding.getType().getUpgradesTo();
            if (from != this && !assumeBuilt.contains(from)) {
                // the existing building's next upgrade is not the
                // new one we want to build
                return Colony.NoBuildReason.WRONG_UPGRADE;
            }
        }
        return Colony.NoBuildReason.NONE;
    }

    @Override
    public int getMinimumIndex(Colony colony, JList<BuildableType> buildQueueList, int UNABLE_TO_BUILD) {
        ListModel<BuildableType> buildQueue = buildQueueList.getModel();
        BuildingType upgradesFrom = this.getUpgradesFrom();
        if (upgradesFrom == null) return 0;
        Building building = colony.getBuilding(this);
        BuildingType buildingType = (building == null) ? null
                : building.getType();
        if (buildingType == upgradesFrom) return 0;
        for (int index = 0; index < buildQueue.getSize(); index++) {
            if (upgradesFrom.equals(buildQueue.getElementAt(index))) {
                return index + 1;
            }
        }
        return UNABLE_TO_BUILD;
    }

    @Override
    public int getMaximumIndex(Colony colony, JList<BuildableType> buildQueueList, int UNABLE_TO_BUILD) {
        ListModel<BuildableType> buildQueue = buildQueueList.getModel();
        final int buildQueueLastPos = buildQueue.getSize();

        boolean canBuild = false;
        if (colony.canBuild(this)) {
            canBuild = true;
        }

        BuildingType upgradesFrom = this.getUpgradesFrom();
        BuildingType upgradesTo = this.getUpgradesTo();
        // does not depend on nothing, but still cannot be built
        if (!canBuild && upgradesFrom == null) {
            return UNABLE_TO_BUILD;
        }

        // if can be built and does not have any upgrade,
        // then it can be built at any time
        if (canBuild && upgradesTo == null) {
            return buildQueueLastPos;
        }

        // if can be built, does not depend on anything, mark
        // upgrades from as found
        boolean foundUpgradesFrom = canBuild;
        for (int index = 0; index < buildQueue.getSize(); index++) {
            BuildableType toBuild = buildQueue.getElementAt(index);

            if (toBuild == this) continue;

            if (!canBuild && !foundUpgradesFrom
                    && upgradesFrom.equals(toBuild)) {
                foundUpgradesFrom = true;
                // nothing else to upgrade this building to
                if (upgradesTo == null) return buildQueueLastPos;
            }
            // found a building it upgrades to, cannot go to or
            // beyond this position
            if (foundUpgradesFrom && upgradesTo != null
                    && upgradesTo.equals(toBuild)) return index;

            // Don't go past a unit this building can build.
            if (this.hasAbility(Ability.BUILD, toBuild)) {
                return index;
            }
        }
        return buildQueueLastPos;
    }


    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(FreeColObject other) {
        int cmp = 0;
        if (other instanceof BuildingType) {
            BuildingType bt = (BuildingType)other;
            // BuildingTypes are simply sorted according to the order in
            // which they are defined in the specification.
            cmp = getIndex() - bt.getIndex();
        }
        if (cmp == 0) cmp = super.compareTo(other);
        return cmp;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends FreeColObject> boolean copyIn(T other) {
        BuildingType o = copyInCast(other, BuildingType.class);
        if (o == null || !super.copyIn(o)) return false;
        this.level = o.getLevel();
        this.workPlaces = o.getWorkPlaces();
        this.minSkill = o.getMinimumSkill();
        this.maxSkill = o.getMaximumSkill();
        this.upkeep = o.getUpkeep();
        this.priority = o.getPriority();
        this.expertConnectionProduction = o.getExpertConnectionProduction();
        this.competenceFactor = o.getCompetenceFactor();
        this.rebelFactor = o.getRebelFactor();
        this.upgradesFrom = o.getUpgradesFrom();
        this.upgradesTo = o.getUpgradesTo();
        this.setProductionTypes(o.getProductionTypes());        
        return true;
    }


    // Serialization

    private static final String COMPETENCE_FACTOR_TAG = "competence-factor";
    private static final String EXPERTS_WITH_CONNECTION_PRODUCTION_TAG
            = "experts-with-connections-production";
    private static final String MAXIMUM_SKILL_TAG = "maximum-skill";
    private static final String MINIMUM_SKILL_TAG = "minimum-skill";
    private static final String PRIORITY_TAG = "priority";
    private static final String PRODUCTION_TAG = "production";
    private static final String REBEL_FACTOR_TAG = "rebel-factor";
    private static final String UPGRADES_FROM_TAG = "upgrades-from";
    private static final String UPKEEP_TAG = "upkeep";
    private static final String WORKPLACES_TAG = "workplaces";
    // @compat 0.11.3
    private static final String OLD_MAX_SKILL_TAG = "maxSkill";
    private static final String OLD_MIN_SKILL_TAG = "minSkill";
    private static final String OLD_UPGRADES_FROM_TAG = "upgradesFrom";
    // end @compat 0.11.3


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        if (upgradesFrom != null) {
            xw.writeAttribute(UPGRADES_FROM_TAG, upgradesFrom);
        }

        xw.writeAttribute(WORKPLACES_TAG, workPlaces);

        if (minSkill != UNDEFINED) {
            xw.writeAttribute(MINIMUM_SKILL_TAG, minSkill);
        }

        if (maxSkill < INFINITY) {
            xw.writeAttribute(MAXIMUM_SKILL_TAG, maxSkill);
        }

        if (upkeep > 0) {
            xw.writeAttribute(UPKEEP_TAG, upkeep);
        }

        if (priority != Consumer.BUILDING_PRIORITY) {
            xw.writeAttribute(PRIORITY_TAG, priority);
        }

        xw.writeAttribute(EXPERTS_WITH_CONNECTION_PRODUCTION_TAG,
                this.expertConnectionProduction);

        xw.writeAttribute(COMPETENCE_FACTOR_TAG, this.competenceFactor);

        xw.writeAttribute(REBEL_FACTOR_TAG, this.rebelFactor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        for (ProductionType productionType : productionTypes) {
            productionType.toXML(xw);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        final Specification spec = getSpecification();

        BuildingType parent = xr.getType(spec, EXTENDS_TAG,
                BuildingType.class, this);

        // @compat 0.11.3
        if (xr.hasAttribute(OLD_UPGRADES_FROM_TAG)) {
            upgradesFrom = xr.getType(spec, OLD_UPGRADES_FROM_TAG,
                    BuildingType.class, (BuildingType)null);
        } else
        // end @compat 0.11.3
            upgradesFrom = xr.getType(spec, UPGRADES_FROM_TAG,
                                      BuildingType.class, (BuildingType)null);
        if (upgradesFrom == null) {
            level = 1;
        } else {
            upgradesFrom.upgradesTo = this;
            level = upgradesFrom.level + 1;
        }

        workPlaces = xr.getAttribute(WORKPLACES_TAG, parent.workPlaces);

        // @compat 0.11.3
        if (xr.hasAttribute(OLD_MIN_SKILL_TAG)) {
            minSkill = xr.getAttribute(OLD_MIN_SKILL_TAG, parent.minSkill);
        } else
        // end @compat 0.11.3
            minSkill = xr.getAttribute(MINIMUM_SKILL_TAG, parent.minSkill);

        // @compat 0.11.3
        if (xr.hasAttribute(OLD_MAX_SKILL_TAG)) {
            maxSkill = xr.getAttribute(OLD_MAX_SKILL_TAG, parent.maxSkill);
        } else
        // end @compat 0.11.3
            maxSkill = xr.getAttribute(MAXIMUM_SKILL_TAG, parent.maxSkill);

        upkeep = xr.getAttribute(UPKEEP_TAG, parent.upkeep);

        priority = xr.getAttribute(PRIORITY_TAG, parent.priority);

        this.expertConnectionProduction
                = xr.getAttribute(EXPERTS_WITH_CONNECTION_PRODUCTION_TAG,
                parent.expertConnectionProduction);

        this.competenceFactor = xr.getAttribute(COMPETENCE_FACTOR_TAG,
                parent.competenceFactor);

        this.rebelFactor = xr.getAttribute(REBEL_FACTOR_TAG,
                parent.rebelFactor);

        if (parent != this) { // Handle "extends" for super-type fields
            if (!xr.hasAttribute(REQUIRED_POPULATION_TAG)) {
                setRequiredPopulation(parent.getRequiredPopulation());
            }

            addFeatures(parent);
            if (parent.isAbstractType()) {
                getFeatureContainer().replaceSource(parent, this);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final Specification spec = getSpecification();
        final String tag = xr.getLocalName();

        if (PRODUCTION_TAG.equals(tag)) {
            if (xr.getAttribute(DELETE_TAG, false)) {
                productionTypes.clear();
                xr.closeTag(PRODUCTION_TAG);

            } else {
                addProductionType(new ProductionType(xr, spec));
            }

        } else {
            super.readChild(xr);
        }

        // @compat 0.11.6
        if (this.expertConnectionProduction == 0
            && hasAbility(Ability.EXPERTS_USE_CONNECTIONS)) {
            this.expertConnectionProduction = 4;
        }
        // end @compat 0.11.6
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return TAG; }
}
