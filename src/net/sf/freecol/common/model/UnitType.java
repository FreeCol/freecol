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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.Specification;
import net.sf.freecol.common.model.Player.PlayerType;

public final class UnitType extends BuildableType {

    public static final int DEFAULT_OFFENCE = 0;
    public static final int DEFAULT_DEFENCE = 1;

    public static enum UpgradeType { EDUCATION, NATIVES, EXPERIENCE,
            LOST_CITY, PROMOTION }

    public static enum DowngradeType { CLEAR_SKILL, DEMOTION, CAPTURE }

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
     * How much a Unit of this type contributes to the Player's score.
     */
    private int scoreValue;

    /**
     * Describe art here.
     */
    private String art;

    /**
     * Describe pathImage here.
     */
    private String pathImage;

    /**
     * Describe maximumAttrition here.
     */
    private int maximumAttrition;

    /**
     * The ID of the skill this UnitType teaches, mostly its own.
     */
    private String skillTaught;

    /**
     * Describe education here.
     */
    private HashMap<String, Upgrade> upgrades = new HashMap<String, Upgrade>();
    
    /**
     * Describe education here.
     */
    private HashMap<String, Downgrade> downgrades = new HashMap<String, Downgrade>();


    
    /**
     * Creates a new <code>UnitType</code> instance.
     *
     * @param index an <code>int</code> value
     */
    public UnitType(int index) {
        setIndex(index);
    }

    /**
     * Returns <code>true</code> if Units of this type can carry other Units.
     *
     * @return a <code>boolean</code> value
     */
    public boolean canCarryUnits() {
        return hasAbility("model.ability.carryUnits");
    }

    /**
     * Returns <code>true</code> if Units of this type can carry Goods.
     *
     * @return a <code>boolean</code> value
     */
    public boolean canCarryGoods() {
        return hasAbility("model.ability.carryGoods");
    }

    /**
     * Get the <code>Art</code> value.
     *
     * @return a <code>String</code> value
     */
    public String getArt() {
        return art;
    }

    /**
     * Set the <code>Art</code> value.
     *
     * @param newArt The new Art value.
     */
    public void setArt(final String newArt) {
        this.art = newArt;
    }

    /**
     * Get the <code>ScoreValue</code> value.
     *
     * @return an <code>int</code> value
     */
    public int getScoreValue() {
        return scoreValue;
    }

    /**
     * Set the <code>ScoreValue</code> value.
     *
     * @param newScoreValue The new ScoreValue value.
     */
    public void setScoreValue(final int newScoreValue) {
        this.scoreValue = newScoreValue;
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
     * 
     * This returns the base price of the <code>UnitType</code>
     * 
     * For the actual price of the unit, use {@link Europe#getUnitPrice(UnitType)} 
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
     * Get the <code>MaximumAttrition</code> value.
     *
     * @return an <code>int</code> value
     */
    public int getMaximumAttrition() {
        return maximumAttrition;
    }

    /**
     * Set the <code>MaximumAttrition</code> value.
     *
     * @param newMaximumAttrition The new MaximumAttrition value.
     */
    public void setMaximumAttrition(final int newMaximumAttrition) {
        this.maximumAttrition = newMaximumAttrition;
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
     * Get the <code>PathImage</code> value.
     *
     * @return a <code>String</code> value
     */
    public String getPathImage() {
        return pathImage;
    }

    /**
     * Set the <code>PathImage</code> value.
     *
     * @param newPathImage The new PathImage value.
     */
    public void setPathImage(final String newPathImage) {
        this.pathImage = newPathImage;
    }

    /**
     * Get the <code>SkillTaught</code> value.
     *
     * @return an <code>String</code> value
     */
    public String getSkillTaught() {
        return skillTaught;
    }

    /**
     * Set the <code>SkillTaught</code> value.
     *
     * @param newSkillTaught The new SkillTaught value.
     */
    public void setSkillTaught(final String newSkillTaught) {
        this.skillTaught = newSkillTaught;
    }

    /**
     * Returns true if the UnitType is available to the given
     * Player.
     *
     * @param player a <code>Player</code> value
     * @return a <code>boolean</code> value
     */
    public boolean isAvailableTo(Player player) {
        java.util.Map<String, Boolean> requiredAbilities = getAbilitiesRequired();
        for (Entry<String, Boolean> entry : requiredAbilities.entrySet()) {
            if (player.hasAbility(entry.getKey()) != entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    public UnitType getPromotion() {
        for (Entry<String, Upgrade> entry : upgrades.entrySet()) {
            if (entry.getValue().asResultOf.get(UpgradeType.PROMOTION)) {
                return FreeCol.getSpecification().getUnitType(entry.getKey());
            }
        }
        return null;
    }

    /**
     * Describe <code>getDowngrade</code> method here.
     *
     * @return an <code>UnitType</code> value
     */
    public UnitType getDowngrade(DowngradeType downgradeType) {
        Iterator<Entry<String, Downgrade>> iterator = downgrades.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<String, Downgrade> pair = iterator.next();
            if (pair.getValue().asResultOf.get(downgradeType)) {
                return FreeCol.getSpecification().getUnitType(pair.getKey());
            }
        }
        return null;
    }

    

    /**
     * Whether this UnitType can be upgraded to the given UnitType by
     * the given means of education.
     *
     * @param newType the UnitType to learn
     * @param educationType an <code>UpgradeType</code> value
     * @return <code>true</code> if can learn the given UnitType
     */
    public boolean canBeUpgraded(UnitType newType, UpgradeType educationType) {
        Upgrade upgrade = upgrades.get(newType.getSkillTaught());
        return upgrade != null && upgrade.asResultOf.get(educationType);
    }

    /**
     * Get a list of UnitType which can learn in a lost city rumour
     *
     * @return <code>UnitType</code> with a skill equal or less than given
     * maximum
     */
    public List<UnitType> getUnitTypesLearntInLostCity() {
        Iterator<Entry<String, Upgrade>> iterator = upgrades.entrySet().iterator();
        ArrayList<UnitType> unitTypes = new ArrayList<UnitType>();
        while (iterator.hasNext()) {
            Entry<String, Upgrade> pair = iterator.next();
            if (pair.getValue().asResultOf.get(UpgradeType.LOST_CITY)) {
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
        for (Entry<String, Upgrade> entry : upgrades.entrySet()) {
            if (entry.getValue().canBeTaught()) {
                UnitType unitType = FreeCol.getSpecification().getUnitType(entry.getKey());
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
        Upgrade upgrade = upgrades.get(unitType.getId());
        if (upgrade != null) {
            return upgrade.turnsToLearn;
        } else {
            return UNDEFINED;
        }
    }

    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        throw new UnsupportedOperationException("Call 'readFromXML' instead.");
    }

    public void readAttributes(XMLStreamReader in, Specification specification)
            throws XMLStreamException {
        offence = getAttribute(in, "offence", DEFAULT_OFFENCE);
        defence = getAttribute(in, "defence", DEFAULT_DEFENCE);
        movement = Integer.parseInt(in.getAttributeValue(null, "movement"));
        lineOfSight = getAttribute(in, "lineOfSight", 1);
        scoreValue = getAttribute(in, "scoreValue", 0);
        space = getAttribute(in, "space", 0);
        hitPoints = getAttribute(in, "hitPoints", 0);
        spaceTaken = getAttribute(in, "spaceTaken", 1);
        maximumAttrition = getAttribute(in, "maximumAttrition", Integer.MAX_VALUE);
        skillTaught = getAttribute(in, "skillTaught", getId());
        
        art = in.getAttributeValue(null, "art");
        pathImage = in.getAttributeValue(null, "pathImage");

        recruitProbability = getAttribute(in, "recruitProbability", 0);
        skill = getAttribute(in, "skill", UNDEFINED);

        setPopulationRequired(getAttribute(in, "population-required", 1));

        price = getAttribute(in, "price", UNDEFINED);
        increasingPrice = getAttribute(in, "increasingPrice", UNDEFINED);

        expertProduction = specification.getType(in, "expert-production", GoodsType.class, null);

    }

    public void readChildren(XMLStreamReader in, Specification specification) throws XMLStreamException {
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            String nodeName = in.getLocalName();
            if ("upgrade".equals(nodeName)) {
                Upgrade upgrade = new Upgrade();
                String educationUnit = in.getAttributeValue(null, "unit");
                upgrade.turnsToLearn = getAttribute(in, "turnsToLearn", UNDEFINED);
                upgrade.asResultOf.put(UpgradeType.EDUCATION, getAttribute(in, "learnInSchool", true));
                upgrade.asResultOf.put(UpgradeType.NATIVES, getAttribute(in, "learnFromNatives", false));
                upgrade.asResultOf.put(UpgradeType.EXPERIENCE, getAttribute(in, "learnFromExperience", false));
                upgrade.asResultOf.put(UpgradeType.LOST_CITY, getAttribute(in, "learnInLostCity", false));
                upgrade.asResultOf.put(UpgradeType.PROMOTION, getAttribute(in, "promotion", false));
                upgrades.put(educationUnit, upgrade);
                in.nextTag(); // close this element
            } else if ("downgrade".equals(nodeName)) {
                Downgrade downgrade = new Downgrade();
                String educationUnit = in.getAttributeValue(null, "unit");
                downgrade.asResultOf.put(DowngradeType.CLEAR_SKILL, getAttribute(in, "clearSkill", true));
                downgrade.asResultOf.put(DowngradeType.DEMOTION, getAttribute(in, "demotion", false));
                downgrade.asResultOf.put(DowngradeType.CAPTURE, getAttribute(in, "capture", false));
                downgrades.put(educationUnit, downgrade);
                in.nextTag(); // close this element
            } else {
                super.readChild(in, specification);
            }
        }
    }


    /**
     * Returns true if this UnitType has a skill.
     *
     * @return a <code>boolean</code> value
     */
    public boolean hasSkill() {

        return skill != UNDEFINED;
    }


    /**
     * Returns true if this UnitType can be built.
     *
     * @return a <code>boolean</code> value
     */
    public boolean canBeBuilt() {
        return getGoodsRequired().isEmpty() == false;
    }


    /**
     * Returns true if this UnitType has a price.
     *
     * @return a <code>boolean</code> value
     */
    public boolean hasPrice() {
        return price != UNDEFINED;
    }

    public int getProductionFor(GoodsType goodsType, int base) {
        if (base == 0) {
            return 0;
        }
        
        base = (int) featureContainer.applyModifier(base, goodsType.getId());
        return Math.max(base, 1);
    }

    public EquipmentType[] getDefaultEquipment() {
        if (hasAbility("model.ability.canBeEquipped")) {
            List<EquipmentType> equipment = new ArrayList<EquipmentType>();
            if (hasAbility("model.ability.expertSoldier")) {
                equipment.add(FreeCol.getSpecification().getEquipmentType("model.equipment.muskets"));
            }
            if (hasAbility("model.ability.expertScout")) {
                equipment.add(FreeCol.getSpecification().getEquipmentType("model.equipment.horses"));
            }
            if (hasAbility("model.ability.expertPioneer")) {
                EquipmentType tools = FreeCol.getSpecification().getEquipmentType("model.equipment.tools");
                for (int count = 0; count < tools.getMaximumCount(); count++) {
                    equipment.add(tools);
                }
            }
            if (hasAbility("model.ability.expertMissionary")) {
                equipment.add(FreeCol.getSpecification().getEquipmentType("model.equipment.missionary"));
            }
            return equipment.toArray(EquipmentType.NO_EQUIPMENT);
        } else {
            return EquipmentType.NO_EQUIPMENT;
        }
    }

    
    private class Upgrade {
        protected int turnsToLearn;
        protected EnumMap<UpgradeType, Boolean> asResultOf = 
            new EnumMap<UpgradeType, Boolean>(UpgradeType.class);
        
        public boolean canBeTaught() {
            return asResultOf.get(UpgradeType.EDUCATION) && turnsToLearn > 0;
        }
    }

    private class Downgrade {
        protected EnumMap<DowngradeType, Boolean> asResultOf = 
            new EnumMap<DowngradeType, Boolean>(DowngradeType.class);
    }
}
