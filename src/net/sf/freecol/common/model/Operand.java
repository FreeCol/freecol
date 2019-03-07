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
 *  MERCHANTLIMIT or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.common.model;

import java.util.Collection;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.option.GameOptions;
import static net.sf.freecol.common.model.Constants.*;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.common.util.Utils;


/**
 * The {@code Operand} class implements Operands to be used in
 * relations, such as the Limit class. The OperandType specifies which
 * types of objects will be considered, and the ScopeLevel specifies
 * the level at which these objects are to be selected. If the
 * ScopeLevel is PLAYER, for example, and the OperandType is UNITS,
 * then all units owned by a particular player will be considered.
 *
 * Since the class inherits from Scope, the choice of objects can be
 * further refined by specifying type, ability or method. However, the
 * return value of the method must be an Integer (or int), since this
 * value will be returned as the value of the Operand itself if the
 * OperandType is NONE.
 */
public class Operand extends Scope {

    private static final Logger logger = Logger.getLogger(Operand.class.getName());

    public static enum OperandType {
        UNITS, BUILDINGS, SETTLEMENTS, FOUNDING_FATHERS, YEAR, OPTION, NONE
    }

    public static enum ScopeLevel {
        SETTLEMENT, PLAYER, GAME, NONE
    }


    /** The type of object the operand really represents. */
    private OperandType operandType = OperandType.NONE;

    /** How broadly to apply the operand. */
    private ScopeLevel scopeLevel = ScopeLevel.NONE;

    /** The operand amount. */
    private Integer value = null;


    /**
     * Deliberately empty constructor.
     */
    public Operand() {}

    /**
     * Creates a new {@code Operand} instance.
     *
     * @param value The initial operand value.
     */
    public Operand(int value) {
        this.value = value;
    }

    /**
     * Creates a new {@code Operand} instance.
     *
     * @param operandType The {@code OperandType} to use.
     * @param scopeLevel The {@code ScopeLevel} to use.
     */
    public Operand(OperandType operandType, ScopeLevel scopeLevel) {
        this.operandType = operandType;
        this.scopeLevel = scopeLevel;
    }

    /**
     * Create a new operand by reading a stream.
     *
     * @param xr The {@code FreeColXMLReader} to read.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    protected Operand(FreeColXMLReader xr) throws XMLStreamException {
        readFromXML(xr);
    }


    /**
     * Gets the operand type.
     *
     * @return The {@code OperandType}.
     */
    public final OperandType getOperandType() {
        return this.operandType;
    }

    /**
     * Set the operand type.
     *
     * @param newOperandType The new {@code OperandType}.
     */
    public final void setOperandType(final OperandType newOperandType) {
        this.operandType = newOperandType;
    }

    /**
     * Gets the scope level.
     *
     * @return The scope level.
     */
    public final ScopeLevel getScopeLevel() {
        return this.scopeLevel;
    }

    /**
     * Sets the scope level.
     *
     * @param newScopeLevel The new {@code ScopeLevel}.
     */
    public final void setScopeLevel(final ScopeLevel newScopeLevel) {
        this.scopeLevel = newScopeLevel;
    }

    /**
     * Gets the operand value.
     *
     * @return The operand value.
     */
    public final Integer getValue() {
        return this.value;
    }

    /**
     * Sets the operand value.
     *
     * @param newValue The new value.
     */
    public final void setValue(final Integer newValue) {
        this.value = newValue;
    }

    /**
     * Count the number of objects in a list that this operand is
     * applicable to.
     *
     * @param objects The list of objects to check.
     * @return The number of applicable objects.
     */
    private Integer ourCount(Collection<? extends FreeColObject> objects) {
        return count(objects, o -> this.appliesTo(o));
    }

    /**
     * Gets the operand value if it is applicable to the given Game.
     *
     * @param game The {@code Game} to check.
     * @return The operand value or null if inapplicable.
     */
    public Integer getValue(Game game) {
        if (this.value != null) return this.value;
        if (this.scopeLevel != ScopeLevel.GAME) return null;

        final String methodName = getMethodName();
        int result = 0;
        switch (this.operandType) {
        case NONE:
            result = game.invokeMethod(methodName, Integer.class, 0);
            break;
        case YEAR:
            result = game.getTurn().getYear();
            break;
        case OPTION:
            result = game.getSpecification().getInteger(getType());
            break;
        default:
            for (Player player : game.getLivePlayerList()) {
                switch (this.operandType) {
                case UNITS:
                    result += ourCount(player.getUnitSet());
                    break;
                case BUILDINGS:
                    result += sum(player.getColonies(),
                                  c -> ourCount(c.getBuildings()));
                    break;
                case SETTLEMENTS:
                    result += ourCount(player.getSettlementList());
                    break;
                case FOUNDING_FATHERS:
                    result += ourCount(player.getFoundingFathers());
                    break;
                default:
                    return null;
                }
            }
        }
        return result;
    }

    /**
     * Gets the operand value if it is applicable to the given Player.
     *
     * @param player The {@code Player} to check.
     * @return The operand value, or null if inapplicable.
     */
    public Integer getValue(Player player) {
        if (this.value != null) return this.value;
        switch (this.scopeLevel) {
        case GAME:
            return getValue(player.getGame());
        case PLAYER: // Real case, handled below
            break;
        default: // Inapplicable
            return null;
        }

        final Specification spec = player.getSpecification();
        final String methodName = getMethodName();
        switch (this.operandType) {
        case UNITS:
            return ourCount(player.getUnitSet());
        case BUILDINGS:
            return sum(player.getColonies(), c -> ourCount(c.getBuildings()));
        case SETTLEMENTS:
            if (methodName == null) {
                return ourCount(player.getSettlementList())
                    + spec.getInteger(GameOptions.SETTLEMENT_LIMIT_MODIFIER);
            }
            final String methodValue = getMethodValue();
            return count(player.getSettlementList(),
                s -> String.valueOf(s.invokeMethod(methodName,
                        Boolean.class, Boolean.FALSE)).equals(methodValue));
        case FOUNDING_FATHERS:
            return ourCount(player.getFoundingFathers());
        default:
            break;
        }
        return player.invokeMethod(methodName, Integer.class, (Integer)null);
    }

    /**
     * Gets the operand value if it is applicable to the given Settlement.
     *
     * @param settlement The {@code Settlement} to check.
     * @return The operand value, or null if inapplicable.
     */
    public Integer getValue(Settlement settlement) {
        if (this.value != null) return this.value;
        // In future, we might expand this to handle native settlements
        if (this.scopeLevel != ScopeLevel.SETTLEMENT
            || !(settlement instanceof Colony)) return null;

        final Colony colony = (Colony)settlement;
        switch (this.operandType) {
        case UNITS:
            return ourCount(colony.getUnitList());
        case BUILDINGS:
            return ourCount(colony.getBuildings());
        default:
            break;
        }
        return colony.invokeMethod(getMethodName(), Integer.class, (Integer)null);
    }


    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends FreeColObject> boolean copyIn(T other) {
        Operand o = copyInCast(other, Operand.class);
        if (o == null || !super.copyIn(o)) return false;
        this.operandType = o.getOperandType();
        this.scopeLevel = o.getScopeLevel();
        this.value = o.getValue();
        return true;
    }

        
    // Serialization

    private static final String OPERAND_TYPE_TAG = "operand-type";
    private static final String SCOPE_LEVEL_TAG = "scope-level";
    // @compat 0.11.3
    private static final String OLD_OPERAND_TYPE_TAG = "operandType";
    private static final String OLD_SCOPE_LEVEL_TAG = "scopeLevel";
    // end @compat 0.11.3


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(OPERAND_TYPE_TAG, this.operandType);

        xw.writeAttribute(SCOPE_LEVEL_TAG, this.scopeLevel);

        if (this.value != null) {
            xw.writeAttribute(VALUE_TAG, this.value);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        // @compat 0.11.3
        if (xr.hasAttribute(OLD_OPERAND_TYPE_TAG)) {
            this.operandType = xr.getAttribute(OLD_OPERAND_TYPE_TAG,
                OperandType.class, OperandType.NONE);
        } else            
        // end @compat 0.11.3
            this.operandType = xr.getAttribute(OPERAND_TYPE_TAG,
                OperandType.class, OperandType.NONE);

        // @compat 0.11.3
        if (xr.hasAttribute(OLD_SCOPE_LEVEL_TAG)) {
            this.scopeLevel = xr.getAttribute(OLD_SCOPE_LEVEL_TAG,
                ScopeLevel.class, ScopeLevel.NONE);
        } else
        // end @compat 0.11.3
            this.scopeLevel = xr.getAttribute(SCOPE_LEVEL_TAG,
                ScopeLevel.class, ScopeLevel.NONE);

        int val = xr.getAttribute(VALUE_TAG, INFINITY);
        if (val != INFINITY) this.value = val;
    }

    // getTagName apparently not needed, uses parents


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof Operand) {
            Operand other = (Operand)o;
            return this.operandType == other.operandType
                && this.scopeLevel == other.scopeLevel
                && Utils.equals(this.value, other.value)
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
        hash = 31 * hash + this.operandType.ordinal();
        hash = 31 * hash + this.scopeLevel.ordinal();
        hash = 31 * hash + this.value;
        return hash;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        if (this.value != null) return Integer.toString(value);
        StringBuilder sb = new StringBuilder();
        sb.append("[Operand type=").append(this.operandType)
            .append(" scopeLevel=").append(this.scopeLevel);
        return super.toString().replaceFirst("^[^ ]*", sb.toString());
    }
}
