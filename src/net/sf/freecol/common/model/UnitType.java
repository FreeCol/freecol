
package net.sf.freecol.common.model;


import java.util.HashSet;
import java.util.Map;

import net.sf.freecol.common.util.Xml;

import org.w3c.dom.Node;


public final class UnitType {
    public static final  String  COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
    public static final  String  LICENSE   = "http://www.gnu.org/licenses/gpl.html";
    public static final  String  REVISION  = "$Revision$";

    public static final  int  UNDEFINED = Integer.MIN_VALUE;

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
     * Describe movement here.
     */
    private int movement;

    /**
     * Describe expertProduction here.
     */
    private GoodsType expertProduction;
    private HashSet<String> abilityArray = new HashSet<String>();    

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

    public void readFromXmlElement(Node xml, Map<String, GoodsType> goodsTypeByRef) {

        id = Xml.attribute(xml, "name");
        name = Xml.attribute(xml, "name");
        offence = Xml.intAttribute(xml, "offence");
        defence = Xml.intAttribute(xml, "defence");
        movement = Xml.intAttribute(xml, "movement");

        if (Xml.hasAttribute(xml, "skill")) {

            skill = Xml.intAttribute(xml, "skill");
        }
        else {
            skill = UNDEFINED;
        }

        if (Xml.hasAttribute(xml, "hammers")) {

            hammersRequired = Xml.intAttribute(xml, "hammers");
            toolsRequired = Xml.intAttribute(xml, "tools");
        }
        else {
            hammersRequired = UNDEFINED;
            toolsRequired = UNDEFINED;
        }

        if (Xml.hasAttribute(xml, "price")) {

            price = Xml.intAttribute(xml, "price");
        }
        else {
            price = UNDEFINED;
        }

        if (Xml.hasAttribute(xml, "expert-production")) {

            String  goodsTypeRef = Xml.attribute(xml, "expert-production");
            expertProduction = goodsTypeByRef.get(goodsTypeRef);
        }
        else {
            expertProduction = null;
        }

        String[] array = Xml.attribute(xml, "abilities").split(",");
        if (array != null)
        {
            for (int i = 0; i < array.length; i++) {
                abilityArray.add(array[i]);
            }
        }

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


    public boolean hasAbility(String abilityName) {
        return abilityArray.contains(abilityName);
    }

}
