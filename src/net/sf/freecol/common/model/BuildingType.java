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

import java.util.HashMap;
import java.util.Map;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Contains information on building types, like the number of upgrade levels a
 * given building type can have. The levels contain the information about the
 * name of the building in a given level and what is needed to build it.
 */
public final class BuildingType extends BuildableType implements Abilities, Modifiers {

    
    private int level, defenseBonus;
  
    private int workPlaces, basicProduction, minSkill, maxSkill;
    private GoodsType consumes, produces;
    
    private BuildingType upgradesFrom;
    private BuildingType upgradesTo;
    
    private HashMap<String, Feature> features = new HashMap<String, Feature>();

    public BuildingType(int index) {
        setIndex(index);
        setPopulationRequired(1);
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
    
    public int getDefenseBonus() {
        return defenseBonus;
    }

    public FreeColGameObjectType getType() {
        return this;
    }

    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        readFromXML(in, null, null);
    }

    public void readFromXML(XMLStreamReader in, final Map<String, GoodsType> goodsTypeByRef,
           final Map<String, BuildingType> buildingTypeByRef) throws XMLStreamException {
        setID(in.getAttributeValue(null, "id"));
        
        if (hasAttribute(in, "upgradesFrom")) {
            upgradesFrom = buildingTypeByRef.get(in.getAttributeValue(null, "upgradesFrom"));
            upgradesFrom.upgradesTo = this;
            level = upgradesFrom.level + 1;
        } else {
            level = 1;
        }
        
        defenseBonus = getAttribute(in, "defense-bonus", 0);
        setHammersRequired(getAttribute(in, "hammers-required", 0));
        setToolsRequired(getAttribute(in, "tools-required", 0));
        
        workPlaces = getAttribute(in, "workplaces", 0);
        basicProduction = getAttribute(in, "basicProduction", 0);
        
        consumes = goodsTypeByRef.get(in.getAttributeValue(null, "consumes"));
        produces = goodsTypeByRef.get(in.getAttributeValue(null, "produces"));
        
        minSkill = getAttribute(in, "minSkill", Integer.MIN_VALUE);
        maxSkill = getAttribute(in, "maxSkill", Integer.MAX_VALUE);

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            String childName = in.getLocalName();
            if (Ability.getXMLElementTagName().equals(childName)) {
                String abilityId = in.getAttributeValue(null, "id");
                boolean value = getAttribute(in, "value", true);
                features.put(abilityId, new Ability(abilityId, getID(), value));
                in.nextTag(); // close this element
            } else if ("required-population".equals(childName)) {
                setPopulationRequired(getAttribute(in, "value", 1));
                in.nextTag(); // close this element
            } else if ("required-ability".equals(childName)) {
                String abilityId = in.getAttributeValue(null, "id");
                boolean value = getAttribute(in, "value", true);
                getAbilitiesRequired().put(abilityId, value);
                in.nextTag(); // close this element
            } else if (Modifier.getXMLElementTagName().equals(childName)) {
                Modifier modifier = new Modifier(in);
                if (modifier.getSource() == null) {
                    modifier.setSource(this.getID());
                }
                setModifier(modifier.getId(), modifier); // Modifier close the element
            } else {
                logger.finest("Parsing of " + childName + " is not implemented yet");
                while (in.nextTag() != XMLStreamConstants.END_ELEMENT ||
                        !in.getLocalName().equals(childName)) {
                    in.nextTag();
                }
            }
        }
    }
    
    public boolean canAdd(UnitType unitType) {
        return unitType.hasSkill() && unitType.getSkill() >= minSkill && unitType.getSkill() <= maxSkill;
    }
  
    /**
     * Returns true if this UnitType has the ability with the given ID.
     *
     * @param id a <code>String</code> value
     * @return a <code>boolean</code> value
       */
    public boolean hasAbility(String id) {
        return features.containsKey(id) && 
            (features.get(id) instanceof Ability) &&
            ((Ability) features.get(id)).getValue();
    }

    /**
     * Sets the ability to newValue;
     *
     * @param id a <code>String</code> value
     * @param newValue a <code>boolean</code> value
     */
    public void setAbility(String id, boolean newValue) {
        features.put(id, new Ability(id, newValue));
    }

    /**
     * Returns a copy of this BuildingType's abilities.
     *
     * @return a <code>Map</code> value
     */
    public Map<String, Boolean> getAbilities() {
        return new HashMap<String, Boolean>();
    }

    /**
     * Get the <code>Modifier</code> value.
     *
     * @param id a <code>String</code> value
     * @return a <code>Modifier</code> value
     */
    public final Modifier getModifier(String id) {
        return (Modifier) features.get(id);
    }

    /**
     * Set the <code>Modifier</code> value.
     *
     * @param id a <code>String</code> value
     * @param newModifier a <code>Modifier</code> value
     */
    public final void setModifier(String id, final Modifier newModifier) {
        features.put(id, newModifier);
    }

    /**
     * Returns a copy of this BuildingType's features.
     *
     * @return a <code>Map</code> value
     */
    public Map<String, Feature> getFeatures() {
        return new HashMap<String, Feature>(features);
    }
}
