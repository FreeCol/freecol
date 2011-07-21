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
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.UnitTypeChange.ChangeType;


public final class UnitType extends BuildableType
    implements Comparable<UnitType>, Consumer {

    public static final int DEFAULT_OFFENCE = 0;
    public static final int DEFAULT_DEFENCE = 1;

    /**
     * Describe offence here.
     */
    private int offence = DEFAULT_OFFENCE;

    /**
     * Describe defence here.
     */
    private int defence = DEFAULT_DEFENCE;

    /**
     * Describe space here.
     */
    private int space = 0;

    /**
     * Describe hitPoints here.
     */
    private int hitPoints = 0;

    /**
     * Describe spaceTaken here.
     */
    private int spaceTaken = 1;

    /**
     * Describe skill here.
     */
    private int skill = UNDEFINED;

    /**
     * Describe price here.
     */
    private int price = UNDEFINED;

    /**
     * Describe movement here.
     */
    private int movement = 3;

    /**
     * Describe lineOfSight here.
     */
    private int lineOfSight = 1;

    /**
     * Describe recruitProbability here.
     */
    private int recruitProbability = 0;

    /**
     * Describe expertProduction here.
     */
    private GoodsType expertProduction;

    /**
     * How much a Unit of this type contributes to the Player's score.
     */
    private int scoreValue = 0;

    /**
     * The maximum experience a unit of this type can accumulate.
     */
    private int maximumExperience = 0;

    /**
     * Describe maximumAttrition here.
     */
    private int maximumAttrition = INFINITY;

    /**
     * The skill this UnitType teaches, mostly its own.
     */
    private UnitType skillTaught;

    /**
     * Describe defaultEquipment here.
     */
    private EquipmentType defaultEquipment;

    /**
     * The goods consumed per turn when in a settlement.
     */
    private TypeCountMap<GoodsType> consumption = new TypeCountMap<GoodsType>();

    /**
     * The possible type changes for this unit type.
     */
    private List<UnitTypeChange> typeChanges = new ArrayList<UnitTypeChange>();


    /**
     * Creates a new <code>UnitType</code> instance.
     *
     */
    public UnitType(String id, Specification specification) {
        super(id, specification);
        setModifierIndex(Modifier.EXPERT_PRODUCTION_INDEX);
    }

    public final String getWorkingAsKey() {
        return getId() + ".workingAs";
    }

    /**
     * Returns <code>true</code> if Units of this type can carry other Units.
     *
     * @return a <code>boolean</code> value
     */
    public boolean canCarryUnits() {
        return hasAbility(Ability.CARRY_UNITS);
    }

    /**
     * Returns <code>true</code> if Units of this type can carry Goods.
     *
     * @return a <code>boolean</code> value
     */
    public boolean canCarryGoods() {
        return hasAbility(Ability.CARRY_GOODS);
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
        return Math.max(spaceTaken, space + 1);
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
     * Get the <code>MaximumExperience</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getMaximumExperience() {
        return maximumExperience;
    }

    /**
     * Set the <code>MaximumExperience</code> value.
     *
     * @param newMaximumExperience The new MaximumExperience value.
     */
    public final void setMaximumExperience(final int newMaximumExperience) {
        this.maximumExperience = newMaximumExperience;
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
     * Get the <code>DefaultEquipment</code> value.
     *
     * @return an <code>EquipmentType</code> value
     */
    public EquipmentType getDefaultEquipmentType() {
        return defaultEquipment;
    }

    /**
     * Set the <code>DefaultEquipment</code> value.
     *
     * @param newDefaultEquipment The new DefaultEquipment value.
     */
    public void setDefaultEquipmentType(final EquipmentType newDefaultEquipment) {
        this.defaultEquipment = newDefaultEquipment;
    }

    public EquipmentType[] getDefaultEquipment() {
        if (hasAbility(Ability.CAN_BE_EQUIPPED) && defaultEquipment != null) {
            int count = defaultEquipment.getMaximumCount();
            EquipmentType[] result = new EquipmentType[count];
            for (int index = 0; index < count; index++) {
                result[index] = defaultEquipment;
            }
            return result;
        } else {
            return EquipmentType.NO_EQUIPMENT;
        }
    }

    public List<UnitTypeChange> getTypeChanges() {
        return typeChanges;
    }

    /**
     * Get the <code>SkillTaught</code> value.
     *
     * @return an <code>UnitType</code> value
     */
    public UnitType getSkillTaught() {
        return skillTaught;
    }

    /**
     * Set the <code>SkillTaught</code> value.
     *
     * @param newSkillTaught The new SkillTaught value.
     */
    public void setSkillTaught(final UnitType newSkillTaught) {
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

    /**
     * Describe <code>getUnitTypeChange</code> method here.
     *
     * @param changeType an <code>UnitTypeChange.Type</code> value
     * @param player a <code>Player</code> value
     * @return an <code>UnitType</code> value
     */
    public UnitType getTargetType(ChangeType changeType, Player player) {
        UnitTypeChange change = getUnitTypeChange(changeType, player);
        return (change == null) ? null : change.getNewUnitType();
    }

    /**
     * Describe <code>getUnitTypeChange</code> method here.
     *
     * @param changeType an <code>UnitTypeChange.Type</code> value
     * @param player a <code>Player</code> value
     * @return an <code>UnitType</code> value
     */
    public UnitTypeChange getUnitTypeChange(ChangeType changeType, Player player) {
        for (UnitTypeChange change : typeChanges) {
            if (change.asResultOf(changeType) && change.appliesTo(player)) {
                UnitType result = change.getNewUnitType();
                if (result.isAvailableTo(player)) {
                    return change;
                }
            }
        }
        return null;
    }


    /**
     * Returns the <code>UnitTypeChange</code> associated with the
     * given <code>UnitType</code>, or <code>null</code> if there is
     * none.
     *
     * @param newType the target UnitType
     * @return the type change
     */
    public UnitTypeChange getUnitTypeChange(UnitType newType) {
        for (UnitTypeChange change : typeChanges) {
            if (change.getNewUnitType() == newType) {
                return change;
            }
        }
        return null;
    }

    /**
     * Return true if this UnitType can be upgraded to the given
     * UnitType by the given means of education. If the given UnitType
     * is null, return true if the UnitType can be upgraded to any
     * other UnitType by the given means of education.
     *
     * @param newType The UnitType to learn (may be null in the case
     *     of attempting to move to a native settlement when the skill
     *     taught there is still unknown).
     * @param changeType an <code>ChangeType</code> value
     * @return <code>true</code> if can learn the given UnitType
     */
    public boolean canBeUpgraded(UnitType newType, ChangeType changeType) {
        for (UnitTypeChange change : typeChanges) {
            if ((newType == null || newType == change.getNewUnitType())
                && change.getProbability(changeType) > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get a list of UnitType which can learn in a lost city rumour
     *
     * @return <code>UnitType</code> with a skill equal or less than given
     * maximum
     */
    public List<UnitType> getUnitTypesLearntInLostCity() {
        List<UnitType> unitTypes = new ArrayList<UnitType>();
        for (UnitTypeChange change : typeChanges) {
            if (change.asResultOf(ChangeType.LOST_CITY)) {
                unitTypes.add(change.getNewUnitType());
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
        for (UnitTypeChange change : typeChanges) {
            if (change.canBeTaught()) {
                UnitType unitType = change.getNewUnitType();
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
        for (UnitTypeChange change : typeChanges) {
            if (change.asResultOf(UnitTypeChange.ChangeType.EDUCATION)) {
                if (unitType == change.getNewUnitType()) {
                    return change.getTurnsToLearn();
                }
            }
        }
        return UNDEFINED;
    }

    /**
     * Is this a naval unit type?
     *
     * @return True if this is a naval unit type.
     */
    public boolean isNaval() {
        return hasAbility(Ability.NAVAL_UNIT);
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

    /**
     * Returns the number of units of the given GoodsType this
     * UnitType consumes per turn (when in a settlement).
     *
     * @return units consumed
     */
    public int getConsumptionOf(GoodsType goodsType) {
        return consumption.getCount(goodsType);
    }

    // Interface Comparable

    /**
     * {@inheritDoc}
     */
    public int compareTo(UnitType other) {
        return getIndex() - other.getIndex();
    }

    // Interface Consumer

    /**
     * Returns a list of GoodsTypes this Consumer consumes.
     *
     * @return a <code>List</code> value
     */
    public List<AbstractGoods> getConsumedGoods() {
        List<AbstractGoods> result = new ArrayList<AbstractGoods>();
        for (GoodsType goodsType : consumption.keySet()) {
            result.add(new AbstractGoods(goodsType, consumption.getCount(goodsType)));
        }
        return result;
    }

    /**
     * The priority of this Consumer. The higher the priority, the
     * earlier will the Consumer be allowed to consume the goods it
     * requires.
     *
     * @return an <code>int</code> value
     */
    public int getPriority() {
        // TODO: make this configurable
        return UNIT_PRIORITY;
    }


    /**
     * Makes an XML-representation of this object.
     *
     * @param out The output stream.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        super.toXML(out, getXMLElementTagName());
    }

    /**
     * Write the attributes of this object to a stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing to
     *     the stream.
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out)
        throws XMLStreamException {
        super.writeAttributes(out);

        out.writeAttribute("offence", Integer.toString(offence));
        out.writeAttribute("defence", Integer.toString(defence));
        out.writeAttribute("movement", Integer.toString(movement));
        out.writeAttribute("lineOfSight", Integer.toString(lineOfSight));
        out.writeAttribute("scoreValue", Integer.toString(scoreValue));
        out.writeAttribute("space", Integer.toString(space));
        out.writeAttribute("spaceTaken", Integer.toString(spaceTaken));
        out.writeAttribute("hitPoints", Integer.toString(hitPoints));
        out.writeAttribute("maximumExperience",
            Integer.toString(maximumExperience));
        if (maximumAttrition < INFINITY) {
            out.writeAttribute("maximumAttrition",
                Integer.toString(maximumAttrition));
        }
        out.writeAttribute("recruitProbability",
            Integer.toString(recruitProbability));
        if (skill != UNDEFINED) {
            out.writeAttribute("skill", Integer.toString(skill));
        }
        if (price != UNDEFINED) {
            out.writeAttribute("price", Integer.toString(price));
        }
        out.writeAttribute("skillTaught", skillTaught.getId());
        if (expertProduction != null) {
            out.writeAttribute("expert-production", expertProduction.getId());
        }
    }

    /**
     * Write the children of this object to a stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing to
     *     the stream.
     */
    @Override
    protected void writeChildren(XMLStreamWriter out)
        throws XMLStreamException {
        super.writeChildren(out);

        for (UnitTypeChange change : typeChanges) {
            change.toXMLImpl(out);
        }
        if (defaultEquipment != null) {
            out.writeStartElement("default-equipment");
            out.writeAttribute(ID_ATTRIBUTE_TAG, defaultEquipment.getId());
            out.writeEndElement();
        }
        if (!consumption.isEmpty()) {
            for (GoodsType goodsType : consumption.keySet()) {
                out.writeStartElement("consumes");
                out.writeAttribute(ID_ATTRIBUTE_TAG, goodsType.getId());
                out.writeAttribute(VALUE_TAG, Integer.toString(consumption.getCount(goodsType)));
                out.writeEndElement();
            }
        }
    }

    /**
     * Reads the attributes of this object from an XML stream.
     *
     * @param in The XML input stream.
     * @throws XMLStreamException if a problem was encountered
     *     during parsing.
     */
    @Override
    protected void readAttributes(XMLStreamReader in)
        throws XMLStreamException {
        super.readAttributes(in);

        String extendString = in.getAttributeValue(null, "extends");
        UnitType parent = (extendString == null) ? this :
            getSpecification().getUnitType(extendString);
        offence = getAttribute(in, "offence", parent.offence);
        defence = getAttribute(in, "defence", parent.defence);
        movement = getAttribute(in, "movement", parent.movement);
        lineOfSight = getAttribute(in, "lineOfSight", parent.lineOfSight);
        scoreValue = getAttribute(in, "scoreValue", parent.scoreValue);
        space = getAttribute(in, "space", parent.space);
        hitPoints = getAttribute(in, "hitPoints", parent.hitPoints);
        spaceTaken = getAttribute(in, "spaceTaken", parent.spaceTaken);
        maximumExperience = getAttribute(in, "maximumExperience",
            parent.maximumExperience);
        maximumAttrition = getAttribute(in, "maximumAttrition",
            parent.maximumAttrition);
        String skillString = in.getAttributeValue(null, "skillTaught");
        skillTaught = (skillString == null) ? this
            : getSpecification().getUnitType(skillString);

        recruitProbability = getAttribute(in, "recruitProbability",
            parent.recruitProbability);
        skill = getAttribute(in, "skill", parent.skill);

        setPopulationRequired(getAttribute(in, "population-required",
                parent.getPopulationRequired()));

        price = getAttribute(in, "price", parent.price);

        expertProduction = getSpecification().getType(in, "expert-production",
            GoodsType.class, parent.expertProduction);

        if (parent != this) {
            typeChanges.addAll(parent.typeChanges);
            defaultEquipment = parent.defaultEquipment;
            consumption.putAll(parent.consumption);
            getFeatureContainer().add(parent.getFeatureContainer());
            if (parent.isAbstractType()) {
                getFeatureContainer().replaceSource(parent, this);
            }
        }
    }

    /**
     * Reads a child object.
     *
     * @param in The XML stream to read.
     * @exception XMLStreamException if an error occurs
     */
    @Override
    protected void readChild(XMLStreamReader in) throws XMLStreamException {
        String nodeName = in.getLocalName();
        Specification spec = getSpecification();
        if ("downgrade".equals(nodeName)
            || "upgrade".equals(nodeName)) {
            if (getAttribute(in, "delete", false)) {
                String unitId = in.getAttributeValue(null, "unit");
                Iterator<UnitTypeChange> iterator = typeChanges.iterator();
                while (iterator.hasNext()) {
                    if (unitId.equals(iterator.next()
                            .getNewUnitType().getId())) {
                        iterator.remove();
                        break;
                    }
                }
                in.nextTag();
            } else {
                UnitTypeChange change = new UnitTypeChange(in, spec);
                if ("downgrade".equals(nodeName)
                    && change.getChangeTypes().isEmpty()) {
                    // add default downgrade type
                    change.getChangeTypes().put(ChangeType.CLEAR_SKILL, 100);
                }
                typeChanges.add(change);
            }
        } else if ("default-equipment".equals(nodeName)) {
            String equipmentString
                = in.getAttributeValue(null, ID_ATTRIBUTE_TAG);
            if (equipmentString != null) {
                defaultEquipment = spec.getEquipmentType(equipmentString);
            }
            in.nextTag(); // close this element
        } else if ("consumes".equals(nodeName)) {
            String typeString = in.getAttributeValue(null, ID_ATTRIBUTE_TAG);
            String valueString = in.getAttributeValue(null, VALUE_TAG);
            if (typeString != null && valueString != null) {
                try {
                    GoodsType type = spec.getGoodsType(typeString);
                    int amount = Integer.parseInt(valueString);
                    consumption.incrementCount(type, amount);
                } catch(Exception e) {
                    logger.warning("Failed to parse integer " + valueString);
                }
            }
            in.nextTag(); // close this element
        } else {
            super.readChild(in);
        }
    }

    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return "unit-type".
     */
    public static String getXMLElementTagName() {
        return "unit-type";
    }
}
