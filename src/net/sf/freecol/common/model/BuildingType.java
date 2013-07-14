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

package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;


/**
 * Encapsulates data common to all instances of a particular kind of
 * {@link Building}, such as the number of workplaces, and the types
 * of goods it produces and consumes.
 */
public final class BuildingType extends BuildableType
    implements Comparable<BuildingType> {

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

    /** The building type this upgrades from. */
    private BuildingType upgradesFrom = null;

    /** The building type this upgrades to. */
    private BuildingType upgradesTo = null;

    /** The possible production types of this building type. */
    private final List<ProductionType> productionTypes
        = new ArrayList<ProductionType>();


    /**
     * Creates a new <code>BuildingType</code> instance.
     *
     * @param id The object identifier.
     * @param specification The <code>Specification</code> to refer to.
     */
    public BuildingType(String id, Specification specification) {
        super(id, specification);

        setModifierIndex(Modifier.BUILDING_PRODUCTION_INDEX);
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
     * Can a unit of a given type be added to a Building of this type?
     *
     * @param unitType The <code>UnitType</code> to check.
     * @return True if the unit type can be added.
     */
    public boolean canAdd(UnitType unitType) {
        return workPlaces > 0
            && unitType.hasSkill()
            && unitType.getSkill() >= minSkill
            && unitType.getSkill() <= maxSkill;
    }

    /**
     * Gets the type of the building type, which is trivially just this
     * object.
     *
     * @return This.
     */
    public FreeColGameObjectType getType() {
        return this;
    }

    /**
     * Gets the BuildingType this BuildingType upgrades from.
     *
     * @return The <code>BuildingType</code> that upgrades to this one.
     */
    public BuildingType getUpgradesFrom() {
        return upgradesFrom;
    }

    /**
     * Get the BuildingType this BuildingType upgrades to.
     *
     * @return The <code>BuildingType</code> to upgrade to from this one.
     */
    public BuildingType getUpgradesTo() {
        return upgradesTo;
    }

    /**
     * Gets the first level of this BuildingType.
     *
     * @return The base <code>BuildingType</code>.
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
     * Get the <code>ProductionTypes</code> value.
     *
     * @return a <code>List<ProductionType></code> value
     */
    public List<ProductionType> getProductionTypes() {
        return productionTypes;
    }

    /**
     * Return the production types available for the given production
     * level. If the production level is null, all production levels
     * will be returned.
     *
     * @param level the production level
     * @return a <code>List<ProductionType></code> value
     */
    public List<ProductionType> getProductionTypes(String level) {
        List<ProductionType> result = new ArrayList<ProductionType>();
        for (ProductionType productionType : productionTypes) {
            if (level == null || level.equals(productionType.getProductionLevel())) {
                result.add(productionType);
            }
        }
        return result;
    }

    // @compat 0.10.6
    /**
     * Get the type of goods consumed by this BuildingType.
     *
     * @return The consumed <code>GoodsType</code>.
     */
    private GoodsType getConsumedGoodsType() {
        if (productionTypes == null || productionTypes.isEmpty()) {
            return null;
        } else {
            List<AbstractGoods> inputs = productionTypes.get(0).getInputs();
            if (inputs == null || inputs.isEmpty()) {
                return null;
            } else {
                return inputs.get(0).getType();
            }
        }
    }
    // end @compat

    /**
     * Gets the type of goods produced by this BuildingType.
     *
     * @return The produced <code>GoodsType</code>.
     */
    public GoodsType getProducedGoodsType() {
        if (productionTypes == null || productionTypes.isEmpty()) {
            return null;
        } else {
            List<AbstractGoods> outputs = productionTypes.get(0).getOutputs();
            if (outputs == null || outputs.isEmpty()) {
                return null;
            } else {
                return outputs.get(0).getType();
            }
        }
    }

    /**
     * Get the index for the given Modifier.
     *
     * @param modifier The <code>Modifier</code> to check.
     * @return A modifier index.
     */
    @Override
    protected final int getModifierIndex(Modifier modifier) {
        GoodsType produces = getProducedGoodsType();
        if (produces != null && produces.getId().equals(modifier.getId())) {
            return Modifier.AUTO_PRODUCTION_INDEX;
        } else {
            return getModifierIndex();
        }
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


    // Interface Comparable

    /**
     * Compares this BuildingType to another.  BuildingTypes are
     * simply sorted according to the order in which they are defined
     * in the specification.
     *
     * @param other The other <code>BuildingType</code> to compare with.
     * @return A comparison result.
     */
    public int compareTo(BuildingType other) {
        return getIndex() - other.getIndex();
    }


    // Serialization

    private static final String BASIC_PRODUCTION_TAG = "basicProduction";
    private static final String CONSUMES_TAG = "consumes";
    private static final String MAX_SKILL_TAG = "maxSkill";
    private static final String MIN_SKILL_TAG = "minSkill";
    private static final String PRIORITY_TAG = "priority";
    private static final String PRODUCES_TAG = "produces";
    private static final String PRODUCTION_TAG = "production";
    private static final String UPGRADES_FROM_TAG = "upgradesFrom";
    private static final String UPKEEP_TAG = "upkeep";
    private static final String WORKPLACES_TAG = "workplaces";


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
            xw.writeAttribute(MIN_SKILL_TAG, minSkill);
        }

        if (maxSkill < INFINITY) {
            xw.writeAttribute(MAX_SKILL_TAG, maxSkill);
        }

        if (upkeep > 0) {
            xw.writeAttribute(UPKEEP_TAG, upkeep);
        }

        if (priority != Consumer.BUILDING_PRIORITY) {
            xw.writeAttribute(PRIORITY_TAG, priority);
        }

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

        upgradesFrom = xr.getType(spec, UPGRADES_FROM_TAG,
            BuildingType.class, (BuildingType)null);
        if (upgradesFrom == null) {
            level = 1;
        } else {
            upgradesFrom.upgradesTo = this;
            level = upgradesFrom.level + 1;
        }

        workPlaces = xr.getAttribute(WORKPLACES_TAG, parent.workPlaces);

        minSkill = xr.getAttribute(MIN_SKILL_TAG, parent.minSkill);

        maxSkill = xr.getAttribute(MAX_SKILL_TAG, parent.maxSkill);

        upkeep = xr.getAttribute(UPKEEP_TAG, parent.upkeep);

        priority = xr.getAttribute(PRIORITY_TAG, parent.priority);

        // @compat 0.10.6
        int basicProduction = xr.getAttribute(BASIC_PRODUCTION_TAG, -1);
        if (basicProduction > 0) {
            GoodsType consumes = xr.getType(spec, CONSUMES_TAG, GoodsType.class,
                parent.getConsumedGoodsType());
            GoodsType produces = xr.getType(spec, PRODUCES_TAG, GoodsType.class,
                parent.getProducedGoodsType());
            productionTypes.add(new ProductionType(consumes, produces,
                    basicProduction));
        }
        // end @compat

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
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {

        super.readChildren(xr);

        // @compat 0.10.x
        // Town Hall and Chapel* unattended production modifiers
        // require a scope.
        if (getId().equals("model.building.townHall")
            || getId().equals("model.building.chapel")
            || getId().equals("model.building.church")
            || getId().equals("model.building.cathedral")) {
            for (Modifier m : getModifiers()) {
                if (m.getType() == Modifier.Type.ADDITIVE && !m.hasScope()) {
                    Scope scope = new Scope();
                    scope.setMatchNegated(true);
                    scope.setAbilityId("model.ability.person");
                    m.addScope(scope);
                    break;
                }
            }
        }
        // end @compat
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final Specification spec = getSpecification();
        final String tag = xr.getLocalName();

        if (PRODUCTION_TAG.equals(tag)) {
            productionTypes.add(new ProductionType(xr, spec));

        } else {
            super.readChild(xr);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "building-type".
     */
    public static String getXMLElementTagName() {
        return "building-type";
    }
}
