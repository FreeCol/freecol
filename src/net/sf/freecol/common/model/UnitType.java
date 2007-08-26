
package net.sf.freecol.common.model;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.sf.freecol.FreeCol;

import net.sf.freecol.common.util.Xml;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public final class UnitType implements Abilities {
    public static final  String  COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
    public static final  String  LICENSE   = "http://www.gnu.org/licenses/gpl.html";
    public static final  String  REVISION  = "$Revision$";

    public static final  int  UNDEFINED = Integer.MIN_VALUE;
    public final int index;

    /**
     * Describe id here.
     */
    private String id;

    /**
     * Describe name here.
     */
    private String name;

    /**
     * Describe offence here.
     */
    private int offence;

    /**
     * Describe defence here.
     */
    private int defence;

    /**
     * Describe space here.
     */
    private int space;

    /**
     * Describe hitPoints here.
     */
    private int hitPoints;

    /**
     * Describe spaceTaken here.
     */
    private int spaceTaken;

    /**
     * Describe hammersRequired here.
     */
    private int hammersRequired;

    /**
     * Describe toolsRequired here.
     */
    private int toolsRequired;

    /**
     * Describe skill here.
     */
    private int skill;

    /**
     * Describe price here.
     */
    private int price;

    /**
     * Describe price here.
     */
    private int increasingPrice;

    /**
     * Describe movement here.
     */
    private int movement;
    
    /**
     * Describe lineOfSight here.
     */
    private int lineOfSight;

    /**
     * Describe recruitProbability here.
     */
    private int recruitProbability;

    /**
     * Describe expertProduction here.
     */
    private GoodsType expertProduction;

    /**
     * Describe promotion here.
     */
    private String promotion;

    /**
     * Describe clearSpeciality here.
     */
    private String clearSpeciality;

    /**
     * Describe education here.
     */
    private Hashtable<String, Upgrade> upgrades = new Hashtable<String, Upgrade>();
    
    /**
     * Stores the abilities of this Type.
     */
    private Hashtable<String, Boolean> abilities = new Hashtable<String, Boolean>();
    
    /**
     * Stores the production bonuses of this Type
     */
    private Hashtable<String, Integer> prodBonuses = new Hashtable<String, Integer>();
    
    /**
     * Stores the production factors of this Type
     */
    private Hashtable<String, Float> prodFactors = new Hashtable<String, Float>();
    
    public UnitType(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }
    
    /**
     * Get the <code>Id</code> value.
     *
     * @return a <code>String</code> value
     */
    public String getId() {
        return id;
    }

    /**
     * Set the <code>Id</code> value.
     *
     * @param newId The new Id value.
     */
    public void setId(final String newId) {
        this.id = newId;
    }

    /**
     * Get the <code>Name</code> value.
     *
     * @return a <code>String</code> value
     */
    public String getName() {
        return name;
    }

    /**
     * Set the <code>Name</code> value.
     *
     * @param newName The new Name value.
     */
    public void setName(final String newName) {
        this.name = newName;
    }

    public String getDescription() {
        return name + ".description";
    }

    /**
     * Get the <code>Offence</code> value.
     *
     * @return an <code>int</code> value
     */
    public int getOffence() {
        return offence;
    }

    /**
     * Set the <code>Offence</code> value.
     *
     * @param newOffence The new Offence value.
     */
    public void setOffence(final int newOffence) {
        this.offence = newOffence;
    }

    /**
     * Get the <code>Defence</code> value.
     *
     * @return an <code>int</code> value
     */
    public int getDefence() {
        return defence;
    }

    /**
     * Set the <code>Defence</code> value.
     *
     * @param newDefence The new Defence value.
     */
    public void setDefence(final int newDefence) {
        this.defence = newDefence;
    }

    /**
     * Get the <code>LineOfSight</code> value.
     *
     * @return an <code>int</code> value
     */
    public int getLineOfSight() {
        return lineOfSight;
    }

    /**
     * Set the <code>LineOfSight</code> value.
     *
     * @param newLineOfSight The new Defence value.
     */
    public void setLineOfSight(final int newLineOfSight) {
        this.lineOfSight = newLineOfSight;
    }

    /**
     * Get the <code>Space</code> value.
     *
     * @return an <code>int</code> value
     */
    public int getSpace() {
        return space;
    }

    /**
     * Set the <code>Space</code> value.
     *
     * @param newSpace The new Space value.
     */
    public void setSpace(final int newSpace) {
        this.space = newSpace;
    }

    /**
     * Get the <code>HitPoints</code> value.
     *
     * @return an <code>int</code> value
     */
    public int getHitPoints() {
        return hitPoints;
    }

    /**
     * Set the <code>HitPoints</code> value.
     *
     * @param newHitPoints The new HitPoints value.
     */
    public void setHitPoints(final int newHitPoints) {
        this.hitPoints = newHitPoints;
    }

    /**
     * Get the <code>SpaceTaken</code> value.
     *
     * @return an <code>int</code> value
     */
    public int getSpaceTaken() {
        return spaceTaken;
    }

    /**
     * Set the <code>SpaceTaken</code> value.
     *
     * @param newSpaceTaken The new SpaceTaken value.
     */
    public void setSpaceTaken(final int newSpaceTaken) {
        this.spaceTaken = newSpaceTaken;
    }

    /**
     * If this UnitType is recruitable in Europe
     *
     * @return an <code>boolean</code> value
     */
    public boolean isRecruitable() {
        return recruitProbability > 0;
    }

    /**
     * Get the <code>RecruitProbability</code> value.
     *
     * @return an <code>int</code> value
     */
    public int getRecruitProbability() {
        return recruitProbability;
    }

    /**
     * Set the <code>RecruitProbability</code> value.
     *
     * @param newRecruitProbability The new RecruitProbability value.
     */
    public void setRecruitProbability(final int newRecruitProbability) {
        this.recruitProbability = newRecruitProbability;
    }

    /**
     * Get the <code>HammersRequired</code> value.
     *
     * @return an <code>int</code> value
     */
    public int getHammersRequired() {
        return hammersRequired;
    }

    /**
     * Set the <code>HammersRequired</code> value.
     *
     * @param newHammersRequired The new HammersRequired value.
     */
    public void setHammersRequired(final int newHammersRequired) {
        this.hammersRequired = newHammersRequired;
    }

    /**
     * Get the <code>ToolsRequired</code> value.
     *
     * @return an <code>int</code> value
     */
    public int getToolsRequired() {
        return toolsRequired;
    }

    /**
     * Set the <code>ToolsRequired</code> value.
     *
     * @param newToolsRequired The new ToolsRequired value.
     */
    public void setToolsRequired(final int newToolsRequired) {
        this.toolsRequired = newToolsRequired;
    }

    /**
     * Get the <code>Skill</code> value.
     *
     * @return an <code>int</code> value
     */
    public int getSkill() {
        return skill;
    }

    /**
     * Set the <code>Skill</code> value.
     *
     * @param newSkill The new Skill value.
     */
    public void setSkill(final int newSkill) {
        this.skill = newSkill;
    }

    /**
     * Get the <code>Price</code> value.
     *
     * @return an <code>int</code> value
     */
    public int getPrice() {
        return price;
    }

    /**
     * Set the <code>Price</code> value.
     *
     * @param newPrice The new Price value.
     */
    public void setPrice(final int newPrice) {
        this.price = newPrice;
    }

    /**
     * Get the <code>IncreasingPrice</code> value.
     *
     * @return an <code>int</code> value
     */
    public int getIncreasingPrice() {
        return increasingPrice;
    }

    /**
     * Set the <code>IncreasingPrice</code> value.
     *
     * @param newIncreasingPrice The new IncreasingPrice value.
     */
    public void setIncreasingPrice(final int newIncreasingPrice) {
        this.increasingPrice = newIncreasingPrice;
    }

    /**
     * Get the <code>Movement</code> value.
     *
     * @return an <code>int</code> value
     */
    public int getMovement() {
        return movement;
    }

    /**
     * Set the <code>Movement</code> value.
     *
     * @param newMovement The new Movement value.
     */
    public void setMovement(final int newMovement) {
        this.movement = newMovement;
    }

    /**
     * Get the <code>ExpertProduction</code> value.
     *
     * @return a <code>GoodsType</code> value
     */
    public GoodsType getExpertProduction() {
        return expertProduction;
    }

    /**
     * Set the <code>ExpertProduction</code> value.
     *
     * @param newExpertProduction The new ExpertProduction value.
     */
    public void setExpertProduction(final GoodsType newExpertProduction) {
        this.expertProduction = newExpertProduction;
    }

    /**
     * Get the <code>Promotion</code> value.
     *
     * @return a <code>UnitType</code> value
     */
    public UnitType getPromotion() {
        return FreeCol.getSpecification().getUnitType(promotion);
    }

    /**
     * Set the <code>Promotion</code> value.
     *
     * @param newPromotion The new Promotion value.
     */
    public void setPromotion(final String newPromotion) {
        this.promotion = newPromotion;
    }

    /**
     * Get the <code>ClearSpeciality</code> value.
     *
     * @return a <code>UnitType</code> value
     */
    public UnitType getClearSpeciality() {
        return FreeCol.getSpecification().getUnitType(clearSpeciality);
    }

    /**
     * Set the <code>ClearSpeciality</code> value.
     *
     * @param newClearSpeciality The new ClearSpeciality value.
     */
    public void setClearSpeciality(final String newClearSpeciality) {
        this.clearSpeciality = newClearSpeciality;
    }

    /**
     * Whether the given UnitType can be teached
     *
     * @param unitType the UnitType to learn
     * @return <code>true</code> if can learn the given UnitType
     */
    public boolean canBeTaught(UnitType unitType) {
        Upgrade upgrade = upgrades.get(unitType.getName());
        return upgrade != null && upgrade.canBeTaught();
    }

    /**
     * Whether can learn from experience the given UnitType
     *
     * @param unitType the UnitType to learn
     * @return <code>true</code> if can learn the given UnitType
     */
    public boolean canLearnFromExperience(UnitType unitType) {
        Upgrade upgrade = upgrades.get(unitType.getName());
        return upgrade != null && upgrade.learnFromExperience;
    }

    /**
     * Whether can learn from natives the given UnitType
     *
     * @param unitType the UnitType to learn
     * @return <code>true</code> if can learn the given UnitType
     */
    public boolean canLearnFromNatives(UnitType unitType) {
        Upgrade upgrade = upgrades.get(unitType.getName());
        return upgrade != null && upgrade.learnFromNatives;
    }

    /**
     * Whether can learn in lost city rumour the given UnitType
     *
     * @param unitType the UnitType to learn
     * @return <code>true</code> if can learn the given UnitType
     */
    public boolean canLearnInLostCity(UnitType unitType) {
        Upgrade upgrade = upgrades.get(unitType.getName());
        return upgrade != null && upgrade.learnInLostCity;
    }

    /**
     * Get a list of UnitType which can learn in a lost city rumour
     *
     * @param maximumSkill the maximum level skill which we are searching for
     * @return <code>UnitType</code> with a skill equal or less than given
     * maximum
     */
    public List<UnitType> getUnitTypesLearntInLostCity() {
        Iterator<Entry<String, Upgrade>> iterator = upgrades.entrySet().iterator();
        ArrayList<UnitType> unitTypes = new ArrayList<UnitType>();
        while (iterator.hasNext()) {
            Entry<String, Upgrade> pair = iterator.next();
            if (pair.getValue().learnInLostCity) {
                unitTypes.add(FreeCol.getSpecification().getUnitType(pair.getKey()));
            }
        }
        return unitTypes;
    }

    /**
     * Get a UnitType to learn with a level skill less or equal than given level
     *
     * @param maximumSkill the maximum level skill which we are searching for
     * @return <code>UnitType</code> with a skill equal or less than given
     * maximum
     */
    public UnitType getEducationUnit(int maximumSkill) {
        Iterator<Entry<String, Upgrade>> unitTypes = upgrades.entrySet().iterator();
        while (unitTypes.hasNext()) {
            Entry<String, Upgrade> pair = unitTypes.next();
            if (pair.getValue().canBeTaught()) {
                UnitType unitType = FreeCol.getSpecification().getUnitType(pair.getKey());
                if (unitType.hasSkill() && unitType.getSkill() <= maximumSkill) {
                    return unitType;
                }
            }
        }
        return null;
    }

    /**
     * Get the <code>EducationTurns</code> value.
     *
     * @return a <code>int</code> value
     */
    public int getEducationTurns(UnitType unitType) {
        Upgrade upgrade = upgrades.get(unitType.getName());
        if (upgrade != null) {
            return upgrade.turnsToLearn;
        } else {
            return UNDEFINED;
        }
    }

    public void readFromXmlElement(Node xml, final Map<String, GoodsType> goodsTypeByRef) {
        name = Xml.attribute(xml, "name");
        String[] buffer = name.split("\\.");
        id = buffer[buffer.length - 1];
        offence = Xml.intAttribute(xml, "offence");
        defence = Xml.intAttribute(xml, "defence");
        movement = Xml.intAttribute(xml, "movement");
        lineOfSight = Xml.intAttribute(xml, "lineOfSight");
        space = Xml.intAttribute(xml, "space", 0);
        hitPoints = Xml.intAttribute(xml, "hitPoints", 0);
        spaceTaken = Xml.intAttribute(xml, "spaceTaken", 1);
        promotion = Xml.attribute(xml, "promotion", null);
        clearSpeciality = Xml.attribute(xml, "clearSpeciality", null);

        recruitProbability = Xml.intAttribute(xml, "recruitProbability", 0);
        skill = Xml.intAttribute(xml, "skill", UNDEFINED);

        hammersRequired = Xml.intAttribute(xml, "hammers", UNDEFINED);
        toolsRequired = Xml.intAttribute(xml, "tools", UNDEFINED);

        price = Xml.intAttribute(xml, "price", UNDEFINED);
        increasingPrice = Xml.intAttribute(xml, "increasingPrice", UNDEFINED);

        if (Xml.hasAttribute(xml, "expert-production")) {
            String goodsTypeRef = Xml.attribute(xml, "expert-production");
            expertProduction = goodsTypeByRef.get(goodsTypeRef);
        } else {
            expertProduction = null;
        }

        Xml.Method method = new Xml.Method() {
                public void invokeOn(Node node) {
                    String nodeName = node.getNodeName();
                    if ("ability".equals(nodeName)) {
                        String abilityId = Xml.attribute(node, "id");
                        boolean value = Xml.booleanAttribute(node, "value");
                        setAbility(abilityId, value);
                    } else if ("upgrade".equals(nodeName)) {
                        Upgrade upgrade = new Upgrade();
                        String educationUnit = Xml.attribute(node, "unit");
                        upgrade.turnsToLearn = Xml.intAttribute(node, "turnsToLearn", UNDEFINED);
                        upgrade.learnFromNatives = Xml.booleanAttribute(node, "learnFromNatives", false);
                        upgrade.learnFromExperience = Xml.booleanAttribute(node, "learnFromExperience", false);
                        upgrade.learnInLostCity = Xml.booleanAttribute(node, "learnInLostCity", false);
                        upgrades.put(educationUnit, upgrade);
                    } else if ("production-bonus".equals(nodeName)) {
                        String goodsType = Xml.attribute(node, "goods-type");
                        if (goodsTypeByRef.containsKey(goodsType)) {
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


    public boolean hasSkill() {

        return skill != UNDEFINED;
    }


    public boolean canBeBuilt() {

        return hammersRequired != UNDEFINED;
    }


    public boolean hasPrice() {

        return price != UNDEFINED;
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

    public int getProductionFor(GoodsType goodsType, int base) {
        if (base == 0) {
            return 0;
        }
        
        Integer bonus = prodBonuses.get(goodsType.getName());
        if (bonus != null) {
            base += bonus.intValue();
        } else {
            Float factor = prodFactors.get(goodsType.getName());
            if (factor != null) {
                base *= factor.floatValue();
            }
        }
        return Math.min(base, 1);
    }
    
    private class Upgrade {
        protected int turnsToLearn;
        protected boolean learnFromNatives, learnFromExperience, learnInLostCity;
        
        public boolean canBeTaught() {
            return turnsToLearn > 0;
        }
    }
}
