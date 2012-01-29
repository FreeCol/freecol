/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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


import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


/**
 * Encapsulates data common to all instances of a particular kind of
 * {@link Building}, such as the number of workplaces, and the types
 * of goods it produces and consumes.
 */
public final class BuildingType extends BuildableType
    implements Comparable<BuildingType> {

    private int level = 1;
    private int workPlaces = 3;
    private int basicProduction = 3;
    private int minSkill = UNDEFINED;
    private int maxSkill = INFINITY;
    private int upkeep = 0;
    private int priority = Consumer.BUILDING_PRIORITY;

    private GoodsType consumes, produces;
    private Modifier productionModifier = null;
    private BuildingType upgradesFrom;
    private BuildingType upgradesTo;


    /**
     * Creates a new <code>BuildingType</code> instance.
     *
     * @param id a <code>String</code> value
     * @param specification a <code>Specification</code> value
     */
    public BuildingType(String id, Specification specification) {
        super(id, specification);
        setModifierIndex(Modifier.BUILDING_PRODUCTION_INDEX);
    }


    /**
     * Returns the BuildingType this BuildingType upgrades from.
     *
     * @return a <code>BuildingType</code> value
     */
    public BuildingType getUpgradesFrom() {
        return upgradesFrom;
    }

    /**
     * Returns the BuildingType this BuildingType upgrades to.
     *
     * @return a <code>BuildingType</code> value
     */
    public BuildingType getUpgradesTo() {
        return upgradesTo;
    }

    /**
     * Returns the first level of this BuildingType.
     *
     * @return a <code>BuildingType</code> value
     */
    public BuildingType getFirstLevel() {
        BuildingType buildingType = this;
        while (buildingType.getUpgradesFrom() != null) {
            buildingType = buildingType.getUpgradesFrom();
        }
        return buildingType;
    }

    /**
     * Returns the number of workplaces, that is the maximum number of
     * Units that can work in this BuildingType.
     *
     * @return an <code>int</code> value
     */
    public int getWorkPlaces() {
        return workPlaces;
    }

    /**
     * Returns the production of a single Unit in this BuildingType
     * before any modifiers are applied.
     *
     * @return an <code>int</code> value
     */
    public int getBasicProduction() {
        return basicProduction;
    }

    /**
     * Returns the type of goods consumed by this BuildingType.
     *
     * @return an <code>GoodsType</code> value
     */
    public GoodsType getConsumedGoodsType() {
        return consumes;
    }

    /**
     * Returns the type of goods produced by this BuildingType.
     *
     * @return an <code>GoodsType</code> value
     */
    public GoodsType getProducedGoodsType() {
        return produces;
    }

    /**
     * Returns the level of this BuildingType.
     *
     * @return an <code>int</code> value
     */
    public int getLevel() {
        return level;
    }

    /**
     * Returns the amount of gold necessary to maintain a Building of
     * this type for one turn.
     *
     * @return an <code>int</code> value
     */
    public int getUpkeep() {
        return upkeep;
    }

    /**
     * The consumption priority of a Building of this type. The higher
     * the priority, the earlier will the Consumer be allowed to
     * consume the goods it requires.
     *
     * @return an <code>int</code> value
     */
    public int getPriority() {
        return priority;
    }


    /**
     * Describe <code>getType</code> method here.
     *
     * @return a <code>FreeColGameObjectType</code> value
     */
    public FreeColGameObjectType getType() {
        return this;
    }

    /**
     * Describe <code>getProductionModifier</code> method here.
     *
     * @return a <code>Modifier</code> value
     */
    public Modifier getProductionModifier() {
        return productionModifier;
    }

    /**
     * Compares this BuildingType to another. BuildingTypes are sorted
     * according to the order in which they are defined in the
     * specification.
     *
     * @param other a <code>BuildingType</code> value
     * @return an <code>int</code> value
     */
    public int compareTo(BuildingType other) {
        return getIndex() - other.getIndex();
    }

    /**
     * Returns true if the given UnitType could be added to a Building
     * of this type.
     *
     * @param unitType an <code>UnitType</code> value
     * @return a <code>boolean</code> value
     */
    public boolean canAdd(UnitType unitType) {
        return workPlaces > 0
            && unitType.hasSkill()
            && unitType.getSkill() >= minSkill
            && unitType.getSkill() <= maxSkill;
    }

    /**
     * Is this building type automatically built in any colony?
     *
     * @return a <code>boolean</code> value
     */
    public boolean isAutomaticBuild() {
        return !needsGoodsToBuild() && getUpgradesFrom() == null;
    }

    /**
     * Get the index for the given Modifier.
     *
     * @param modifier a <code>Modifier</code> value
     * @return an <code>int</code> value
     */
    @Override
    public final int getModifierIndex(Modifier modifier) {
        if (produces != null && produces.getId().equals(modifier.getId())) {
            return Modifier.AUTO_PRODUCTION_INDEX;
        } else {
            return getModifierIndex();
        }
    }


    /**
     * Makes an XML-representation of this object.
     *
     * @param out The output stream.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    public void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        super.toXML(out, getXMLElementTagName());
    }

    /**
     * Write the attributes of this object to a stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing to
     *     the stream.
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out)
        throws XMLStreamException {
        super.writeAttributes(out);

        if (upgradesFrom != null) {
            out.writeAttribute("upgradesFrom", upgradesFrom.getId());
        }
        out.writeAttribute("workplaces", Integer.toString(workPlaces));
        out.writeAttribute("basicProduction", Integer.toString(basicProduction));
        if (minSkill > UNDEFINED) {
            out.writeAttribute("minSkill", Integer.toString(minSkill));
        }
        if (maxSkill < INFINITY) {
            out.writeAttribute("maxSkill", Integer.toString(maxSkill));
        }
        if (upkeep > 0) {
            out.writeAttribute("upkeep", Integer.toString(upkeep));
        }
        if (priority != Consumer.BUILDING_PRIORITY) {
            out.writeAttribute("priority", Integer.toString(priority));
        }
        if (consumes != null) {
            out.writeAttribute("consumes", consumes.getId());
        }
        if (produces != null) {
            out.writeAttribute("produces", produces.getId());
        }
    }

    /**
     * Reads the attributes of this object from an XML stream.
     *
     * @param in The XML input stream.
     * @throws XMLStreamException if a problem was encountered
     *     during parsing.
     */
    @Override
    protected void readAttributes(XMLStreamReader in)
        throws XMLStreamException {
        super.readAttributes(in);

        String extendString = in.getAttributeValue(null, "extends");
        BuildingType parent = (extendString == null) ? this :
            getSpecification().getBuildingType(extendString);
        String upgradeString = in.getAttributeValue(null, "upgradesFrom");
        if (upgradeString == null) {
            level = 1;
        } else {
            upgradesFrom = getSpecification().getBuildingType(upgradeString);
            upgradesFrom.upgradesTo = this;
            level = upgradesFrom.level + 1;
        }
        setPopulationRequired(getAttribute(in, "required-population", parent.getPopulationRequired()));

        workPlaces = getAttribute(in, "workplaces", parent.workPlaces);
        basicProduction = getAttribute(in, "basicProduction", parent.basicProduction);

        consumes = getSpecification().getType(in, "consumes", GoodsType.class, parent.consumes);
        produces = getSpecification().getType(in, "produces", GoodsType.class, parent.produces);

        if (produces != null && basicProduction > 0) {
            productionModifier = new Modifier(produces.getId(), this, basicProduction,
                                              Modifier.Type.ADDITIVE);
        }

        minSkill = getAttribute(in, "minSkill", parent.minSkill);
        maxSkill = getAttribute(in, "maxSkill", parent.maxSkill);

        priority = getAttribute(in, "priority", parent.priority);
        upkeep = getAttribute(in, "upkeep", parent.upkeep);

        if (parent != this) {
            getFeatureContainer().add(parent.getFeatureContainer());
            if (parent.isAbstractType()) {
                getFeatureContainer().replaceSource(parent, this);
            }
        }
    }

    /**
     * Reads the children of this object from an XML stream.
     *
     * @param in The XML input stream.
     * @throws XMLStreamException if a problem was encountered
     *     during parsing.
     */
    @Override
    protected void readChildren(XMLStreamReader in) throws XMLStreamException {
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            readChild(in);
        }
    }

    /**
     * Compatibility hack, called from the specification reader when
     * it is finishing up.
     * @compat 0.9.x
     */
    public void fixup09x() {
        try {
            if (hasAbility(Ability.AUTO_PRODUCTION)) {
                if (!hasAbility(Ability.AVOID_EXCESS_PRODUCTION)) {
                    // old-style auto-production
                    Ability ability = new Ability(Ability.AVOID_EXCESS_PRODUCTION);
                    addAbility(ability);
                    getFeatureContainer().removeModifiers("model.goods.horses");
                    float value = ("model.building.country".equals(getId()))
                        ? 50 : 25;
                    Modifier modifier = new Modifier("model.modifier.breedingDivisor",
                                                     this, value,
                                                     Modifier.Type.ADDITIVE);
                    addModifier(modifier);
                    getSpecification().addModifier(modifier);
                    modifier = new Modifier("model.modifier.breedingFactor",
                                            this, 2, Modifier.Type.ADDITIVE);
                    addModifier(modifier);
                    getSpecification().addModifier(modifier);
                }
                if (getModifierSet("model.modifier.consumeOnlySurplusProduction").isEmpty()) {
                    Modifier modifier = new Modifier("model.modifier.consumeOnlySurplusProduction",
                                                     this, 0.5f, Modifier.Type.MULTIPLICATIVE);
                    addModifier(modifier);
                    getSpecification().addModifier(modifier);
                }
            }
        } catch(Exception e) {
            // no such ability, we don't care
        }
    }

    /**
     * Gets the tag name of the root element representing this object.
     * This method should be overwritten by any sub-class, preferably
     * with the name of the class with the first letter in lower case.
     *
     * @return "building-type".
     */
    public static String getXMLElementTagName() {
        return "building-type";
    }
}
