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

package net.sf.freecol.common.model;


import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


/**
 * Contains information on building types, like the number of upgrade levels a
 * given building type can have. The levels contain the information about the
 * name of the building in a given level and what is needed to build it.
 */
public final class BuildingType extends BuildableType implements Comparable<BuildingType> {
    
    private static int nextIndex = 0;

    private int level = 1;
    private int workPlaces = 3;
    private int basicProduction = 3;
    private int minSkill = Integer.MIN_VALUE;
    private int maxSkill = Integer.MAX_VALUE;
    private int sequence = 0;

    private GoodsType consumes, produces;
    private Modifier productionModifier = null;
    private BuildingType upgradesFrom;
    private BuildingType upgradesTo;
    
    public BuildingType() {
        setIndex(nextIndex++);
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

    public int getSequence() {
        return sequence;
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


    public void readAttributes(XMLStreamReader in, Specification specification) throws XMLStreamException {
        String extendString = in.getAttributeValue(null, "extends");
        BuildingType parent = (extendString == null) ? this :
            specification.getBuildingType(extendString);
        String upgradeString = in.getAttributeValue(null, "upgradesFrom");
        if (upgradeString == null) {
            level = 1;
        } else {
            upgradesFrom = specification.getBuildingType(upgradeString);
            upgradesFrom.upgradesTo = this;
            level = upgradesFrom.level + 1;
        }
        setPopulationRequired(getAttribute(in, "required-population", parent.getPopulationRequired()));

        workPlaces = getAttribute(in, "workplaces", parent.workPlaces);
        basicProduction = getAttribute(in, "basicProduction", parent.basicProduction);

        consumes = specification.getType(in, "consumes", GoodsType.class, parent.consumes);
        produces = specification.getType(in, "produces", GoodsType.class, parent.produces);

        if (produces != null && basicProduction > 0) {
            productionModifier = new Modifier(produces.getId(), this, basicProduction,
                                              Modifier.Type.ADDITIVE);
        }

        minSkill = getAttribute(in, "minSkill", parent.minSkill);
        maxSkill = getAttribute(in, "maxSkill", parent.maxSkill);
        
        sequence = getAttribute(in, "sequence", parent.sequence);

        if (parent != this) {
            getFeatureContainer().add(parent.getFeatureContainer());
        }
    }

    /**
     * Makes an XML-representation of this object.
     * 
     * @param out The output stream.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        // Start element:
        out.writeStartElement(getXMLElementTagName());

        // Add attributes:
        super.writeAttributes(out);
        if (upgradesFrom != null) {
            out.writeAttribute("upgradesFrom", upgradesFrom.getId());
        }
        out.writeAttribute("workplaces", Integer.toString(workPlaces));
        out.writeAttribute("basicProduction", Integer.toString(basicProduction));
        out.writeAttribute("minSkill", Integer.toString(minSkill));
        out.writeAttribute("maxSkill", Integer.toString(maxSkill));
        out.writeAttribute("sequence", Integer.toString(sequence));

        if (consumes != null) {
            out.writeAttribute("consumes", consumes.getId());
        }
        if (produces != null) {
            out.writeAttribute("produces", produces.getId());
        }
        super.writeChildren(out);

        // End element:
        out.writeEndElement();

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

    public boolean canAdd(UnitType unitType) {
        return unitType.hasSkill() && unitType.getSkill() >= minSkill && unitType.getSkill() <= maxSkill;
    }
  
}
