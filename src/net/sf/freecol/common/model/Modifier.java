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

import java.util.Comparator;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.util.Utils;


/**
 * The {@code Modifier} class encapsulates a bonus or penalty
 * that can be applied to any action within the game, most obviously
 * combat. The Modifier may be applicable only to certain Objects
 * specified by means of {@code Scope} objects.
 */
public class Modifier extends Feature {

    public static final String TAG = "modifier";
    
    /**
     * Comparator to sort by ascending modifier index, then type, then
     * source, then FCO order.
     */
    public static final Comparator<Modifier> ascendingModifierIndexComparator
        = Comparator.<Modifier>comparingInt(Modifier::getModifierIndex)
            .thenComparingInt(m -> m.getType().ordinal())
            .thenComparing(FreeColObject.fcoComparator)
            .thenComparing(Modifier::getSource, FreeColObject.fcoComparator);
    
    public static final String AMPHIBIOUS_ATTACK
        = "model.modifier.amphibiousAttack";
    public static final String ARTILLERY_AGAINST_RAID
        = "model.modifier.artilleryAgainstRaid";
    public static final String ARTILLERY_IN_THE_OPEN
        = "model.modifier.artilleryInTheOpen";
    public static final String ATTACK_BONUS
        = "model.modifier.attackBonus";
    public static final String BIG_MOVEMENT_PENALTY
        = "model.modifier.bigMovementPenalty";
    public static final String BOMBARD_BONUS
        = "model.modifier.bombardBonus";
    public static final String BREEDING_DIVISOR
        = "model.modifier.breedingDivisor";
    public static final String BREEDING_FACTOR
        = "model.modifier.breedingFactor";
    public static final String BUILDING_PRICE_BONUS
        = "model.modifier.buildingPriceBonus";
    public static final String CARGO_PENALTY
        = "model.modifier.cargoPenalty";
    public static final String COLONY_GOODS_PARTY
        = "model.modifier.colonyGoodsParty";
    public static final String CONSUME_ONLY_SURPLUS_PRODUCTION
        = "model.modifier.consumeOnlySurplusProduction";
    public static final String CONVERSION_ALARM_RATE
        = "model.modifier.conversionAlarmRate";
    public static final String CONVERSION_SKILL
        = "model.modifier.conversionSkill";
    public static final String DEFENCE
        = "model.modifier.defence";
    public static final String EXPLORE_LOST_CITY_RUMOUR
        = "model.modifier.exploreLostCityRumour";
    public static final String EXPOSED_TILES_RADIUS
        = "model.modifier.exposedTilesRadius";
    public static final String FORTIFIED
        = "model.modifier.fortified";
    public static final String IMMIGRATION
        = "model.modifier.immigration";
    public static final String LAND_PAYMENT_MODIFIER
        = "model.modifier.landPaymentModifier";
    public static final String LIBERTY
        = "model.modifier.liberty";
    public static final String LINE_OF_SIGHT_BONUS
        = "model.modifier.lineOfSightBonus";
    public static final String MINIMUM_COLONY_SIZE
        = "model.modifier.minimumColonySize";
    public static final String MISSIONARY_TRADE_BONUS
        = "model.modifier.missionaryTradeBonus";
    public static final String MOVEMENT_BONUS
        = "model.modifier.movementBonus";
    public static final String NATIVE_ALARM_MODIFIER
        = "model.modifier.nativeAlarmModifier";
    public static final String NATIVE_CONVERT_BONUS
        = "model.modifier.nativeConvertBonus";
    public static final String OFFENCE
        = "model.modifier.offence";
    public static final String OFFENCE_AGAINST
        = "model.modifier.offenceAgainst";
    public static final String PEACE_TREATY
        = "model.modifier.peaceTreaty";
    public static final String POPULAR_SUPPORT
        = "model.modifier.popularSupport";
    public static final String RELIGIOUS_UNREST_BONUS
        = "model.modifier.religiousUnrestBonus";
    public static final String SAIL_HIGH_SEAS
        = "model.modifier.sailHighSeas";
    public static final String SHIP_TRADE_PENALTY
        = "model.modifier.shipTradePenalty";
    public static final String SMALL_MOVEMENT_PENALTY
        = "model.modifier.smallMovementPenalty";
    public static final String SOL
        = "model.modifier.SoL";
    public static final String TILE_TYPE_CHANGE_PRODUCTION
        = "model.modifier.tileTypeChangeProduction";
    public static final String TRADE_BONUS
        = "model.modifier.tradeBonus";
    public static final String TRADE_VOLUME_PENALTY
        = "model.modifier.tradeVolumePenalty";
    public static final String TREASURE_TRANSPORT_FEE
        = "model.modifier.treasureTransportFee";
    public static final String WAREHOUSE_STORAGE
        = "model.modifier.warehouseStorage";

    public static final float UNKNOWN = Float.MIN_VALUE;

    public static final int DEFAULT_MODIFIER_INDEX = 0;

    // These are now attached to modifiers in the spec and mostly
    // should not be needed any more.  However the dynamic modifiers
    // (e.g. disaster, goods party related) do still need to be built
    // explicitly with the right index.  So we might as well leave
    // them all visible here for clarity.
    public static final int RESOURCE_PRODUCTION_INDEX = 10;
    public static final int COLONY_PRODUCTION_INDEX = 20;
    public static final int EXPERT_PRODUCTION_INDEX = 30;
    public static final int FATHER_PRODUCTION_INDEX = 40;
    public static final int IMPROVEMENT_PRODUCTION_INDEX = 50;
    public static final int AUTO_PRODUCTION_INDEX = 60;
    public static final int BUILDING_PRODUCTION_INDEX = 70;
    public static final int NATION_PRODUCTION_INDEX = 80;
    public static final int PARTY_PRODUCTION_INDEX = 90;
    public static final int DISASTER_PRODUCTION_INDEX = 100;
    public static final int DEFAULT_PRODUCTION_INDEX = 100;

    // Specific combat indicies
    public static final int BASE_COMBAT_INDEX = 10;
    public static final int UNIT_ADDITIVE_COMBAT_INDEX = 20;
    public static final int UNIT_NORMAL_COMBAT_INDEX = 40;
    public static final int ROLE_COMBAT_INDEX = 30;
    public static final int GENERAL_COMBAT_INDEX = 50;

    public static enum ModifierType {
        ADDITIVE,
        MULTIPLICATIVE,
        PERCENTAGE
    }

    /** The type of this Modifier. */
    private ModifierType modifierType;

    /** The value of this Modifier. */
    private float value;

    /**
     * The value increments per turn.  This can be used to create
     * Modifiers whose values increase or decrease over time.
     */
    private float increment;

    /** The type of increment. */
    private ModifierType incrementType;

    /** A sorting index. */
    private int modifierIndex = DEFAULT_MODIFIER_INDEX;


    /**
     * Deliberately empty constructor.
     *
     * @param specification The defining {@code Specification}.
     */
    public Modifier(Specification specification) {
        super(specification);
    }

    /**
     * Creates a new {@code Modifier} instance.
     *
     * @param id The object identifier.
     * @param value The modifier value.
     * @param type The type of the modifier.
     * @param source The source {@code FreeColObject}.
     * @param modifierIndex The modifier index.
     */
    public Modifier(String id, float value, ModifierType type,
                    FreeColObject source, int modifierIndex) {
        this((source == null) ? null : source.getSpecification());

        setId(id);
        setValue(value);
        setType(type);
        setSource(source);
        setModifierIndex(modifierIndex);
    }

    /**
     * Creates a new {@code Modifier} instance.
     *
     * @param id The object identifier.
     * @param value The modifier value.
     * @param type The type of the modifier.
     * @param source The source {@code FreeColObject}.
     */
    public Modifier(String id, float value, ModifierType type,
                    FreeColObject source) {
        this(id, value, type, source, DEFAULT_MODIFIER_INDEX);
    }

    /**
     * Creates a new {@code Modifier} instance.
     *
     * @param id The object identifier.
     * @param value The modifier value.
     * @param type The type of the modifier.
     */
    public Modifier(String id, float value, ModifierType type) {
        this(id, value, type, null);
    }

    /**
     * Creates a new {@code Modifier} instance.
     *
     * @param xr The {@code FreeColXMLReader} to read from.
     * @param specification The {@code Specification} to refer to.
     * @exception XMLStreamException if there is an error reading the
     *     stream.
     */
    public Modifier(FreeColXMLReader xr, Specification specification) throws XMLStreamException {
        this(specification);

        readFromXML(xr);
    }


    /**
     * Make a copy of a modifier.
     *
     * @param modifier The {@code Modifier} to copy.
     * @return A copy of the modifier.
     */
    public static Modifier makeModifier(Modifier modifier) {
        Modifier ret = new Modifier(modifier.getSpecification());
        ret.setId(modifier.getId());
        ret.copyIn(modifier);
        return ret;
    }

    /**
     * Makes a timed modifier (one with start/end turn and increment)
     * with the specified identifier from a template modifier
     * (containing the increment and value) and given start turn.
     *
     * Currently the only suitable template is
     * "model.modifier.colonyGoodsParty".
     *
     * @param id The id for the new modifier.
     * @param template A template {@code Modifier} with increment.
     * @param start The starting {@code Turn}.
     * @return A new timed modifier.
     */
    public static Modifier makeTimedModifier(String id, Modifier template,
                                             Turn start) {
        Modifier modifier = makeModifier(template);
        modifier.setId(id);
        float inc = template.getIncrement();
        int duration = template.getDuration();
        modifier.setTemporary(template.isTemporary());
        // FIXME: this only works for additive modifiers
        if (duration == 0) {
            duration = (int)(template.getValue()/-inc);
        }
        modifier.setIncrement(template.getIncrementType(), inc, start,
                              new Turn(start.getNumber() + duration));
        return modifier;
    }

    /**
     * Get the modifier type.
     *
     * @return The {@code ModifierType}.
     */
    public final ModifierType getType() {
        return modifierType;
    }

    /**
     * Set the modifier type.
     *
     * @param modifierType The new {@code ModifierType} value.
     * @return This modifier.
     */
    public final Modifier setType(final ModifierType modifierType) {
        this.modifierType = modifierType;
        return this;
    }

    /**
     * Is this a "known" value.
     *
     * @param value The value to test.
     * @return True if the value is known.
     */
    public static boolean isFloatKnown(float value) {
        return Float.compare(value, UNKNOWN) != 0;
    }

    /**
     * Does this modifier have a known value?
     *
     * That is, is it not set to the UNKNOWN value.
     *
     * @return True if the value is known.
     */
    public boolean isKnown() {
        return Modifier.isFloatKnown(this.value);
    }

    /**
     * Get the modifier value.
     *
     * @return The modifier value.
     */
    public final float getValue() {
        return this.value;
    }

    /**
     * Get the value the modifier during the given Turn.
     *
     * @param turn The {@code Turn} to check.
     * @return The turn-dependent modifier value.
     */
    public final float getValue(Turn turn) {
        if (appliesTo(turn)) {
            if (hasIncrement()) {
                float f = (turn.getNumber() - getFirstTurn().getNumber())
                    * increment;
                return apply(this.value, f, incrementType);
            } else {
                return this.value;
            }
        } else {
            return 0;
        }
    }

    /**
     * Set the modifier value.
     *
     * @param value The new value.
     * @return This modifier.
     */
    public final Modifier setValue(final float value) {
        this.value = value;
        return this;
    }

    /**
     * Does this modifier have an increment?
     *
     * @return True if this modifier has an increment.
     */
    public final boolean hasIncrement() {
        return incrementType != null;
    }

    /**
     * Get the increment type.
     *
     * @return The increment {@code ModifierType}.
     */
    public final ModifierType getIncrementType() {
        return incrementType;
    }

    /**
     * Set the increment type.
     *
     * @param incrementType The new increment {@code ModifierType}.
     * @return This modifier.
     */
    public final Modifier setIncrementType(final ModifierType incrementType) {
        this.incrementType = incrementType;
        return this;
    }

    /**
     * Get the increment value.
     *
     * @return The increment value.
     */
    public final float getIncrement() {
        return increment;
    }

    /**
     * Set the increment value.
     *
     * @param increment The new value.
     * @return This modifier.
     */
    public final Modifier setIncrement(final float increment) {
        this.increment = increment;
        return this;
    }

    /**
     * Set the whole increment.
     *
     * @param incrementType The new {@code ModifierType}.
     * @param increment The new increment value.
     * @param firstTurn The first {@code Turn} the increment is
     *     active.
     * @param lastTurn The last {@code Turn} the increment is
     *     active.
     * @return This modifier.
     */
    public final Modifier setIncrement(final ModifierType incrementType,
                                       final float increment,
                                       Turn firstTurn, Turn lastTurn) {
        if (firstTurn == null) {
            throw new RuntimeException("Null firstTurn: " + this);
        }
        this.incrementType = incrementType;
        this.increment = increment;
        setFirstTurn(firstTurn);
        setLastTurn(lastTurn);
        return this;
    }

    /**
     * Get the modifier index.
     *
     * @return The modifier index.
     */
    public final int getModifierIndex() {
        return modifierIndex;
    }

    /**
     * Set the modifier index.
     *
     * @param modifierIndex The new modifier index value.
     * @return This modifier.
     */
    public final Modifier setModifierIndex(final int modifierIndex) {
        this.modifierIndex = modifierIndex;
        return this;
    }

    /**
     * Applies the given value to the given base value, depending on
     * the type of this Modifier.
     *
     * @param base a {@code float} value
     * @param value a {@code float} value
     * @return a {@code float} value
     */
    public float apply(float base, float value) {
        return apply(base, value, getType());
    }

    /**
     * Applies the given value to the given base value, depending on
     * the give modifier Type.
     *
     * @param base The base value.
     * @param value The modifier value.
     * @param type The {@code ModifierType}.
     * @return The result of applying the value to the base.
     */
    private float apply(float base, float value, ModifierType type) {
        switch (type) {
        case ADDITIVE:
            return base + value;
        case MULTIPLICATIVE:
            return base * value;
        case PERCENTAGE:
            return base + (base * value) / 100;
        default:
            return base;
        }
    }

    /**
     * Applies this Modifier to a number. This method does not take
     * scopes, increments or time limits into account.
     *
     * @param number a {@code float} value
     * @return a {@code float} value
     */
    public float applyTo(float number) {
        return apply(number, value);
    }

    /**
     * Applies this Modifier to a number.  This method does take
     * increments into account.
     *
     * @param number The number to modify.
     * @param turn The {@code Turn} to evaluate increments in.
     * @return The modified number.
     */
    public float applyTo(float number, Turn turn) {
        return (incrementType == null) ? apply(number, value)
            : apply(number, getValue(turn), getType());
    }


    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends FreeColObject> boolean copyIn(T other) {
        Modifier o = copyInCast(other, Modifier.class);
        if (o == null || !super.copyIn(o)) return false;
        this.setType(o.getType());
        this.setValue(o.getValue());
        this.setIncrement(o.getIncrement());
        this.setIncrementType(o.getIncrementType());
        this.setModifierIndex(o.getModifierIndex());
        return true;
    }


    // Serialization

    private static final String INCREMENT_TAG = "increment";
    private static final String INCREMENT_TYPE_TAG = "increment-type";
    private static final String INDEX_TAG = "index";
    private static final String TYPE_TAG = "type";
    // @compat 0.11.3
    private static final String OLD_INCREMENT_TYPE_TAG = "incrementType";
    // end @compat 0.11.3


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(VALUE_TAG, value);

        xw.writeAttribute(TYPE_TAG, modifierType);

        if (incrementType != null) {
            xw.writeAttribute(INCREMENT_TYPE_TAG, incrementType);

            xw.writeAttribute(INCREMENT_TAG, increment);
        }

        if (modifierIndex >= 0) {
            xw.writeAttribute(INDEX_TAG, modifierIndex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        modifierType = xr.getAttribute(TYPE_TAG, ModifierType.class,
                                       (ModifierType)null);

        value = xr.getAttribute(VALUE_TAG, UNKNOWN);

        // @compat 0.11.3
        if (xr.hasAttribute(OLD_INCREMENT_TYPE_TAG)) {
            incrementType = xr.getAttribute(OLD_INCREMENT_TYPE_TAG,
                                            ModifierType.class,
                                            (ModifierType)null);
            increment = xr.getAttribute(INCREMENT_TAG, UNKNOWN);
        // end @compat 0.11.3
        } else if (xr.hasAttribute(INCREMENT_TYPE_TAG)) {
            incrementType = xr.getAttribute(INCREMENT_TYPE_TAG,
                                            ModifierType.class,
                                            (ModifierType)null);

            increment = xr.getAttribute(INCREMENT_TAG, UNKNOWN);
        } else {
            incrementType = null;
            increment = 0;
        }

        modifierIndex = xr.getAttribute(INDEX_TAG, DEFAULT_MODIFIER_INDEX);
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
    public boolean equals(Object o) {
        if (o == this) return true;
        if (o instanceof Modifier) { 
            Modifier other = (Modifier)o;
            return Float.compare(this.value, other.value) == 0
                && Float.compare(this.increment, other.increment) == 0
                && this.modifierIndex == other.modifierIndex
                && Utils.equals(this.modifierType, other.modifierType)
                && Utils.equals(this.incrementType, other.incrementType)
                && super.equals(other);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 31 * hash + Float.floatToIntBits(value);
        hash = 31 * hash + Float.floatToIntBits(increment);
        hash = 31 * hash + Utils.hashCode(modifierType);
        hash = 31 * hash + Utils.hashCode(incrementType);
        return 31 * hash + modifierIndex;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(64);
        sb.append("[Modifier ").append(getId());
        if (getSource() != null) {
            sb.append(" (").append(getSource().getId()).append(')');
        }
        sb.append(' ').append(modifierType)
            .append(' ').append(value);
        if (modifierIndex >= DEFAULT_MODIFIER_INDEX) {
            sb.append(" index=").append(modifierIndex);
        }
        List<Scope> scopes = getScopeList();
        if (!scopes.isEmpty()) {
            sb.append(" [");
            for (Scope s : scopes) sb.append(' ').append(s);
            sb.append(" ]");
        }
        sb.append(']');
        return sb.toString();
    }
}
