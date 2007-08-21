
package net.sf.freecol.common.model;


import java.util.Hashtable;
import java.util.Map;
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
     * Describe expertProduction here.
     */
    private GoodsType expertProduction;

    /**
     * Describe promotion here.
     */
    private String promotion;

    /**
     * Describe educationUnit here.
     */
    private String educationUnit = null;

    /**
     * Describe educationTurns here.
     */
    private int educationTurns;
    
    /**
     * Stores the abilities of this Type.
     */
    private Hashtable<String, Boolean> abilities = new Hashtable<String, Boolean>();

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
     * Get the <code>EducationUnit</code> value.
     *
     * @return a <code>UnitType</code> value
     */
    public UnitType getEducationUnit() {
        return FreeCol.getSpecification().getUnitType(educationUnit);
    }

    /**
     * Set the <code>EducationUnit</code> value.
     *
     * @param newEducationUnit The new EducationUnit value.
     */
    public void setEducationUnit(final String newEducationUnit) {
        this.educationUnit = newEducationUnit;
    }

    /**
     * Get the <code>EducationTurns</code> value.
     *
     * @return a <code>int</code> value
     */
    public int getEducationTurns() {
        return educationTurns;
    }

    /**
     * Set the <code>EducationTurns</code> value.
     *
     * @param newEducationTurns The new EducationUnit value.
     */
    public void setEducationTurns(final int newEducationTurns) {
        this.educationTurns = newEducationTurns;
    }

    public void readFromXmlElement(Node xml, Map<String, GoodsType> goodsTypeByRef) {

        id = Xml.attribute(xml, "name");
        name = Xml.attribute(xml, "name");
        offence = Xml.intAttribute(xml, "offence");
        defence = Xml.intAttribute(xml, "defence");
        movement = Xml.intAttribute(xml, "movement");
        lineOfSight = Xml.intAttribute(xml, "lineOfSight");
        space = Xml.intAttribute(xml, "space", 0);
        hitPoints = Xml.intAttribute(xml, "hitPoints", 0);
        spaceTaken = Xml.intAttribute(xml, "spaceTaken", 1);
        promotion = Xml.attribute(xml, "promotion", null);

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
                    } else if ("education".equals(nodeName)) {
                        educationUnit = Xml.attribute(node, "unit");
                        educationTurns = Xml.intAttribute(node, "turns");
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


}
