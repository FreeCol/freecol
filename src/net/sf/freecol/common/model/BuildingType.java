package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import net.sf.freecol.FreeCol;

import net.sf.freecol.common.util.Xml;

import org.w3c.dom.Node;

/**
 * Contains information on building types, like the number of upgrade levels a
 * given building type can have. The levels contain the information about the
 * name of the building in a given level and what is needed to build it.
 */
public final class BuildingType extends FreeColGameObjectType implements Abilities {
    public static final String COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";
    
    private int level, defenseBonus;
    private int hammersRequired, toolsRequired, populationRequired;
  
    private int workPlaces, basicProduction, minSkill, maxSkill;
    private GoodsType consumes, produces;
    
    private BuildingType upgradesFrom;
    private BuildingType upgradesTo;
    
    private HashMap<String, Boolean> abilities = new HashMap<String, Boolean>();
    
    /**
     * Stores the production bonuses of this Type
     */
    private HashMap<GoodsType, Integer> prodBonuses = new HashMap<GoodsType, Integer>();
    
    /**
     * Stores the production factors of this Type
     */
    private HashMap<GoodsType, Float> prodFactors = new HashMap<GoodsType, Float>();
  

    public BuildingType(int index) {
        setIndex(index);
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

    public int getHammersRequired() {
        return hammersRequired;
    }

    public int getToolsRequired() {
        return toolsRequired;
    }
    
    public int getPopulationRequired() {
        return populationRequired;
    }
    
    public int getLevel() {
        return level;
    }
    
    public int getDefenseBonus() {
        return defenseBonus;
    }

    /**
     * Reads the content of this BuildingType object from the given XML node.
     * 
     * @param xml an XML node from which to fill this BuildingType's fields.
     */
    public void readFromXmlElement(Node xml, final Map<String, GoodsType> goodsTypeByRef,
                                             final Map<String, BuildingType> buildingTypeByRef) {
        setID(Xml.attribute(xml, "id"));
        
        if (Xml.hasAttribute(xml, "upgradesFrom")) {
            upgradesFrom = buildingTypeByRef.get(Xml.attribute(xml, "upgradesFrom"));
            upgradesFrom.upgradesTo = this;
            level = upgradesFrom.level + 1;
        } else {
            level = 1;
        }
        
        defenseBonus = Xml.intAttribute(xml, "defense-bonus", 0);
        hammersRequired = Xml.intAttribute(xml, "hammers-required", 0);
        toolsRequired = Xml.intAttribute(xml, "tools-required", 0);
        
        workPlaces = Xml.intAttribute(xml, "workplaces");
        basicProduction = Xml.intAttribute(xml, "basicProduction", 0);
        
        consumes = goodsTypeByRef.get(Xml.attribute(xml, "consumes", null));
        produces = goodsTypeByRef.get(Xml.attribute(xml, "produces", null));
        
        minSkill = Xml.intAttribute(xml, "minSkill", Integer.MIN_VALUE);
        maxSkill = Xml.intAttribute(xml, "maxSkill", Integer.MAX_VALUE);
        populationRequired = 1;

          Xml.Method method = new Xml.Method() {
            public void invokeOn(Node node) {
                String childName = node.getNodeName();

                if ("ability".equals(childName)) {
                    String abilityId = Xml.attribute(node, "id");
                    boolean value = Xml.booleanAttribute(node, "value");
                    setAbility(abilityId, value);
                } else if ("required-population".equals(childName)) {
                    populationRequired = Xml.intAttribute(node, "value");
                } else if ("production-bonus".equals(childName)) {
                    GoodsType goodsType = goodsTypeByRef.get(Xml.attribute(node, "goods-type"));
                    if (goodsType != null) {
                        if (Xml.hasAttribute(node, "bonus")) {
                            int bonus = Xml.intAttribute(node, "bonus");
                            prodBonuses.put(goodsType, bonus);
                        } else if (Xml.hasAttribute(node, "factor")) {
                            float factor = Xml.floatAttribute(node, "factor");
                            prodFactors.put(goodsType, factor);
                        }
                    }
                }
              }
          };
  
          Xml.forEachChild(xml, method);
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
        return abilities.containsKey(id) && abilities.get(id);
    }

    /**
     * Sets the ability to newValue;
     *
     * @param id a <code>String</code> value
     * @param newValue a <code>boolean</code> value
     */
    public void setAbility(String id, boolean newValue) {
        abilities.put(id, newValue);
    }

    /**
     * Returns a copy of this BuildingType's abilities.
     *
     * @return a <code>Map</code> value
     */
    public Map<String, Boolean> getAbilities() {
        return new HashMap<String, Boolean>(abilities);
    }

    /**
     * Returns a copy of this building's production bonuses
     *
     * @return a <code>Map</code> value
     */
    public Set<Entry<GoodsType, Integer>> getProductionBonuses() {
        return prodBonuses.entrySet();
    }

    /**
     * Returns whether this building has a modifier to production of given goods
     *
     * @param goodsType A goods type
     * @return a <code>Boolean</code> value
     */
    public boolean hasProductionModifierFor(GoodsType goodsType) {
        return prodBonuses.containsKey(goodsType) || prodFactors.containsKey(goodsType);
    }

    /**
     * Returns a copy of this building's production factors
     *
     * @return a <code>Map</code> value
     */
    public Set<Entry<GoodsType, Float>> getProductionFactors() {
        return prodFactors.entrySet();
    }
}
