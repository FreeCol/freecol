/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import javax.swing.JList;
import javax.swing.ListModel;
import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Colony.NoBuildReason;
import static net.sf.freecol.common.model.Constants.*;
import net.sf.freecol.common.model.UnitTypeChange;
import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * The various types of units in FreeCol.
 */
public final class UnitType extends BuildableType implements Consumer {

    public static final String TAG = "unit-type";

    /** Comparator for defence ability. */
    public static final Comparator<UnitType> defenceComparator
            = Comparator.comparingDouble(UnitType::getDefence);

    /** The default offence value. */
    public static final int DEFAULT_OFFENCE = 0;

    /** The default offence value. */
    public static final int DEFAULT_DEFENCE = 1;


    /**
     * The offence of this UnitType. Only Units with an offence value
     * greater than zero can attack.
     */
    private int baseOffence = DEFAULT_OFFENCE;

    /** The defence of this UnitType. */
    private int baseDefence = DEFAULT_DEFENCE;

    /** The capacity of this UnitType. */
    private int space = 0;

    /** Is this the default unit type? */
    private boolean defaultUnitType = false;

    /**
     * The number of hit points this UnitType has. At the moment, this
     * is only used for ships. All other UnitTypes are downgraded or
     * destroyed if they lose a battle.
     */
    private int hitPoints = 0;

    /** The space taken by this UnitType. */
    private int spaceTaken = 1;

    /** The skill level of this UnitType. */
    private int skill = UNDEFINED;

    /** The price of this UnitType. */
    private int price = UNDEFINED;

    /** The optional price of this UnitType for use by mercenary forces. */
    private int mercenaryPrice = UNDEFINED;

    /** The initial moves of this UnitType. */
    private int movement = 3;

    /** The maximum distance of tiles this UnitType can observe. */
    private int lineOfSight = 1;

    /** The probability of recruiting a Unit of this type in Europe. */
    private int recruitProbability = 0;

    /** The expert production of this UnitType. */
    private GoodsType expertProduction = null;

    /** How much a Unit of this type contributes to the Player's score. */
    private int scoreValue = 0;

    /** The maximum experience a unit of this type can accumulate. */
    private int maximumExperience = 0;

    /**
     * The maximum attrition this UnitType can accumulate without
     * being destroyed.
     */
    private int maximumAttrition = INFINITY;

    /** Consumption order. */
    private int priority = Consumer.UNIT_PRIORITY;

    /** The skill this UnitType teaches, mostly its own. */
    private UnitType skillTaught = null;

    /** The default role for a unit of this type. */
    private Role defaultRole = null;

    /** The goods consumed per turn when in a settlement. */
    private TypeCountMap<GoodsType> consumption = null;


    /**
     * Creates a new {@code UnitType} instance.
     *
     * @param id The object identifier.
     * @param specification The {@code Specification} to refer to.
     */
    public UnitType(String id, Specification specification) {
        super(id, specification);

        this.defaultRole = specification.getDefaultRole();
    }


    /**
     * Get a key for the working as this unit type message.
     *
     * @return A message key.
     */
    public final String getWorkingAsKey() {
        return getId() + ".workingAs";
    }

    /**
     * Can this unit type carry units?
     *
     * @return True if units can be carried.
     */
    public boolean canCarryUnits() {
        return hasAbility(Ability.CARRY_UNITS);
    }

    /**
     * Can this unit type carry goods?
     *
     * @return True if goods can be carried.
     */
    public boolean canCarryGoods() {
        return hasAbility(Ability.CARRY_GOODS);
    }

    /**
     * Gets the score for acquiring a unit of this type.
     *
     * @return The score for this unit type.
     */
    public int getScoreValue() {
        return scoreValue;
    }

    /**
     * Get the base offence value.
     *
     * @return The base offence value.
     */
    public int getBaseOffence() {
        return this.baseOffence;
    }

    /**
     * Get the offence of this unit type.
     *
     * @return The offence value.
     */
    public double getOffence() {
        return apply(this.baseOffence, null, Modifier.OFFENCE);
    }

    /**
     * Is this an offensive unit type?
     *
     * @return True if base offensive ability is greater than the default.
     */
    public boolean isOffensive() {
        return getBaseOffence() > UnitType.DEFAULT_OFFENCE;
    }

    /**
     * Get the base defence value.
     *
     * @return The defence value.
     */
    public int getBaseDefence() {
        return this.baseDefence;
    }

    /**
     * Get the total defence of this unit type.
     *
     * @return The defence value.
     */
    public double getDefence() {
        return apply(this.baseDefence, null, Modifier.DEFENCE);
    }

    /**
     * Is this a defensive unit type?
     *
     * Default defence is 1, same a for colonists, thus to be defensive, a
     * colonist must have a military role.  Artillery of all sorts has
     * higher defense so they are automatically defensive.
     *
     * @return True if base defensive ability is greater than the default.
     */
    public boolean isDefensive() {
        return getBaseDefence() > UnitType.DEFAULT_DEFENCE;
    }

    /**
     * Is this the default unit type?
     *
     * @return True if this is the default unit type.
     */
    public boolean isDefaultUnitType() {
        return this.defaultUnitType;
    }

    /**
     * Get the `line of sight' distance (in tiles).
     *
     * @return The line of sight distance.
     */
    public int getLineOfSight() {
        return lineOfSight;
    }

    /**
     * Get the space this unit type has to carry cargo.
     *
     * @return The cargo capacity of this unit type.
     */
    public int getSpace() {
        return space;
    }

    /**
     * Set the space this unit type has to carry cargo.
     * Required by the test suite.
     *
     * @param newSpace The new cargo capacity.
     */
    public void setSpace(final int newSpace) {
        this.space = newSpace;
    }

    /**
     * Get the unit type hit points.
     *
     * @return The hit points.
     */
    public int getHitPoints() {
        return hitPoints;
    }

    /**
     * Gets the number of cargo slots a unit of this type takes on a carrier.
     *
     * @return The number of cargo slots.
     */
    public int getSpaceTaken() {
        return Math.max(spaceTaken, space + 1);
    }

    /**
     * Set the number of cargo slots a unit of this type takes on a carrier.
     * Required by the test suite.
     *
     * @param newSpaceTaken The new number of cargo slots.
     */
    public void setSpaceTaken(final int newSpaceTaken) {
        this.spaceTaken = newSpaceTaken;
    }

    /**
     * Is this UnitType recruitable in Europe?
     *
     * @return True if European-recruitable.
     */
    public boolean isRecruitable() {
        return recruitProbability > 0;
    }

    /**
     * Get the relative probability of recruiting this unit in Europe.
     *
     * @return A relative probability.
     */
    public int getRecruitProbability() {
        return recruitProbability;
    }

    /**
     * Get the skill level associated with this unit type.
     *
     * @return The skill level.
     */
    public int getSkill() {
        return skill;
    }

    /**
     * Set the skill level associated with this unit type.
     * Required by the test suite.
     *
     * @param newSkill The new skill level.
     */
    public void setSkill(final int newSkill) {
        this.skill = newSkill;
    }

    /**
     * Get the base price of this unit type.
     * For the actual price of the unit, use
     * {@link Europe#getUnitPrice(UnitType)}
     *
     * @return The base price.
     */
    public int getPrice() {
        return price;
    }

    /**
     * Get the special price used by mercenary forces when there is no
     * standard base price (e.g. ManOWar).
     *
     * @return The mercenary price.
     */
    public int getMercenaryPrice() {
        return mercenaryPrice;
    }

    // @compat 0.11.6
    // Only used in spec fixups
    /**
     * Set the mercenary price.
     *
     * @param price The mercenary price.
     */
    public void setMercenaryPrice(int price) {
        this.mercenaryPrice = price;
    }
    // end @compat 0.11.6
    
    /**
     * Get the base movement of this unit type.
     *
     * @return The base movement.
     */
    public int getMovement() {
        return movement;
    }

    /**
     * Get the maximum experience required a unit of this type may achieve.
     *
     * @return The maximum experience.
     */
    public final int getMaximumExperience() {
        return maximumExperience;
    }

    /**
     * Is this unit type subject to attrition?
     *
     * @return True if attrition can happen for this unit type.
     */
    public boolean hasMaximumAttrition() {
        return maximumAttrition != INFINITY;
    }

    /**
     * Get the maximum attrition for this unit type (greater attrition than
     * this destroys the unit).
     *
     * @return The maximum attrition.
     */
    public int getMaximumAttrition() {
        return maximumAttrition;
    }

    /**
     * Get the type of goods this unit type has expert ability to produce.
     *
     * @return The expert production {@code GoodsType}.
     */
    public GoodsType getExpertProduction() {
        return expertProduction;
    }

    /**
     * Get the skill taught by this unit type.
     *
     * @return The skill taught by this unit type.
     */
    public UnitType getSkillTaught() {
        return skillTaught;
    }

    /**
     * Gets the default role of this unit type, mostly model.role.default.
     *
     * @return The default {@code Role}.
     */
    public Role getDefaultRole() {
        return defaultRole;
    }

    /**
     * Returns a list of roles for which a unit of this type is an
     * expert.
     *
     * @return a list of expert roles
     */
    public List<Role> getExpertRoles() {
        return transform(getSpecification().getRoles(),
                matchKey(this, Role::getExpertUnit));
    }

    /**
     * Get a role identifier for display routines to use for this unit type.
     *
     * @return A suitable role identifier for display purposes.
     */
    public String getDisplayRoleId() {
        Role r = first(getExpertRoles());
        return (r != null) ? r.getId() : Specification.DEFAULT_ROLE_ID;
    }

    /**
     * Get the type that this unit type can be educated to by a
     * teacher unit type, if any.
     *
     * @param teacherType The {@code UnitType} of the teacher.
     * @return The {@code UnitType} that this unit type can be educated
     *     to by the teacher unit type, or null if education is not possible.
     */
    public UnitType getTeachingType(UnitType teacherType) {
        final Specification spec = getSpecification();
        final UnitType taught = teacherType.getSkillTaught();
        final int taughtLevel = taught.getSkill();
        if (getSkill() >= taughtLevel) return null; // Fail fast

        // Is there an education change that gets this unit type to the
        // type taught by the teacher type?  If so, the taught type is valid
        // and should be returned at once.  Accumulate other intermediate
        // changes that do not reach the taught type level.
        List<UnitType> todo = new ArrayList<>();
        for (UnitTypeChange uc : spec.getUnitChanges(UnitChangeType.EDUCATION, this)) {
            if (uc.to == taught) return taught;
            if (uc.to.getSkill() < taughtLevel) todo.add(uc.to);
        }
        // Can the teacher teach any of the intermediate changes?  If so,
        // that change is valid.  Otherwise, education is not possible.
        for (UnitType ut : todo) {
            if (ut.getTeachingType(teacherType) != null) return ut;
        }
        return null;
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
     * Is this a person, not a ship or wagon?
     *
     * @return True if this unit type represents a person
     */
    public boolean isPerson() {
        return hasAbility(Ability.PERSON);
    }

    /**
     * Is this unit type able to build a colony?
     *
     * @return True if this unit type can build colonies.
     */
    public boolean canBuildColony() {
        return hasAbility(Ability.FOUND_COLONY);
    }

    /**
     * Can this unit type move to the High Seas?
     *
     * ATM this is synonymous with being a naval unit, but we should use
     * this routine instead of isNaval() in case this changes.
     *
     * @return True if units of this type can move to the High Seas.
     */
    public boolean canMoveToHighSeas() {
        return isNaval();
    }

    /**
     * Does this UnitType have a skill?
     *
     * @return True if this unit type has a skill.
     */
    public boolean hasSkill() {
        return skill != UNDEFINED;
    }

    /**
     * Does this UnitType have a price?
     *
     * @return True if the unit type has a price.
     */
    public boolean hasPrice() {
        return price != UNDEFINED;
    }

    /**
     * Get the consumption map.
     *
     * @return The map of the consumed goods.
     */
    protected TypeCountMap<GoodsType> getConsumption() {
        return this.consumption;
    }

    /**
     * Set the consumption map.
     *
     * @param consumption The new map of the consumed goods.
     */
    protected void setConsumption(TypeCountMap<GoodsType> consumption) {
        this.consumption = consumption;
    }

    /**
     * Gets the number of units of the given GoodsType this UnitType
     * consumes per turn (when in a settlement).
     *
     * @param goodsType The {@code GoodsType} to consume.
     * @return The amount of goods consumed per turn.
     */
    public int getConsumptionOf(GoodsType goodsType) {
        return (consumption == null) ? 0 : consumption.getCount(goodsType);
    }

    /**
     * Add consumption.
     *
     * @param type The {@code GoodsType} to consume.
     * @param amount The amount of goods to consume.
     */
    private void addConsumption(GoodsType type, int amount) {
        if (consumption == null) {
            consumption = new TypeCountMap<>();
        }
        consumption.incrementCount(type, amount);
    }

    /**
     * {@inheritDoc}
     */
    public NoBuildReason canBeBuiltInColony(Colony colony,
                                            List<BuildableType> assumeBuilt) {
        // Non-person units need a BUILD ability, present or assumed.
        if (!hasAbility(Ability.PERSON)
            && !colony.hasAbility(Ability.BUILD, this)
            && none(assumeBuilt, bt -> bt.hasAbility(Ability.BUILD, this))) {
            return Colony.NoBuildReason.MISSING_BUILD_ABILITY;
        }
        return Colony.NoBuildReason.NONE;
    }

    @Override
    public int getMinimumIndex(Colony colony, JList<BuildableType> buildQueueList, int UNABLE_TO_BUILD) {
        ListModel<BuildableType> buildQueue = buildQueueList.getModel();
        if (colony.canBuild(this)) return 0;
        for (int index = 0; index < buildQueue.getSize(); index++) {
            if (buildQueue.getElementAt(index).hasAbility(Ability.BUILD, this)) return index + 1;
        }
        return UNABLE_TO_BUILD;
    }

    @Override
    public int getMaximumIndex(Colony colony, JList<BuildableType> buildQueueList, int UNABLE_TO_BUILD) {
        ListModel<BuildableType> buildQueue = buildQueueList.getModel();
        final int buildQueueLastPos = buildQueue.getSize();

        boolean canBuild = false;
        if (colony.canBuild(this)) {
            canBuild = true;
        }

        // does not depend on anything, nothing depends on it
        // can be built at any time
        if (canBuild) return buildQueueLastPos;
        // check for building in queue that allows builting this unit
        for (int index = 0; index < buildQueue.getSize(); index++) {
            BuildableType toBuild = buildQueue.getElementAt(index);
            if (toBuild == this) continue;
            if (toBuild.hasAbility(Ability.BUILD, this)) {
                return buildQueueLastPos;
            }
        }
        return UNABLE_TO_BUILD;
    }


    // Interface Consumer

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AbstractGoods> getConsumedGoods() {
        return (consumption == null) ? Collections.<AbstractGoods>emptyList()
                : transform(consumption.keySet(),
                gt -> consumption.getCount(gt) != 0,
                gt -> new AbstractGoods(gt, consumption.getCount(gt)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPriority() {
        return priority;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<Modifier> getConsumptionModifiers(String id) {
        return getModifiers(id);
    }


    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends FreeColObject> boolean copyIn(T other) {
        UnitType o = copyInCast(other, UnitType.class);
        if (o == null || !super.copyIn(o)) return false;
        this.baseOffence = o.getBaseOffence();
        this.baseDefence = o.getBaseDefence();
        this.space = o.getSpace();
        this.defaultUnitType = o.isDefaultUnitType();
        this.hitPoints = o.getHitPoints();
        this.spaceTaken = o.getSpaceTaken();
        this.skill = o.getSkill();
        this.price = o.getPrice();
        this.movement = o.getMovement();
        this.lineOfSight = o.getLineOfSight();
        this.recruitProbability = o.getRecruitProbability();
        this.expertProduction = o.getExpertProduction();
        this.scoreValue = o.getScoreValue();
        this.maximumExperience = o.getMaximumExperience();
        this.maximumAttrition = o.getMaximumAttrition();
        this.priority = o.getPriority();
        this.skillTaught = o.getSkillTaught();
        this.defaultRole = o.getDefaultRole();
        this.consumption = o.getConsumption();
        return true;
    }


    // Serialization

    private static final String CONSUMES_TAG = "consumes";
    private static final String DEFAULT_ROLE_TAG = "default-role";
    private static final String DEFAULT_UNIT_TAG = "default-unit";
    private static final String DEFENCE_TAG = "defence";
    private static final String EXPERT_PRODUCTION_TAG = "expert-production";
    private static final String HIT_POINTS_TAG = "hit-points";
    private static final String LINE_OF_SIGHT_TAG = "line-of-sight";
    private static final String MERCENARY_PRICE_TAG = "mercenary-price";
    private static final String MOVEMENT_TAG = "movement";
    private static final String MAXIMUM_EXPERIENCE_TAG = "maximum-experience";
    private static final String MAXIMUM_ATTRITION_TAG = "maximum-attrition";
    private static final String OFFENCE_TAG = "offence";
    private static final String PRICE_TAG = "price";
    private static final String PRIORITY_TAG = "priority";
    private static final String RECRUIT_PROBABILITY_TAG = "recruit-probability";
    private static final String SCORE_VALUE_TAG = "score-value";
    private static final String SKILL_TAG = "skill";
    private static final String SKILL_TAUGHT_TAG = "skill-taught";
    private static final String SPACE_TAG = "space";
    private static final String SPACE_TAKEN_TAG = "space-taken";
    private static final String UNIT_TAG = "unit";
    // @compat 0.11.0
    private static final String OLD_DEFAULT_EQUIPMENT_TAG = "default-equipment";
    // end @compat 0.11.0
    // @compat 0.11.3
    private static final String OLD_DEFAULT_UNIT_TAG = "defaultUnit";
    private static final String OLD_HIT_POINTS_TAG = "hitPoints";
    private static final String OLD_LINE_OF_SIGHT_TAG = "lineOfSight";
    private static final String OLD_MAXIMUM_EXPERIENCE_TAG = "maximumExperience";
    private static final String OLD_MAXIMUM_ATTRITION_TAG = "maximumAttrition";
    private static final String OLD_RECRUIT_PROBABILITY_TAG = "recruitProbability";
    private static final String OLD_SCORE_VALUE_TAG = "scoreValue";
    private static final String OLD_SKILL_TAUGHT_TAG = "skillTaught";
    private static final String OLD_SPACE_TAKEN_TAG = "spaceTaken";
    // end @compat 0.11.3
    // @compat 0.11.6
    private static final String DOWNGRADE_TAG = "downgrade";
    private static final String UPGRADE_TAG = "upgrade";
    // end @compat 0.11.6

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(OFFENCE_TAG, baseOffence);

        xw.writeAttribute(DEFENCE_TAG, baseDefence);

        xw.writeAttribute(DEFAULT_UNIT_TAG, this.defaultUnitType);

        xw.writeAttribute(MOVEMENT_TAG, movement);

        xw.writeAttribute(LINE_OF_SIGHT_TAG, lineOfSight);

        xw.writeAttribute(SCORE_VALUE_TAG, scoreValue);

        xw.writeAttribute(SPACE_TAG, space);

        xw.writeAttribute(SPACE_TAKEN_TAG, spaceTaken);

        xw.writeAttribute(HIT_POINTS_TAG, hitPoints);

        xw.writeAttribute(MAXIMUM_EXPERIENCE_TAG, maximumExperience);

        if (hasMaximumAttrition()) {
            xw.writeAttribute(MAXIMUM_ATTRITION_TAG, maximumAttrition);
        }

        xw.writeAttribute(RECRUIT_PROBABILITY_TAG, recruitProbability);

        if (hasSkill()) {
            xw.writeAttribute(SKILL_TAG, skill);
        }

        if (hasPrice()) {
            xw.writeAttribute(PRICE_TAG, price);
        }

        if (mercenaryPrice != UNDEFINED) {
            xw.writeAttribute(MERCENARY_PRICE_TAG, mercenaryPrice);
        }

        xw.writeAttribute(SKILL_TAUGHT_TAG, skillTaught);

        if (expertProduction != null) {
            xw.writeAttribute(EXPERT_PRODUCTION_TAG, expertProduction);
        }

        xw.writeAttribute(PRIORITY_TAG, priority);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        final Specification spec = getSpecification();

        if (defaultRole != null && defaultRole != spec.getDefaultRole()) {
            xw.writeStartElement(DEFAULT_ROLE_TAG);

            xw.writeAttribute(ID_ATTRIBUTE_TAG, defaultRole);

            xw.writeEndElement();
        }

        if (consumption != null) {
            for (GoodsType goodsType : consumption.keySet()) {
                xw.writeStartElement(CONSUMES_TAG);

                xw.writeAttribute(ID_ATTRIBUTE_TAG, goodsType);

                xw.writeAttribute(VALUE_TAG, consumption.getCount(goodsType));

                xw.writeEndElement();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        final Specification spec = getSpecification();

        UnitType parent = xr.getType(spec, EXTENDS_TAG, UnitType.class, this);

        this.baseOffence = xr.getAttribute(OFFENCE_TAG, parent.baseOffence);

        this.baseDefence = xr.getAttribute(DEFENCE_TAG, parent.baseDefence);

        // @compat 0.11.3
        if (xr.hasAttribute(OLD_DEFAULT_UNIT_TAG)) {
            this.defaultUnitType = xr.getAttribute(OLD_DEFAULT_UNIT_TAG, false);
        } else
            // end @compat 0.11.3
            this.defaultUnitType = xr.getAttribute(DEFAULT_UNIT_TAG, false);

        movement = xr.getAttribute(MOVEMENT_TAG, parent.movement);

        // @compat 0.11.3
        if (xr.hasAttribute(OLD_LINE_OF_SIGHT_TAG)) {
            lineOfSight = xr.getAttribute(OLD_LINE_OF_SIGHT_TAG, parent.lineOfSight);
        } else
            // end @compat 0.11.3
            lineOfSight = xr.getAttribute(LINE_OF_SIGHT_TAG, parent.lineOfSight);

        // @compat 0.11.3
        if (xr.hasAttribute(OLD_SCORE_VALUE_TAG)) {
            scoreValue = xr.getAttribute(OLD_SCORE_VALUE_TAG, parent.scoreValue);
        } else
            // end @compat 0.11.3
            scoreValue = xr.getAttribute(SCORE_VALUE_TAG, parent.scoreValue);

        space = xr.getAttribute(SPACE_TAG, parent.space);

        // @compat 0.11.3
        if (xr.hasAttribute(OLD_HIT_POINTS_TAG)) {
            hitPoints = xr.getAttribute(OLD_HIT_POINTS_TAG, parent.hitPoints);
        } else
            // end @compat 0.11.3
            hitPoints = xr.getAttribute(HIT_POINTS_TAG, parent.hitPoints);

        // @compat 0.11.3
        if (xr.hasAttribute(OLD_SPACE_TAKEN_TAG)) {
            spaceTaken = xr.getAttribute(OLD_SPACE_TAKEN_TAG, parent.spaceTaken);
        } else
            // end @compat 0.11.3
            spaceTaken = xr.getAttribute(SPACE_TAKEN_TAG, parent.spaceTaken);

        // @compat 0.11.3
        if (xr.hasAttribute(OLD_MAXIMUM_EXPERIENCE_TAG)) {
            maximumExperience = xr.getAttribute(OLD_MAXIMUM_EXPERIENCE_TAG,
                    parent.maximumExperience);
        } else
            // end @compat 0.11.3
            maximumExperience = xr.getAttribute(MAXIMUM_EXPERIENCE_TAG,
                    parent.maximumExperience);

        // @compat 0.11.3
        if (xr.hasAttribute(OLD_MAXIMUM_ATTRITION_TAG)) {
            maximumAttrition = xr.getAttribute(OLD_MAXIMUM_ATTRITION_TAG,
                    parent.maximumAttrition);
        } else
            // end @compat 0.11.3
            maximumAttrition = xr.getAttribute(MAXIMUM_ATTRITION_TAG,
                    parent.maximumAttrition);

        // @compat 0.11.3
        if (xr.hasAttribute(OLD_SKILL_TAUGHT_TAG)) {
            skillTaught = xr.getType(spec, OLD_SKILL_TAUGHT_TAG, UnitType.class, this);
        } else
            // end @compat 0.11.3
            skillTaught = xr.getType(spec, SKILL_TAUGHT_TAG, UnitType.class, this);

        // @compat 0.11.3
        if (xr.hasAttribute(OLD_RECRUIT_PROBABILITY_TAG)) {
            recruitProbability = xr.getAttribute(OLD_RECRUIT_PROBABILITY_TAG,
                    parent.recruitProbability);
        } else
            // end @compat 0.11.3
            recruitProbability = xr.getAttribute(RECRUIT_PROBABILITY_TAG,
                    parent.recruitProbability);

        // New in 0.11.4, but default is backward compatible.
        priority = xr.getAttribute(PRIORITY_TAG, Consumer.UNIT_PRIORITY);

        skill = xr.getAttribute(SKILL_TAG, parent.skill);

        price = xr.getAttribute(PRICE_TAG, parent.price);

        mercenaryPrice = xr.getAttribute(MERCENARY_PRICE_TAG,
                                         parent.mercenaryPrice);
        
        expertProduction = xr.getType(spec, EXPERT_PRODUCTION_TAG,
                GoodsType.class, parent.expertProduction);

        if (parent != this) { // Handle "extends" for super-type fields
            if (!xr.hasAttribute(REQUIRED_POPULATION_TAG)) {
                setRequiredPopulation(parent.getRequiredPopulation());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        final Specification spec = getSpecification();

        // Clear containers.
        if (xr.shouldClearContainers()) {
            consumption = null;
        }
        defaultRole = spec.getDefaultRole();

        UnitType parent = xr.getType(spec, EXTENDS_TAG, UnitType.class, this);
        if (parent != this) {
            defaultRole = parent.defaultRole;

            if (parent.consumption != null) {
                if (consumption == null) consumption = new TypeCountMap<>();
                consumption.putAll(parent.consumption);
            }

            addFeatures(parent);
            if (parent.isAbstractType()) {
                getFeatureContainer().replaceSource(parent, this);
            }
        }

        super.readChildren(xr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final Specification spec = getSpecification();
        final String tag = xr.getLocalName();

        if (CONSUMES_TAG.equals(tag)) {
            addConsumption(xr.getType(spec, ID_ATTRIBUTE_TAG,
                    GoodsType.class, (GoodsType)null),
                    xr.getAttribute(VALUE_TAG, UNDEFINED));
            xr.closeTag(CONSUMES_TAG);

        // @compat 0.11.0
        } else if (OLD_DEFAULT_EQUIPMENT_TAG.equals(tag)) {
            xr.swallowTag(OLD_DEFAULT_EQUIPMENT_TAG);
        // end @compat 0.11.0

        } else if (DEFAULT_ROLE_TAG.equals(tag)) {
            defaultRole = xr.getType(spec, ID_ATTRIBUTE_TAG,
                    Role.class, spec.getDefaultRole());
            xr.closeTag(DEFAULT_ROLE_TAG);

        // @compat 0.11.6
        } else if (DOWNGRADE_TAG.equals(tag) || UPGRADE_TAG.equals(tag)) {
            xr.closeTag(tag, Scope.TAG);
        // end @compat 0.11.6

        } else {
            super.readChild(xr);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return TAG; }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getId();
    }
}
