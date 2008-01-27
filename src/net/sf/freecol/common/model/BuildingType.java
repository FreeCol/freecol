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

import java.util.ArrayList;
import java.util.Map;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.freecol.common.Specification;

/**
 * Contains information on building types, like the number of upgrade levels a
 * given building type can have. The levels contain the information about the
 * name of the building in a given level and what is needed to build it.
 */
public final class BuildingType extends BuildableType {
    
    private int level, defenceBonus;
  
    private int workPlaces, basicProduction, minSkill, maxSkill;
    private GoodsType consumes, produces;
    
    private BuildingType upgradesFrom;
    private BuildingType upgradesTo;
    
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
    
    public int getDefenceBonus() {
        return defenceBonus;
    }

    public FreeColGameObjectType getType() {
        return this;
    }

    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        throw new UnsupportedOperationException("Call 'readFromXML' instead.");
    }

    public void readFromXML(XMLStreamReader in, Specification specification) throws XMLStreamException {
        setId(in.getAttributeValue(null, "id"));
        
        if (hasAttribute(in, "upgradesFrom")) {
            upgradesFrom = specification.getBuildingType(in.getAttributeValue(null, "upgradesFrom"));
            upgradesFrom.upgradesTo = this;
            level = upgradesFrom.level + 1;
        } else {
            level = 1;
        }
        
        defenceBonus = getAttribute(in, "defence-bonus", 0);
        workPlaces = getAttribute(in, "workplaces", 0);
        basicProduction = getAttribute(in, "basicProduction", 0);

        String consumeStr = in.getAttributeValue(null, "consumes");
        if (consumeStr != null) {
            consumes = specification.getGoodsType(consumeStr);
        }

        String produceStr = in.getAttributeValue(null, "produces");
        if (produceStr != null) {
            produces = specification.getGoodsType(produceStr);
        }

        minSkill = getAttribute(in, "minSkill", Integer.MIN_VALUE);
        maxSkill = getAttribute(in, "maxSkill", Integer.MAX_VALUE);

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            String childName = in.getLocalName();
            if (Ability.getXMLElementTagName().equals(childName)) {
                Ability ability = new Ability(in);
                if (ability.getSource() == null) {
                    ability.setSource(this.getId());
                }
                addFeature(ability); // Ability close the element
            } else if ("required-population".equals(childName)) {
                setPopulationRequired(getAttribute(in, "value", 1));
                in.nextTag(); // close this element
            } else if ("required-ability".equals(childName)) {
                String abilityId = in.getAttributeValue(null, "id");
                boolean value = getAttribute(in, "value", true);
                getAbilitiesRequired().put(abilityId, value);
                in.nextTag(); // close this element
            } else if ("required-goods".equals(childName)) {
                GoodsType type = specification.getGoodsType(in.getAttributeValue(null, "id"));
                int amount = getAttribute(in, "value", 0);
                AbstractGoods requiredGoods = new AbstractGoods(type, amount);
                if (getGoodsRequired() == null) {
                    setGoodsRequired(new ArrayList<AbstractGoods>());
                }
                getGoodsRequired().add(requiredGoods);
                in.nextTag(); // close this element
            } else if (Modifier.getXMLElementTagName().equals(childName)) {
                Modifier modifier = new Modifier(in);
                if (modifier.getSource() == null) {
                    modifier.setSource(this.getId());
                }
                addFeature(modifier); // Modifier close the element
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
  
}
