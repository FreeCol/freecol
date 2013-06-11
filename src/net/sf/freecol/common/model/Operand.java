/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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

import java.lang.reflect.Method;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;


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
        if (value == null) {
            if (scopeLevel == ScopeLevel.GAME){
                return calculateGameValue(game);
            } else {
                return null;
            }
        } else {
            return value;
        }
    }

    /**
     * Calculate the operand value within a given game.
     *
     * @param game The <code>Game</code> to check.
     * @return The operand value.
     */
    private Integer calculateGameValue(Game game) {
        switch (operandType) {
        case NONE:
            if (getMethodName() != null) {
                try {
                    Method method = game.getClass().getMethod(getMethodName());
                    if (method != null &&
                        Integer.class.isAssignableFrom(method.getReturnType())) {
                        return (Integer) method.invoke(game);
                    }
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Unable to invoke: "
                        + getMethodName(), e);
                }
            }
            return null;
        case YEAR:
            return game.getTurn().getYear();
        case OPTION:
            return game.getSpecification().getInteger(getType());
        default:
            List<FreeColObject> list = new LinkedList<FreeColObject>();
            for (Player player : game.getPlayers()) {
                switch(operandType) {
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
        if (value == null) {
            if (scopeLevel == ScopeLevel.PLAYER) {
                List<FreeColObject> list = new LinkedList<FreeColObject>();
                switch(operandType) {
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
                    if (getMethodName() != null) {
                        try {
                            Method method = player.getClass().getMethod(getMethodName());
                            if (method != null
                                && (int.class.equals(method.getReturnType())
                                    || Integer.class.equals(method.getReturnType()))) {
                                return (Integer) method.invoke(player);
                            }
                        } catch (Exception e) {
                            logger.log(Level.WARNING, "Unable to invoke: "
                                + getMethodName(), e);
                            return null;
                        }
                    }
                    return null;
                }
                return count(list);
            } else if (scopeLevel == ScopeLevel.GAME) {
                return getValue(player.getGame());
            } else {
                return null;
            }
        } else {
            return value;
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
                List<FreeColObject> list = new LinkedList<FreeColObject>();
                switch(operandType) {
                case UNITS:
                    list.addAll(colony.getUnitList());
                    break;
                case BUILDINGS:
                    list.addAll(colony.getBuildings());
                    break;
                default:
                    if (getMethodName() != null) {
                        try {
                            Method method = colony.getClass().getMethod(getMethodName());
                            if (method != null &&
                                Integer.class.isAssignableFrom(method.getReturnType())) {
                                return (Integer) method.invoke(colony);
                            }
                        } catch (Exception e) {
                            logger.log(Level.WARNING, "Unable to invoke: "
                                + getMethodName(), e);
                            return null;
                        }
                    }
                    return null;
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
    private int count(List<FreeColObject> objects) {
        int result = 0;
        for (FreeColObject object : objects) {
            if (appliesTo(object)) {
                result++;
            }
        }
        return result;
    }


    // Serialization

    private static final String OPERAND_TYPE_TAG = "operandType";
    private static final String SCOPE_LEVEL_TAG = "scopeLevel";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(OPERAND_TYPE_TAG, operandType.toString());

        xw.writeAttribute(SCOPE_LEVEL_TAG, scopeLevel.toString());

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

        operandType = xr.getAttribute(OPERAND_TYPE_TAG,
                                      OperandType.class, OperandType.NONE);

        scopeLevel = xr.getAttribute(SCOPE_LEVEL_TAG,
                                     ScopeLevel.class, ScopeLevel.NONE);

        int val = xr.getAttribute(VALUE_TAG, INFINITY);
        if (val != INFINITY) value = new Integer(val);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return (value != null) ? Integer.toString(value)
            : scopeLevel + "'s number of " + operandType + "s";
    }

    // getXMLElementTagName apparently not needed, uses parents.
}
