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
public final class BuildingType extends FreeColGameObjectType implements Abilities, Modifiers {
    public static final String COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";
    
    private int level, defenseBonus;
    private int hammersRequired, toolsRequired, populationRequired;
  
    private int workPlaces, basicProduction, minSkill, maxSkill;
    private GoodsType consumes, produces;
    
    private BuildingType upgradesFrom;
    private BuildingType upgradesTo;
    
    /**
     * Stores the abilities required by this Type.
     */
    private HashMap<String, Boolean> requiredAbilities = new HashMap<String, Boolean>();
    
    private HashMap<String, Modifier> modifiers = new HashMap<String, Modifier>();
  

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
        hammersRequired = getAttribute(in, "hammers-required", 0);
        toolsRequired = getAttribute(in, "tools-required", 0);
        
        workPlaces = getAttribute(in, "workplaces", 0);
        basicProduction = getAttribute(in, "basicProduction", 0);
        
        consumes = goodsTypeByRef.get(in.getAttributeValue(null, "consumes"));
        produces = goodsTypeByRef.get(in.getAttributeValue(null, "produces"));
        
        minSkill = getAttribute(in, "minSkill", Integer.MIN_VALUE);
        maxSkill = getAttribute(in, "maxSkill", Integer.MAX_VALUE);
        populationRequired = 1;

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            String childName = in.getLocalName();
            if ("ability".equals(childName)) {
                String abilityId = in.getAttributeValue(null, "id");
                boolean value = getAttribute(in, "value", true);
                setModifier(abilityId, new Modifier(abilityId, getID(), value));
                in.nextTag(); // close this element
            } else if ("required-population".equals(childName)) {
                populationRequired = getAttribute(in, "value", 1);
                in.nextTag(); // close this element
            } else if ("required-ability".equals(childName)) {
                String abilityId = in.getAttributeValue(null, "id");
                boolean value = getAttribute(in, "value", true);
                requiredAbilities.put(abilityId, value);
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
     * Returns the abilities required by this BuildingType.
     *
     * @return the abilities required by this BuildingType.
     */
    public Map<String, Boolean> getAbilitiesRequired() {
        return requiredAbilities;
    }


    /**
     * Returns true if this UnitType has the ability with the given ID.
     *
     * @param id a <code>String</code> value
     * @return a <code>boolean</code> value
       */
    public boolean hasAbility(String id) {
        return modifiers.containsKey(id) && modifiers.get(id).getBooleanValue();
    }

    /**
     * Sets the ability to newValue;
     *
     * @param id a <code>String</code> value
     * @param newValue a <code>boolean</code> value
     */
    public void setAbility(String id, boolean newValue) {
        modifiers.put(id, new Modifier(id, newValue));
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
        return modifiers.get(id);
    }

    /**
     * Set the <code>Modifier</code> value.
     *
     * @param id a <code>String</code> value
     * @param newModifier a <code>Modifier</code> value
     */
    public final void setModifier(String id, final Modifier newModifier) {
        modifiers.put(id, newModifier);
    }

    /**
     * Returns a copy of this FoundingFather's modifiers.
     *
     * @return a <code>Map</code> value
     */
    public Map<String, Modifier> getModifiers() {
        return new HashMap<String, Modifier>(modifiers);
    }
}
