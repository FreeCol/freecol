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

package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


/**
 * Contains information on building types, like the number of upgrade levels a
 * given building type can have. The levels contain the information about the
 * name of the building in a given level and what is needed to build it.
 */
public final class BuildingType extends BuildableType implements Comparable<BuildingType> {

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


    public BuildingType(String id, Specification specification) {
        super(id, specification);
        setModifierIndex(Modifier.BUILDING_PRODUCTION_INDEX);
    }


    public BuildingType getUpgradesFrom() {
        return upgradesFrom;
    }

    public BuildingType getUpgradesTo() {
        return upgradesTo;
    }

    public BuildingType getFirstLevel() {
        BuildingType buildingType = this;
        while (buildingType.getUpgradesFrom() != null) {
            buildingType = buildingType.getUpgradesFrom();
        }
        return buildingType;
    }

    public int getWorkPlaces() {
        return workPlaces;
    }

    public int getBasicProduction() {
        return basicProduction;
    }

    public GoodsType getConsumedGoodsType() {
        return consumes;
    }

    public GoodsType getProducedGoodsType() {
        return produces;
    }

    public int getLevel() {
        return level;
    }

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


    public FreeColGameObjectType getType() {
        return this;
    }

    public Modifier getProductionModifier() {
        return productionModifier;
    }

    public int compareTo(BuildingType other) {
        return getIndex() - other.getIndex();
    }

    public boolean canAdd(UnitType unitType) {
        return workPlaces > 0
            && unitType.hasSkill()
            && unitType.getSkill() >= minSkill
            && unitType.getSkill() <= maxSkill;
    }

    /**
     * Is this building type automatically built in any colony?
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


    public void readAttributes(XMLStreamReader in) throws XMLStreamException {
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

    public void readChildren(XMLStreamReader in) throws XMLStreamException {
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            readChild(in);
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

    public void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
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
     * 0.9.x compatibility hack, called from the specification reader
     * when it is finishing up.
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
