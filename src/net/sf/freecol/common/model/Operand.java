/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.util.Utils;


/**
 * The <code>Operand</code> class implements Operands to be used in
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
     * Creates a new <code>Operand</code> instance.
     *
     * @param value The initial operand value.
     */
    public Operand(int value) {
        this.value = value;
    }

    /**
     * Creates a new <code>Operand</code> instance.
     *
     * @param operandType The <code>OperandType</code> to use.
     * @param scopeLevel The <code>ScopeLevel</code> to use.
     */
    public Operand(OperandType operandType, ScopeLevel scopeLevel) {
        this.operandType = operandType;
        this.scopeLevel = scopeLevel;
    }

    /**
     * Create a new operand by reading a stream.
     *
     * @param xr The <code>FreeColXMLReader</code> to read.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    protected Operand(FreeColXMLReader xr) throws XMLStreamException {
        readFromXML(xr);
    }


    /**
     * Gets the operand type.
     *
     * @return The <code>OperandType</code>.
     */
    public final OperandType getOperandType() {
        return operandType;
    }

    /**
     * Set the operand type.
     *
     * @param newOperandType The new <code>OperandType</code>.
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
        return scopeLevel;
    }

    /**
     * Sets the scope level.
     *
     * @param newScopeLevel The new <code>ScopeLevel</code>.
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
        return value;
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
     * Gets the operand value if it is applicable to the given Game.
     *
     * @param game The <code>Game</code> to check.
     * @return The operand value or null if inapplicable.
     */
    public Integer getValue(Game game) {
        return (value != null) ? value
            : (scopeLevel == ScopeLevel.GAME) ? calculateGameValue(game)
            : null;
    }

    /**
     * Calculate the operand value within a given game.
     *
     * @param game The <code>Game</code> to check.
     * @return The operand value.
     */
    private Integer calculateGameValue(Game game) {
        final String methodName = getMethodName();
        switch (operandType) {
        case NONE:
            return game.invokeMethod(methodName, Integer.class, 0);
        case YEAR:
            return game.getTurn().getYear();
        case OPTION:
            return game.getSpecification().getInteger(getType());
        default:
            List<FreeColObject> list = new LinkedList<>();
            for (Player player : game.getLivePlayers(null)) {
                switch (operandType) {
                case UNITS:
                    list.addAll(player.getUnits());
                    break;
                case BUILDINGS:
                    for (Colony colony : player.getColonies()) {
                        list.addAll(colony.getBuildings());
                    }
                    break;
                case SETTLEMENTS:
                    list.addAll(player.getSettlements());
                    break;
                case FOUNDING_FATHERS:
                    list.addAll(player.getFathers());
                    break;
                default:
                    return null;
                }
            }
            return count(list);
        }
    }

    /**
     * Gets the operand value if it is applicable to the given Player.
     *
     * @param player The <code>Player</code> to check.
     * @return The operand value, or null if inapplicable.
     */
    public Integer getValue(Player player) {
        if (value != null) return value;
        switch (scopeLevel) {
        case GAME:
            return getValue(player.getGame());
        case PLAYER: // Real case, handled below
            break;
        default: // Inapplicable
            return null;
        }

        final Specification spec = player.getSpecification();
        final String methodName = getMethodName();
        List<FreeColObject> list = new LinkedList<>();
        switch (operandType) {
        case UNITS:
            return count(player.getUnits());
        case BUILDINGS:
            for (Colony colony : player.getColonies()) {
                list.addAll(colony.getBuildings());
            }
            return count(list);
        case SETTLEMENTS:
            if (methodName == null) {
                return count(player.getSettlements())
                    + spec.getInteger(GameOptions.SETTLEMENT_LIMIT_MODIFIER);
            } else {
                final String methodValue = getMethodValue();
                int result = 0;
                for (Settlement settlement : player.getSettlements()) {
                    Boolean b = settlement.invokeMethod(methodName,
                        Boolean.class, Boolean.FALSE);
                    if (String.valueOf(b).equals(methodValue)) result++;
                }
                return result;
            }
        case FOUNDING_FATHERS:
            list.addAll(player.getFathers());
            return count(list);
        default:
            return player.invokeMethod(methodName, Integer.class,
                                       (Integer)null);
        }
    }

    /**
     * Gets the operand value if it is applicable to the given Settlement.
     *
     * @param settlement The <code>Settlement</code> to check.
     * @return The operand value, or null if inapplicable.
     */
    public Integer getValue(Settlement settlement) {
        if (value == null) {
            if (scopeLevel == ScopeLevel.SETTLEMENT
                && settlement instanceof Colony) {
                Colony colony = (Colony) settlement;
                List<FreeColObject> list = new LinkedList<>();
                switch(operandType) {
                case UNITS:
                    list.addAll(colony.getUnitList());
                    break;
                case BUILDINGS:
                    list.addAll(colony.getBuildings());
                    break;
                default:
                    return colony.invokeMethod(getMethodName(), Integer.class,
                                               (Integer)null);
                }
                return count(list);
            } else {
                // in future, we might expand this to handle native
                // settlements
                return null;
            }
        } else {
            return value;
        }
    }

    /**
     * Count the number of objects in a list that this operand is
     * applicable to.
     *
     * @param objects The list of objects to check.
     * @return The number of applicable objects.
     */
    private int count(List<? extends FreeColObject> objects) {
        int result = 0;
        for (FreeColObject object : objects) {
            if (appliesTo(object)) {
                result++;
            }
        }
        return result;
    }


    // Interface Object

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        return this == o
            || (o instanceof Operand
                && operandType == ((Operand)o).operandType
                && scopeLevel == ((Operand)o).scopeLevel
                && Utils.equals(value, ((Operand)o).value)
                && super.equals(o));
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

        xw.writeAttribute(OPERAND_TYPE_TAG, operandType);

        xw.writeAttribute(SCOPE_LEVEL_TAG, scopeLevel);

        if (value != null) {
            xw.writeAttribute(VALUE_TAG, value);
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
            operandType = xr.getAttribute(OLD_OPERAND_TYPE_TAG,
                                          OperandType.class, OperandType.NONE);
        } else            
        // end @compat 0.11.3
            operandType = xr.getAttribute(OPERAND_TYPE_TAG,
                                          OperandType.class, OperandType.NONE);

        // @compat 0.11.3
        if (xr.hasAttribute(OLD_SCOPE_LEVEL_TAG)) {
            scopeLevel = xr.getAttribute(OLD_SCOPE_LEVEL_TAG,
                                         ScopeLevel.class, ScopeLevel.NONE);
        } else
        // end @compat 0.11.3
            scopeLevel = xr.getAttribute(SCOPE_LEVEL_TAG,
                                         ScopeLevel.class, ScopeLevel.NONE);

        int val = xr.getAttribute(VALUE_TAG, INFINITY);
        if (val != INFINITY) value = val;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        if (value != null) return Integer.toString(value);
        StringBuffer sb = new StringBuffer();
        sb.append("[Operand type=").append(operandType)
            .append(" scopeLevel=").append(scopeLevel);
        return super.toString().replaceFirst("^[^ ]*", sb.toString());
    }

    // getXMLElementTagName apparently not needed, uses parents.
}
